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
package net.seas.opengis.pt;

// Miscellaneous
import java.util.Arrays;
import java.io.Serializable;
import java.awt.geom.Point2D;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.ParseException;
import java.text.Format;

import net.seas.util.XClass;
import net.seas.text.AngleFormat;
import net.seas.text.CoordinateFormat;
import net.seas.resources.Resources;


/**
 * A position defined by a list of numbers. The ordinate values are
 * indexed from <code>0<code> to <code>(numDim-1)</code>, where
 * <code>numDim</code> is the dimension of the coordinate system
 * the coordinate point belongs in.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.pt.PT_CoordinatePoint
 */
public final class CoordinatePoint implements Cloneable, Serializable
{
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -6497488719261361913L;

    /**
     * A shared instance of {@link CoordinateFormat}. The referenced
     * type should be {@link Format} in order to avoid class loading
     * before necessary.
     */
    private static Reference format;

    /**
     * The ordinates of the coordinate point.
     */
    public final double[] ord;

    /**
     * Construct a coordinate with the
     * specified number of dimensions.
     *
     * @param  numDim Number of dimensions.
     * @throws NegativeArraySizeException if <code>numDim</code> is negative.
     */
    public CoordinatePoint(final int numDim) throws NegativeArraySizeException
    {ord = new double[numDim];}

    /**
     * Construct a coordinate with the specified ordinates.
     * The <code>ord</code> will be copied.
     */
    public CoordinatePoint(final double[] ord)
    {this.ord = (double[]) ord.clone();}

    /**
     * Construct a 2D coordinate from
     * the specified ordinates.
     */
    public CoordinatePoint(final double x, final double y)
    {ord = new double[] {x,y};}

    /**
     * Construct a 3D coordinate from
     * the specified ordinates.
     */
    public CoordinatePoint(final double x, final double y, final double z)
    {ord = new double[] {x,y,z};}

    /**
     * Construct a coordinate from
     * the specified {@link Point2D}.
     */
    public CoordinatePoint(final Point2D point)
    {this(point.getX(), point.getY());}

    /**
     * Construit une coordonnée qui représente la position spécifiée. Les arguments <var>x</var> et <var>y</var>
     * représentent normalement la longitude et la latitude respectivement. Toutefois, si au moins un de ces deux
     * arguments est de la classe {@link Latitude} ou {@link Longitude}, alors ce constructeur utilisera cette
     * information suplémentaire pour vérifier l'ordre des arguments <var>x</var> et <var>y</var> et, au besoin
     * les inverser.
     *
     * @param  x Longitude (de préférence).
     * @param  y Latitude  (de préférence).
     * @throws IllegalArgumentException Si les arguments sont tout deux des latitudes ou des longitudes.
     */
    public CoordinatePoint(final Angle x, final Angle y) throws IllegalArgumentException
    {this(x,y,null);}

    /**
     * Construit une coordonnée qui représente la position spécifiée. Les arguments <var>x</var> et <var>y</var>
     * représentent normalement la longitude et la latitude respectivement. Toutefois, si au moins un de ces deux
     * arguments est de la classe {@link Latitude} ou {@link Longitude}, alors ce constructeur utilisera cette
     * information suplémentaire pour vérifier l'ordre des arguments <var>x</var> et <var>y</var> et, au besoin
     * les inverser.
     *
     * @param  x Longitude (de préférence).
     * @param  y Latitude (de préférence).
     * @param  z Altitude (habituellement en mètres), ou <code>null</code> si elle n'est pas connue.
     * @throws IllegalArgumentException Si les deux premiers arguments sont tout deux des latitudes ou des longitudes.
     */
    public CoordinatePoint(Angle x, Angle y, final Number z) throws IllegalArgumentException
    {
        final boolean normal  = (x instanceof Longitude) || (y instanceof Latitude );
        final boolean inverse = (x instanceof Latitude ) || (y instanceof Longitude);
        if (normal && inverse)
        {
            throw new IllegalArgumentException(Resources.format(Clé.NON_ORTHOGONAL_ANGLES¤1, new Integer((x instanceof Longitude) ? 0 : 1)));
        }
        if (inverse)
        {
            final Angle t=x;
            x=y; y=t;
        }
        if (z!=null)
        {
            ord = new double[]
            {
                x.degrees(),
                y.degrees(),
                z.doubleValue()
            };
        }
        else
        {
            ord = new double[]
            {
                x.degrees(),
                y.degrees()
            };
        }
    }

    /**
     * Constructs a newly allocated <code>CoordinatePoint</code> object that
     * represents the geographic coordinate represented by the string. The
     * string should contains longitude and latitude in either fractional
     * degrees (e.g. 45.5°) or degrees with minutes and seconds (e.g. 45°30').
     * Hemispheres (N, S, E, W) are optional.
     *
     * @param  string A string to be converted to an <code>CoordinatePoint</code>.
     * @throws NumberFormatException if the string does not contain a parsable coordinate.
     */
    public CoordinatePoint(final String string) throws NumberFormatException
    {
        try
        {
            final CoordinatePoint coord=((CoordinateFormat)getFormat()).parse(string);
            this.ord = coord.ord;
        }
        catch (ParseException exception)
        {
            NumberFormatException e=new NumberFormatException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * The number of ordinates of a <code>CoordinatePoint</code>.
     * This is equivalent to <code>{@link #ord ord}.length</code>.
     */
    public final int getDimension()
    {return ord.length;}

    /**
     * Return the first ordinate (usually longitude or <var>x</var>).
     * If this coordinate point doesn't have enough dimension, returns
     * {@link Double#NaN}.
     */
    public final double getX()
    {return (ord.length>0) ? ord[0] : Double.NaN;}

    /**
     * Return the second ordinate (usually latitude or <var>y</var>).
     * If this coordinate point doesn't have enough dimension, returns
     * {@link Double#NaN}.
     */
    public final double getY()
    {return (ord.length>1) ? ord[1] : Double.NaN;}

    /**
     * Return the third ordinate (usually altitude or <var>z</var>).
     * If this coordinate point doesn't have enough dimension, returns
     * {@link Double#NaN}.
     */
    public final double getZ()
    {return (ord.length>2) ? ord[2] : Double.NaN;}

    /**
     * Returns a hash value for this coordinate.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        long code=0;
        if (ord!=null)
        {
            for (int i=ord.length; --i>=0;)
                code = (code << 1) ^ Double.doubleToLongBits(ord[i]);
        }
        return (int)(code >>> 32) ^ (int)code;
    }

    /**
     * Compares the specified object with
     * this coordinate for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof CoordinatePoint)
        {
            final CoordinatePoint that = (CoordinatePoint) object;
            return Arrays.equals(this.ord, that.ord);
        }
        return false;
    }

    /**
     * Returns a deep copy of this coordinate.
     */
    public CoordinatePoint clone()
    {return new CoordinatePoint((double[]) ord.clone());}

    /**
     * Returns a string representation of this coordinate.
     */
    public String toString()
    {return getFormat().format(this);}

    /**
     * Returns a shared instance of {@link CoordinateFormat}.
     * The return type is {@link Format} in order to avoid
     * class loading before necessary.
     */
    private static synchronized Format getFormat()
    {
        if (format!=null)
        {
            final Format coordFormat = (Format) format.get();
            if (coordFormat!=null) return coordFormat;
        }
        final Format newFormat = new CoordinateFormat((AngleFormat)Angle.getFormat());
        format = new SoftReference(newFormat);
        return newFormat;
    }
}
