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
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;

// Map
import net.seas.map.Layer;
import net.seas.map.Isoline;
import net.seas.map.Polygon;
import net.seas.map.GeoMouseEvent;
import net.seas.map.MapPaintContext;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;

// Graphics
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Graphics2D;
import javax.media.jai.GraphicsJAI;


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
     * The isoline data.
     */
    protected final Isoline isoline;

    /**
     * Paint for contour lines.
     */
    private Paint contour = Color.black;

    /**
     * Paint for filling holes.
     */
    private Paint background = Color.white;

    /**
     * Paint for filling elevations.
     */
    private Paint fill = Color.blue;

    /**
     * Construct a layer for the specified isoline.
     */
    public IsolineLayer(Isoline isoline)
    {
        super((isoline=isoline.clone()).getCoordinateSystem());
        this.isoline = isoline;
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
                break;
            }
            case Polygon.DEPRESSION:
            {
                graphics.setPaint(background);
                graphics.fill(polygon);
                break;
            }
        }
        graphics.setPaint(contour);
        graphics.draw(polygon);
    }

    /**
     * Drawn the isolines. Default implementation invokes <code>drawPolygon(...)</code>
     * for each polygon to drawn.
     *
     * @param  graphics The graphics context.
     * @param  context  The set of transformations needed for transforming geographic
     *         coordinates (<var>longitude</var>,<var>latitude</var>) into pixels coordinates.
     * @return A bounding shape of isolines, in points coordinates.
     * @throws TransformException If a transformation failed.
     */
    protected Shape paint(final GraphicsJAI graphics, final MapPaintContext context) throws TransformException
    {
        final Paint     oldPaint = graphics.getPaint();
        final Stroke   oldStroke = graphics.getStroke();
        final AffineTransform tr = context.getAffineTransform(MapPaintContext.FROM_WORLD_TO_POINT);
        double t; t=Math.sqrt((t=tr.getScaleX())*t + (t=tr.getScaleY())*t + (t=tr.getShearX())*t + (t=tr.getShearY())*t);

        isoline.setCoordinateSystem(context.getViewCoordinateSystem());
        isoline.paint(graphics, (float)(1/t), this);
        graphics.setStroke(oldStroke);
        graphics.setPaint (oldPaint);

        final Rectangle2D bounds = isoline.getBounds2D();
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
