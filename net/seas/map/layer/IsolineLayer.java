/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
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
package net.seas.map.layer;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.TransformException;

// Map
import net.seas.map.Layer;
import net.seas.map.Contour;
import net.seas.map.Polygon;
import net.seas.map.Isoline;
import net.seas.map.GeoMouseEvent;
import net.seas.map.RenderingContext;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seagis.resources.XDimension2D;
import net.seagis.resources.XAffineTransform;

// Graphics
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Graphics2D;
import javax.swing.UIManager;
import javax.media.jai.GraphicsJAI;

// Miscellaneous
import java.util.List;
import java.util.ArrayList;
import net.seagis.resources.XMath;


/**
 * A layer for an {@link Isoline} object. Instances of this class are typically
 * used for isobaths. Each isobath (e.g. sea-level, 50 meters, 100 meters...)
 * require a different instance of <code>IsolineLayer</code>.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class IsolineLayer extends Layer implements Polygon.Renderer
{
    /**
     * Set to <code>false</code> to disable clipping acceleration.
     * May be useful if you suspect that a bug is preventing proper
     * rendering.
     */
    private static final boolean ENABLE_CLIP = false;
    // TODO: NEED TO DEBUG. NEED TO TRACE INTO RenderingContext.clip

    /**
     * Default color for fills.
     */
    private static final Color FILL_COLOR = new Color(59,107,92);

    /**
     * The "preferred line tickness" relative to the isoline's resolution.
     * A value of 1 means that isoline might be drawn with a line as tick
     * as the isoline's resolution. A value of 0.25 means that isoline might
     * be drawn with a line of tickness equals to 1/4 of the isoline's resolution.
     */
    private static final double TICKNESS = 0.25;

    /**
     * The isoline data.
     */
    protected final Isoline isoline;

    /**
     * Clipped isolines. A clipped isoline may be
     * faster to render than the full isoline.
     */
    private final List<Contour> clipped = ENABLE_CLIP ? new ArrayList<Contour>(4) : null;

    /**
     * Paint for contour lines. Default to
     * panel's foreground (usually black).
     */
    private Paint contour = UIManager.getColor("Panel.foreground");

    /**
     * Paint for filling holes. Default to
     * panel's background (usually gray).
     */
    private Paint background = UIManager.getColor("Panel.background");

    /**
     * Paint for filling elevations.
     */
    private Paint fill = FILL_COLOR;

    /**
     * The desired rendering resolution in points.
     */
    private int resolution = 6;

    /**
     * Construct a layer for the specified isoline.
     */
    public IsolineLayer(Isoline isoline)
    {
        super((isoline=isoline.clone()).getCoordinateSystem());
        this.isoline = isoline;
        setZOrder(isoline.value);

        final Rectangle2D  bounds = isoline.getBounds2D();
        final float    resolution = isoline.getResolution();
        final Ellipsoid ellipsoid = isoline.getEllipsoid();
        final double dx,dy;
        if (ellipsoid!=null)
        {
            // Transforms the resolution into a pixel size in the middle of 'bounds'.
            // Note: 'r' is the inverse of **apparent** ellipsoid's radius at latitude 'y'.
            //       For the inverse of "real" radius, we would have to swap sin and cos.
            final double   y = Math.toRadians(bounds.getCenterY());
            final double sin = Math.sin(y);
            final double cos = Math.cos(y);
            final double   r = XMath.hypot(sin/ellipsoid.getSemiMajorAxis(),
                                           cos/ellipsoid.getSemiMinorAxis());
            dy = Math.toDegrees(resolution*r);
            dx = dy*cos;
        }
        else dx = dy = resolution;
        setPreferredPixelSize(new XDimension2D.Double(TICKNESS*dx , TICKNESS*dy));
        setPreferredArea(bounds);
        if (clipped!=null)
        {
            clipped.add(isoline);
        }
    }

    /**
     * Sets the contouring color or paint.
     * This paint will be used by all polygons.
     */
    public void setContour(final Paint paint)
    {contour=paint;}

    /**
     * Returns the contouring color or paint.
     */
    public Paint getContour()
    {return contour;}

    /**
     * Sets the filling color or paint. This paint
     * will be used only for closed polygons.
     */
    public void setFill(final Paint paint)
    {fill=paint;}

    /**
     * Returns the filling color or paint.
     */
    public Paint getfill()
    {return fill;}

    /**
     * Set the background color or paint. This information
     * is needed in order to allows <code>IsolineLayer</code>
     * to fill holes correctly.
     */
    public void setBackground(final Paint paint)
    {background=paint;}

    /**
     * Returns the background color or paint.
     */
    public Paint getBackground()
    {return background;}

    /**
     * Sets the rendering resolution in points. A value of 6 means that <code>IsolineLayer</code>
     * will try to render polygons with line of about 6 points long. Higher values can speed up
     * rendering and reduce memory footprint at the expense of quality. The actual number of points
     * used for rendering will be dynamically computed from the zoom active at drawing time.
     *
     * @param resolution The desired rendering resolution in points. When rendering on
     *        screen, a point is a pixel.  When rendering on printer, a point is about
     *        1/72 of inch.
     */
    public void setRenderingResolution(final int resolution)
    {
        if (resolution>0) this.resolution=resolution;
        else throw new IllegalArgumentException(String.valueOf(resolution));
    }

    /**
     * Returns the rendering resolution in points.
     */
    public int getRenderingResolution()
    {return resolution;}

    /**
     * Draw or fill a polygon. The rendering is usually done with <code>graphics.draw(polygon)</code>
     * or <code>graphics.fill(polygon)</code>. This method may change the paint and stroke attributes
     * of <code>graphics</code> before to perform the rendering. However, it should not make any change
     * to <code>polygon</code> since this method may be invoked with arguments internal to some objects,
     * for performance raisons.
     *
     * @param graphics The graphics context.
     * @param polygon  The polygon to draw.
     */
    public void drawPolygon(final Graphics2D graphics, final Polygon polygon)
    {
        switch (polygon.getInteriorSign())
        {
            case Polygon.ELEVATION:
            {
                graphics.setPaint(fill);
                graphics.fill(polygon);
                if (contour.equals(fill)) return;
                break;
            }
            case Polygon.DEPRESSION:
            {
                graphics.setPaint(background);
                graphics.fill(polygon);
                if (contour.equals(background)) return;
                break;
            }
        }
        graphics.setPaint(contour);
        graphics.draw(polygon);
    }

    /**
     * Drawn the isolines. Default implementation invokes <code>drawPolygon(...)</code>
     * for each polygon to drawn.   Note that polygons given to code>drawPolygon</code>
     * may be clipped or decimated for faster rendering.
     *
     * @param  graphics The graphics context.
     * @param  context  The set of transformations needed for transforming geographic
     *         coordinates (<var>longitude</var>,<var>latitude</var>) into pixels coordinates.
     * @return A bounding shape of isolines, in points coordinates.
     * @throws TransformException If a transformation failed.
     */
    protected Shape paint(final GraphicsJAI graphics, final RenderingContext context) throws TransformException
    {
        /*
         * Reproject isoline if the coordinate system changed
         * (all cached isolines must be discarded in this case).
         */
        final CoordinateSystem viewCS = context.getViewCoordinateSystem();
        if (!viewCS.equivalents(isoline.getCoordinateSystem()))
        {
            isoline.setCoordinateSystem(viewCS);
            if (clipped!=null)
            {
                clipped.clear();
                clipped.add(isoline);
            }
        }
        /*
         * Rendering acceleration: First performs the clip (if enabled),
         *                         then compute the decimation to use.
         */
        final Rectangle2D  bounds = isoline.getBounds2D();
        final AffineTransform  tr = context.getAffineTransform(RenderingContext.WORLD_TO_POINT);
        final Isoline      toDraw = (clipped!=null) ? (Isoline)context.clip(clipped) : isoline;
        if (toDraw!=null)
        {
            final Paint      oldPaint = graphics.getPaint();
            final Stroke    oldStroke = graphics.getStroke();
            final Ellipsoid ellipsoid = isoline.getEllipsoid();
            double r; // Desired resolution (a higher value will lead to faster rendering)
            if (ellipsoid!=null)
            {
                final double  x = bounds.getCenterX();
                final double  y = bounds.getCenterY();
                final double dx = 0.5/XAffineTransform.getScaleX0(tr);
                final double dy = 0.5/XAffineTransform.getScaleY0(tr);
                r = ellipsoid.orthodromicDistance(x-dx, y-dy, x+dy, y+dy);
            }
            else
            {
                // Assume a cartesian coordinate system.
                r = 1/Math.sqrt((r=tr.getScaleX())*r + (r=tr.getScaleY())*r +
                                (r=tr.getShearX())*r + (r=tr.getShearY())*r);
            }
            toDraw.paint(graphics, (float)(resolution*r), this);
            graphics.setStroke(oldStroke);
            graphics.setPaint (oldPaint);
        }
        return XAffineTransform.transform(tr, bounds, bounds);
    }

    /**
     * Returns a tool tip text for the specified coordinates.
     * Default implementation delegate to {@link Isoline#getToolTipText}.
     *
     * @param  event The mouve event with geographic coordinétes.
     * @return The tool tip text, or <code>null</code> if there
     *         in no tool tips for this location.
     */
    protected String getToolTipText(final GeoMouseEvent event)
    {
        final Point2D point=event.getVisualCoordinate(null);
        if (point!=null)
        {
            final String toolTips = isoline.getToolTipText(point);
            if (toolTips!=null) return toolTips;
        }
        return super.getToolTipText(event);
    }
}
