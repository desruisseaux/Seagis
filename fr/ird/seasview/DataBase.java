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
 * Source de donn�es utilis�es par l'ensemble de l'application.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class DataBase {
    /**
     * <code>true</code> pour compiler la version "M�diterran�e" de l'application, or
     * <code>false</code> pour compiler la version "Oc�an Indien". Ce drapeau n'est
     * qu'un bricolage temporaire en attendant de trouver une solution plus g�n�rale.
     *
     * Ce drapeau est initialis� au moment du d�marrage de l'application par {@link Main}.
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
     * Les pr�f�rences de l'utilisateur pour l'application.
     */
    public static final Preferences preferences = Preferences.userNodeForPackage(DataBase.class);

    /**
     * Nom de la table des donn�es de courants g�ostrophiques.
     */
    private static String getGeostrophicCurrentTable() {
        return MEDITERRANEAN_VERSION ? "SLA (M�diterran�e - NRT)"
                                     : "SLA (R�union)";
    }

    /**
     * Connexion vers la base de donn�es d'images.
     * Si <code>null</code>, alors la connexion sera
     * �tablie la premi�re fois o� elle sera demand�e.
     */
    private CoverageDataBase images;

    /**
     * Connexion vers la base de donn�es de p�ches.
     * Si <code>null</code>, alors la connexion sera
     * �tablie la premi�re fois o� elle sera demand�e.
     */
    private SampleDataBase fisheries;

    /**
     * Groupe des threads qui sont en train d'effectuer une op�ration.
     * Il peut s'agir notamment d'exporter des donn�es ou d'effectuer
     * un couplage entre des donn�es de p�ches et d'environnement. Les
     * applications qui acc�dent aux bases de donn�es en arri�re-plan
     * sont encourag�s � placer leurs threads dans ce groupe ou dans
     * un de ses sous-groupes.
     */
    final ThreadGroup threads=new ThreadGroup("SEAS workers");

    /**
     * Sous-groupe des threads ayant la charge de lire des images en arri�re-plan.
     * <code>readers</code> est un sous-groupe de {@link #threads}, mais avec une
     * priorit� l�g�rement plus �lev�e afin de fournir une r�ponse plus rapide �
     * l'utilisateur.
     */
    final ThreadGroup readers=new ThreadGroup(threads, "Image readers");

    /**
     * Sous-groupe des threads ayant la charge de construire une interface utilisateur.
     * <code>builder</code> est un sous-groupe de {@link #threads}, mais avec une
     * priorit� plus �lev�e afin de fournir une r�ponse plus rapide � l'utilisateur.
     */
    final ThreadGroup builder=new ThreadGroup(threads, "GUI builders");

    /**
     * Construit une nouvelle source de donn�es avec des
     * connections vers les bases de donn�es sp�cifi�es.
     */
    private DataBase(final CoverageDataBase images, final SampleDataBase fisheries) {
        this.images    = images;
        this.fisheries = fisheries;
        threads.setMaxPriority(Thread.NORM_PRIORITY-3);
        readers.setMaxPriority(Thread.NORM_PRIORITY-2);
        builder.setMaxPriority(Thread.NORM_PRIORITY-1);
    }

    /**
     * Construit une nouvelle source de donn�es avec
     * des connections vers les bases de donn�es par
     * d�faut. Ces connections par d�faut peuvent
     * avoir �t� enregistr�es dans les pr�f�rences
     * de l'utilisateur.
     *
     * @throws SQLException si les connections avec les bases de donn�es ont �chou�es.
     */
    public DataBase() throws SQLException {
        this(null, null);
    }

    /**
     * Construit une nouvelle source de donn�es
     * qui tiendra une connexion vers les bases
     * de donn�es du nom sp�cifi�.
     *
     * @param images      Nom de la base de donn�es d'images.   Ce sera typiquement <code>"PELOPS-Images"</code>.
     * @param imagesTZ    Fuseau horaire par d�faut des dates dans les bases de donn�es d'images.
     * @param fisheries   Nom de la base de donn�es des p�ches. Ce sera typiquement <code>"PELOPS-P�ches"</code>.
     * @param fisheriesTZ Fuseau horaire par d�faut des dates dans les bases de donn�es de p�ches.
     * @throws SQLException si les connections avec les bases de donn�es ont �chou�es.
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
     * Retourne la base de donn�es d'images.
     *
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     */
    public synchronized CoverageDataBase getCoverageDataBase() throws SQLException {
        if (images == null) {
            images = new fr.ird.database.coverage.sql.CoverageDataBase();
        }
        return images;
    }

    /**
     * Retourne la base de donn�es des p�ches.
     *
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     */
    protected synchronized SampleDataBase getSampleDataBase() throws SQLException {
        if (fisheries == null) {
            fisheries = new fr.ird.database.sample.sql.SampleDataBase();
        }
        return fisheries;
    }

    /**
     * Retourne une table d'images pour la s�rie sp�cifi�e.
     *
     * @param  series La s�rie voulue, ou <code>null</code> pour une s�rie par d�faut.
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     */
    public CoverageTable getCoverageTable(final SeriesEntry series) throws SQLException {
        final CoverageDataBase images = getCoverageDataBase();
        return (series!=null) ? images.getCoverageTable(series) : images.getCoverageTable();
    }

    /**
     * Construit et retourne une liste de controleurs permettant
     * d'ajouter ou de retirer des couches sur une image.
     *
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
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
     * Ferme toutes les connections avec les bases de donn�es.
     * @throws SQLException si un probl�me est survenu
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
