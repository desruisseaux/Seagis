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
import java.util.Date;
import java.util.Arrays;
import java.util.Locale;
import java.util.Collection;
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
 * Teste le bon fonctionnement de {@link ParameterCoverage3D}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ParameterCoverageTest extends TestCase {
    /**
     * La base de données d'images.
     */
    private CoverageDataBase database;

    /**
     * La couverture à tester.
     */
    private ParameterCoverage3D coverage;

    /**
     * Liste des paramètres déclarées dans la base de données.
     */
    private Collection<+ParameterEntry> parameters;

    /**
     * Objet à utiliser pour lire et écrire des dates.
     * Le format attendu est de la forme "24/12/1997".
     */
    private DateFormat dateFormat;

    /**
     * Construit la suite de tests.
     */
    public ParameterCoverageTest(final String name) {
        super(name);
    }

    /**
     * Etablit la connexion avec la base de données.
     */
    protected void setUp() throws SQLException {
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        database   = new fr.ird.database.coverage.sql.CoverageDataBase();
        coverage   = new ParameterCoverage3D(database.getCoverageTable(), null);
        SampleDataBase samples = new fr.ird.database.sample.sql.SampleDataBase();
        parameters = samples.getParameters(database.getSeriesTable());
        samples.close();
    }

    /**
     * Ferme la connexion avec la base de données.
     */
    protected void tearDown() throws SQLException {
        coverage.dispose();
        database.close();
    }

    /**
     * Test des valeurs individuelles.
     */
    public void testValues() throws SQLException, ParseException {
        setParameter("PP1");
        evaluate(40.10,  -18.33,  "19/08/1999");
        evaluate(53.36,    3.65,  "14/11/1999");
        evaluate(56.95,   -3.53,  "25/11/1999");
        evaluate(38.05,  -19.23,  "18/08/1999");
        evaluate(39.80,  -17.90,  "18/08/1999");
        evaluate(47.53,  -11.95,  "21/08/1999");
    }

    /**
     * Définit le paramètre à utiliser.
     */
    private void setParameter(final String parameter) throws SQLException {
        ParameterEntry entry = null;
        for (final ParameterEntry param : parameters) {
            if (parameter.equalsIgnoreCase(param.getName())) {
                entry = param;
                break;
            }
        }
        coverage.setParameter(entry);
    }
    

    /**
     * Retourne la valeur évaluée à la position spécifiée.
     * Cette méthode est un bon endroit où placer un point d'arrêt à des fins de déboguage.
     */
    private float evaluate(final double x, final double y, final String date) throws ParseException {
        final Point2D coord = new Point2D.Double(x,y);
        final Date    time  = dateFormat.parse(date);
        return (float)coverage.evaluate(coord, time); //       <--- Break point ici
    }

    /**
     * Retourne la suite de tests.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ParameterCoverageTest.class);
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
