/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.map.array;

// Divers
import java.awt.geom.Point2D;
import net.seas.resources.Resources;


/**
 * Impl�mentation par d�faut de {@link PointArray}. Cette classe enveloppe
 * un tableau de <code>float[]</code> sans utiliser quelle que compression
 * que ce soit. L'impl�mentation par d�faut est imutable. Toutefois, certaines
 * classes d�riv�es (notamment {@link DynamicArray}) ne le seront pas forc�ment.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class DefaultArray extends PointArray
{
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = -3807313945071488165L;

    /**
     * Tableaux des coordonn�es � envelopper. Ces coordonn�es
     * sont normalement m�moris�es sous forme de paires (x,y).
     */
    protected float[] array;

    /**
     * Enveloppe le tableau <code>float[]</code> sp�cifi�. Ce tableau
     * ne sera pas copi�. Il devra obligatoirement avoir une longueur
     * paire.
     */
    public DefaultArray(final float[] array)
    {
        this.array=array;
        if ((array.length & 1)!=0)
            throw new IllegalArgumentException(Resources.format(Cl�.ODD_ARRAY_LENGTH�1, new Integer(array.length)));
    }

    /**
     * Retourne l'index de la
     * premi�re coordonn�e valide.
     */
    protected int lower()
    {return 0;}

    /**
     * Retourne l'index suivant celui
     * de la derni�re coordonn�e valide.
     */
    protected int upper()
    {return array.length;}

    /**
     * Retourne le nombre de points dans ce tableau.
     */
    public final int count()
    {return (upper()-lower())/2;}

    /**
     * M�morise dans l'objet sp�cifi�
     * les coordonn�es du premier point.
     *
     * @param  point Point dans lequel m�moriser la coordonn�e.
     * @return L'argument <code>point</code>, ou un nouveau point
     *         si <code>point</code> �tait nul.
     */
    public final Point2D getFirstPoint(final Point2D point)
    {
        final int lower=lower();
        assert(lower <= upper());
        final float x = array[lower+0];
        final float y = array[lower+1];
        if (point!=null)
        {
            point.setLocation(x,y);
            return point;
        }
        else return new Point2D.Float(x,y);
    }

    /**
     * M�morise dans l'objet sp�cifi�
     * les coordonn�es du dernier point.
     *
     * @param  point Point dans lequel m�moriser la coordonn�e.
     * @return L'argument <code>point</code>, ou un nouveau point
     *         si <code>point</code> �tait nul.
     */
    public final Point2D getLastPoint(final Point2D point)
    {
        final int upper=upper();
        assert(upper >= lower());
        final float x = array[upper-2];
        final float y = array[upper-1];
        if (point!=null)
        {
            point.setLocation(x,y);
            return point;
        }
        else return new Point2D.Float(x,y);
    }

    /**
     * Retourne un it�rateur qui balaiera les
     * points partir de l'index sp�cifi�.
     */
    public final PointIterator iterator(final int index)
    {return new DefaultIterator(array, (2*index)+lower(), upper());}

    /**
     * Retourne un tableau enveloppant les m�mes points que le tableau courant,
     * mais des index <code>lower</code> inclusivement jusqu'� <code>upper</code>
     * exclusivement. Si le sous-tableau ne contient aucun point (c'est-�-dire si
     * <code>lower==upper</code>), alors cette m�thode retourne <code>null</code>.
     *
     * @param lower Index du premier point � prendre en compte.
     * @param upper Index suivant celui du dernier point � prendre en compte.
     */
    public PointArray subarray(int lower, int upper)
    {
        final int thisLower=lower();
        final int thisUpper=upper();
        lower = lower*2 + thisLower;
        upper = upper*2 + thisLower;
        if (lower            == upper              ) return null;
        if (lower==thisLower && upper==thisUpper   ) return this;
        if (lower==0         && upper==array.length) return new DefaultArray(array);
        return new SubArray(array, lower, upper);
    }

    /**
     * Ins�re les donn�es de <code>this</code> dans le tableau sp�cifi�. Cette m�thode est
     * strictement r�serv�e � l'impl�mentation de {@link #insertAt(int,PointArray,boolean)}.
     * La classe {@link DefaultArray} remplace l'impl�mentation par d�faut par une nouvelle
     * impl�mentation qui �vite de copier les donn�es avec {@link #toArray()}.
     */
    PointArray insertTo(final PointArray dest, final int index, final boolean reverse)
    {return dest.insertAt(index, array, lower(), upper(), reverse);}

    /**
     * Ins�re les donn�es (<var>x</var>,<var>y</var>) du tableau <code>toMerge</code> sp�cifi�.
     * Si le drapeau <code>reverse</code> � la valeur <code>true</code>, alors les points de
     * <code>toMerge</code> seront copi�es en ordre inverse.
     *
     * @param  index Index � partir d'o� ins�rer les points dans ce tableau. Le point � cet
     *         index ainsi que tous ceux qui le suivent seront d�cal�s vers des index plus �lev�s.
     * @param  toMerge Tableau de coordonn�es (<var>x</var>,<var>y</var>) � ins�rer dans ce
     *         tableau de points. Ses valeurs seront copi�es.
     * @param  lower Index de la premi�re coordonn�e de <code>toMerge</code> � copier dans ce tableau.
     * @param  upper Index suivant celui de la derni�re coordonn�e de <code>toMerge</code> � copier.
     * @param  reverse <code>true</code> s'il faut inverser l'ordre des points de <code>toMerge</code>
     *         lors de la copie. Cette inversion ne change pas l'ordre (<var>x</var>,<var>y</var>) des
     *         coordonn�es de chaque points.
     *
     * @return <code>this</code> si l'insertion � pu �tre faite sur
     *         place, ou un autre tableau si �a n'a pas �t� possible.
     */
    public PointArray insertAt(final int index, final float toMerge[], final int lower, final int upper, final boolean reverse)
    {
        int count = upper-lower;
        if (count==0) return this;
        return new DynamicArray(array, lower(), upper(), count+Math.min(count, 256)).insertAt(index, toMerge, lower, upper, reverse);
    }

    /**
     * Renverse l'ordre de tous les points compris dans ce tableau.
     *
     * @return <code>this</code> si l'invertion � pu �tre faite sur-place,
     *         ou un autre tableau si �a n'a pas �t� possible.
     */
    public PointArray reverse()
    {return new DynamicArray(array, lower(), upper(), 16).reverse();}

    /**
     * Retourne un tableau immutable qui contient les m�mes donn�es que celui-ci.
     * Apr�s l'appel de cette m�thode, toute tentative de modification (avec les
     * m�thodes {@link #insertAt} ou {@link #reverse}) vont retourner un autre
     * tableau de fa�on � ne pas modifier le tableau immutable.
     *
     * @param  compress <code>true</code> si l'on souhaite aussi comprimer les
     *         donn�es. Cette compression peut se traduire par une plus grande
     *         lenteur lors des acc�s aux donn�es.
     * @return Tableau immutable et �ventuellement compress�, <code>this</code>
     *         si ce tableau r�pondait d�j� aux conditions ou <code>null</code>
     *         si ce tableau ne contient aucune donn�e.
     */
    public PointArray getFinal(final boolean compress)
    {
        if (compress && count()>=8)
        {
            return new CompressedArray(array, lower(), upper());
        }
        return super.getFinal(compress);
    }

    /**
     * Retourne une copie des donn�es de ce tableau. Toutes les donn�es seront copi�es
     * dans le tableau <code>copy</code> � partir de l'index <code>offset</code>.   Si
     * l'argument <code>n</code> est sup�rieur � 1, alors une d�cimation sera fa�te en
     * ne retenant qu'un point sur <code>n</code>. Le tableau <code>copy</code> doit
     * avoir une longueur d'au moins <code>offset + ceil({@link #length}/n)</code>.
     *
     * @param  copy Tableau dans lequel copier les coordonn�es. Si cet argument est nul,
     *         alors cette m�thode se contentera de calculer la longueur minimale que
     *         devrait avoir le tableau <code>copy</code>.
     * @param  offset Index du premier �l�ment de <code>copy</code>
     *         dans lequel copier la premi�re coordonn�e <var>x</var>.
     * @param  n D�cimation � effectuer (1 pour n'en effectuer aucune).
     * @return Index suivant celui de la derni�re coordonn�e <var>y</var>
     *         copi�e dans le tableau <code>copy</code>.
     */
    public final int toArray(final float[] copy, int offset, int n)
    {
        if (n<1)
        {
            throw new IllegalArgumentException(String.valueOf(n));
        }
        if (copy==null)
        {
            int count = count();
            count = (count+(n-1)) / n;
            return offset + 2*count;
        }
        if (n==1)
        {
            final int lower  = lower();
            final int length = upper()-lower;
            System.arraycopy(array, lower, copy, offset, length);
            return offset+length;
        }
        else
        {
            n *= 2;
            final int upper=upper();
            for (int i=lower(); i<upper; i+=n)
            {
                System.arraycopy(array, i, copy, offset, 2);
                offset += 2;
            }
            return offset;
        }
    }
}