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

// OpenGIS (SEAGIS) dependencies
import net.seas.opengis.ct.MathTransform;
import net.seas.opengis.ct.MathTransformFactory;

// Geometry
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
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
     * Transformation affine convertissant les indices de pixels de l'image en coordonn�es logiques (en
     * m�tres ou en degr�s selon le syst�me de coordonn�es de l'image). Pour convertir des indices de
     * pixels en coordonn�es logiques, il suffit d'�crire:
     *
     * <pre>gridToCoordinateJAI.transform(pixels, point);</pre>
     *
     * Notez que la coordonn�es obtenue sera dans le coin sup�rieur gauche du pixel (vers les indices
     * <var>i</var> et <var>j</var> minimums). Pour obtenir des coordonn�es au centre du pixel, il faut
     * d'abord appeller <code>geoReferencing.translate(0.5, 0.5)</code> avant de faire les transformations,
     * ou encore ajouter 0.5 aux coordonn�es pixels <code>pixels.x</code> et <code>pixels.y</code>.
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
     * Construct a new two-dimensional grid geometry. A map transform will
     * be computed automatically with an inverted <var>y</var> axis (i.e.
     * <code>gridRange</code> and <code>userRange</code> are assumed to
     * have axis in opposite direction).
     *
     * @param gridRange The valid coordinate range of a grid coverage.
     *                  Increasing <var>x</var> values goes right and
     *                  increasing <var>y</var> values goes <strong>down</strong>.
     * @param userRange The corresponding coordinate range in user coordinate.
     *                  Increasing <var>x</var> values goes right and
     *                  increasing <var>y</var> values goes <strong>up</strong>.
     *                  This rectangle must contains entirely all pixels, i.e.
     *                  the rectangle's upper left corner must coincide with
     *                  the upper left corner of the first pixel and the rectangle's
     *                  lower right corner must coincide with the lower right corner
     *                  of the last pixel.
     */
    public GridGeometry(final Rectangle gridRange, final Rectangle2D userRange)
    {
        final double scaleX = userRange.getWidth()  / gridRange.getWidth();
        final double scaleY = userRange.getHeight() / gridRange.getHeight();
        final double transX = userRange.getMinX()   - gridRange.x*scaleX;
        final double transY = userRange.getMaxY()   + gridRange.y*scaleY;
        this.gridRange           = new GridRange(gridRange);
        this.gridToCoordinateJAI = new AffineTransform(scaleX, 0, 0, -scaleY, transX, transY);
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
     */
    public synchronized MathTransform getGridToCoordinateSystem()
    {
        if (gridToCoordinateSystem==null)
        {
            if (gridToCoordinateJAI!=null)
            {
                // AffineTransform's operations are applied in reverse order.
                // We translate the grid coordinate by (0.5,0.5) first (which
                // set the position in the pixel center),  and then apply the
                // transformation specified by gridToCoordinateJAI().
                final AffineTransform tr = new AffineTransform(gridToCoordinateJAI);
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
}
