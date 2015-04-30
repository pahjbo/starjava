package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * TapMetaReader implementation that reads data from a vs:TableSet document.
 * This can be found at the /tables endpoint of a TAP service.
 *
 * <p>All of the available information is read by {@link #readSchemas},
 * so the other <code>read*</code> methods never need be called,
 * and will throw UnsupportedOperationExceptions.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2015
 * @see      <a href="http://www.ivoa.net/documents/VODataService/"
 *              >VODataService</a>
 */
public class TableSetTapMetaReader implements TapMetaReader {

    private final URL url_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param    tablesetUrl  URL of some document containing VOSITables
     *           <code>&lt;schema&gt;</code> elements
     */
    public TableSetTapMetaReader( String tablesetUrl ) {
        try {
            url_ = new URL( tablesetUrl );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalArgumentException( "Not a URL: " + tablesetUrl );
        }
    }

    public SchemaMeta[] readSchemas() throws IOException {
        logger_.info( "Reading table metadata from " + url_ );
        try {
            return TableSetSaxHandler.readTableSet( url_ );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Invalid TableSet XML document" )
                 .initCause( e );
        }
    }

    /** @throws UnsupportedOperationException */
    public TableMeta[] readTables( SchemaMeta schema ) {
        throw readNotNeeded();
    }

    /** @throws UnsupportedOperationException */
    public ColumnMeta[] readColumns( TableMeta table ) {
        throw readNotNeeded();
    }

    /** @throws UnsupportedOperationException */
    public ForeignMeta[] readForeignKeys( TableMeta table ) {
        throw readNotNeeded();
    }

    public String getSource() {
        return url_.toString();
    }

    /**
     * Returns a new UnsupportedOperationException indicating that a
     * read method is never needed.
     */
    private UnsupportedOperationException readNotNeeded() {
        String msg = "readSchemas method returns fully populated schemas; "
                   + "you should not need to call this method";
        return new UnsupportedOperationException( msg );
    }
}