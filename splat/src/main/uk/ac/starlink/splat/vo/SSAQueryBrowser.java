/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.ConnectionUtil;
import jsky.util.gui.ProgressPanel;
import jsky.util.SwingWorker;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.gui.ProxySetupFrame;

import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;

/**
 * Display a page of controls for querying an SSA server and display the
 * results of that query. The spectra returned from that query can then be
 * selected and displayed in the main SPLAT browser.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAQueryBrowser
    extends JFrame
{
    /**
     * The object holding the list of servers that we should use for SSA
     * querys.
     */
    private SSAServerList serverList = null;

    /**
     * The instance of SPLAT we're associated with. XXX should abstract this
     * into an interface.
     */
    private SplatBrowser browser = null;

    /** Content pane of frame */
    protected JPanel contentPane = null;

    /** Panel for action buttons */
    protected JPanel actionBarContainer = null;

    /** Menubar */
    protected JMenuBar menuBar = null;

    /** The file menu */
    protected JMenu fileMenu = null;

    /** Object name */
    protected JTextField nameField = null;

    /** Central RA */
    protected JTextField raField = null;

    /** Central Dec */
    protected JTextField decField = null;

    /** Region radius */
    protected JTextField radiusField = null;

    /** Tabbed pane showing the query results tables */
    protected JTabbedPane resultsPane = null;

    /** The list of StarJTables in use */
    protected ArrayList starJTables = null;

    /** NED nameserver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD nameserver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAQueryBrowser( SSAServerList serverList, SplatBrowser browser )
    {
        this.serverList = serverList;
        this.browser = browser;
        initUI();
        initMenus();
        initFrame();
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "Query VO for Spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        setSize( new Dimension( 550, 500 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    private void initMenus()
    {
        //  Add the menuBar.
        menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon helpImage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );

        //  Create the File menu.
        fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBarContainer = new JPanel();
        actionBarContainer.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "ssa-window", "Help on window",
                                  menuBar, null );
    }


    /**
     * Create and display the UI components.
     */
    private void initUI()
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        initQueryComponents();

        initResultsComponent();

        setDefaultNameServers();
    }

    /**
     * Populate the NORTH part of window with the basic query components.
     */
    private void initQueryComponents()
    {
        JPanel queryPanel = new JPanel();
        queryPanel.setBorder
            ( BorderFactory.createTitledBorder( "Search region:" ) );

        GridBagLayouter layouter =
            new GridBagLayouter( queryPanel, GridBagLayouter.SCHEME3 );
        contentPane.add( queryPanel, BorderLayout.NORTH );

        //  Object name.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 15 );
        nameField.setToolTipText( "Enter the name of an object " +
                                  "and press return to get coordinates" );
        layouter.add( nameLabel, false );
        layouter.add( nameField, false );

        JButton nameLookup = new JButton( "Lookup" );
        layouter.add( nameLookup, true );

        nameLookup.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    resolveName();
                }
            });

        //  Also need to arrange for a resolver to look up the coordinates of
        //  the object name, when return is pressed.
        nameField.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    resolveName();
                }
            });

        //  RA and Dec fields. We're free-formatting on these (decimal degrees
        //  not required).
        JLabel raLabel = new JLabel( "RA:" );
        raField = new JTextField( 15 );
        layouter.add( raLabel, false );
        layouter.add( raField, true );
        raField.setToolTipText( "Enter the RA of field centre, " +
                                "decimal degrees or hh:mm:ss.ss" );

        JLabel decLabel = new JLabel( "Dec:" );
        decField = new JTextField( 15 );
        layouter.add( decLabel, false );
        layouter.add( decField, true );
        decField.setToolTipText( "Enter the Dec of field centre, " +
                                 "decimal degrees or dd:mm:ss.ss" );

        //  Radius field.
        JLabel radiusLabel = new JLabel( "Radius:" );
        radiusField = new JTextField( "10.0", 15 );
        layouter.add( radiusLabel, false );
        layouter.add( radiusField, true );
        radiusField.setToolTipText( "Enter radius of field to search" +
                                    " from given centre, arcminutes" );

        //  Do the search.
        JButton goButton = new JButton( "Go" );
        JPanel buttonPanel = new JPanel();
        buttonPanel.add( goButton );

        layouter.add( buttonPanel, true );
        layouter.eatSpare();

        goButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    doQuery();
                }
            });
    }

    /**
     * Make the results component. This is mainly JTabbedPane containing a
     * JTable for each set of results (the tables are realized later) and
     * a button to display the selected spectra.
     */
    private void initResultsComponent()
    {
        JPanel resultsPanel = new JPanel( new BorderLayout() );
        resultsPanel.setBorder
            ( BorderFactory.createTitledBorder( "Query results:" ) );

        resultsPane = new JTabbedPane();
        resultsPanel.add( resultsPane, BorderLayout.CENTER );

        JPanel controlPanel = new JPanel();
        JButton displaySelectedButton = new JButton( "Display selected" );
        controlPanel.add( displaySelectedButton );

        //  Add action to display all currently selected spectra.
        displaySelectedButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    displaySpectra( true );
                }
            });

        JButton displayAllButton = new JButton( "Display all" );
        controlPanel.add( displayAllButton );

        //  Add action to display all spectra.
        displayAllButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    displaySpectra( false );
                }
            });

        resultsPanel.add( controlPanel, BorderLayout.SOUTH );
        contentPane.add( resultsPanel, BorderLayout.CENTER );
    }


    /**
     * Arrange to resolve the object name into coordinates.
     */
    protected void resolveName()
    {
        String objectName = nameField.getText();
        if ( objectName != null && objectName.length() > 0 ) {

            //  Should offer choice between NED and SIMBAD.
            final QueryArgs queryArgs = new BasicQueryArgs( simbadCatalogue );
            queryArgs.setId( objectName );

            Thread thread = new Thread( "Name server" ) {
               public void run() {
                  try {
                      QueryResult r = simbadCatalogue.query( queryArgs );
                      if ( r instanceof TableQueryResult ) {
                          Coordinates coords =
                              ((TableQueryResult) r).getCoordinates(0);
                          if ( coords instanceof WorldCoords ) {
                              //  Enter values into RA and Dec fields.
                              String[] radec = ((WorldCoords)coords).format();
                              raField.setText( radec[0] );
                              decField.setText( radec[1] );
                          }
                      }
                  }
                  catch (Exception e) {
                      e.printStackTrace();
                  }
               }
            };

            //  Start the nameserver.
            thread.start();
        }
    }

    /**
     * Setup the default name servers (SIMBAD and NED) to use to resolve
     * astronomical object names. Note these are just those used in JSky.
     * A better implementation should reuse the JSky classes.
     */
    private void setDefaultNameServers()
    {
        Properties p1 = new Properties();
        p1.setProperty( "serv_type", "namesvr" );
        p1.setProperty( "long_name", "SIMBAD Names via CADC" );
        p1.setProperty( "short_name", "simbad_ns@cadc" );
        p1.setProperty
            ( "url",
              "http://cadcwww.dao.nrc.ca/cadcbin/sim-server?&o=%id" );
        SkycatConfigEntry entry = new SkycatConfigEntry( p1 );
        simbadCatalogue = new SkycatCatalog( entry );

        Properties p2 = new Properties();
        p2.setProperty( "serv_type", "namesvr" );
        p2.setProperty( "long_name", "NED Names" );
        p2.setProperty( "short_name", "ned@eso" );
        p2.setProperty
            ( "url",
              "http://archive.eso.org/skycat/servers/ned-server?&o=%id" );
        entry = new SkycatConfigEntry( p2 );
        nedCatalogue = new SkycatCatalog( entry );
    }

    /**
     * Perform the query to all the known SSA servers (XXX need to select
     * which servers to use).
     */
    public void doQuery()
    {
        //  Get the position.
        String ra = raField.getText();
        String dec = decField.getText();
        if ( ra == null || ra.length() == 0 ||
             dec == null || dec.length() == 0 ) {
            JOptionPane.showMessageDialog( this,
                  "You have not supplied a search centre",
                  "No RA or Dec", JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  And the radius.
        String radiusText = radiusField.getText();
        double radius = 10.0;
        if ( radiusText != null && radiusText.length() > 0 ) {
            try {
                radius = Double.parseDouble( radiusText );
            }
            catch (NumberFormatException e) {
                new ExceptionDialog(this, "Cannot understand radius value", e);
                return;
            }
        }

        //  Create a stack of all queries to perform.
        ArrayList queryList = new ArrayList();
        Iterator i = serverList.getIterator();
        SSAServer server = null;
        while( i.hasNext() ) {
            SSAQuery ssaQuery = new SSAQuery( (SSAServer) i.next() );
            ssaQuery.setPosition( ra, dec );
            ssaQuery.setRadius( radius );
            queryList.add( ssaQuery );
        }

        // Now actually do the queries, these are performed in a separate
        // Thread so we avoid locking the interface.
        processQueryList( queryList );
    }

    private ProgressPanel progressPanel = null;
    private SwingWorker worker = null;

    /**
     * If it does not already exist, make the panel used to display
     * the progress of network access.
     */
    protected void makeProgressPanel()
    {
        if ( progressPanel == null ) {
            progressPanel = ProgressPanel.makeProgressPanel
                ( "Querying SSA servers for spectra...", this );

            //  When the cancel button is pressed we want to stop the
            //  SwingWorker thread.
            progressPanel.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( worker != null ) {
                        worker.interrupt();
                        worker = null;
                    }
                }
            });
        }
    }

    /**
     * Process a list of URL queries to SSA servers and display the
     * results. All processing is performed in a background Thread.
     */
    protected void processQueryList( ArrayList queryList )
    {
        final ArrayList localQueryList = queryList;
        makeProgressPanel();

        worker = new SwingWorker()
        {
            boolean interrupted = false;
            public Object construct()
            {
                try {
                    runProcessQueryList( localQueryList );
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
                return null;
            }

            public void finished()
            {
                System.out.println( "SwingWorker finished: " + interrupted );
                progressPanel.stop();
                worker = null;
                queryThread = null;

                //  Display the results.
                if ( ! interrupted ) {
                    makeResultsDisplay( localQueryList );
                }
            }
        };
        worker.start();
    }

    private Thread queryThread = null;

    /**
     * Do the query to all the SSAP servers.
     */
    private void runProcessQueryList( ArrayList queryList )
        throws InterruptedException
    {
        //  We just download VOTables, so avoid attempting to build the other
        //  formats.
        StarTableFactory factory = new StarTableFactory();
        TableBuilder[] blist = { new VOTableBuilder() };
        factory.setDefaultBuilders( blist );

        StarTable starTable = null;
        Iterator i = queryList.iterator();
        //int j = 0;
        while( i.hasNext() ) {
            try {
                SSAQuery ssaQuery = (SSAQuery) i.next();
                URL url = ssaQuery.getQueryURL();
                progressPanel.logMessage( "Querying: " +
                                          ssaQuery.getDescription() );
                starTable = factory.makeStarTable( url );
                ssaQuery.setStarTable( starTable );
                progressPanel.logMessage( "Done" );

                //uk.ac.starlink.table.StarTableOutput sto =
                //    new uk.ac.starlink.table.StarTableOutput();
                //sto.writeStarTable( starTable,
                //                    "votable" + j + ".xml", null);
                //j++;
            }
            catch (IOException ie) {
                progressPanel.logMessage( ie.getMessage() );
                ie.printStackTrace();
            }
            if ( Thread.interrupted() ) {
                throw new InterruptedException();
            }
        }
        progressPanel.logMessage( "Completed downloads" );
    }

    /**
     * Display the results of the queries to the SSA servers.
     */
    protected void makeResultsDisplay( ArrayList tableList )
    {
        if ( starJTables == null ) {
            starJTables = new ArrayList();
        }

        //  Remove existing tables (XXX reuse for efficiency).
        resultsPane.removeAll();
        starJTables.clear();

        Iterator i = tableList.iterator();
        JScrollPane scrollPane = null;
        SSAQuery ssaQuery = null;
        StarTable starTable = null;
        StarJTable table = null;
        while ( i.hasNext() ) {
            ssaQuery = (SSAQuery) i.next();
            starTable = ssaQuery.getStarTable();
            if ( starTable != null ) {
                table = new StarJTable( starTable, true );
                scrollPane = new JScrollPane( table );
                resultsPane.addTab( ssaQuery.getDescription(), scrollPane );
                starJTables.add( table );

                //  Set widths of columns.
                table.configureColumnWidths( 200, 5 );
            }
        }
    }

    /**
     * Get the main SPLAT browser to download and display any selected
     * spectra.
     */
    protected void displaySpectra( boolean selected )
    {
        //  List of all spectra to be loaded and there data formats.
        ArrayList specList = new ArrayList();
        ArrayList typeList = new ArrayList();

        //  Visit all the tabbed StarJTables.
        Iterator i = starJTables.iterator();
        RowSequence rseq = null;
        while ( i.hasNext() ) {
            extractSpectraFromTable( (StarJTable) i.next(), specList,
                                     typeList, selected );
        }

        //  If we have no spectra complain and stop.
        if ( specList.size() == 0 ) {
            String mess;
            if ( selected ) {
                mess = "There are no spectra selected";
            }
            else {
                mess = "No spectra available";
            }
            JOptionPane.showMessageDialog( this, mess, "No spectra", 
                                           JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  Create the String[] list of names and also transform MIME types
        //  into SPLAT types.
        int nspec = specList.size();
        String[] spectra = new String[nspec];
        for ( int k = 0; k < nspec; k++ ) {
            spectra[k] = ((String) specList.get( k )).trim();
        }
        int ntypes = typeList.size();
        int[] types = null;
        if ( ntypes > 0 ) {
            types = mimeToSPLATTypes( typeList );
        }

        //  And load and display...
        browser.threadLoadSpectra( spectra, types );
    }

    /**
     * Extract all the links to spectra for downloading, plus the associated
     * data formats, if available from the rows of table. Can return the
     * selected spectra, if requested, otherwise all spectra are returned.
     */
    private void extractSpectraFromTable(StarJTable table, ArrayList specList,
                                         ArrayList typeList, boolean selected)
    {
        int[] selection = null;
        //  Check for a selection if required.
        if ( selected ) {
            selection = table.getSelectedRows();
        }

        // Only do this if we're processing all rows or we have a selection.
        if ( selection == null || selection.length > 0 ) {
            StarTable starTable = table.getStarTable();

            //  Check for a column that contains links to the actual data
            //  (XXX these could be XML links to data within this
            //  document). The signature for this is an UCD of DATA_LINK.
            int ncol = starTable.getColumnCount();
            int linkcol = -1;
            int typecol = -1;
            ColumnInfo colInfo;
            for( int k = 0; k < ncol; k++ ) {
                colInfo = starTable.getColumnInfo( k );
                if (colInfo.getUCD().equalsIgnoreCase("DATA_LINK")) {
                    linkcol = k;
                }
                if (colInfo.getUCD().equalsIgnoreCase("VOX:SPECTRUM_FORMAT")) {
                    typecol = k;
                }
            }
            
            //  If we have a DATA_LINK column, gather the URLs it contains
            //  that are appropriate.
            if ( linkcol != -1 ) {
                RowSequence rseq = null;
                try {
                    if ( ! selected ) {
                        //  Using all rows.
                        rseq = starTable.getRowSequence();
                        while ( rseq.hasNext() ) {
                            rseq.next();
                            specList.add( rseq.getCell( linkcol ) );
                            if ( typecol != -1 ) {
                                typeList.add( rseq.getCell( typecol ) );
                            }
                        }
                    }
                    else {
                        //  Just using selected rows. To do this we step
                        //  through the table and check if that row is the
                        //  next one in the selection (the selection is
                        //  sorted).
                        rseq = starTable.getRowSequence();
                        int k = 0; // Table row
                        int l = 0; // selection index
                        while ( rseq.hasNext() ) {
                            rseq.next();
                            if ( k == selection[l] ) {

                                // Store this one as matches selection.
                                specList.add( rseq.getCell( linkcol ) );
                                if ( typecol != -1 ) {
                                    typeList.add( rseq.getCell(typecol) );
                                }

                                //  Move to next selection.
                                l++;
                                if ( l >= selection.length ) {
                                    break;
                                }
                            }
                            k++;
                        }
                    }
                }
                catch (IOException ie) {
                    ie.printStackTrace();
                }
                finally {
                    try {
                        if ( rseq != null ) {
                            rseq.close();
                        }
                    }
                    catch (IOException iie) {
                        // Ignore.
                    }
                }
            }
        }
    }

    /**
     * Convert a list of mime types into the equivalent SPLAT types (these are
     * int constants defined in SpecDataFactory).
     */
    private int[] mimeToSPLATTypes( ArrayList typeList )
    {
        int ntypes = typeList.size();
        int[] types = new int[ntypes];
        String type = null;
        for ( int k = 0; k < ntypes; k++ ) {
            type = (String) typeList.get( k );
            if ( type.equals( "application/fits" ) ) {
                //  FITS format, is that image or table?
                types[k] = SpecDataFactory.FITS;
            }
            else if ( type.equals( "spectrum/fits" ) ) {
                //  FITS format, is that image or table? Don't know who
                //  thought this was a mime-type?
                types[k] = SpecDataFactory.FITS;
            }
            else if ( type.equals( "text/plain" ) ) {
                //  ASCII table of some kind.
                types[k] = SpecDataFactory.TABLE;
            }
            else if ( type.equals( "application/x-votable+xml" ) ) {
                // VOTable spectrum.
                types[k] = SpecDataFactory.TABLE;
            }
            else {
                // Hope the URL has a recognisable file extension.
                types[k] = SpecDataFactory.DEFAULT;
            }
        }
        return types;
    }
        
    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    public static void main( String[] args )
    {
        SSAQueryBrowser b = new SSAQueryBrowser( new SSAServerList(), null );
        b.pack();
        b.setVisible( true );
    }
}
