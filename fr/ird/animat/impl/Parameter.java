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
 * Un param�tre observ� par les {@linkplain Animal animaux}. La classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plut�t des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Parameter implements fr.ird.animat.Parameter, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -1934991927931117874L;

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
     * L'impl�mentation par d�faut obtient la couverture de ce param�tre environnemental avec
     * {@link Environment#getCoverage(Parameter) Environment.getCoverage(...)} et appelle
     * {@link Coverage#evaluate(CoordinatePoint, float[]) Coverage.evaluate(...)}.
     *
     * @param animal L'animal pour lequel obtenir la valeur de ce param�tre.
     * @param coord  La position de cet animal, en degr�s de longitude et de latitude.
     * @param perceptionArea La r�gion jusqu'o� s'�tend la perception de cet animal.
     * @param dest Le tableau dans lequel m�moriser les valeurs de ce param�tre, ou <code>null</code>.
     * @return Le tableau <code>dest</code>, ou un nouveau tableau si <code>dest</code> �tait nul.
     */
    protected float[] evaluate(final Animal         animal,
                               final CoordinatePoint coord,
                               final Shape  perceptionArea,
                               final float[]          dest)
    {
        return animal.getPopulation().getEnvironment().getCoverage(this).evaluate(coord, dest);
    }

    /**
     * Retourne le nombre d'�l�ments valides dans le tableau retourn� par la m�thode
     * {@link #evaluate evaluate(...)}. Ce nombre sera g�n�ralement de 1 ou 3.
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
     * Retourne une valeur de hachage pour cet objet.
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Compare ce param�tre avec l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            return name.equals(((Parameter) object).name);
        }
        return false;
    }
}
