/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le D�veloppement
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

// Bases de donn�es
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
 * Contr�leur d'une couche {@link CatchLayer}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class CatchLayerControl extends LayerControl {
    /**
     * Connexion vers la base des donn�es de p�ches. Cette connexion
     * n'est utilis�e que pour construire {@link SpeciesChooser} la
     * premi�re fois o� il sera n�cessaire.
     */
    private final FisheryDataBase database;

    /**
     * Connexion vers la table des donn�es de p�ches. Cette
     * connexion ne sera construite que la premi�re fois o�
     * elle sera n�cessaire.
     */
    private CatchTable catchTable;

    /**
     * Paneau de configuration qui permet de s�lectionner des esp�ces
     * et de les param�trer. Ce paneau sera construit la premi�re fois
     * o� il sera demand� et conserv� par la suite.
     */
    private SpeciesChooser controler;

    /**
     * The display type for marks. May be one of the following constants:
     * {@link CatchLayer#POSITIONS_ONLY}, {@link CatchLayer#GEAR_COVERAGES}
     * or {@link CatchLayer#CATCH_AMOUNTS}.
     */
    private int markType = CatchLayer.GEAR_COVERAGES;

    /**
     * Construit une couche des p�ches.
     *
     * @param  database Connexion vers la base de donn�es des p�ches.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
     * Si l'argument <code>layers</code> est nul, alors cette m�thode retourne
     * de nouvelles couches proprement configur�es. Cette m�thode peut �tre
     * appel�e de n'importe quel thread (g�n�ralement pas celui de <i>Swing</i>).
     *
     * @param  layers Couche � configurer.
     * @param  listeners Objets � informer des progr�s d'une �ventuelle lecture.
     * @return Des couches proprement configur�es, ou <code>null</code> si la configuration
     *         se traduirait � toute fin pratique par la disparition des couches (par
     *         exemple parce qu'aucune esp�ce n'a �t� s�lectionn�e).
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
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
     * Fait appara�tre un paneau de configuration pour cette couche.
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
     * Lib�re les ressources utilis�es par cette couche.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public synchronized void dispose() throws SQLException {
        if (catchTable != null) {
            catchTable.close();
            catchTable = null;
            // Don't close 'database', since we don't own it.
        }
    }
}
