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
package fr.ird.database.coverage.sql;

// J2SE and JAI dependencies
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

// Seagis dependencies.
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Interrogation de la table des couvertures géographiques.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class GeographicBoundingBoxTable extends Table {
    /**
     * Requête SQL utilisée pour obtenir les coordonnées
     * et la plage de temps couverte par les images.
     */
    static final ConfigurationKey SELECT = createKey(BOUNDING_BOX, ResourceKeys.SQL_GRID_GEOMETRIES,
            "SELECT MIN(start_time) " + "AS start_time, " +
                   "MAX(end_time) "   + "AS end_time, "   +
                   "MIN(x_min) "      + "AS x_min, "      +
                   "MIN(y_min) "      + "AS y_min, "      +
                   "MAX(x_max) "      + "AS x_max, "      +
                   "MAX(y_max) "      + "AS y_max\n"      +
            "FROM "+SCHEMA+".\""+GRID_COVERAGES+"\" "+
            "INNER JOIN "+SCHEMA+".\""+SUBSERIES   +"\" ON subseries=\""+SUBSERIES+"\".identifier "+
            "INNER JOIN "+SCHEMA+".\""+BOUNDING_BOX+"\" ON extent=\""+BOUNDING_BOX+"\".oid\n"+
            "WHERE (visible=TRUE)");


    /** Date de début des images de la base de données.      */ private long  startTime;
    /** Date de fin des images de la base de données.        */ private long  endTime;
    /** Longitude minimale des images de la base de données. */ private float xmin;
    /** Latitude  minimale des images de la base de données. */ private float ymin;
    /** Longitude maximale des images de la base de données. */ private float xmax;
    /** Latitude  maximale des images de la base de données. */ private float ymax;

    /**
     * Indique si les coordonnées géographiques et la plage de temps sont valides.
     */
    private boolean isValid;

    /**
     * Construit une table utilisant la connexion spécifiée.
     */
    public GeographicBoundingBoxTable(final CoverageDataBase database,
                                      final Connection     connection,
                                      final TimeZone         timezone)
            throws RemoteException, SQLException
    {
        super(database);
        final Statement statement = connection.createStatement();
        final ResultSet result = statement.executeQuery(getProperty(SELECT));
        if (result.next()) {
            boolean wasNull = false;
            final Calendar      calendar = new GregorianCalendar(timezone);
            final Calendar localCalendar = new GregorianCalendar();
            final Date         startTime = getTimestamp(1, result, calendar, localCalendar); wasNull |= (startTime==null);
            final Date           endTime = getTimestamp(2, result, calendar, localCalendar); wasNull |= (  endTime==null);
            xmin = result.getFloat(3); wasNull |= result.wasNull();
            ymin = result.getFloat(4); wasNull |= result.wasNull();
            xmax = result.getFloat(5); wasNull |= result.wasNull();
            ymax = result.getFloat(6); wasNull |= result.wasNull();
            if (!wasNull) {
                this.startTime = startTime.getTime();
                this.  endTime =   endTime.getTime();
                this.isValid   = true;
            }
        }
        result.close();
        statement.close();
    }

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
    public Rectangle2D getGeographicArea() throws SQLException {
        return (isValid) ? new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin) : null;
    }

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de données. Cette plage sera délimitée par des
     * objets {@link Date}.
     *
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     */
    public Range getTimeRange() throws SQLException {
        return (isValid) ? new Range(Date.class, new Date(startTime), new Date(endTime)) : null;
    }

    /**
     * Libère les ressources utilisées par cette table.
     */
    public void close() throws RemoteException {
    }
}
