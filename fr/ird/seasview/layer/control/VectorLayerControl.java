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

// J2SE
import java.util.Date;
import java.util.List;
import java.awt.Color;
import java.awt.Paint;
import java.awt.image.RenderedImage;
import javax.swing.event.EventListenerList;
import javax.swing.JComponent;
import java.sql.SQLException;
import java.io.IOException;

// Geotools
import org.geotools.gc.GridCoverage;
import org.geotools.ct.TransformException;
import org.geotools.renderer.geom.Arrow2D;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedGridMarks;

// Seagis
import fr.ird.awt.GridMarkControler;
import fr.ird.seasview.DataBase;
import fr.ird.seasview.layer.VectorLayer;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageEntry;


/**
 * Controleur des couches des vecteurs de courants.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class VectorLayerControl extends LayerControl {
    /**
     * Donn�es de courants.
     */
    private final CoverageTable table;

    /**
     * Bandes des composantes U et V des courants.
     */
    private final int bandU, bandV;

    /**
     * Composante qui permet � l'utilisateur
     * de configurer cette couche.
     */
    private GridMarkControler controler;

    /**
     * Construit une couche des courants.
     *
     * @param table Table des images contenant les donn�es vectorielles.
     * @param bandU Bande de la composante U des courants.
     * @param bandV Bande de la composante V des courants.
     */
    public VectorLayerControl(final CoverageTable table, final int bandU, final int bandV) {
        super(false);
        this.table = table;
        this.bandU = bandU;
        this.bandV = bandV;
    }

    /**
     * Retourne le nom de cette couche.
     */
    public String getName() {
        return "Courants g�ostrophiques"; // TODO
        // return table.getSeries().getName();
    }

    /**
     * Configure une couche en fonction de cet objet <code>LayerControl</code>.
     * Si l'argument <code>layer</code> est nul, alors cette m�thode retourne
     * une nouvelle couche proprement configur�e. Cette m�thode peut �tre
     * appel�e de n'importe quel thread (g�n�ralement pas celui de <i>Swing</i>).
     *
     * @param  layer Couche � configurer. Si non-nul, alors cette couche doit
     *         avoir �t� cr�� pr�c�demment par ce m�me objet <code>LayerControl</code>.
     * @param  entry Image � afficher. Il s'agit d'une image s�lectionn�e par
     *         l'utilisateur dans la liste d�roulante qui appara�t � gauche de
     *         la mosa�que d'images.
     * @param  listeners Objets � informer des progr�s d'une �ventuelle lecture.
     * @return Une couche proprement configur�e, ou <code>null</code> si la configuration
     *         se traduirait � toute fin pratique par la disparition de la couche.
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     * @throws IOException si une erreur d'entr�/sortie est survenue.
     * @throws TransformException si une transformation �tait n�cessaire et a �chou�e.
     */
    public RenderedLayer[] configLayers(final RenderedLayer[]   layers,
                                              CoverageEntry     entry,
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
            layer.setBands(new int[]{bandU, bandV});
            layer.setGridCoverage(coverage);
        } else {
            layer = new VectorLayer(coverage, bandU, bandV);
            setDecimation(layer, DataBase.preferences.getInt("vectors.decimation", 0));
        }
        synchronized(this) {
            if (controler != null) {
                layer.setMarkPaint(controler.getColor());
                setDecimation(layer, controler.getDecimation());
            }
        }
        return new RenderedLayer[] {layer};
    }

    /**
     * Applique une d�cimation la couche sp�cifi�e. Par convention, une d�cimation de 0
     * signifie qu'il faut laisser l'application la calculer elle-m�me.
     */
    private static void setDecimation(final RenderedGridMarks layer, final int decimation) {
        if (decimation != 0) {
            layer.setDecimation(decimation, decimation);
        } else {
            layer.setAutoDecimation(16,16);
        }
    }

    /**
     * Fait appara�tre un paneau de configuration pour cette couche. Cette
     * m�thode est responsable d'appeler {@link #fireStateChanged} si l'�tat
     * de cette couche a chang� suite aux interventions de l'utilisateur.
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
                controler.setDecimation(DataBase.preferences.getInt("vectors.decimation", 0));
            }
            if (false) {// TODO
                final RenderedImage sample = null;
                controler.setBackground(sample);
            }
            oldColor      = controler.getColor();
            oldDecimation = controler.getDecimation();
        }
        final boolean changed = controler.showDialog(owner);
        synchronized(this) {
            controler.setBackground((RenderedImage) null);
            if (changed) {
                newColor      = controler.getColor();
                newDecimation = controler.getDecimation();
                DataBase.preferences.putInt("vectors.decimation", newDecimation);
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