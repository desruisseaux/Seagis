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

// Utilities
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.media.jai.util.Range;
import java.rmi.RemoteException;

// Geographic coordinates
import java.awt.geom.Rectangle2D;
import org.geotools.resources.Utilities;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;

// Seagis
import fr.ird.sql.DataBase;
import fr.ird.animat.Species;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Base class for {@link CatchTable} implementations. {@link CatchTable}
 * allows for querying fishery data in some pre-selected geographic area
 * and time range. This base class is suitable both for longline and seine
 * fisheries.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class AbstractCatchTable extends Table implements CatchTable {
    /**
     * Tonnage minimal et maximal des captures à prendre en compte.
     */
    private static final Range DEFAULT_CATCH_RANGE = new Range(Double.class, new Double(0), null);

    /**
     * Requête utilisé pour mettre à jour un enregistrement.
     */
    static final String SQL_UPDATE = "UPDATE "+CATCHS+" SET [?]=? WHERE ID=?";

    /** Numéro d'argument. */ private static final int ARG_VALUE = 1;
    /** Numéro d'argument. */ private static final int ARG_ID    = 2;

    /**
     * The SQL instruction to use for query fishery data. The "SELECT" clause
     * in this instruction <strong>do not</strong> include species. Species
     * will be added on the fly when needed.
     */
    private final String sqlSelect;

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
     * Plage de capture à prendre en compte.
     */
    protected Range catchRange;

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
    private transient PreparedStatement update;

    /**
     * Dernière colonne à avoir été mise à jour. Utilisé pour
     * déterminer s'il faut reconstruire la requête {@link #update}.
     */
    private transient String lastColumnUpdated;

    /**
     * Construit une objet qui interrogera la
     * base de données en utilisant la requête
     * spécifiée.
     *
     * @param  connection Connection vers une base de données de pêches.
     * @param  statement Interrogation à soumettre à la base de données.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @param  species Ensemble des espèces demandées.
     * @throws SQLException si <code>FisheryTable</code> n'a pas pu construire sa requête SQL.
     */
    protected AbstractCatchTable(final Connection   connection,
                                 final String       statement,
                                 final TimeZone     timezone,
                                 final Set<Species> species) throws SQLException
    {
        super(connection.prepareStatement(completeQuery(statement, species)));
        this.sqlSelect = statement;
        this.species   = new SpeciesSet(species);
        this.calendar  = new GregorianCalendar(timezone);

        setTimeRange(new Date(0), new Date());
        setGeographicArea(new Rectangle2D.Double(-180, -90, 360, 180));
        setCatchRange(DEFAULT_CATCH_RANGE);
    }

    /**
     * Complète la requète SQL en ajoutant les noms de colonnes des espèces
     * spécifiées juste avant la première clause "FROM" dans la requête SQL.
     * Une condition basée sur les captures est aussi ajoutée.
     */
    private static String completeQuery(String query, final Set<Species> species)
            throws SQLException
    {
        final String[] columns = new String[species.size()];
        int index=0;
        for (final Species sp : species) {
            try {
                columns[index++] = sp.getName(Species.FAO);
            } catch (RemoteException exception) {
                throw new fr.ird.sql.RemoteException(
                            "L'obtention du code de la FAO a échouée.", exception);
            }
        }
        assert index == columns.length;
        query = completeSelect(query, columns);
        /*
         * Ajoute une condition "total".
         */
        final String total = "total";
        index = indexOfWord(query, total);
        if (index>0 && index+1<query.length() &&
            !Character.isUnicodeIdentifierPart(query.charAt(index-1)) &&
            !Character.isUnicodeIdentifierPart(query.charAt(index+total.length())))
        {
            final StringBuffer buffer = new StringBuffer(query.substring(0, index));
            boolean additional = false;
            buffer.append('(');
            if (columns.length != 0) {
                for (int i=0; i<columns.length; i++) {
                    final String name = columns[i];
                    if (name != null) {
                        if (additional) {
                            buffer.append('+');
                        }
                        buffer.append('[');
                        buffer.append(name);
                        buffer.append(']');
                        additional = true;
                    }
                }
            } else {
                buffer.append('0');
            }
            buffer.append(')');
            buffer.append(query.substring(index+total.length()));
            query = buffer.toString();
        }
        final LogRecord record = new LogRecord(DataBase.SQL_SELECT, query);
        record.setSourceClassName ("CatchTable");
        record.setSourceMethodName("setSpecies");
        logger.log(record);
        System.out.println(query);
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
    public final synchronized void setSpecies(final Set<Species> newSpecies) throws SQLException {
        if (!species.equals(newSpecies)) {
            final Rectangle2D      area = getGeographicArea();
            final Range       timeRange = getTimeRange();
            final Range      catchRange = getCatchRange();
            final Connection connection = statement.getConnection();
            statement.close();
            statement = null; // Au cas où l'instruction suivante échourait.
            statement = connection.prepareStatement(completeQuery(sqlSelect, newSpecies));
            this.species = new SpeciesSet(newSpecies);
            setCatchRange(catchRange);
            setTimeRange(timeRange);
            setGeographicArea(area);
        }
    }

    /**
     * Procède à l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException {
        if (false) {
            // Cette ligne aurait suffit si ce n'était du bug #4380653...
            return result.getTimestamp(field, calendar);
        } else {
            if (localCalendar==null) {
                localCalendar=new GregorianCalendar();
            }
            Date date;
            try {
                date=result.getTimestamp(field, localCalendar);
            } catch (SQLException exception) {
                if (Utilities.getShortClassName(exception).startsWith("NotImplemented")) {
                    // Workaround for a bug in MySQL's JDBC:
                    // org.gjt.mm.mysql.jdbc2.NotImplemented
                    date=result.getTimestamp(field);
                } else {
                    throw exception;
                }
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
    public final Set<Species> getSpecies() {
        return species;
    }

    /**
     * Retourne le système de coordonnées utilisées
     * pour les positions de pêches dans cette table.
     */
    public final CoordinateSystem getCoordinateSystem() {
        return GeographicCoordinateSystem.WGS84;
    }

    /**
     * Retourne les coordonnées géographiques de la région des captures.  Cette région
     * ne sera pas plus grande que la région qui a été spécifiée lors du dernier appel
     * de la méthode {@link #setGeographicArea}.  Elle peut toutefois être plus petite
     * de façon à n'englober que les données de pêches présentes dans la base de données.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public final synchronized Rectangle2D getGeographicArea() throws SQLException {
        if (!packed) {
            packEnvelope();
            packed = true;
        }
        return (Rectangle2D) geographicArea.clone();
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
     * Retourne la plage de dates des pêches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates spécifiée lors du dernier appel de la méthode {@link #setTimeRange}.
     * Elle peut toutefois être plus petite de façon à n'englober que les données de pêches
     * présentes dans la base de données.
     *
     * @param  La plage de dates des données de pêches. Cette plage sera constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public final synchronized Range getTimeRange() throws SQLException {
        if (!packed) {
            packEnvelope();
            packed = true;
        }
        return new Range(Date.class, new Date(startTime), new Date(endTime));
    }

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données de pêches.
     * Toutes les pêches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des données.
     *         Cette plage doit être constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public final void setTimeRange(final Range timeRange) throws SQLException {
        Date min = (Date)timeRange.getMinValue();
        Date max = (Date)timeRange.getMaxValue();
        if (min==null || max==null) {
            throw new UnsupportedOperationException("Les intervalles ouverts ne sont pas encore supportés");
        }
        if (!timeRange.isMinIncluded()) {
            min = new Date(min.getTime()+1);
        }
        if (!timeRange.isMaxIncluded()) {
            max = new Date(max.getTime()-1);
        }
        setTimeRange(min, max);
    }

    /**
     * Retourne la plage de valeurs de captures d'intérêt. Il peut s'agit de captures
     * en tonnées ou en nombre d'individus, dépendament du type de pêche.
     */
    public Range getCatchRange() throws SQLException {
        return catchRange;
    }

    /**
     * Définit la plage de valeurs de captures d'intérêt.
     */
    public final synchronized void setCatchRange(final Range catchRange) throws SQLException {
        if (!catchRange.isMinIncluded() || !catchRange.isMaxIncluded()) {
            throw new UnsupportedOperationException("Les intervalles ouverts ne sont pas encore supportés");
        }
        final Number min = (Number)catchRange.getMinValue();
        final Number max = (Number)catchRange.getMaxValue();
        setCatchRange((min!=null) ? min.doubleValue() : Double.NEGATIVE_INFINITY,
                      (max!=null) ? max.doubleValue() : Double.POSITIVE_INFINITY);
        this.catchRange = catchRange;
    }

    /**
     * Définit la plage de valeurs de captures d'intérêt. Cette méthode est équivalente
     * à {@link #setCatchRange(Range)}. La valeur <code>maximum</code> peut être {@link
     * Double#POSITIVE_INFINITY}.
     *
     * @param minimum Capture minimale, inclusif. Ce nombre doit être positif.
     * @param maximum Capture maximale, inclusif. Peut être {@link Double#POSITIVE_INFINITY}.
     */
    public final synchronized void setCatchRange(double minimum, double maximum) throws SQLException {
        if (!(minimum>=0 && minimum<=maximum)) {
            throw new IllegalArgumentException();
        }
        if (!Double.isInfinite(maximum)) {
            throw new UnsupportedOperationException("Les limites supérieures ne sont pas encore impléméntées.");
        }
        setMinimumCatch(minimum);
        catchRange = new Range(Double.class, new Double(minimum), null);
    }

    /**
     * Définit les captures minimales exigées pour prendre en compte les captures.
     */
    abstract void setMinimumCatch(final double minimum) throws SQLException;

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
    public final void setValue(final CatchEntry capture, final String columnName, final float value)
        throws SQLException
    {
        setValue(capture, columnName, new Float(value));
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
    public final void setValue(final CatchEntry capture, final String columnName, final boolean value)
        throws SQLException
    {
        // Note: PostgreSQL demande que "TRUE" et "FALSE" soient
        //       en majuscules. MySQL n'a pas de type boolean.
        setValue(capture, columnName, (value ? Boolean.TRUE : Boolean.FALSE));
    }

    /**
     * Execute une requête de mise à jour pour une capture données.
     *
     * @param capture    Capture à mettre à jour. Cette capture définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture spécifiée n'existe pas, ou si la mise à jour
     *         de la base de données a échouée pour une autre raison.
     */
    private synchronized void setValue(final CatchEntry capture,
                                       final String columnName,
                                       final Object value)
        throws SQLException
    {
        if (!columnName.equals(lastColumnUpdated)) {
            if (update != null) {
                update.close();
                update = null;
                lastColumnUpdated = null;
            }
            String query = replaceQuestionMark(preferences.get(CATCHS+":UPDATE", SQL_UPDATE), columnName);
            update = statement.getConnection().prepareStatement(query);
            lastColumnUpdated = columnName;
        }
        update.setObject(ARG_VALUE, value);
        update.setInt(ARG_ID, capture.getID());
        if (update.executeUpdate() == 0) {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_CATCH_NOT_FOUND_$1, capture));
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
    public final synchronized void close() throws SQLException {
        if (update != null) {
            update.close();
            lastColumnUpdated = null;
        }
        super.close();
    }
}
