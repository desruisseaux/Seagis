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
package fr.ird.animat.impl;

// J2SE dependencies
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.Serializable;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.pt.CoordinatePoint;

// Animat dependencies
import fr.ird.animat.Observation;


/**
 * Un paramètre observé par les {@linkplain Animal animaux}. La classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plutôt des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Parameter implements fr.ird.animat.Parameter, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -1934991927931117874L;

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
     * L'implémentation par défaut obtient la couverture de ce paramètre environnemental avec
     * {@link Environment#getCoverage(Parameter) Environment.getCoverage(...)} et appelle
     * {@link Coverage#evaluate(CoordinatePoint, float[]) Coverage.evaluate(...)}.
     *
     * @param animal L'animal pour lequel obtenir la valeur de ce paramètre.
     * @param coord  La position de cet animal, en degrés de longitude et de latitude.
     * @param perceptionArea La région jusqu'où s'étend la perception de cet animal.
     * @param dest Le tableau dans lequel mémoriser les valeurs de ce paramètre, ou <code>null</code>.
     * @return Le tableau <code>dest</code>, ou un nouveau tableau si <code>dest</code> était nul.
     */
    protected float[] evaluate(final Animal         animal,
                               final CoordinatePoint coord,
                               final Shape  perceptionArea,
                               final float[]          dest)
    {
        return animal.getPopulation().getEnvironment().getCoverage(this).evaluate(coord, dest);
    }

    /**
     * Retourne le nombre d'éléments valides dans le tableau retourné par la méthode
     * {@link #evaluate evaluate(...)}. Ce nombre sera généralement de 1 ou 3.
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
     * Retourne une valeur de hachage pour cet objet.
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Compare ce paramètre avec l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            return name.equals(((Parameter) object).name);
        }
        return false;
    }
}
