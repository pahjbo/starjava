package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Panel for display of a TAP query for a given TAP service.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public class TapQueryPanel extends JPanel {

    private final JEditorPane textPanel_;
    private final TableSetPanel tmetaPanel_;
    private final TapCapabilityPanel tcapPanel_;
    private final JLabel serviceLabel_;
    private final JLabel countLabel_;
    private final AdqlTextAction exampleAct_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public TapQueryPanel() {
        super( new BorderLayout() );

        /* Prepare a panel for table metadata display. */
        tmetaPanel_ = new TableSetPanel();

        /* Prepare a panel to contain service capability information. */
        tcapPanel_ = new TapCapabilityPanel();

        /* Prepare a panel to contain user-entered ADQL text. */
        textPanel_ = new JEditorPane();
        textPanel_.setEditable( true );
        textPanel_.setFont( Font.decode( "Monospaced" ) );
        JComponent textScroller = new JScrollPane( textPanel_ );

        /* Actions to replace text in ADQL panel. */
        final AdqlTextAction clearAct =
                new AdqlTextAction( "Clear",
                                    "Clear currently visible ADQL text "
                                  + "from editor" ) {
            public boolean isEnabled() {
                return textPanel_.getDocument().getLength() > 0;
            }
        };
        clearAct.setAdqlText( "" );
        exampleAct_ =
            new AdqlTextAction( "Example",
                                "Set ADQL text to an example query "
                              + "for this service" );
        textPanel_.getDocument().addDocumentListener( new DocumentListener() {
            public void changedUpdate( DocumentEvent evt ) {
            }
            public void insertUpdate( DocumentEvent evt ) {
                changed();
            }
            public void removeUpdate( DocumentEvent evt ) {
                changed();
            }
            private void changed() {
                clearAct.setEnabled( clearAct.isEnabled() );
            }
        } );
        tmetaPanel_.getTableSelector().addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                configureExamples();
            }
        } );

        /* Controls for ADQL text panel. */
        Box buttLine = Box.createHorizontalBox();
        buttLine.setBorder( BorderFactory.createEmptyBorder( 0, 2, 2, 0 ) );
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( exampleAct_ ) );
        buttLine.add( Box.createHorizontalStrut( 5 ) );
        buttLine.add( new JButton( clearAct ) );

        /* Place components on ADQL panel. */
        JComponent adqlPanel = new JPanel( new BorderLayout() );
        adqlPanel.add( buttLine, BorderLayout.NORTH );
        adqlPanel.add( textScroller, BorderLayout.CENTER );
        JComponent qPanel = new JPanel( new BorderLayout() );
        qPanel.add( tcapPanel_, BorderLayout.NORTH );
        qPanel.add( adqlPanel, BorderLayout.CENTER );

        /* Prepare a panel for the TAP service heading. */
        serviceLabel_ = new JLabel();
        countLabel_ = new JLabel();
        JComponent tableHeading = Box.createHorizontalBox();
        tableHeading.add( new JLabel( "Service: " ) );
        tableHeading.add( serviceLabel_ );
        tableHeading.add( Box.createHorizontalStrut( 10 ) );
        tableHeading.add( countLabel_ );

        /* Arrange the components in a split pane. */
        final JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        JComponent servicePanel = new JPanel( new BorderLayout() );
        servicePanel.add( tableHeading, BorderLayout.NORTH );
        servicePanel.add( tmetaPanel_, BorderLayout.CENTER );
        adqlPanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "ADQL Text" ) );
        servicePanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Table Metadata" ) );
        tcapPanel_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Service Capabilities" ) );
        splitter.setTopComponent( servicePanel );
        splitter.setBottomComponent( qPanel );
        splitter.setResizeWeight( 0.8 );
        add( splitter, BorderLayout.CENTER );
    }

    /**
     * Returns the panel used to hold and display the TAP capability
     * information.
     *
     * @return  capability display panel
     */
    public TapCapabilityPanel getCapabilityPanel() {
        return tcapPanel_;
    }

    /**
     * Returns the text panel used for the ADQL text entered by the user.
     *
     * @return   ADQL text entry component
     */
    public JEditorPane getAdqlPanel() {
        return textPanel_;
    }

    /**
     * Returns the text currently entered in the ADQL text component.
     *
     * @return  adql text supplied by user
     */
    public String getAdql() {
        return textPanel_.getText();
    }

    /**
     * Sets a short text string describing the TAP service used by this panel.
     *
     * @param  serviceHeading  short, human-readable label for the
     *         service this panel relates to
     */
    public void setServiceHeading( String serviceHeading ) {
        serviceLabel_.setText( serviceHeading );
    }

    /**
     * Sets the service URL for the TAP service used by this panel.
     * Calling this will initiate an asynchronous attempt to fill in
     * service metadata from the service at the given URL.
     *
     * @param  serviceUrl  base URL for a TAP service
     */
    public void setServiceUrl( final String serviceUrl ) {

        /* Prepare the URL where we can find the TableSet document. */
        final URL url;
        try {
            url = new URL( serviceUrl );
        }
        catch ( MalformedURLException e ) {
            return;
        }

        /* Dispatch a request to acquire the table metadata from
         * the service. */
        setTables( null );
        tmetaPanel_.showFetchProgressBar( "Fetching Table Metadata" );
        new Thread( "Table metadata fetcher" ) {
            public void run() {
                final TableMeta[] tableMetas;
                try {
                    tableMetas = TapQuery.readTableMetadata( url );
                }
                catch ( final Exception e ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            tmetaPanel_.showFetchFailure( url + "/tables", e );
                        }
                    } );
                    return;
                }

                /* On success, install this information in the GUI. */
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        setTables( tableMetas );
                    }
                } );
            }
        }.start();

        /* Dispatch a request to acquire the service capability information
         * from the service. */
        tcapPanel_.setCapability( null );
        new Thread( "Table capability fetcher" ) {
            public void run() {
                final TapCapability cap;
                try {
                    cap = TapQuery.readTapCapability( url );
                }
                catch ( final Exception e ) {
                    logger_.warning( "Failed to acquire TAP service capability "
                                   + "information" );
                    return;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        tcapPanel_.setCapability( cap );
                        configureExamples();
                    }
                } );
            }
        }.start();
    }

    /**
     * Sets the metadata panel to display a given set of table metadata.
     *
     * @param  tmetas  table metadata list; null if no metadata is available
     */
    private void setTables( TableMeta[] tmetas ) {

        /* Populate table metadata JTable. */
        tmetaPanel_.setTables( tmetas );

        /* Display number of tables. */
        String countText;
        if ( tmetas == null ) {
            countText = "";
        }
        else if ( tmetas.length == 1 ) {
            countText = "(1 table)";
        }
        else {
            countText = "(" + tmetas.length + " tables)";
        }
        countLabel_.setText( countText );
        configureExamples();
    }

    /**
     * Works with the known table and service metadata currently displayed
     * to set up example queries.
     */
    private void configureExamples() {
        TableMeta table = tmetaPanel_.getSelectedTable();
        if ( table != null ) {
            AdqlExemplifier exampler =
                AdqlExemplifier
               .createExemplifier( tcapPanel_.getQueryLanguage(), true );
            exampleAct_.setAdqlText( exampler.createSimpleExample( table ) );
        }
    }

    /**
     * Action which replaces the current content of the ADQL text entry
     * area with some fixed string.
     */
    private class AdqlTextAction extends AbstractAction {
        private String text_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  description   action short description
         */
        public AdqlTextAction( String name, String description ) {
            super( name );
            putValue( SHORT_DESCRIPTION, description );
            setAdqlText( null );
        }

        public void actionPerformed( ActionEvent evt ) {
            textPanel_.setText( text_ );
        }

        /**
         * Sets the text which this action will insert.
         * Enabledness is determined by whether <code>text</code> is null.
         *
         * @param  text  ADQL text
         */
        public void setAdqlText( String text ) {
            text_ = text;
            setEnabled( text != null );
        }
    }
}
