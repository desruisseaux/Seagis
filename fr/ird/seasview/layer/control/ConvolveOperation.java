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
import org.geotools.gui.swing.KernelEditor;


/**
 * Op�ration "Convolve".
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ConvolveOperation extends ProcessorOperation implements Configurable {
    /**
     * L'�diteur.
     */
    private transient KernelEditor editor;

    /**
     * The kernel.
     */
    private KernelJAI kernel = KernelJAI.ERROR_FILTER_STUCKI;

    /**
     * Construit une op�ration sans param�tres.
     *
     * @param processor   Le processeur.
     * @param operation   Le nom de l'op�ration.
     * @param description Une description � faire appara�tre dans le menu.
     */
    public ConvolveOperation(final GridCoverageProcessor processor, final String operation, final String description)
    {
        super(processor, operation, description);
        parameters.setParameter("kernel", kernel);
    }

    /**
     * Fait appara�tre le contr�leur.
     */
    public void showControler(final Component owner) {
        if (editor == null) {
            editor = new KernelEditor();
            editor.addDefaultKernels();
            editor.setKernel(kernel);
        }
        if (SwingUtilities.showOptionDialog(owner, editor, "Magnitude du gradient")) {
            final KernelJAI k = editor.getKernel();
            if (!kernel.equals(k)) {
                parameters.setParameter("kernel", kernel=k);
                clearCache();
            }
        } else {
            editor.setKernel(kernel);
        }
    }
}
