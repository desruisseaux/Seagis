package fr.ird.database.gui.swing;

// J2SE.
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOWriteProgressListener;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// SEAGIS.
import fr.ird.n1b.georef.gui.*;

/**
 * Boite de dialogue permettant de visualiser la progression d'un traitement.
 *
 * @author Remi Eve
 * @version $Id$
 */
class JProgress extends JDialog implements IIOReadProgressListener, 
                                           IIOWriteProgressListener 
{   
    /**
     * Bar de progression.
     */
    private final JProgressBar progress = new JProgressBar();
    
    /**
     * Constructeur.
     */
    public JProgress(final Frame parent, final boolean modal) {
        super (parent, modal);
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        progress.setIndeterminate(true);
        panel.add(progress);
        getContentPane().add(panel);       
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {{
                setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            }
        }});                
    }    
    
    /**
     * Set progressBar undeterminated.
     *
     * @param state     'true' pour indeterminé et 'false' sinon.
     */
    public final void setIndeterminate(final boolean state) {
        progress.setValue(0);
        progress.setIndeterminate(state);
    }
    
    /** 
     * Reports that the current image read operation has completed.
     * All <code>ImageReader</code> implementations are required to
     * call this method exactly once upon completion of each image
     * read operation.
     *
     * @param source the <code>ImageReader</code> object calling this
     * method.
     */
    public void imageComplete(final ImageReader source) {}
    
    /** 
     * Reports the approximate degree of completion of the current
     * <code>read</code> call of the associated
     * <code>ImageReader</code>.
     *
     * <p> The degree of completion is expressed as a percentage
     * varying from <code>0.0F</code> to <code>100.0F</code>.  The
     * percentage should ideally be calculated in terms of the
     * remaining time to completion, but it is usually more practical
     * to use a more well-defined metric such as pixels decoded or
     * portion of input stream consumed.  In any case, a sequence of
     * calls to this method during a given read operation should
     * supply a monotonically increasing sequence of percentage
     * values.  It is not necessary to supply the exact values
     * <code>0</code> and <code>100</code>, as these may be inferred
     * by the callee from other methods.
     *
     * <p> Each particular <code>ImageReader</code> implementation may
     * call this method at whatever frequency it desires.  A rule of
     * thumb is to call it around each 5 percent mark.
     *
     * @param source the <code>ImageReader</code> object calling this method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    public void imageProgress(final ImageReader source, final float percentageDone)  {
        progress.setValue(Math.round(percentageDone));
    }
    
    /** 
     * Reports that an image read operation is beginning.  All
     * <code>ImageReader</code> implementations are required to call
     * this method exactly once when beginning an image read
     * operation.
     *
     * @param source the <code>ImageReader</code> object calling this method.
     * @param imageIndex the index of the image being read within its
     * containing input file or stream.
     */
    public void imageStarted(final ImageReader source, final int imageIndex) {}
    
    /** 
     * Reports that a read has been aborted via the reader's
     * <code>abort</code> method.  No further notifications will be
     * given.
     *
     * @param source the <code>ImageReader</code> object calling this
     * method.
     */
    public void readAborted(final ImageReader source) {}
    
    /** 
     * Reports that a sequence of read operationshas completed.
     * <code>ImageReader</code> implementations are required to call
     * this method exactly once from their
     * <code>readAll(Iterator)</code> method.
     *
     * @param source the <code>ImageReader</code> object calling this method.
     */
    public void sequenceComplete(final ImageReader source) {}
    
    /** 
     * Reports that a sequence of read operations is beginning.
     * <code>ImageReader</code> implementations are required to call
     * this method exactly once from their
     * <code>readAll(Iterator)</code> method.
     *
     * @param source the <code>ImageReader</code> object calling this method.
     * @param minIndex the index of the first image to be read.
     */
    public void sequenceStarted(final ImageReader source, final int minIndex) {}
    
    /** 
     * Reports that a thumbnail read operation has completed.  All
     * <code>ImageReader</code> implementations are required to call
     * this method exactly once upon completion of each thumbnail read
     * operation.
     *
     * @param source the <code>ImageReader</code> object calling this
     * method.
     */
    public void thumbnailComplete(final ImageReader source) {}
    
    /** 
     * Reports the approximate degree of completion of the current
     * <code>getThumbnail</code> call within the associated
     * <code>ImageReader</code>.  The semantics are identical to those
     * of <code>imageProgress</code>.
     *
     * @param source the <code>ImageReader</code> object calling this method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    public void thumbnailProgress(final ImageReader source, final float percentageDone) {}
    
    /** 
     * Reports that a thumbnail read operation is beginning.  All
     * <code>ImageReader</code> implementations are required to call
     * this method exactly once when beginning a thumbnail read
     * operation.
     *
     * @param source the <code>ImageReader</code> object calling this method.
     * @param imageIndex the index of the image being read within its
     * containing input file or stream.
     * @param thumbnailIndex the index of the thumbnail being read.
     */
    public void thumbnailStarted(final ImageReader source, final int imageIndex, final int thumbnailIndex) {}    
    
    /** 
     * Reports that the image write operation has completed.  All
     * <code>ImageWriter</code> implementations are required to call
     * this method exactly once upon completion of each image write
     * operation.
     *
     * @param source the <code>ImageWriter</code> object calling this method.
     */
    public void imageComplete(final ImageWriter source) {}
    
    /** 
     * Reports the approximate degree of completion of the current
     * <code>write</code> call within the associated
     * <code>ImageWriter</code>.
     *
     * <p> The degree of completion is expressed as an index
     * indicating which image is being written, and a percentage
     * varying from <code>0.0F</code> to <code>100.0F</code>
     * indicating how much of the current image has been output.  The
     * percentage should ideally be calculated in terms of the
     * remaining time to completion, but it is usually more practical
     * to use a more well-defined metric such as pixels decoded or
     * portion of input stream consumed.  In any case, a sequence of
     * calls to this method during a given read operation should
     * supply a monotonically increasing sequence of percentage
     * values.  It is not necessary to supply the exact values
     * <code>0</code> and <code>100</code>, as these may be inferred
     * by the callee from other methods.
     *
     * <p> Each particular <code>ImageWriter</code> implementation may
     * call this method at whatever frequency it desires.  A rule of
     * thumb is to call it around each 5 percent mark.
     *
     * @param source the <code>ImageWriter</code> object calling this method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    public void imageProgress(final ImageWriter source, final float percentageDone) {
        progress.setValue(Math.round(percentageDone));
    }
    
    /** 
     * Reports that an image write operation is beginning.  All
     * <code>ImageWriter</code> implementations are required to call
     * this method exactly once when beginning an image write
     * operation.
     *
     * @param source the <code>ImageWriter</code> object calling this
     * method.
     * @param imageIndex the index of the image being written within
     * its containing input file or stream.
     */
    public void imageStarted(final ImageWriter source, final int imageIndex) {}
    
    /** 
     * Reports that a thumbnail write operation has completed.  All
     * <code>ImageWriter</code> implementations are required to call
     * this method exactly once upon completion of each thumbnail
     * write operation.
     *
     * @param source the <code>ImageWriter</code> object calling this
     * method.
     */
    public void thumbnailComplete(final ImageWriter source) {}
    
    /** 
     * Reports the approximate degree of completion of the current
     * thumbnail write within the associated <code>ImageWriter</code>.
     * The semantics are identical to those of
     * <code>imageProgress</code>.
     *
     * @param source the <code>ImageWriter</code> object calling this
     * method.
     * @param percentageDone the approximate percentage of decoding that
     * has been completed.
     */
    public void thumbnailProgress(final ImageWriter source, final float percentageDone) {}
    
    /** 
     * Reports that a thumbnail write operation is beginning.  All
     * <code>ImageWriter</code> implementations are required to call
     * this method exactly once when beginning a thumbnail write
     * operation.
     *
     * @param source the <code>ImageWrite</code> object calling this method.
     * @param imageIndex the index of the image being written within its
     * containing input file or stream.
     * @param thumbnailIndex the index of the thumbnail being written.
     */
    public void thumbnailStarted(final ImageWriter source, final int imageIndex, final int thumbnailIndex) {}
    
    /** 
     * Reports that a write has been aborted via the writer's
     * <code>abort</code> method.  No further notifications will be
     * given.
     *
     * @param source the <code>ImageWriter</code> object calling this
     * method.
     */
    public void writeAborted(final ImageWriter source) {}    
}