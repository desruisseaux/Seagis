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
 * Couche contenant une image. Certaines op�rations pourront �tre
 * appliqu�es sur les images, comme par exemple une convolution.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class ImageLayerControl extends LayerControl {
    /**
     * Objet � utiliser pour configurer l'affichage des images.
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
     * Retourne une couche appropri�e pour l'image sp�cifi�e. Cette m�thode peut �tre
     * appel�e de n'importe quel Thread, g�n�ralement pas celui de <i>Swing</i>.
     *
     * @param  layers Couches � configurer. Si non-nul, alors ces couches doivent
     *         avoir �t� cr��es pr�c�demment par ce m�me objet <code>LayerControl</code>.
     * @param  entry Image � afficher. Il s'agit d'une image s�lectionn�e par
     *         l'utilisateur dans la liste d�roulante qui appara�t � gauche de
     *         la mosa�que d'images.
     * @param  listeners Objets � informer des progr�s d'une �ventuelle lecture.
     * @return Une couche proprement configur�e, ou <code>null</code> si la configuration
     *         se traduirait � toute fin pratique par la disparition de la couche.
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     * @throws IOException si une erreur d'entr�/sortie est survenue.
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
     * Fait appara�tre un paneau de configuration pour cette couche. Cette
     * m�thode est responsable d'appeler {@link #fireStateChanged} si l'�tat
     * de cette couche a chang� suite aux interventions de l'utilisateur.
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
