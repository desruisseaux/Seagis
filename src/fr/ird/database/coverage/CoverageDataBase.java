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
package fr.ird.database.coverage;

// J2SE et JAI
import java.sql.SQLException;
import java.util.logging.Logger;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

// Seagis
import fr.ird.database.DataBase;


/**
 * Connection avec la base de données d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageDataBase extends DataBase {
    /**
     * Journal des évènements.
     *
     * @see #SQL_SELECT
     * @see #SQL_UPDATE
     */
    public static final Logger LOGGER = Logger.getLogger("fr.ird.database");

    /**
     * Retourne les coordonnées géographiques couvertes par les images de la base
     * de données. Les longitudes et latitudes minimales et maximales seront lues
     * dans la base de données.
     *
     * @return Coordonnées géographiques (en degrés de longitude et de latitude)
     *         couverte par les images, où <code>null</code> si la base de données
     *         ne contient pas d'images.
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     */
    public abstract Rectangle2D getGeographicArea() throws SQLException;

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de données. Cette plage sera délimitée par des
     * objets {@link Date}.
     *
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     */
    public abstract Range getTimeRange() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Series" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link SeriesTable#close}.
     *
     * @return La plage de temps de la base de données.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract SeriesTable getSeriesTable() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table "GridCoverages" de
     * la base de données d'images. Cette table fera ses recherches dans une
     * série par défaut. Il faudra appeler {@link CoverageTable#setSeries}
     * pour spécifier une autre série. Lorsque cette table n'est plus nécessaire,
     * il faudra appeler {@link CoverageTable#close}.
     *
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de données d'images. Lorsque
     * cette table n'est plus nécessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Référence vers la série d'images.
     * @throws SQLException si la référence n'est pas valide ou table n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable(final SeriesEntry series) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de données d'images. Lorsque
     * cette table n'est plus nécessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Nom de la série d'images.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable(final String series) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Numéro de la série d'images.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract CoverageTable getCoverageTable(final int seriesID) throws SQLException;
}
