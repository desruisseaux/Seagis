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
package fr.ird.util;

// J2SE dependencies
import java.util.Set;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.io.PrintWriter;

// JUnit dependencies
import junit.framework.*;

// Geotools dependencies
import org.geotools.resources.Arguments;
import org.geotools.util.MonolineFormatter;


/**
 * Teste {@link CombinaisonSet}.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
public class CombinaisonSetTest extends TestCase {
    /**
     * Construit la suite de tests.
     */
    public CombinaisonSetTest(final String name) {
        super(name);
    }

    /**
     * Construit un ensemble � utiliser pour les tests.
     */
    private static Set<String> createSet(final int degree) {
        final Set<String> elements = new LinkedHashSet<String>();
        for (char c='0'; c<='9'; c++) {
            elements.add(String.valueOf(c));
        }
        return new CombinaisonSet(elements, degree);
    }

    /**
     * V�rifie que les �l�ments retourn�s par l'it�rateur sont bien uniques.
     */
    public void testUniqueness() {
        final Set<String> check = new LinkedHashSet<String>();
        for (int degree=0; degree<=4; degree++) {
            final Set<String> combinaisons = createSet(degree);
            int count = 0;
            for (final String term : combinaisons) {
                count++;
            }
            assertEquals("Size mismatches", count, combinaisons.size());
            check.clear();
            check.addAll(combinaisons);
            assertTrue("Set not equals", check.equals(combinaisons));
        }
    }

    /**
     * Ecrit le nombre de combinaisons form�s � partir d'ensemble de 1 � 10 �l�ments.
     * Cette m�thode peut �tre utilis�e pour tester le calcul du nombre de combinaisons
     * � partir du nombre d'�l�ments (on peut coller les donn�es dans un tableur et faire
     * passer une r�gression polynomiale par-dessus; il existe surement une m�thode plus
     * g�n�rique mais je ne la connais pas).
     */
    private static void printSize(final PrintWriter out, final int degree) {
        out.println("N. �l�ments\tN. combinaisons");
        for (int i='0'; i<='9'; i++) {
            Set<String> elements = new LinkedHashSet<String>();
            for (char c='0'; c<=i; c++) {
                elements.add(String.valueOf(c));
            }
            out.print(elements.size());
            out.print('\t');
            elements = new CombinaisonSet(elements, degree);
            int count = 0;
            for (final String term : elements) {
                count++;
            }
            out.println(count);
        }
    }

    /**
     * Retourne la suite de tests.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(CombinaisonSetTest.class);
        return suite;
    }

    /**
     * Ex�cute la suite de tests � partir de la ligne de commande.
     * Si l'argument "-degree" est sp�cifi� sur la ligne de commande,
     * le nombre de combinaisons sera test� et affich� � l'�cran pour
     * le degr� sp�cifi�.
     */
    public static void main(final String[] args) {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        Locale.setDefault(arguments.locale);
        final Integer degree = arguments.getOptionalInteger("-degree");
        if (degree != null) {
            printSize(arguments.out, degree.intValue());
            return;
        }
        junit.textui.TestRunner.run(suite());
    }
}
