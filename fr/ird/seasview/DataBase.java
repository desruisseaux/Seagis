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
package fr.ird.seasview;

// J2SE
import java.util.List;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.sql.SQLException;
import java.io.PrintWriter;

// Seagis
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.database.sample.SampleDataBase;

// Layers
import fr.ird.seasview.layer.control.LayerControl;
import fr.ird.seasview.layer.control.SampleLayerControl;
import fr.ird.seasview.layer.control.VectorLayerControl;
import fr.ird.seasview.layer.control.IsolineLayerControl;
import fr.ird.seasview.layer.control.CoverageLayerControl;

// Miscellaneous
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Source de données utilisées par l'ensemble de l'application.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class DataBase {
    /**
     * <code>true</code> pour compiler la version "Méditerranée" de l'application, or
     * <code>false</code> pour compiler la version "Océan Indien". Ce drapeau n'est
     * qu'un bricolage temporaire en attendant de trouver une solution plus générale.
     *
     * Ce drapeau est initialisé au moment du démarrage de l'application par {@link Main}.
     */
    public static boolean MEDITERRANEAN_VERSION = false;

    /**
     * Flot de sortie standard.
     */
    public static PrintWriter out = new PrintWriter(System.out); // Will be modified by 'Main'

    /**
     * Le journal de l'application.
     */
    public static final Logger logger = Logger.getLogger("fr.ird.seasview");

    /**
     * Les préférences de l'utilisateur pour l'application.
     */
    public static final Preferences preferences = Preferences.userNodeForPackage(DataBase.class);

    /**
     * Nom de la table des données de courants géostrophiques.
     */
    private static String getGeostrophicCurrentTable() {
        return MEDITERRANEAN_VERSION ? "SLA (Méditerranée - NRT)"
                                     : "SLA (Réunion)";
    }

    /**
     * Connexion vers la base de données d'images.
     * Si <code>null</code>, alors la connexion sera
     * établie la première fois où elle sera demandée.
     */
    private CoverageDataBase images;

    /**
     * Connexion vers la base de données de pêches.
     * Si <code>null</code>, alors la connexion sera
     * établie la première fois où elle sera demandée.
     */
    private SampleDataBase fisheries;

    /**
     * Groupe des threads qui sont en train d'effectuer une opération.
     * Il peut s'agir notamment d'exporter des données ou d'effectuer
     * un couplage entre des données de pêches et d'environnement. Les
     * applications qui accèdent aux bases de données en arrière-plan
     * sont encouragés à placer leurs threads dans ce groupe ou dans
     * un de ses sous-groupes.
     */
    final ThreadGroup threads=new ThreadGroup("SEAS workers");

    /**
     * Sous-groupe des threads ayant la charge de lire des images en arrière-plan.
     * <code>readers</code> est un sous-groupe de {@link #threads}, mais avec une
     * priorité légèrement plus élevée afin de fournir une réponse plus rapide à
     * l'utilisateur.
     */
    final ThreadGroup readers=new ThreadGroup(threads, "Image readers");

    /**
     * Sous-groupe des threads ayant la charge de construire une interface utilisateur.
     * <code>builder</code> est un sous-groupe de {@link #threads}, mais avec une
     * priorité plus élevée afin de fournir une réponse plus rapide à l'utilisateur.
     */
    final ThreadGroup builder=new ThreadGroup(threads, "GUI builders");

    /**
     * Construit une nouvelle source de données avec des
     * connections vers les bases de données spécifiées.
     */
    private DataBase(final CoverageDataBase images, final SampleDataBase fisheries) {
        this.images    = images;
        this.fisheries = fisheries;
        threads.setMaxPriority(Thread.NORM_PRIORITY-3);
        readers.setMaxPriority(Thread.NORM_PRIORITY-2);
        builder.setMaxPriority(Thread.NORM_PRIORITY-1);
    }

    /**
     * Construit une nouvelle source de données avec
     * des connections vers les bases de données par
     * défaut. Ces connections par défaut peuvent
     * avoir été enregistrées dans les préférences
     * de l'utilisateur.
     *
     * @throws SQLException si les connections avec les bases de données ont échouées.
     */
    public DataBase() throws SQLException {
        this(null, null);
    }

    /**
     * Construit une nouvelle source de données
     * qui tiendra une connexion vers les bases
     * de données du nom spécifié.
     *
     * @param images      Nom de la base de données d'images.   Ce sera typiquement <code>"PELOPS-Images"</code>.
     * @param imagesTZ    Fuseau horaire par défaut des dates dans les bases de données d'images.
     * @param fisheries   Nom de la base de données des pêches. Ce sera typiquement <code>"PELOPS-Pêches"</code>.
     * @param fisheriesTZ Fuseau horaire par défaut des dates dans les bases de données de pêches.
     * @throws SQLException si les connections avec les bases de données ont échouées.
     */
    public DataBase(final String images,    final TimeZone imagesTZ,
                    final String fisheries, final TimeZone fisheriesTZ) throws SQLException
    {
        this(new fr.ird.database.coverage.sql.CoverageDataBase(images, imagesTZ),
             new fr.ird.database.sample.sql.SampleDataBase(fisheries, fisheriesTZ));
    }

    /**
     * Returns a thread group that may be used
     * for reading images as a background process.
     */
    public ThreadGroup getThreadGroup() {
        return readers;
    }

    /**
     * Retourne la base de données d'images.
     *
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public synchronized CoverageDataBase getCoverageDataBase() throws SQLException {
        if (images == null) {
            images = new fr.ird.database.coverage.sql.CoverageDataBase();
        }
        return images;
    }

    /**
     * Retourne la base de données des pêches.
     *
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    protected synchronized SampleDataBase getSampleDataBase() throws SQLException {
        if (fisheries == null) {
            fisheries = new fr.ird.database.sample.sql.SampleDataBase();
        }
        return fisheries;
    }

    /**
     * Retourne une table d'images pour la série spécifiée.
     *
     * @param  series La série voulue, ou <code>null</code> pour une série par défaut.
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public CoverageTable getCoverageTable(final SeriesEntry series) throws SQLException {
        final CoverageDataBase images = getCoverageDataBase();
        return (series!=null) ? images.getCoverageTable(series) : images.getCoverageTable();
    }

    /**
     * Construit et retourne une liste de controleurs permettant
     * d'ajouter ou de retirer des couches sur une image.
     *
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public synchronized LayerControl[] getLayerControls() throws SQLException {
        final CoverageDataBase   images = getCoverageDataBase();
        final List<LayerControl> layers = new ArrayList<LayerControl>(3);
        final CoverageTable    currents = images.getCoverageTable(getGeostrophicCurrentTable());
        layers.add(new CoverageLayerControl());
        if (currents != null) {
            layers.add(new VectorLayerControl(currents, 1, 2));
        }
        final SampleDataBase fisheries = getSampleDataBase();
        layers.add(new IsolineLayerControl());
        layers.add(new SampleLayerControl(fisheries));
        return (LayerControl[])layers.toArray(new LayerControl[layers.size()]);
    }

    /**
     * Ferme toutes les connections avec les bases de données.
     * @throws SQLException si un problème est survenu
     *         lors de la fermeture d'une connection.
     */
    public synchronized void close() throws SQLException {
        if (images != null) {
            images.close();
            images = null;
        }
        if (fisheries != null) {
            fisheries.close();
            fisheries = null;
        }
    }
}
