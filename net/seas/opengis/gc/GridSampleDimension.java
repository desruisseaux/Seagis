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

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cv.CategoryList;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.cv.ColorInterpretation;


/**
 * Describes the band values for a grid coverage.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
final class GridSampleDimension extends SampleDimension
{
    /**
     * The grid coverage.
     */
//  private final GridCoverage coverage;

    /**
     * Construct a sample dimension with a set of categories.
     *
     * @param coverage The grid coverage.
     * @param categories The category list for this sample dimension, or
     *        <code>null</code> if this sample dimension has no category.
     */
    public GridSampleDimension(/*final GridCoverage coverage,*/ final CategoryList categories)
    {
        super(categories);
//      this.coverage = coverage;
    }

    /**
     * Returns the color interpretation of the sample dimension.
     * Since {@link CategoryList} are designed for indexed color
     * models, current implementation returns {@link ColorInterpretation#PALETTE_INDEX}.
     * We need to find a more general way in some future version.
     */
    public ColorInterpretation getColorInterpretation()
    {return ColorInterpretation.PALETTE_INDEX;}

    /**
     * Returns the minimum value occurring in this sample dimension.
     */
    public double getMinimumValue()
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Returns the maximum value occurring in this sample dimension.
     */
    public double getMaximumValue()
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Determine the mode grid value in this sample dimension.
     */
    public double getModeValue()
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Determine the median grid value in this sample dimension.
     */
    public double getMedianValue()
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Determine the mean grid value in this sample dimension.
     */
    public double getMeanValue()
    {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Determine the standard deviation from the mean
     * of the grid values in this sample dimension.
     */
    public double getStandardDeviation()
    {throw new UnsupportedOperationException("Not implemented");}
}
