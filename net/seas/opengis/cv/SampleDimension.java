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
package net.seas.opengis.cv;

// Miscellaneous
import java.util.Locale;
import javax.units.Unit;


/**
 * Describes the data values for a coverage.
 * For a grid coverage a sample dimension is a band.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cv.CV_SampleDimension
 */
public abstract class SampleDimension
{
    /**
     * The category list for this sample dimension,
     * or <code>null</code> if there is none.
     */
    private final CategoryList categories;

    /**
     * Default constructor.
     *
     * @param categories The category list for this sample
     *        dimension, or <code>null</code> if there is none.
     */
    public SampleDimension(final CategoryList categories)
    {this.categories = categories;}

    /**
     * Get the sample dimension name.
     */
    public abstract String getName();

    /**
     * Get the sample dimension title or description.
     * This string may be <code>null</code> if no description is present.
     * The default implementation returns the name of what seem to be the
     * "main" category, i.e. the quantative category (if there is one) with
     * the widest sample range.
     *
     * @param  locale The locale, or <code>null</code> for the default one.
     * @return The localized description. If no description was available
     *         in the specified locale, a default locale is used.
     */
    public String getDescription(final Locale locale)
    {
        if (categories!=null)
        {
            float range=0;
            Category category=null;
            for (int i=categories.size(); --i>=0;)
            {
                final Category candidate = categories.get(i);
                if (candidate!=null && candidate.isQuantitative())
                {
                    final float candidateRange = candidate.upper - candidate.lower;
                    if (candidateRange > range)
                    {
                        range = candidateRange;
                        category = candidate;
                    }
                }
            }
            if (category!=null)
                return category.getName(locale);
        }
        return null;
    }

    /**
     * Returns the category list for the values contained in a sample dimension.
     * This allows for names to be assigned to numerical values. If no categories
     * exist, <code>null</code> is returned.
     */
    public CategoryList getCategoryList()
    {return categories;}

    // NOTE: "getPaletteInterpretation()" is not available in SEAGIS since
    //       palette are backed by IndexColorModel, which support only RGB.

    /**
     * Returns the color interpretation of the sample dimension.
     * A sample dimension can be an index into a color palette or be a color model
     * component. If the sample dimension is not assigned a color interpretation
     * the value is {@link ColorInterpretation#UNDEFINED}.
     */
    public abstract ColorInterpretation getColorInterpretation();

    // NOTE: "scale" and "offset" functionality are handled by Category.

    /**
     * Returns the unit information for this sample dimension.
     * May returns <code>null</code> is this dimension has no units.
     */
    public Unit getUnits()
    {return (categories!=null) ? categories.getUnits() : null;}

    /**
     * Returns the minimum value occurring in this sample dimension.
     */
    public abstract double getMinimumValue();

    /**
     * Returns the maximum value occurring in this sample dimension.
     */
    public abstract double getMaximumValue();

    /**
     * Determine the mode grid value in this sample dimension.
     */
    public abstract double getModeValue();

    /**
     * Determine the median grid value in this sample dimension.
     */
    public abstract double getMedianValue();

    /**
     * Determine the mean grid value in this sample dimension.
     */
    public abstract double getMeanValue();

    /**
     * Determine the standard deviation from the mean
     * of the grid values in this sample dimension.
     */
    public abstract double getStandardDeviation();

    // No histogram. It is moved in GridCoverage instead.
}
