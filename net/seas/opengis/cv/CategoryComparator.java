/*
 * OpenGIS implementation in Java
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.cv;

// Collections
import java.util.Arrays;
import java.util.Comparator;


/**
 * Comparateur ayant la charge de classer des catégories {@link Category} en fonction
 * d'un de leurs champs. Les champs pris en compte sont {@link Category#lower} et
 * {@link Category#minimum}, qui interviennent respectivement dans les décodages
 * et encodages des pixels.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class CategoryComparator implements Comparator<Category>
{
    /**
     * Comparateur servant à classer les thèmes en ordre
     * croissant d'index {@link Category#lower}.
     */
    public static final CategoryComparator BY_INDEX=new CategoryComparator()
    {
        protected float getLower(final Category category) {return category.lower;}
        protected float getUpper(final Category category) {return category.upper;}
    };

    /**
     * Comparateur servant à classer les thèmes en ordre
     * croissant de valeurs {@link Category#minimum}.
     */
    public static final CategoryComparator BY_VALUES=new CategoryComparator()
    {
        protected float getLower(final Category category) {return category.minimum;}
        protected float getUpper(final Category category) {return category.maximum;}
    };

    /**
     * Retourne la valeur du champ à comparer. Cette méthode retournera
     * {@link Category#lower} ou {@link Category#minimum} selon
     * l'implémentation utilisée.
     */
    protected abstract float getLower(final Category category);

    /**
     * Retourne la valeur du champ à comparer. Cette méthode retournera
     * {@link Category#upper} ou {@link Category#maximum} selon
     * l'implémentation utilisée.
     */
    protected abstract float getUpper(final Category category);

    /**
     * Compare deux objets {@link Category}. Cette méthode sert à
     * classer les thèmes en ordre croissant de valeurs de leur
     * champ {@link #getLower}.
     */
    public final int compare(final Category o1, final Category o2)
    {
        final float v1 = getLower(o1);
        final float v2 = getLower(o2);
        final int cmp=Float.compare(v1, v2);
        if (cmp==0)
        {
            // Special test for NaN
            final int bits1 = Float.floatToRawIntBits(v1);
            final int bits2 = Float.floatToRawIntBits(v2);
            if (bits1 < bits2) return -1;
            if (bits1 > bits2) return +1;
        }
        return cmp;
    }

    /**
     * Classe le tableau <code>category</code>, puis retourne uu tableau
     * qui contiendra tous les index {@link Category#upper} ou
     * {@link Category#maximum} du tableau.
     */
    public final float[] sort(final Category[] categories)
    {
        Arrays.sort(categories, this);
        assert(isSorted(categories));
        final float[] index=new float[categories.length];
        for (int i=0; i<index.length; i++)
            index[i] = getLower(categories[i]);
        return index;
    }

    /**
     * Vérifie si le tableau de thèmes spécifié est bien en ordre croissant.
     * La comparaison ne tient pas compte des valeurs <code>NaN</code>.
     */
    public final boolean isSorted(final Category[] categories)
    {
        for (int i=1; i<categories.length; i++)
            if (getUpper(categories[i-1]) >= getLower(categories[i]))
                return false;
        return true;
    }
}
