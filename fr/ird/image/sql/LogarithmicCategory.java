/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.image.sql;

// Divers
import java.awt.Color;
import net.seas.util.XMath;
import net.seas.opengis.cv.Category;


/**
 * Repr�sentation d'un th�me par une plage d'index. Un th�me est une plage de valeurs de pixels qui
 * repr�sentent une structure g�omorphologique ou un param�tre g�ophysique.   Par exemple certaines
 * valeurs de pixels pourraient repr�senter des nuages,  tandis que d'autres pourraient repr�senter
 * des valeurs de temp�ratures de la surface de la mer. Chaque th�me doit avoir un nom parlant pour
 * l'utilisateur.  Il peut aussi d�finir une �quation de la forme <code>y=log10(C0+C1*x)</code> qui
 * convertit des valeurs de pixels <var>x</var> en valeurs g�ophysiques <var>y</var>.
 * <br><br>
 * Par design, les objets <code>Category</code> sont immutables (s'ils ne l'�taient pas, �a
 * introduirait de difficiles probl�mes de synchronisation avec {@link IndexedThemeMapper}).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class LogarithmicCategory extends Category
{
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 6537248278156251909L;

    /**
     * Construit un th�me qui repr�sentera un param�tre g�ophysique quantifiable. Les valeurs des
     * pixels de ce th�me iront de <code>lower</code> jusqu'� <code>upper</code> <u>inclusivement</u>
     * (contrairement � la convention des tableaux en C/C++ et en Java), ce que l'on peut noter
     * par <code>[lower..upper]</code>.
     * Les pixels seront convertits en valeurs g�ophysiques � l'aide d'une �quation de la forme
     * <code>y=10^(C0+C1*x)</code>. Les unit�s seront ceux de l'objet {@link IndexedThemeMapper}
     * auquel appartiendra ce th�me.
     *
     * @param  name   Nom du th�me. Ce nom ne doit pas �tre nul.
     * @param  colors Couleurs qui appara�tront dans le gradient de couleur du th�me. Un tableau
     *                d'un seul �l�ment signifie qu'une couleur uniforme doit �tre employ�e pour
     *                tout le th�me. Un argument <code>null</code> ou un tableau vide signifie
     *                qu'un gradient par d�faut (allant du noir au blanc opaque) sera utilis�.
     *                Il n'est pas obligatoire que la longueur de ce tableau soit �gale �
     *                <code>upper-lower</code>. Il sera automatiquement interpol� si n�cessaire.
     * @param  lower  Valeur la plus basse des pixels appartenant � ce th�me.
     * @param  upper  Valeur la plus haute des pixels appartenant � ce th�me.
     * @param  C0     Coefficient <code>C0</code> de l'�quation <code>y=C0+C1*x</code>.
     * @param  C1     Coefficient <code>C1</code> de l'�quation <code>y=C0+C1*x</code>.
     * @throws IllegalArgumentException si <code>lower</code> est sup�rieur � <code>upper</code>.
     */
    public LogarithmicCategory(final String name, final Color[] colors, final int lower, final int upper, final float C0, final float C1) throws IllegalArgumentException
    {super(name, colors, lower, upper, C0, C1);}

    /**
     * Calcule la valeur g�ophysique associ�e � l'index sp�cifi�. Cette
     * m�thode ne v�rifie pas si le pixel <code>pixel</code> se trouve
     * dans la plage <code>[{@link #lower}..{@link #upper}]</code>. On
     * suppose que cette v�rification a d�j� �t� faite. Si ce n'est pas
     * le cas, la valeur retourn�e risque d'�tre une extrapolation qui
     * n'aura pas n�cessairement de sens physique.
     */
    protected double toValue(final int index)
    {return XMath.pow10(offset + scale*index);}

    /**
     * Calcule l'index associ� � la valeur g�ophysique sp�cifi�e. le r�sultat sera
     * ramen� dans la plage <code>[{@link #lower}..{@link #upper}]</code> si c'est
     * n�cessaire. Si la valeur sp�cifi�e est {@link Float#NaN}, alors par convention
     * cette m�thode retourne {@link #lower}. Cette m�thode ne retourne jamais un
     * nombre infini ou NaN.
     */
    protected int toIndex(final double value)
    {
        final double index = Math.rint((XMath.log10(value)-offset)/scale);
        return (index>=lower) ? ((index<upper) ? (int)index : upper-1) : lower;
    }
}
