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

// Dependencies
import net.seas.opengis.ct.MathTransform;


/**
 * Describes the valid range of grid coordinates and the math
 * transform to transform grid coordinates to real world coordinates.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.gc.GC_GridGeometry
 */
public abstract class GridGeometry
{
    /**
     * The valid coordinate range of a grid coverage. The lowest
     * valid grid coordinate is zero. A grid with 512 cells can
     * have a minimum coordinate of 0 and maximum of 512, with 511
     * as the highest valid index.
     */
    private final GridRange range;

    /**
     * Construct a new grid geometry.
     *
     * @param range The valid coordinate range of a grid coverage.
     */
    public GridGeometry(final GridRange range)
    {this.range = range;}

    /**
     * Returns the valid coordinate range of a grid coverage.
     * The lowest valid grid coordinate is zero. A grid with
     * 512 cells can have a minimum coordinate of 0 and maximum
     * of 512, with 511 as the highest valid index.
     */
    public GridRange getGridRange()
    {return range;}

    /**
     * Returns the math transform which allows for the transformations from grid
     * coordinates to real world earth coordinates. The transform is often an
     * affine transformation. The coordinate system of the real world coordinates
     * is given by {@link net.seas.opengis.cv.Coverage#getCoordinateSystem}. If no
     * math transform is available, this method returns <code>null</code>.
     */
    public abstract MathTransform gridToCoordinateSystem();
}
