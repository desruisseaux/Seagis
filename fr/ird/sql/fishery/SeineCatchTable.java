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
package fr.ird.sql.fishery;

// Base de donn�es
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

// Temps
import java.util.Date;
import java.util.TimeZone;

// Coordonn�es
import java.awt.geom.Rectangle2D;

// Collections
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

// Divers
import fr.ird.animat.Species;


/**
 * Objet interrogeant la base de donn�es pour obtenir la liste des p�ches �
 * la senne qu'elle contient. Ces p�ches pourront �tre s�lectionn�es dans
 * une certaine r�gion g�ographique et � certaines dates.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SeineCatchTable extends AbstractCatchTable
{
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des p�ches.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes [@link #DATE}, [@link #LONGITUDE} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID        */ SEINES+".ID, "      +
                                /*[02] CALEES    */ SEINES+".nCalees, " +
                                /*[03] DATE      */ SEINES+".date, "    +
                                /*[04] LONGITUDE */ SEINES+".x, "       +
                                /*[05] LATITUDE  */ SEINES+".y "        +

                    "FROM "+SEINES+" "+
                    "WHERE "+
                         " (date>=? AND date<=?) "+
                      "AND (x>=? AND x<=?) "+
                      "AND (y>=? AND y<=?)\n"+
                    "ORDER BY date";

    // IMPORTANT: Les donn�es DOIVENT �tre class�es en ordre croissant de date
    //            du d�but de la p�che (StartTime), pour le bon fonctionnement
    //            de {@link Coupling}.

    /** Num�ro de colonne. */ static final int ID           =  1;
    /** Num�ro de colonne. */ static final int CALEES       =  2;
    /** Num�ro de colonne. */ static final int DATE         =  3;
    /** Num�ro de colonne. */ static final int LONGITUDE    =  4;
    /** Num�ro de colonne. */ static final int LATITUDE     =  5;
    /** Num�ro de colonne. */ static final int CATCH_AMOUNT =  6; // Used by SenneCatchEntry

    /** Num�ro d'argument. */ private static final int ARG_START_TIME  =  1;
    /** Num�ro d'argument. */ private static final int ARG_END_TIME    =  2;
    /** Num�ro d'argument. */ private static final int ARG_XMIN        =  3;
    /** Num�ro d'argument. */ private static final int ARG_XMAX        =  4;
    /** Num�ro d'argument. */ private static final int ARG_YMIN        =  5;
    /** Num�ro d'argument. */ private static final int ARG_YMAX        =  6;

    /**
     * Construit un objet en utilisant la connection sp�cifi�e.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  species Esp�ces � inclure dans l'interrogation de la base de donn�es.
     * @throws SQLException si <code>CatchTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected SeineCatchTable(final Connection connection, final TimeZone timezone, final Set<Species> species) throws SQLException
    {
        super(connection, SEINES, preferences.get(SEINES, SQL_SELECT), timezone, species);
    }

    /**
     * D�finit les coordonn�es g�ographiques de la r�gion dans laquelle on veut rechercher des p�ches.
     * Les coordonn�es doivent �tre exprim�es en degr�s de longitude et de latitude selon l'ellipso�de
     * WGS&nbsp;1984. Toutes les p�ches qui interceptent cette r�gion seront prises en compte lors du
     * prochain appel de {@link #getEntries}.
     *
     * @param  rect Coordonn�es g�ographiques de la r�gion, en degr�s de longitude et de latitude.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized void setGeographicArea(final Rectangle2D rect) throws SQLException
    {
        statement.setDouble(ARG_XMIN, rect.getMinX());
        statement.setDouble(ARG_XMAX, rect.getMaxX());
        statement.setDouble(ARG_YMIN, rect.getMinY());
        statement.setDouble(ARG_YMAX, rect.getMaxY());
        geographicArea.setRect(rect);
        packed = false;
    }

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es de p�ches.
     * Toutes les p�ches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized void setTimeRange(final Date startTime, final Date endTime) throws SQLException
    {
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
     * Calcule les coordonn�es g�ographiques et la plage de temps couvertes
     * par les donn�es de p�ches. La plage de temps aura �t� sp�cifi�e avec
     * {@link #setTimeRange} et les limites de la r�gion g�ographique avec
     * {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    protected void packEnvelope() throws SQLException
    {
        // TODO:
        // Faire "String CatchTable.getMinMaxQuery(String query)" (qui coupe avant "ORDER BY").
        // Faire "static LonglineCatchTable.setGeographicArea(PreparedStatement statement, Rectangle2D area)"
        // et m�me chose pour "setTimeRange".

        final ResultSet result = statement.executeQuery();
        double xmin = geographicArea.getMinX();
        double ymin = geographicArea.getMinY();
        double xmax = geographicArea.getMaxX();
        double ymax = geographicArea.getMaxY();
        long   tmin = startTime;
        long   tmax =   endTime;
        while (result.next())
        {
            double x=result.getDouble(LONGITUDE); if (result.wasNull()) x=Double.NaN;
            double y=result.getDouble(LATITUDE ); if (result.wasNull()) y=Double.NaN;
            long   t=getTimestamp(DATE, result).getTime();
            if (x<xmin) xmin=x;
            if (x>xmax) xmax=x;
            if (y<ymin) ymin=y;
            if (y>ymax) ymax=y;
            if (t<tmin) tmin=t;
            if (t>tmax) tmax=t;
        }
        result.close();
        if (tmin<tmax)
        {
            startTime = tmin;
              endTime = tmax;
        }
        if (xmin<=xmax && ymin<=ymax)
        {
            geographicArea.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
        }
    }

    /**
     * Retourne la liste des captures connues dans la r�gion et dans la plage de
     * dates pr�alablement s�lectionn�es. Ces plages auront �t� sp�cifi�es � l'aide
     * des diff�rentes m�thodes <code>set...</code> de cette classe. Cette m�thode
     * ne retourne jamais <code>null</code>, mais peut retourner une liste de
     * longueur 0.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized List<CatchEntry> getEntries() throws SQLException
    {
        final ResultSet      result = statement.executeQuery();
        final List<CatchEntry> list = new ArrayList<CatchEntry>();
        while (result.next())
        {
            list.add(new SeineCatchEntry(this, result));
        }
        result.close();
        return list;
    }
}
