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
 */
package fr.ird.database.coverage;

// J2SE and JAI dependencies
import java.rmi.RemoteException;
import java.util.logging.Logger;
import javax.media.jai.util.Range;
import java.awt.geom.Rectangle2D;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.geometry.XRectangle2D;

// Seagis
import fr.ird.database.DataBase;
import fr.ird.database.ConfigurationKey;


/**
 * A connection to a database managing {@linkplain GridCoverage grid coverage} informations.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageDataBase extends DataBase {
    /**
     * Key for fetching the home directory of grid coverage files.
     *
     * @see #getProperty
     */
    public static final ConfigurationKey DIRECTORY = new ConfigurationKey("Directory", null, "/home/data/");

    /**
     * The logger for events relative to this object.
     */
    public static final Logger LOGGER = Logger.getLogger("fr.ird.database.coverage");

    /**
     * Retourne les coordonnées géographiques couvertes par les images de la base
     * de données. Les longitudes et latitudes minimales et maximales seront lues
     * dans la base de données.
     *
     * @return Coordonnées géographiques (en degrés de longitude et de latitude)
     *         couverte par les images, où <code>null</code> si la base de données
     *         ne contient pas d'images.
     * @throws RemoteException si le catalogue n'a pas pu être interrogée.
     */
    public abstract Rectangle2D getGeographicArea() throws RemoteException;

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de données. Cette plage sera délimitée par des
     * objets {@link Date}.
     *
     * @throws RemoteException si le catalogue n'a pas pu être interrogée.
     */
    public abstract Range getTimeRange() throws RemoteException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Series" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link SeriesTable#close}.
     *
     * @return La plage de temps de la base de données.
     * @throws RemoteException si le catalogue n'a pas pu être construite.
     */
    public abstract SeriesTable getSeriesTable() throws RemoteException;

    /**
     * Construit et retourne un objet qui interrogera la table "GridCoverages" de
     * la base de données d'images. Cette table fera ses recherches dans une
     * série par défaut. Il faudra appeler {@link CoverageTable#setSeries}
     * pour spécifier une autre série. Lorsque cette table n'est plus nécessaire,
     * il faudra appeler {@link CoverageTable#close}.
     *
     * @throws RemoteException si le catalogue n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable() throws RemoteException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de données d'images. Lorsque
     * cette table n'est plus nécessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Référence vers la série d'images.
     * @throws RemoteException si la référence n'est pas valide ou table n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable(final SeriesEntry series) throws RemoteException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de données d'images. Lorsque
     * cette table n'est plus nécessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Nom de la série d'images.
     * @throws RemoteException si la table n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable(final String series) throws RemoteException;
}
