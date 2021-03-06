package uk.ac.starlink.fits;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Handles writing of a StarTable in FITS binary format.
 * Not all columns can be written to a FITS table, only those ones
 * whose <tt>contentClass</tt> is in the following list:
 * <ul>
 * <li>Boolean
 * <li>Character
 * <li>Byte
 * <li>Short
 * <li>Integer
 * <li>Long
 * <li>Float
 * <li>Double
 * <li>Character
 * <li>String
 * <li>boolean[]
 * <li>char[]
 * <li>byte[]
 * <li>short[]
 * <li>int[]
 * <li>long[]
 * <li>float[]
 * <li>double[]
 * <li>String[]
 * </ul>
 * In all other cases a warning message will be logged and the column
 * will be ignored for writing purposes.
 * <p>
 * Output is currently to fixed-width columns only.  For StarTable columns
 * of variable size, a first pass is made through the table data to
 * determine the largest size they assume, and the size in the output
 * table is set to the largest of these.  Excess space is padded
 * with some sort of blank value (NaN for floating point values,
 * spaces for strings, zero-like values otherwise).
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableWriter extends AbstractFitsTableWriter {

    private final boolean allowSignedByte_;
    private final WideFits wide_;

    /**
     * Default constructor.
     */
    public FitsTableWriter() {
        this( "fits-basic", true, WideFits.DEFAULT );
    }

    /**
     * Custom constructor.
     *
     * @param   name   writer name
     * @param   allowSignedByte  if true, bytes written as FITS signed bytes
     *          (TZERO=-128), if false bytes written as signed shorts
     * @param   wide   convention for representing over-wide tables;
     *                 null to avoid this convention
     */
    public FitsTableWriter( String name, boolean allowSignedByte,
                            WideFits wide ) {
        super( name );
        allowSignedByte_ = allowSignedByte;
        wide_ = wide;
    }

    /**
     * Returns true if <tt>location</tt> ends with something like ".fit"
     * or ".fits" or ".fts".
     *
     * @param  location  filename
     * @return true if it sounds like a fits file
     */
    public boolean looksLikeFile( String location ) {
        int dotPos = location.lastIndexOf( '.' );
        if ( dotPos > 0 ) {
            String exten = location.substring( dotPos + 1 ).toLowerCase();
            if ( exten.equals( "fit" ) ||
                 exten.equals( "fits" ) ||
                 exten.equals( "fts" ) ) {
                return true;
            }
        }
        return false;
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        return new StandardFitsTableSerializer( table, allowSignedByte_,
                                                wide_ );
    }
}
