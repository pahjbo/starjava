package uk.ac.starlink.tptask;

import gnu.jel.CompilationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.tplot.DataBounds;
import uk.ac.starlink.tplot.HistogramUtils;
import uk.ac.starlink.tplot.HistogramPlotState;
import uk.ac.starlink.tplot.PlotData;
import uk.ac.starlink.tplot.PlotState;
import uk.ac.starlink.tplot.Range;
import uk.ac.starlink.tplot.Style;
import uk.ac.starlink.tplot.TablePlot;

/**
 * PlotStateFactory for a histogram plot.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2008
 */
public class HistogramPlotStateFactory extends PlotStateFactory {

    private final DoubleParameter yloParam_;
    private final DoubleParameter yhiParam_;
    private final BooleanParameter ylogParam_;
    private final DoubleParameter binwidthParam_;
    private final BooleanParameter normParam_;
    private final BooleanParameter cumulativeParam_;
    private final BooleanParameter zeromidParam_;

    /**
     * Constructor.
     */
    public HistogramPlotStateFactory() {
        super( new String[] { "X", }, false, false, 0 );

        yloParam_ = new DoubleParameter( "ylo" );
        yloParam_.setPrompt( "Lower bound for Y axis" );
        yloParam_.setDescription( new String[] {
            "<p>Lower bound for Y axis.",
            "</p>",
        } );
        yloParam_.setDefault( "0" );

        yhiParam_ = new DoubleParameter( "yhi" );
        yhiParam_.setNullPermitted( true );
        yhiParam_.setPrompt( "Upper bound for Y axis" );
        yhiParam_.setDescription( new String[] {
            "<p>Upper bound for Y axis.",
            "Autogenerated from the data if not supplied.",
            "</p>",
        } );
 
        ylogParam_ = new BooleanParameter( "ylog" );
        ylogParam_.setPrompt( "Logarithmic Y axis?" );
        ylogParam_.setDescription( new String[] {
            "<p>Whether to use a logarithmic scale for the Y axis.",
            "</p>",
        } );
        ylogParam_.setDefault( "false" );

        binwidthParam_ = new DoubleParameter( "binwidth" );
        binwidthParam_.setPrompt( "Bin width" );
        binwidthParam_.setDescription( new String[] {
            "<p>Defines the width on the X axis of histogram bins.",
            "</p>",
        } );
        binwidthParam_.setNullPermitted( true );

        normParam_ = new BooleanParameter( "norm" );
        normParam_.setPrompt( "Normalise bin sizes?" );
        normParam_.setDescription( new String[] {
            "<p>Determines whether bin counts are normalised.",
            "If true, histogram bars are scaled such that",
            "summed height of all bars over the whole dataset is equal to one.",
            "Otherwise (the default), no scaling is done.",
            "</p>",
        } );
        normParam_.setDefault( "false" );

        cumulativeParam_ = new BooleanParameter( "cumulative" );
        cumulativeParam_.setPrompt( "Cumulative plot?" );
        cumulativeParam_.setDescription( new String[] {
            "<p>Determines whether historams are cumulative.",
            "When false (the default), the height of each bar is determined",
            "by counting the number of points which fall into the range",
            "on the X axis that it covers.",
            "When true, the height is determined by counting all the points",
            "between negative infinity and the upper bound of the range",
            "on the X axis that it covers.",
            "</p>",
        } );
        cumulativeParam_.setDefault( "false" );

        zeromidParam_ = new BooleanParameter( "offset" );
        zeromidParam_.setDefault( "false" );
        
    }

    public Parameter[] getParameters() {
        String tSuffix = TABLE_VARIABLE;
        List paramList =
            new ArrayList( Arrays.asList( super.getParameters() ) );
        paramList.add( yloParam_ );
        paramList.add( yhiParam_ );
        paramList.add( ylogParam_ );
        paramList.add( createWeightParameter( tSuffix ) );
        paramList.add( binwidthParam_ );
        paramList.add( normParam_ );
        paramList.add( cumulativeParam_ );
        paramList.add( zeromidParam_ );
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    protected PlotState createPlotState() {
        return new HistogramPlotState();
    }

    protected void configurePlotState( PlotState pstate, Environment env )
            throws TaskException {
        super.configurePlotState( pstate, env );
        HistogramPlotState state = (HistogramPlotState) pstate;

        boolean ylog = ylogParam_.booleanValue( env );
        state.setLogFlags( new boolean[] {
            state.getLogFlags()[ 0 ],
            ylog,
        } );
        state.setFlipFlags( new boolean[] {
            state.getFlipFlags()[ 0 ],
            false,
        } );

        if ( ylog ) {
            yloParam_.setNullPermitted( true );
            yloParam_.setDefault( null );
        }
        else {
            yloParam_.setNullPermitted( false );
            yloParam_.setDefault( "0" );
        }
        double ylo = yloParam_.doubleValue( env );
        double yhi = yhiParam_.doubleValue( env );

        state.setRanges( new double[][] {
            state.getRanges()[ 0 ],
            new double[] { ylo, yhi, },
        } );

        state.setBinWidth( binwidthParam_.doubleValue( env ) );

        state.setNormalised( normParam_.booleanValue( env ) );

        state.setAxisLabels( new String[] {
            state.getAxisLabels()[ 0 ],
            HistogramUtils.getYInfo( state.getWeighted(),
                                     state.getNormalised() )
                          .getName(),
        } );

        state.setNormalised( normParam_.booleanValue( env ) );
        state.setCumulative( cumulativeParam_.booleanValue( env ) );
        state.setZeroMid( zeromidParam_.booleanValue( env ) );
    }

    protected TablePlotData createPlotData( Environment env, String tLabel,
                                            StarTable table, String[] setExprs,
                                            String[] setNames,
                                            Style[] setStyles, String labelExpr,
                                            String[] coordExprs,
                                            String[] errExprs )
            throws TaskException, CompilationException {
        coordExprs =
            new String[] { coordExprs[ 0 ],
                           createWeightParameter( tLabel ).stringValue( env ) };
        return new CartesianTablePlotData( table, setExprs, setNames, setStyles,
                                           labelExpr, coordExprs, errExprs );
    }

    protected StyleFactory createStyleFactory( String prefix ) {
        return new BarStyleFactory( prefix );
    }

    protected boolean requiresConfigureFromBounds( PlotState state ) {
        for ( int idim = 0; idim < 2; idim++ ) {
            double[] range = state.getRanges()[ idim ];
            if ( Double.isNaN( range[ 0 ] ) || Double.isNaN( range[ 1 ] ) ) {
                return true;
            }
        }
        return false;
    }

    public DataBounds calculateBounds( PlotState state, TablePlot plot ) {
        DataBounds xDataBounds = super.calculateBounds( state, plot );
        Range xRange = xDataBounds.getRanges()[ 0 ];
        boolean xlog = state.getLogFlags()[ 0 ];
        double[] xBounds = xRange.getFiniteBounds( xlog );
  return null;
    }

    private Parameter createWeightParameter( String tlabel )  {
        Parameter param = new Parameter( "weight" + tlabel );
        param.setPrompt( "Histogram weighting for table " + tlabel );
        param.setDescription( new String[] {
            "<p>Defines a weighting for each point accumulated to determine",
            "the height of plotted bars.",
            "If this parameter has a value other than 1 (the default)",
            "then instead of simply accumulating the number of points per bin",
            "to determine bar height,",
            "the bar height will be the sum over the weighting expression",
            "for the points in each bin.",
            "Note that with weighting, the figure drawn is no longer",
            "strictly speaking a histogram.",
            "</p>",
            "<p>When weighted, bars can be of negative height.",
            "An anomaly of the plot as currently implemented is that the",
            "Y axis never descends below zero, so any such bars are currently",
            "invisible.",
            "This may be amended in a future release",
            "(contact the author to lobby for such an amendment).",
            "</p>",
        } );
        param.setDefault( "1" );
        return param;
    }
}
