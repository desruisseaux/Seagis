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

// J2SE
import java.util.Date;
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.event.EventListenerList;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;

// Geotools
import org.geotools.gui.swing.ProgressWindow;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedGeometries;
import org.geotools.renderer.geom.GeometryCollection;

// Seagis
import fr.ird.seasview.DataBase;
import fr.ird.io.map.GEBCOFactory;
import fr.ird.io.map.IsolineFactory;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Couche contenant une bathymétrie.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class IsolineLayerControl extends LayerControl {
    /**
     * Factories for isolines.
     */
    private static final IsolineFactory[] FACTORIES;
    static {
        try {
            FACTORIES = new IsolineFactory[] {
                new GEBCOFactory(DataBase.MEDITERRANEAN_VERSION ? "Méditerranée"
                                                                : "Océan_Indien")
            };
        } catch (FileNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Couleur par défaut pour le remplissage des terres. La couleur RFB des images SST
     * est (210,200,160). Pour le remplissage, nous prennons une couleur légèrement plus
     * foncée.
     */
    private static final Color FOREGROUND = new Color(168,160,128);

    /**
     * The default set of selected values.
     */
    private static final float[] DEFAULT_VALUES = new float[1];

    /**
     * Objet à utiliser pour configurer l'affichage des images.
     */
    private transient IsolineControlPanel controler;

    /**
     * Construit une couche de la bathymétrie.
     */
    public IsolineLayerControl() {
        super(false);
    }

    /**
     * Retourne le nom de cette couche.
     */
    public String getName() {
        return Resources.format(ResourceKeys.BATHYMETRY);
    }

    /**
     * Retourne des couches appropriées pour l'image spécifié. Cette méthode peut être
     * appelée de n'importe quel Thread, généralement pas celui de <i>Swing</i>.
     *
     * @param  layers Couches à configurer. Si non-nul, alors ces couches doivent
     *         avoir été créé précédemment par ce même objet <code>LayerControl</code>.
     * @param  entry Image à afficher. Il s'agit d'une image sélectionnée par
     *         l'utilisateur dans la liste déroulante qui apparaît à gauche de
     *         la mosaïque d'images.
     * @param  listeners Objets à informer des progrès d'une éventuelle lecture.
     * @return Des couches proprement configurées, ou <code>null</code> si la configuration
     *         se traduirait à toute fin pratique par la disparition de la couche.
     * @throws SQLException si les accès à la base de données ont échoués.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     */
    public RenderedLayer[] configLayers(final RenderedLayer[]   layers,
                                        final CoverageEntry     entry,
                                        final EventListenerList listeners)
        throws RemoteException , IOException
    {
        final float[] values;
        synchronized (this) {
            if (controler != null) {
                values = controler.getSelectedValues();
            } else {
                values = DEFAULT_VALUES;
            }
        }
        IsolineFactory factory = FACTORIES[0]; // TODO: Select the right factory
        final ProgressWindow progress = new ProgressWindow(null);
        final GeometryCollection[] isolines;
        try {
            factory.setProgressListener(progress);
            isolines = factory.get(values);
        } finally {
            factory.setProgressListener(null);
            progress.dispose();
        }
        final RenderedGeometries[] isoLayers = new RenderedGeometries[isolines.length];
        for (int i=0; i<isoLayers.length; i++) {
            final RenderedGeometries isoLayer = new RenderedGeometries(isolines[i]);
            isoLayer.setContour   (Color.white);  // TODO: Set colors
            isoLayer.setForeground(FOREGROUND);
            isoLayers[i] = isoLayer;
        }
        return isoLayers;
    }

    /**
     * Fait apparaître un paneau de configuration pour cette couche. Cette
     * méthode est responsable d'appeler {@link #fireStateChanged} si l'état
     * de cette couche a changé suite aux interventions de l'utilisateur.
     */
    protected void showControler(final JComponent owner) {
        final Object oldContent;
        synchronized (this) {
            if (controler == null) {
                controler = new IsolineControlPanel();
                final ProgressWindow progress = new ProgressWindow(owner);
                try {
                    for (int i=0; i<FACTORIES.length; i++) {
                        final IsolineFactory factory = FACTORIES[i];
                        try {
                            factory.setProgressListener(progress);
                            controler.addValues(factory.getAvailableValues());
                        } catch (IOException exception) {
                            factory.setProgressListener(null);
                            ExceptionMonitor.show(owner, exception);
                        } finally {
                            factory.setProgressListener(null);
                        }
                    }
                } finally {
                    progress.dispose();
                }
                controler.setSelectedValues(DEFAULT_VALUES); // Must be last.
            }
            oldContent = controler.mark();
        }
        if (controler.showDialog(owner)) {
            synchronized(this) {
                final Object newContent = controler.mark();
                fireStateChanged(new Edit() {
                    protected void edit(final boolean redo) {
                        controler.reset(redo ? newContent : oldContent);
                    }
                });
            }
        }
    }
}
