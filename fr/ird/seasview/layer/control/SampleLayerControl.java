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

// J2SE
import java.util.Set;
import java.util.Date;
import java.util.Arrays;
import java.util.LinkedHashSet;
import javax.swing.event.EventListenerList;
import javax.swing.JComponent;
import java.sql.SQLException;
import java.io.IOException;
import java.awt.Color;

// Geotools
import org.geotools.resources.Utilities;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis
import fr.ird.animat.Species;
import fr.ird.awt.SpeciesChooser;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.sample.SampleTable;
import fr.ird.database.sample.SampleDataBase;
import fr.ird.database.gui.map.SampleLayer;
import fr.ird.database.gui.map.SampleTableLayer;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Contr�leur d'une couche {@link SampleLayer}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class SampleLayerControl extends LayerControl {
    /**
     * Connexion vers la base des donn�es de p�ches. Cette connexion
     * n'est utilis�e que pour construire {@link SpeciesChooser} la
     * premi�re fois o� il sera n�cessaire.
     */
    private final SampleDataBase database;

    /**
     * Connexion vers la table des donn�es de p�ches. Cette
     * connexion ne sera construite que la premi�re fois o�
     * elle sera n�cessaire.
     */
    private SampleTable sampleTable;

    /**
     * Paneau de configuration qui permet de s�lectionner des esp�ces
     * et de les param�trer. Ce paneau sera construit la premi�re fois
     * o� il sera demand� et conserv� par la suite.
     */
    private SpeciesChooser controler;

    /**
     * The display type for marks. May be one of the following constants:
     * {@link SampleLayer#POSITIONS_ONLY}, {@link SampleLayer#GEAR_COVERAGES}
     * or {@link SampleLayer#CATCH_AMOUNTS}.
     */
    private int markType = SampleLayer.GEAR_COVERAGES;

    /**
     * Construit une couche des p�ches.
     *
     * @param  database Connexion vers la base de donn�es des p�ches.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public SampleLayerControl(final SampleDataBase database) throws SQLException {
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
    public synchronized RenderedLayer[] configLayers(final RenderedLayer[]   layers,
                                                     final CoverageEntry     entry,
                                                     final EventListenerList listeners)
        throws SQLException
    {
        final SampleTableLayer layer;
        if (layers!=null && layers.length==1 && layers[0] instanceof SampleTableLayer) {
            layer = (SampleTableLayer) layers[0];
            layer.setTimeRange(entry.getTimeRange());
        } else {
            if (sampleTable == null) {
                sampleTable = database.getSampleTable();
            }
            sampleTable.setTimeRange(entry.getTimeRange());
            layer = new SampleTableLayer(sampleTable);
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
                    if (sampleTable == null) {
                        sampleTable = database.getSampleTable();
                    }
                    final Species.Icon[] icons = controler.getSelectedIcons();
                    final Set<Species> oldSpecies = sampleTable.getSpecies();
                    final Set<Species> newSpecies;  // Will be set later
                    final Set<Species> tmpSpecies;
                    final int oldMarkType = markType;
                    final int newMarkType;
                    if (controler.isCatchAmountSelected()) {
                        newMarkType = SampleLayer.CATCH_AMOUNTS;
                        tmpSpecies = new LinkedHashSet<Species>(2*icons.length);
                        for (int i=0; i<icons.length; i++) {
                            tmpSpecies.add(icons[i].getSpecies());
                        }
                    } else {
                        newMarkType = SampleLayer.GEAR_COVERAGES;
                        tmpSpecies = database.getSpecies();
                    }
                    sampleTable.setSpecies(tmpSpecies);
                    newSpecies = sampleTable.getSpecies(); // Get a more compact and immutable view.
                    this.markType = newMarkType;
                    fireStateChanged(new Edit() {
                        protected void edit(final boolean redo) {
                            try {
                                sampleTable.setSpecies(redo ? newSpecies  : oldSpecies);
                                markType =             redo ? newMarkType : oldMarkType;
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
        if (sampleTable != null) {
            sampleTable.close();
            sampleTable = null;
            // Don't close 'database', since we don't own it.
        }
    }
}
