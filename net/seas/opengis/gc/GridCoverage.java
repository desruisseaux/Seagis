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
package net.seas.opengis.gc;

// Coverage
import net.seas.opengis.cv.Coverage;

// Miscellaneous
import javax.media.jai.Histogram;


/**
 * Basic access to grid data values.
 * Each band in an image is represented as a sample dimension.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.gc.GC_GridCoverage
 */
public abstract class GridCoverage extends Coverage
{
    /**
     * Default constructor.
     */
    public GridCoverage()
    {}

    /**
     * Returns <code>true</code> if grid data can be edited.
     * The default implementation returns <code>false</code>.
     */
    public boolean isDataEditable()
    {return false;}

    /**
     * Returns information for the grid coverage geometry. Grid geometry
     * includes the valid range of grid coordinates and the georeferencing.
     */
    public abstract GridGeometry getGridGeometry();

    /**
     * Return a sequence of strongly typed values for a block.
     * A value for each sample dimension will be returned. The return value is an
     * <CODE>N+1</CODE> dimensional array, with dimensions. For 2 dimensional
     * grid coverages, this array will be accessed as (sample dimension, column,
     * row). The index values will be based from 0. The indices in the returned
     * <CODE>N</CODE> dimensional array will need to be offset by grid range
     * minimum coordinates to get equivalent grid coordinates.
     */
//  public abstract DoubleMultiArray getDataBlockAsDouble(final GridRange range)
//  {
        // TODO: Waiting for multiarray package (JSR-083)!
        //       Same for setDataBlock*
//  }

    /**
     * Determine the histogram of the grid values for this coverage.
     *
     * @param  miniumEntryValue Minimum value stored in the first histogram entry.
     * @param  maximumEntryValue Maximum value stored in the last histogram entry.
     * @param  numberEntries Number of entries in the histogram.
     * @return The histogram.
     */
    public abstract Histogram getHistogram(double minimumEntryValue, double maximumEntryValue, int numberEntries);
}
