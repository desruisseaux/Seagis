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
import java.util.TimeZone;
import java.util.Collection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

// JAI dependencies
import javax.media.jai.JAI;

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.util.NumberRange;
import org.geotools.gui.swing.FrameFactory;
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
     * La couverture à tester.
     */
    private ParameterCoverage3D coverage;

    /**
     * Liste des paramètres déclarées dans la base de données.
     */
    private Collection<? extends ParameterEntry> parameters;

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
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256*1024*1024);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        coverage   = new ParameterCoverage3D();
        parameters = coverage.getParameters();
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Ferme la connexion avec la base de données.
     */
    protected void tearDown() throws SQLException {
        coverage.dispose();
    }

    /**
     * Test des valeurs individuelles. Les coordonnées spatio-temporelles utilisées dans
     * ce test correspond à des positions de captures pour lesquels on a un maximum de
     * paramètres environnementaux.
     */
    public void testValues() throws SQLException, ParseException {
        setParameter(null);
        assertTrue(Float.isNaN(evaluate(39.80,  -17.90,  "18/08/1999")));
        assertTrue(Float.isNaN(evaluate(38.05,  -19.23,  "18/08/1999")));
        assertTrue(Float.isNaN(evaluate(40.10,  -18.33,  "19/08/1999")));
        assertTrue(Float.isNaN(evaluate(47.53,  -11.95,  "21/08/1999")));
        assertTrue(Float.isNaN(evaluate(53.36,    3.65,  "14/11/1999")));
        assertTrue(Float.isNaN(evaluate(56.95,   -3.53,  "25/11/1999")));

        setParameter("SST"); // SST le jour même
        assertEquals(26.21554f,  evaluate(39.80,  -17.90,  "18/08/1999"), 0.09f); // 26.296907
        assertEquals(25.67984f,  evaluate(38.05,  -19.23,  "18/08/1999"), 0.05f); // 25.701126
        assertEquals(25.86468f,  evaluate(40.10,  -18.33,  "19/08/1999"), 0.02f); // 25.876064
        assertEquals(26.79711f,  evaluate(47.53,  -11.95,  "21/08/1999"), 0.02f); // 26.803180
        assertEquals(29.60495f,  evaluate(53.36,    3.65,  "14/11/1999"), 0.01f); // 29.602047
        assertEquals(28.23608f,  evaluate(56.95,   -3.53,  "25/11/1999"), 0.01f); // 28.235308

        // SST 5 jours avant
        assertEquals(28.90020f,  evaluate(39.80,  -17.90,  "13/08/1999"), 0.09f);
        assertEquals(26.49956f,  evaluate(38.05,  -19.23,  "13/08/1999"), 0.05f);
        assertEquals(28.69938f,  evaluate(40.10,  -18.33,  "14/08/1999"), 0.02f);
        assertEquals(27.14296f,  evaluate(47.53,  -11.95,  "16/08/1999"), 0.02f);
        assertEquals(29.55049f,  evaluate(53.36,    3.65,  "10/11/1999"), 0.05f);
        assertEquals(28.09617f,  evaluate(56.95,   -3.53,  "21/11/1999"), 0.01f);

        setParameter("SLA"); // SLA le jour même
        assertEquals( 3.084735f, evaluate(39.80,  -17.90,  "18/08/1999"), 1.0f);
        assertEquals( 22.43602f, evaluate(38.05,  -19.23,  "18/08/1999"), 3.1f);
        assertEquals( 3.027869f, evaluate(40.10,  -18.33,  "19/08/1999"), 1.5f);
        assertEquals(-2.475523f, evaluate(47.53,  -11.95,  "21/08/1999"), 1.0f);
        assertEquals(-13.31123f, evaluate(53.36,    3.65,  "14/11/1999"), 1.0f);
        assertEquals(-10.75712f, evaluate(56.95,   -3.53,  "25/11/1999"), 1.0f);

        final float SST = 10.0f; // Doit correspondre au coefficient dans la base de données.
        final float SLA =  0.1f; // Doit correspondre au coefficient dans la base de données.
        final float C   = -500f; // Doit correspondre au coefficient dans la base de données.
        setParameter("PP-Test1");// C + SST0 + SST5 + SLA
        assertEquals(C+SST*(26.21554f+28.90020f)+SLA* 3.084735f, evaluate(39.80,  -17.90,  "18/08/1999"), 1.0f);
        assertEquals(C+SST*(25.67984f+26.49956f)+SLA* 22.43602f, evaluate(38.05,  -19.23,  "18/08/1999"), 0.5f);
        assertEquals(C+SST*(25.86468f+28.69938f)+SLA* 3.027869f, evaluate(40.10,  -18.33,  "19/08/1999"), 0.5f);
        assertEquals(C+SST*(26.79711f+27.14296f)+SLA*-2.475523f, evaluate(47.53,  -11.95,  "21/08/1999"), 0.5f);
        assertEquals(C+SST*(29.60495f+29.55049f)+SLA*-13.31123f, evaluate(53.36,    3.65,  "14/11/1999"), 5.0f);
        assertEquals(C+SST*(28.23608f+28.09617f)+SLA*-10.75712f, evaluate(56.95,   -3.53,  "25/11/1999"), 0.5f);

        setParameter("PP-Test2");// C + SST0 * SST5.   L'erreur est C*(SST0*E5 + SST5*E0 + E1*E2)
        assertEquals(C+SST*(26.21554f*28.90020f), evaluate(39.80,  -17.90,  "18/08/1999"),  40f);
        assertEquals(C+SST*(25.67984f*26.49956f), evaluate(38.05,  -19.23,  "18/08/1999"),   6f);
        assertEquals(C+SST*(25.86468f*28.69938f), evaluate(40.10,  -18.33,  "19/08/1999"),   6f);
        assertEquals(C+SST*(26.79711f*27.14296f), evaluate(47.53,  -11.95,  "21/08/1999"),   6f);
        assertEquals(C+SST*(29.60495f*29.55049f), evaluate(53.36,    3.65,  "14/11/1999"), 120f);
        assertEquals(C+SST*(28.23608f*28.09617f), evaluate(56.95,   -3.53,  "25/11/1999"),   6f);
    }

    /**
     * Test la création d'une image.
     */
    public void testImages() throws SQLException, ParseException {
        if (false) {
            setParameter("PP1");
            coverage.setOutputRange(new NumberRange(-2.0, 2.0));
            assertNotNull(coverage.getSampleDimensions());
            FrameFactory.show(getGridCoverage("18/08/1999"));
            coverage.setOutputRange(null);
        }
    }

    /**
     * Définit le paramètre à utiliser.
     */
    private void setParameter(final String parameter) throws SQLException {
        ParameterEntry entry = null;
        if (parameter != null) {
            for (final ParameterEntry param : parameters) {
                if (parameter.equalsIgnoreCase(param.getName())) {
                    entry = param;
                    break;
                }
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
     * Retourne une image à la date spécifiée.
     * Cette méthode est un bon endroit où placer un point d'arrêt à des fins de déboguage.
     */
    private GridCoverage getGridCoverage(final String date) throws ParseException {
        final Date              time = dateFormat.parse(date);
        final GridCoverage      grid = coverage.getGridCoverage2D(time);
        final RenderedImage rendered = grid.getRenderedImage();
        assertEquals("Wrong data type", DataBuffer.TYPE_FLOAT, rendered.getSampleModel().getDataType());
        assertNotNull(rendered.getData());
        return grid;
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
