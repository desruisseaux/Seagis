/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 * Un param�tre observ� par les {@linkplain Animal animaux}. La classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plut�t des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Parameter extends RemoteObject implements fr.ird.animat.Parameter {
    /**
     * Le cap des animaux, ainsi que leur position actuelle. Le cap peut �tre obtenu
     * par un appel � {@link #getValue}, alors que la position peut �tre obtenue par
     * un appel � {@link #getLocation}.
     */
    public static final Parameter HEADING = new Parameter("Heading");

    /**
     * Le nom de ce param�tre.
     */
    private final String name;

    /**
     * Nombre de fois que cet objet a �t� export�.
     */
    private transient int exportCount;

    /**
     * Construit un nouveau param�tre.
     *
     * @param name Le nom du param�tre.
     */
    protected Parameter(final String name) {
        this.name = name.trim();
    }

    /**
     * Retourne le nom de ce param�tre. En g�n�ral, la m�thode {@link #toString}
     * retournera aussi ce m�me nom afin de faciliter l'insertion des param�tres
     * dans une interface graphique <cite>Swing</cite> (par exemple une liste
     * d�roulante).
     */
    public String getName() {
        return name;
    }

    /**
     * Retourne la plage de valeurs attendue pour ce param�tre, ou <code>null</code>
     * si elle n'est pas connue.
     */
    public NumberRange getRange() {
        return null;
    }

    /**
     * Retourne le poids de ce param�tre dans le choix de la trajectoire de l'{@linkplain Animal
     * animal} sp�cifi�. L'impl�mentation par d�faut retourne toujours 1.
     *
     * @param  L'animal pour lequel on veut le poids de ce param�tre.
     * @return Un poids �gal ou sup�rieur � 0.
     */
    public float getWeight(final fr.ird.animat.Animal animal) {
        return 1;
    }

    /**
     * Retourne la valeur de ce param�tre pour l'animal sp�cifi�. Le tableau retourn� peut avoir
     * une longueur de 1 ou 3. Les informations qu'il contient devront obligatoirement �tre dans
     * l'ordre suivant:
     * <ul>
     *   <li>La valeur du param�tre</li>
     *   <li>La longitude � laquelle cette valeur a �t� mesur�e.</li>
     *   <li>La latitude � laquelle cette valeur a �t� mesur�e.</li>
     * </ul>
     * Les deux derniers �l�ments peuvent �tre absents s'ils ne s'appliquent pas. Le nombre
     * d'�l�ments valides que contiendra le tableau est sp�cifi� par {@link #getNumSampleDimensions}.
     *
     * @param animal L'animal pour lequel obtenir la valeur de ce param�tre.
     * @param coord  La position de cet animal, en degr�s de longitude et de latitude.
     * @param perceptionArea La r�gion jusqu'o� s'�tend la perception de cet animal.
     * @param dest Le tableau dans lequel m�moriser les valeurs de ce param�tre, ou <code>null</code>.
     * @return Le tableau <code>dest</code>, ou un nouveau tableau si <code>dest</code> �tait nul.
     *         Si aucune donn�e n'est disponible pour ce param�tre � la date et position courante,
     *         alors le tableau retourn� ne contiendra que des valeurs {@link Float#NaN NaN}.
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
             * Cette exception ne comprend que les cas o� l'animal est en dehors de la couverture
             * spatiale. Les cas o� il est en dehors de la couverture temporelle ont �t� pris en
             * compte dans l'environnement, qui aura retourn� une couverture nulle.
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
     * Retourne le nombre d'�l�ments valides dans le tableau retourn� par la m�thode
     * <code>environment.{@link Environment#getCoverage getCoverage}(this)</code>.
     * Ce nombre sera g�n�ralement de 1 ou 3.
     */
    protected int getNumSampleDimensions() {
        return 1;
    }

    /**
     * Retourne le nom de ce param�tre. Cette m�thode ne retourne que le nom afin de
     * permettre aux objets <code>Parameter</code> de s'ins�rer plus facilement dans
     * des composantes graphiques de <cite>Swing</cite> tel que des menus d�roulants.
     */
    public String toString() {
        return name;
    }

    /**
     * Exporte ce param�tre de fa�on � ce qu'il puisse accepter les appels de machines distantes.
     * Etant donn� qu'un m�me objet <code>Parameter</code> peut �tre partag� par plusieurs exp�ces,
     * cette m�thode tient un compte des appels de <code>export</code> et <code>unexport</code>.
     *
     * @param  port Num�ro de port, ou 0 pour choisir un port anonyme.
     * @throws RemoteException si ce param�tre n'a pas pu �tre export�.
     */
    final void export(final int port) throws RemoteException {
        if (exportCount++ == 0) {
            UnicastRemoteObject.exportObject(this, port);
        }
    }

    /**
     * Annule l'exportation de ce param�tre. Si le param�tre �tait d�j� en train d'ex�cuter une
     * m�thode, alors <code>unexport(...)</code> attendra un maximum d'une seconde avant de forcer
     * l'arr�t de l'ex�cution.
     *
     * Etant donn� qu'un m�me objet <code>Parameter</code> peut �tre partag� par plusieurs exp�ces,
     * cette m�thode tient un compte des appels de <code>export</code> et <code>unexport</code>.
     */
    final void unexport() {
        if (--exportCount == 0) {
            Animal.unexport("Parameter", this);
        }
    }
}
