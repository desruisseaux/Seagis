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

// Java DataBase Connectivity
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Time
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

// Geographic coordinates
import java.awt.geom.Rectangle2D;
import org.geotools.resources.Utilities;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;

// Utilities
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Miscellaneous
import fr.ird.sql.DataBase;
import fr.ird.animat.Species;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;
import javax.media.jai.util.Range;


/**
 * Base class for {@link CatchTable} implementations. {@link CatchTable}
 * allows for querying fishery data in some pre-selected geographic area
 * and time range. This base class is suitable both for longline and seine
 * fisheries.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class AbstractCatchTable extends Table implements CatchTable
{
    /**
     * The SQL instruction to use for query fishery data. The "SELECT" clause
     * in this instruction <strong>do not</strong> include species. Species
     * will be added on the fly when needed.
     */
    private final String sqlSelect;

    /**
     * The table name.
     */
    private final String table;

    /**
     * The calendar for computing dates. This calendar
     * use the time zone specified at construction time.
     */
    protected final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Ensemble immutable des esp�ces. Le contenu d'un objet {@link SpeciesSet} ne doit
     * pas changer. Toutefois, <code>species</code> pourra se r�f�rer � d'autres objets
     * {@link SpeciesSet}. Les objets {@link SpeciesSet} enveloppe la liste des esp�ces
     * dans un tableau ({@link SpeciesSet#species}) qui sera acc�d� directement par les
     * classes {@link AbstractCatchEntry} et d�riv�es.
     */
    protected SpeciesSet species;

    /**
     * Coordonn�es g�ographiques demand�es par l'utilisateur. Ces
     * coordonn�es sont sp�cifi�es par {@link #setGeographicArea}.
     * Ces coordonn�es peuvent �tre r�duites lors de l'appel de
     * {@link #packEnvelope}.
     */
    protected final Rectangle2D geographicArea = new Rectangle2D.Double();

    /**
     * Date de d�but et de fin de la plage de temps demand�e par l'utilisateur.
     * Cette plage est sp�cifi�e par {@link #setTimeRange}. Cette plage peut
     * �tre r�duite lors de l'appel de {@link #packEnvelope}.
     */
    protected long startTime, endTime;

    /**
     * Indique si la m�thode {@link #packEnvelope} a �t� appel�e. Ce
     * champ <strong>doit</strong> �tre remis � <code>false</code> par les
     * m�thodes <code>setTimeRange</code> and <code>setGeographicArea</code>.
     */
    protected boolean packed;

    /**
     * Objet � utiliser pour les mises � jour. Cet
     * objet ne sera construit que la premi�re fois
     * o� il sera n�cessaire.
     */
    private transient Statement update;

    /**
     * Construit une objet qui interrogera la
     * base de donn�es en utilisant la requ�te
     * sp�cifi�e.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  table Le nom de la table dans laquelle puiser les donn�es.
     * @param  statement Interrogation � soumettre � la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  species Ensemble des esp�ces demand�es.
     * @throws SQLException si <code>FisheryTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected AbstractCatchTable(final Connection connection, final String table, final String statement, final TimeZone timezone, final Set<Species> species) throws SQLException
    {
        super(connection.prepareStatement(completeQuery(statement, table, species)));
        this.table     = table;
        this.sqlSelect = statement;
        this.species   = new SpeciesSet(species);
        this.calendar  = new GregorianCalendar(timezone);

        setTimeRange(new Date(0), new Date());
        setGeographicArea(new Rectangle2D.Double(-180, -90, 360, 180));
    }

    /**
     * Compl�te la requ�te SQL en ajouter les noms de colonnes des esp�ces
     * sp�cifi�es juste avant la premi�re clause "FROM" dans la requ�te SQL.
     */
    private static String completeQuery(String query, final String table, final Set<Species> species)
    {
        int index = query.toUpperCase().indexOf("FROM");
        if (index>=0)
        {
            while (index>=1 && Character.isWhitespace(query.charAt(index-1))) index--;
            final StringBuffer buffer=new StringBuffer(query.substring(0, index));
            for (final Iterator<Species> it=species.iterator(); it.hasNext();)
            {
                final String name = it.next().getName(null);
                if (name!=null)
                {
                    buffer.append(", ");
                    buffer.append(table);
                    buffer.append('.');
                    buffer.append(name);
                }
            }
            buffer.append(query.substring(index));
            query=buffer.toString();
        }
        return query;
    }

    /**
     * Sp�cifie l'ensemble des esp�ces � prendre en compte lors des interrogations de
     * la base de donn�es. Les objets {@link CatchEntry} retourn�s par cette table ne
     * contiendront des informations que sur ces esp�ces, et la m�thode {@link CatchEntry#getCatch()}
     * (qui retourne la quantit� totale de poisson captur�) ignorera toute esp�ce qui
     * n'apparait pas dans l'ensemble <code>species</code>.
     *
     * @param species Ensemble des esp�ces � prendre en compte.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final synchronized void setSpecies(final Set<Species> newSpecies) throws SQLException
    {
        if (!species.equals(newSpecies))
        {
            final Rectangle2D area = getGeographicArea();
            final Range  timeRange = getTimeRange();
            final Connection connection = statement.getConnection();
            statement.close();
            statement = null; // Au cas o� l'instruction suivante �chourait.
            statement = connection.prepareStatement(completeQuery(sqlSelect, table, newSpecies));
            this.species = new SpeciesSet(newSpecies);
            setTimeRange(timeRange);
            setGeographicArea(area);
        }
    }

    /**
     * Proc�de � l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException
    {
        if (false)
        {
            // Cette ligne aurait suffit si ce n'�tait du bug #4380653...
            return result.getTimestamp(field, calendar);
        }
        else
        {
            if (localCalendar==null)
                localCalendar=new GregorianCalendar();
            Date date;
            try
            {
                date=result.getTimestamp(field, localCalendar);
            }
            catch (SQLException exception)
            {
                if (Utilities.getShortClassName(exception).startsWith("NotImplemented"))
                {
                    // Workaround for a bug in MySQL's JDBC:
                    // org.gjt.mm.mysql.jdbc2.NotImplemented
                    date=result.getTimestamp(field);
                }
                else throw exception;
            }
            localCalendar.setTime(date);
            calendar.     setTime(date);
            calendar.set(Calendar.ERA,         localCalendar.get(Calendar.ERA        ));
            calendar.set(Calendar.YEAR,        localCalendar.get(Calendar.YEAR       ));
            calendar.set(Calendar.DAY_OF_YEAR, localCalendar.get(Calendar.DAY_OF_YEAR));
            calendar.set(Calendar.HOUR_OF_DAY, localCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE,      localCalendar.get(Calendar.MINUTE     ));
            calendar.set(Calendar.SECOND,      localCalendar.get(Calendar.SECOND     ));
            calendar.set(Calendar.MILLISECOND, localCalendar.get(Calendar.MILLISECOND));
            return calendar.getTime();
        }
    }

    /**
     * Retourne l'ensemble des esp�ces comprises dans la requ�te
     * de cette table. L'ensemble retourn� est immutable.
     */
    public final Set<Species> getSpecies()
    {return species;}

    /**
     * Retourne le syst�me de coordonn�es utilis�es
     * pour les positions de p�ches dans cette table.
     */
    public final CoordinateSystem getCoordinateSystem()
    {return GeographicCoordinateSystem.WGS84;}

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion des captures.  Cette r�gion
     * ne sera pas plus grande que la r�gion qui a �t� sp�cifi�e lors du dernier appel
     * de la m�thode {@link #setGeographicArea}.  Elle peut toutefois �tre plus petite
     * de fa�on � n'englober que les donn�es de p�ches pr�sentes dans la base de donn�es.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final synchronized Rectangle2D getGeographicArea() throws SQLException
    {
        if (!packed)
        {
            packEnvelope();
            packed = true;
        }
        return (Rectangle2D) geographicArea.clone();
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
    public final synchronized Range getTimeRange() throws SQLException
    {
        if (!packed)
        {
            packEnvelope();
            packed = true;
        }
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
    protected abstract void packEnvelope() throws SQLException;

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es de p�ches.
     * Toutes les p�ches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des donn�es.
     *         Cette plage doit �tre constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final void setTimeRange(final Range timeRange) throws SQLException
    {setTimeRange((Date)timeRange.getMinValue(), (Date)timeRange.getMaxValue());}

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
    public final void setValue(final CatchEntry capture, final String columnName, final float value) throws SQLException
    {
        setValue(capture, "UPDATE "+table+" SET "+columnName+"="+value+" WHERE ID="+capture.getID());
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
    public final void setValue(final CatchEntry capture, final String columnName, final boolean value) throws SQLException
    {
        // Note: PostgreSQL demande que "TRUE" et "FALSE" soient en majuscules. MySQL n'a pas de type boolean.
        setValue(capture, "UPDATE "+table+" SET "+columnName+"="+(value ? "TRUE" : "FALSE")+" WHERE ID="+capture.getID());
    }

    /**
     * Execute une requ�te de mise � jour pour une capture donn�es.
     *
     * @param  capture Capture � mettre � jour.
     * @param  sql Requ�te � ex�cuter.
     * @throws Si la mise � jour de la base de donn�es a �chou�e.
     */
    private synchronized void setValue(final CatchEntry capture, final String sql) throws SQLException
    {
        if (update==null)
        {
            update = statement.getConnection().createStatement();
        }
        if (update.executeUpdate(sql)==0)
        {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_CATCH_NOT_FOUND_$1, capture));
        }
        final LogRecord record = new LogRecord(DataBase.SQL_UPDATE, sql);
        record.setSourceClassName ("CatchTable");
        record.setSourceMethodName("setValue");
        logger.log(record);
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public final synchronized void close() throws SQLException
    {
        if (update!=null) update.close();
        super.close();
    }
}
