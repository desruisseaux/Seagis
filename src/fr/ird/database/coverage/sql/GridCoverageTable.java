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
 */
package fr.ird.database.coverage.sql;

// Base de donn�es
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.rmi.RemoteException;

// G�om�trie
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
import java.util.HashMap;

// Divers
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.media.jai.ParameterList;
import javax.media.jai.util.Range;

// Geotools dependencies (CTS)
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.measure.Longitude;
import org.geotools.measure.Latitude;

// Geotools dependencies (GCS)
import org.geotools.gp.Operation;
import org.geotools.gp.OperationNotFoundException;
import org.geotools.gc.GridCoverage;

// Geotools dependencies (resources)
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.geometry.XRectangle2D;

// Seagis
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageRanges;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.database.coverage.CoverageComparator;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Connection vers une table d'images. Cette table contient des r�f�rences vers des images sous
 * forme d'objets {@link CoverageEntry}.  Une table <code>GridCoverageTable</code> est capable
 * de fournir la liste des entr�s {@link CoverageEntry} qui interceptent une certaines r�gion
 * g�ographique et une certaine plage de dates.
 *
 * @see CoverageDataBase#getGridCoverageTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class GridCoverageTable extends Table implements CoverageTable {
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des images.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes {@link #SERIES}, {@link #FILENAME} et compagnie.
     */
    static final ConfigurationKey SELECT = createKey(GRID_COVERAGES, ResourceKeys.SQL_GRID_COVERAGES,
            "SELECT " + "series, "     +  // [01] SERIES
                        "pathname, "   +  // [02] PATHNAME
                        "filename, "   +  // [03] FILENAME
                        "start_time, " +  // [04] START_TIME
                        "end_time, "   +  // [05] END_TIME
                        "x_min, "      +  // [06] XMIN
                        "x_max, "      +  // [07] XMAX
                        "y_min, "      +  // [08] YMIN
                        "y_max, "      +  // [09] YMAX
                        "width, "      +  // [10] WIDTH
                        "height, "     +  // [11] HEIGHT
                        "\"CRS\", "    +  // [12] CRS
                        "format\n"     +  // [13] FORMAT
             "FROM "       +SCHEMA+".\""+GRID_COVERAGES+"\" "+
             "INNER JOIN " +SCHEMA+".\""+BOUNDING_BOX+"\" ON extent=\""+BOUNDING_BOX+"\".identifier " +
             "INNER JOIN " +SCHEMA+".\""+SUBSERIES+   "\" ON subseries=\""+SUBSERIES+"\".identifier\n"+
             "WHERE (x_max>? AND x_min<? AND y_max>? AND y_min<?) "+
               "AND (((end_time IS NULL) OR end_time>=?) AND ((start_time IS NULL) OR start_time<=?)) "+
               "AND series=?\n"+
               "ORDER BY end_time, subseries"); // DOIT �tre en ordre chronologique.
                                                // Voir {@link GridCoverageEntry#compare}.

    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des images.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
     * les constantes {@link #SERIES}, {@link #FILENAME} et compagnie.
     */
    static final ConfigurationKey SELECT_ID = createKey(GRID_COVERAGES+":filename",
                                       ResourceKeys.SQL_GRID_COVERAGES_BY_FILENAME,
                                       selectWithoutWhere(SELECT.defaultValue) +
            " WHERE (visible=TRUE) AND (series=?) AND (filename=?)");
    
    /** Num�ro de colonne. */ static final int SERIES     =  1;
    /** Num�ro de colonne. */ static final int PATHNAME   =  2;
    /** Num�ro de colonne. */ static final int FILENAME   =  3;
    /** Num�ro de colonne. */ static final int START_TIME =  4;
    /** Num�ro de colonne. */ static final int END_TIME   =  5;
    /** Num�ro de colonne. */ static final int XMIN       =  6;
    /** Num�ro de colonne. */ static final int XMAX       =  7;
    /** Num�ro de colonne. */ static final int YMIN       =  8;
    /** Num�ro de colonne. */ static final int YMAX       =  9;
    /** Num�ro de colonne. */ static final int WIDTH      = 10;
    /** Num�ro de colonne. */ static final int HEIGHT     = 11;
    /** Num�ro de colonne. */ static final int CRS        = 12;
    /** Num�ro de colonne. */ static final int FORMAT     = 13;

    /** Num�ro d'argument. */ private static final int ARG_XMIN       = 1;
    /** Num�ro d'argument. */ private static final int ARG_XMAX       = 2;
    /** Num�ro d'argument. */ private static final int ARG_YMIN       = 3;
    /** Num�ro d'argument. */ private static final int ARG_YMAX       = 4;
    /** Num�ro d'argument. */ private static final int ARG_START_TIME = 5;
    /** Num�ro d'argument. */ private static final int ARG_END_TIME   = 6;
    /** Num�ro d'argument. */ private static final int ARG_SERIES     = 7;

    /**
     * R�ference vers la s�rie d'images.
     */
    private SeriesEntry series;

    /**
     * L'op�ration � appliquer sur les images lue,
     * ou <code>null</code> s'il n'y en a aucune.
     */
    private Operation operation;

    /**
     * Param�tres de l'op�ration, ou <code>null</code> s'il n'y a pas d'op�ration.
     */
    private ParameterList opParam;

    /**
     * Dimension logique (en degr�s de longitude et de latitude) d�sir�e des pixels
     * de l'images. Cette information n'est qu'approximative. Il n'est pas garantie
     * que les lectures produiront effectivement des images de cette r�solution.
     * Une valeur nulle signifie que les lectures doivent se faire avec la meilleure
     * r�solution possible.
     */
    private Dimension2D resolution;

    /**
     * Coordonn�es g�ographiques de la r�gion d'int�ret, en degr�s de longitude et de latitude.
     * Ces coordonn�es peuvent �tre sp�cifi�es par un appel � {@link #setGeographicArea}.  Les
     * objets {@link Rectangle2D} affect�s � <code>geographicArea</code> peuvent changer, mais
     * les coordonn�es d'un objet {@link Rectangle2D} donn� ne doivent PAS �tre chang�es.   La
     * classe <code>GridCoverageEntry</code> se r�ferera directement � ce champ afin d'�viter la
     * cr�ation d'une multitude de clones.
     */
    private Rectangle2D geographicArea;

    /**
     * Date du d�but de la plage de temps des images
     * recherch�es par cet objet <code>GridCoverageTable</code>.
     */
    private long startTime;

    /**
     * Date du fin de la plage de temps des images
     * recherch�es par cet objet <code>GridCoverageTable</code>.
     */
    private long endTime;

    /**
     * Calendrier utilis� pour pr�parer les dates. Ce calendrier
     * utilisera le fuseau horaire sp�cifi� lors de la construction.
     */
    private final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Fuseau horaire de la base de donn�es.
     */
    protected final TimeZone timezone;

    /**
     * Formatteur � utiliser pour �crire des dates pour l'utilisateur. Les caract�res et
     * les conventions linguistiques d�pendront de la langue de l'utilisateur. Toutefois,
     * le fuseau horaire devrait �tre celui de la r�gion d'�tude plut�t que celui du pays
     * de l'utilisateur.
     */
    private final DateFormat dateFormat;

    /**
     * Table des syst�mes de coordonn�es. Ne sera construit que la premi�re fois o� elle
     * sera n�cessaire.
     */
    private transient CoordinateSystemTable coordinateSystemTable;

    /**
     * Table des formats. Cette table ne sera construite que la premi�re fois
     * o� elle sera n�cessaire.  Elle sera ensuite ferm�e chaque fois qu'elle
     * n'est plus utilis�e pour �conomiser des ressources.
     */
    private transient FormatTable formatTable;

    /**
     * Ensemble des formats d�j� lue. Autant que possible,
     * on r�utilisera les formats qui ont d�j� �t� cr��s.
     */
    private final Map<String,FormatEntry> formats = new HashMap<String,FormatEntry>();

    /**
     * Requ�te SQL faisant le lien avec la base de donn�es.
     */
    private final PreparedStatement statement;

    /**
     * Table d'images pouvant �tre r�cup�r�e par leur nom.
     * Cette table ne sera construite que si elle est n�cessaire.
     */
    private transient PreparedStatement imageByName;

    /**
     * Derniers param�tres � avoir �t� construit. Ces param�tres sont
     * retenus afin d'�viter d'avoir � les reconstruires trop souvent
     * si c'est �vitable.
     */
    private transient Parameters parameters;

    /**
     * Construit une table des images en utilisant la connection sp�cifi�e.
     * L'appellant <strong>doit</strong> appeler {@link #setSeries},
     * {@link #setGeographicArea} et {@link #setTimeRange} avant d'utiliser
     * cette table.
     *
     * @param  connection Connection vers une base de donn�es d'images.
     * @param  timezone   Fuseau horaire des dates inscrites dans la base
     *                    de donn�es. Cette information est utilis�e pour
     *                    convertir en heure UTC les dates �crites dans la
     *                    base de donn�es.
     * @throws SQLException si <code>GridCoverageTable</code> n'a pas pu construire sa requ�te SQL.
     */
    GridCoverageTable(final CoverageDataBase database,
                      final Connection     connection,
                      final TimeZone         timezone)
            throws RemoteException, SQLException
    {
        super(database);
        statement = connection.prepareStatement(getProperty(SELECT));
        this.timezone   = timezone;
        this.calendar   = new GregorianCalendar(timezone);
        this.dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        this.dateFormat.setCalendar(calendar);
    }

    /**
     * {@inheritDoc}
     */
    public final SeriesEntry getSeries() {
        return series;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setSeries(final SeriesEntry series)
            throws CatalogException
    {
        if (!series.equals(this.series)) {
            final boolean toLog = (this.series != null);
            parameters = null;
            try {
                statement.setString(ARG_SERIES, series.getName());
            } catch (SQLException cause) {
                throw new CatalogException(cause);
            }
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
    public final synchronized void setEnvelope(final Envelope envelope) throws CatalogException {
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
    public final void setTimeRange(final Range range) throws CatalogException {
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
            throws CatalogException
    {
        final long newStartTime = startTime.getTime();
        final long newEndTime   =   endTime.getTime();
        if (newStartTime!=this.startTime || newEndTime!=this.endTime) {
            parameters = null;
            final Timestamp time = new Timestamp(newStartTime);
            try {
                statement.setTimestamp(ARG_START_TIME, time, calendar);
                time.setTime(newEndTime);
                statement.setTimestamp(ARG_END_TIME, time, calendar);
            } catch (SQLException cause) {
                throw new CatalogException(cause);
            }        
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
        return new XRectangle2D((Rectangle2D) geographicArea.clone());
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setGeographicArea(final Rectangle2D rect)
            throws CatalogException
    {
        if (!rect.equals(geographicArea)) try {
            parameters = null;
            statement.setDouble(ARG_XMIN, rect.getMinX());
            statement.setDouble(ARG_XMAX, rect.getMaxX());
            statement.setDouble(ARG_YMIN, rect.getMinY());
            statement.setDouble(ARG_YMAX, rect.getMaxY());
            geographicArea = new XRectangle2D(rect);
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }
        if (series != null) {
            // Don't log if this object is configured by CoverageDataBase.
            log("setGeographicArea", Level.CONFIG, ResourceKeys.SET_GEOGRAPHIC_AREA_$2,
                                     new String[] {getStringArea(), series.getName()});
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
            final int cl�;
            final Object param;
            if (pixelSize != null) {
                resolution = (Dimension2D)pixelSize.clone();
                cl� = ResourceKeys.SET_RESOLUTION_$3;
                param = new Object[] {
                    new Double(resolution.getWidth()),
                    new Double(resolution.getHeight()),
                    series.getName()
                };
            } else {
                resolution = null;
                cl� = ResourceKeys.UNSET_RESOLUTION_$1;
                param = series.getName();
            }
            log("setPreferredResolution", Level.CONFIG, cl�, param);
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
        this.operation  = operation;
        final int cl�;
        final Object param;
        if (operation != null) {
            opParam = operation.getParameterList();
            param   = new String[] {operation.getName(), series.getName()};
            cl�     = ResourceKeys.SET_OPERATION_$2;
        } else {
            opParam = null;
            param   = series.getName();
            cl�     = ResourceKeys.UNSET_OPERATION_$1;
        }
        log("setOperation", Level.CONFIG, cl�, param);
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
    public final List<CoverageEntry> getEntries() throws RemoteException {
        return getRanges(new CoverageRanges(false, false, false, true)).entries;
    }

    /**
     * {@inheritDoc}
     */
    public final CoverageEntry getEntry() throws RemoteException {
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
     */
    public final synchronized CoverageEntry getEntry(final String name) throws RemoteException {
        try {
            if (imageByName == null) {
                imageByName = statement.getConnection().prepareStatement(getProperty(SELECT_ID));
            }
            imageByName.setString(1, series.getName());
            imageByName.setString(2, name);
            return getEntry(imageByName);
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }
    }

    /**
     * Retourne l'image correspondant � la requ�te sp�cifi�e. Il ne
     * doit y avoir qu'une image correspondant � cette requ�te.
     */
    private CoverageEntry getEntry(final PreparedStatement query) throws RemoteException {
        assert Thread.holdsLock(this);
        GridCoverageEntry entry = null;
        try {
            final ResultSet result = query.executeQuery();
            if (result.next()) {
                entry = new GridCoverageEntry(this, result).canonicalize();
                while (result.next()) {
                    final GridCoverageEntry check = new GridCoverageEntry(this, result);
                    if (!entry.equals(check)) {
                        throw new IllegalRecordException(GRID_COVERAGES, Resources.format(
                                    ResourceKeys.ERROR_DUPLICATED_COVERAGE_$2,
                                    entry.getName(), check.getName()));
                    }
                }
            }
            result.close();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized CoverageRanges getRanges(final CoverageRanges ranges) throws RemoteException {
        final List<CoverageEntry> entries = ranges.entries;
        try {
            GridCoverageEntry newEntry = null;
            long           lastEndTime = Long.MIN_VALUE;
            final int       startIndex = (entries!=null) ? entries.size() : 0;
            final ResultSet     result = statement.executeQuery();
      loop: while (result.next()) {
                /*
                 * Add the new entry to the list.  If many entries have the same
                 * spatio-temporal coordinates but different resolution, then an
                 * entry with a resolution close to the requested resolution will
                 * be selected.
                 */
                if (entries != null) {
                    newEntry = new GridCoverageEntry(this, result);
                    for (int i=entries.size(); --i>=startIndex;) {
                        final GridCoverageEntry olderEntry = (GridCoverageEntry) entries.get(i);
                        if (!olderEntry.compare(newEntry)) {
                            // Entry not equals according the "ORDER BY" clause.
                            break;
                        }
                        final GridCoverageEntry lowestResolution = olderEntry.getLowestResolution(newEntry);
                        if (lowestResolution != null) {
                            // Two entries has the same spatio-temporal coordinates.
                            if (lowestResolution.hasEnoughResolution(resolution)) {
                                // The entry with the lowest resolution is enough.
                                entries.set(i, lowestResolution);
                            } else if (lowestResolution == olderEntry) {
                                // No entry has enough resolution;
                                // keep the one with the finest resolution.
                                entries.set(i, newEntry);
                            }
                            continue loop;
                        }
                    }
                    entries.add(newEntry);
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
                if (ranges.t != null) {
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
                            // Il arrive parfois que des images soient prises � toutes les 24 heures,
                            // mais pendant 12 heures seulement. On veut �viter que de telles images
                            // apparaissent tout le temps entrecoup�es d'images manquantes.
                            startTime.setTime(checkTime);
                        }
                        lastEndTime = lgEndTime;
                        ranges.t.add(startTime, endTime);
                    }
                }
                if (ranges.x != null) {
                    final double xmin;
                    final double xmax;
                    if (newEntry != null) {
                        xmin = newEntry.xmin;
                        xmax = newEntry.xmax;
                    } else {
                        xmin = result.getDouble(XMIN);
                        xmax = result.getDouble(XMAX);
                    }
                    ranges.x.add(new Longitude(xmin), new Longitude(xmax));
                }
                if (ranges.y != null) {
                    final double ymin;
                    final double ymax;
                    if (newEntry != null) {
                        ymin = newEntry.ymin;
                        ymax = newEntry.ymax;
                    } else {
                        ymin = result.getDouble(YMIN);
                        ymax = result.getDouble(YMAX);
                    }
                    ranges.y.add(new Latitude(ymin), new Latitude(ymax));
                }
            }
            result.close();
            if (entries != null) {
                final int size = entries.size();
                for (int i=startIndex; i<size; i++) {
                    entries.set(i, ((GridCoverageEntry) entries.get(i)).canonicalize());
                }
                log("getEntries", Level.FINE, ResourceKeys.FOUND_COVERAGES_$1,
                                  new Integer(size-startIndex));
            }
        } catch (SQLException e) {
            throw new CatalogException(e);
        }     
        return ranges;
    }

    /**
     * Proc�de � l'extraction d'une date en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException {
        assert Thread.holdsLock(this);
        if (false) {
            // Cette ligne aurait suffit si ce n'�tait du bug #4380653...
            return result.getTimestamp(field, calendar);
        } else {
            if (localCalendar == null) {
                localCalendar = new GregorianCalendar();
            }
            return getTimestamp(field, result, calendar, localCalendar);
        }
    }

    /**
     * Retourne le format qui correspond au nom sp�cifi�. Si ce format avait
     * d�j� �t� demand� auparavant, le m�me format sera r�utilis�. Cette m�thode
     * ne retourne jamais nul.
     *
     * @param  ID Nom identifiant le format voulu.
     * @throws RemoteException si le format sp�cifi� n'a pas �t� trouv�.
     */
    private FormatEntry getFormat(final String ID) throws RemoteException, SQLException {
        assert Thread.holdsLock(this);
        FormatEntry format = formats.get(ID);
        if (format == null) {
            if (formatTable == null) {
                formatTable = new FormatTable(database, statement.getConnection());
            }
            formatTable.countBeforeClose = 20;
            format = formatTable.getEntry(ID);
            formats.put(ID, format);
        } else if (formatTable != null) {
            if (--formatTable.countBeforeClose < 0) {
                /*
                 * Si on a demand� un format qui avait d�j� �t� lu auparavant,
                 * il y a de bonnes chances pour qu'on n'ai plus besoin de la
                 * table des formats. On va donc la fermer pour �conomiser des
                 * ressources. On la r�ouvrira plus tard si c'est n�cessaire.
                 */
                formatTable.close();
                formatTable = null;
            }
        }
        return format;
    }

    /**
     * Retourne les param�tres de cette table. Pour des raisons d'�conomie
     * de m�moire (de tr�s nombreux objets <code>Parameters</code> pouvant
     * �tre cr��s), cette m�thode retourne un exemplaire unique autant que
     * possible. L'objet retourn� ne doit donc pas �tre modifi�!
     *
     * @param  seriesID Nom ID de la s�rie, pour fin de v�rification. Ce
     *                  nom doit correspondre � celui de la s�rie examin�e
     *                  par cette table.
     * @param  formatID Nom ID du format des images.
     * @param  crsID    Nom ID du syst�me de r�f�rence des coordonn�es.
     * @param  pathname Chemin relatif des images.
     *
     * @return Un objet incluant les param�tres demand�es ainsi que ceux de la table.
     * @throws SQLException si les param�tres n'ont pas pu �tre obtenus.
     */
    final synchronized Parameters getParameters(final String seriesID,
                                                final String formatID,
                                                final String crsID,
                                                final String pathname)
        throws SQLException, RemoteException
    {
        final String seriesName = series.getName();
        if (!Utilities.equals(seriesID, seriesName)) {
            throw new CatalogException(Resources.format(ResourceKeys.ERROR_WRONG_SERIES_$1, seriesName));
        }
        /*
         * Si les param�tres sp�cifi�s sont identiques � ceux qui avaient �t�
         * sp�cifi�s la derni�re fois, retourne le dernier bloc de param�tres.
         */
        if (parameters != null &&
            Utilities.equals(parameters.format .getName(), formatID) &&
            Utilities.equals(parameters.imageCS.getName(), crsID)    &&
            Utilities.equals(parameters.pathname,          pathname))
        {
            if (formatTable != null) {
                if (--formatTable.countBeforeClose < 0) {
                    /*
                     * Si on a demand� un format qui avait d�j� �t� lu auparavant,
                     * il y a de bonnes chances pour qu'on n'ai plus besoin de la
                     * table des formats. On va donc la fermer pour �conomiser des
                     * ressources. On la r�ouvrira plus tard si c'est n�cessaire.
                     */
                    formatTable.close();
                    formatTable = null;
                }
            }
            return parameters;
        }
        /*
         * Construit un nouveau bloc de param�tres et proj�te les
         * coordonn�es vers le syst�me de coordonn�es de l'image.
         */
        parameters = new Parameters(series,
                                    getFormat(formatID), pathname, operation, opParam,
                                    getCoordinateSystem(),
                                    getCoordinateSystemTable().getCoordinateSystem(crsID),
                                    geographicArea, resolution, dateFormat,
                                    getProperty(CoverageDataBase.ROOT_DIRECTORY),
                                    getProperty(CoverageDataBase.ROOT_URL));
        parameters = (Parameters)POOL.canonicalize(parameters);
        return parameters;
    }

    /**
     * Retourne la table des syst�mes de coordonn�es.
     */
    final CoordinateSystemTable getCoordinateSystemTable() throws RemoteException, SQLException {
        assert Thread.holdsLock(this);
        if (coordinateSystemTable == null) {
            coordinateSystemTable = new CoordinateSystemTable(database, getConnection());
        }
        return coordinateSystemTable;
    }

    /**
     * Retourne la connexion vers la base de donn�es.
     */
    final Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws RemoteException {        
        try {
            if (imageByName != null) {
                imageByName.close();
                imageByName = null;
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
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * Retourne une cha�ne de caract�res d�crivant cette table.
     */
    public final String toString() {
        final StringBuilder buffer = new StringBuilder(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(series.getName());
        buffer.append("\": ");
        buffer.append(getStringArea());
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Retourne les coordonn�es demand�es sous forme de cha�ne de caract�res.
     */
    private String getStringArea() {
        return CTSUtilities.toWGS84String(getCoordinateSystem(), geographicArea);
    }

    /**
     * Enregistre un �v�nement dans le journal.
     */
    private static void log(final String method, final Level level, final int cl�, final Object param) {
        final Resources resources = Resources.getResources(null);
        final LogRecord record = resources.getLogRecord(level, cl�, param);
        record.setSourceClassName("CoverageTable");
        record.setSourceMethodName(method);
        CoverageDataBase.LOGGER.log(record);
    }

    /**
     * Ajoute une entr�e dans la table "<code>GridCoverages</code>".
     * Cette m�thode sera red�finie dans {@link WritableGridCoverageTable}.
     */
    public boolean addGridCoverage(final GridCoverage coverage, final String filename) throws RemoteException {
        throw new CatalogException("Table en lecture seule.");
    }
}
