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
package fr.ird.map.array;

// Divers
import java.awt.geom.Point2D;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Tableaux de points compress�s. Les objets
 * de cette classe sont immutables.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class CompressedArray extends PointArray
{
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 5354903298883819938L;

    /**
     * Tableaux des coordonn�es <u>relatives</u>. Ces coordonn�es
     * sont normalement m�moris�es sous forme de paires (dx,dy).
     * Chaque paire (dx,dy) repr�sente le d�placement par rapport
     * au point pr�c�dent.
     */
    protected final byte[] array;

    /**
     * Coordonn�es du point qui pr�c�de le premier point.
     * Les coordonn�es du "vrai" premier point seront
     * obtenus par
     *
     * <pre>
     *     x = x0 + array[0]*scaleX;
     *     y = y0 + array[1]*scaleY;
     * </pre>
     */
    protected final float x0, y0;

    /**
     * Constantes servant � transformer lin�airement les
     * valeurs {@link #array} vers des <code>float</code>.
     */
    protected final float scaleX, scaleY;

    /**
     * Construit un sous-tableau � partir
     * d'un autre tableau compress�.
     *
     * @param  other Tableau source.
     * @param  lower Index de la premi�re coordonn�es <var>x</var> �
     *         prendre en compte dans le tableau <code>other</code>.
     */
    protected CompressedArray(final CompressedArray other, final int lower)
    {
        if (lower < other.lower())
            throw new IllegalArgumentException(lower+" < "+other.lower());

        this.scaleX = other.scaleX;
        this.scaleY = other.scaleY;
        this.array  = other.array;

        int dx=0,dy=0;
        for (int i=other.lower(); i<lower;)
        {
            dx += array[i++];
            dy += array[i++];
        }
        this.x0 = other.x0 + scaleX*dx;
        this.y0 = other.y0 + scaleY*dy;
    }

    /**
     * Construit un tableau compress�.
     *
     * @param  coord Tableau de coordonn�es (<var>x</var>,<var>y</var>).
     * @param  lower Index de la premi�re coordonn�es <var>x</var> �
     *         prendre en compte dans le tableau <code>coord</code>.
     * @param  upper Index suivant celui de la derni�re coordonn�e <var>y</var> �
     *         prendre en compte dans le tableau <code>coord</code>. La diff�rence
     *         <code>upper-lower</code> doit obligatoirement �tre paire.
     * @throws ArithmeticException Si la compression a �chou�e �
     *         cause d'une erreur arithm�tique dans l'algorithme.
     */
    public CompressedArray(final float[] coord, final int lower, final int upper) throws ArithmeticException
    {
        checkRange(coord, lower, upper);
        if (upper-lower < 2)
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_RANGE_$2, new Integer(lower), new Integer(upper)));
        }
        /*
         * Calcule les plus grands �carts de longitude (<var>dx</var>)
         * et de latitude (<var>dy</var>) entre deux points.
         */
        float dxMin=Float.POSITIVE_INFINITY;
        float dxMax=Float.NEGATIVE_INFINITY;
        float dyMin=Float.POSITIVE_INFINITY;
        float dyMax=Float.NEGATIVE_INFINITY;
        for (int i=lower+2; i<upper; i++)
        {
            float delta;
            delta=coord[i]-coord[i-2];
            if (delta<dxMin) dxMin=delta;
            if (delta>dxMax) dxMax=delta;
            i++;
            delta=coord[i]-coord[i-2];
            if (delta<dyMin) dyMin=delta;
            if (delta>dyMax) dyMax=delta;
        }
        /*
         * Construit le tableau de coordonn�es compress�es.
         */
        this.x0     = coord[lower+0];
        this.y0     = coord[lower+1];
        this.array  = new byte[upper-lower];

        int reduceXMin = 0;
        int reduceXMax = 0;
        int reduceYMin = 0;
        int reduceYMax = 0;
  init: for (int test=0; test<16; test++)
        {
            final float scaleX = Math.max(dxMax/(Byte.MAX_VALUE-reduceXMax), dxMin/(Byte.MIN_VALUE+reduceXMin));
            final float scaleY = Math.max(dyMax/(Byte.MAX_VALUE-reduceYMax), dyMin/(Byte.MIN_VALUE+reduceYMin));
            int lastx = 0;
            int lasty = 0;
            for (int j=0,i=lower; i<upper;)
            {
                final int  x = Math.round((coord[i++]-x0)/scaleX);
                final int  y = Math.round((coord[i++]-y0)/scaleY);
                final int dx = x-lastx;
                final int dy = y-lasty;
                if (dx<Byte.MIN_VALUE) {reduceXMin++; continue init;}
                if (dx>Byte.MAX_VALUE) {reduceXMax++; continue init;}
                if (dy<Byte.MIN_VALUE) {reduceYMin++; continue init;}
                if (dy>Byte.MAX_VALUE) {reduceYMax++; continue init;}
                array[j++] = (byte) dx;
                array[j++] = (byte) dy;
                lastx = x;
                lasty = y;
            }
            assert(array[0]==0);
            assert(array[1]==0);
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            return;
        }
        throw new ArithmeticException(); // Should not happen
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
        final int lower = lower();
        final float x = x0+scaleX*array[lower+0];
        final float y = y0+scaleY*array[lower+1];
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
        int dx=0;
        int dy=0;
        final int upper=upper();
        for (int i=lower(); i<upper;)
        {
            dx += array[i++];
            dy += array[i++];
        }
        final float x = x0+scaleX*dx;
        final float y = y0+scaleY*dy;
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
    {return new CompressedIterator(this, index);}

    /**
     * Retourne un tableau enveloppant les m�mes points que le tableau courant,
     * mais des index <code>lower</code> inclusivement jusqu'� <code>upper</code>
     * exclusivement. Si le sous-tableau ne contient aucun point (c'est-�-dire si
     * <code>lower==upper</code>), alors cette m�thode retourne <code>null</code>.
     *
     * @param lower Index du premier point � prendre en compte.
     * @param upper Index suivant celui du dernier point � prendre en compte.
     */
    public final PointArray subarray(int lower, int upper)
    {
        final int thisLower=lower();
        final int thisUpper=upper();
        lower = lower*2 + thisLower;
        upper = upper*2 + thisLower;
        if (lower            == upper              ) return null;
        if (lower==thisLower && upper==thisUpper   ) return this;
        return new SubCompressedArray(this, lower, upper);
    }

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
     * @return Un nouveau tableau non-compress�.
     */
    public final PointArray insertAt(final int index, final float toMerge[], final int lower, final int upper, final boolean reverse)
    {
        if (lower==upper) return this;
        return new DynamicArray(this).insertAt(index, toMerge, lower, upper, reverse);
    }

    /**
     * Renverse l'ordre de tous les points compris dans ce tableau.
     *
     * @return Un nouveau tableau non-compress� qui
     *         contiendra les points en ordre inverse.
     */
    public final PointArray reverse()
    {return new DynamicArray(this).reverse();}

    /**
     * Retourne un tableau immutable qui contient les m�mes donn�es que celui-ci.
     * Cette m�thode retourne toujours <code>this</code> puisque ce tableau est
     * d�j� immutable et compress�.
     */
    public final PointArray getFinal(final boolean compress)
    {return this;}

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
    public final int toArray(final float[] copy, int offset, final int n)
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
        final int lower = lower();
        final int upper = upper();
        int dx=0,dy=0;
        if (n==1)
        {
            for (int i=lower; i<upper;)
            {
                dx += array[i++];
                dy += array[i++];
                copy[offset++] = x0 + scaleX*dx;
                copy[offset++] = y0 + scaleY*dy;
            }
        }
        else
        {
            int c=n-1;
            for (int i=lower; i<upper;)
            {
                dx += array[i++];
                dy += array[i++];
                if (++c >= n)
                {
                    c=0;
                    copy[offset++] = x0 + scaleX*dx;
                    copy[offset++] = y0 + scaleY*dy;
                }
            }
        }
        return offset;
    }
}
