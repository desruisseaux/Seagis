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
import net.seas.opengis.ct.CoordinateTransformFactory;
import net.seas.opengis.cs.GeographicCoordinateSystem;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Formatting
import java.text.Format;
import java.text.NumberFormat;
import java.text.FieldPosition;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.text.AngleFormat;

// Logging
import java.util.logging.Logger;

// Miscellaneous
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.util.Version;


/**
 * Lignes de contour à un seul niveau. Ces lignes de contours peuvent être un isobath
 * (par exemple le tracé des côtes d'un archipel) ou un polygone seul (par exemple le
 * tracé de la côte d'une seule île d'un archipel).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class Contour implements Shape, Cloneable, Serializable
{
    /**
     * Objet à utiliser pour fabriquer des transformations.
     */
    static final CoordinateTransformFactory TRANSFORMS = CoordinateTransformFactory.DEFAULT;

    /**
     * The logger for map operations.
     */
    static final Logger logger = Logger.getLogger("net.seas.map");

    /**
     * Nom de ce contour.   Il s'agit en général d'un nom géographique, par exemple
     * "Île d'Anticosti" ou "Lac Supérieur". Ce champs peut être nul si ce polygone
     * ne porte pas de nom.
     */
    private String name;

    /**
     * Construit un contour initialement vide.
     */
    public Contour()
    {}

    /**
     * Donne un nom à ce contour. Ce nom peut-être un nom de lieu,
     * par exemple si ce contour représente une île ou un lac. Ce
     * nom peut être nul si ce contour n'est pas nommé.
     */
    public void setName(final String name)
    {this.name = name;}

    /**
     * Retourne le nom de ce contour. Ce nom peut
     * être nul si ce contour n'est pas nommé.
     */
    public String getName()
    {return name;}

    /**
     * Retourne le système de coordonnées de ce contour.
     * Cette méthode peut retourner <code>null</code> s'il
     * n'est pas connu.
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Spécifie le système de coordonnées dans lequel retourner les points du contour.
     * Appeller cette méthode est équivalent à projeter tous les points du contour de
     * l'ancien système de coordonnées vers le nouveau.
     *
     * @param  coordinateSystem Système de coordonnées dans lequel exprimer les points
     *         du contour. La valeur <code>null</code> restaurera le système "natif".
     * @throws TransformException si une projection cartographique a échouée.
     */
    public abstract void setCoordinateSystem(final CoordinateSystem coordinateSystem) throws TransformException;

    /**
     * Determines whetever this contour is empty.
     */
    public abstract boolean isEmpty();

    /**
     * Indique si ce contour contient entièrement le rectangle spécifié.
     * Le rectangle doit être exprimé selon le système de coordonnées de
     * ce contour, soit {@link #getCoordinateSystem()}.
     */
    public boolean contains(final double x, final double y, final double width, final double height)
    {return contains(new Rectangle2D.Double(x, y, width, height));}

    /**
     * Test if the interior of this contour
     * entirely contains the given shape.
     */
    public abstract boolean contains(final Shape shape);

    /**
     * Indique si ce contour intercepte au moins en partie le rectangle spécifié.
     * Le rectangle doit être exprimé selon le système de coordonnées du polygone,
     * soit {@link #getCoordinateSystem()}.
     */
    public boolean intersects(final double x, final double y, final double width, final double height)
    {return intersects(new Rectangle2D.Double(x, y, width, height));}

    /**
     * Indique si ce contour intercepte au
     * moins en partie la forme spécifiée.
     */
    public abstract boolean intersects(final Shape shape);

    /**
     * Renvoie la résolution moyenne de ce contour. Cette résolution sera la distance moyenne
     * (en mètres) entre deux points du contour, mais sans prendre en compte les "points de
     * bordure" (par exemple les points qui suivent le bord d'une carte plutôt que de représenter
     * une structure géographique réelle).
     *
     * @return La résolution moyenne en mètres, ou {@link Float#NaN}
     *         si ce contour ne contient pas de points.
     */
    public abstract float getResolution();

    /**
     * Modifie la résolution de ce contour. Cette méthode procèdera en interpolant les données de façon
     * à ce que chaque point soit séparé du précédent par la distance spécifiée.   Cela peut se traduire
     * par des économies importante de mémoire si une trop grande résolution n'est pas nécessaire. Notez
     * que cette opération est irreversible.  Appeler cette méthode une seconde fois avec une résolution
     * plus fine gonflera la taille des tableaux internes, mais sans amélioration réelle de la précision.
     *
     * @param  resolution Résolution désirée (en mètres).
     * @throws TransformException Si une erreur est survenue lors d'une projection cartographique.
     */
    public abstract void setResolution(final double resolution) throws TransformException;

    /**
     * Return the number of points in this contour.
     */
    public abstract int getPointCount();

    /**
     * Retourne le texte à afficher dans une bulle lorsque la souris
     * traîne à la coordonnée spécifiée.
     *
     * @param point Coordonnées pointées par la souris. Ces coordonnées
     *        doivent être exprimées selon le système de coordonnées de
     *        ce contour ({@link #getCoordinateSystem}).
     */
    public abstract String getToolTipText(final Point2D point);

    /**
     * Return a string representation of this contour for debugging purpose.
     * The returned string will look like
     * "<code>Polygon["Île Quelconque", 44°30'N-51°59'N  70°59'W-54°59'W (56 pts)]</code>".
     */
    public String toString()
    {
        final Format format;
        final Rectangle2D bounds=getBounds2D();
        Object minX,minY,maxX,maxY;
        if (getCoordinateSystem() instanceof GeographicCoordinateSystem)
        {
            minX=new Longitude(bounds.getMinX());
            minY=new Latitude (bounds.getMinY());
            maxX=new Longitude(bounds.getMaxX());
            maxY=new Latitude (bounds.getMaxY());
            format = new AngleFormat();
        }
        else
        {
            minX=new Float((float) bounds.getMinX());
            minY=new Float((float) bounds.getMinY());
            maxX=new Float((float) bounds.getMaxX());
            maxY=new Float((float) bounds.getMaxY());
            format=NumberFormat.getNumberInstance();
        }
        final String         name=getName();
        final FieldPosition dummy=new FieldPosition(0);
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        if (name!=null)
        {
            buffer.append('"');
            buffer.append(name);
            buffer.append("\", ");
        }
        format.format(minY, buffer, dummy).append('-' );
        format.format(maxY, buffer, dummy).append("  ");
        format.format(minX, buffer, dummy).append('-' );
        format.format(maxX, buffer, dummy).append(" (");
        buffer.append(getPointCount()); buffer.append(" pts)]");
        return buffer.toString();
    }

    /**
     * Returns an hash code for this contour. Subclasses should
     * redefined this method to provide a more appropriate value.
     */
    public int hashCode()
    {return (name!=null) ? name.hashCode() : 0;}

    /**
     * Indique si ce contour est identique à l'objet spécifié. Cette méthode retourne <code>true</code>
     * si <code>object</code> est de la même classe que <code>this</code> et si les deux contours ont le
     * même nom. Les classes dérivées devront redéfinir cette méthode pour vérifier aussi les coordonnées
     * des points.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            return XClass.equals(getName(), ((Contour) object).getName());
        }
        else return false;
    }

    /**
     * Return a clone of this contour.
     */
    public Contour clone()
    {
        try
        {
            return (Contour) super.clone();
        }
        catch (CloneNotSupportedException exception)
        {
            // Should never happen, since we are cloneable.
            InternalError e=new InternalError(exception.getLocalizedMessage());
            if (Version.MINOR>=4) e.initCause(exception);
            throw e;
        }
    }
}
