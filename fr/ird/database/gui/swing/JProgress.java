/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.database.gui.swing;

// Swing
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JProgressBar;

// AWT
import java.awt.Frame;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// Image I/O
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOWriteProgressListener;


/**
 * Boite de dialogue permettant de visualiser la progression d'un traitement.
 *
 * @author Remi Eve
 * @version $Id$
 */
final class JProgress extends JDialog implements IIOReadProgressListener,
                                                 IIOWriteProgressListener
{   
    /**
     * Barre de progression.
     */
    private final JProgressBar progress = new JProgressBar();

    /**
     * Construit la boîte de dialogue.
     */
    public JProgress(final Frame parent, final boolean modal) {
        super (parent, modal);
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        progress.setIndeterminate(true);
        panel.add(progress);
        getContentPane().add(panel);       
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }    

    /**
     * Set the progress bar status to undeterminated.
     *
     * @param state <code>true</code> for underterminated status, <code>false</code> otherwise.
     */
    public void setIndeterminate(final boolean state) {
        progress.setValue(0);
        progress.setIndeterminate(state);
    }

    /**
     * Set the progress bar to the specified percentage.
     */
    private void setProgress(final float percentageDone) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                progress.setValue(Math.round(percentageDone));
            }
        });
    }
    
    /** 
     * Reports that a sequence of read operations is beginning.
     */
    public void sequenceStarted(final ImageReader source, final int minIndex) {
    }

    /** 
     * Reports that a thumbnail read operation is beginning.
     */
    public void thumbnailStarted(final ImageReader source, final int imageIndex, final int thumbnailIndex) {
    }
    
    /** 
     * Reports that a thumbnail write operation is beginning.
     */
    public void thumbnailStarted(final ImageWriter source, final int imageIndex, final int thumbnailIndex) {
    }
    
    /** 
     * Reports that an image read operation is beginning.
     */
    public void imageStarted(final ImageReader source, final int imageIndex) {
    }
    
    /** 
     * Reports that an image write operation is beginning.
     */
    public void imageStarted(final ImageWriter source, final int imageIndex) {
    }
    
    /** 
     * Reports the approximate degree of completion of the current
     * <code>getThumbnail</code> call within the associated
     * <code>ImageReader</code>.
     */
    public void thumbnailProgress(final ImageReader source, final float percentageDone) {
        setProgress(percentageDone);
    }
    
    /** 
     * Reports the approximate degree of completion of the current
     * thumbnail write within the associated <code>ImageWriter</code>.
     */
    public void thumbnailProgress(final ImageWriter source, final float percentageDone) {
        setProgress(percentageDone);
    }
    
    /** 
     * Reports the approximate degree of completion of the current
     * <code>read</code> call of the associated <code>ImageReader</code>.
     */
    public void imageProgress(final ImageReader source, final float percentageDone)  {
        setProgress(percentageDone);
    }
    
    /** 
     * Reports the approximate degree of completion of the current
     * <code>write</code> call within the associated <code>ImageWriter</code>.
     */
    public void imageProgress(final ImageWriter source, final float percentageDone) {
        setProgress(percentageDone);
    }
    
    /** 
     * Reports that a thumbnail read operation has completed.
     */
    public void thumbnailComplete(final ImageReader source) {
    }
    
    /** 
     * Reports that a thumbnail write operation has completed.
     */
    public void thumbnailComplete(final ImageWriter source) {
    }

    /** 
     * Reports that the current image read operation has completed.
     */
    public void imageComplete(final ImageReader source) {
    }
    
    /** 
     * Reports that the image write operation has completed.
     */
    public void imageComplete(final ImageWriter source) {
    }
    
    /** 
     * Reports that a sequence of read operationshas completed.
     */
    public void sequenceComplete(final ImageReader source) {
    }
    
    /** 
     * Reports that a read has been aborted via the reader's <code>abort</code> method.
     */
    public void readAborted(final ImageReader source) {
    }
    
    /** 
     * Reports that a write has been aborted via the writer's
     * <code>abort</code> method.
     */
    public void writeAborted(final ImageWriter source) {
    }
}
