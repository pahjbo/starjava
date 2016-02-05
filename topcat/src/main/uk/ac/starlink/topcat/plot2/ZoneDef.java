package uk.ac.starlink.topcat.plot2;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.PlotLayer;

/**
 * Supplies information about the content and configuration
 * of a plot on a single plot surface.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2016
 */
public interface ZoneDef<P,A> {

    /**
     * Returns the axis control GUI component for this zone.
     *
     * @return  axis controller
     */
    AxisController<P,A> getAxisController();

    /**
     * Returns the layers to be plotted on this zone.
     *
     * @return   plot layer array
     */
    PlotLayer[] getLayers();

    /**
     * Returns the legend icon associated with this zone, if any.
     *
     * @return  legend icon, or null
     */
    Icon getLegend();

    /**
     * Returns an array indicating the fractional position of the legend
     * within the plot surface.  A null value indicates that the legend,
     * if any, is to be displayed externally to the plot.
     *
     * @return   2-element x,y fractional location in range 0..1, or null
     */
    float[] getLegendPosition();

    /**
     * Returns a title string associated with this zone, if any.
     *
     * @return  title string, or null
     */
    String getTitle();

    /**
     * Returns the shader control GUI component for this zone.
     *
     * @return  shader control
     */
    ShaderControl getShaderControl();
}