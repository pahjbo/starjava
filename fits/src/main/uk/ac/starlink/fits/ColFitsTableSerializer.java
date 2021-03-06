package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Object which can serialize a StarTable to a data stream as a one-row FITS
 * file in which each element contains an entire column of the table.
 *
 * @author   Mark Taylor
 * @since    26 Jun 2006
 */
public class ColFitsTableSerializer implements FitsTableSerializer {

    private final WideFits wide_;
    private final ColumnStore[] colStores_;
    private final String[] colids_;
    private final int ncol_;
    private final long nrow_;
    private final String tname_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param   table  table to serialize
     * @param   wide   convention for representing over-wide tables;
     *                 null to avoid this convention
     * @throws IOException if it won't be possible to write the given table
     */
    public ColFitsTableSerializer( StarTable table, WideFits wide )
            throws IOException {
        wide_ = wide;

        /* Prepare an array of column storage objects which know how to do
         * serial storage/retrieval of the data in a table column. */
        tname_ = table.getName();
        ncol_ = table.getColumnCount();
        colStores_ = new ColumnStore[ ncol_ ];
        colids_ = new String[ ncol_ ];
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            colids_[ icol ] = info.toString();
            colStores_[ icol ] = FileColumnStore.createColumnStore( info );
            if ( colStores_[ icol ] == null ) {
                logger_.warning( "Can't serialize column " + info );
            }
            else {
                nUseCol++;
            }
        }
        FitsConstants.checkColumnCount( wide, nUseCol );

        /* Store the table data into these storage objects. */
        boolean ok = false;
        RowSequence rseq = table.getRowSequence();
        try {
            long lrow = 0L;
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    ColumnStore colStore = colStores_[ icol ];
                    if ( colStore != null ) {
                        colStore.storeValue( row[ icol ] ); 
                    }
                }
                lrow++;
            }
            nrow_ = lrow;
            ok = true;
        }
        finally {
            rseq.close();

            /* In case of error, tidy up now, since these objects may be
             * expensive. */
            if ( ! ok ) {
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    if ( colStores_[ icol ] != null ) {
                        colStores_[ icol ].dispose();
                    }
                }
            }
        }

        /* Declare that the storage has been completed, thereby readying the
         * storage objects for data retrieval. */
        assert ok;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            if ( colStores_[ icol ] != null ) {
                if ( colStores_[ icol ] != null ) {
                    colStores_[ icol ].endStores();
                }
            }
        }
    }

    public Header getHeader() throws HeaderCardException {

        /* Work out the length of the single table row. */
        long size = 0L; 
        long extSize = 0L;
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnStore colStore = colStores_[ icol ];
            if ( colStore != null ) {
                nUseCol++;
                long leng = colStore.getDataLength();
                size += leng;
                if ( wide_ != null &&
                     nUseCol >= wide_.getContainerColumnIndex() ) {
                    extSize += leng;
                }
            }
        }

        /* Work out the number of standard and extended columns.
         * This won't fail because of checks carried out in constructor. */
        int nStdCol =
              wide_ != null && nUseCol > wide_.getContainerColumnIndex()
            ? wide_.getContainerColumnIndex()
            : nUseCol;
        boolean hasExtCol = nUseCol > nStdCol;

        /* Write the standard part of the FITS header. */
        Header hdr = new Header();
        hdr.addValue( "XTENSION", "BINTABLE", "binary table extension" );
        hdr.addValue( "BITPIX", 8, "8-bit bytes" );
        hdr.addValue( "NAXIS", 2, "2-dimensional table" );
        hdr.addValue( "NAXIS1", size, "width of single row in bytes" );
        hdr.addValue( "NAXIS2", 1, "single-row table" );
        hdr.addValue( "PCOUNT", 0, "size of special data area" );
        hdr.addValue( "GCOUNT", 1, "one data group" );
        hdr.addValue( "TFIELDS", nStdCol, "number of columns" );

        /* Add EXTNAME record containing table name. */
        if ( tname_ != null && tname_.trim().length() > 0 ) {
            FitsConstants
           .addTrimmedValue( hdr, "EXTNAME", tname_, "table name" );
        }

        /* Add extended column header information if applicable. */
        if ( hasExtCol ) {
            wide_.addExtensionHeader( hdr, nUseCol );
            AbstractWideFits.logWideWrite( logger_, nStdCol, nUseCol ); 
        }

        /* Ask each ColumnStore to add header cards describing the data
         * cell it will write. */
        int jcol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnStore colStore = colStores_[ icol ];
            if ( colStore != null ) {
                jcol++;
                if ( hasExtCol && jcol == nStdCol ) {
                    wide_.addContainerColumnHeader( hdr, extSize, nrow_ );
                }
                BintableColumnHeader colhead =
                      hasExtCol && jcol >= nStdCol
                    ? wide_.createExtendedHeader( nStdCol, jcol )
                    : BintableColumnHeader.createStandardHeader( jcol );
                colStore.addHeaderInfo( hdr, colhead, jcol );
            }
        }
        return hdr;
    }

    /**
     * Write the FITS data unit populated by this serializer.
     *
     * @param  out  destination stream
     */
    public void writeData( DataOutput out ) throws IOException {

        /* Write the cell provided by each serializer in turn. */
        long size = 0L;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            if ( colStores_[ icol ] != null ) {
                logger_.info( "Writing column " + ( icol + 1 ) + "/" + ncol_ 
                            + ": " + colids_[ icol ] );
                ColumnStore colStore = colStores_[ icol ];
                colStore.streamData( out );
                size += colStore.getDataLength();
                colStore.dispose();
            }
        }

        /* Write padding if necessary. */
        int over = (int) ( size % 2880L );
        if ( over > 0 ) {
            out.write( new byte[ 2880 - over ] );
        }
    }

    public long getRowCount() {
        return nrow_;
    }

    public char getFormatChar( int icol ) {
        ColumnStore colStore = colStores_[ icol ];
        if ( colStore != null ) {
            String tform = getCardValue( colStore, "TFORM" ).trim();
            return tform.charAt( tform.length() - 1 );
        }
        else {
            return (char) 0;
        }
    }

    public int[] getDimensions( int icol ) {
        ColumnStore colStore = colStores_[ icol ];
        if ( colStore != null ) {
            long[] tdims =
                ColFitsStarTable.parseTdim( getCardValue( colStore, "TDIM" ) );
            int[] dims = new int[ tdims.length - 1 ];
            for ( int i = 0; i < tdims.length - 1; i++ ) {
                dims[ i ] = Tables.checkedLongToInt( tdims[ i ] );
            }
            return dims;
        }
        else {
            return null;
        }
    }

    public String getBadValue( int icol ) {
        ColumnStore colStore = colStores_[ icol ];
        if ( colStore != null ) {
            String tnull = getCardValue( colStore, "TNULL" );
            if ( tnull != null && tnull.trim().length() > 0 ) {
                return tnull;
            }
        }
        return null;
    }

    private static String getCardValue( ColumnStore colStore, String tcard ) {
        Header hdr = new Header();
        int icol = 99;
        BintableColumnHeader colhead =
            BintableColumnHeader.createStandardHeader( icol );

        /* Avoid unwanted logging messages that might occur in case of
         * truncated card values. */
        Level level = logger_.getLevel();
        logger_.setLevel( Level.SEVERE );
        try {
            colStore.addHeaderInfo( hdr, colhead, icol );
        }
        catch ( HeaderCardException e ) {
            throw new AssertionError( e );
        }
        finally {
            logger_.setLevel( level );
        }
        String key = colhead.getKeyName( tcard );
        return hdr.containsKey( key )
             ? hdr.findCard( key ).getValue().trim()
             : null;
    }
}
