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

// OpenGIS (SEAGIS) dependencies
import net.seas.opengis.ct.MathTransform;
import net.seas.opengis.ct.MathTransformFactory;

// Miscellaneous
import java.io.Serializable;
import java.awt.geom.AffineTransform;


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
public class GridGeometry implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 398116590319460364L;

    /**
     * The valid coordinate range of a grid coverage. The lowest
     * valid grid coordinate is zero. A grid with 512 cells can
     * have a minimum coordinate of 0 and maximum of 512, with 511
     * as the highest valid index.
     */
    private final GridRange range;

    /**
     * The affine transform, or <code>null</code> if the transform
     * can't be represented as a 2D affine transform.
     */
    private final AffineTransform transformJAI;

    /**
     * The math transform. If <code>null</code>, will be
     * computed from <code>transformJAI</code> when requested.
     */
    private MathTransform transform;

    /**
     * Construct a new grid geometry.
     *
     * @param range The valid coordinate range of a grid coverage.
     * @param transform The math transform which allows for the transformations
     *        from grid coordinates (pixel's <em>center</em>) to real world earth
     *        coordinates.
     */
    public GridGeometry(final GridRange range, final MathTransform transform)
    {
        this.range     = range;
        this.transform = transform;
        transformJAI   = null;
    }

    /**
     * Construct a new grid geometry.
     *
     * @param range The valid coordinate range of a grid coverage.
     * @param transform The affine transform which allows for the transformations
     *        from grid coordinates (pixel's <em>upper left</em> corner) to real
     *        world earth coordinates.
     */
    public GridGeometry(final GridRange range, final AffineTransform transform)
    {
        this.range   = range;
        transformJAI = new AffineTransform(transform);
    }

    /**
     * Returns the valid coordinate range of a grid coverage.
     * The lowest valid grid coordinate is zero. A grid with
     * 512 cells can have a minimum coordinate of 0 and maximum
     * of 512, with 511 as the highest valid index.
     */
    public GridRange getGridRange()
    {return range;}

    /**
     * Returns the math transform which allows for the transformations
     * from grid coordinates to real world earth coordinates. The transform is often an
     * affine transformation. The coordinate system of the real world coordinates
     * is given by {@link net.seas.opengis.cv.Coverage#getCoordinateSystem}. If no
     * math transform is available, this method returns <code>null</code>.
     * <br><br>
     * The default implementation compute the math
     * transform from {@link #gridToCoordinateJAI}.
     */
    public synchronized MathTransform gridToCoordinateSystem()
    {
        if (transform==null)
        {
            final AffineTransform tr = gridToCoordinateJAI();
            if (tr!=null)
            {
                // AffineTransform's operations are applied in reverse order.
                // We translate the grid coordinate by (0.5,0.5) first (which
                // set the position in the pixel center),  and then apply the
                // transformation specified by gridToCoordinateJAI().
                tr.translate(0.5, 0.5);
                transform = MathTransformFactory.DEFAULT.createAffineTransform(tr);
            }
        }
        return transform;
    }

    /**
     * <FONT COLOR="#FF6633">Returns the affine transform which allows for the transformations
     * from grid coordinates to real world earth coordinates.</FONT> The returned affine follows
     * <A HREF="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</A>
     * convention, i.e. its convert the pixel's <em>upper left corner</em> coordinates
     * (<var>i</var>,<var>j</var>) into real world earth coordinates (<var>x</var>,<var>y</var>).
     * In contrast, {link #gridToCoordinateSystem()} contert the pixel's <em>center</em>
     * coordinates into real world earth coordinates.
     */
    public AffineTransform gridToCoordinateJAI()
    {return (transformJAI!=null) ? (AffineTransform) transformJAI.clone() : null;}
}
