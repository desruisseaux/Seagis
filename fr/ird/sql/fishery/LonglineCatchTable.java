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
import net.seas.util.XClass;
import fr.ird.animat.Species;
import fr.ird.resources.Resources;
import javax.media.jai.util.Range;


/**
 * Objet interrogeant la base de donn�es pour obtenir la liste des p�ches �
 * la palangre qu'elle contient. Ces p�ches pourront �tre s�lectionn�es dans
 * une certaine r�gion g�ographique et � certaines dates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class LonglineCatchTable extends AbstractCatchTable
{
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des p�ches.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
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

    // IMPORTANT: Les donn�es DOIVENT �tre class�es en ordre croissant de date
    //            du d�but de la p�che (StartTime), pour le bon fonctionnement
    //            de {@link Coupling}.

    /** Num�ro de colonne. */ static final int ID              =  1;
    /** Num�ro de colonne. */ static final int DATE            =  2;
    /** Num�ro de colonne. */ static final int START_LONGITUDE =  3;
    /** Num�ro de colonne. */ static final int START_LATITUDE  =  4;
    /** Num�ro de colonne. */ static final int END_LONGITUDE   =  5;
    /** Num�ro de colonne. */ static final int END_LATITUDE    =  6;
    /** Num�ro de colonne. */ static final int CATCHS          =  7;

    /** Num�ro d'argument. */ private static final int ARG_START_TIME  =  1;
    /** Num�ro d'argument. */ private static final int ARG_END_TIME    =  2;
    /** Num�ro d'argument. */ private static final int ARG_XMIN        =  3;
    /** Num�ro d'argument. */ private static final int ARG_YMIN        =  4;
    /** Num�ro d'argument. */ private static final int ARG_XMAX        =  5;
    /** Num�ro d'argument. */ private static final int ARG_YMAX        =  6;

    /**
     * Coordonn�es g�ographiques demand�es par l'utilisateur. Ces
     * coordonn�es sont sp�cifi�es par {@link #setGeographicArea}.
     * Ces coordonn�es peuvent �tre r�duites lors de l'appel de
     * {@link #packEnvelope}.
     */
    private final Rectangle2D geographicArea = new Rectangle2D.Double();

    /**
     * Date de d�but et de fin de la plage de temps demand�e par l'utilisateur.
     * Cette plage est sp�cifi�e par {@link #setTimeRange}. Cette plage peut
     * �tre r�duite lors de l'appel de {@link #packEnvelope}.
     */
    private long startTime, endTime;

    /**
     * Indique si la m�thode {@link #packEnvelope} a �t� appel�e.
     */
    private boolean packed;

    /**
     * Objet � utiliser pour les mises � jour. Cet
     * objet ne sera construit que la premi�re fois
     * o� il sera n�cessaire.
     */
    private transient Statement update;

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
    protected LonglineCatchTable(final Connection connection, final TimeZone timezone, final Set<Species> species) throws SQLException
    {
        super(connection.prepareStatement(completeQuery(preferences.get(LONGLINES, SQL_SELECT), LONGLINES, species)), timezone, species);
        setTimeRange(new Date(0), new Date());
        setGeographicArea(new Rectangle2D.Double(-180, -90, 360, 180));
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
//      statement.setDouble(ARG_XMIN, rect.getMinX());
//      statement.setDouble(ARG_XMAX, rect.getMaxX());
//      statement.setDouble(ARG_YMIN, rect.getMinY());
//      statement.setDouble(ARG_YMAX, rect.getMaxY());
        geographicArea.setRect(rect);
        packed=false;
    }

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion des captures.  Cette r�gion
     * ne sera pas plus grande que la r�gion qui a �t� sp�cifi�e lors du dernier appel
     * de la m�thode {@link #setGeographicArea}.  Elle peut toutefois �tre plus petite
     * de fa�on � n'englober que les donn�es de p�ches pr�sentes dans la base de donn�es.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized Rectangle2D getGeographicArea() throws SQLException
    {
        packEnvelope();
        return (Rectangle2D) geographicArea.clone();
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
        this.packed  = false;
    }

    /**
     * Retourne la plage de dates des p�ches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates sp�cifi�e lors du dernier appel de la m�thode {@link #setTimeRange}.
     * Elle peut toutefois �tre plus petite de fa�on � n'englober que les donn�es de p�ches
     * pr�sentes dans la base de donn�es.
     *
     * @param  La plage de dates des donn�es de p�ches. Cette plage sera constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized Range getTimeRange() throws SQLException
    {
        packEnvelope();
        return new Range(Date.class, new Date(startTime), new Date(endTime));
    }

    /**
     * Calcule les coordonn�es g�ographiques et la plage de temps couvertes
     * par les donn�es de p�ches. La plage de temps aura �t� sp�cifi�e avec
     * {@link #setTimeRange} et les limites de la r�gion g�ographique avec
     * {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    private void packEnvelope() throws SQLException
    {
        if (packed) return;

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
            list.add(new LonglineCatchEntry(this, result));
        }
        result.close();
        return list;
    }

    /**
     * Retourne un it�rateur qui balayera la liste des captures dans la r�gion et dans la
     * plage de dates pr�alablement s�lectionn�es. Ces plages auront �t� sp�cifi�es � l'aide
     * des diff�rentes m�thodes <code>set...</code> de cette classe. Cette m�thode ne retourne
     * jamais <code>null</code>, mais peut retourner un it�rateur qui ne balayera aucun �l�ment.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * D�finie une valeur r�elle pour une capture donn�es.  Cette m�thode peut �tre utilis�e
     * pour mettre � jour certaine informations relatives � la capture. La capture sp�cifi�e
     * doit exister dans la base de donn�es.
     *
     * @param capture    Capture � mettre � jour. Cette capture d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture sp�cifi�e n'existe pas, ou si la mise � jour
     *         de la base de donn�es a �chou�e pour une autre raison.
     */
    public synchronized void setValue(final CatchEntry capture, final String columnName, final float value) throws SQLException
    {
        if (update==null)
        {
            update = statement.getConnection().createStatement();
        }
        if (update.executeUpdate("UPDATE "+LONGLINES+" SET "+columnName+"="+value+" WHERE ID="+capture.getID())==0)
        {
            throw new SQLException(Resources.format(Cl�.CATCH_NOT_FOUND�1, capture));
        }
    }

    /**
     * D�finie une valeur bool�enne pour une capture donn�es. Cette m�thode peut �tre utilis�e
     * pour mettre � jour certaine informations relatives � la capture.   La capture sp�cifi�e
     * doit exister dans la base de donn�es.
     *
     * @param capture    Capture � mettre � jour. Cette capture d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture sp�cifi�e n'existe pas, ou si la mise � jour
     *         de la base de donn�es a �chou�e pour une autre raison.
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
            throw new SQLException(Resources.format(Cl�.CATCH_NOT_FOUND�1, capture));
        }
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (update!=null) update.close();
        super.close();
    }
}
