/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.seasview.layer.control;

// AWT dependencies
import java.awt.Component;

// JAI dependencies
import javax.media.jai.KernelJAI;

// Geotools dependencies
import org.geotools.resources.SwingUtilities;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.gui.swing.GradientKernelEditor;


/**
 * Classe de base des objets qui sont capable de transformer une image g�or�f�renc�e.
 * Une transformation peut consister par exemple � supprimer le grillage d'une image,
 * ou � appliquer une convolution. Certaines op�rations peuvent changer les unit�s du
 * param�tre g�ophysique.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class GradientMagnitudeOperation extends ProcessorOperation implements Configurable {
    /**
     * L'�diteur.
     */
    private transient GradientKernelEditor editor;

    /**
     * The horizontal and vertical kernels
     */
    private KernelJAI mask1 = KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL,
                      mask2 = KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL;

    /**
     * Construit une op�ration sans param�tres.
     *
     * @param processor   Le processeur.
     * @param operation   Le nom de l'op�ration.
     * @param description Une description � faire appara�tre dans le menu.
     */
    public GradientMagnitudeOperation(final GridCoverageProcessor processor,
                                      final String                operation,
                                      final String              description)
    {
        super(processor, operation, description);
    }

    /**
     * Fait appara�tre le contr�leur.
     */
    public void showControler(final Component owner) {
        if (editor == null) {
            editor = new GradientKernelEditor();
            editor.addDefaultKernels();
        }
        if (SwingUtilities.showOptionDialog(owner, editor, "Magnitude du gradient")) {
            final KernelJAI m1 = editor.getHorizontalEditor().getKernel();
            final KernelJAI m2 = editor.getVerticalEditor()  .getKernel();
            if (!m1.equals(mask1) || !m2.equals(mask2)) {
                parameters.setParameter("mask1", mask1=m1);
                parameters.setParameter("mask2", mask2=m2);
                clearCache();
            }
        } else {
            editor.getHorizontalEditor().setKernel(mask1);
            editor.getVerticalEditor()  .setKernel(mask2);
        }
    }
}
