/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.image.sql;

// Divers
import java.awt.Color;
import net.seas.util.XMath;
import net.seas.opengis.cv.Category;


/**
 * Représentation d'un thème par une plage d'index. Un thème est une plage de valeurs de pixels qui
 * représentent une structure géomorphologique ou un paramètre géophysique.   Par exemple certaines
 * valeurs de pixels pourraient représenter des nuages,  tandis que d'autres pourraient représenter
 * des valeurs de températures de la surface de la mer. Chaque thème doit avoir un nom parlant pour
 * l'utilisateur.  Il peut aussi définir une équation de la forme <code>y=log10(C0+C1*x)</code> qui
 * convertit des valeurs de pixels <var>x</var> en valeurs géophysiques <var>y</var>.
 * <br><br>
 * Par design, les objets <code>Category</code> sont immutables (s'ils ne l'étaient pas, ça
 * introduirait de difficiles problèmes de synchronisation avec {@link IndexedThemeMapper}).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class LogarithmicCategory extends Category
{
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = 6537248278156251909L;

    /**
     * Construit un thème qui représentera un paramètre géophysique quantifiable. Les valeurs des
     * pixels de ce thème iront de <code>lower</code> jusqu'à <code>upper</code> <u>inclusivement</u>
     * (contrairement à la convention des tableaux en C/C++ et en Java), ce que l'on peut noter
     * par <code>[lower..upper]</code>.
     * Les pixels seront convertits en valeurs géophysiques à l'aide d'une équation de la forme
     * <code>y=10^(C0+C1*x)</code>. Les unités seront ceux de l'objet {@link IndexedThemeMapper}
     * auquel appartiendra ce thème.
     *
     * @param  name   Nom du thème. Ce nom ne doit pas être nul.
     * @param  colors Couleurs qui apparaîtront dans le gradient de couleur du thème. Un tableau
     *                d'un seul élément signifie qu'une couleur uniforme doit être employée pour
     *                tout le thème. Un argument <code>null</code> ou un tableau vide signifie
     *                qu'un gradient par défaut (allant du noir au blanc opaque) sera utilisé.
     *                Il n'est pas obligatoire que la longueur de ce tableau soit égale à
     *                <code>upper-lower</code>. Il sera automatiquement interpolé si nécessaire.
     * @param  lower  Valeur la plus basse des pixels appartenant à ce thème.
     * @param  upper  Valeur la plus haute des pixels appartenant à ce thème.
     * @param  C0     Coefficient <code>C0</code> de l'équation <code>y=C0+C1*x</code>.
     * @param  C1     Coefficient <code>C1</code> de l'équation <code>y=C0+C1*x</code>.
     * @throws IllegalArgumentException si <code>lower</code> est supérieur à <code>upper</code>.
     */
    public LogarithmicCategory(final String name, final Color[] colors, final int lower, final int upper, final float C0, final float C1) throws IllegalArgumentException
    {super(name, colors, lower, upper, C0, C1);}

    /**
     * Calcule la valeur géophysique associée à l'index spécifié. Cette
     * méthode ne vérifie pas si le pixel <code>pixel</code> se trouve
     * dans la plage <code>[{@link #lower}..{@link #upper}]</code>. On
     * suppose que cette vérification a déjà été faite. Si ce n'est pas
     * le cas, la valeur retournée risque d'être une extrapolation qui
     * n'aura pas nécessairement de sens physique.
     */
    protected double toValue(final int index)
    {return XMath.pow10(offset + scale*index);}

    /**
     * Calcule l'index associé à la valeur géophysique spécifiée. le résultat sera
     * ramené dans la plage <code>[{@link #lower}..{@link #upper}]</code> si c'est
     * nécessaire. Si la valeur spécifiée est {@link Float#NaN}, alors par convention
     * cette méthode retourne {@link #lower}. Cette méthode ne retourne jamais un
     * nombre infini ou NaN.
     */
    protected int toIndex(final double value)
    {
        final double index = Math.rint((XMath.log10(value)-offset)/scale);
        return (index>=lower) ? ((index<upper) ? (int)index : upper-1) : lower;
    }
}
