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

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Arguments;
import org.geotools.util.MonolineFormatter;

// Seagis dependencies
import fr.ird.database.sql.AbstractDataBase;


/**
 * Tests connections to the database and queries.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class QueriesTest extends TestCase {
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
        final SampleDimension[] bd;
        bd = table.getSampleDimensions("ASC Anomalie de hauteur de l'eau et courants géostrophiques");
        assertEquals(3, bd.length);
        table.close();
    }
}
