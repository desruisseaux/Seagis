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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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
import java.text.ChoiceFormat;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * A box defined by two positions. The two positions must have the
 * same dimension. Each of the ordinate values in the minimum point
 * must be less than or equal to the corresponding ordinate value
 * in the maximum point. Please note that these two points may be
 * outside the valid domain of their coordinate system. (Of course
 * the points and envelope do not explicitly reference a coordinate
 * system, but their implicit coordinate system is defined by their
 * context.)
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.pt.PT_Envelope
 * @see java.awt.geom.Rectangle2D
 */
public final class Envelope implements Dimensioned, Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3698298355829867895L;

    /**
     * Minimum and maximum ordinate values. The first half contains minimum
     * ordinates, while the last half contains maximum ordinates.
     */
    private final double[] ord;

    /**
     * Check if ordinate values in the minimum point are less than or
     * equal to the corresponding ordinate value in the maximum point.
     *
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    private void checkCoherence() throws IllegalArgumentException
    {
        final int dimension = ord.length/2;
        for (int i=0; i<dimension; i++)
            if (!(ord[i] <= ord[dimension+i])) // Use '!' in order to catch 'NaN'.
                throw new IllegalArgumentException(Resources.format(Cl�.BAD_ORDINATE�1, new Integer(i+1)));
    }

    /**
     * Construct a copy of the specified envelope.
     */
    private Envelope(final Envelope envelope)
    {ord = (double[]) envelope.ord.clone();}

    /**
     * Construct an empty envelope of the specified dimension.
     * All ordinates are initialized to 0.
     */
    public Envelope(final int dimension)
    {ord = new double[dimension*2];}

    /**
     * Construct one-dimensional envelope defined by a range of values.
     *
     * @param min The minimal value.
     * @param max The maximal value.
     */
    public Envelope(final double min, final double max)
    {
        ord = new double[] {min, max};
        checkCoherence();
    }

    /**
     * Construct a envelope defined by two positions.
     *
     * @param  minCP Minimum ordinate values.
     * @param  maxCP Maximum ordinate values.
     * @throws MismatchedDimensionException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    public Envelope(final double[] minCP, final double[] maxCP) throws MismatchedDimensionException
    {
        if (minCP.length != maxCP.length)
        {
            throw new MismatchedDimensionException(minCP.length, maxCP.length);
        }
        ord = new double[minCP.length + maxCP.length];
        System.arraycopy(minCP, 0, ord, 0,            minCP.length);
        System.arraycopy(maxCP, 0, ord, minCP.length, maxCP.length);
        checkCoherence();
    }

    /**
     * Construct a envelope defined by two positions.
     *
     * @param  minCP Point containing minimum ordinate values.
     * @param  maxCP Point containing maximum ordinate values.
     * @throws MismatchedDimensionException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    public Envelope(final CoordinatePoint minCP, final CoordinatePoint maxCP) throws MismatchedDimensionException
    {this(minCP.ord, maxCP.ord);}

    /**
     * Construct two-dimensional envelope defined by a {@link Rectangle2D}.
     */
    public Envelope(final Rectangle2D rect)
    {
        ord = new double[]
        {
            rect.getMinX(), rect.getMinY(),
            rect.getMaxX(), rect.getMaxY()
        };
        checkCoherence();
    }

    /**
     * Convenience method for checking the envelope's dimension validity.
     * This method is usually call for argument checking.
     *
     * @param  expectedDimension Expected dimension for this envelope.
     * @throws MismatchedDimensionException if this envelope doesn't have the expected dimension.
     */
    void ensureDimensionMatch(final int expectedDimension) throws MismatchedDimensionException
    {
        final int dimension = getDimension();
        if (dimension != expectedDimension)
        {
            throw new MismatchedDimensionException(dimension, expectedDimension);
        }
    }

    /**
     * Determines whether or not this envelope is empty.
     * An envelope is non-empty only if it has a length
     * greater that 0 along all dimensions.
     */
    public boolean isEmpty()
    {
        final int dimension = ord.length/2;
        for (int i=0; i<dimension; i++)
            if (!(ord[i] < ord[i+dimension])) // Use '!' in order to catch NaN
                return true;
        return false;
    }

    /**
     * Returns the number of dimensions.
     */
    public int getDimension()
    {return ord.length/2;}

    /**
     * Returns the minimal ordinate
     * along the specified dimension.
     */
    public double getMinimum(final int dimension)
    {
        if (dimension<ord.length) return ord[dimension];
        throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * Returns the maximal ordinate
     * along the specified dimension.
     */
    public double getMaximum(final int dimension)
    {
        if (dimension>=0) return ord[dimension+ord.length/2];
        throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * Returns the center ordinate
     * along the specified dimension.
     */
    public double getCenter(final int dimension)
    {return 0.5*(ord[dimension] + ord[dimension+ord.length/2]);}

    /**
     * Returns the envelope length along the specified dimension.
     * This length is equals to the maximum ordinate minus the
     * minimal ordinate.
     */
    public double getLength(final int dimension)
    {return ord[dimension+ord.length/2] - ord[dimension];}

    /*
     * Returns the range of values along a dimension as a {@link Range} object. This method may
     * be overridden by subclasses wanting more flexibility  than what is usually possible with
     * <code>getMinimum</code> and <code>getMaximum</code>. For example, range along a temporal
     * axis may be delimited by {@link java.util.Date} objects instead of {@link Double}.
     */
//  public Range getRange(final int dimension)
//  {return new Range(Double.class, new Double(getMinimum(dimension)), new Double(getMaximum(dimension)));}

    /*
     * Set the envelope's range along the specified dimension. The default
     * implementation expect a range delimited by {@link Number} objects.
     */
//  public void setRange(final int dimension, final Range range)
//  {
//      double minimum = ((Number)range.getMinValue()).doubleValue();
//      double maximum = ((Number)range.getMaxValue()).doubleValue();
//      if (!range.isMinIncluded()) minimum=ChoiceFormat.    nextDouble(minimum);
//      if (!range.isMaxIncluded()) maximum=ChoiceFormat.previousDouble(maximum);
//      setRange(dimension, minimum, maximum);
//  }

    /**
     * Set the envelope's range along the specified dimension.
     *
     * @param dimension The dimension to set.
     * @param minimum   The minimum value along the specified dimension.
     * @param maximum   The maximum value along the specified dimension.
     */
    public void setRange(final int dimension, double minimum, double maximum)
    {
        if (minimum > maximum)
        {
            // Make an empty envelope (min==max)
            // while keeping it legal (min<=max).
            minimum = maximum = 0.5*(minimum+maximum);
        }
        if (dimension>=0)
        {
            // Do not make any change if 'dimension' is out of range.
            ord[dimension+ord.length/2] = maximum;
            ord[dimension             ] = minimum;
        }
        else throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * Adds a point to this envelope. The resulting envelope
     * is the smallest envelope that contains both the original envelope and the
     * specified point. After adding a point, a call to {@link #contains} with the
     * added point as an argument will return <code>true</code>, except if one of
     * the point's ordinates was {@link Double#NaN} (in which case the corresponding
     * ordinate have been ignored).
     *
     * @param  point The point to add.
     * @throws MismatchedDimensionException if the specified point doesn't have
     *         the expected dimension.
     */
    public void add(final CoordinatePoint point) throws MismatchedDimensionException
    {
        final int dim = ord.length/2;
        point.ensureDimensionMatch(dim);
        for (int i=0; i<dim; i++)
        {
            final double value = point.ord[i];
            if (value < ord[i    ]) ord[i    ]=value;
            if (value > ord[i+dim]) ord[i+dim]=value;
        }
    }

    /**
     * Adds an envelope object to this envelope.
     * The resulting envelope is the union of the
     * two <code>Envelope</code> objects.
     *
     * @param  envelope the <code>Envelope</code> to add to this envelope.
     * @throws MismatchedDimensionException if the specified envelope doesn't
     *         have the expected dimension.
     */
    public void add(final Envelope envelope) throws MismatchedDimensionException
    {
        final int dim = ord.length/2;
        envelope.ensureDimensionMatch(dim);
        for (int i=0; i<dim; i++)
        {
            final double min = envelope.ord[i    ];
            final double max = envelope.ord[i+dim];
            if (min < ord[i    ]) ord[i    ]=min;
            if (max > ord[i+dim]) ord[i+dim]=max;
        }
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this envelope.
     *
     * @param  point The point to text.
     * @return <code>true</code> if the specified coordinates are inside the boundary
     *         of this envelope; <code>false</code> otherwise.
     * @throws MismatchedDimensionException if the specified point doesn't have
     *         the expected dimension.
     */
    public boolean contains(final CoordinatePoint point) throws MismatchedDimensionException
    {
        final int dimension = ord.length/2;
        point.ensureDimensionMatch(dimension);
        for (int i=0; i<dimension; i++)
        {
            final double value = point.ord[i];
            if (!(value >= ord[i          ])) return false;
            if (!(value <= ord[i+dimension])) return false;
            // Use '!' in order to take 'NaN' in account.
        }
        return true;
    }

    /**
     * Returns a new envelope representing the intersection of this
     * <code>Envelope</code> with the specified <code>Envelope</code>.
     *
     * @param  envelope The <code>Envelope</code> to intersect with this envelope.
     * @return The largest envelope contained in both the specified <code>Envelope</code>
     *         and in this <code>Envelope</code>.
     * @throws MismatchedDimensionException if the specified envelope doesn't
     *         have the expected dimension.
     */
    public Envelope createIntersection(final Envelope envelope) throws MismatchedDimensionException
    {
        final int dim = ord.length/2;
        envelope.ensureDimensionMatch(dim);
        final Envelope dest = new Envelope(dim);
        for (int i=0; i<dim; i++)
        {
            double min = Math.max(ord[i    ], envelope.ord[i    ]);
            double max = Math.min(ord[i+dim], envelope.ord[i+dim]);
            if (min > max)
            {
                // Make an empty envelope (min==max)
                // while keeping it legal (min<=max).
                min = max = 0.5*(min+max);
            }
            dest.ord[i    ]=min;
            dest.ord[i+dim]=max;
        }
        return dest;
    }

    /**
     * Returns a {@link Rectangle2D} with the same bounds as this <code>Envelope</code>.
     * This is a convenience method for interoperability with Java2D.
     *
     * @throws IllegalStateException if this envelope is not two-dimensional.
     */
    public Rectangle2D toRectangle2D() throws IllegalStateException
    {
        if (ord.length == 4)
        {
            return new Rectangle2D.Double(ord[0], ord[1], ord[2]-ord[0], ord[3]-ord[1]);
        }
        throw new IllegalStateException(Resources.format(Cl�.NOT_TWO_DIMENSIONAL�1, new Integer(getDimension())));
    }

    /**
     * Returns a hash value for this envelope.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {return CoordinatePoint.hashCode(ord);}

    /**
     * Compares the specified object with
     * this envelope for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof Envelope)
        {
            final Envelope that = (Envelope) object;
            return Arrays.equals(this.ord, that.ord);
        }
        return false;
    }

    /**
     * Returns a deep copy of this envelope.
     */
    public Envelope clone()
    {return new Envelope(this);}

    /**
     * Returns a string representation of this envelope.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    public String toString()
    {return CoordinatePoint.toString(this, ord);}
}
