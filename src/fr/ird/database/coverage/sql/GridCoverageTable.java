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
package fr.ird.database.coverage.sql;

// Base de données
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Géométrie
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;

// Temps
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;

// Collections
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

// Divers
import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.media.jai.ParameterList;
import javax.media.jai.util.Range;

// Geotools dependencies (CTS)
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;

// Geotools dependencies (GCS)
import org.geotools.gp.Operation;
import org.geotools.gp.OperationNotFoundException;
import org.geotools.gc.GridCoverage;
import org.geotools.util.RangeSet;

// Geotools dependencies (resources)
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.geometry.XDimension2D;
import org.geotools.resources.geometry.XRectangle2D;

// Seagis
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.CoverageComparator;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;


/**
 * Connection vers une table d'images. Cette table contient des références vers des images sous
 * forme d'objets {@link CoverageEntry}.  Une table <code>GridCoverageTable</code> est capable
 * de fournir la liste des entrés {@link CoverageEntry} qui interceptent une certaines région
 * géographique et une certaine plage de dates.
 *
 * @see CoverageDataBase#getGridCoverageTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class GridCoverageTable extends Table implements CoverageTable {
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des images.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes {@link #SERIES}, {@link #FILENAME} et compagnie.
     */
    static final String SQL_SELECT =
                    "SELECT "+  /*[01] ID         */ GRID_COVERAGES+".ID, "          +
                                /*[02] SERIES     */                 "series, "      +
                                /*[03] PATHNAME   */                 "pathname, "    +
                                /*[04] FILENAME   */                 "filename, "    +
                                /*[05] START_TIME */                 "start_time, "  +
                                /*[06] END_TIME   */                 "end_time, "    +
                                /*[07] XMIN       */                 "xmin, "        +
                                /*[08] XMAX       */                 "xmax, "        +
                                /*[09] YMIN       */                 "ymin, "        +
                                /*[10] YMAX       */                 "ymax, "        +
                                /*[11] WIDTH      */                 "width, "       +
                                /*[12] HEIGHT     */                 "height, "      +
                                /*[13] CS         */                 "coordinate_system, " +
                                /*[14] FORMAT     */                 "format\n"      +

                    "FROM ("+GRID_COVERAGES+
                    " INNER JOIN "+GRID_GEOMETRIES+" ON "+GRID_COVERAGES+".geometry="+GRID_GEOMETRIES+".ID)" +
                    " INNER JOIN "+SUBSERIES+      " ON "+GRID_COVERAGES+".subseries="     +SUBSERIES+".ID\n"+

                    "WHERE (xmax>? AND xmin<? AND ymax>? AND ymin<?) "+
                      "AND (((end_time Is Null) OR end_time>=?) AND ((start_time Is Null) OR start_time<=?)) "+
                      "AND series=?\n"+
                      "ORDER BY end_time, subseries"; // DOIT être en ordre chronologique.
                                                      // Voir {@link GridCoverageEntry#compare}.

    /** Numéro de colonne. */ static final int ID                =  1;
    /** Numéro de colonne. */ static final int SERIES            =  2;
    /** Numéro de colonne. */ static final int PATHNAME          =  3;
    /** Numéro de colonne. */ static final int FILENAME          =  4;
    /** Numéro de colonne. */ static final int START_TIME        =  5;
    /** Numéro de colonne. */ static final int END_TIME          =  6;
    /** Numéro de colonne. */ static final int XMIN              =  7;
    /** Numéro de colonne. */ static final int XMAX              =  8;
    /** Numéro de colonne. */ static final int YMIN              =  9;
    /** Numéro de colonne. */ static final int YMAX              = 10;
    /** Numéro de colonne. */ static final int WIDTH             = 11;
    /** Numéro de colonne. */ static final int HEIGHT            = 12;
    /** Numéro de colonne. */ static final int COORDINATE_SYSTEM = 13;
    /** Numéro de colonne. */ static final int FORMAT            = 14;

    /** Numéro d'argument. */ private static final int ARG_XMIN       = 1;
    /** Numéro d'argument. */ private static final int ARG_XMAX       = 2;
    /** Numéro d'argument. */ private static final int ARG_YMIN       = 3;
    /** Numéro d'argument. */ private static final int ARG_YMAX       = 4;
    /** Numéro d'argument. */ private static final int ARG_START_TIME = 5;
    /** Numéro d'argument. */ private static final int ARG_END_TIME   = 6;
    /** Numéro d'argument. */ private static final int ARG_SERIES     = 7;

    /**
     * Réference vers la série d'images. Cette référence
     * est construite à partir du champ ID dans la table
     * "Series" de la base de données.
     */
    private fr.ird.database.coverage.SeriesEntry series;

    /**
     * L'opération à appliquer sur les images lue,
     * ou <code>null</code> s'il n'y en a aucune.
     */
    private Operation operation;

    /**
     * Paramètres de l'opération, ou <code>null</code>
     * s'il n'y a pas d'opération.
     */
    private ParameterList opParam;

    /**
     * Dimension logique (en degrés de longitude et de latitude) désirée des pixels
     * de l'images. Cette information n'est qu'approximative. Il n'est pas garantie
     * que les lectures produiront effectivement des images de cette résolution.
     * Une valeur nulle signifie que les lectures doivent se faire avec la meilleure
     * résolution possible.
     */
    private Dimension2D resolution;

    /**
     * Coordonnées géographiques de la région d'intéret, en degrés de longitude et de latitude.
     * Ces coordonnées peuvent être spécifiées par un appel à {@link #setGeographicArea}.  Les
     * objets {@link Rectangle2D} affectés à <code>geographicArea</code> peuvent changer, mais
     * les coordonnées d'un objet {@link Rectangle2D} donné ne doivent PAS être changées.   La
     * classe <code>GridCoverageEntry</code> se réferera directement à ce champ afin d'éviter la
     * création d'une multitude de clones.
     */
    private Rectangle2D geographicArea;

    /**
     * Date du début de la plage de temps des images
     * recherchées par cet objet <code>GridCoverageTable</code>.
     */
    private long startTime;

    /**
     * Date du fin de la plage de temps des images
     * recherchées par cet objet <code>GridCoverageTable</code>.
     */
    private long endTime;

    /**
     * Calendrier utilisé pour préparer les dates. Ce calendrier
     * utilisera le fuseau horaire spécifié lors de la construction.
     */
    private final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Fuseau horaire de la base de données.
     */
    protected final TimeZone timezone;

    /**
     * Formatteur à utiliser pour écrire des dates pour l'utilisateur. Les caractères et
     * les conventions linguistiques dépendront de la langue de l'utilisateur. Toutefois,
     * le fuseau horaire devrait être celui de la région d'étude plutôt que celui du pays
     * de l'utilisateur.
     */
    private final DateFormat dateFormat;

    /**
     * Table des systèmes de coordonnées. Ne sera construit que la première fois où elle
     * sera nécessaire.
     */
    private transient CoordinateSystemTable coordinateSystemTable;

    /**
     * Table des formats. Cette table ne sera construite que la première fois
     * où elle sera nécessaire.  Elle sera ensuite fermée chaque fois qu'elle
     * n'est plus utilisée pour économiser des ressources.
     */
    private transient FormatTable formatTable;

    /**
     * Ensemble des formats déjà lue. Autant que possible,
     * on réutilisera les formats qui ont déjà été créés.
     */
    private final Map<Integer,FormatEntry> formats = new HashMap<Integer,FormatEntry>();

    /**
     * Requète SQL faisant le lien avec la base de données.
     */
    private final PreparedStatement statement;

    /**
     * Table d'images pouvant être récupérée par leur numéro ID.
     * Cette table ne sera construite que si elle est nécessaire.
     */
    private transient PreparedStatement imageByID;

    /**
     * Table d'images pouvant être récupérée par leur nom.
     * Cette table ne sera construite que si elle est nécessaire.
     */
    private transient PreparedStatement imageByName;

    /**
     * Derniers paramètres à avoir été construit. Ces paramètres sont
     * retenus afin d'éviter d'avoir à les reconstruires trop souvent
     * si c'est évitable.
     */
    private transient Parameters parameters;

    /**
     * Construit une table des images en utilisant la connection spécifiée.
     * L'appellant <strong>doit</strong> appeler {@link #setSeries},
     * {@link #setGeographicArea} et {@link #setTimeRange} avant d'utiliser
     * cette table.
     *
     * @param  connection Connection vers une base de données d'images.
     * @param  timezone   Fuseau horaire des dates inscrites dans la base
     *                    de données. Cette information est utilisée pour
     *                    convertir en heure UTC les dates écrites dans la
     *                    base de données.
     * @throws SQLException si <code>GridCoverageTable</code> n'a pas pu construire sa requête SQL.
     */
    GridCoverageTable(final Connection connection, final TimeZone timezone) throws SQLException {
        statement = connection.prepareStatement(PREFERENCES.get(GRID_COVERAGES, SQL_SELECT));
        this.timezone   = timezone;
        this.calendar   = new GregorianCalendar(timezone);
        this.dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        this.dateFormat.setCalendar(calendar);
    }

    /**
     * {@inheritDoc}
     */
    public final fr.ird.database.coverage.SeriesEntry getSeries() {
        return series;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setSeries(final fr.ird.database.coverage.SeriesEntry series)
            throws SQLException
    {
        if (!series.equals(this.series)) {
            final boolean toLog = (this.series!=null);
            parameters = null;
            statement.setInt(ARG_SERIES, series.getID());
            this.series = series;
            if (toLog) {
                // Don't log if this object is configured by CoverageDataBase.
                log("setSeries", Level.CONFIG, ResourceKeys.SET_SERIES_$1, series.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final CoordinateSystem getCoordinateSystem() {
        return CoordinateSystemTable.WGS84;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Envelope getEnvelope() {
        final Envelope envelope = new Envelope(3);
        envelope.setRange(0, geographicArea.getMinX(), geographicArea.getMaxX());
        envelope.setRange(1, geographicArea.getMinY(), geographicArea.getMaxY());
        envelope.setRange(2, CoordinateSystemTable.toJulian(startTime),
                             CoordinateSystemTable.toJulian(endTime));
        return envelope;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setEnvelope(final Envelope envelope) throws SQLException {
        // No coordinate transformation needed for this implementation.
        setGeographicArea(new Rectangle2D.Double(envelope.getMinimum(0), envelope.getMinimum(1),
                                                 envelope.getLength (0), envelope.getLength (1)));
        setTimeRange(CoordinateSystemTable.toDate(envelope.getMinimum(2)),
                     CoordinateSystemTable.toDate(envelope.getMaximum(2)));
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Range getTimeRange() {
        return new Range(Date.class, new Date(startTime), new Date(endTime));
    }

    /**
     * {@inheritDoc}
     */
    public final void setTimeRange(final Range range) throws SQLException {
        Date startTime = (Date) range.getMinValue();
        Date   endTime = (Date) range.getMaxValue();
        if (!range.isMinIncluded()) {
            startTime = new Date(startTime.getTime()+1);
        }
        if (!range.isMaxIncluded()) {
            endTime = new Date(endTime.getTime()-1);
        }
        setTimeRange(startTime, endTime);
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setTimeRange(final Date startTime, final Date endTime)
            throws SQLException
    {
        final long newStartTime = startTime.getTime();
        final long newEndTime   =   endTime.getTime();
        if (newStartTime!=this.startTime || newEndTime!=this.endTime) {
            parameters = null;
            final Timestamp time=new Timestamp(newStartTime);
            statement.setTimestamp(ARG_START_TIME, time, calendar);
            time.setTime(newEndTime);
            statement.setTimestamp(ARG_END_TIME, time, calendar);
            this.startTime = newStartTime;
            this.endTime   = newEndTime;

            if (series != null) {
                // Don't log if this object is configured by CoverageDataBase.
                final String startText = dateFormat.format(startTime);
                final String   endText = dateFormat.format(  endTime);
                log("setTimeRange", Level.CONFIG, ResourceKeys.SET_TIME_RANGE_$3,
                                    new String[]{startText, endText, series.getName()});
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Rectangle2D getGeographicArea() {
        return (Rectangle2D) geographicArea.clone();
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setGeographicArea(final Rectangle2D rect) throws SQLException {
        if (!rect.equals(geographicArea)) {
            parameters = null;
            statement.setDouble(ARG_XMIN, rect.getMinX());
            statement.setDouble(ARG_XMAX, rect.getMaxX());
            statement.setDouble(ARG_YMIN, rect.getMinY());
            statement.setDouble(ARG_YMAX, rect.getMaxY());
            geographicArea = new XRectangle2D(rect);

            if (series != null) {
                // Don't log if this object is configured by CoverageDataBase.
                log("setGeographicArea", Level.CONFIG, ResourceKeys.SET_GEOGRAPHIC_AREA_$2,
                                         new String[] {getStringArea(), series.getName()});
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Dimension2D getPreferredResolution() {
        return (resolution!=null) ? (Dimension2D)resolution.clone() : null;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setPreferredResolution(final Dimension2D pixelSize) {
        if (!Utilities.equals(resolution, pixelSize)) {
            parameters = null;
            final int clé;
            final Object param;
            if (pixelSize != null) {
                resolution = (Dimension2D)pixelSize.clone();
                clé = ResourceKeys.SET_RESOLUTION_$3;
                param = new Object[] {
                    new Double(resolution.getWidth()),
                    new Double(resolution.getHeight()),
                    series.getName()
                };
            } else {
                resolution = null;
                clé = ResourceKeys.UNSET_RESOLUTION_$1;
                param = series.getName();
            }
            log("setPreferredResolution", Level.CONFIG, clé, param);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Operation getOperation() {
        return operation;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized ParameterList setOperation(final Operation operation) {
        this.parameters = null;
        this.operation=operation;
        final int clé;
        final Object param;
        if (operation != null) {
            opParam = operation.getParameterList();
            param   = new String[] {operation.getName(), series.getName()};
            clé     = ResourceKeys.SET_OPERATION_$2;
        } else {
            opParam = null;
            param   = series.getName();
            clé     = ResourceKeys.UNSET_OPERATION_$1;
        }
        log("setOperation", Level.CONFIG, clé, param);
        return opParam;
    }

    /**
     * {@inheritDoc}
     */
    public final ParameterList setOperation(final String operation) throws OperationNotFoundException {
        return setOperation(operation!=null ? GridCoverageEntry.PROCESSOR.getOperation(operation) : null);
    }

    /**
     * {@inheritDoc}
     */
    public final List<CoverageEntry> getEntries() throws SQLException {
        /*
         * On construit un tableau de l'interface ET NON de l'implémentation
         * parce que certains utilisateurs (par exemple CoverageTableModel)
         * voudront remplacer certains éléments de ce tableau sans que
         * ça ne lance un {@link java.lang.ArrayStoreException}.
         */
        final List<CoverageEntry> entries = new ArrayList<CoverageEntry>();
        getRanges(null, null, null, entries);
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public final CoverageEntry getEntry() throws SQLException {
        CoverageEntry best = null;
        final CoverageComparator comparator = new CoverageComparator(this);
        for (final CoverageEntry entry : getEntries()) {
            if (best==null || comparator.compare(entry, best) <= -1) {
                best = entry;
            }
        }
        return best;
    }

    /**
     * {@inheritDoc}
     *
     * @task TODO: Move hard-coded SQL statements into some configuration file.
     */
    public final synchronized CoverageEntry getEntry(final int ID) throws SQLException {
        if (imageByID == null) {
            final String query = select(PREFERENCES.get(GRID_COVERAGES, SQL_SELECT)) +
                                        " WHERE "+GRID_COVERAGES+".ID=?";
            imageByID = statement.getConnection().prepareStatement(query);
        }
        imageByID.setInt(1, ID);
        return getEntry(imageByID);
    }

    /**
     * {@inheritDoc}
     *
     * @task TODO: Move hard-coded SQL statements into some configuration file.
     */
    public final synchronized CoverageEntry getEntry(final String name) throws SQLException {
        if (imageByName == null) {
            final String query = select(PREFERENCES.get(GRID_COVERAGES, SQL_SELECT)) +
                                " WHERE (visible=TRUE) AND (series=?) AND (filename LIKE ?)";
            imageByName = statement.getConnection().prepareStatement(query);
        }
        imageByName.setInt(1, series.getID());
        imageByName.setString(2, name);
        return getEntry(imageByName);
    }

    /**
     * Retourne l'image correspondant à la requête spécifiée. Il ne
     * doit y avoir qu'une image correspondant à cette requête.
     */
    private CoverageEntry getEntry(final PreparedStatement query) throws SQLException {
        assert Thread.holdsLock(this);
        GridCoverageEntry entry = null;
        final ResultSet result = query.executeQuery();
        if (result.next()) {
            entry = new GridCoverageEntry(this, result).canonicalize();
            while (result.next()) {
                final GridCoverageEntry check = new GridCoverageEntry(this, result);
                if (!entry.equals(check)) {
                    throw new IllegalRecordException(GRID_COVERAGES, Resources.format(
                            ResourceKeys.ERROR_DUPLICATED_COVERAGE_$2, entry.getName(), check.getName()));
                }
            }
        }
        result.close();
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public final void getRanges(final RangeSet x, final RangeSet y, final RangeSet t)
            throws SQLException
    {
        getRanges(x, y, t, null);
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void getRanges(final RangeSet x, final RangeSet y, final RangeSet t,
                                             final List<CoverageEntry> entryList)
            throws SQLException
    {
        GridCoverageEntry newEntry = null;
        long           lastEndTime = Long.MIN_VALUE;
        final int       startIndex = (entryList!=null) ? entryList.size() : 0;
        final ResultSet     result = statement.executeQuery();
  loop: while (result.next()) {
            /*
             * Add the new entry to the list.  If many entries have the same
             * spatio-temporal coordinates but different resolution, then an
             * entry with a resolution close to the requested resolution will
             * be selected.
             */
            if (entryList != null) {
                newEntry = new GridCoverageEntry(this, result);
                for (int i=entryList.size(); --i>=0;) {
                    final GridCoverageEntry olderEntry = (GridCoverageEntry) entryList.get(i);
                    if (!olderEntry.compare(newEntry)) {
                        // Entry not equals according the "ORDER BY" clause.
                        break;
                    }
                    final GridCoverageEntry lowestResolution = olderEntry.getLowestResolution(newEntry);
                    if (lowestResolution != null) {
                        // Two entries has the same spatio-temporal coordinates.
                        if (lowestResolution.hasEnoughResolution(resolution)) {
                            // The entry with the lowest resolution is enough.
                            entryList.set(i, lowestResolution);
                        } else if (lowestResolution == olderEntry) {
                            // No entry has enough resolution;
                            // keep the one with the finest resolution.
                            entryList.set(i, newEntry);
                        }
                        continue loop;
                    }
                }
                entryList.add(newEntry);
            }
            /*
             * Compute ranges if it has been requested.  If we have previously
             * constructed an GridCoverageEntry, fetch the data from this entry
             * since some JDBC driver doesn't allow to get data from the same
             * column twice. Furthermore, this is faster... The "continue loop"
             * statement above may have hidden some rows, but since those rows
             * have the same spatio-temporal coordinates than one visible row,
             * it should not have any effect except improving performance.
             */
            if (t != null) {
                final long period = Math.round(series.getPeriod()*CoordinateSystemTable.DAY);
                final Date startTime;
                final Date   endTime;
                if (newEntry != null) {
                    startTime = newEntry.getStartTime();
                      endTime = newEntry.getEndTime();
                } else {
                    startTime = getTimestamp(START_TIME, result);
                      endTime = getTimestamp(  END_TIME, result);
                }
                if (startTime!=null && endTime!=null) {
                    final long lgEndTime = endTime.getTime();
                    final long checkTime = lgEndTime-period;
                    if (checkTime <= lastEndTime  &&  checkTime < startTime.getTime()) {
                        // Il arrive parfois que des images soient prises à toutes les 24 heures,
                        // mais pendant 12 heures seulement. On veut éviter que de telles images
                        // apparaissent tout le temps entrecoupées d'images manquantes.
                        startTime.setTime(checkTime);
                    }
                    lastEndTime = lgEndTime;
                    t.add(startTime, endTime);
                }
            }
            if (x != null) {
                final float xmin;
                final float xmax;
                if (newEntry!=null) {
                    xmin = newEntry.xmin;
                    xmax = newEntry.xmax;
                } else {
                    xmin = result.getFloat(XMIN);
                    xmax = result.getFloat(XMAX);
                }
                x.add(xmin, xmax);
            }
            if (y != null) {
                final float ymin;
                final float ymax;
                if (newEntry != null) {
                    ymin = newEntry.ymin;
                    ymax = newEntry.ymax;
                } else {
                    ymin = result.getFloat(YMIN);
                    ymax = result.getFloat(YMAX);
                }
                y.add(ymin, ymax);
            }
        }
        result.close();
        if (entryList != null) {
            final List<CoverageEntry> newEntries;
            newEntries = entryList.subList(startIndex, entryList.size());
            final int size = newEntries.size();
            GridCoverageEntry.canonicalize(newEntries.toArray(new CoverageEntry[size]));
            log("getEntries", Level.FINE, ResourceKeys.FOUND_COVERAGES_$1, new Integer(size));
        }
    }

    /**
     * Procède à l'extraction d'une date en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException {
        assert Thread.holdsLock(this);
        if (false) {
            // Cette ligne aurait suffit si ce n'était du bug #4380653...
            return result.getTimestamp(field, calendar);
        } else {
            if (localCalendar == null) {
                localCalendar=new GregorianCalendar();
            }
            return getTimestamp(field, result, calendar, localCalendar);
        }
    }

    /**
     * Retourne le format qui correspond au numéro spécifié. Si ce format avait
     * déjà été demandé auparavant, le même format sera réutilisé. Cette méthode
     * ne retourne jamais nul.
     *
     * @param  formatID Numéro identifiant le format voulu.
     * @throws SQLException si le format spécifié n'a pas été trouvé.
     */
    private FormatEntry getFormat(final int formatID) throws SQLException {
        assert Thread.holdsLock(this);
        final Integer ID   = new Integer(formatID);
        FormatEntry format = formats.get(ID);
        if (format == null) {
            if (formatTable == null) {
                formatTable = new FormatTable(statement.getConnection());
            }
            format = formatTable.getEntry(ID);
            formats.put(ID, format);
        } else if (formatTable != null) {
            /*
             * Si on a demandé un format qui avait déjà été lu auparavant,
             * il y a de bonnes chances pour qu'on n'ai plus besoin de la
             * table des formats. On va donc la fermer pour économiser des
             * ressources. On la réouvrira plus tard si c'est nécessaire.
             */
            formatTable.close();
            formatTable = null;
        }
        return format;
    }

    /**
     * Retourne les paramètres de cette table. Pour des raisons d'économie
     * de mémoire (de très nombreux objets <code>Parameters</code> pouvant
     * être créés), cette méthode retourne un exemplaire unique autant que
     * possible. L'objet retourné ne doit donc pas être modifié!
     *
     * @param  seriesID Numéro ID de la série, pour fin de vérification. Ce
     *                  numéro doit correspondre à celui de la série examinée
     *                  par cette table.
     * @param  formatID Numéro ID du format des images.
     * @param  csID     Numéro ID du système de coordonnées.
     * @param  pathname Chemin relatif des images.
     *
     * @return Un objet incluant les paramètres demandées ainsi que ceux de la table.
     * @throws SQLException si les paramètres n'ont pas pu être obtenus.
     */
    final synchronized Parameters getParameters(final int    seriesID,
                                                final int    formatID,
                                                final int    csID,
                                                final String pathname)
        throws SQLException
    {
        if (seriesID != series.getID()) {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_WRONG_SERIES_$1, series.getName()));
        }
        /*
         * Si les paramètres spécifiés sont identiques à ceux qui avaient été
         * spécifiés la dernière fois, retourne le dernier bloc de paramètres.
         */
        if (parameters != null &&
            parameters.format.getID() == formatID &&
            Utilities.equals(parameters.pathname, pathname))
        {
            if (formatTable != null) {
                /*
                 * Si on a demandé un format qui avait déjà été lu auparavant,
                 * il y a de bonnes chances pour qu'on n'ai plus besoin de la
                 * table des formats. On va donc la fermer pour économiser des
                 * ressources. On la réouvrira plus tard si c'est nécessaire.
                 */
                formatTable.close();
                formatTable = null;
            }
            return parameters;
        }
        /*
         * Construit un nouveau bloc de paramètres et projète les
         * coordonnées vers le système de coordonnées de l'image.
         */
        parameters = new Parameters(series, getFormat(formatID), pathname, operation, opParam,
                                    getCoordinateSystem(),
                                    getCoordinateSystemTable().getCoordinateSystem(csID),
                                    geographicArea, resolution, dateFormat);
        parameters = (Parameters)POOL.canonicalize(parameters);
        return parameters;
    }

    /**
     * Retourne la table des systèmes de coordonnées.
     */
    final CoordinateSystemTable getCoordinateSystemTable() throws SQLException {
        if (coordinateSystemTable == null) {
            coordinateSystemTable = new CoordinateSystemTable(getConnection());
        }
        return coordinateSystemTable;
    }

    /**
     * Retourne la connexion vers la base de données.
     */
    final Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        if (imageByID != null) {
            imageByID.close();
            imageByID = null;
        }
        if (formatTable != null) {
            formatTable.close();
            formatTable = null;
        }
        if (coordinateSystemTable != null) {
            coordinateSystemTable.close();
            coordinateSystemTable = null;
        }
        statement.close();
    }

    /**
     * Retourne une chaîne de caractères décrivant cette table.
     */
    public final String toString() {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(series.getName());
        buffer.append("\": ");
        buffer.append(getStringArea());
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Retourne les coordonnées demandées sous forme de chaîne de caractères.
     */
    private String getStringArea() {
        return CTSUtilities.toWGS84String(getCoordinateSystem(), geographicArea);
    }

    /**
     * Enregistre un évènement dans le journal.
     */
    private static void log(final String method, final Level level, final int clé, final Object param) {
        final Resources resources = Resources.getResources(null);
        final LogRecord record = resources.getLogRecord(level, clé, param);
        record.setSourceClassName("CoverageTable");
        record.setSourceMethodName(method);
        CoverageDataBase.LOGGER.log(record);
    }

    /**
     * Ajoute une entrée dans la table "<code>GridCoverages</code>".
     * Cette méthode sera redéfinie dans {@link WritableGridCoverageTable}.
     */
    public Integer addGridCoverage(final GridCoverage coverage, final String filename) throws SQLException {
        throw new SQLException("Table en lecture seule.");
    }
}
