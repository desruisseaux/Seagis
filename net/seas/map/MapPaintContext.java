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
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.ct.CannotCreateTransformException;

// Geometry
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;


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
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #FROM_WORLD_TO_POINT}).transform(point, point)</code></td>
 *       <td>pour transformer les mètres en des unités proches de 1/72 de pouce. Avec cette transformation,
 *           nous passons des "mètres" sur le terrain en "points" sur l'écran (ou l'imprimante).</td></tr>
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #FROM_POINT_TO_PIXEL}).transform(point, point)</code></td>
 *       <td>pour passer des 1/72 de pouce vers des unités qui dépendent du périphérique.</td></tr>
 * </table></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see Layer#paint
 * @see MapPanel#paintComponent
 */
public final class MapPaintContext
{
    /**
     * Constante désignant la transformation affine qui permet de
     * passer des coordonnées logiques vers des unités de points.
     */
    public static final int FROM_WORLD_TO_POINT = 1;

    /**
     * Constante désignant la transformation affine qui permet de
     * passer des des unités de points vers des coordonnées pixels.
     */
    public static final int FROM_POINT_TO_PIXEL = 2;

    /**
     * Transformation (généralement une projection cartographique) servant à convertir les
     * coordonnées géographiques vers les données projetées à l'écran. La valeur retournée
     * par {@link CoordinateTransform#getTargetCS} doit obligatoirement être le système de
     * coordonnées utilisé pour l'affichage. En revanche, la valeur retournée par {@link
     * CoordinateTransform#getSourceCS} peut être n'importe quel système de coordonnées,
     * mais il vaux mieux pour des raisons de performance que ce soit le système de
     * coordonnées le plus utilisé par les couches.
     */
    private final CoordinateTransform transform;

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
     * Indique si le traçage se fait vers l'imprimante plutôt qu'à l'écran.
     */
    private final boolean isPrinting;

    /**
     * Construit un objet <code>MapPaintContext</code> avec les paramètres spécifiés.
     * Ce constructeur ne fait pas de clones.
     *
     * @param transform  Transformation (généralement une projection cartographique) servant à convertir les
     *                   coordonnées géographiques vers les données projetées à l'écran. La valeur retournée
     *                   par {@link CoordinateTransform#getTargetCS} doit obligatoirement être le système de
     *                   coordonnées utilisé pour l'affichage. En revanche, la valeur retournée par {@link
     *                   CoordinateTransform#getSourceCS} peut être n'importe quel système de coordonnées,
     *                   mais il vaux mieux pour des raisons de performance que ce soit le système de
     *                   coordonnées le plus utilisé par les couches.
     * @param fromWorld  Transformation affine convertissant les mètres vers les unités de texte (1/72 de pouce).
     * @param fromPoints Transformation affine convertissant des unités de texte (1/72 de pouce) en unités dépendantes du périphérique.
     * @param bounds     Position et dimension de la région de la fenêtre dans lequel se fait le traçage.
     * @param isPrinting Indique si le traçage se fait vers l'imprimante plutôt qu'à l'écran.
     */
    MapPaintContext(final CoordinateTransform transform, final AffineTransform fromWorld, final AffineTransform fromPoints,
                    final Rectangle bounds, final boolean isPrinting)
    {
        if (transform!=null && fromWorld!=null && fromPoints!=null)
        {
            this.transform  = transform;
            this.fromWorld  = fromWorld;
            this.fromPoints = fromPoints;
            this.isPrinting = isPrinting;
            this.bounds     = bounds;
        }
        else throw new NullPointerException();
    }

    /**
     * Retourne le système de coordonnées de l'afficheur.
     */
    public CoordinateSystem getViewCoordinateSystem()
    {return transform.getTargetCS();}

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
    public CoordinateTransform getCoordinateTransform(final Layer layer) throws CannotCreateTransformException
    {
        final CoordinateSystem source=layer.getCoordinateSystem();
        if (!transform.getSourceCS().equivalents(source))
        {
            return Contour.TRANSFORMS.createFromCoordinateSystems(source, transform.getTargetCS());
        }
        return transform;
    }

    /**
     * Retourne une transformation affine. Deux types de transformations sont d'intéret:
     *
     * <ul>
     *   <li>{@link #FROM_WORLD_TO_POINT}:
     *       Transformation affine convertissant les mètres vers les unités de texte (1/72 de pouce).
     *       Ces unités de textes pourront ensuite être converties en unités du périphérique avec la
     *       transformation {@link #FROM_POINT_TO_PIXEL}. Cette transformation peut varier en fonction
     *       de l'échelle de la carte.</li>
     *   <li>{@link #FROM_POINT_TO_PIXEL}:
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
            case FROM_WORLD_TO_POINT: return fromWorld;
            case FROM_POINT_TO_PIXEL: return fromPoints;
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
     * Indique si le traçage se fait vers l'imprimante
     * (ou un autre périphique) plutôt qu'à l'écran.
     */
    public boolean isPrinting()
    {return isPrinting;}
}
