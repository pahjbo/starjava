package uk.ac.starlink.fits;

import java.util.logging.Logger;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;

/**
 * Implementations of the WideFits interface.
 * This class fills in the details of the general idea defined in
 * WideFits.  Static methods provide concrete implementations.
 *
 * <p>The Wide FITS convention is defined in the file
 * (fits/src/docs/)wide-fits.txt
 *
 * @author   Mark Taylor
 * @since    27 Jul 2017
 */
public abstract class AbstractWideFits implements WideFits {

    private final int icolContainer_;
    private final int extColMax_;
    private final String name_;

    /** Index of container column hosting extended column data. */
    public static final String KEY_ICOL_CONTAINER = "XT_ICOL";

    /** Header key for extended column count - includes standard ones. */
    public static final String KEY_NCOL_EXT = "XT_NCOL";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  icolContainer  1-based index of container column
     *                        used for storing extended column data;
     *                        usually 999
     * @param  extColMax    maximum number of extended columns
     *                      (including standard columns) that can be
     *                      represented by this convention
     * @param  implName   base name of this implementation
     */
    protected AbstractWideFits( int icolContainer, int extColMax,
                                String implName ) {
        icolContainer_ = icolContainer;
        if ( icolContainer_ > MAX_NCOLSTD ) {
            throw new IllegalArgumentException( "Container column index > "
                                              + MAX_NCOLSTD );
        }
        extColMax_ = extColMax;
        name_ = implName
              + ( icolContainer == MAX_NCOLSTD ? "" : icolContainer );
    }

    public int getContainerColumnIndex() {
        return icolContainer_;
    }

    public int getExtColumnMax() {
        return extColMax_;
    }

    public void addContainerColumnHeader( Header hdr, long nbyteExt,
                                          long nslice ) {

        /* Work out how we will declare the data type for this column
         * in the FITS header.  Since this column data is not supposed
         * to have any meaning, it doesn't matter what format we use
         * as long as it's the right length.
         * The obvious thing would be to use 'B' format (byte),
         * and that does work.  However, FITS bytes are unsigned,
         * and java bytes are signed.  To get round that,
         * the STIL FITS reading code usually turns FITS bytes into java
         * shorts on read.  That doesn't break anything, but it means that
         * if a non-WideFits-aware STIL reader encounters a WideFits table,
         * it may end up doing expensive and useless conversions of
         * large byte arrays to large short arrays.
         * (Other software may or may not have similar issues with
         * FITS unsigned bytes, I don't know).
         * So, if the element size is an even number of bytes, write it
         * using TFORM = 'I' (16-bit signed integer) instead.
         * Since the FITS and java 16-bit types match each other, this
         * avoids the problem.  If it's an odd number, we still have to
         * go with bytes. */
        final char formChr;
        final short formSiz;
        long elSize = nslice > 0 ? nbyteExt / nslice : nbyteExt;
        if ( elSize % 2 == 0 ) {
            formChr = 'I';
            formSiz = 2;
        }
        else {
            formChr = 'B';
            formSiz = 1;
        }
        long nEl = nbyteExt / formSiz;
        assert nEl * formSiz == nbyteExt;

        /* If requested, prepare to write a TDIM header. */
        final String dimStr;
        if ( nslice > 0 ) {
            if ( nEl % nslice == 0 ) {
                dimStr = new StringBuffer()
                    .append( '(' )
                    .append( nEl / nslice )
                    .append( ',' )
                    .append( nslice )
                    .append( ')' )
                    .toString();
            }
            else {
                logger_.severe( nEl + " not divisible by " + nslice
                              + " - no TDIM" + icolContainer_ );
                dimStr = null;
            }
        }
        else {
            dimStr = null;
        }

        /* Add the relevant entries to the header. */
        BintableColumnHeader colhead =
            BintableColumnHeader.createStandardHeader( icolContainer_ );
        String forcol = " for column " + icolContainer_;
        try {
            hdr.addValue( colhead.getKeyName( "TTYPE" ), "XT_MORECOLS",
                          "label" + forcol );
            hdr.addValue( colhead.getKeyName( "TFORM" ), nEl + "" + formChr,
                          "format" + forcol );
            if ( dimStr != null ) {
                hdr.addValue( colhead.getKeyName( "TDIM" ), dimStr,
                              "dimensions" + forcol );

            }
            hdr.addValue( colhead.getKeyName( "TCOMM" ),
                          "Extension buffer for columns beyond "
                         + icolContainer_, null );
        }
        catch ( HeaderCardException e ) {
            throw new RuntimeException( e );  // shouldn't happen
        }
    }

    public void addExtensionHeader( Header hdr, int ncolExt ) {
        try {
            hdr.addValue( KEY_ICOL_CONTAINER, icolContainer_,
                          "index of container column" );
            hdr.addValue( KEY_NCOL_EXT, ncolExt,
                          "total columns including extended" );
        }
        catch ( HeaderCardException e ) {
            throw new RuntimeException( e );  // shouldn't happen
        }
    }

    public int getExtendedColumnCount( HeaderCards cards, int ncolStd ) {
        Integer icolContainerValue = cards.getIntValue( KEY_ICOL_CONTAINER );
        Integer ncolExtValue = cards.getIntValue( KEY_NCOL_EXT );
        if ( icolContainerValue == null && ncolExtValue == null ) {
            return ncolStd;
        }
        else if ( icolContainerValue == null || ncolExtValue == null ) {
            logger_.warning( "FITS header has one but not both of "
                           + KEY_ICOL_CONTAINER + " and " + KEY_NCOL_EXT
                           + " - no extended columns" );
            return ncolStd;
        }
        int icolContainer = icolContainerValue.intValue();
        int ncolExt = ncolExtValue.intValue();
        if ( icolContainer != ncolStd ) {
            logger_.warning( "FITS header " + KEY_ICOL_CONTAINER + "="
                           + icolContainer
                           + " != standard column count (TFIELDS) " + ncolStd
                           + " - no extended columns" );
            return ncolStd;
        }
        logger_.config( "Located extended columns in wide FITS file" );
        return ncolExt;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Utility method to write a log message indicating that this
     * convention is being used to write a FITS file.
     *
     * @param  logger   logger
     * @param  nStdcol  number of standard FITS columns
     * @param  nAllcol  total number of columns including extended
     */
    public static void logWideWrite( Logger logger, int nStdcol, int nAllcol ) {
        if ( nAllcol > nStdcol ) {
            logger.warning( "Using non-standard extended column convention" );
            logger.warning( "Other FITS software may not see columns "
                          + nStdcol + "-" + nAllcol );
        }
    }

    /**
     * Utility method to write a log message indicating that this
     * convention is being used to read a FITS file.
     *
     * @param  logger   logger
     * @param  nStdcol  number of standard FITS columns
     * @param  nAllcol  total number of columns including extended
     */
    public static void logWideRead( Logger logger, int nStdcol, int nAllcol ) {
        if ( nAllcol > nStdcol ) {
            logger.info( "Using non-standard extended column convention "
                       + "for columns " + nStdcol + "-" + nAllcol );
        }
    }
}