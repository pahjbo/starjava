package uk.ac.starlink.topcat;

import java.awt.Window;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.astrogrid.samp.Message;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.topcat.activate.SampSender;
import uk.ac.starlink.topcat.activate.ViewDatalinkActivationType;
import uk.ac.starlink.topcat.func.BasicImageDisplay;
import uk.ac.starlink.topcat.func.Browsers;
import uk.ac.starlink.topcat.func.Sog;

/**
 * Defines an action that consumes a URL.
 *
 * <p>Some useful implementations are provided by static factory methods.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public class UrlOptions {

    private final UrlInvoker[] invokers_;
    private final Map<ResourceType,UrlInvoker> dfltMap_;

    public UrlOptions( UrlInvoker[] invokers,
                       Map<ResourceType,UrlInvoker> dfltMap ) {
        invokers_ = invokers;
        dfltMap_ = dfltMap;
    }

    public UrlInvoker[] getInvokers() {
        return invokers_;
    }

    public Map<ResourceType,UrlInvoker> getDefaultsMap() {
        return dfltMap_;
    }

    public static UrlOptions createOptions( DatalinkPanel dlPanel ) {
        ControlWindow controlWin = ControlWindow.getInstance();
        UrlInvoker reportUrl = createReportInvoker();
        UrlInvoker viewImage = createViewImageInvoker();
        UrlInvoker loadTable = createLoadTableInvoker( controlWin );
        UrlInvoker browser = createBrowserInvoker();
        UrlInvoker viewDatalink = createDatalinkInvoker( dlPanel );
        UrlInvoker sendFitsImage =
            createSampInvoker( "FITS Image", "image.load.fits" );
        UrlInvoker sendTable =
            createSampInvoker( "Table", "table.load.votable" );
        // Note additional spectrum metadata is not supplied here.
        UrlInvoker sendSpectrum =
            createSampInvoker( "Spectrum", "spectrum.load.ssa-generic" );
        UrlInvoker[] invokers = new UrlInvoker[] {
            reportUrl,
            viewImage,
            loadTable,
            viewDatalink,
            sendFitsImage,
            sendSpectrum,
            sendTable,
            browser,
        };
        Map<ResourceType,UrlInvoker> map =
            new LinkedHashMap<ResourceType,UrlInvoker>();
        map.put( ResourceType.TABLE, loadTable );
        map.put( ResourceType.DATALINK, viewDatalink );
        map.put( ResourceType.FITS_IMAGE, viewImage );
        map.put( ResourceType.SPECTRUM, sendSpectrum );
        map.put( ResourceType.IMAGE, viewImage );
        map.put( ResourceType.WEB, browser );
        map.put( ResourceType.UNKNOWN, browser );
        assert map.keySet()
              .equals( new HashSet( Arrays.asList( ResourceType.values() ) ) );
        return new UrlOptions( invokers, map );
    }

    /**
     * Returns an invoker that simply reports the text of the URL
     * as its success outcome message.
     *
     * @return  new invoker
     */
    private static UrlInvoker createReportInvoker() {
        return new AbstractUrlInvoker( "Report URL" ) {
            public Outcome invokeUrl( URL url ) {
                return url == null ? Outcome.failure( "No URL" )
                                   : Outcome.success( url.toString() );
            }
        };
    }

    /**
     * Returns an invoker for loading a table into the application.
     *
     * @param  controlWin  application control window
     * @return  new invoker
     */
    private static UrlInvoker
            createLoadTableInvoker( final ControlWindow controlWin ) { 
        final boolean isSelect = true;
        final StarTableFactory tfact = controlWin.getTableFactory();
        return new AbstractUrlInvoker( "Load Table" ) {
            public Outcome invokeUrl( URL url ) {
                final String loc = url.toString();
                final StarTable table;
                try {
                    table = tfact.makeStarTable( loc );
                }
                catch ( IOException e ) {
                    return Outcome.failure( e );
                }
                final AtomicReference<TopcatModel> tcmref =
                    new AtomicReference<TopcatModel>();
                try {
                    SwingUtilities.invokeAndWait( new Runnable() {
                        public void run() {
                            tcmref.set( controlWin
                                       .addTable( table, loc, isSelect ) );
                        }
                    } );
                }
                catch ( Exception e ) {
                    return Outcome.failure( e );
                }
                return Outcome.success( tcmref.get().toString() );
            }
        };
    }

    /**
     * Returns an invoker for displaying DataLink table in a given panel.
     *
     * @param   dlPanel  pre-existing datalink table display panel,
     *                   or null to create one on demand
     * @return  new invoker
     */
    private static UrlInvoker
            createDatalinkInvoker( final DatalinkPanel dlPanel ) {
        return new AbstractUrlInvoker( "View DataLink Table" ) {
            private DatalinkPanel dlPanel_ = dlPanel;
            public Outcome invokeUrl( URL url ) {
                final boolean isNewPanel;
                synchronized ( this ) {
                    isNewPanel = dlPanel_ == null;
                    if ( isNewPanel ) {
                        dlPanel_ = new DatalinkPanel( true );
                    }
                }
                if ( isNewPanel ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            JFrame frm = new JFrame();
                            frm.getContentPane().add( dlPanel_ );
                            frm.pack();
                            frm.setVisible( true );
                        }
                    } );
                }
                Window window = isNewPanel
                              ? null
                              : (Window) SwingUtilities
                               .getAncestorOfClass( Window.class, dlPanel_ );
                return ViewDatalinkActivationType
                      .invokeLocation( url.toString(), dlPanel_, window );
            }
        };
    }

    /**
     * Returns an invoker for viewing an image in a suitable viewer.
     *
     * @return   new invoker
     */
    private static UrlInvoker createViewImageInvoker() {
        return new AbstractUrlInvoker( "View image internally" ) {
            public Outcome invokeUrl( URL url ) {
                String loc = url.toString();
                String label = "Image";
                String msg = TopcatUtils.canSog()
                           ? BasicImageDisplay.displayBasicImage( label, loc )
                           : Sog.sog( label, loc );
                return Outcome.success( msg );
            }
        };
    }

    /**
     * Returns an invoker for loading the URL into the system browser.
     *
     * @return  new invoker
     */
    private static UrlInvoker createBrowserInvoker() {
        return new AbstractUrlInvoker( "Show web page" ) {
            public Outcome invokeUrl( URL url ) {
                Browsers.systemBrowser( url.toString() );
                return Outcome.success( url.toString() );
            }
        };
    }

    /**
     * Returns a UrlInvoker that sends a SAMP load-type message.
     * The MType is supplied, and the message sent includes the
     * url string as the value of the message's "url" parameter.
     *
     * @param   contentName  user-readable type for resource to be sent
     * @param   mtype   MType for message to send
     * @return   new invoker
     */
    private static UrlInvoker createSampInvoker( final String contentName,
                                                 final String mtype ) {
        final SampSender sender = new SampSender( mtype );
        return new AbstractUrlInvoker( "Send " + contentName ) {
            public Outcome invokeUrl( URL url ) {
                Message message = new Message( mtype );
                message.addParam( "url", url.toString() );
                return sender.activateMessage( message );
            }
        };
    }

    /**
     * Utility class providing a partial UrlInvoker implementation.
     */
    private static abstract class AbstractUrlInvoker implements UrlInvoker {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  invoker name
         */
        AbstractUrlInvoker( String name ) {
            name_ = name;
        }

        public String getTitle() {
            return name_;
        }

        @Override
        public String toString() {
            return name_;
        }
    }
}