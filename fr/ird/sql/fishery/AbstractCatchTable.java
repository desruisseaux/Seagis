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
     * Ensemble immutable des espèces. Le contenu d'un objet {@link SpeciesSet} ne doit
     * pas changer. Toutefois, <code>species</code> pourra se référer à d'autres objets
     * {@link SpeciesSet}. Les objets {@link SpeciesSet} enveloppe la liste des espèces
     * dans un tableau ({@link SpeciesSet#species}) qui sera accédé directement par les
     * classes {@link AbstractCatchEntry} et dérivées.
     */
    protected SpeciesSet species;

    /**
     * Coordonnées géographiques demandées par l'utilisateur. Ces
     * coordonnées sont spécifiées par {@link #setGeographicArea}.
     * Ces coordonnées peuvent être réduites lors de l'appel de
     * {@link #packEnvelope}.
     */
    protected final Rectangle2D geographicArea = new Rectangle2D.Double();

    /**
     * Date de début et de fin de la plage de temps demandée par l'utilisateur.
     * Cette plage est spécifiée par {@link #setTimeRange}. Cette plage peut
     * être réduite lors de l'appel de {@link #packEnvelope}.
     */
    protected long startTime, endTime;

    /**
     * Indique si la méthode {@link #packEnvelope} a été appelée. Ce
     * champ <strong>doit</strong> être remis à <code>false</code> par les
     * méthodes <code>setTimeRange</code> and <code>setGeographicArea</code>.
     */
    protected boolean packed;

    /**
     * Objet à utiliser pour les mises à jour. Cet
     * objet ne sera construit que la première fois
     * où il sera nécessaire.
     */
    private transient Statement update;

    /**
     * Construit une objet qui interrogera la
     * base de données en utilisant la requête
     * spécifiée.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  table Le nom de la table dans laquelle puiser les données.
     * @param  statement Interrogation à soumettre à la base de données.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @param  species Ensemble des espèces demandées.
     * @throws SQLException si <code>FisheryTable</code> n'a pas pu construire sa requête SQL.
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
     * Complète la requète SQL en ajouter les noms de colonnes des espèces
     * spécifiées juste avant la première clause "FROM" dans la requête SQL.
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
     * Spécifie l'ensemble des espèces à prendre en compte lors des interrogations de
     * la base de données. Les objets {@link CatchEntry} retournés par cette table ne
     * contiendront des informations que sur ces espèces, et la méthode {@link CatchEntry#getCatch()}
     * (qui retourne la quantité totale de poisson capturé) ignorera toute espèce qui
     * n'apparait pas dans l'ensemble <code>species</code>.
     *
     * @param species Ensemble des espèces à prendre en compte.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public final synchronized void setSpecies(final Set<Species> newSpecies) throws SQLException
    {
        if (!species.equals(newSpecies))
        {
            final Rectangle2D area = getGeographicArea();
            final Range  timeRange = getTimeRange();
            final Connection connection = statement.getConnection();
            statement.close();
            statement = null; // Au cas où l'instruction suivante échourait.
            statement = connection.prepareStatement(completeQuery(sqlSelect, table, newSpecies));
            this.species = new SpeciesSet(newSpecies);
            setTimeRange(timeRange);
            setGeographicArea(area);
        }
    }

    /**
     * Procède à l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException
    {
        if (false)
        {
            // Cette ligne aurait suffit si ce n'était du bug #4380653...
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
     * Retourne l'ensemble des espèces comprises dans la requête
     * de cette table. L'ensemble retourné est immutable.
     */
    public final Set<Species> getSpecies()
    {return species;}

    /**
     * Retourne le système de coordonnées utilisées
     * pour les positions de pêches dans cette table.
     */
    public final CoordinateSystem getCoordinateSystem()
    {return GeographicCoordinateSystem.WGS84;}

    /**
     * Retourne les coordonnées géographiques de la région des captures.  Cette région
     * ne sera pas plus grande que la région qui a été spécifiée lors du dernier appel
     * de la méthode {@link #setGeographicArea}.  Elle peut toutefois être plus petite
     * de façon à n'englober que les données de pêches présentes dans la base de données.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
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
     * Retourne la plage de dates des pêches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates spécifiée lors du dernier appel de la méthode {@link #setTimeRange}.
     * Elle peut toutefois être plus petite de façon à n'englober que les données de pêches
     * présentes dans la base de données.
     *
     * @param  La plage de dates des données de pêches. Cette plage sera constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
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
     * Calcule les coordonnées géographiques et la plage de temps couvertes
     * par les données de pêches. La plage de temps aura été spécifiée avec
     * {@link #setTimeRange} et les limites de la région géographique avec
     * {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    protected abstract void packEnvelope() throws SQLException;

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données de pêches.
     * Toutes les pêches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des données.
     *         Cette plage doit être constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public final void setTimeRange(final Range timeRange) throws SQLException
    {setTimeRange((Date)timeRange.getMinValue(), (Date)timeRange.getMaxValue());}

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
    public final void setValue(final CatchEntry capture, final String columnName, final float value) throws SQLException
    {
        setValue(capture, "UPDATE "+table+" SET "+columnName+"="+value+" WHERE ID="+capture.getID());
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
    public final void setValue(final CatchEntry capture, final String columnName, final boolean value) throws SQLException
    {
        // Note: PostgreSQL demande que "TRUE" et "FALSE" soient en majuscules. MySQL n'a pas de type boolean.
        setValue(capture, "UPDATE "+table+" SET "+columnName+"="+(value ? "TRUE" : "FALSE")+" WHERE ID="+capture.getID());
    }

    /**
     * Execute une requête de mise à jour pour une capture données.
     *
     * @param  capture Capture à mettre à jour.
     * @param  sql Requête à exécuter.
     * @throws Si la mise à jour de la base de données a échouée.
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
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public final synchronized void close() throws SQLException
    {
        if (update!=null) update.close();
        super.close();
    }
}
