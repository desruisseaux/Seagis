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
package fr.ird.sql.fishery;

// Base de données
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

// Temps
import java.util.Date;
import java.util.TimeZone;

// Coordonnées
import java.awt.geom.Rectangle2D;

// Collections
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

// Divers
import net.seas.util.XClass;
import fr.ird.animat.Species;
import fr.ird.resources.Resources;
import javax.media.jai.util.Range;


/**
 * Objet interrogeant la base de données pour obtenir la liste des pêches à
 * la palangre qu'elle contient. Ces pêches pourront être sélectionnées dans
 * une certaine région géographique et à certaines dates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class LonglineCatchTable extends AbstractCatchTable
{
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des pêches.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes [@link #START_TIME}, [@link #LONGITUDE} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID              */ LONGLINES+".ID, "        +
                                /*[02] DATE            */ LONGLINES+".date, "      +
                                /*[03] START_LONGITUDE */ LONGLINES+".x1, "        +
                                /*[04] START_LATITUDE  */ LONGLINES+".y1, "        +
                                /*[05] END_LONGITUDE   */ LONGLINES+".x2, "        +
                                /*[06] END_LATITUDE    */ LONGLINES+".y2\n"        +

                    "FROM "+LONGLINES+" "+
                    "WHERE (date>=? AND date<=?) "+
//                    "AND (max(x1,x2)>=? AND min(x1,x2)<=?) "+
//                    "AND (max(y1,y2)>=? AND min(y1,y2)<=?)\n"+
                    "ORDER BY date";

    // IMPORTANT: Les données DOIVENT être classées en ordre croissant de date
    //            du début de la pêche (StartTime), pour le bon fonctionnement
    //            de {@link Coupling}.

    /** Numéro de colonne. */ static final int ID              =  1;
    /** Numéro de colonne. */ static final int DATE            =  2;
    /** Numéro de colonne. */ static final int START_LONGITUDE =  3;
    /** Numéro de colonne. */ static final int START_LATITUDE  =  4;
    /** Numéro de colonne. */ static final int END_LONGITUDE   =  5;
    /** Numéro de colonne. */ static final int END_LATITUDE    =  6;
    /** Numéro de colonne. */ static final int CATCHS          =  7;

    /** Numéro d'argument. */ private static final int ARG_START_TIME  =  1;
    /** Numéro d'argument. */ private static final int ARG_END_TIME    =  2;
    /** Numéro d'argument. */ private static final int ARG_XMIN        =  3;
    /** Numéro d'argument. */ private static final int ARG_YMIN        =  4;
    /** Numéro d'argument. */ private static final int ARG_XMAX        =  5;
    /** Numéro d'argument. */ private static final int ARG_YMAX        =  6;

    /**
     * Coordonnées géographiques demandées par l'utilisateur. Ces
     * coordonnées sont spécifiées par {@link #setGeographicArea}.
     * Ces coordonnées peuvent être réduites lors de l'appel de
     * {@link #packEnvelope}.
     */
    private final Rectangle2D geographicArea = new Rectangle2D.Double();

    /**
     * Date de début et de fin de la plage de temps demandée par l'utilisateur.
     * Cette plage est spécifiée par {@link #setTimeRange}. Cette plage peut
     * être réduite lors de l'appel de {@link #packEnvelope}.
     */
    private long startTime, endTime;

    /**
     * Indique si la méthode {@link #packEnvelope} a été appelée.
     */
    private boolean packed;

    /**
     * Objet à utiliser pour les mises à jour. Cet
     * objet ne sera construit que la première fois
     * où il sera nécessaire.
     */
    private transient Statement update;

    /**
     * Construit un objet en utilisant la connection spécifiée.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @param  species Espèces à inclure dans l'interrogation de la base de données.
     * @throws SQLException si <code>CatchTable</code> n'a pas pu construire sa requête SQL.
     */
    protected LonglineCatchTable(final Connection connection, final TimeZone timezone, final Set<Species> species) throws SQLException
    {
        super(connection.prepareStatement(completeQuery(preferences.get(LONGLINES, SQL_SELECT), LONGLINES, species)), timezone, species);
        setTimeRange(new Date(0), new Date());
        setGeographicArea(new Rectangle2D.Double(-180, -90, 360, 180));
    }

    /**
     * Définit les coordonnées géographiques de la région dans laquelle on veut rechercher des pêches.
     * Les coordonnées doivent être exprimées en degrés de longitude et de latitude selon l'ellipsoïde
     * WGS&nbsp;1984. Toutes les pêches qui interceptent cette région seront prises en compte lors du
     * prochain appel de {@link #getEntries}.
     *
     * @param  rect Coordonnées géographiques de la région, en degrés de longitude et de latitude.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized void setGeographicArea(final Rectangle2D rect) throws SQLException
    {
//      statement.setDouble(ARG_XMIN, rect.getMinX());
//      statement.setDouble(ARG_XMAX, rect.getMaxX());
//      statement.setDouble(ARG_YMIN, rect.getMinY());
//      statement.setDouble(ARG_YMAX, rect.getMaxY());
        geographicArea.setRect(rect);
        packed=false;
    }

    /**
     * Retourne les coordonnées géographiques de la région des captures.  Cette région
     * ne sera pas plus grande que la région qui a été spécifiée lors du dernier appel
     * de la méthode {@link #setGeographicArea}.  Elle peut toutefois être plus petite
     * de façon à n'englober que les données de pêches présentes dans la base de données.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized Rectangle2D getGeographicArea() throws SQLException
    {
        packEnvelope();
        return (Rectangle2D) geographicArea.clone();
    }

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données de pêches.
     * Toutes les pêches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
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
        this.packed  = false;
    }

    /**
     * Retourne la plage de dates des pêches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates spécifiée lors du dernier appel de la méthode {@link #setTimeRange}.
     * Elle peut toutefois être plus petite de façon à n'englober que les données de pêches
     * présentes dans la base de données.
     *
     * @param  La plage de dates des données de pêches. Cette plage sera constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized Range getTimeRange() throws SQLException
    {
        packEnvelope();
        return new Range(Date.class, new Date(startTime), new Date(endTime));
    }

    /**
     * Calcule les coordonnées géographiques et la plage de temps couvertes
     * par les données de pêches. La plage de temps aura été spécifiée avec
     * {@link #setTimeRange} et les limites de la région géographique avec
     * {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    private void packEnvelope() throws SQLException
    {
        if (packed) return;

        // TODO:
        // Faire "String CatchTable.getMinMaxQuery(String query)" (qui coupe avant "ORDER BY").
        // Faire "static LonglineCatchTable.setGeographicArea(PreparedStatement statement, Rectangle2D area)"
        // et même chose pour "setTimeRange".

        final ResultSet result = statement.executeQuery();
        double xmin = geographicArea.getMinX();
        double ymin = geographicArea.getMinY();
        double xmax = geographicArea.getMaxX();
        double ymax = geographicArea.getMaxY();
        long   tmin = startTime;
        long   tmax =   endTime;
        while (result.next())
        {
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
        if (tmin<tmax)
        {
            startTime = tmin;
              endTime = tmax;
        }
        if (xmin<=xmax && ymin<=ymax)
        {
            geographicArea.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
        }
        packed = true;
    }

    /**
     * Retourne la liste des captures connues dans la région et dans la plage de
     * dates préalablement sélectionnées. Ces plages auront été spécifiées à l'aide
     * des différentes méthodes <code>set...</code> de cette classe. Cette méthode
     * ne retourne jamais <code>null</code>, mais peut retourner une liste de
     * longueur 0.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized List<CatchEntry> getEntries() throws SQLException
    {
        final ResultSet      result = statement.executeQuery();
        final List<CatchEntry> list = new ArrayList<CatchEntry>();
        while (result.next())
        {
            list.add(new LonglineCatchEntry(this, result));
        }
        result.close();
        return list;
    }

    /**
     * Retourne un itérateur qui balayera la liste des captures dans la région et dans la
     * plage de dates préalablement sélectionnées. Ces plages auront été spécifiées à l'aide
     * des différentes méthodes <code>set...</code> de cette classe. Cette méthode ne retourne
     * jamais <code>null</code>, mais peut retourner un itérateur qui ne balayera aucun élément.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
//  public synchronized DataIterator getCatchIterator() throws SQLException
//  {
//      final ResultSet result=statement.executeQuery();
//      final boolean hasFirst=result.next();
//      return new DataIterator()
//      {
//          private boolean hasNext = hasFirst;
//
//          public boolean hasNext()
//          {return hasNext;}
//
//          public LocatedData next() throws SQLException
//          {
//              final LongLineCatch c=new LongLineCatch(LongLineTable.this, result);
//              if ((hasNext=result.next())==false) result.close();
//              return c;
//          }
//      };
//  }

    /**
     * Définie une valeur réelle pour une capture données.  Cette méthode peut être utilisée
     * pour mettre à jour certaine informations relatives à la capture. La capture spécifiée
     * doit exister dans la base de données.
     *
     * @param capture    Capture à mettre à jour. Cette capture définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture spécifiée n'existe pas, ou si la mise à jour
     *         de la base de données a échouée pour une autre raison.
     */
    public synchronized void setValue(final CatchEntry capture, final String columnName, final float value) throws SQLException
    {
        if (update==null)
        {
            update = statement.getConnection().createStatement();
        }
        if (update.executeUpdate("UPDATE "+LONGLINES+" SET "+columnName+"="+value+" WHERE ID="+capture.getID())==0)
        {
            throw new SQLException(Resources.format(Clé.CATCH_NOT_FOUND¤1, capture));
        }
    }

    /**
     * Définie une valeur booléenne pour une capture données. Cette méthode peut être utilisée
     * pour mettre à jour certaine informations relatives à la capture.   La capture spécifiée
     * doit exister dans la base de données.
     *
     * @param capture    Capture à mettre à jour. Cette capture définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture spécifiée n'existe pas, ou si la mise à jour
     *         de la base de données a échouée pour une autre raison.
     */
    public synchronized void setValue(final CatchEntry capture, final String columnName, final boolean value) throws SQLException
    {
        if (update==null)
        {
            update = statement.getConnection().createStatement();
        }
        // Note: PostgreSQL demande que "TRUE" et "FALSE" soient en majuscules. MySQL n'a pas de type boolean.
        if (update.executeUpdate("UPDATE "+LONGLINES+" SET "+columnName+"="+(value ? "TRUE" : "FALSE")+" WHERE ID="+capture.getID())==0)
        {
            throw new SQLException(Resources.format(Clé.CATCH_NOT_FOUND¤1, capture));
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (update!=null) update.close();
        super.close();
    }
}
