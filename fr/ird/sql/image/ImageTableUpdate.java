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
package fr.ird.sql.image;

// Databases
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// OpenGIS
import net.seagis.pt.Envelope;
import net.seagis.cs.Ellipsoid;
import net.seagis.gc.GridRange;
import net.seagis.gc.GridCoverage;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.CompoundCoordinateSystem;
import net.seagis.cs.TemporalCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;

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


/**
 * Add new entry to the images table and related tables.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageTableUpdate extends Table
{
    private static final String SELECT_AREA = "SELECT ID FROM "+AREAS+" WHERE "+
                                              "xmin=? AND xmax=? AND "+
                                              "ymin=? AND ymax=? AND "+
                                              "width=? AND height=?";

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
     * Statement for "INSERT INTO Areas...".
     * This statement is built by {@link #addArea} when first needed.
     */
    private Statement insertArea;

    /**
     * Statement for "INSERT INTO Images...".
     * This statement is built by {@link #add} when first needed.
     */
    private Statement insertImages;

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
     * Construct a new <code>ImageTableUpdate</code>.
     *
     * @param connection The connection to the database.
     * @param timezone   The database time zone.
     */
    public ImageTableUpdate(final Connection connection, final TimeZone timezone)
    {
        this.connection = connection;
        this.timezone   = timezone;
    }

    /**
     * Add an entry to the <code>Images</code> table.
     *
     * @throws SQLException if the operation failed.
     */
    public synchronized void add(final GridCoverage coverage, final File file, final int groupID) throws SQLException
    {
        CoordinateSystem cs = coverage.getCoordinateSystem();
        final GeographicCoordinateSystem geoCS;
        if (cs instanceof CompoundCoordinateSystem)
        {
            cs = ((CompoundCoordinateSystem) cs).getHeadCS();
        }
        if (cs instanceof GeographicCoordinateSystem)
        {
            geoCS = (GeographicCoordinateSystem) cs;
            final Ellipsoid ellipsoid = geoCS.getHorizontalDatum().getEllipsoid();
            if (Ellipsoid.WGS84.equals(ellipsoid))
            {
                final Envelope envelope = coverage.getEnvelope();
                final GridRange range = coverage.getGridGeometry().getGridRange();
                final Rectangle2D area = new Rectangle2D.Double(envelope.getMinimum(0),
                                                                envelope.getMinimum(1),
                                                                envelope.getLength (0),
                                                                envelope.getLength (1));
                final Dimension  size = new Dimension(range.getLength(0), range.getLength(1));
                final String filename = getFileName(file, groupID);
                final int      areaID = addArea(area, size);
                final Date  startTime = null; // TODO
                final Date    endTime = null; // TODO
                if (dateFormat==null)
                {
                    dateFormat = new SimpleDateFormat("#dd/MM/yyyy HH:mm:ss#", Locale.US);
                    dateFormat.setTimeZone(timezone);
                }
                if (insertImages==null)
                {
                    insertImages = connection.createStatement();
                }
                if (insertImages.executeUpdate("INSERT INTO "+IMAGES+
                                 " (group,filename,start_time,end_time,area) VALUES("+
                                 groupID  + ", " +
                                 filename + ", " +
                                 dateFormat.format(startTime) + ", " +
                                 dateFormat.format(  endTime) + ", " +
                                 areaID + ')')!=1)
                {
                    // Should not happen.
                    throw new SQLException("Error while updating "+IMAGES);
                }
                return;
            }
        }
        throw new SQLException(Resources.format(ResourceKeys.ERROR_BAD_COORDINATE_SYSTEM));
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
    private int addArea(final Rectangle2D area, final Dimension size) throws SQLException
    {
        if (selectArea==null)
        {
            selectArea = connection.prepareStatement(SELECT_AREA);
        }
        selectArea.setDouble(1, area.getMinX());
        selectArea.setDouble(2, area.getMaxX());
        selectArea.setDouble(3, area.getMinY());
        selectArea.setDouble(4, area.getMaxY());
        selectArea.setInt   (5, size.width);
        selectArea.setInt   (6, size.height);
        /*
         * Looks for existing entries. The following
         * loops is excuted only 1 or 2 times.
         */
        int updateCount = 0;
        do
        {
            int ID = 0;
            boolean hasFound = false;
            final ResultSet result = selectArea.executeQuery();
            while (result.next())
            {
                final int nextID = result.getInt(1);
                if (hasFound)
                {
                    warning(ResourceKeys.ERROR_DUPLICATED_AREA_$1, new Integer(nextID));
                }
                else
                {
                    ID = nextID;
                    hasFound = true;
                }
            }
            result.close();
            if (hasFound)
            {
                return ID;
            }
            /*
             * Add a new entry. If the update has already been
             * attempted, then this is an error if we reach this point.
             */
            if (updateCount!=0)
            {
                 break;
            }
            if (insertArea==null)
            {
                insertArea = connection.createStatement();
            }
            updateCount = insertArea.executeUpdate("INSERT INTO "+AREAS+" (ellipsoid,xmin,xmax,ymin,ymax,width,height) "+
                                                   "VALUES('WGS 1984', "+
                                                   area.getMinX() + ", " +
                                                   area.getMaxX() + ", " +
                                                   area.getMinY() + ", " +
                                                   area.getMaxY() + ", " +
                                                   size.width     + ", " +
                                                   size.height    + ')');
        }
        while (updateCount==1);
        // Should not happen.
        throw new SQLException("Error while updating "+AREAS);
    }

    /**
     * Returns the filename for the specified file. Current version
     * doesn't check if the filename is consistent with path and
     * extension specified in the group. Future version may do that.
     */
    private static String getFileName(final File file, final int groupID)
    {
        final String filename = file.getName();
        final int ext = filename.lastIndexOf('.');
        return (ext>=0) ? filename.substring(0, ext) : filename;
    }

    /**
     * Libère les ressources utilisées par cette table.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *        lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (insertImages!=null)
        {
            insertImages.close();
            insertImages=null;
        }
        if (insertArea!=null)
        {
            insertArea.close();
            insertArea=null;
        }
        if (selectArea!=null)
        {
            selectArea.close();
            selectArea=null;
        }
    }

    /**
     * Issue a warning to the log.
     */
    private static void warning(final int clé, final Object param)
    {
        final Resources resources = Resources.getResources(null);
        final LogRecord record = resources.getLogRecord(Level.WARNING, clé, param);
        record.setSourceClassName ("ImageTable");
        record.setSourceMethodName("addGridCoverage");
        logger.log(record);
    }
}
