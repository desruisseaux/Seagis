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
package net.seas.opengis.gc;

// Miscellaneous
import java.util.Arrays;
import java.awt.Rectangle;
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;


/**
 * Defines a range of grid coverage coordinates.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.gc.GC_GridRange
 */
public final class GridRange implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 4840837441311884179L;

    /**
     * Minimum and maximum grid ordinates. The first half contains minimum
     * ordinates, while the last half contains maximum ordinates.
     */
    private final int[] index;

    /**
     * Check if ordinate values in the minimum index are less than or
     * equal to the corresponding ordinate value in the maximum index.
     *
     * @throws IllegalArgumentException if an ordinate value in the minimum index is not
     *         less than or equal to the corresponding ordinate value in the maximum index.
     */
    private void checkCoherence() throws IllegalArgumentException
    {
        final int dimension = index.length/2;
        for (int i=0; i<dimension; i++)
            if (!(index[i] <= index[dimension+i]))
                throw new IllegalArgumentException(Resources.format(Cl�.BAD_GRID_RANGE�1, new Integer(i+1)));
    }

    /**
     * Construct one-dimensional grid range.
     *
     * @param min The minimal inclusive value.
     * @param max The maximal exclusive value.
     */
    public GridRange(final int lower, final int upper)
    {
        index = new int[] {lower, upper};
        checkCoherence();
    }

    /**
     * Construct a new grid range.
     *
     * @param lower The valid minimum inclusive grid coordinate.
     *              The array contains a minimum value for each
     *              dimension of the grid coverage. The lowest
     *              valid grid coordinate is zero.
     * @param upper The valid maximum exclusive grid coordinate.
     *              The array contains a maximum value for each
     *              dimension of the grid coverage.
     */
    public GridRange(final int[] lower, final int[] upper)
    {
        if (lower.length != upper.length)
        {
            throw new IllegalArgumentException(Resources.format(Cl�.MISMATCHED_DIMENSION�2,
                                               new Integer(lower.length), new Integer(upper.length)));
        }
        index = new int[lower.length + upper.length];
        System.arraycopy(lower, 0, index, 0,            lower.length);
        System.arraycopy(upper, 0, index, lower.length, upper.length);
        checkCoherence();
    }

    /**
     * Construct two-dimensional range defined by a {@link Rectangle}.
     */
    public GridRange(final Rectangle rect)
    {
        index = new int[]
        {
            rect.x,            rect.y,
            rect.x+rect.width, rect.y+rect.height
        };
        checkCoherence();
    }

    /**
     * Construct two-dimensional range defined by a {@link Raster}.
     */
    public GridRange(final Raster raster)
    {
        final int x = raster.getMinX();
        final int y = raster.getMinY();
        index = new int[]
        {
            x,                   y,
            x+raster.getWidth(), y+raster.getHeight()
        };
        checkCoherence();
    }

    /**
     * Construct two-dimensional range defined by a {@link RenderedImage}.
     */
    public GridRange(final RenderedImage image)
    {
        final int x = image.getMinX();
        final int y = image.getMinY();
        index = new int[]
        {
            x,                  y,
            x+image.getWidth(), y+image.getHeight()
        };
        checkCoherence();
    }

    /**
     * <FONT COLOR="#FF6633">Returns the number of dimensions.</FONT>
     */
    public int getDimension()
    {return index.length/2;}

    /**
     * <FONT COLOR="#FF6633">Returns the valid minimum inclusive grid
     * coordinate along the specified dimension.</FONT>
     */
    public int getLower(final int dimension)
    {
        if (dimension<index.length) return index[dimension];
        throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * <FONT COLOR="#FF6633">Returns the valid maximum exclusive grid
     * coordinate along the specified dimension.</FONT>
     */
    public int getUpper(final int dimension)
    {
        if (dimension>=0) return index[dimension+index.length/2];
        else throw new ArrayIndexOutOfBoundsException(dimension);
    }

    /**
     * Returns a hash value for this grid range.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        int code=45123678;
        if (index!=null)
            for (int i=index.length; --i>=0;)
                code = code*31 + index[i];
        return code;
    }

    /**
     * Compares the specified object with
     * this grid range for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof GridRange)
        {
            final GridRange that = (GridRange) object;
            return Arrays.equals(this.index, that.index);
        }
        else return false;
    }

    /**
     * Returns a string repr�sentation of this grid range.
     * The returned string is implementation dependent. It
     * is usually provided for debugging purposes.
     */
    public String toString()
    {
        final int dimension = index.length/2;
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        for (int i=0; i<dimension; i++)
        {
            if (i!=0) buffer.append(", ");
            buffer.append(index[i]);
            buffer.append("..");
            buffer.append(index[i+dimension]);
        }
        buffer.append(']');
        return buffer.toString();
    }
}