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
     * Tonnage minimal et maximal des captures � prendre en compte.
     */
    private static final Range DEFAULT_CATCH_RANGE = new Range(Double.class, new Double(0), null);

    /**
     * Requ�te utilis� pour mettre � jour un enregistrement.
     */
    static final String SQL_UPDATE = "UPDATE "+CATCHS+" SET [?]=? WHERE ID=?";

    /** Num�ro d'argument. */ private static final int ARG_VALUE = 1;
    /** Num�ro d'argument. */ private static final int ARG_ID    = 2;

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
     * Plage de capture � prendre en compte.
     */
    protected Range catchRange;

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
    private transient PreparedStatement update;

    /**
     * Derni�re colonne � avoir �t� mise � jour. Utilis� pour
     * d�terminer s'il faut reconstruire la requ�te {@link #update}.
     */
    private transient String lastColumnUpdated;

    /**
     * Construit une objet qui interrogera la
     * base de donn�es en utilisant la requ�te
     * sp�cifi�e.
     *
     * @param  connection Connection vers une base de donn�es de p�ches.
     * @param  statement Interrogation � soumettre � la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  species Ensemble des esp�ces demand�es.
     * @throws SQLException si <code>FisheryTable</code> n'a pas pu construire sa requ�te SQL.
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
     * Compl�te la requ�te SQL en ajoutant les noms de colonnes des esp�ces
     * sp�cifi�es juste avant la premi�re clause "FROM" dans la requ�te SQL.
     * Une condition bas�e sur les captures est aussi ajout�e.
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
                            "L'obtention du code de la FAO a �chou�e.", exception);
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
     * Sp�cifie l'ensemble des esp�ces � prendre en compte lors des interrogations de
     * la base de donn�es. Les objets {@link CatchEntry} retourn�s par cette table ne
     * contiendront des informations que sur ces esp�ces, et la m�thode {@link CatchEntry#getCatch()}
     * (qui retourne la quantit� totale de poisson captur�) ignorera toute esp�ce qui
     * n'apparait pas dans l'ensemble <code>species</code>.
     *
     * @param species Ensemble des esp�ces � prendre en compte.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final synchronized void setSpecies(final Set<Species> newSpecies) throws SQLException {
        if (!species.equals(newSpecies)) {
            final Rectangle2D      area = getGeographicArea();
            final Range       timeRange = getTimeRange();
            final Range      catchRange = getCatchRange();
            final Connection connection = statement.getConnection();
            statement.close();
            statement = null; // Au cas o� l'instruction suivante �chourait.
            statement = connection.prepareStatement(completeQuery(sqlSelect, newSpecies));
            this.species = new SpeciesSet(newSpecies);
            setCatchRange(catchRange);
            setTimeRange(timeRange);
            setGeographicArea(area);
        }
    }

    /**
     * Proc�de � l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException {
        if (false) {
            // Cette ligne aurait suffit si ce n'�tait du bug #4380653...
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
     * Retourne l'ensemble des esp�ces comprises dans la requ�te
     * de cette table. L'ensemble retourn� est immutable.
     */
    public final Set<Species> getSpecies() {
        return species;
    }

    /**
     * Retourne le syst�me de coordonn�es utilis�es
     * pour les positions de p�ches dans cette table.
     */
    public final CoordinateSystem getCoordinateSystem() {
        return GeographicCoordinateSystem.WGS84;
    }

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion des captures.  Cette r�gion
     * ne sera pas plus grande que la r�gion qui a �t� sp�cifi�e lors du dernier appel
     * de la m�thode {@link #setGeographicArea}.  Elle peut toutefois �tre plus petite
     * de fa�on � n'englober que les donn�es de p�ches pr�sentes dans la base de donn�es.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final synchronized Rectangle2D getGeographicArea() throws SQLException {
        if (!packed) {
            packEnvelope();
            packed = true;
        }
        return (Rectangle2D) geographicArea.clone();
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
     * Retourne la plage de dates des p�ches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates sp�cifi�e lors du dernier appel de la m�thode {@link #setTimeRange}.
     * Elle peut toutefois �tre plus petite de fa�on � n'englober que les donn�es de p�ches
     * pr�sentes dans la base de donn�es.
     *
     * @param  La plage de dates des donn�es de p�ches. Cette plage sera constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final synchronized Range getTimeRange() throws SQLException {
        if (!packed) {
            packEnvelope();
            packed = true;
        }
        return new Range(Date.class, new Date(startTime), new Date(endTime));
    }

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es de p�ches.
     * Toutes les p�ches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des donn�es.
     *         Cette plage doit �tre constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final void setTimeRange(final Range timeRange) throws SQLException {
        Date min = (Date)timeRange.getMinValue();
        Date max = (Date)timeRange.getMaxValue();
        if (min==null || max==null) {
            throw new UnsupportedOperationException("Les intervalles ouverts ne sont pas encore support�s");
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
     * Retourne la plage de valeurs de captures d'int�r�t. Il peut s'agit de captures
     * en tonn�es ou en nombre d'individus, d�pendament du type de p�che.
     */
    public Range getCatchRange() throws SQLException {
        return catchRange;
    }

    /**
     * D�finit la plage de valeurs de captures d'int�r�t.
     */
    public final synchronized void setCatchRange(final Range catchRange) throws SQLException {
        if (!catchRange.isMinIncluded() || !catchRange.isMaxIncluded()) {
            throw new UnsupportedOperationException("Les intervalles ouverts ne sont pas encore support�s");
        }
        final Number min = (Number)catchRange.getMinValue();
        final Number max = (Number)catchRange.getMaxValue();
        setCatchRange((min!=null) ? min.doubleValue() : Double.NEGATIVE_INFINITY,
                      (max!=null) ? max.doubleValue() : Double.POSITIVE_INFINITY);
        this.catchRange = catchRange;
    }

    /**
     * D�finit la plage de valeurs de captures d'int�r�t. Cette m�thode est �quivalente
     * � {@link #setCatchRange(Range)}. La valeur <code>maximum</code> peut �tre {@link
     * Double#POSITIVE_INFINITY}.
     *
     * @param minimum Capture minimale, inclusif. Ce nombre doit �tre positif.
     * @param maximum Capture maximale, inclusif. Peut �tre {@link Double#POSITIVE_INFINITY}.
     */
    public final synchronized void setCatchRange(double minimum, double maximum) throws SQLException {
        if (!(minimum>=0 && minimum<=maximum)) {
            throw new IllegalArgumentException();
        }
        if (!Double.isInfinite(maximum)) {
            throw new UnsupportedOperationException("Les limites sup�rieures ne sont pas encore impl�m�nt�es.");
        }
        setMinimumCatch(minimum);
        catchRange = new Range(Double.class, new Double(minimum), null);
    }

    /**
     * D�finit les captures minimales exig�es pour prendre en compte les captures.
     */
    abstract void setMinimumCatch(final double minimum) throws SQLException;

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
    public final void setValue(final CatchEntry capture, final String columnName, final float value)
        throws SQLException
    {
        setValue(capture, columnName, new Float(value));
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
    public final void setValue(final CatchEntry capture, final String columnName, final boolean value)
        throws SQLException
    {
        // Note: PostgreSQL demande que "TRUE" et "FALSE" soient
        //       en majuscules. MySQL n'a pas de type boolean.
        setValue(capture, columnName, (value ? Boolean.TRUE : Boolean.FALSE));
    }

    /**
     * Execute une requ�te de mise � jour pour une capture donn�es.
     *
     * @param capture    Capture � mettre � jour. Cette capture d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture sp�cifi�e n'existe pas, ou si la mise � jour
     *         de la base de donn�es a �chou�e pour une autre raison.
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
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
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
