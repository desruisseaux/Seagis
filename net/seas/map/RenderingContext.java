/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 * Information relatives aux tra�age d'une carte. Ces informations comprennent notamment la suite des transformations
 * n�cessaires � la conversion de coordonn�es g�ographiques en coordonn�es pixels. Soit <code>point</code> un objet
 * {@link Point2D} repr�sentant une coordonn�e. Alors la conversion de la coordonn�e g�ographique vers la coordonn�e
 * pixel suivra le chemin suivant (l'ordre des op�rations est important):
 *
 * <blockquote><table>
 *   <tr><td><code>{@link #getCoordinateTransform getCoordinateTransform}(layer).transform(point, point)</code></td>
 *       <td>pour convertir la coordonn�e g�ographique de la couche {@link Layer} sp�cifi�e vers
 *           vers le syst�me de coordonn�es de l'afficheur {@link MapPanel}. Le r�sultat est
 *           encore en coordonn�es logiques (par exemple en v�ritables m�tres sur le terrain
 *           ou encore en degr�s de longitude ou de latitude).</td></tr>
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #WORLD_TO_POINT}).transform(point, point)</code></td>
 *       <td>pour transformer les m�tres en des unit�s proches de 1/72 de pouce. Avec cette transformation,
 *           nous passons des "m�tres" sur le terrain en "points" sur l'�cran (ou l'imprimante).</td></tr>
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #POINT_TO_PIXEL}).transform(point, point)</code></td>
 *       <td>pour passer des 1/72 de pouce vers des unit�s qui d�pendent du p�riph�rique.</td></tr>
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
     * Constante d�signant la transformation affine qui permet de
     * passer des coordonn�es logiques vers des unit�s de points.
     */
    public static final int WORLD_TO_POINT = 1;

    /**
     * Constante d�signant la transformation affine qui permet de
     * passer des des unit�s de points vers des coordonn�es pixels.
     */
    public static final int POINT_TO_PIXEL = 2;

    /**
     * Indique si le tra�age se fait vers l'imprimante plut�t qu'� l'�cran.
     */
    private final boolean isPrinting;

    /**
     * Transformation (g�n�ralement une projection cartographique) servant � convertir les
     * coordonn�es g�ographiques vers les donn�es projet�es � l'�cran. La valeur retourn�e
     * par {@link CoordinateTransformation#getTargetCS} doit obligatoirement �tre le syst�me
     * de coordonn�es utilis� pour l'affichage. En revanche, la valeur retourn�e par {@link
     * CoordinateTransformation#getSourceCS} peut �tre n'importe quel syst�me de coordonn�es,
     * mais il vaux mieux pour des raisons de performance que ce soit le syst�me de
     * coordonn�es le plus utilis� par les couches.
     */
    private final CoordinateTransformation transformation;

    /**
     * Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     * Ces unit�s de textes pourront ensuite �tre converties en unit�s du p�riph�rique avec la
     * transformation {@link #fromPoints}. Cette transformation <code>fromWorld</code> peut varier
     * en fonction de l'�chelle de la carte, tandis que la transformation {@link #fromPoints}
     * ne varie g�n�ralement pas pour un p�riph�rique donn�.
     */
    private final AffineTransform fromWorld;

    /**
     * Transformation affine convertissant des unit�s de texte (1/72 de pouce) en unit�s d�pendantes
     * du p�riph�rique. Lors des sorties vers l'�cran, cette transformation est g�n�ralement la matrice
     * identit�. Pour les �critures vers l'imprimante, il s'agit d'une matrice configur�e d'une fa�on
     * telle que chaque point correspond � environ 1/72 de pouce.
     *
     * Cette transformation affine reste habituellement identique d'un tra�age � l'autre
     * de la composante. Elle varie si par exemple on passe de l'�cran vers l'imprimante.
     */
    private final AffineTransform fromPoints;

    /**
     * Position et dimension de la r�gion de la
     * fen�tre dans lequel se fait le tra�age.
     */
    private final Rectangle bounds;

    /**
     * The {@link #bounds} rectangle transformed into logical
     * coordinates (according {@link #getViewCoordinateSystem}).
     */
    private transient Rectangle2D logicalClip;

    /**
     * Objet � utiliser pour d�couper les polygones. Cet objet
     * ne sera cr�� que la premi�re fois o� il sera demand�.
     */
    private transient Clipper clipper;

    /**
     * Construit un objet <code>RenderingContext</code> avec les param�tres sp�cifi�s.
     * Ce constructeur ne fait pas de clones.
     *
     * @param transformation Transformation (g�n�ralement une projection cartographique) servant � convertir les
     *                   coordonn�es g�ographiques vers les donn�es projet�es � l'�cran. La valeur retourn�e
     *                   par {@link CoordinateTransformation#getTargetCS} doit obligatoirement �tre le syst�me de
     *                   coordonn�es utilis� pour l'affichage. En revanche, la valeur retourn�e par {@link
     *                   CoordinateTransformation#getSourceCS} peut �tre n'importe quel syst�me de coordonn�es,
     *                   mais il vaux mieux pour des raisons de performance que ce soit le syst�me de
     *                   coordonn�es le plus utilis� par les couches.
     * @param fromWorld  Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     * @param fromPoints Transformation affine convertissant des unit�s de texte (1/72 de pouce) en unit�s d�pendantes du p�riph�rique.
     * @param bounds     Position et dimension de la r�gion de la fen�tre dans lequel se fait le tra�age.
     * @param isPrinting Indique si le tra�age se fait vers l'imprimante plut�t qu'� l'�cran.
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
     * Retourne le syst�me de coordonn�es de l'afficheur.
     */
    public CoordinateSystem getViewCoordinateSystem()
    {return transformation.getTargetCS();}

    /**
     * Retourne la transformation � utiliser pour convertir les coordonn�es d'une couche vers
     * les coordonn�es projet�es � l'�cran.  Cette transformation sera souvent une projection
     * cartographique.
     *
     * @param  layer Couche dont on veut convertir les coordonn�es.
     * @return Une transformation qui transformera les coordonn�es de la couche sp�cifi�e
     *         (<code>layer</code>) vers les coordonn�es affich�es � l'�cran (afficheur
     *         {@link MapPanel}).
     * @throws CannotCreateTransformException Si la transformation n'a pas pu �tre cr��e.
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
     * Retourne une transformation affine. Deux types de transformations sont d'int�ret:
     *
     * <ul>
     *   <li>{@link #WORLD_TO_POINT}:
     *       Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     *       Ces unit�s de textes pourront ensuite �tre converties en unit�s du p�riph�rique avec la
     *       transformation {@link #POINT_TO_PIXEL}. Cette transformation peut varier en fonction
     *       de l'�chelle de la carte.</li>
     *   <li>{@link #POINT_TO_PIXEL}:
     *       Transformation affine convertissant des unit�s de texte (1/72 de pouce) en unit�s d�pendantes
     *       du p�riph�rique. Lors des sorties vers l'�cran, cette transformation est g�n�ralement la matrice
     *       identit�. Pour les �critures vers l'imprimante, il s'agit d'une matrice configur�e d'une fa�on
     *       telle que chaque point correspond � environ 1/72 de pouce. Cette transformation affine reste
     *       habituellement identique d'un tra�age � l'autre de la composante. Elle ne varie que si par
     *       exemple on passe de l'�cran vers l'imprimante.</li>
     * </ul>
     *
     * <strong>Note: cette m�thode ne fait pas de clone. Ne modifiez pas l'objet retourn�!</strong>
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
     * Retourne la position et dimension de la r�gion de la fen�tre dans lequel se
     * fait le tra�age. Les coordonn�es de ce rectangle sont exprim�es en pixels.
     * <strong>Note: cette m�thode ne fait pas de clone. Ne modifiez pas l'objet
     * retourn�!</strong>
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
            throw new IllegalArgumentException(Resources.format(Cl�.EMPTY_LIST));
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
     * Indique si le tra�age se fait vers l'imprimante
     * (ou un autre p�riph�rique) plut�t qu'� l'�cran.
     */
    public boolean isPrinting()
    {return isPrinting;}
}
