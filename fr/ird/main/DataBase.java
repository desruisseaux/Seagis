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
 * Source de donn�es utilis�es par l'ensemble de l'application.
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
     * Nom de la table des donn�es de SLA.
     */
    private static final String SLA_TABLE = "SLA (M�diterran�e)";
//  private static final String SLA_TABLE = "SLA (R�union)";

    /**
     * Connexion vers la base de donn�es d'images.
     */
    private final ImageDataBase images;

    /**
     * Connexion vers la base de donn�es de p�ches.
     */
    private final FisheryDataBase fisheries;

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
     * Construit une nouvelle source de donn�es avec
     * des connections vers les bases de donn�es par
     * d�faut. Ces connections par d�faut peuvent
     * avoir �t� enregistr�es dans les pr�f�rences
     * de l'utilisateur.
     *
     * @throws SQLException si les connections avec les bases de donn�es ont �chou�es.
     */
    public DataBase() throws SQLException
    {this(new ImageDataBase(), new FisheryDataBase());}

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
     * Retourne la base de donn�es d'images.
     *
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     */
    public ImageDataBase getImageDataBase() throws SQLException
    {return images;}

    /**
     * Retourne une table d'images pour la s�rie sp�cifi�e.
     *
     * @param  series La s�rie voulue, ou <code>null</code> pour une s�rie par d�faut.
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     */
    public ImageTable getImageTable(final SeriesEntry series) throws SQLException
    {return (series!=null) ? images.getImageTable(series) : images.getImageTable();}

    /**
     * Construit et retourne une liste de controleurs permettant
     * d'ajouter ou de retirer des couches sur une image.
     *
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
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
     * Ferme toutes les connections avec les bases de donn�es.
     * @throws SQLException si un probl�me est survenu
     *         lors de la fermeture d'une connection.
     */
    public synchronized void close() throws SQLException
    {
        images   .close();
        fisheries.close();
    }
}
