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
package fr.ird.database.sample.sql;

// Base de donn�es
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

// Coordonn�es spatio-temporelles
import java.util.Date;
import java.util.TimeZone;
import java.awt.geom.Rectangle2D;

// Collections
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;

// Seagis
import fr.ird.animat.Species;
import fr.ird.database.sample.SampleEntry;


/**
 * Objet interrogeant la base de donn�es pour obtenir la liste des �chantillons pris le long
 * d'une ligne (par exemple une p�che � la palangre).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class LinearSampleTable extends SampleTable {
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des �chantillons.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes [@link #DATE}, [@link #START_LONGITUDE} et compagnie.
     */
    static final String SQL_SELECT = Table.configuration.get(Configuration.KEY_LINEAR_SAMPLE);
    // static final String SQL_SELECT=
    //                 "SELECT "+  /*[01] ID              */ "ID, "         +
    //                             /*[02] DATE            */ "date, "       +
    //                             /*[03] START_LONGITUDE */ "x1, "         +
    //                             /*[04] START_LATITUDE  */ "y1, "         +
    //                             /*[05] END_LONGITUDE   */ "x2, "         +
    //                             /*[06] END_LATITUDE    */ "y2, "         +
    //                             /*[07] EFFORT_UNIT     */ "nb_hame�ons " +
    // 
    //                 "FROM "+SAMPLES+"\n"+
    //                 "WHERE valid=TRUE "+
    //                   "AND (date>=? AND date<=?) "+
    //                   "AND (total>=?) "+
    //                 "ORDER BY date";

    // IMPORTANT: Les donn�es DOIVENT �tre class�es en ordre croissant de date
    //            du d�but de la p�che (StartTime), pour le bon fonctionnement
    //            de EnvironmentTableFiller.

    /** Num�ro de colonne. */ static final int ID              =  1;
    /** Num�ro de colonne. */ static final int DATE            =  2;
    /** Num�ro de colonne. */ static final int START_LONGITUDE =  3;
    /** Num�ro de colonne. */ static final int START_LATITUDE  =  4;
    /** Num�ro de colonne. */ static final int END_LONGITUDE   =  5;
    /** Num�ro de colonne. */ static final int END_LATITUDE    =  6;
    /** Num�ro de colonne. */ static final int EFFORT_UNIT     =  7;
    /** Num�ro de colonne. */ static final int SAMPLE_VALUE    =  8;

    /** Num�ro d'argument. */ private static final int ARG_START_TIME  =  1;
    /** Num�ro d'argument. */ private static final int ARG_END_TIME    =  2;
    /** Num�ro d'argument. */ private static final int ARG_TOTAL       =  3;

    /**
     * Construit un objet en utilisant la connection sp�cifi�e.
     *
     * @param  connection Connection vers une base de donn�es des �chantillons.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  species Esp�ces � inclure dans l'interrogation de la base de donn�es.
     * @throws SQLException si <code>SampleTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected LinearSampleTable(final Connection   connection,
                                final TimeZone     timezone,
                                final Set<Species> species)
            throws SQLException
    {
        super(connection, SQL_SELECT, timezone, species);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setGeographicArea(final Rectangle2D rect) throws SQLException {
        // Il est difficile de construire une requ�te en SQL standard
        // qui v�rifiera si la palangre intercepte un rectangle. On
        // v�rifiera plut�t � l'int�rieur de {@link #getEntries}.
        geographicArea.setRect(rect);
        packed = false;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setTimeRange(final Date startTime, final Date endTime) throws SQLException {
        final long startTimeMillis = startTime.getTime();
        final long   endTimeMillis =   endTime.getTime();
        final Timestamp time=new Timestamp(startTimeMillis);
        statement.setTimestamp(ARG_START_TIME, time, calendar);
        this.startTime = startTimeMillis;
        time.setTime(endTimeMillis);
        statement.setTimestamp(ARG_END_TIME, time, calendar);
        this.endTime = endTimeMillis;
        packed = false;
    }

    /**
     * {@inheritDoc}
     *
     * @task TODO: Faire "String SampleTable.getMinMaxQuery(String query)" (qui coupe avant "ORDER BY").
     *             Faire "static LinearSampleTable.setGeographicArea(PreparedStatement statement, Rectangle2D area)"
     *             et m�me chose pour "setTimeRange".
     */
    protected void packEnvelope() throws SQLException {
        final ResultSet result = statement.executeQuery();
        double xmin = geographicArea.getMinX();
        double ymin = geographicArea.getMinY();
        double xmax = geographicArea.getMaxX();
        double ymax = geographicArea.getMaxY();
        long   tmin = startTime;
        long   tmax =   endTime;
        while (result.next()) {
            double x1=result.getDouble(START_LONGITUDE); if (result.wasNull()) x1=Double.NaN;
            double y1=result.getDouble(START_LATITUDE ); if (result.wasNull()) y1=Double.NaN;
            double x2=result.getDouble(  END_LONGITUDE); if (result.wasNull()) x2=x1;
            double y2=result.getDouble(  END_LATITUDE ); if (result.wasNull()) y2=y1;
            long    t=getTimestamp(DATE, result).getTime();
            if (Double.isNaN(x1)) x1=x2;
            if (Double.isNaN(y1)) y1=y2;
            if (x1>x2) {final double x=x1; x1=x2; x2=x;}
            if (y1>y2) {final double y=y1; y1=y2; y2=y;}
            if (x1<xmin) xmin=x1;
            if (x2>xmax) xmax=x2;
            if (y1<ymin) ymin=y1;
            if (y2>ymax) ymax=y2;
            if ( t<tmin) tmin=t;
            if ( t>tmax) tmax=t;
        }
        result.close();
        if (tmin<tmax) {
            startTime = tmin;
              endTime = tmax;
        }
        if (xmin<=xmax && ymin<=ymax) {
            geographicArea.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
        }
    }

    /**
     * {@inheritDoc}
     */
    final void setMinimumValue(final double minimum) throws SQLException {
        statement.setDouble(ARG_TOTAL, minimum);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Collection<SampleEntry> getEntries() throws SQLException {
        final ResultSet             result = statement.executeQuery();
        final Collection<SampleEntry> list = new ArrayList<SampleEntry>();
        while (result.next()) {
            final SampleEntry entry = new LinearSampleEntry(this, result);
            if (entry.intersects(geographicArea)) {
                list.add(entry);
            }
        }
        result.close();
        return list;
    }
}
