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
package fr.ird.database.coverage.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.awt.geom.Rectangle2D;

// JAI
import javax.media.jai.util.Range;


/**
 * Interrogation de la table des couvertures g�ographiques.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class GridGeometryTable extends Table {
    /**
     * Requ�te SQL utilis�e pour obtenir les coordonn�es
     * et la plage de temps couverte par les images.
     */
    static final String SQL_SELECT =
            "SELECT Min("+GRID_COVERAGES +".start_time) AS start_time, "+
                   "Max("+GRID_COVERAGES +".end_time) AS end_time, "+
                   "Min("+GRID_GEOMETRIES+".xmin) AS xmin, "+
                   "Min("+GRID_GEOMETRIES+".ymin) AS ymin, "+
                   "Max("+GRID_GEOMETRIES+".xmax) AS xmax, "+
                   "Max("+GRID_GEOMETRIES+".ymax) AS ymax\n"+
            "FROM ("+GRID_COVERAGES+' '+
            "INNER JOIN "+SERIES         +" ON "+GRID_COVERAGES+".series="  +SERIES         +".ID) "+
            "INNER JOIN "+GRID_GEOMETRIES+" ON "+GRID_COVERAGES+".geometry="+GRID_GEOMETRIES+".ID\n"+
            "WHERE (visible=TRUE)";

    /** Date de d�but des images de la base de donn�es.      */ private long  startTime;
    /** Date de fin des images de la base de donn�es.        */ private long  endTime;
    /** Longitude minimale des images de la base de donn�es. */ private float xmin;
    /** Latitude  minimale des images de la base de donn�es. */ private float ymin;
    /** Longitude maximale des images de la base de donn�es. */ private float xmax;
    /** Latitude  maximale des images de la base de donn�es. */ private float ymax;

    /**
     * Indique si les coordonn�es g�ographiques et la plage de temps sont valides.
     */
    private boolean isValid;

    /**
     * Construit une table utilisant la connexion sp�cifi�e.
     */
    public GridGeometryTable(final Connection connection, final TimeZone timezone) throws SQLException {
        final Statement statement = connection.createStatement();
        final ResultSet result = statement.executeQuery(PREFERENCES.get(GRID_GEOMETRIES, SQL_SELECT));
        if (result.next()) {
            boolean wasNull=false;
            final Calendar      calendar = new GregorianCalendar(timezone);
            final Calendar localCalendar = new GregorianCalendar();
            final Date         startTime = getTimestamp(1, result, calendar, localCalendar); wasNull |= (startTime==null);
            final Date           endTime = getTimestamp(2, result, calendar, localCalendar); wasNull |= (  endTime==null);
            xmin=result.getFloat(3); wasNull |= result.wasNull();
            ymin=result.getFloat(4); wasNull |= result.wasNull();
            xmax=result.getFloat(5); wasNull |= result.wasNull();
            ymax=result.getFloat(6); wasNull |= result.wasNull();
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
     * Retourne les coordonn�es g�ographiques couvertes par les images de la base
     * de donn�es. Les longitudes et latitudes minimales et maximales seront lues
     * dans la base de donn�es.
     *
     * @return Coordonn�es g�ographiques (en degr�s de longitude et de latitude)
     *         couverte par les images, o� <code>null</code> si la base de donn�es
     *         ne contient pas d'images.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     */
    public Rectangle2D getGeographicArea() throws SQLException {
        return (isValid) ? new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin) : null;
    }

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de donn�es. Cette plage sera d�limit�e par des
     * objets {@link Date}.
     *
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     */
    public Range getTimeRange() throws SQLException {
        return (isValid) ? new Range(Date.class, new Date(startTime), new Date(endTime)) : null;
    }

    /**
     * Lib�re les ressources utilis�es par cette table.
     */
    public void close() throws SQLException {
    }
}
