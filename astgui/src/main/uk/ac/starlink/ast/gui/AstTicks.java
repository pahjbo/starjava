/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     02-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.grf.DefaultGrf;

/**
 * AstTicks is a model of the tick mark elements shown in say an AST Plot. It
 * encompasses all the values that describe the representation and returns
 * these in various formats (such as the complete AST Plot options list for
 * drawing it).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstTicks
    extends AbstractPlotControlsModel
{
    /**
     * Whether tick marking is set or unset.
     */
    protected boolean isSet;

    /**
     * Whether ticks mark should be shown (different from unset).
     */
    protected boolean show;

    /**
     * Whether to apply the log spacing values, or leave at the context
     * default (on for logplot, off otherwise).
     */
    protected boolean logSpacingSet;

    /**
     * Whether X tick mark should be shown with log spacing.
     */
    protected boolean xLogSpacing;

    /**
     * Whether Y tick mark should be shown with log spacing.
     */
    protected boolean yLogSpacing;

    /**
     * The colour of the ticks.
     */
    protected Color colour;

    /**
     * The gap between X axis major ticks (set to BAD if not set). This is in
     * units of the X axis.
     */
    protected double xGap;

    /**
     * The gap between Y axis major ticks (set to BAD if not set). This is in
     * units of the Y axis.
     */
    protected double yGap;

    /**
     * The gap between X axis major ticks (set to 0 if not set),
     * when drawing with log spacing. This is a power of 10, not the actual
     * gap.
     */
    protected int xLogGap;

    /**
     * The gap between Y axis major ticks (set to 0 if not set),
     * when drawing with log spacing. This is a power of 10, not the actual
     * gap.
     */
    protected int yLogGap;

    /**
     * Length of the major tick marks of the X axis. Set as a fraction of the
     * displayed plot size.
     */
    protected double majorXTicklen;

    /**
     * Length of the major tick marks of the Y axis. Set as a fraction of the
     * displayed plot size.
     */
    protected double majorYTicklen;

    /**
     * Length of the minor tick marks of the X axis. Set as a fraction of the
     * displayed plot size.
     */
    protected double minorXTicklen;

    /**
     * Length of the minor tick marks of the X axis. Set as a fraction of the
     * displayed plot size.
     */
    protected double minorYTicklen;

    /**
     * Number of minor divisions to shown between major tick marks on the X
     * axis (0 if not set).
     */
    protected int minorXDivisions;

    /**
     * Number of minor divisions to shown between major tick marks on the Y
     * axis (0 if not set).
     */
    protected int minorYDivisions;

    /**
     * The style of line used for tickmarks. Should be a Grf style.
     */
    protected int style;

    /**
     * The width of the lines used for tickmarks.
     */
    protected double width;

    /**
     * Whether to tick all axes, or just the main ones.
     */
    protected boolean tickAll;

    /**
     * Suggested maximum length of tick mark.
     */
    public final static double MAX_LENGTH = 0.1;

    /**
     * Suggested minimum length of tick mark.
     */
    public final static double MIN_LENGTH = -MAX_LENGTH;

    /**
     * Suggested step (resolution) between tick mark lengths.
     */
    public final static double STEP_LENGTH = 0.005;


    /**
     * Create a empty instance. This indicates that all tick marking should
     * remain at the AST Plot default.
     */
    public AstTicks()
    {
        setDefaults();
    }


    /**
     * Set all values to their defaults.
     */
    public void setDefaults()
    {
        isSet = false;
        show = true;
        logSpacingSet = false;
        xLogSpacing = true;
        yLogSpacing = true;
        colour = Color.black;
        xGap = DefaultGrf.BAD;
        yGap = DefaultGrf.BAD;
        xLogGap = 0;
        yLogGap = 0;
        majorXTicklen = 0.015;
        majorYTicklen = 0.015;
        minorXTicklen = 0.007;
        minorYTicklen = 0.007;
        minorXDivisions = 0;
        minorYDivisions = 0;
        style = DefaultGrf.PLAIN;
        width = 1.0;
        tickAll = true;
        fireChanged();
    }


    /**
     * Set whether any of these values are set, It not set then all tick
     * marking remains at the default configuration.
     *
     * @param isSet The new state value
     */
    public void setState( boolean isSet )
    {
        this.isSet = isSet;
    }


    /**
     * Return whether values are set.
     *
     * @return The state value
     */
    public boolean getState()
    {
        return isSet;
    }


    /**
     * Set whether the tick marks should be shown or not.
     *
     * @param show The new shown value
     */
    public void setShown( boolean show )
    {
        this.show = show;
        setState( true );
        fireChanged();
    }


    /**
     * Get whether the tick marks are to be shown.
     *
     * @return The shown value
     */
    public boolean getShown()
    {
        return show;
    }

    /**
     * Set whether to apply log spacing setting to axes.
     *
     * @param logSpacingSet new value.
     */
    public void setLogSpacingSet( boolean logSpacingSet )
    {
        this.logSpacingSet = logSpacingSet;
        if ( logSpacingSet ) {
            setState( true );
        }
        fireChanged();
    }

    /**
     * Get whether the log spacing values are being applied.
     *
     * @return The value
     */
    public boolean getLogSpacingSet()
    {
        return logSpacingSet;
    }

    /**
     * Set whether to display ticks using log spacing along X axis.
     *
     * @param xLogSpacing new value.
     */
    public void setXLogSpacing( boolean xLogSpacing )
    {
        this.xLogSpacing = xLogSpacing;
        setState( true );
        fireChanged();
    }

    /**
     * Get whether to use log spacing along X axis.
     *
     * @return The value
     */
    public boolean getXLogSpacing()
    {
        return xLogSpacing;
    }

    /**
     * Set whether to display ticks using log spacing along Y axis.
     *
     * @param yLogSpacing new value.
     */
    public void setYLogSpacing( boolean yLogSpacing )
    {
        this.yLogSpacing = yLogSpacing;
        setState( true );
        fireChanged();
    }

    /**
     * Get whether to use log spacing along Y axis.
     *
     * @return The value
     */
    public boolean getYLogSpacing()
    {
        return yLogSpacing;
    }

   /**
     * Set the colour of the tick marks.
     *
     * @param colour The new colour value
     */
    public void setColour( Color colour )
    {
        this.colour = colour;
        if ( colour != null ) {
            setState( true );
        }
        fireChanged();
    }

    /**
     * Get the colour of the tick marks.
     *
     * @return The colour value
     */
    public Color getColour()
    {
        return colour;
    }


    /**
     * Set the major gap along the X axis. Grf.BAD to indicate that the
     * default value should be used. The units are those of the X axis.
     *
     * @param xGap The new xGap value
     */
    public void setXGap( double xGap )
    {
        if ( xGap == 0.0 ) {
            this.xGap = DefaultGrf.BAD;  // For backward compat, 0.0
                                         // meant this once.
        }
        else {
            this.xGap = xGap;
        }
        if ( xGap != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the X gap between major ticks.
     *
     * @return The xGap value
     */
    public double getXGap()
    {
        return xGap;
    }


    /**
     * Set the major gap along the Y axis. Grf.BAD to indicate that the
     * default value should be used. The units are those of the Y axis.
     *
     * @param yGap The new yGap value
     */
    public void setYGap( double yGap )
    {
        if ( yGap == 0.0 ) {
            this.yGap = DefaultGrf.BAD;  // For backward compat, 0.0
                                         // meant this once.
        }
        else {
            this.yGap = yGap;
        }
        if ( yGap != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the Y gap between major ticks.
     *
     * @return The yGap value
     */
    public double getYGap()
    {
        return yGap;
    }


    /**
     * Set log spacing power of 10 for major ticks along the X axis.
     * A setting of 0 indicates that the default value should be used.
     *
     * @param xLogGap The new xLogGap value
     */
    public void setXLogGap( int xLogGap )
    {
        this.xLogGap = xLogGap;
        if ( xLogGap != 0 ) {
            setState( true );
        }
        fireChanged();
    }

    /**
     * Get the gap power of 10 for the major ticks on an X log axis.
     *
     * @return The xLogGap value
     */
    public int getXLogGap()
    {
        return xLogGap;
    }

    /**
     * Set log spacing power of 10 for major ticks along the X axis.
     * A setting of 0 indicates that the default value should be used.
     *
     * @param yLogGap The new yLogGap value
     */
    public void setYLogGap( int yLogGap )
    {
        this.yLogGap = yLogGap;
        if ( yLogGap != 0 ) {
            setState( true );
        }
        fireChanged();
    }

    /**
     * Get the gap power of 10 for the major ticks on an Y log axis.
     *
     * @return The yLogGap value
     */
    public int getYLogGap()
    {
        return yLogGap;
    }


    /**
     * Set the line width. The value DefaultGrf.BAD means no value.
     *
     * @param width The new width value
     */
    public void setWidth( double width )
    {
        this.width = width;
        if ( width != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the line width.
     *
     * @return The width value
     */
    public double getWidth()
    {
        return width;
    }


    /**
     * Set the line style. This should be a style known to the Grf class (i.e.
     * Grf.PLAIN, Grf.DASH etc.)
     *
     * @param style The new style value
     */
    public void setStyle( int style )
    {
        this.style = style;
        setState( true );
        fireChanged();
    }


    /**
     * Get the line style.
     *
     * @return The style value
     */
    public double getStyle()
    {
        return style;
    }


    /**
     * Set whether to tick all the axes, or just the ones adjacent to the
     * number labels.
     *
     * @param tickAll The new tickAll value
     */
    public void setTickAll( boolean tickAll )
    {
        this.tickAll = tickAll;
        setState( true );
        fireChanged();
    }


    /**
     * Get whether we're ticking all the axes or just the ones adjacent to the
     * number labels.
     *
     * @return The tickAll value
     */
    public boolean getTickAll()
    {
        return tickAll;
    }


    /**
     * Set the length of the X axis major ticks. DefaultGrf.BAD means use the
     * default size
     *
     * @param majorXTicklen The new majorXTicklength value
     */
    public void setMajorXTicklength( double majorXTicklen )
    {
        this.majorXTicklen = majorXTicklen;
        if ( majorXTicklen != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Set the length of the Y axis major ticks. DefaultGrf.BAD means use the
     * default size
     *
     * @param majorYTicklen The new majorYTicklength value
     */
    public void setMajorYTicklength( double majorYTicklen )
    {
        this.majorYTicklen = majorYTicklen;
        if ( majorXTicklen != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the length of the X axis major tick marks.
     *
     * @return The majorXTicklength value
     */
    public double getMajorXTicklength()
    {
        return majorXTicklen;
    }


    /**
     * Get the length of the Y axis major tick marks.
     *
     * @return The majorYTicklength value
     */
    public double getMajorYTicklength()
    {
        return majorYTicklen;
    }


    /**
     * Set the length of the X axis minor ticks. DefaultGrf.BAD means use the
     * default size
     *
     * @param minorXTicklen The new minorXTicklength value
     */
    public void setMinorXTicklength( double minorXTicklen )
    {
        this.minorXTicklen = minorXTicklen;
        if ( minorXTicklen != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Set the length of the Y axis minor ticks. DefaultGrf.BAD means use the
     * default size
     *
     * @param minorYTicklen The new minorYTicklength value
     */
    public void setMinorYTicklength( double minorYTicklen )
    {
        this.minorYTicklen = minorYTicklen;
        if ( minorYTicklen != DefaultGrf.BAD ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the length of the X axis minor tick marks.
     *
     * @return The minorXTicklength value
     */
    public double getMinorXTicklength()
    {
        return minorXTicklen;
    }


    /**
     * Get the length of the Y axis minor tick marks.
     *
     * @return The minorYTicklength value
     */
    public double getMinorYTicklength()
    {
        return minorYTicklen;
    }


    /**
     * Set the number of minor divisions, between major ticks, along the X
     * axis. Set to 0 for default number.
     *
     * @param minorXDivisions The new minorXDivisions value
     */
    public void setMinorXDivisions( int minorXDivisions )
    {
        this.minorXDivisions = minorXDivisions;
        if ( minorXDivisions != 0 ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the number of minor divisions between major ticks on the X axis.
     *
     * @return The minorXDivisions value
     */
    public int getMinorXDivisions()
    {
        return minorXDivisions;
    }


    /**
     * Set the number of minor divisions, between major ticks, along the Y
     * axis. Set to 0 for default number.
     *
     * @param minorYDivisions The new minorYDivisions value
     */
    public void setMinorYDivisions( int minorYDivisions )
    {
        this.minorYDivisions = minorYDivisions;
        if ( minorYDivisions != 0 ) {
            setState( true );
        }
        fireChanged();
    }


    /**
     * Get the number of minor divisions between major ticks on the Y axis.
     *
     * @return The minorYDivisions value
     */
    public int getMinorYDivisions()
    {
        return minorYDivisions;
    }


    /**
     * Get the AST plot options description of this object.
     *
     * @return The astOptions value
     */
    public String getAstOptions()
    {
        if ( ! isSet ) {
            return "";
        }

        StringBuffer buffer = new StringBuffer();

        // If not showing ticks then need to set lengths to 0.
        if ( ! show ) {
            buffer.append( "MajTicklen=0.0" );
            buffer.append( ",MinTicklen=0.0" );
            return buffer.toString();
        }

        //  General options (all ticks).
        buffer.append( "Style(ticks)=" );
        buffer.append( style );
        if ( colour != null ) {
            buffer.append( ",Colour(ticks)=" );
            int value = DefaultGrf.encodeColor( colour );
            if ( value == -1 ) {
                value = 0;
            }
            buffer.append( value );
        }

        if ( width != DefaultGrf.BAD ) {
            buffer.append( ",Width(ticks)=" );
            buffer.append( width );
        }

        if ( tickAll ) {
            buffer.append( ",TickAll=1" );
        }
        else {
            buffer.append( ",TickAll=0" );
        }

        //  Log options.
        if ( logSpacingSet ) {
            if ( xLogSpacing ) {
                buffer.append( ",LogTicks(1)=1" );
            }
            else {
                buffer.append( ",LogTicks(1)=0" );
            }
            if ( yLogSpacing ) {
                buffer.append( ",LogTicks(2)=1" );
            }
            else {
                buffer.append( ",LogTicks(2)=0" );
            }
        }
        if ( xLogGap != 0 ) {
            buffer.append( ",LogGap(1)=" );
            buffer.append( Math.pow( 10.0, xLogGap ) );
        }
        if ( yLogGap != 0 ) {
            buffer.append( ",LogGap(2)=" );
            buffer.append( Math.pow( 10.0, yLogGap ) );
        }

        //  Axis options.
        if ( xGap != DefaultGrf.BAD ) {
            buffer.append( ",Gap(1)=" );
            buffer.append( xGap );
        }
        if ( yGap != DefaultGrf.BAD ) {
            buffer.append( ",Gap(2)=" );
            buffer.append( yGap );
        }

        if ( majorXTicklen != DefaultGrf.BAD ) {
            buffer.append( ",MajTicklen(1)=" );
            buffer.append( majorXTicklen );
        }
        if ( majorYTicklen != DefaultGrf.BAD ) {
            buffer.append( ",MajTicklen(2)=" );
            buffer.append( majorYTicklen );
        }

        if ( minorXTicklen != DefaultGrf.BAD ) {
            buffer.append( ",MinTicklen(1)=" );
            buffer.append( minorXTicklen );
        }
        if ( minorYTicklen != DefaultGrf.BAD ) {
            buffer.append( ",MinTicklen(2)=" );
            buffer.append( minorYTicklen );
        }

        if ( minorXDivisions != 0 ) {
            buffer.append( ",MinTick(1)=" );
            buffer.append( minorXDivisions );
        }
        if ( minorYDivisions != 0 ) {
            buffer.append( ",MinTick(2)=" );
            buffer.append( minorYDivisions );
        }
        return buffer.toString();
    }


    /**
     * Get a string representation of the AST options.
     *
     * @return Description of the Return Value
     */
    public String toString()
    {
        return getAstOptions();
    }


//
// Encode and decode this object to/from XML representation.
//
    /**
     * The name of our enclosing tag.
     */
    public String getTagName()
    {
        return "ticks";
    }

    /**
     * Description of the Method
     *
     * @param rootElement Description of the Parameter
     */
    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "isSet", isSet );

        addChildElement( rootElement, "show", show );

        if ( colour != null ) {
            addChildElement( rootElement, "colour", colour );
        }

        addChildElement( rootElement, "logSpacingSet", logSpacingSet );
        addChildElement( rootElement, "xLogSpacing", xLogSpacing );
        addChildElement( rootElement, "yLogSpacing", yLogSpacing );
        addChildElement( rootElement, "xLogGap", xLogGap );
        addChildElement( rootElement, "yLogGap", yLogGap );

        addChildElement( rootElement, "xGap", xGap );
        addChildElement( rootElement, "yGap", yGap );

        addChildElement( rootElement, "majorXTicklen", majorXTicklen );
        addChildElement( rootElement, "majorYTicklen", majorYTicklen );

        addChildElement( rootElement, "minorXTicklen", minorXTicklen );
        addChildElement( rootElement, "minorYTicklen", minorYTicklen );

        addChildElement( rootElement, "minorXDivisions", minorXDivisions );
        addChildElement( rootElement, "minorYDivisions", minorYDivisions );

        addChildElement( rootElement, "style", style );

        addChildElement( rootElement, "width", width );

        addChildElement( rootElement, "tickAll", tickAll );
    }


    /**
     * Set the value of a member variable by matching its name to a known
     * local property string.
     *
     * @param name The new fromString value
     * @param value The new fromString value
     */
    public void setFromString( String name, String value )
    {
        if ( name.equals( "isSet" ) ) {
            setState( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "show" ) ) {
            setShown( booleanFromString( value ) );
            return;
        }

        if ( name.equals( "logSpacingSet" ) ) {
            setLogSpacingSet( booleanFromString( value ) );
        }
        if ( name.equals( "xLogSpacing" ) ) {
            setXLogSpacing( booleanFromString( value ) );
        }
        if ( name.equals( "yLogSpacing" ) ) {
            setYLogSpacing( booleanFromString( value ) );
        }

        if ( name.equals( "xLogGap" ) ) {
            setXLogGap( intFromString( value ) );
            return;
        }
        if ( name.equals( "yLogGap" ) ) {
            setYLogGap( intFromString( value ) );
            return;
        }

        if ( name.equals( "colour" ) ) {
            setColour( colorFromString( value ) );
            return;
        }

        if ( name.equals( "xGap" ) ) {
            setXGap( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "yGap" ) ) {
            setYGap( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "majorXTicklen" ) ) {
            setMajorXTicklength( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "majorYTicklen" ) ) {
            setMajorYTicklength( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "minorXTicklen" ) ) {
            setMinorXTicklength( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "minorYTicklen" ) ) {
            setMinorYTicklength( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "minorXDivisions" ) ) {
            setMinorXDivisions( intFromString( value ) );
            return;
        }
        if ( name.equals( "minorYDivisions" ) ) {
            setMinorYDivisions( intFromString( value ) );
            return;
        }

        if ( name.equals( "style" ) ) {
            setStyle( intFromString( value ) );
            return;
        }

        if ( name.equals( "width" ) ) {
            setWidth( doubleFromString( value ) );
            return;
        }

        if ( name.equals( "tickAll" ) ) {
            setTickAll( booleanFromString( value ) );
            return;
        }

        System.err.println( "AstTicks: unknown configuration property:" +
            name + " (" + value + ")" );
    }
}
