/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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

// Databases
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.rmi.RemoteException;

// Geometry
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;

// Miscellaneous
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.gc.GridRange;
import org.geotools.gc.GridCoverage;
import org.geotools.cs.TemporalDatum;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.resources.CTSUtilities;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.sql.LoggingLevel;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Insère de nouvelles entrées dans les tables de la base de données d'images. Par exemple,
 * cette classe peut être utilisée pour ajouter de nouvelles entrées dans la table "Images",
 * ce qui peut impliquer l'ajout d'entrés dans la table "Areas" en même temps. Cette classe
 * peut ajouter de nouvelles lignes aux tables existantes, mais ne modifie jamais les lignes
 * existantes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class WritableGridCoverageTable extends GridCoverageTable {
    /**
     * The SQL instruction to use when looking for bounding box.
     * Note that an "INSERT" instruction may be generated
     * by the {@link #sqlInsertGeometry} method.
     */
    static final ConfigurationKey SELECT_BBOX = createKey(BOUNDING_BOX+":ID", ResourceKeys.SQL_GRID_GEOMETRY,
            "SELECT ID FROM "+SCHEMA+".\""+BOUNDING_BOX+"\" WHERE "+
            "x_min=? AND x_max=? AND "+
            "y_min=? AND y_max=? AND "+
            "width=? AND height=?");

    /**
     * The SQL instruction for inserting a new geographic bounding box.
     */
    static final ConfigurationKey INSERT_BBOX = createKey(BOUNDING_BOX+":INSERT", ResourceKeys.SQL_GRID_GEOMETRIES_INSERT,
            "INSERT INTO "+SCHEMA+".\""+BOUNDING_BOX+"\" " +
            "(x_min, x_max, y_min, y_max, width, height) " +
            "VALUES (?, ?, ?, ?, ?, ?)");
    
    /**
     * The SQL instruction for inserting a new grid coverage.
     */
    static final ConfigurationKey INSERT_COVERAGE = createKey(GRID_COVERAGES+":INSERT", ResourceKeys.SQL_GRID_COVERAGES_INSERT,
            "INSERT INTO "+SCHEMA+".\""+GRID_COVERAGES+"\" "         +
            "(subseries, filename, start_time, end_time, geometry) " +
            "VALUES (?, ?, ?, ?, ?)");

    /**
     * Prepared statement for selecting a bounding box ID.
     * This statement is built by {@link #addBoundingBox} when first needed.
     */
    private PreparedStatement selectBBox;

    /**
     * Statement for inserting a bounding box.
     * This statement is built by {@link #addBoundingBox} when first needed.
     */
    private PreparedStatement insertBBox;

    /**
     * Statement for "INSERT INTO Images...".
     * This statement is built by {@link #addGridCoverage} when first needed.
     */
    private PreparedStatement insertCoverage;

    /**
     * Date format for formating SQL statement.
     * This format is built only when first needed.
     */
    private DateFormat dateFormat;

    /**
     * Construct a new <code>WritableGridCoverageTable</code>.
     *
     * @param  connection The connection to the database.
     * @param  timezone   The database time zone.
     * @throws SQLException if the table can't be constructed.
     */
    public WritableGridCoverageTable(final CoverageDataBase database,
                                     final Connection     connection,
                                     final TimeZone         timezone)
            throws RemoteException, SQLException
    {
        super(database, connection, timezone);
    }

    /**
     * Retourne la sous-séries à utiliser.
     *
     * @todo A éliminer si on décide de fournir plutôt une méthode
     *       <code>getSubSeries()</code> dans <code>SeriesEntry</code>.
     */
    private Entry getSubSeries() throws RemoteException {
        final fr.ird.database.coverage.SeriesEntry candidate = getSeries();
        if (candidate instanceof SeriesEntry) {
            final Entry[] subseries = ((SeriesEntry) candidate).subseries;
            if (subseries != null) {
                switch (subseries.length) {
                    case 1: {
                        return subseries[0];
                    }
                    // TODO: Autre cas?
                }
            }
        }
        throw new IllegalRecordException(SUBSERIES, "Une seule sous-série était attendue.");
    }

    /**
     * Log a record. This is used for logging warning or information
     * messages when the database is updated. Since this class is used
     * by {@link ImageTable#addGridCoverage} only, we will set source
     * class and method name according.
     */
    private static void log(final LogRecord record) {
        record.setSourceClassName("CoverageTable");
        record.setSourceMethodName("addGridCoverage");
        CoverageDataBase.LOGGER.log(record);
    }

    /**
     * Log an "SQL_UPDATE" record with the specified query as the message.
     * This method replace all question marks found in the query by the
     * specified argument values.
     */
    private static void logUpdate(final String query, final Comparable... values) {
        final StringBuilder buffer = new StringBuilder();
        int last = 0;
        for (int i=0; i<values.length; i++) {
            final int stop = query.indexOf('?', last);
            if (stop < 0) {
                // Missing arguments in the query. Since this method is used for logging
                // purpose only, we will not stop the normal execution flow for that.
                break;
            }
            final boolean isChar = (values[i] instanceof CharSequence);
            buffer.append(query.substring(last, stop));
            if (isChar) buffer.append('\'');
            buffer.append(values[i].toString());
            if (isChar) buffer.append('\'');
            last = stop+1;
        }
        buffer.append(query.substring(last));
        log(new LogRecord(LoggingLevel.UPDATE, buffer.toString()));
    }

    /**
     * Execute a query and returns the first ID found.  The query should be a
     * "SELECT ID FROM ..." clause, where the only column (ID) is the primary
     * key of the table. If more than one ID was found, then a warning is logged.
     *
     * @param  statement {@link #selectBBox}.
     * @param  errorKey  1-argument resources key to use if more than one ID is found.
     * @return The ID, or <code>null</code> if none was found.
     * @throws SQLException if an error occured while reading the database.
     */
    private static Integer executeQuery(final PreparedStatement statement, final int errorKey)
            throws SQLException
    {
        Integer ID = null;
        final ResultSet result = statement.executeQuery();
        while (result.next()) {
            final Integer nextID = new Integer(result.getInt(1));
            if (ID!=null && !ID.equals(nextID)) {
                log(Resources.getResources(null).getLogRecord(Level.WARNING, errorKey, nextID));
            } else {
                ID = nextID;
            }
        }
        result.close();
        return ID;
    }

    /**
     * Add an entry to the <code>GeographicBoundingBox</code> table, if not already presents.
     *
     * @param  bbox The geographic bounding box, in degrees of latitude
     *              and longitude on the WGS84 ellipsoid.
     * @param  size The image size, in pixels.
     * @throws SQLException if the operation failed.
     */
    private int addBoundingBox(final Rectangle2D bbox,
                               final Dimension   size)
            throws RemoteException, SQLException
    {
        assert Thread.holdsLock(this);
        if (selectBBox == null) {
            selectBBox = getConnection().prepareStatement(getProperty(SELECT_BBOX));
        }
        selectBBox.setDouble(1, bbox.getMinX());
        selectBBox.setDouble(2, bbox.getMaxX());
        selectBBox.setDouble(3, bbox.getMinY());
        selectBBox.setDouble(4, bbox.getMaxY());
        selectBBox.setInt   (5, size.width);
        selectBBox.setInt   (6, size.height);
        Integer ID = executeQuery(selectBBox, ResourceKeys.ERROR_DUPLICATED_GEOMETRY_$1);
        if (ID != null) {
            return ID.intValue();
        }
        if (insertBBox == null) {
            insertBBox = getConnection().prepareStatement(getProperty(INSERT_BBOX));
        }       
        insertBBox.setDouble(1, bbox.getMinX());
        insertBBox.setDouble(2, bbox.getMaxX());
        insertBBox.setDouble(3, bbox.getMinY());
        insertBBox.setDouble(4, bbox.getMaxY());
        insertBBox.setInt   (5, size.width);
        insertBBox.setInt   (6, size.height);
        if (insertBBox.executeUpdate() == 1) {
            ID = executeQuery(selectBBox, ResourceKeys.ERROR_DUPLICATED_GEOMETRY_$1);
            /*
             * TODO: Missing ID in the log message.
             */
            logUpdate(getProperty(SELECT_BBOX),
                                  bbox.getMinX(), bbox.getMaxX(),
                                  bbox.getMinY(), bbox.getMaxY(),
                                  size.width,     size.height);
            if (ID != null) {
                return ID.intValue();
            }
        }                
        // Should not happen.
        throw new SQLException("Unexpected update result");
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean addGridCoverage(final GridCoverage coverage, final String filename)
            throws RemoteException
    {
        final CoordinateSystem cs = coverage.getCoordinateSystem();
        final TemporalCoordinateSystem timeCS = CTSUtilities.getTemporalCS(cs);
        if (!TemporalDatum.UTC.equals(timeCS.getTemporalDatum())) {
            throw new CatalogException(Resources.format(ResourceKeys.ERROR_BAD_COORDINATE_SYSTEM));
        }
        /*
         * Gets the envelope, image size, bbox, start time and end time.
         * TODO: We should check for 2D-only coverage! In this case, there
         *       is no temporal axis (timeCS==null) and no start/end time.
         */
        final Envelope envelope = coverage.getEnvelope();
        final GridRange   range = coverage.getGridGeometry().getGridRange();
        final Rectangle2D  bbox = envelope.getSubEnvelope(0,2).toRectangle2D();
        final Dimension    size = new Dimension(range.getLength(0), range.getLength(1));
        final Date    startTime = timeCS.toDate(envelope.getMinimum(2));
        final Date      endTime = timeCS.toDate(envelope.getMaximum(2));
        try {
            // TODO: check the csID with the one declared in 'subseries'.
            final String csID = getCoordinateSystemTable().getID(cs);
            final int  bboxID = addBoundingBox(bbox, size);
            if (insertCoverage == null) {
                insertCoverage = getConnection().prepareStatement(getProperty(INSERT_COVERAGE));
            }
            /*
             * Set the argument. Note: We don't rely on 'setTimestamp(...)' since this
             * method is highly unreliable, du to Sun's bug #4380653.
             */
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.US);
                dateFormat.setTimeZone(timezone);
            }
            final String startTimeText = dateFormat.format(startTime);
            final String   endTimeText = dateFormat.format(  endTime);
            final String     subseries = getSubSeries().getName();
            insertCoverage.setString(1, subseries);
            insertCoverage.setString(2, filename);        
            insertCoverage.setString(3, startTimeText);
            insertCoverage.setString(4, endTimeText);
            insertCoverage.setInt   (5, bboxID);
            if (insertCoverage.executeUpdate() == 1) {
                logUpdate(getProperty(INSERT_COVERAGE), subseries, filename,
                          startTimeText, endTimeText, bboxID);
                return true;
            }
            // Should not happen.
            throw new SQLException("Unexpected update result");
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws RemoteException {
        try {
            if (insertCoverage != null) {
                insertCoverage.close();
                insertCoverage = null;
            }
            if (insertBBox != null) {
                insertBBox.close();
                insertBBox = null;
            }
            if (selectBBox != null) {
                selectBBox.close();
                selectBBox = null;
            }
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }
}
