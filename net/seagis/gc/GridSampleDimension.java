/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.gc;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cv.CategoryList;
import net.seagis.cv.SampleDimension;
import net.seagis.cv.ColorInterpretation;

// Miscellaneous
import javax.media.jai.Histogram;


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
     * Band number for this sample dimension.
     * TODO: Should be set from GridCoverage.
     */
    private final int band = 0;

    /**
     * Construct a sample dimension with a set of categories.
     *
     * @param categories The category list for this sample dimension, or
     *        <code>null</code> if this sample dimension has no category.
     */
    public GridSampleDimension(final CategoryList categories)
    {super(categories);}

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
//  public double getMinimumValue()
//  {return getHistogram().getLowValue(band);}

    /**
     * Returns the maximum value occurring in this sample dimension.
     */
//  public double getMaximumValue()
//  {return getHistogram().getHighValue(band);}

    /**
     * Determine the mode grid value in this sample dimension.
     */
//  public double getModeValue()
//  {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Determine the median grid value in this sample dimension.
     */
//  public double getMedianValue()
//  {throw new UnsupportedOperationException("Not implemented");}

    /**
     * Determine the mean grid value in this sample dimension.
     */
//  public double getMeanValue()
//  {return getHistogram().getMean()[band];}

    /**
     * Determine the standard deviation from the mean
     * of the grid values in a sample dimension.
     */
//  public double getStandardDeviation()
//  {return getHistogram().getStandardDeviation()[band];}

    /**
     * Gets the histogram for the underlying grid coverage.
     */
//  private Histogram getHistogram()
//  {throw new UnsupportedOperationException("Not implemented");}
}