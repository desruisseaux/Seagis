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
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #FROM_WORLD_TO_POINT}).transform(point, point)</code></td>
 *       <td>pour transformer les m�tres en des unit�s proches de 1/72 de pouce. Avec cette transformation,
 *           nous passons des "m�tres" sur le terrain en "points" sur l'�cran (ou l'imprimante).</td></tr>
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #FROM_POINT_TO_PIXEL}).transform(point, point)</code></td>
 *       <td>pour passer des 1/72 de pouce vers des unit�s qui d�pendent du p�riph�rique.</td></tr>
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
     * Constante d�signant la transformation affine qui permet de
     * passer des coordonn�es logiques vers des unit�s de points.
     */
    public static final int FROM_WORLD_TO_POINT = 1;

    /**
     * Constante d�signant la transformation affine qui permet de
     * passer des des unit�s de points vers des coordonn�es pixels.
     */
    public static final int FROM_POINT_TO_PIXEL = 2;

    /**
     * Transformation (g�n�ralement une projection cartographique) servant � convertir les
     * coordonn�es g�ographiques vers les donn�es projet�es � l'�cran. La valeur retourn�e
     * par {@link CoordinateTransform#getTargetCS} doit obligatoirement �tre le syst�me de
     * coordonn�es utilis� pour l'affichage. En revanche, la valeur retourn�e par {@link
     * CoordinateTransform#getSourceCS} peut �tre n'importe quel syst�me de coordonn�es,
     * mais il vaux mieux pour des raisons de performance que ce soit le syst�me de
     * coordonn�es le plus utilis� par les couches.
     */
    private final CoordinateTransform transform;

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
     * Indique si le tra�age se fait vers l'imprimante plut�t qu'� l'�cran.
     */
    private final boolean isPrinting;

    /**
     * Construit un objet <code>MapPaintContext</code> avec les param�tres sp�cifi�s.
     * Ce constructeur ne fait pas de clones.
     *
     * @param transform  Transformation (g�n�ralement une projection cartographique) servant � convertir les
     *                   coordonn�es g�ographiques vers les donn�es projet�es � l'�cran. La valeur retourn�e
     *                   par {@link CoordinateTransform#getTargetCS} doit obligatoirement �tre le syst�me de
     *                   coordonn�es utilis� pour l'affichage. En revanche, la valeur retourn�e par {@link
     *                   CoordinateTransform#getSourceCS} peut �tre n'importe quel syst�me de coordonn�es,
     *                   mais il vaux mieux pour des raisons de performance que ce soit le syst�me de
     *                   coordonn�es le plus utilis� par les couches.
     * @param fromWorld  Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     * @param fromPoints Transformation affine convertissant des unit�s de texte (1/72 de pouce) en unit�s d�pendantes du p�riph�rique.
     * @param bounds     Position et dimension de la r�gion de la fen�tre dans lequel se fait le tra�age.
     * @param isPrinting Indique si le tra�age se fait vers l'imprimante plut�t qu'� l'�cran.
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
     * Retourne le syst�me de coordonn�es de l'afficheur.
     */
    public CoordinateSystem getViewCoordinateSystem()
    {return transform.getTargetCS();}

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
     * Retourne une transformation affine. Deux types de transformations sont d'int�ret:
     *
     * <ul>
     *   <li>{@link #FROM_WORLD_TO_POINT}:
     *       Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     *       Ces unit�s de textes pourront ensuite �tre converties en unit�s du p�riph�rique avec la
     *       transformation {@link #FROM_POINT_TO_PIXEL}. Cette transformation peut varier en fonction
     *       de l'�chelle de la carte.</li>
     *   <li>{@link #FROM_POINT_TO_PIXEL}:
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
            case FROM_WORLD_TO_POINT: return fromWorld;
            case FROM_POINT_TO_PIXEL: return fromPoints;
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
     * Indique si le tra�age se fait vers l'imprimante
     * (ou un autre p�riphique) plut�t qu'� l'�cran.
     */
    public boolean isPrinting()
    {return isPrinting;}
}
