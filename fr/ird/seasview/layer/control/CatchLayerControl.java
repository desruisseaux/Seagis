/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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

// Bases de données
import java.io.IOException;
import java.sql.SQLException;
import fr.ird.animat.Species;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;

// Cartographie
import fr.ird.awt.SpeciesChooser;
import fr.ird.seasview.layer.CatchLayer;
import fr.ird.seasview.layer.CatchTableLayer;
import org.geotools.renderer.j2d.RenderedLayer;

// Interface utilisateur
import java.awt.Color;
import javax.swing.JComponent;

// Divers
import java.util.Set;
import java.util.Date;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import javax.swing.event.EventListenerList;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.ExceptionMonitor;

// Resources
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Contrôleur d'une couche {@link CatchLayer}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class CatchLayerControl extends LayerControl {
    /**
     * Connexion vers la base des données de pêches. Cette connexion
     * n'est utilisée que pour construire {@link SpeciesChooser} la
     * première fois où il sera nécessaire.
     */
    private final FisheryDataBase database;

    /**
     * Connexion vers la table des données de pêches. Cette
     * connexion ne sera construite que la première fois où
     * elle sera nécessaire.
     */
    private CatchTable catchTable;

    /**
     * Paneau de configuration qui permet de sélectionner des espèces
     * et de les paramétrer. Ce paneau sera construit la première fois
     * où il sera demandé et conservé par la suite.
     */
    private SpeciesChooser controler;

    /**
     * The display type for marks. May be one of the following constants:
     * {@link CatchLayer#POSITIONS_ONLY}, {@link CatchLayer#GEAR_COVERAGES}
     * or {@link CatchLayer#CATCH_AMOUNTS}.
     */
    private int markType = CatchLayer.GEAR_COVERAGES;

    /**
     * Construit une couche des pêches.
     *
     * @param  database Connexion vers la base de données des pêches.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public CatchLayerControl(final FisheryDataBase database) throws SQLException {
        super(false);
        this.database = database;
    }

    /**
     * Retourne le nom de cette couche.
     */
    public String getName() {
        return Resources.format(ResourceKeys.CATCHS);
    }

    /**
     * Configure des couches en fonction de cet objet <code>LayerControl</code>.
     * Si l'argument <code>layers</code> est nul, alors cette méthode retourne
     * de nouvelles couches proprement configurées. Cette méthode peut être
     * appelée de n'importe quel thread (généralement pas celui de <i>Swing</i>).
     *
     * @param  layers Couche à configurer.
     * @param  listeners Objets à informer des progrès d'une éventuelle lecture.
     * @return Des couches proprement configurées, ou <code>null</code> si la configuration
     *         se traduirait à toute fin pratique par la disparition des couches (par
     *         exemple parce qu'aucune espèce n'a été sélectionnée).
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public synchronized RenderedLayer[] configLayers(final RenderedLayer[] layers,
                                                     final ImageEntry entry,
                                                     final EventListenerList listeners)
        throws SQLException
    {
        final CatchTableLayer layer;
        if (layers!=null && layers.length==1 && layers[0] instanceof CatchTableLayer) {
            layer = (CatchTableLayer) layers[0];
            layer.setTimeRange(entry.getTimeRange());
        } else {
            if (catchTable == null) {
                catchTable = database.getCatchTable();
            }
            catchTable.setTimeRange(entry.getTimeRange());
            layer = new CatchTableLayer(catchTable);
        }
        if (controler != null) {
            layer.defineIcons(controler.getIcons());
        }
        layer.setMarkType(markType);
        return new RenderedLayer[] {layer};
    }

    /**
     * Fait apparaître un paneau de configuration pour cette couche.
     */
    protected void showControler(final JComponent owner) {
        try {
            synchronized(this) {
                if (controler == null) {
                    controler = new SpeciesChooser(database);
                }
            }
            if (controler.showDialog(owner)) {
                synchronized(this) {
                    if (catchTable == null) {
                        catchTable = database.getCatchTable();
                    }
                    final Species.Icon[] icons = controler.getSelectedIcons();
                    final Set<Species> oldSpecies = catchTable.getSpecies();
                    final Set<Species> newSpecies;  // Will be set later
                    final Set<Species> tmpSpecies;
                    final int oldMarkType = markType;
                    final int newMarkType;
                    if (controler.isCatchAmountSelected()) {
                        newMarkType = CatchLayer.CATCH_AMOUNTS;
                        tmpSpecies = new LinkedHashSet<Species>(2*icons.length);
                        for (int i=0; i<icons.length; i++) {
                            tmpSpecies.add(icons[i].getSpecies());
                        }
                    } else {
                        newMarkType = CatchLayer.GEAR_COVERAGES;
                        tmpSpecies = database.getSpecies();
                    }
                    catchTable.setSpecies(tmpSpecies);
                    newSpecies = catchTable.getSpecies(); // Get a more compact and immutable view.
                    this.markType = newMarkType;
                    fireStateChanged(new Edit() {
                        protected void edit(final boolean redo) {
                            try {
                                catchTable.setSpecies(redo ? newSpecies  : oldSpecies);
                                markType =            redo ? newMarkType : oldMarkType;
                            } catch (SQLException exception) {
                                ExceptionMonitor.show(owner, exception);
                            }
                        }
                    });
                }
            }
        } catch (SQLException exception) {
            ExceptionMonitor.show(owner, exception);
        }
    }

    /**
     * Libère les ressources utilisées par cette couche.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public synchronized void dispose() throws SQLException {
        if (catchTable != null) {
            catchTable.close();
            catchTable = null;
            // Don't close 'database', since we don't own it.
        }
    }
}
