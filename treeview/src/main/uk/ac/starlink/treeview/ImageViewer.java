package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.array.NDArray;

/**
 * Displays the pixels of a 2-d array, optionally with an AST grid plotted
 * over the top.  Some basic controls for configuring the display
 * may be available.
 */
class ImageViewer extends JPanel {

    /**
     * Construct an image view from an NDArray.
     *
     * @param  nda   a 2-dimensional readable NDArray with random access.
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     * @throws  IOException  if there is an error in data access
     */
    public ImageViewer( NDArray nda, FrameSet wcs ) throws IOException {
        super( new BorderLayout() );

        /* Get and place a viewer. */
        final ImageViewPane view = new ImageViewPane( nda, wcs );
        add( view, BorderLayout.CENTER );
        TreeviewLAF.configureMainPanel( view );

        /* Set up a box for basic viewer controls. */
        Box controlBox = new Box( BoxLayout.Y_AXIS );

        /* Add a control for Frame selection if appropriate. */
        if ( wcs != null ) {

            /* Construct a current frame selector. */
            int nfrm = wcs.getNframe();
            String[] frameNames = new String[ nfrm ];
            for ( int i = 0; i < nfrm; i++ ) {
                frameNames[ i ] = ( i + 1 ) + ": " 
                                + wcs.getFrame( i + 1 ).getDomain();
            }
            final JComboBox selecter = new JComboBox( frameNames );
            final Plot plot = view.getPlot();
            assert plot != null;
            selecter.setSelectedIndex( plot.getCurrent() - 2 );
            selecter.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent evt ) {
                    if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                        int currentFrame = selecter.getSelectedIndex() + 1;
                        plot.setCurrent( currentFrame + 1 );
                        view.rePlot();
                    }
                }
            } );

            /* Place it in the control box. */
            JComponent selecterBox = new Box( BoxLayout.X_AXIS );
            selecterBox.add( new JLabel( "Coordinate grid: " ) );
            selecterBox.add( selecter );
            selecterBox.add( Box.createGlue() );
            TreeviewLAF.configureControl( selecterBox );
            controlBox.add( selecterBox );
        }

        /* Place the control panel (unless it's empty). */
        if ( controlBox.getComponentCount() > 0 ) {
            TreeviewLAF.configureControlPanel( controlBox );
            add( controlBox, BorderLayout.NORTH );
        }
    }

} 
