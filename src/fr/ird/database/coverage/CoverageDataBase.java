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
package fr.ird.database.coverage;

// J2SE et JAI
import java.sql.SQLException;
import java.util.logging.Logger;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

// Seagis
import fr.ird.database.DataBase;


/**
 * Connection avec la base de donn�es d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageDataBase extends DataBase {
    /**
     * Journal des �v�nements.
     *
     * @see #SQL_SELECT
     * @see #SQL_UPDATE
     */
    public static final Logger LOGGER = Logger.getLogger("fr.ird.database");

    /**
     * Retourne les coordonn�es g�ographiques couvertes par les images de la base
     * de donn�es. Les longitudes et latitudes minimales et maximales seront lues
     * dans la base de donn�es.
     *
     * @return Coordonn�es g�ographiques (en degr�s de longitude et de latitude)
     *         couverte par les images, o� <code>null</code> si la base de donn�es
     *         ne contient pas d'images.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     */
    public abstract Rectangle2D getGeographicArea() throws SQLException;

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de donn�es. Cette plage sera d�limit�e par des
     * objets {@link Date}.
     *
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     */
    public abstract Range getTimeRange() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Series" de la base de donn�es d'images.  Lorsque cette
     * table n'est plus n�cessaire, il faudra appeler
     * {@link SeriesTable#close}.
     *
     * @return La plage de temps de la base de donn�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract SeriesTable getSeriesTable() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table "GridCoverages" de
     * la base de donn�es d'images. Cette table fera ses recherches dans une
     * s�rie par d�faut. Il faudra appeler {@link CoverageTable#setSeries}
     * pour sp�cifier une autre s�rie. Lorsque cette table n'est plus n�cessaire,
     * il faudra appeler {@link CoverageTable#close}.
     *
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract CoverageTable getCoverageTable() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de donn�es d'images. Lorsque
     * cette table n'est plus n�cessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series R�f�rence vers la s�rie d'images.
     * @throws SQLException si la r�f�rence n'est pas valide ou table n'a pas pu �tre construite.
     */
    public abstract CoverageTable getCoverageTable(final SeriesEntry series) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de donn�es d'images. Lorsque
     * cette table n'est plus n�cessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Nom de la s�rie d'images.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract CoverageTable getCoverageTable(final String series) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table
     * "GridCoverages" de la base de donn�es d'images.  Lorsque cette
     * table n'est plus n�cessaire, il faudra appeler
     * {@link CoverageTable#close}.
     *
     * @param  series Num�ro de la s�rie d'images.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract CoverageTable getCoverageTable(final int seriesID) throws SQLException;
}
