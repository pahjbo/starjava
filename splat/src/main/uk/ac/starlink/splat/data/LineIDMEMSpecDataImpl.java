/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-APR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A LineIDSpecDataImpl that provides facilities for storing spectral
 * line identification information in a memory-based spectrum. It's
 * main uses are for making copies of existing objects and providing a
 * set of methods for use by implementations that will store there
 * information in memory only.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineIDMEMSpecDataImpl
    extends MEMSpecDataImpl
    implements LineIDSpecDataImpl
{
    /**
     * Create an object with just a symbolic name. The caller has
     * responsibility to make sure the spectrum is valid before
     * further use.
     *
     * @param name a symbolic name for the lines identified
     */
    public LineIDMEMSpecDataImpl( String name )
        throws SplatException
    {
        super( name );
    }

    /**
     * Create an object by copying another of the same type.
     *
     * @param name a symbolic name for the spectrum.
     * @param spectrum a SpecData object to copy.
     */
    public LineIDMEMSpecDataImpl( String name, SpecData spectrum )
        throws SplatException
    {
        super( name, spectrum );

        // Get any labels.
        if ( spectrum instanceof LineIDSpecData ) {
            setLabels( ( (LineIDSpecData) spectrum ).getLabels(), true );
        }
        else {
            //  Need a dummy set.
            makeLabels();
        }
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "Line Identifiers (MEMORY)";
    }

    /**
     * Return the line identification labels.
     */
    public String[] getLabels()
    {
        return labels;
    }

    /**
     * Set all the labels by making a copy (of the array holding the
     * String references), if asked.
     */
    public void setLabels( String[] labels, boolean copy )
        throws SplatException
    {
        String[] localLabels = labels;
        if ( copy ) {
            localLabels = new String[labels.length];
            for ( int i = 0; i < labels.length; i++ ) {
                localLabels[i] = labels[i];
            }
        }
        setLabels( localLabels );
    }

    /**
     * Set all the labels.
     */
    public void setLabels( String[] labels )
        throws SplatException
    {
        if ( coords == null || ( labels.length == coords.length ) ) {
            this.labels = labels;
        }
        else {
            throw new SplatException( "Array length must match coordinates" );
        }
    }

    /**
     * Make up a set of labels (assuming these will be modified
     * individually later). The names created are the equivalent
     * X axis value, or just a sequence number (plus some fluff to
     * make it clearly a String).
     */
    public void makeLabels()
        throws SplatException
    {
        if ( coords != null ) {
            String[] labels = new String[coords.length];
            if ( astref != null ) {
                for ( int i = 0; i < coords.length; i++ ) {
                    labels[i] = "Line at: " + astref.format( 1, coords[i] );
                }
            }
            else {
                for ( int i = 0; i < coords.length; i++ ) {
                    labels[i] = "Number: " + i;
                }
            }
            setLabels( labels );
        }
        else {
            throw new SplatException( "No coordinates are available" );
        }
    }

    /**
     * Get a specific label.
     */
    public String getLabel( int index )
    {
        if ( labels != null && ( index < labels.length ) ) {
            return labels[index];
        }
        return null;
    }

    /**
     * Set a specific label.
     */
    public void setLabel( int index, String label )
    {
        if ( getLabel( index ) != null ) {
            labels[index] = label;
        }
    }

    /**
     * Return if there were a complete set of data positions.
     */
    public boolean haveDataPositions()
    {
        return haveDataPositions;
    }

//
// Implementation specific methods and variables.
//
    /**
     * Reference to the line ID strings.
     */
    protected String[] labels = null;

    /**
     * Whether there are a complete set of data positions.
     */
    protected boolean haveDataPositions = false;

//
// Override some methods so that we can keep the haveDataPositions
// variable up to date.
//

    public void setData( double[] coords, double[] data )
        throws SplatException
    {
        haveDataPositions = ( data != null );
        super.setData( coords, data );
    }

    public void setData( FrameSet frameSet, double[] data )
        throws SplatException
    {
        haveDataPositions = ( data != null );
        super.setData( frameSet, data );
    }

    public void setDataQuick( double[] coords, double[] data )
    {
        haveDataPositions = ( data != null );
        super.setDataQuick( coords, data );
    }

    public void setDataQuick( FrameSet frameSet, double[] data )
    {
        haveDataPositions = ( data != null );
        super.setDataQuick( frameSet, data );
    }

    public void setData( double[] coords, double[] data, double[] errors )
        throws SplatException
    {
        haveDataPositions = ( data != null );
        super.setData( coords, data, errors );
    }

    public void setData( FrameSet frameSet, double[] data, double[] errors )
        throws SplatException
    {
        haveDataPositions = ( data != null );
        super.setData( frameSet, data, errors );
    }

    public void setDataQuick( double[] coords, double[] data, double[] errors )
    {
        haveDataPositions = ( data != null );
        super.setDataQuick( coords, data, errors );
    }

    public void setDataQuick( FrameSet frameSet, double[] data,
                              double[] errors )
    {
        haveDataPositions = ( data != null );
        super.setDataQuick( frameSet, data, errors );
    }

    public void setYDataValue( int index, double value )
        throws SplatException
    {
        haveDataPositions = true;
        if ( index < data.length ) {
            data[index] = value;
        }
        else {
            throw new SplatException( "Array index out of bounds" );
        }
    }
}

