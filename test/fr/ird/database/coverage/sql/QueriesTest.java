/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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

// J2SE dependencies
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.awt.geom.Rectangle2D;
import javax.swing.tree.TreeModel;

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Arguments;
import org.geotools.util.MonolineFormatter;
import org.geotools.gui.swing.tree.*;

// Seagis dependencies
import fr.ird.database.sql.AbstractDataBase;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageRanges;


/**
 * Tests connections to the database and queries.
 * There is few really usefull tests in this class.
 * Most tests really just verify if methods can be
 * run without raising an exception.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class QueriesTest extends TestCase {
    /**
     * Name of a format to use for tests.
     */
    private static final String FORMAT =
            "ASC Anomalie de hauteur de l'eau et courants géostrophiques";

    /**
     * Name of a series to use for tests.
     */
    private static final String SERIES = "SLA (Monde - TP)";

    /**
     * Timezone to use for tests.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * The entry for {@link #SERIES}.
     */
    private static SeriesEntry series;

    /**
     * The connection to the database.
     */
    private Connection connection;

    /**
     * Create a new test suite.
     */
    public QueriesTest(final String name) {
        super(name);
    }

    /**
     * Get the initial JDBC connection.
     */
    protected void setUp() throws Exception {
        Class.forName(AbstractDataBase.DRIVER.defaultValue).newInstance();
        connection = DriverManager.getConnection(AbstractDataBase.SOURCE  .defaultValue,
                                                 AbstractDataBase.USER    .defaultValue,
                                                 AbstractDataBase.PASSWORD.defaultValue);
    }

    /**
     * Close the JDBC connection.
     */
    protected void tearDown() throws SQLException {
        connection.close();
        connection = null;
    }

    /**
     * Returns the test suite.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(QueriesTest.class);
        return suite;
    }

    /**
     * Run the test suite from the command line.
     */
    public static void main(final String[] args) {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        Locale.setDefault(arguments.locale);
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Test the category table.
     */
    public void testCategory() throws Exception {
        final CategoryTable table = new CategoryTable(null, connection);
        assertEquals(0, table.getCategories(0).length);
        table.close();
    }

    /**
     * Test the sample dimension table.
     */
    public void testSampleDimension() throws Exception {
        final SampleDimensionTable table = new SampleDimensionTable(null, connection);
        assertEquals(0, table.getSampleDimensions("dummy").length);
        assertEquals(3, table.getSampleDimensions(FORMAT ).length);
        table.close();
    }

    /**
     * Test the format table.
     */
    public void testFormat() throws Exception {
        final FormatTable table = new FormatTable(null, connection);
        final FormatEntry entry = table.getEntry(FORMAT);
        assertEquals(FORMAT, entry.getName());
        assertEquals(3, entry.getSampleDimensions().length);
        assertEquals(entry, entry);
        table.close();
    }

    /**
     * Test the series table.
     */
    public void testSeries() throws Exception {
        final SeriesTable table = new SeriesTable(null, connection);
        TreeModel tree = table.getTree(SeriesTable.CATEGORY_LEAF);
        series = table.getEntry(SERIES);
        table.close();
    }

    /**
     * Test the series table.
     */
    public void testBoundingBox() throws Exception {
        final GeographicBoundingBoxTable table = new GeographicBoundingBoxTable(null, connection, UTC);
        assertFalse  (table.getGeographicArea().isEmpty());
        assertNotNull(table.getTimeRange());
        table.close();
    }

    /**
     * Test the coverage table.
     */
    public void testGridCoverage() throws Exception {
        if (series == null) {
            testSeries();
        }
        final GridCoverageTable table = new GridCoverageTable(null, connection, UTC);
        table.setSeries(series);
        table.setGeographicArea(new Rectangle2D.Float(-180, -90, 360, 180));
        table.setTimeRange(new Date(0), new Date());
        assertNull   (table.getOperation());
        assertNull   (table.getPreferredResolution());
        assertNotNull(table.getTimeRange());
        assertEquals (table.getEnvelope().getSubEnvelope(0, 2), new Envelope(table.getGeographicArea()));
        assertNotNull(table.getRanges(new CoverageRanges(true,true,true,true)));
        final GridCoverageEntry entry = (GridCoverageEntry) table.getEntry();
        assertSame   (entry, table.getEntry());
        assertSame   (entry, table.getEntry(entry.getName()));
        assertNotNull(entry.getURL());
        assertNotNull(entry.getGridGeometry());
        assertFalse  (entry.getGeographicArea().isEmpty());
        assertEquals (entry.getEnvelope().getSubEnvelope(0, 2), new Envelope(entry.getGeographicArea()));
        assertTrue   (entry.getStartTime().before(entry.getEndTime()));
        assertTrue   (entry.getGeographicArea().intersects(table.getGeographicArea()));
        table.close();
    }
}
