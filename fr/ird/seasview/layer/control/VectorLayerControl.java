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

// DataBase
import java.io.IOException;
import java.sql.SQLException;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;

// Images
import org.geotools.gc.GridCoverage;
import java.awt.image.RenderedImage;

// Graphical user interface
import java.awt.Color;
import java.awt.Paint;
import javax.swing.JComponent;
import javax.swing.event.EventListenerList;
import fr.ird.awt.GridMarkControler;

// Map components
import org.geotools.renderer.j2d.RenderedLayer;
import fr.ird.seasview.layer.VectorLayer;

// Divers
import java.util.Date;
import java.util.List;
import org.geotools.ct.TransformException;
import org.geotools.renderer.geom.Arrow2D;


/**
 * Controleur des couches des vecteurs de courants.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class VectorLayerControl extends LayerControl {
    /**
     * Données de courants.
     */
    private final ImageTable table;

    /**
     * Bandes des composantes U et V des courants.
     */
    private final int bandU, bandV;

    /**
     * Composante qui permet à l'utilisateur
     * de configurer cette couche.
     */
    private GridMarkControler controler;

    /**
     * Construit une couche des courants.
     *
     * @param table Table des images contenant les données vectorielles.
     * @param bandU Bande de la composante U des courants.
     * @param bandV Bande de la composante V des courants.
     */
    public VectorLayerControl(final ImageTable table, final int bandU, final int bandV) {
        super(false);
        this.table = table;
        this.bandU = bandU;
        this.bandV = bandV;
    }

    /**
     * Retourne le nom de cette couche.
     */
    public String getName() {
        return "Courants géostrophiques"; // TODO
        // return table.getSeries().getName();
    }

    /**
     * Configure une couche en fonction de cet objet <code>LayerControl</code>.
     * Si l'argument <code>layer</code> est nul, alors cette méthode retourne
     * une nouvelle couche proprement configurée. Cette méthode peut être
     * appelée de n'importe quel thread (généralement pas celui de <i>Swing</i>).
     *
     * @param  layer Couche à configurer. Si non-nul, alors cette couche doit
     *         avoir été créé précédemment par ce même objet <code>LayerControl</code>.
     * @param  entry Image à afficher. Il s'agit d'une image sélectionnée par
     *         l'utilisateur dans la liste déroulante qui apparaît à gauche de
     *         la mosaïque d'images.
     * @param  listeners Objets à informer des progrès d'une éventuelle lecture.
     * @return Une couche proprement configurée, ou <code>null</code> si la configuration
     *         se traduirait à toute fin pratique par la disparition de la couche.
     * @throws SQLException si les accès à la base de données ont échoués.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     * @throws TransformException si une transformation était nécessaire et a échouée.
     */
    public RenderedLayer[] configLayers(final RenderedLayer[]   layers,
                                              ImageEntry        entry,
                                        final EventListenerList listeners)
        throws SQLException, IOException, TransformException
    {
        if (!table.getSeries().equals(entry.getSeries())) {
            table.setTimeRange(entry.getTimeRange());
            entry = table.getEntry();
            if (entry == null) {
                return null;
            }
        }
        final GridCoverage coverage = entry.getGridCoverage(listeners);
        final VectorLayer layer;
        if (layers!=null && layers.length==1 && layers[0] instanceof VectorLayer) {
            layer = (VectorLayer) layers[0];
            layer.setGridCoverage(coverage);
            layer.setBands(new int[]{bandU, bandV});
        } else {
            layer = new VectorLayer(coverage, bandU, bandV);
        }
        synchronized(this) {
            if (controler != null) {
                layer.setColor(controler.getColor());
                final int decimation = controler.getDecimation();
                if (decimation != 0) {
                    layer.setDecimation(decimation, decimation);
                } else {
                    layer.setAutoDecimation(16,16);
                }
            }
        }
        return new RenderedLayer[] {layer};
    }

    /**
     * Fait apparaître un paneau de configuration pour cette couche. Cette
     * méthode est responsable d'appeler {@link #fireStateChanged} si l'état
     * de cette couche a changé suite aux interventions de l'utilisateur.
     */
    protected void showControler(final JComponent owner) {
        final Color oldColor;
        final Color newColor;
        final int   oldDecimation;
        final int   newDecimation;
        synchronized(this) {
            if (controler == null) {
                controler = new GridMarkControler(getName());
                controler.setShape(new Arrow2D(-24, -20, 48, 40));
                controler.setColor(new Color(0, 153, 255, 128));
            }
            if (false) {// TODO
                final RenderedImage sample = null;
                controler.setBackground(sample);
            }
            oldColor      = controler.getColor();
            oldDecimation = controler.getDecimation();
        }
        final boolean changed=controler.showDialog(owner);
        synchronized(this) {
            controler.setBackground((RenderedImage) null);
            if (changed) {
                newColor      = controler.getColor();
                newDecimation = controler.getDecimation();
                fireStateChanged(new Edit() {
                    protected void edit(final boolean redo) {
                        controler.setColor     (redo ? newColor      : oldColor);
                        controler.setDecimation(redo ? newDecimation : oldDecimation);
                    }
                });
            }
        }
    }
}
