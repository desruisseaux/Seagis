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
package fr.ird.database.coverage;

// J2SE dependencies
import java.util.Date;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// JAI dependencies
import javax.media.jai.util.Range;

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Arguments;
import org.geotools.resources.MonolineFormatter;

// Seagis dependencies
import fr.ird.database.coverage.*;


/**
 * Teste le fonctionnement de {@link SeriesCoverage3D}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SeriesCoverageTest extends TestCase {
    /**
     * Connexion vers la base de données.
     */
    private CoverageDataBase database;

    /**
     * La couverture à tester. Sera construit par les différentes méthodes <code>testXXX</code>.
     */
    private SeriesCoverage3D coverage;

    /**
     * Objet à utiliser pour lire et écrire des dates.
     * Le format attendu est de la forme "24/12/1997".
     */
    private DateFormat dateFormat;

    /**
     * Construit la suite de tests.
     */
    public SeriesCoverageTest(final String name) {
        super(name);
    }

    /**
     * Etablit la connexion avec la base de données.
     */
    protected void setUp() throws SQLException {
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        database = new fr.ird.database.coverage.sql.CoverageDataBase();
    }

    /**
     * Ferme la connexion avec la base de données.
     */
    protected void tearDown() throws SQLException {
        database.close();
    }

    /**
     * Teste quelques valeurs de chlorophylle.
     */
    public void testCHL() throws Exception {
        final CoverageTable table = database.getCoverageTable("CHL (Monde - hebdomadaires)");
        coverage = new SeriesCoverage3D(table);
        coverage.setInterpolationAllowed(false);
        assertEquals(0.0851138f, evaluate(66.6100,  -3.2100, "24/12/1997"), 0.00001f);
        assertEquals(0.0851138f, evaluate(60.9576, -11.6657, "15/03/1998"), 0.00001f);
        assertTrue  (Float.isNaN(evaluate(52.6300,  +3.6600, "15/06/1999")));
        table.close();
    }

    /**
     * Teste quelques valeurs de hauteur de l'eau.
     */
    public void testSLA() throws Exception {
        final CoverageTable table = database.getCoverageTable("SLA (Monde - TP/ERS)");
        //
        // Teste l'extraction de valeurs en obtenant directement les images.
        //
        final Range oldRange = table.getTimeRange();
        table.setTimeRange(dateFormat.parse("01/03/1996"), dateFormat.parse("02/03/1996"));
        CoverageEntry entry = table.getEntry();
        GridCoverage  grid  = entry.getGridCoverage(null); // Déclanche la lecture maintenant.
        double[]     output = null;
        assertEquals(  4.1f, (output=grid.evaluate(new Point2D.Double(12.00+.25/2, -61.50+.25/2), output))[0], 0.0001f);
        assertEquals( 11.7f, (output=grid.evaluate(new Point2D.Double(17.00+.25/2, -52.25+.25/2), output))[0], 0.0001f);
        assertEquals( 15.0f, (output=grid.evaluate(new Point2D.Double(20.00+.25/2, -41.25+.25/2), output))[0], 0.0001f);
        assertEquals( -0.1f, (output=grid.evaluate(new Point2D.Double(22.50+.25/2,  75.25+.25/2), output))[0], 0.0001f);
        table.setTimeRange(oldRange);
        //
        // Teste l'extraction de valeurs via une vue 3D.
        //
        coverage = new SeriesCoverage3D(table);
        coverage.setInterpolationAllowed(false);
        assertEquals( 20.4f, evaluate(60.9576, -11.6657, "15/03/1998"), 0.0001f);
        assertEquals(-10.9f, evaluate(61.7800,  -3.5100, "06/01/1997"), 0.0001f);
        assertEquals( 20.5f, evaluate(49.6000,  -5.8600, "03/03/1993"), 0.0001f);

        // Valeurs puisées dans les fichiers textes.
        assertEquals(  4.1f, evaluate(12.00+.25/2, -61.50+.25/2, "01/03/1996"), 0.0001f);
        assertEquals( 11.7f, evaluate(17.00+.25/2, -52.25+.25/2, "01/03/1996"), 0.0001f);
        assertEquals( 15.0f, evaluate(20.00+.25/2, -41.25+.25/2, "01/03/1996"), 0.0001f);
        assertEquals( -0.1f, evaluate(22.50+.25/2,  75.25+.25/2, "01/03/1996"), 0.0001f);

        // Valeurs puisées dans les fichiers textes aux même positions deux jours consécutifs.
        assertEquals(  4.5f, evaluate( 6.75+.25/2,  77.00+.25/2, "04/07/1999"), 0.0001f);
        assertEquals(-18.1f, evaluate(15.25+.25/2,  35.00+.25/2, "04/07/1999"), 0.0001f);
        assertEquals(-40.0f, evaluate(17.25+.25/2, -40.75+.25/2, "04/07/1999"), 0.0001f);
        assertEquals( 13.9f, evaluate(21.25+.25/2, -45.50+.25/2, "04/07/1999"), 0.0001f);

        assertEquals(  3.5f, evaluate( 6.75+.25/2,  77.00+.25/2, "14/07/1999"), 0.0001f);
        assertEquals(-13.5f, evaluate(15.25+.25/2,  35.00+.25/2, "14/07/1999"), 0.0001f);
        assertEquals(-38.1f, evaluate(17.25+.25/2, -40.75+.25/2, "14/07/1999"), 0.0001f);
        assertEquals(  8.3f, evaluate(21.25+.25/2, -45.50+.25/2, "14/07/1999"), 0.0001f);

        coverage.setInterpolationAllowed(true);
        // Utilise une tolérance égale à la pente de la droite reliant les deux points
        // dans le temps: (SLA2 - SLA1) / 10 jours.  Autrement dit, accepte une erreur
        // de 24 heures dans la date.
        assertEquals(  4.5f, evaluate( 6.75+.25/2,  77.00+.25/2, "04/07/1999"), 0.10f);
        assertEquals(-18.1f, evaluate(15.25+.25/2,  35.00+.25/2, "04/07/1999"), 0.46f);
        assertEquals(-40.0f, evaluate(17.25+.25/2, -40.75+.25/2, "04/07/1999"), 0.19f);
        assertEquals( 13.9f, evaluate(21.25+.25/2, -45.50+.25/2, "04/07/1999"), 0.56f);

        assertEquals(  3.5f, evaluate( 6.75+.25/2,  77.00+.25/2, "14/07/1999"), 0.10f);
        assertEquals(-13.5f, evaluate(15.25+.25/2,  35.00+.25/2, "14/07/1999"), 0.46f);
        assertEquals(-38.1f, evaluate(17.25+.25/2, -40.75+.25/2, "14/07/1999"), 0.19f);
        assertEquals(  8.3f, evaluate(21.25+.25/2, -45.50+.25/2, "14/07/1999"), 0.56f);

        assertEquals(  4.0f, evaluate( 6.75+.25/2,  77.00+.25/2, "09/07/1999"), 0.10f);
        assertEquals(-15.8f, evaluate(15.25+.25/2,  35.00+.25/2, "09/07/1999"), 0.46f);
        assertEquals(-39.1f, evaluate(17.25+.25/2, -40.75+.25/2, "09/07/1999"), 0.19f);
        assertEquals( 11.1f, evaluate(21.25+.25/2, -45.50+.25/2, "09/07/1999"), 0.56f);

        table.close();
    }

    /**
     * Retourne la valeur de la première bande évaluée à la position spécifiée.
     * Cette méthode est un bon endroit où placer un point d'arrêt à des fins de déboguage.
     */
    private float evaluate(final double x, final double y, final String date) throws ParseException {
        final Point2D coord = new Point2D.Double(x,y);
        final Date    time  = dateFormat.parse(date);
        float[] array=null, compare=null;
        array   = coverage.evaluate(coord, time, array); //       <--- Break point ici
        compare = coverage.getGridCoverage2D(time).evaluate(coord, compare);
        assertTrue(Arrays.equals(array, compare));
        return array[0];
    }

    /**
     * Retourne la suite de tests.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SeriesCoverageTest.class);
        return suite;
    }

    /**
     * Exécute la suite de tests à partir de la ligne de commande.
     */
    public static void main(final String[] args) {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        Locale.setDefault(arguments.locale);
        junit.textui.TestRunner.run(suite());
    }
}
