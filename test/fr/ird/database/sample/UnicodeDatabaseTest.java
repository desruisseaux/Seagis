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
package fr.ird.database.sample;

// J2SE dependencies
import java.io.*;
import java.sql.*;
import java.util.*;

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.resources.Arguments;
import org.geotools.util.MonolineFormatter;


/**
 * Teste la lecture des caractères unicodes à partir de la base de données.
 * Ce test utilise la table "Descripteurs", qui contient des symboles de
 * gradients et des indices.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class UnicodeDatabaseTest extends TestCase {
    /**
     * Périphérique de sortie, ou <code>null</code> si aucun.
     */
    private static PrintWriter out;

    /**
     * La requête a exécuter.
     */
    private PreparedStatement statement;

    /**
     * Construit la suite de tests.
     */
    public UnicodeDatabaseTest(final String name) {
        super(name);
    }

    /**
     * Etablit la connexion avec la base de données.
     */
    protected void setUp() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("charSet", "UTF-8");
        properties.put("useUnicode", "true");
        final Driver driver = (Driver)Class.forName("sun.jdbc.odbc.JdbcOdbcDriver").newInstance();
        final Connection connection = DriverManager.getConnection("jdbc:odbc:SEAS-Sennes", properties);
        statement = connection.prepareStatement("SELECT nom FROM Descripteurs");
    }

    /**
     * Ferme la connexion avec la base de données.
     */
    protected void tearDown() throws SQLException {
        final Connection connection = statement.getConnection();
        statement.close();
        connection.close();
        if (out != null) {
            out.flush();
        }
    }

    /**
     * Vérifie que la chaîne est légale. Le pilote jdbc-odbc avec Access remplace
     * certains caractères unicode par un point d'interrogation. Cette méthode
     * vérifie que la chaîne n'en contient pas.
     */
    private static void checkName(final String name) {
        if (out != null) {
            out.println(name);
        }
        assertTrue(name.indexOf('?') < 0);
    }

    /**
     * Teste {@link ResultSet#getString}.
     */
    public void testString() throws SQLException {
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final String name = results.getString(1);
            checkName(name);
        }
        results.close();
    }

    /**
     * Teste {@link ResultSet#getBytes}.
     */
    public void testBytes() throws SQLException {
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final byte[] bytes = results.getBytes(1);
            final String name = new String(bytes);
            checkName(name);
        }
        results.close();
    }

    /**
     * Teste {@link ResultSet#getAsciiStream}.
     */
    public void testCharacterStream() throws SQLException, IOException {
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final StringBuffer buffer = new StringBuffer();
            final Reader  stream = results.getCharacterStream(1);
            int lower;
            while ((lower=stream.read()) >= 0) {
                buffer.append((char) lower);
            }
            stream.close();
            final String name = buffer.toString();
            checkName(name);
        }
        results.close();
    }

    /**
     * Teste {@link ResultSet#getAsciiStream}.
     */
    public void testAsciiStream() throws SQLException, IOException {
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final StringBuffer buffer = new StringBuffer();
            final InputStream  stream = results.getAsciiStream(1);
            int lower;
            while ((lower=stream.read()) >= 0) {
                buffer.append((char) lower);
            }
            stream.close();
            final String name = buffer.toString();
            checkName(name);
        }
        results.close();
    }

    /**
     * Teste {@link ResultSet#getUnicodeStream}.
     */
    public void testUnicodeStream() throws SQLException, IOException {
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final StringBuffer buffer = new StringBuffer();
            final InputStream  stream = results.getUnicodeStream(1);
            int lower,upper;
            while ((upper=stream.read()) >= 0) {
                if ((lower=stream.read()) >= 0) {
                    buffer.append((char) ((upper << 8) | lower));
                }
            }
            stream.close();
            final String name = buffer.toString();
            checkName(name);
        }
        results.close();
    }

    /**
     * Teste {@link ResultSet#getBinaryStream}.
     */
    public void testBinaryStream() throws SQLException, IOException {
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final StringBuffer buffer = new StringBuffer();
            final InputStream  stream = results.getBinaryStream(1);
            int lower;
            while ((lower=stream.read()) >= 0) {
                buffer.append((char) lower);
            }
            stream.close();
            final String name = buffer.toString();
            checkName(name);
        }
        results.close();
    }

    /**
     * Retourne la suite de tests.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(UnicodeDatabaseTest.class);
        return suite;
    }

    /**
     * Code propose sur le forum de discussion "Unicode and Access 2000 using JDBC":
     *
     * http://forum.java.sun.com/thread.jsp?forum=48&thread=231861
     */
    public static void testTableCreation() throws Exception {
        if (false) {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            // When UTF-8 is used, inserting and retrieving the data works 
            // correct (as with MySQL)! However, the column displayed with the
            // Access GUI is NOT correct!
            // UTF-16, UTF-16BE, UTF-16LE all generate exceptions!
            Properties prop = new Properties();            
            prop.put("charSet", "UTF-8");
            Connection con = DriverManager.getConnection("jdbc:odbc:SEAS-Tests", prop);
            String s = "\u20AC (euro) \u2126 (ohm)";
            Statement stm = con.createStatement();
            stm.executeUpdate("CREATE TABLE TestUnicode (code VARCHAR)");
            stm.executeUpdate("INSERT INTO TestUnicode (code) VALUES (\'"+s+"\')");
            ResultSet rs = stm.executeQuery("SELECT code FROM TestUnicode");
            if (!rs.next()) System.err.println("No row?!");
            System.out.println(s.equals(rs.getString("code")) ? "OK" : "FAILED");
        }
    }

    /**
     * Exécute la suite de tests à partir de la ligne de commande.
     */
    public static void main(final String[] args) {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        Locale.setDefault(arguments.locale);
        //out = arguments.out;
        junit.textui.TestRunner.run(suite());
    }
}
