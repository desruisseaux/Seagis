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
package fr.ird.main;

// Database
import java.sql.SQLException;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.fishery.FisheryDataBase;

// Layers
import fr.ird.layer.control.LayerControl;
import fr.ird.layer.control.ImageLayerControl;
import fr.ird.layer.control.CatchLayerControl;
import fr.ird.layer.control.VectorLayerControl;

// Logger
import java.util.logging.Logger;

// Collections
import java.util.List;
import java.util.ArrayList;

// Miscellaneous
import java.util.TimeZone;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Source de données utilisées par l'ensemble de l'application.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class DataBase
{
    /**
     * The logger for the application.
     */
    public static final Logger logger = Logger.getLogger("fr.ird");

    /**
     * Nom de la table des données de SLA.
     */
    private static final String SLA_TABLE = "SLA (Méditerranée)";
//  private static final String SLA_TABLE = "SLA (Réunion)";

    /**
     * Connexion vers la base de données d'images.
     */
    private final ImageDataBase images;

    /**
     * Connexion vers la base de données de pêches.
     */
    private final FisheryDataBase fisheries;

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
    private DataBase(final ImageDataBase images, final FisheryDataBase fisheries)
    {
        this.images    = images;
        this.fisheries = fisheries;
        threads.setMaxPriority(Thread.NORM_PRIORITY-3);
        readers.setMaxPriority(Thread.NORM_PRIORITY-2);
        builder.setMaxPriority(Thread.NORM_PRIORITY-1);
        readers.setDaemon(true);
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
    public DataBase() throws SQLException
    {this(new ImageDataBase(), new FisheryDataBase());}

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
    public DataBase(final String images, final TimeZone imagesTZ, final String fisheries, final TimeZone fisheriesTZ) throws SQLException
    {this(new ImageDataBase(images, imagesTZ), new FisheryDataBase(fisheries, fisheriesTZ));}

    /**
     * Retourne le fuseau horaire de la table d'images.
     */
    final TimeZone getTimeZone()
    {return images.getTimeZone();}

    /**
     * Returns a thread group that may be used
     * for reading images as a background process.
     */
    public ThreadGroup getThreadGroup()
    {return readers;}

    /**
     * Retourne la base de données d'images.
     *
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public ImageDataBase getImageDataBase() throws SQLException
    {return images;}

    /**
     * Retourne une table d'images pour la série spécifiée.
     *
     * @param  series La série voulue, ou <code>null</code> pour une série par défaut.
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public ImageTable getImageTable(final SeriesEntry series) throws SQLException
    {return (series!=null) ? images.getImageTable(series) : images.getImageTable();}

    /**
     * Construit et retourne une liste de controleurs permettant
     * d'ajouter ou de retirer des couches sur une image.
     *
     * @throws SQLException si les accès à la base de données ont échoués.
     */
    public synchronized LayerControl[] getLayerControls() throws SQLException
    {
        final List<LayerControl> layers = new ArrayList<LayerControl>(3);
        final ImageTable       currents = images.getImageTable(SLA_TABLE);
        layers.add(new ImageLayerControl());
        if (currents!=null)
        {
            layers.add(new VectorLayerControl(currents, 1, 2));
        }
        layers.add(new CatchLayerControl(fisheries));
        return layers.toArray(new LayerControl[layers.size()]);
    }

    /**
     * Ferme toutes les connections avec les bases de données.
     * @throws SQLException si un problème est survenu
     *         lors de la fermeture d'une connection.
     */
    public synchronized void close() throws SQLException
    {
        images   .close();
        fisheries.close();
    }
}
