/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.sql.image;

// Databases
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import fr.ird.sql.DataBase;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.gc.GridRange;
import org.geotools.gc.GridCoverage;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.TemporalDatum;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;

// Geometry
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;

// Logging
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Miscellaneous
import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.units.Unit;


/**
 * Implementation of {@link TableFiller}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class TableFillerImpl extends Table implements TableFiller {
    /**
     * The SQL instruction to use when looking for area.
     * Note that an "INSERT" instruction may be generated
     * by the {@link #sqlInsertArea} method.
     */
    private static final String SELECT_AREA = "SELECT ID FROM "+AREAS+" WHERE "+
                                              "xmin=? AND xmax=? AND "+
                                              "ymin=? AND ymax=? AND "+
                                              "width=? AND height=?";

    /**
     * The SQL instruction to use when looking for an image.
     * Note that an "INSERT" instruction may be generated by
     * the {@link #sqlInsertImage} method.
     */
    private static final String SELECT_IMAGE = "SELECT ID FROM "+IMAGES+" WHERE "+
                                               "groupe=? AND filename LIKE ?";

    /**
     * The connection to the database.
     */
    private final Connection connection;

    /**
     * Prepared statement for "SELECT ID FROM Areas WHERE...".
     * This statement is built by {@link #addArea} when first needed.
     */
    private PreparedStatement selectArea;

    /**
     * Prepared statement for "SELECT ID FROM Images WHERE...".
     * This statement is built by {@link #addImage} when first needed.
     */
    private PreparedStatement selectImage;

    /**
     * Statement for "INSERT INTO Areas...".
     * This statement is built by {@link #addArea} when first needed.
     */
    private Statement insertArea;

    /**
     * Statement for "INSERT INTO Images...".
     * This statement is built by {@link #addImage} when first needed.
     */
    private Statement insertImage;

    /**
     * Date format for formating SQL statement.
     * This format is built only when first needed.
     */
    private DateFormat dateFormat;

    /**
     * The database time zone.
     */
    private final TimeZone timezone;

    /**
     * The group into which inserting new image. This is the ID that will
     * be copied in the "Images.groupe" column. Images will be inserted
     * using the {@link #addImage} method.
     */
    private int groupID;

    /**
     * Construct a new <code>ImageTableUpdate</code>.
     *
     * @param connection The connection to the database.
     * @param timezone   The database time zone.
     */
    public TableFillerImpl(final Connection connection, final TimeZone timezone) {
        this.connection = connection;
        this.timezone   = timezone;
    }

    /**
     * Execute a query and returns the first ID found.  The query should be a
     * "SELECT ID FROM ..." clause, where the only column (ID) is the primary
     * key of the table. If more than one ID was found, then a warning is logged.
     *
     * @param  statement {@link #selectArea} or {@link #selectImage}.
     * @param  errorKey  1-argument resources key to use if more than one ID is found.
     * @return The ID, or <code>null</code> if none was found.
     * @throws SQLException if an error occured while reading the database.
     */
    private static Integer executeQuery(final PreparedStatement statement, final int errorKey) throws SQLException {
        Integer ID = null;
        final ResultSet result = statement.executeQuery();
        while (result.next()) {
            final Integer nextID = new Integer(result.getInt(1));
            if (ID != null) {
                log(Resources.getResources(null).getLogRecord(Level.WARNING, errorKey, nextID));
            } else {
                ID = nextID;
            }
        }
        result.close();
        return ID;
    }

    /**
     * Log a record. This is used for logging warning or information
     * messages when the database is updated. Since this class is used
     * by {@link ImageTable#addGridCoverage} only, we will set source
     * class and method name according.
     */
    private static void log(final LogRecord record) {
        record.setSourceClassName ("ImageTable");
        record.setSourceMethodName("addGridCoverage");
        logger.log(record);
    }

    /**
     * Returns the date for the specified value. We assume that
     * the temporal axis use the UTC datum (this is not checked).
     */
    private static Date getDate(final TemporalCoordinateSystem cs, double value) {
        value = Unit.DAY.convert(value, cs.getUnits(0)) * (24*60*60*1000);
        final Date date = cs.getEpoch();
        date.setTime(date.getTime() + Math.round(value));
        return date;
    }

    /**
     * Set the group into which inserting new image. Images
     * will be inserted using the {@link #addImage} method.
     *
     * @param  ID The ID for the image's group. This is the
     *         ID that will be copied in the "groupe" column.
     * @throws SQLException if the operation failed.
     */
    public synchronized void setGroup(final int ID) throws SQLException {
        this.groupID = ID;
    }

    /**
     * Add an entry to the <code>Images</code> table.
     *
     * @param  coverage The coverage to add. This coverage should have a three-dimensional
     *         envelope, the third dimension being along the time axis.
     * @param  filename The filename for the grid coverage, without extension.
     *         This is the filename that will be copied in the "filename" column.
     * @return <code>true</code> if the image has been added to the database, or
     *         <code>false</code> if an image with the same filename was already
     *         presents in the database. In the later case, the database is not
     *         updated and a information message is logged.
     * @throws SQLException if the operation failed.
     */
    public synchronized boolean addImage(final GridCoverage coverage, final String filename) throws SQLException {
        /*
         * Gets the geographic (mandatory) and the temporal (optional)
         * coordinate systems. If there is no temporal coordinate CS,
         * then <code>timeCS</code> will be <code>null</code>.
         */
        final CoordinateSystem cs = coverage.getCoordinateSystem();
        final GeographicCoordinateSystem geoCS;
        final TemporalCoordinateSystem  timeCS;
        try {
            if (cs instanceof CompoundCoordinateSystem) {
                final CompoundCoordinateSystem ccs = (CompoundCoordinateSystem) cs;
                geoCS  = (GeographicCoordinateSystem) ccs.getHeadCS();
                timeCS = (TemporalCoordinateSystem)   ccs.getTailCS();
            } else {
                geoCS  = (GeographicCoordinateSystem) cs;
                timeCS = null;
            }
        } catch (ClassCastException exception) {
            SQLException e = new SQLException(Resources.format(ResourceKeys.ERROR_BAD_COORDINATE_SYSTEM));
            e.initCause(exception);
            throw e;
        }
        /*
         * Gets the ellipsoid. Current implementation requires a
         * WGS84 ellipsoid, since this is the expected ellipsoid
         * in the database.
         */
        final Ellipsoid ellipsoid = geoCS.getHorizontalDatum().getEllipsoid();
        if (!Ellipsoid.WGS84.equals(ellipsoid) || !TemporalDatum.UTC.equals(timeCS.getTemporalDatum())) {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_BAD_COORDINATE_SYSTEM));
        }
        /*
         * Gets the envelope, image size, area, start time and end time.
         * TODO: We should check for 2D-only coverage! In this case, there
         *       is no temporal axis (timeCS==null) and no start/end time.
         */
        final Envelope envelope = coverage.getEnvelope();
        final GridRange   range = coverage.getGridGeometry().getGridRange();
        final Rectangle2D  area = envelope.getSubEnvelope(0,2).toRectangle2D();
        final Dimension    size = new Dimension(range.getLength(0), range.getLength(1));
        final Date    startTime = getDate(timeCS, envelope.getMinimum(2));
        final Date      endTime = getDate(timeCS, envelope.getMaximum(2));
        final int        areaID = addArea(area, size);
        /*
         * Check if the image already exist. If there is already an image
         * with the same name for the same group, log an information message
         * and return from this method.
         */
        if (selectImage==null) {
            selectImage = connection.prepareStatement(SELECT_IMAGE);
        }
        selectImage.setInt   (1, groupID);
        selectImage.setString(2, filename);
        Integer ID = executeQuery(selectImage, ResourceKeys.ERROR_DUPLICATED_IMAGE_$1);
        if (ID != null) {
            log(Resources.getResources(null).getLogRecord(Level.INFO, ResourceKeys.ERROR_IMAGE_ALREADY_EXIST_$1, filename));
            return false;
        }
        /*
         * The image is not already present. Insert the image,
         * and log an information message with the SQL statement.
         */
        if (insertImage == null) {
            insertImage = connection.createStatement();
        }
        if (insertImage.executeUpdate(sqlInsertImage(null, filename, startTime, endTime, areaID)) == 1) {
            ID = executeQuery(selectImage, ResourceKeys.ERROR_DUPLICATED_IMAGE_$1);
            log(new LogRecord(DataBase.SQL_UPDATE, sqlInsertImage(ID, filename, startTime, endTime, areaID)));
            if (ID != null) {
                return true;
            }
        }
        // Should not happen.
        throw new SQLException("Error while updating "+IMAGES);
    }

    /**
     * Add an entry to the <code>Areas</code> table,
     * if not already presents.
     *
     * @param  area The geographic area, in degrees of latitude
     *              and longitude on the WGS84 ellipsoid.
     * @param  size The image size, in pixels.
     * @return The ID for the area (either an existing one or a new one).
     * @throws SQLException if the operation failed.
     */
    private int addArea(final Rectangle2D area, final Dimension size) throws SQLException {
        if (selectArea == null) {
            selectArea = connection.prepareStatement(SELECT_AREA);
        }
        selectArea.setDouble(1, area.getMinX());
        selectArea.setDouble(2, area.getMaxX());
        selectArea.setDouble(3, area.getMinY());
        selectArea.setDouble(4, area.getMaxY());
        selectArea.setInt   (5, size.width);
        selectArea.setInt   (6, size.height);

        Integer ID = executeQuery(selectArea, ResourceKeys.ERROR_DUPLICATED_AREA_$1);
        if (ID != null) {
            return ID.intValue();
        }
        if (insertArea == null) {
            insertArea = connection.createStatement();
        }
        if (insertArea.executeUpdate(sqlInsertArea(null, area, size)) == 1) {
            ID = executeQuery(selectArea, ResourceKeys.ERROR_DUPLICATED_AREA_$1);
            log(new LogRecord(DataBase.SQL_UPDATE, sqlInsertArea(ID, area, size)));
            if (ID != null) {
                return ID.intValue();
            }
        }
        // Should not happen.
        throw new SQLException("Error while updating "+AREAS);
    }

    /**
     * Construct an SQL string for updating the IMAGES table.
     */
    private String sqlInsertImage(final Integer ID, final String filename,
                                  final Date startTime, final Date endTime, final int areaID)
    {
        if (dateFormat==null) {
            dateFormat = new SimpleDateFormat("#MM/dd/yyyy HH:mm:ss#", Locale.US);
            dateFormat.setTimeZone(timezone);
        }
        final StringBuffer buffer = new StringBuffer("INSERT INTO ");
        buffer.append(IMAGES);
        buffer.append(" (");
        if (ID != null) {
            buffer.append("ID, ");
        }
        buffer.append("groupe, filename, start_time, end_time, area) VALUES(");
        if (ID != null) {
            buffer.append(ID);
            buffer.append(", ");
        }
        buffer.append(groupID);                      buffer.append(", '");
        buffer.append(filename);                     buffer.append("', ");
        buffer.append(dateFormat.format(startTime)); buffer.append(", ");
        buffer.append(dateFormat.format(  endTime)); buffer.append(", ");
        buffer.append(areaID);                       buffer.append(')' );
        return buffer.toString();
    }

    /**
     * Construct an SQL string for updating the AREA table.
     */
    private static String sqlInsertArea(final Integer ID, final Rectangle2D area, final Dimension size) {
        final StringBuffer buffer=new StringBuffer("INSERT INTO ");
        buffer.append(AREAS);
        buffer.append(" (");
        if (ID != null) {
            buffer.append("ID, ");
        }
        buffer.append("ellipsoid, xmin, xmax, ymin, ymax, width, height) VALUES(");
        if (ID != null) {
            buffer.append(ID);
            buffer.append(", ");
        }
        buffer.append("'WGS 1984', ");
        buffer.append(area.getMinX()); buffer.append(", ");
        buffer.append(area.getMaxX()); buffer.append(", ");
        buffer.append(area.getMinY()); buffer.append(", ");
        buffer.append(area.getMaxY()); buffer.append(", ");
        buffer.append(size.width    ); buffer.append(", ");
        buffer.append(size.height   ); buffer.append(')' );
        return buffer.toString();
    }

    /**
     * Lib�re les ressources utilis�es par cette table.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *        lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        if (insertImage != null) {
            insertImage.close();
            insertImage=null;
        }
        if (insertArea != null) {
            insertArea.close();
            insertArea=null;
        }
        if (selectImage != null) {
            selectImage.close();
            selectImage=null;
        }
        if (selectArea != null) {
            selectArea.close();
            selectArea=null;
        }
    }
}
