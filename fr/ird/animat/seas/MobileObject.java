/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.animat.seas;

// Géométrie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;

// Divers
import org.geotools.cs.Ellipsoid;
import org.geotools.resources.XMath;
import org.geotools.resources.XArray;
import org.geotools.resources.Utilities;


/**
 * Représentation d'un objet mobile. Un objet mobile peut être par
 * exemple un animal.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class MobileObject implements Shape
{
    /**
     * Le rayon de la terre, en mètres.
     */
    private static final double EARTH_RADIUS = Ellipsoid.WGS84.getSemiMajorAxis();

    /**
     * Longueur valide du tableau {@link #points}. Le nombre
     * de points sera la moitié de cette longueur.
     */
    private int validLength;

    /**
     * Coordonnées (<var>x</var>,<var>y</var>) de cet animal le long de
     * sa trajectoire, en <u>radians</u> de longitude et de latitude. Le
     * dernier point de ce tableau est la position actuelle de l'animal.
     */
    private float[] points = new float[8];

    /**
     * Direction actuelle de cet animal, en <u>radians arithmétique</u>.
     * La valeur par défaut est PI/2, ce qui correspond au nord vrai.
     */
    private double direction = (Math.PI/2);

    /**
     * Valeurs minimales de <var>x</var> et <var>y</var>,
     * en <u>radians</u> de longitude et de latitude.
     */
    private float xmin = Float.POSITIVE_INFINITY,
                  ymin = Float.POSITIVE_INFINITY,
                  xmax = Float.NEGATIVE_INFINITY,
                  ymax = Float.NEGATIVE_INFINITY;

    /**
     * Construit un objet mobile qui n'a pas de position initiale.
     * Appellez {@link #setLocation} après la construction de cet
     * objet pour lui affecter une position.
     */
    public MobileObject()
    {}

    /**
     * Définit la coordonnées <code>x,y</code> de cet animal en
     * <u>radians</u> de longitude et de latitude.   Ce nouveau
     * point sera ajouté à la trajectoire de l'animal,  mais la
     * direction restera inchangée.
     */
    private void setLocation(final float x, final float y)
    {
        if (validLength >= points.length)
        {
            points = XArray.resize(points, validLength + Math.min(validLength, 512));
        }
        points[validLength++] = x;
        points[validLength++] = y;
        if (x<xmin) xmin=x;
        if (x>xmax) xmax=x;
        if (y<ymin) ymin=y;
        if (y>ymax) ymax=y;
    }

    /**
     * Définit la coordonnées <code>x,y</code> de cet
     * animal en degrés de longitude et de latitude.
     * Ce nouveau point sera ajouté à la trajectoire
     * de l'animal.
     */
    public void setLocation(final Point2D position)
    {
        setLocation((float) Math.toRadians(position.getX()),
                    (float) Math.toRadians(position.getY()));
        // Note: si on veut ajuster la direction, il faudrait
        //       le faire ici (et non dans la méthode privée
        //       ci-dessous).
    }

    /**
     * Retourne la position actuelle de cet animal,
     * en degrés de longitude et de latitude.
     */
    public Point2D getLocation()
    {
        if (validLength==0) return null;
        return new Point2D.Double(Math.toDegrees(points[validLength-2]),
                                  Math.toDegrees(points[validLength-1]));
    }

    /**
     * Retourne la direction de cet animal, en degrés
     * géographique par rapport au nord vrai.
     */
    public double getDirection()
    {
        return 90-Math.toDegrees(direction);
    }

    /**
     * Tourne l'animal d'un certain angle par rapport
     * à sa direction actuelle.
     *
     * @param angle Angle en degrées, dans le sens des
     *        aiguilles d'une montre.
     */
    public void rotate(final double angle)
    {
        direction -= Math.toRadians(angle);
    }

    /**
     * Avance l'animal d'une certaine distance.
     *
     * @param distance Distance à avancer, en mètres.
     */
    public void move(final double distance)
    {
        if (validLength==0) return;
        double x = points[validLength-2];
        double y = points[validLength-1];

        // Compute parameters for a Mercator projection
        // centred on the current location (x,y).
        final double ak0      = Math.cos(y) * EARTH_RADIUS;
        final double northing = -ak0 * Math.log(Math.tan((Math.PI/4) + 0.5*y));
        final double meridian = x;

        // Compute the new position relative to (0,0).
        x = distance * Math.cos(direction);
        y = distance * Math.sin(direction);

        // Inverse project this point
        // from projected to geographic.
        x = x/ak0 + meridian;
        y = (Math.PI/2) - 2*Math.atan(Math.exp((northing-y)/ak0));
        setLocation((float)x, (float)y);
    }

    /**
     * Avance l'animal d'une certaine distance dans la direction du point
     * spécifié. Cet animal peut ne pas atteindre le point si la distance
     * est trop courte. Si la distance est trop longue, alors l'animal
     * s'arrêtera à la position du point spécifié. La direction de l'animal
     * sera modifiée de façon à correspondre à la direction vers le nouveau
     * point.
     *
     * @param distance Distance à parcourir, en mètres. Une valeur négative
     *                 fera fuir l'animal.
     * @param point    Point vers lequel avancer, en mètres. Un point identique
     *                 à <code>this</code> ne fera pas bouger l'animal, quelle
     *                 que soit la valeur de <code>distance</code>.
     * @return <code>true</code> si l'animal a atteint le point spécifié, ou
     *         <code>false</code> s'il s'est déplacé en direction de ce point
     *         sans l'atteindre.
     */
    public boolean moveToward(final double distance, final Point2D point)
    {
        if (validLength==0) return false;
        double x = points[validLength-2];
        double y = points[validLength-1];

        // Compute parameters for a Mercator projection
        // centred on the current location (x,y).
        final double ak0      = Math.cos(y) * EARTH_RADIUS;
        final double northing = -ak0 * Math.log(Math.tan((Math.PI/4) + 0.5*y));
        final double meridian = x;

        // Project the (px,py) point from
        // geographic to projected CS.
        double px = Math.toRadians(point.getX());
        double py = Math.toRadians(point.getY());
        px = ak0 * (px - meridian);
        py = ak0 * Math.log(Math.tan((Math.PI/4) + 0.5*py)) + northing;

        // Compute the direction toward the (px,py) point.
        final double newDirection = Math.atan2(py, px);
        if (!java.lang.Double.isNaN(newDirection))
        {
            direction = newDirection;
        }

        // Compute the new position relative to (0,0).
        final double fc = distance/XMath.hypot(px, py);
        if (java.lang.Double.isInfinite(fc) || fc>=1)
        {
            setLocation(point);
            return true;
        }
        px *= fc;
        py *= fc;

        // Inverse project the (px,py) point
        // from projected to geographic CS.
        x = px/ak0 + meridian;
        y = (Math.PI/2) - 2*Math.atan(Math.exp((northing-py)/ak0));
        setLocation((float)x, (float)y);
        return false;
    }

    /**
     * Transforme les coordonnées de la forme spécifiée des mètres
     * vers des coordonnées géographiques. En entrée, les coordonnées
     * de <code>shape</code> sont des metres relatifs à la position
     * actuelle de l'animal (c'est-à-dire que la position actuelle est
     * définie comme étant l'origine (0,0) du système de coordonnées).
     * En sortie, les coordonnées de <code>shape</code> sont des degrés
     * de longitude et de latitude.
     *
     * @param  shape Forme géométrique à transformer. Les coordonnées
     *         de cette forme <strong>seront</code> transformées.
     * @return La forme <code>shape</code>, ou <code>null</code>
     *         si la transformation n'a pas pu être effectuée.
     */
    public RectangularShape relativeToGeographic(final RectangularShape shape)
    {
        if (validLength==0) return null;
        double xmin = shape.getMinX();
        double xmax = shape.getMaxX();
        double ymin = shape.getMinY();
        double ymax = shape.getMaxY();
        final double x = points[validLength-2];
        final double y = points[validLength-1];
        final double[] corners = new double[]
        {
            xmin, ymin,
            xmax, ymin,
            xmax, ymax,
            xmin, ymax
        };

        // Compute parameters for a Mercator projection
        // centred on the current location (x,y).
        final double ak0      = Math.cos(y) * EARTH_RADIUS;
        final double northing = -ak0 * Math.log(Math.tan((Math.PI/4) + 0.5*y));
        final double meridian = x;

        // Inverse project this point
        // from projected to geographic.
        for (int i=0; i<corners.length; i+=2)
        {
            corners[i+0] = corners[i]/ak0 + meridian;
            corners[i+1] = (Math.PI/2) - 2*Math.atan(Math.exp((northing-corners[i+1])/ak0));
        }

        // Compute the shape.
        xmin = Math.toDegrees(Math.min(corners[0], corners[6]));
        xmax = Math.toDegrees(Math.max(corners[2], corners[4]));
        ymin = Math.toDegrees(Math.min(corners[1], corners[3]));
        ymax = Math.toDegrees(Math.max(corners[5], corners[7]));
        shape.setFrame(xmin, ymin, xmax-xmin, ymax-ymin);
        return shape;
    }

    /**
     * Retourne le nombre de points mémorisé jusqu'à maintenant.
     */
    public int getPointCount()
    {return validLength/2;}

    /**
     * Retourne le chemin suivit par l'animal jusqu'ici.
     */
    public Shape getPath()
    {return this;}

    /**
     * Retourne une chaîne de caractère représentant
     * cet animal. Cette information est utile pour
     * faciliter les déboguages.
     */
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer.append(validLength/2);
        buffer.append(" points");
        final Point2D location = getLocation();
        if (location != null)
        {
            buffer.append("; last=");
            buffer.append((float)location.getX());
            buffer.append(", ");
            buffer.append((float)location.getX());
        }
        buffer.append(']');
        return buffer.toString();
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////                                                       //////////
    //////////          IMPLEMENTATION DE L'INTERFACE Shape          //////////
    //////////                                                       //////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Tests if a specified {@link Point2D} is inside the boundary
     * of the <code>Shape</code>.
     */
    public boolean contains(Point2D p)
    {return false;}

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains the
     * specified <code>Rectangle2D</code>.
     */
    public boolean contains(Rectangle2D r)
    {return false;}

    /**
     * Tests if the specified coordinates are inside the boundary of the
     * <code>Shape</code>.
     */
    public boolean contains(double x, double y)
    {return false;}

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains
     * the specified rectangular area.
     */
    public boolean contains(double x, double y, double w, double h)
    {return false;}

    /**
     * Tests if the interior of the <code>Shape</code> intersects the
     * interior of a specified <code>Rectangle2D</code>.
     */
    public boolean intersects(Rectangle2D r)
    {return getBounds2D().intersects(r);}

    /**
     * Tests if the interior of the <code>Shape</code> intersects the
     * interior of a specified rectangular area.
     */
    public boolean intersects(double x, double y, double w, double h)
    {return intersects(new Rectangle2D.Double(x,y,w,h));}

    /**
     * Returns an integer {@link Rectangle} that completely encloses the
     * <code>Shape</code>.
     */
    public Rectangle getBounds()
    {
        final int xmin = (int)Math.floor(Math.toDegrees(this.xmin));
        final int ymin = (int)Math.floor(Math.toDegrees(this.ymin));
        final int xmax = (int)Math.ceil (Math.toDegrees(this.xmax));
        final int ymax = (int)Math.ceil (Math.toDegrees(this.ymax));
        return new Rectangle(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Returns a high precision and more accurate bounding box of
     * the <code>Shape</code> than the <code>getBounds</code> method.
     */
    public Rectangle2D getBounds2D()
    {
        final double xmin = Math.toDegrees(this.xmin);
        final double ymin = Math.toDegrees(this.ymin);
        final double xmax = Math.toDegrees(this.xmax);
        final double ymax = Math.toDegrees(this.ymax);
        return new Rectangle2D.Float((float)xmin, (float)ymin,
                                     (float)(xmax-xmin),
                                     (float)(ymax-ymin));
    }

    /**
     * Returns an iterator object that iterates along the
     * <code>Shape</code> boundary and provides access to the geometry of the
     * <code>Shape</code> outline.
     */
    public PathIterator getPathIterator(final AffineTransform at)
    {return new Iterator(points, validLength, at);}

    /**
     * Returns an iterator object that iterates along the <code>Shape</code>
     * boundary and provides access to a flattened view of the
     * <code>Shape</code> outline geometry.
     */
    public PathIterator getPathIterator(final AffineTransform at, double flatness)
    {return getPathIterator(at);}

    /**
     * Balaie les points de la trajectoire.
     */
    private static final class Iterator implements PathIterator
    {
        /**
         * Longueur valide du tableau {@link #points}.
         */
        private final int validLength;

        /**
         * Coordonnées (<var>x</var>,<var>y</var>) de cet animal le long de
         * sa trajectoire, en <u>radians</u> de longitude et de latitude.
         */
        private final float[] points;

        /**
         * Position actuelle.
         */
        private int pos;

        /**
         * Transformation affine à appliquer.
         */
        private final AffineTransform at;

        /**
         * Construit un nouvel itérateur.
         */
        private Iterator(final float[] points, final int validLength, final AffineTransform at)
        {
            this.points      = points;
            this.validLength = validLength;
            this.at          = at;
        }

        /**
         * Returns the winding rule for determining the interior of the path.
         */
        public int getWindingRule()
        {return WIND_EVEN_ODD;}

        /**
         * Tests if the iteration is complete.
         */
        public boolean isDone()
        {return pos>=validLength;}

        /**
         * Returns the coordinates and type of the current path segment in
         * the iteration.
         */
        public int currentSegment(final double[] coords)
        {
            coords[0] = Math.toDegrees(points[pos+0]);
            coords[1] = Math.toDegrees(points[pos+1]);
            if (at!=null)
            {
                at.transform(coords, 0, coords, 0, 1);
            }
            return (pos==0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Returns the coordinates and type of the current path segment in
         * the iteration.
         */
        public int currentSegment(final float[] coords)
        {
            coords[0] = (float)Math.toDegrees(points[pos+0]);
            coords[1] = (float)Math.toDegrees(points[pos+1]);
            if (at!=null)
            {
                at.transform(coords, 0, coords, 0, 1);
            }
            return (pos==0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Moves the iterator to the next segment of the path forwards
         * along the primary direction of traversal as long as there are
         * more points in that direction.
         */
        public void next()
        {pos += 2;}
    }
}
