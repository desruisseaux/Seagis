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
package net.seas.map;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.MathTransform2D;
import net.seagis.ct.TransformException;
import net.seagis.ct.CoordinateTransformation;
import net.seagis.ct.CannotCreateTransformException;
import net.seagis.resources.OpenGIS;

// Geometry
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seagis.resources.XAffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Miscellaneous
import java.util.List;
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;


/**
 * Information relatives aux traçage d'une carte. Ces informations comprennent notamment la suite des transformations
 * nécessaires à la conversion de coordonnées géographiques en coordonnées pixels. Soit <code>point</code> un objet
 * {@link Point2D} représentant une coordonnée. Alors la conversion de la coordonnée géographique vers la coordonnée
 * pixel suivra le chemin suivant (l'ordre des opérations est important):
 *
 * <blockquote><table>
 *   <tr><td><code>{@link #getCoordinateTransform getCoordinateTransform}(layer).transform(point, point)</code></td>
 *       <td>pour convertir la coordonnée géographique de la couche {@link Layer} spécifiée vers
 *           vers le système de coordonnées de l'afficheur {@link MapPanel}. Le résultat est
 *           encore en coordonnées logiques (par exemple en véritables mètres sur le terrain
 *           ou encore en degrés de longitude ou de latitude).</td></tr>
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #WORLD_TO_POINT}).transform(point, point)</code></td>
 *       <td>pour transformer les mètres en des unités proches de 1/72 de pouce. Avec cette transformation,
 *           nous passons des "mètres" sur le terrain en "points" sur l'écran (ou l'imprimante).</td></tr>
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #POINT_TO_PIXEL}).transform(point, point)</code></td>
 *       <td>pour passer des 1/72 de pouce vers des unités qui dépendent du périphérique.</td></tr>
 * </table></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see Layer#paint
 * @see MapPanel#paintComponent
 */
public final class RenderingContext
{
    /**
     * Expansion factor for clip. When a clip for some rectangle is requested, a bigger
     * clip will be computed in order to avoid recomputing a new one if user zoom up or
     * apply translation. A scale of 2 means that rectangle two times wider and heigher
     * will be computed.
     *
     * DEBUGGING TIPS: Set this scale to a value below 1 to <em>see</em> the clipping's
     *                 effect in the window area.
     */
    private static final double CLIP_SCALE = 0.75;

    /**
     * Constante désignant la transformation affine qui permet de
     * passer des coordonnées logiques vers des unités de points.
     */
    public static final int WORLD_TO_POINT = 1;

    /**
     * Constante désignant la transformation affine qui permet de
     * passer des des unités de points vers des coordonnées pixels.
     */
    public static final int POINT_TO_PIXEL = 2;

    /**
     * Indique si le traçage se fait vers l'imprimante plutôt qu'à l'écran.
     */
    private final boolean isPrinting;

    /**
     * Transformation (généralement une projection cartographique) servant à convertir les
     * coordonnées géographiques vers les données projetées à l'écran. La valeur retournée
     * par {@link CoordinateTransformation#getTargetCS} doit obligatoirement être le système
     * de coordonnées utilisé pour l'affichage. En revanche, la valeur retournée par {@link
     * CoordinateTransformation#getSourceCS} peut être n'importe quel système de coordonnées,
     * mais il vaux mieux pour des raisons de performance que ce soit le système de
     * coordonnées le plus utilisé par les couches.
     */
    private final CoordinateTransformation transformation;

    /**
     * Transformation affine convertissant les mètres vers les unités de texte (1/72 de pouce).
     * Ces unités de textes pourront ensuite être converties en unités du périphérique avec la
     * transformation {@link #fromPoints}. Cette transformation <code>fromWorld</code> peut varier
     * en fonction de l'échelle de la carte, tandis que la transformation {@link #fromPoints}
     * ne varie généralement pas pour un périphérique donné.
     */
    private final AffineTransform fromWorld;

    /**
     * Transformation affine convertissant des unités de texte (1/72 de pouce) en unités dépendantes
     * du périphérique. Lors des sorties vers l'écran, cette transformation est généralement la matrice
     * identité. Pour les écritures vers l'imprimante, il s'agit d'une matrice configurée d'une façon
     * telle que chaque point correspond à environ 1/72 de pouce.
     *
     * Cette transformation affine reste habituellement identique d'un traçage à l'autre
     * de la composante. Elle varie si par exemple on passe de l'écran vers l'imprimante.
     */
    private final AffineTransform fromPoints;

    /**
     * Position et dimension de la région de la
     * fenêtre dans lequel se fait le traçage.
     */
    private final Rectangle bounds;

    /**
     * The {@link #bounds} rectangle transformed into logical
     * coordinates (according {@link #getViewCoordinateSystem}).
     */
    private transient Rectangle2D logicalClip;

    /**
     * Objet à utiliser pour découper les polygones. Cet objet
     * ne sera créé que la première fois où il sera demandé.
     */
    private transient Clipper clipper;

    /**
     * Construit un objet <code>RenderingContext</code> avec les paramètres spécifiés.
     * Ce constructeur ne fait pas de clones.
     *
     * @param transformation Transformation (généralement une projection cartographique) servant à convertir les
     *                   coordonnées géographiques vers les données projetées à l'écran. La valeur retournée
     *                   par {@link CoordinateTransformation#getTargetCS} doit obligatoirement être le système de
     *                   coordonnées utilisé pour l'affichage. En revanche, la valeur retournée par {@link
     *                   CoordinateTransformation#getSourceCS} peut être n'importe quel système de coordonnées,
     *                   mais il vaux mieux pour des raisons de performance que ce soit le système de
     *                   coordonnées le plus utilisé par les couches.
     * @param fromWorld  Transformation affine convertissant les mètres vers les unités de texte (1/72 de pouce).
     * @param fromPoints Transformation affine convertissant des unités de texte (1/72 de pouce) en unités dépendantes du périphérique.
     * @param bounds     Position et dimension de la région de la fenêtre dans lequel se fait le traçage.
     * @param isPrinting Indique si le traçage se fait vers l'imprimante plutôt qu'à l'écran.
     */
    RenderingContext(final CoordinateTransformation transformation, final AffineTransform fromWorld, final AffineTransform fromPoints, final Rectangle bounds, final boolean isPrinting)
    {
        if (transformation!=null && fromWorld!=null && fromPoints!=null)
        {
            this.transformation = transformation;
            this.fromWorld      = fromWorld;
            this.fromPoints     = fromPoints;
            this.isPrinting     = isPrinting;
            this.bounds         = bounds;
        }
        else throw new NullPointerException();
    }

    /**
     * Retourne le système de coordonnées de l'afficheur.
     */
    public CoordinateSystem getViewCoordinateSystem()
    {return transformation.getTargetCS();}

    /**
     * Retourne la transformation à utiliser pour convertir les coordonnées d'une couche vers
     * les coordonnées projetées à l'écran.  Cette transformation sera souvent une projection
     * cartographique.
     *
     * @param  layer Couche dont on veut convertir les coordonnées.
     * @return Une transformation qui transformera les coordonnées de la couche spécifiée
     *         (<code>layer</code>) vers les coordonnées affichées à l'écran (afficheur
     *         {@link MapPanel}).
     * @throws CannotCreateTransformException Si la transformation n'a pas pu être créée.
     */
    public MathTransform2D getMathTransform2D(final Layer layer) throws CannotCreateTransformException
    {
        CoordinateTransformation transformation = this.transformation;
        final CoordinateSystem source=layer.getCoordinateSystem();
        if (!transformation.getSourceCS().equivalents(source))
        {
            transformation = Contour.createFromCoordinateSystems(source, transformation.getTargetCS(), "RenderingContext", "getCoordinateTransform");
        }
        return (MathTransform2D) transformation.getMathTransform();
    }

    /**
     * Retourne une transformation affine. Deux types de transformations sont d'intéret:
     *
     * <ul>
     *   <li>{@link #WORLD_TO_POINT}:
     *       Transformation affine convertissant les mètres vers les unités de texte (1/72 de pouce).
     *       Ces unités de textes pourront ensuite être converties en unités du périphérique avec la
     *       transformation {@link #POINT_TO_PIXEL}. Cette transformation peut varier en fonction
     *       de l'échelle de la carte.</li>
     *   <li>{@link #POINT_TO_PIXEL}:
     *       Transformation affine convertissant des unités de texte (1/72 de pouce) en unités dépendantes
     *       du périphérique. Lors des sorties vers l'écran, cette transformation est généralement la matrice
     *       identité. Pour les écritures vers l'imprimante, il s'agit d'une matrice configurée d'une façon
     *       telle que chaque point correspond à environ 1/72 de pouce. Cette transformation affine reste
     *       habituellement identique d'un traçage à l'autre de la composante. Elle ne varie que si par
     *       exemple on passe de l'écran vers l'imprimante.</li>
     * </ul>
     *
     * <strong>Note: cette méthode ne fait pas de clone. Ne modifiez pas l'objet retourné!</strong>
     */
    public AffineTransform getAffineTransform(final int type)
    {
        switch (type)
        {
            case WORLD_TO_POINT: return fromWorld;
            case POINT_TO_PIXEL: return fromPoints;
            default: throw new IllegalArgumentException(Integer.toString(type));
        }
    }

    /**
     * Retourne la position et dimension de la région de la fenêtre dans lequel se
     * fait le traçage. Les coordonnées de ce rectangle sont exprimées en pixels.
     * <strong>Note: cette méthode ne fait pas de clone. Ne modifiez pas l'objet
     * retourné!</strong>
     */
    public Rectangle getZoomableBounds()
    {return bounds;}

    /**
     * Clip a contour to the current widget's bounds. The clip is only approximative
     * in that the resulting contour may extends outside the widget's area. However,
     * it is garanteed that the resulting contour will contains at least the interior
     * of the widget's area (providing that the first contour in the supplied list
     * cover this area).
     *
     * This method is used internally by some layers (like {@link net.seas.map.layer.IsolineLayer})
     * when computing and drawing a clipped contour may be faster than drawing the full contour
     * (especially if clipped contours are cached for reuse).
     * <br><br>
     * This method expect a <em>modifiable</em> list of {@link Contour} objects as argument.
     * The first element in this list must be the "master" contour (the contour to clip) and
     * will never be modified.  Elements at index greater than 0 may be added and removed at
     * this method's discression, so that the list is actually used as a cache for clipped
     * <code>Contour</code> objects.
     *
     * <br><br>
     * <strong>WARNING: This method is not yet debugged</strong>
     *
     * @param  contours A modifiable list with the contour to clip at index 0.
     * @return A possibly clipped contour. May be any element of the list or a new contour.
     *         May be <code>null</code> if the "master" contour doesn't intercept the clip.
     */
    public Contour clip(final List<Contour> contours)
    {
        if (contours.isEmpty())
        {
            throw new IllegalArgumentException(Resources.format(Clé.EMPTY_LIST));
        }
        if (isPrinting)
        {
            return contours.get(0);
        }
        /*
         * Gets the clip area expressed in MapPanel's coordinate system
         * (i.e. gets bounds in "logical visual coordinates").
         */
        if (logicalClip==null) try
        {
            logicalClip = XAffineTransform.inverseTransform(fromWorld, bounds, logicalClip);
        }
        catch (NoninvertibleTransformException exception)
        {
            // (should not happen) Clip failed: conservatively returns the whole contour.
            ExceptionMonitor.unexpectedException("net.seas.map", "RenderingContext", "clip", exception);
            return contours.get(0);
        }
        final CoordinateSystem targetCS = getViewCoordinateSystem();
        /*
         * Iterate through the list (starting from the last element)
         * until we found a contour capable to handle the clip area.
         */
        Contour contour;
        Rectangle2D clip;
        Rectangle2D bounds;
        Rectangle2D temporary=null;
        int index=contours.size();
        do
        {
            contour = contours.get(--index);
            clip    = logicalClip;
            /*
             * First, we need to know the clip in contour's coordinates.
             * The {@link net.seas.map.layer.IsolineLayer} usually keeps
             * isoline in the same coordinate system than the MapPanel's
             * one. But a user could invoke this method in a more unusual
             * way, so we are better to check...
             */
            final CoordinateSystem sourceCS;
            synchronized (contour)
            {
                bounds   = contour.getCachedBounds();
                sourceCS = contour.getCoordinateSystem();
            }
            if (!targetCS.equivalents(sourceCS)) try
            {
                CoordinateTransformation transformation = this.transformation;
                if (!transformation.getSourceCS().equivalents(sourceCS))
                {
                    transformation = Contour.createFromCoordinateSystems(sourceCS, targetCS, "RenderingContext", "clip");
                }
                clip = temporary = OpenGIS.transform((MathTransform2D)transformation.getMathTransform(), clip, temporary);
            }
            catch (TransformException exception)
            {
                ExceptionMonitor.unexpectedException("net.seas.map", "RenderingContext", "clip", exception);
                continue; // A contour seems invalid. It will be ignored (and probably garbage collected soon).
            }
            /*
             * Now that both rectangles are using the same coordinate system,
             * test if the clip fall completly inside the contour. If yes,
             * then we should use this contour for clipping.
             */
            if (Layer.contains(bounds, clip, true)) break;
        }
        while (index!=0);
        /*
         * A clipped contour has been found (or we reached the begining
         * of the list). Check if the requested clip is small enough to
         * worth a clipping.
         */
        final double ratio2 = (bounds.getWidth()*bounds.getHeight()) / (clip.getWidth()*clip.getHeight());
        if (ratio2 >= CLIP_SCALE*CLIP_SCALE)
        {
            if (clipper==null)
            {
                clipper = new Clipper(scale(logicalClip, CLIP_SCALE), targetCS);
            }
            // Remove the last part of the list, which is likely to be invalide.
            contours.subList(index+1, contours.size()).clear();
            contour=contour.getClipped(clipper);
            if (contour!=null)
            {
                contours.add(contour);
                Contour.LOGGER.finer("Clip performed"); // TODO: give more precision
            }
        }
        return contour;
    }

    /**
     * Expand or shrunk a rectangle by some factor. A scale of 1 lets the rectangle
     * unchanged. A scale of 2 make the rectangle two times wider and heigher. In
     * any case, the rectangle's center doesn't move.
     */
    private static Rectangle2D scale(final Rectangle2D rect, final double scale)
    {
        final double trans  = 0.5*(scale-1);
        final double width  = rect.getWidth();
        final double height = rect.getHeight();
        return new Rectangle2D.Double(rect.getX()-trans*width,
                                      rect.getY()-trans*height,
                                      scale*width, scale*height);
    }

    /**
     * Indique si le traçage se fait vers l'imprimante
     * (ou un autre périphérique) plutôt qu'à l'écran.
     */
    public boolean isPrinting()
    {return isPrinting;}
}
