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
package fr.ird.awt.geom;

// Geometry
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

// Miscellaneous
import java.util.NoSuchElementException;


/**
 * Arrow oriented toward positives x values (0� arithmetic). This shape doesn't have
 * direct support for rotation. To rotate the arrow toward an other direction, use
 * {@link java.awt.geom.AffineTransform}.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/Arrow2D.png"></p>
 * <p align="center">The <code>Arrow2D</code> shape</p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Arrow2D extends RectangularShape
{
    /**
     * Coordonn�es <var>x</var> et <var>y</var> minimales.
     */
    private double x,y;

    /**
     * Longueur de la fl�che. Cette longueur est mesur�e horizontalement
     * (selon l'axe des <var>x</var>) de la base jusqu'� la pointe de la
     * fl�che.
     */
    private double length;

    /**
     * Largeur de la fl�che. Cette largeur est mesur�e verticalement
     * (selon l'axe des <var>y</var>) le long de la partie la plus
     * large de cette fl�che.
     */
    private double height;

    /**
     * Largeur et hauteur de la queue de la fl�che, en proportion avec les dimensions
     * totales de cette fl�che. Ces facteurs doivent �tre compris entre 0 et 1. Les
     * valeurs par d�faut sont de 1/3 selon <var>y</var> et 2/3 selon <var>x</var>,
     * ce qui signifie que la queue de la fl�che aura le tiers de la largeur totale
     * disponible et les deux tiers de la longueur disponible. La pointe de la fl�che
     * aura le reste. Ces proportions donne un assez bon r�sultat lorsque la fl�che
     * est deux fois plus longue que large.
     */
    private double sy0=0, sy1=(1./3), sx=(2./3);

    /**
     * Construit une fl�che donc la surface initiale est nulle.
     */
    public Arrow2D()
    {}

    /**
     * Construit une fl�che situ�e aux coordonn�es (<var>x</var>,<var>y</var>) avec
     * une longueur selon <var>x</var> de <code>length</code> et une hauteur selon
     * <var>y</var> de <code>height</code>.
     *
     * @param x Coordonn�e <var>x</var> minimale.
     * @param y Coordonn�e <var>y</var> minimale.
     * @param length Longueur selon l'axe des <var>x</var>, de la base � la pointe de la fl�che.
     * @param height Largeur maximale de la fl�che, mesur�e selon l'axe des <var>y</var>.
     */
    public Arrow2D(final double x, final double y, final double length, final double height)
    {
        this.x=x;
        this.y=y;
        this.length=length;
        this.height=height;
    }

    /**
     * Modifie la largeur et hauteur de la queue de la fl�che, en proportion avec les dimensions
     * totales de cette fl�che. Ces facteurs doivent �tre compris entre 0 et 1. Les valeurs par
     * d�faut sont de 1/3 selon <var>y</var> et 2/3 selon <var>x</var>, ce qui signifie que la
     * queue de la fl�che aura le tiers de la largeur totale disponible et les deux tiers de la
     * longueur disponible. La pointe de la fl�che aura le reste. Ces proportions donnent d'assez
     * bons r�sultats lorsque la fl�che est deux fois plus longue que large.
     *
     * @param sx  Proportion de la longueur occup�e par la queue de la fl�che, entre 0 et 1.
     * @param sy1 Proportion de la largeur occup�e par la queue de la fl�che pr�s de la base, entre 0 et 1.
     * @param sy0 Proportion de la largeur occup�e par le bout de la queue de la fl�che, entre 0 et 1.
     */
    public void setTailProportion(double sx, double sy1, double sy0)
    {
        if (sy1<0) sy1=0;
        if (sy1>1) sy1=1;
        if (sy0<0) sy0=0;
        if (sy0>1) sy0=1;
        if (sx <0) sx =0;
        if (sx >1) sx =1;
        this.sy1=sy1;
        this.sy0=sy0;
        this.sx =sx;
    }

    /**
     * Renvoie la longueur de la queue de la fl�che,
     * @return La longueur de la queue, compris de 0 � <code>getWidth</code>.
     */
    public double getTailLength()
    {return sx*length;}

    /**
     * Retourne la coordonn�e <var>x</var>. Il s'agira du <var>x</var>
     * le plus bas de la superficie couverte par la fl�che.
     * @return La coordonn�e <var>x</var> minimale.
     */
    public double getX()
    {return x;}

    /**
     * Retourne la coordonn�e <var>y</var>. Il s'agira du <var>y</var>
     * le plus bas de la superficie couverte par la fl�che.
     * @return La coordonn�e <var>y</var> minimale.
     */
    public double getY()
    {return y;}

    /**
     * Retourne la longueur de la fl�che, de la base jusqu'� sa pointe.
     * Cette longueur est mesur�e selon l'axe des <var>x</var>.
     */
    public double getWidth()
    {return length;}

    /**
     * Retourne la largeur de la partie la plus large de la fl�che.
     * Cette largeur est mesur�e selon l'axe des <var>y</var>.
     */
    public double getHeight()
    {return height;}

    /**
     * Renvoie la largeur de la fl�che � la position <var>x</var>. Si cette position
     * n'est pas comprise de <code>getLeft()</code> � <code>getRight</code>, cette
     * m�thode retourne 0. Sinon elle retourne la largeur de la fl�che � la position
     * sp�cifi�e.
     *
     * @param x Coordonn�e <var>x</var> � laquelle on veut la largeur.
     * @return La largeur de la fl�che, comprise entre 0 et <code>getHeight</code>.
     */
    public double getHeight(double x)
    {
        x = (x-this.x)/(sx*length);
        if (x<0 || x>1)
            return 0;
        else if (x <= 1)
            return (sy0+(sy1-sy0)*x)*height;
        else
            return (x-1)*sx/(1-sx)*height;
    }

    /**
     * D�termine si la superficie de cette fl�che est nulle.
     * @return <code>true</code> si la superficie de cette fl�che est nulle.
     */
    public boolean isEmpty()
    {return !(length>0 && height>0);}

    /**
     * Affecte une nouvelle position et une nouvelle largeur � la fl�che.
     * @param x Coordonn�ex <var>x</var> minimale.
     * @param y Coordonn�ex <var>y</var> minimale.
     * @param length Longueur, de la base � la pointe de la fl�che.
     * @param height Largeur de la partie la plus large de la fl�che.
     */
    public void setFrame(final double x, final double y, final double length, final double height)
    {
        this.x=x;
        this.y=y;
        this.length=length;
        this.height=height;
    }

    /**
     * Renvoie les dimensions de cette fl�che.
     * @return Les dimensions de cette fl�che.
     */
    public Rectangle2D getBounds2D()
    {return new Rectangle2D.Double(x, y, length, height);}

    /**
     * Indique si cette fl�che contient le point sp�cifi�.
     * @param x Coordonn�e <var>x</var> du point � v�rifier.
     * @param y Coordonn�e <var>y</var> du point � v�rifier.
     */
    public boolean contains(final double x, double y)
    {
        if (x<this.x) return false;
        final double base=this.x+sx*length;
        if (x<=base)
        {
            /*
             * Point dans la queue. V�rifie s'il
             * se trouve dans le triangle...
             */
            double halfheight=0.5*height;
            y -= (this.y+halfheight);
            halfheight *= sy0+(sy1-sy0)*((x-this.x)/(base-this.x));
            return (Math.abs(y) <= halfheight);
        }
        else
        {
            /*
             * Point dans la pointe. V�rifie s'il
             * se trouve dans le triangle.
             */
            final double pointe=this.x+length;
            if (x>pointe) return false;
            double halfheight = 0.5*height;
            y -= (this.y+halfheight);
            halfheight *= (pointe-x)/(pointe-base);
            return (Math.abs(y) <= halfheight);
        }
    }

    /**
     * Indique si la fl�che contient enti�rement le rectangle sp�cifi�. Ce sera
     * le cas si la fl�che contient chacun des quatre coins du rectangle.
     * @param x Coordonn�ex <var>x</var> minimale du rectangle.
     * @param y Coordonn�ex <var>y</var> minimale du rectangle.
     * @param width Largeur du rectangle.
     * @param height Hauteur du rectangle.
     * @return <code>true</code> si la fl�che contient le rectangle.
     */
    public boolean contains(final double x, final double y, final double width, final double height)
    {
        return  contains(x      , y       ) &&
                contains(x+width, y       ) &&
                contains(x+width, y+height) &&
                contains(x      , y+height);
    }

    /**
     * Indique si la fl�che intersepte le rectangle sp�cifi�.
     * @param x Coordonn�ex <var>x</var> minimale du rectangle.
     * @param y Coordonn�ex <var>y</var> minimale du rectangle.
     * @param width Largeur du rectangle.
     * @param height Hauteur du rectangle.
     * @return <code>true</code> si la fl�che intersepte le rectangle.
     */
    public boolean intersects(final double x, double y, final double width, final double height)
    {
        final double right=x+width;
        final double thisPointe=this.x+this.length;
        if (x<=thisPointe && right>=this.x)
        {
            y -= 0.5*this.height;
            final double thisBase=this.x+this.length*sx;
            final double top=y+height;
            if (right>=thisBase)
            {
                /*
                 * Rectangle intercepte la pointe de la fl�che?
                 */
                double halfheight=0.5*this.height;
                halfheight *= (thisPointe-right)/(thisPointe-thisBase);
                if (top>=this.y-halfheight && y<=this.y+halfheight) return true;
            }
            if (x<=thisBase)
            {
                /*
                 * Rectangle intercepte la queue de la fl�che?
                 */
                double halfheight=0.5*this.height;
                halfheight *= sy0+(sy1-sy0)*((x-this.x)/(thisBase-this.x));
                if (top>=this.y-halfheight && y<=this.y+halfheight) return true;
            }
        }
        return false;
    }

    /**
     * Retourne un it�rateur permettant de balayer les segments
     * formant la fl�che. Comme cette fl�che ne contient pas de
     * portion arrondie, cette m�thode retourne la m�me chose
     * que l'autre m�thode <code>getPathIterator</code>.
     *
     * @param at Une transformation affine facultative.
     * @param flatness Ignor�.
     */
    public PathIterator getPathIterator(final AffineTransform at, final double flatness)
    {return new Iterator(at);}

    /**
     * Retourne un it�rateur permettant de balayer les segments formant la fl�che.
     * @param at Une transformation affine facultative.
     */
    public PathIterator getPathIterator(final AffineTransform at)
    {return new Iterator(at);}

    /**
     * It�rateur balayant les coordonn�es de la fl�che.
     */
    private class Iterator implements PathIterator
    {
        /**
         * Transformation affine � utiliser pour
         * transformer les coordonn�es. Peut �tre nul.
         */
        private final AffineTransform at;

        /**
         * Quelques variables calcul�es lors de la construction
         * afin d'�viter d'avoir � les recalculer trop souvent.
         */
        private final double halfBottom0, halfBottom1, center, halfTop1, halfTop0, base;

        /**
         * Code indiquant quel sera la prochaine donn�e � retourner pour cette fl�che.
         */
        private int code;

        /**
         * Construit un it�rateur balayant les coordonn�es de la fl�che.
         * @param at Transformation affine � utiliser pour transformer les coordonn�es. Peut �tre nul.
         */
        Iterator(final AffineTransform at)
        {
            this.at=at;
            final double halfheight=0.5*height;
            halfBottom0=y+halfheight*(1-sy0);
            halfBottom1=y+halfheight*(1-sy1);
            center=y+halfheight;
            halfTop1=y+halfheight*(1+sy1);
            halfTop0=y+halfheight*(1+sy0);
            base=x+sx*length;
        }

        /**
         * Retourne la r�gle utilis� pour remplir le polygone.
         * @return Toujours <code>EVEN_ODD</code>.
         */
        public int getWindingRule()
        {return WIND_EVEN_ODD;}

        /**
         * Passe au point suivant.
         */
        public void next()
        {code++;}

        /**
         * Renvoie les coordonn�es du segment actuel.
         * @param Tableau dans lequel m�moriser les coordonn�es.
         * @return Le code des coordonn�es m�moris�es.
         */
        public int currentSegment(final float[] coords)
        {
            switch (code)
            {
                case 0: coords[0]=(float) x;          coords[1]=(float) halfBottom0; break;
                case 1: coords[0]=(float) base;       coords[1]=(float) halfBottom1; break;
                case 2: coords[0]=(float) base;       coords[1]=(float) y;           break;
                case 3: coords[0]=(float) (x+length); coords[1]=(float) center;      break;
                case 4: coords[0]=(float) base;       coords[1]=(float) (y+height);  break;
                case 5: coords[0]=(float) base;       coords[1]=(float) halfTop1;    break;
                case 6: coords[0]=(float) x;          coords[1]=(float) halfTop0;    break;
                case 7: coords[0]=(float) x;          coords[1]=(float) halfBottom0; break;
                case 8:  return SEG_CLOSE;
                default: throw new NoSuchElementException();
            }
            if (at!=null) at.transform(coords, 0, coords, 0, 1);
            return (code==0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Renvoie les coordonn�es du segment actuel.
         * @param Tableau dans lequel m�moriser les coordonn�es.
         * @return Le code des coordonn�es m�moris�es.
         */
        public int currentSegment(final double[] coords)
        {
            switch (code)
            {
                case 0: coords[0]=x;        coords[1]=halfBottom0; break;
                case 1: coords[0]=base;     coords[1]=halfBottom1; break;
                case 2: coords[0]=base;     coords[1]=y;           break;
                case 3: coords[0]=x+length; coords[1]=center;      break;
                case 4: coords[0]=base;     coords[1]=y+height;    break;
                case 5: coords[0]=base;     coords[1]=halfTop1;    break;
                case 6: coords[0]=x;        coords[1]=halfTop0;    break;
                case 7: coords[0]=x;        coords[1]=halfBottom0; break;
                case 8:  return SEG_CLOSE;
                default: throw new NoSuchElementException();
            }
            if (at!=null) at.transform(coords, 0, coords, 0, 1);
            return (code==0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Indique si l'on a termin� de balayer les points.
         * @return <code>true</code> si le balayage est termin�.
         */
        public boolean isDone()
        {return code>8;}
    }

    /**
     * Indique si cette fl�che est identique
     * � la fl�che sp�cifi�e.
     */
    public boolean equals(final Object obj)
    {
        if (obj==this) return true;
        if (obj!=null && getClass().equals(obj.getClass()))
        {
            final Arrow2D cast = (Arrow2D) obj;
            return Double.doubleToLongBits(height) == Double.doubleToLongBits(cast.height) &&
                   Double.doubleToLongBits(length) == Double.doubleToLongBits(cast.length) &&
                   Double.doubleToLongBits(  x   ) == Double.doubleToLongBits(cast.x     ) &&
                   Double.doubleToLongBits(  y   ) == Double.doubleToLongBits(cast.y     ) &&
                   Double.doubleToLongBits(  sx  ) == Double.doubleToLongBits(cast.sx    ) &&
                   Double.doubleToLongBits(  sy0 ) == Double.doubleToLongBits(cast.sy1   ) &&
                   Double.doubleToLongBits(  sy1 ) == Double.doubleToLongBits(cast.sy0   );
        }
        else return false;      
    }

    /**
     * Retourne un code "hash value"
     * pour cette fl�che.
     */
    public int hashCode()
    {
        final long code=Double.doubleToLongBits(height) + 37*
                       (Double.doubleToLongBits(length) + 37*
                       (Double.doubleToLongBits(  x   ) + 37*
                       (Double.doubleToLongBits(  y   ) + 37*
                       (Double.doubleToLongBits(  sx  ) + 37*
                       (Double.doubleToLongBits(  sy0 ) + 37*
                       (Double.doubleToLongBits(  sy1 )))))));
        return (int) code + (int) (code >>> 32);
    }
}