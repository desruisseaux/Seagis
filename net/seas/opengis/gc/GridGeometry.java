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

// Geometry
import java.awt.geom.Dimension2D;
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;
import net.seas.util.XDimension2D;

// Miscellaneous
import java.io.Serializable;


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
    private static final long serialVersionUID = 256746343396114708L;

    /**
     * The valid coordinate range of a grid coverage. The lowest
     * valid grid coordinate is zero. A grid with 512 cells can
     * have a minimum coordinate of 0 and maximum of 512, with 511
     * as the highest valid index.
     */
    private final GridRange gridRange;

    /**
     * Transformation affine convertissant les indices de pixels de l'image en coordonnées logiques (en
     * mètres ou en degrés selon le système de coordonnées de l'image). Pour convertir des indices de
     * pixels en coordonnées logiques, il suffit d'écrire:
     *
     * <pre>gridToCoordinateJAI.transform(pixels, point);</pre>
     *
     * Notez que la coordonnées obtenue sera dans le coin supérieur gauche du pixel (vers les indices
     * <var>i</var> et <var>j</var> minimums). Pour obtenir des coordonnées au centre du pixel, il faut
     * d'abord appeller <code>geoReferencing.translate(0.5, 0.5)</code> avant de faire les transformations,
     * ou encore ajouter 0.5 aux coordonnées pixels <code>pixels.x</code> et <code>pixels.y</code>.
     */
    private final AffineTransform gridToCoordinateJAI;

    /**
     * The math transform. If <code>null</code>, will be computed
     * from <code>gridToCoordinateJAI</code> when requested.
     */
    private MathTransform gridToCoordinateSystem;

    /**
     * Construct a new grid geometry.
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     * @param gridToCoordinateSystem The math transform which allows for the transformations
     *        from grid coordinates (pixel's <em>center</em>) to real world earth coordinates.
     */
    public GridGeometry(final GridRange gridRange, final MathTransform gridToCoordinateSystem)
    {
        this.gridRange              = gridRange;
        this.gridToCoordinateSystem = gridToCoordinateSystem;
        this.gridToCoordinateJAI    = null;
    }

    /**
     * Construct a new grid geometry.
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     * @param gridToCoordinateJAI The affine transform which allows for the transformations
     *        from grid coordinates (pixel's <em>upper left</em> corner) to real world earth
     *        coordinates.
     */
    GridGeometry(final GridRange gridRange, final AffineTransform gridToCoordinateJAI)
    {
        this.gridRange           = gridRange;
        this.gridToCoordinateJAI = gridToCoordinateJAI; // Cloned by caller
    }

    /**
     * Returns the valid coordinate range of a grid coverage.
     * The lowest valid grid coordinate is zero. A grid with
     * 512 cells can have a minimum coordinate of 0 and maximum
     * of 512, with 511 as the highest valid index.
     */
    public GridRange getGridRange()
    {return gridRange;}

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
    public synchronized MathTransform getGridToCoordinateSystem()
    {
        if (gridToCoordinateSystem==null)
        {
            if (gridRange.getDimension()!=2)
            {
                // TODO
                throw new UnsupportedOperationException("Not implemented");
            }
            final AffineTransform tr = getGridToCoordinateJAI();
            if (tr!=null)
            {
                // AffineTransform's operations are applied in reverse order.
                // We translate the grid coordinate by (0.5,0.5) first (which
                // set the position in the pixel center),  and then apply the
                // transformation specified by gridToCoordinateJAI().
                tr.translate(0.5, 0.5);
                gridToCoordinateSystem = MathTransformFactory.DEFAULT.createAffineTransform(tr);
            }
        }
        return gridToCoordinateSystem;
    }

    /**
     * Returns the affine transform which allows for the transformations
     * from grid coordinates to real world earth coordinates. The returned affine follows
     * <A HREF="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</A>
     * convention, i.e. its convert the pixel's <em>upper left corner</em> coordinates
     * (<var>i</var>,<var>j</var>) into real world earth coordinates (<var>x</var>,<var>y</var>).
     * In contrast, {link #gridToCoordinateSystem()} contert the pixel's <em>center</em>
     * coordinates into real world earth coordinates.
     */
    final AffineTransform getGridToCoordinateJAI()
    {return gridToCoordinateJAI;} // No clone for performance raisons.

    /**
     * Returns an estimation of cell size, in user coordinates.
     * Note: the returned dimension is an <em>estimation only</em>,
     *       and may be improved in future version.
     */
    public Dimension2D getCellSize2D()
    {
        if (gridToCoordinateJAI!=null)
        {
            final double scaleX0 = XAffineTransform.getScaleX0(gridToCoordinateJAI);
            final double scaleY0 = XAffineTransform.getScaleY0(gridToCoordinateJAI);
            return new XDimension2D.Double(scaleX0, scaleY0);
        }
        else
        {
            // TODO
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
