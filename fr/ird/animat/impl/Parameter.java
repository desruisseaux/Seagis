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
import java.io.Serializable;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.cv.Coverage;

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
     * @param name  Le nom du paramètre.
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
     * Retourne la {@link Observation#getValue valeur d'une observation}.
     * L'implémentation par défaut retourne <code>data[0]</code>.
     *
     * @param  data Les valeurs extraites d'une {@linkplain Coverage couverture} de données à
     *         la position de l'animal. Ces valeurs sont généralement obtenues par la methode
     *         {@link Coverage#evaluate(CoordinatePoint, float[]) evaluate}.
     * @return La valeur de l'observation, ou {@link Float#NaN} si aucune valeur n'est disponible.
     */
    protected float getValue(final float[] data) {
        return data[0];
    }

    /**
     * Retourne la {@linkplain Observation#getLocation position d'une observation}.
     * L'implémentation par défaut retourne (<code>data[1]</code>,<code>data[2]</code>)
     *
     * @param  data Les valeurs extraites d'une {@linkplain Coverage couverture} de données à
     *         la position de l'animal. Ces valeurs sont généralement obtenues par la methode
     *         {@link Coverage#evaluate(CoordinatePoint, float[]) evaluate}.
     * @return La position de l'observation, ou <code>null</code> si aucune position n'est
     *         disposible ou si cette information ne s'applique pas à ce paramètre.
     */
    protected Point2D getLocation(final float[] data) {
        return new Point2D.Float(data[1],data[2]);
    }

    /**
     * Indique si les observations de ce paramètre se font à des positions bien précises.
     * Si cette méthode retourne <code>false</code>, alors cela signifie que la méthode
     * {@link #getLocation} ne retournera jamais une position non-nulle. L'implémentation
     * par défaut retourne toujours <code>true</code>.
     */
    protected boolean isLocalized() {
        return true;
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
