/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.coverage;

// J2SE dependencies
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.resources.Arguments;
import org.geotools.resources.MonolineFormatter;

// Seagis dependencies
import fr.ird.database.coverage.*;


/**
 * Teste le fonctionnement de {@link Coverage3D}.
 *
 * @author Martin Desruisseaux
 */
public class Coverage3DTest extends TestCase {
    /**
     * Connexion vers la base de donn�es.
     */
    private CoverageDataBase database;

    /**
     * La couverture � tester. Sera construit par les diff�rentes m�thodes <code>testXXX</code>.
     */
    private Coverage3D coverage;

    /**
     * Objet � utiliser pour lire et �crire des dates.
     * Le format attendu est de la forme "24/12/1997".
     */
    private DateFormat dateFormat;

    /**
     * Construit la suite de tests.
     */
    public Coverage3DTest(final String name) {
        super(name);
    }

    /**
     * Etablit la connexion avec la base de donn�es.
     */
    protected void setUp() throws SQLException {
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        database = new fr.ird.database.coverage.sql.CoverageDataBase();
    }

    /**
     * Ferme la connexion avec la base de donn�es.
     */
    protected void tearDown() throws SQLException {
        database.close();
    }

    /**
     * Teste quelques valeurs de chlorophylle.
     */
    public void testCHL() throws Exception {
        final CoverageTable table = database.getCoverageTable("Chlorophylle-a (Monde)");
        coverage = new Coverage3D(table);
        coverage.setInterpolationAllowed(false);
        assertEquals(0.0851138f, evaluate(66.61, -3.21, "24/12/1997"), 0.0001f);
        table.close();
    }

    /**
     * Retourne la valeur de la premi�re bande �valu�e � la position sp�cifi�e.
     * Cette m�thode est un bon endroit o� placer un point d'arr�t � des fins de d�boguage.
     */
    private float evaluate(final double x, final double y, final String date) throws ParseException {
        final Point2D coord = new Point2D.Double(x,y);
        final Date    time  = dateFormat.parse(date);
        float[] array = null;
        array = coverage.evaluate(coord, time, array); //       <--- Break point ici
        return array[0];
    }

    /**
     * Retourne la suite de tests.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(Coverage3DTest.class);
        return suite;
    }

    /**
     * Ex�cute la suite de tests � partir de la ligne de commande.
     */
    public static void main(final String[] args) {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        Locale.setDefault(arguments.locale);
        junit.textui.TestRunner.run(suite());
    }
}
