/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.server;

// J2SE dependencies
import java.util.Arrays;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.util.NumberRange;

// Seagis dependencies
import fr.ird.animat.Observation;
import fr.ird.operator.coverage.Evaluator;


/**
 * Un paramètre observé par les {@linkplain Animal animaux}. La classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plutôt des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Parameter extends RemoteObject implements fr.ird.animat.Parameter {
    /**
     * Le cap des animaux, ainsi que leur position actuelle. Le cap peut être obtenu
     * par un appel à {@link #getValue}, alors que la position peut être obtenue par
     * un appel à {@link #getLocation}.
     */
    public static final Parameter HEADING = new Parameter("Heading");

    /**
     * Le nom de ce paramètre.
     */
    private final String name;

    /**
     * Nombre de fois que cet objet a été exporté.
     */
    private transient int exportCount;

    /**
     * Construit un nouveau paramètre.
     *
     * @param name Le nom du paramètre.
     */
    protected Parameter(final String name) {
        this.name = name.trim();
    }

    /**
     * Retourne le nom de ce paramètre. En général, la méthode {@link #toString}
     * retournera aussi ce même nom afin de faciliter l'insertion des paramètres
     * dans une interface graphique <cite>Swing</cite> (par exemple une liste
     * déroulante).
     */
    public String getName() {
        return name;
    }

    /**
     * Retourne la plage de valeurs attendue pour ce paramètre, ou <code>null</code>
     * si elle n'est pas connue.
     */
    public NumberRange getRange() {
        return null;
    }

    /**
     * Retourne le poids de ce paramètre dans le choix de la trajectoire de l'{@linkplain Animal
     * animal} spécifié. L'implémentation par défaut retourne toujours 1.
     *
     * @param  L'animal pour lequel on veut le poids de ce paramètre.
     * @return Un poids égal ou supérieur à 0.
     */
    public float getWeight(final fr.ird.animat.Animal animal) {
        return 1;
    }

    /**
     * Retourne la valeur de ce paramètre pour l'animal spécifié. Le tableau retourné peut avoir
     * une longueur de 1 ou 3. Les informations qu'il contient devront obligatoirement être dans
     * l'ordre suivant:
     * <ul>
     *   <li>La valeur du paramètre</li>
     *   <li>La longitude à laquelle cette valeur a été mesurée.</li>
     *   <li>La latitude à laquelle cette valeur a été mesurée.</li>
     * </ul>
     * Les deux derniers éléments peuvent être absents s'ils ne s'appliquent pas. Le nombre
     * d'éléments valides que contiendra le tableau est spécifié par {@link #getNumSampleDimensions}.
     *
     * @param animal L'animal pour lequel obtenir la valeur de ce paramètre.
     * @param coord  La position de cet animal, en degrés de longitude et de latitude.
     * @param perceptionArea La région jusqu'où s'étend la perception de cet animal.
     * @param dest Le tableau dans lequel mémoriser les valeurs de ce paramètre, ou <code>null</code>.
     * @return Le tableau <code>dest</code>, ou un nouveau tableau si <code>dest</code> était nul.
     *         Si aucune donnée n'est disponible pour ce paramètre à la date et position courante,
     *         alors le tableau retourné ne contiendra que des valeurs {@link Float#NaN NaN}.
     */
    final float[] evaluate(final Animal         animal,
                           final CoordinatePoint coord,
                           final Shape  perceptionArea,
                                 float[]          dest)
    {
        final Environment environment = animal.getPopulation().getEnvironment();
        try {
            final Coverage coverage = environment.getCoverage(this);
            if (coverage instanceof Evaluator) {
                return ((Evaluator) coverage).evaluate(perceptionArea, dest);
            } else if (coverage != null) {
                return coverage.evaluate(coord, dest);
            }
        } catch (PointOutsideCoverageException exception) {
            /*
             * Cette exception ne comprend que les cas où l'animal est en dehors de la couverture
             * spatiale. Les cas où il est en dehors de la couverture temporelle ont été pris en
             * compte dans l'environnement, qui aura retourné une couverture nulle.
             */
            environment.getReport().numPointOutside++;
        }
        environment.getReport().numPoints++;
        final int n = getNumSampleDimensions();
        if (dest == null) {
            dest = new float[n];
        }
        Arrays.fill(dest, 0, n, Float.NaN);
        return dest;
    }

    /**
     * Retourne le nombre d'éléments valides dans le tableau retourné par la méthode
     * <code>environment.{@link Environment#getCoverage getCoverage}(this)</code>.
     * Ce nombre sera généralement de 1 ou 3.
     */
    protected int getNumSampleDimensions() {
        return 1;
    }

    /**
     * Retourne le nom de ce paramètre. Cette méthode ne retourne que le nom afin de
     * permettre aux objets <code>Parameter</code> de s'insérer plus facilement dans
     * des composantes graphiques de <cite>Swing</cite> tel que des menus déroulants.
     */
    public String toString() {
        return name;
    }

    /**
     * Exporte ce paramètre de façon à ce qu'il puisse accepter les appels de machines distantes.
     * Etant donné qu'un même objet <code>Parameter</code> peut être partagé par plusieurs expèces,
     * cette méthode tient un compte des appels de <code>export</code> et <code>unexport</code>.
     *
     * @param  port Numéro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si ce paramètre n'a pas pu être exporté.
     */
    final void export(final int port) throws RemoteException {
        if (exportCount++ == 0) {
            UnicastRemoteObject.exportObject(this, port);
        }
    }

    /**
     * Annule l'exportation de ce paramètre. Si le paramètre était déjà en train d'exécuter une
     * méthode, alors <code>unexport(...)</code> attendra un maximum d'une seconde avant de forcer
     * l'arrêt de l'exécution.
     *
     * Etant donné qu'un même objet <code>Parameter</code> peut être partagé par plusieurs expèces,
     * cette méthode tient un compte des appels de <code>export</code> et <code>unexport</code>.
     */
    final void unexport() {
        if (--exportCount == 0) {
            Animal.unexport("Parameter", this);
        }
    }
}
