/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.seasview.layer.control;

// Geotools dependencies
import org.geotools.gc.GridCoverage;

// Databases and images
import java.io.IOException;
import java.sql.SQLException;
import fr.ird.sql.image.ImageEntry;

// Map components
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import fr.ird.operator.coverage.Operation;

// Graphical user interface
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.AbstractButton;
import javax.swing.event.EventListenerList;

// Miscellaneous
import java.util.Date;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Couche contenant une image. Certaines opérations pourront être
 * appliquées sur les images, comme par exemple une convolution.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class ImageLayerControl extends LayerControl {
    /**
     * Objet à utiliser pour configurer l'affichage des images.
     */
    private transient ImageControlPanel controler;

    /**
     * Construit une couche des images.
     */
    public ImageLayerControl() {
        super(true);
    }

    /**
     * Retourne le nom de cette couche.
     */
    public String getName() {
        return Resources.format(ResourceKeys.IMAGES);
    }

    /**
     * Retourne une couche appropriée pour l'image spécifiée. Cette méthode peut être
     * appelée de n'importe quel Thread, généralement pas celui de <i>Swing</i>.
     *
     * @param  layers Couches à configurer. Si non-nul, alors ces couches doivent
     *         avoir été créées précédemment par ce même objet <code>LayerControl</code>.
     * @param  entry Image à afficher. Il s'agit d'une image sélectionnée par
     *         l'utilisateur dans la liste déroulante qui apparaît à gauche de
     *         la mosaïque d'images.
     * @param  listeners Objets à informer des progrès d'une éventuelle lecture.
     * @return Une couche proprement configurée, ou <code>null</code> si la configuration
     *         se traduirait à toute fin pratique par la disparition de la couche.
     * @throws SQLException si les accès à la base de données ont échoués.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     */
    public RenderedLayer[] configLayers(final RenderedLayer[]   layers,
                                        final ImageEntry        entry,
                                        final EventListenerList listeners)
        throws SQLException, IOException
    {
        GridCoverage coverage = entry.getGridCoverage(listeners);
        if (coverage == null) {
            return null;
        }
        synchronized(this) {
            if (controler != null) {
                final Operation operation = controler.getSelectedOperation();
                if (operation != null) {
                    coverage = operation.filter(coverage);
                }
            }
        }
        final RenderedGridCoverage layer = new RenderedGridCoverage(coverage);
        layer.setZOrder(Float.NEGATIVE_INFINITY);
        return new RenderedLayer[] {layer};
    }

    /**
     * Fait apparaître un paneau de configuration pour cette couche. Cette
     * méthode est responsable d'appeler {@link #fireStateChanged} si l'état
     * de cette couche a changé suite aux interventions de l'utilisateur.
     */
    protected void showControler(final JComponent owner) {
        final Operation oldOperation;
        final Operation newOperation;
        synchronized (this) {
            if (controler == null) {
                controler = new ImageControlPanel();
                controler.addDefaultOperations();
            }
            oldOperation = controler.getSelectedOperation();
        }
        if (controler.showDialog(owner)) {
            synchronized(this) {
                newOperation = controler.getSelectedOperation();
                fireStateChanged(new Edit() {
                    protected void edit(final boolean redo) {
                        controler.setSelectedOperation(redo ? newOperation : oldOperation);
                    }
                });
            }
        }
    }
}
