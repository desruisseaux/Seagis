/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.animat.server;

// G�om�trie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;

// Entr�s/sorties et divers
import java.util.Arrays;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// Divers
import org.geotools.cs.Ellipsoid;
import org.geotools.resources.XMath;
import org.geotools.resources.XArray;
import org.geotools.resources.Utilities;
import org.geotools.resources.XRectangle2D;

// Animats
import fr.ird.animat.Clock;


/**
 * Trajectoire suivit par un {@link Animal}. Cette trajectoire contient les coordonn�es
 * g�ographiques (en degr�s de longitudes et de latitudes) de toutes les positions visit�es
 * par l'animal � chaque {@linkplain Clock#getStepSequenceNumber(Date) pas de temps}.
 *
 * Toutes les coordonn�es spatiales sont exprim�es en degr�es de longitudes
 * et de latitudes selon l'ellipso�de {@linkplain Ellipsoid#WGS84 WGS84}.
 * Les d�placements sont exprim�es en milles nautiques, et les directions
 * en degr�s g�ographiques (c'est-�-dire par rapport au nord "vrai").
 * Les d�placements et le cap sont calcul�s en utilisant une projection
 * de Mercator mobile, toujours centr�e sur la position actuelle.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Path extends Point2D implements Shape, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -8013330420228233319L;

    /**
     * Le rayon de la terre, en milles nautiques.
     */
    private static final double EARTH_RADIUS = Ellipsoid.WGS84.getSemiMajorAxis()/1852;

    /**
     * The length of a (x,y) record.
     */
    static final int RECORD_LENGTH = 2;

    /**
     * Longueur valide du tableau {@link #points}.  Le nombre de points sera la moiti�
     * de cette longueur. Le nombre de points valides est habituellement �gal � {@link
     * Clock#getStepSequenceNumber} plus 1,  le "+1" �tant n�cessaire afin de contenir
     * la coordonn�e du point au pas de temps courant.
     */
    private transient int validLength;

    /**
     * Coordonn�es (<var>x</var>,<var>y</var>) le long de trajectoire de
     * l'animal, en <u>radians</u> de longitude et de latitude. Le dernier
     * point de ce tableau est la position actuelle de l'animal.
     */
    private float[] points = new float[4*RECORD_LENGTH];

    /**
     * Direction actuelle de cet animal, en <u>radians arithm�tique</u>.
     * La valeur par d�faut est PI/2, ce qui correspond au nord vrai.
     */
    private double direction = (Math.PI/2);

    /**
     * Valeurs minimales de <var>x</var> et <var>y</var>,
     * en <u>radians</u> de longitude et de latitude.
     */
    private float xmin = java.lang.Float.POSITIVE_INFINITY,
                  ymin = java.lang.Float.POSITIVE_INFINITY,
                  xmax = java.lang.Float.NEGATIVE_INFINITY,
                  ymax = java.lang.Float.NEGATIVE_INFINITY;

    /**
     * Construit une trajectoire qui commencera � la position sp�cifi�e.  Notez que cette position
     * initiale n'est pas d�finitive; on peut la changer � n'importe quel moment par un appel � la
     * m�thode {@link #setLocation}. Elle ne deviendra d�finitive qu'apr�s avoir appel� la m�thode
     * {@link Animal#observe}.
     *
     * @param position Position initiale de la trajectoire.
     */
    public Path(final Point2D position) {
        setLocationRadians((float)Math.toRadians(position.getX()),
                           (float)Math.toRadians(position.getY()));
        assert getPointCount() == 1 : validLength;
    }

    /**
     * D�fini le nombre de points que contiendra ce chemin.  Cette m�thode est appel�e par {@link
     * Animal#observe}  pour signaler  qu'il faut m�moriser la position actuelle  dans le tableau
     * {@link #points} � un indice correspondant au {@linkplain Clock#getStepSequenceNumber() pas
     * de temps courant}.   En g�n�ral, cet appel n'aura aucun effet puisque la position actuelle
     * est d�j� au bon endroit dans le tableau {@link #points}.    Elle peut toutefois r�duire le
     * tableau si l'animal a �t� d�plac� plusieurs fois dans le m�me pas de temps, ou agrandir le
     * tableau si l'animal n'a pas �t� d�plac� du tout, afin que la longueur valide du tableau
     * soit coh�rent avec le num�ro du pas de temps courant. Dans tous les cas, le cap est conserv�.
     */
    final void setPointCount(int n) {
        n *= RECORD_LENGTH;
        if (n == validLength) {
            return;
        }
        if (n >= points.length) {
            points = XArray.resize(points, Math.max(n + Math.min(n, 1024*RECORD_LENGTH), 4*RECORD_LENGTH));
        }
        do {
            // Copy only once if we are reducing the array.
            System.arraycopy(points, validLength-RECORD_LENGTH, points, n-RECORD_LENGTH, RECORD_LENGTH);
        } while ((validLength+=RECORD_LENGTH) < n);
        validLength = n;
    }

    /**
     * Ajoute les coordonn�es <code>x,y</code> (en <u>radians</u> de longitude et de latitude)
     * � cette trajectoire. Le cap restera inchang�.
     *
     * @param x Longitude en radians.
     * @param y Latitude en radians.
     */
    private void setLocationRadians(final float x, final float y) {
        if (validLength >= points.length) {
            points = XArray.resize(points, Math.max(validLength + Math.min(validLength, 512), 8));
        }
        points[validLength++] = x;
        points[validLength++] = y;
        if (x<xmin) xmin=x;
        if (x>xmax) xmax=x;
        if (y<ymin) ymin=y;
        if (y>ymax) ymax=y;
    }

    /**
     * Ajoute les coordonn�es <code>x,y</code> (en degr�s de longitude et de latitude) � cette
     * trajectoire. Le {@linkplain #getHeading cap} sera modifi� en fonction de la droite
     * reliant la derni�re position � la position sp�cifi�e.
     *
     * @param x Longitude en degr�s.
     * @param y Latitude en degr�s.
     */
    public void setLocation(double x, double y) {
        setLocationRadians((float) Math.toRadians(x),
                           (float) Math.toRadians(y));
        x -= points[validLength - 2*RECORD_LENGTH + 0];
        y -= points[validLength - 2*RECORD_LENGTH + 1];
        direction = Math.atan2(y,x);
    }

    /**
     * Retourne la longitude de la position actuelle, en degr�s.
     */
    public double getX() {
        return Math.toDegrees(points[validLength-RECORD_LENGTH+0]);
    }

    /**
     * Retourne la latitude de la position actuelle, en degr�s.
     */
    public double getY() {
        return Math.toDegrees(points[validLength-RECORD_LENGTH+1]);
    }

    /**
     * Retourne le cap actuel, en degr�s g�ographiques par rapport au nord vrai.
     */
    public double getHeading() {
        return 90-Math.toDegrees(direction);
    }

    /**
     * Retourne le nombre de points m�moris� jusqu'� maintenant.
     */
    public int getPointCount() {
        return validLength/RECORD_LENGTH;
    }

    /**
     * Retourne une des positions visit�es depuis la cr�ation de
     * cet objet. Si seule la position actuelle est d�sir�e, alors
     * on peut utiliser directement <code>this</code>.
     *
     * @param  index Indice de la position d�sir�e, de 0 inclusivement jusqu'�
     *         {@link #getPointCount} exclusivement. L'indice du pas de temps
     *         courant peut �tre obtenu avec {@link Clock#getStepSequenceNumber}.
     * @return Les coordonn�es � la position sp�cifi�e.
     * @throws IndexOutOfBoundsException si <code>index</code> est en dehors des limites permises.
     */
    public Point2D getLocation(int index) throws IndexOutOfBoundsException {
        index *= RECORD_LENGTH;
        if (index<0 || index>=validLength) {
            throw new IndexOutOfBoundsException(String.valueOf(index/RECORD_LENGTH));
        }
        return new Point2D.Double(Math.toDegrees(points[index+0]),
                                  Math.toDegrees(points[index+1]));
    }

    /**
     * M�morise dans le buffer sp�cifi� une des positions visit�es depuis la cr�ation de cet
     * objet.
     *
     * @param  index  Indice de la position d�sir�e.
     * @param  buffer Buffer dans lequel m�moriser les coordonn�es.
     * @param  offset Index � partir d'o� placer la coordonn�e (x,y) dans le buffer.
     * @throws IndexOutOfBoundsException si <code>index</code> est en dehors des limites permises.
     */
    final void getLocation(int index, final float[] buffer, final int offset)
            throws IndexOutOfBoundsException
    {
        index *= RECORD_LENGTH;
        if (index<0 || index>=validLength) {
            throw new IndexOutOfBoundsException(String.valueOf(index/RECORD_LENGTH));
        }
        buffer[offset+0] = (float) Math.toDegrees(points[index+0]);
        buffer[offset+1] = (float) Math.toDegrees(points[index+1]);
    }

    /**
     * Retourne le cap (en degr�s g�ographiques par rapport au nord vrai) apr�s
     * le d�placement correspondant � l'index sp�cifi�.
     *
     * @param  index Indice du cap d�sir�, de 0 inclusivement jusqu'� {@link #getPointCount}
     *         exclusivement. L'indice du pas de temps courant peut �tre obtenu avec {@link
     *         Clock#getStepSequenceNumber}.
     * @return Le cap apr�s le d�placement sp�cifi�.
     * @throws IndexOutOfBoundsException si <code>index</code> est en dehors des limites permises.
     */
    final double getHeading(int index) {
        index *= RECORD_LENGTH;
        if (index<0 || index+1>=validLength) {
            if (index+1 == validLength) {
                return getHeading();
            }
            throw new IndexOutOfBoundsException(String.valueOf(index/RECORD_LENGTH));
        }
        final double dx = points[validLength + 0] - points[validLength - RECORD_LENGTH + 0];
        final double dy = points[validLength + 1] - points[validLength - RECORD_LENGTH + 1];
        return 90 - Math.toDegrees(Math.atan2(dy,dx));
    }

    /**
     * Change de cap en tournant d'un certain angle par rapport
     * au cap actuel.
     *
     * @param angle Angle de rotation en degr�es, dans le sens des
     *        aiguilles d'une montre.
     */
    public void rotate(final double angle) {
        direction -= Math.toRadians(angle);
    }

    /**
     * Avance d'une certaine distance dans la direction actuelle.
     * Cette direction peut �tre obtenue avec {@link #getHeading}.
     *
     * @param distance Distance � avancer, en milles nautiques.
     *        Une valeur n�gative fera reculer l'animal.
     */
    public void moveForward(final double distance) {
        double x = points[validLength-RECORD_LENGTH+0];
        double y = points[validLength-RECORD_LENGTH+1];

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
        setLocationRadians((float)x, (float)y);
    }

    /**
     * Avance d'une certaine distance dans la direction du point sp�cifi�. On peut ne pas
     * atteindre le point si la distance est trop courte. Si la distance est trop longue,
     * alors on s'arr�tera � la position du point sp�cifi�. Le {@linkplain #getHeading cap}
     * sera modifi� de fa�on � correspondre � la direction vers le nouveau point.
     *
     * @param point Point vers lequel avancer, en degr�s de longitude et de latitude.
     *              Un point identique � <code>this</code> ne fera pas bouger l'animal,
     *              quelle que soit la valeur de <code>distance</code>.
     * @param distance Distance maximale � parcourir, en milles nautiques. Une valeur
     *                 n�gative fera fuir l'animal dans la direction oppos�e.
     * @return <code>true</code> si on a atteint le point sp�cifi�, ou
     *         <code>false</code> si on s'est d�plac� sans l'atteindre.
     */
    public boolean moveToward(final Point2D point, final double distance) {
        double x = points[validLength-RECORD_LENGTH+0];
        double y = points[validLength-RECORD_LENGTH+1];

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
        if (!java.lang.Double.isNaN(newDirection)) {
            direction = newDirection;
        }

        // Compute the new position relative to (0,0).
        final double fc = distance/XMath.hypot(px, py);
        if (java.lang.Double.isInfinite(fc) || fc>=1) {
            setLocationRadians((float)Math.toRadians(point.getX()),
                               (float)Math.toRadians(point.getY()));
            return true;
        }
        px *= fc;
        py *= fc;

        // Inverse project the (px,py) point
        // from projected to geographic CS.
        x = px/ak0 + meridian;
        y = (Math.PI/2) - 2*Math.atan(Math.exp((northing-py)/ak0));
        setLocationRadians((float)x, (float)y);
        return false;
    }

    /**
     * Transforme les coordonn�es de la forme sp�cifi�e des milles nautiques vers des coordonn�es
     * g�ographiques. En entr�e, les coordonn�es de <code>shape</code> sont des milles nautiques
     * relatifs � la position actuelle (c'est-�-dire que la position actuelle est d�finie comme
     * �tant l'origine (0,0) du syst�me de coordonn�es de <code>shape</code>). En sortie, les
     * coordonn�es de <code>shape</code> sont des degr�s de longitudes et de latitudes.
     *
     * @param  shape Forme g�om�trique � transformer.
     */
    public void relativeToGeographic(final RectangularShape shape) {
        relativeToGeographic(shape, points[validLength-RECORD_LENGTH+0],
                                    points[validLength-RECORD_LENGTH+1]);
    }

    /**
     * Transforme les coordonn�es de la forme sp�cifi�e des milles nautiques vers des coordonn�es
     * g�ographiques. En entr�e, les coordonn�es de <code>shape</code> sont des milles nautiques
     * relatifs � une position pr�c�dement visit�e (c'est-�-dire que la position visit�e est
     * d�finie comme �tant l'origine (0,0) du syst�me de coordonn�es de <code>shape</code>).
     * En sortie, les coordonn�es de <code>shape</code> sont des degr�s de longitudes et de
     * latitudes.
     *
     * @param  shape Forme g�om�trique � transformer.
     * @param  index Indice de la position d�sir�e, de 0 inclusivement jusqu'�
     *         {@link #getPointCount} exclusivement. L'indice du pas de temps
     *         courant peut �tre obtenu avec {@link Clock#getStepSequenceNumber}.
     * @throws IndexOutOfBoundsException si <code>index</code> est en
     *         dehors des limites permises.
     */
    public void relativeToGeographic(final RectangularShape shape, int index)
            throws IndexOutOfBoundsException
    {
        index *= RECORD_LENGTH;
        if (index<0 || index>=validLength) {
            throw new IndexOutOfBoundsException(String.valueOf(index/RECORD_LENGTH));
        }
        relativeToGeographic(shape, points[index], points[index+1]);
    }

    /**
     * Transforme les coordonn�es de la forme sp�cifi�e des milles nautiques vers des coordonn�es
     * g�ographiques. En entr�e, les coordonn�es de <code>shape</code> sont des milles nautiques
     * relatifs � la position sp�cifi�e (c'est-�-dire que la position sp�cifi�e est d�finie comme
     * �tant l'origine (0,0) du syst�me de coordonn�es de <code>shape</code>). En sortie, les
     * coordonn�es de <code>shape</code> sont des degr�s de longitudes et de latitudes.
     *
     * @param  shape Forme g�om�trique � transformer.
     * @param  x Longitude de l'origine de la forme, en <strong>radians</strong>.
     * @param  y Latitude de l'origine de la forme, en <strong>radians</strong>.
     */
    private static void relativeToGeographic(final RectangularShape shape,
                                             final double x, final double y)
    {
        double xmin = shape.getMinX();
        double xmax = shape.getMaxX();
        double ymin = shape.getMinY();
        double ymax = shape.getMaxY();
        final double[] corners = new double[] {
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
        for (int i=0; i<corners.length; i+=2) {
            corners[i+0] = corners[i]/ak0 + meridian;
            corners[i+1] = (Math.PI/2) - 2*Math.atan(Math.exp((northing-corners[i+1])/ak0));
        }

        // Compute the shape.
        xmin = Math.toDegrees(Math.min(corners[0], corners[6]));
        xmax = Math.toDegrees(Math.max(corners[2], corners[4]));
        ymin = Math.toDegrees(Math.min(corners[1], corners[3]));
        ymax = Math.toDegrees(Math.max(corners[5], corners[7]));
        shape.setFrame(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Retourne une cha�ne de caract�re repr�sentant
     * cet animal. Cette information est utile pour
     * faciliter les d�boguages.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer.append(validLength/RECORD_LENGTH);
        buffer.append(" points");
        if (validLength != 0) {
            buffer.append("; last=");
            buffer.append((float)getX());
            buffer.append(", ");
            buffer.append((float)getX());
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Retourne une copie de ce chemin.
     */
    public Path clone() {
        final Path that = (Path) super.clone();
        /*
         * Les anciennes donn�es ne peuvent pas �tre modifi�es.
         * En cons�quence,  ce n'est pas la peine de copier le
         * tableau sauf s'il reste de la place pour ajouter de
         * nouvelles donn�es. C'est exactement la v�rification
         * que fait 'trimToSize()'.
         */
        that.trimToSize();
        return that;
    }

    /**
     * Retourne un code pour ce chemin. Ce code permet
     * d'utiliser les objets <code>Path</code> dans des
     * ensembles {@link java.util.Set}
     */
    public int hashCode() {
        int code = validLength;
        for (int i=0; i<validLength; i+=7) {
            code = code*37 + java.lang.Float.floatToIntBits(points[i]);
        }
        return code;
    }

    /**
     * V�rifie si ce chemin est identique � l'objet sp�cifi�.
     * Deux chemins sont consid�r�s identiques s'ils ont exactement
     * les m�mes coordonn�es pour tous les points visit�s.
     */
    public boolean equals(final Object other) {
        if (other!=null && other.getClass().equals(getClass())) {
            final Path that = (Path) other;
            if (java.lang.Double.doubleToLongBits(this.direction) ==
                java.lang.Double.doubleToLongBits(that.direction))
            {
                this.trimToSize();
                that.trimToSize();
                return Arrays.equals(this.points, that.points);
            }
        }
        return false;
    }

    /**
     * R�duit la longueur du tableau {@link #points} au minimum n�cessaire.
     * NOTE: La methode {@link #clone} s'attend � ce que cette m�thode
     *       cr�e un nouveau tableau {@link #points} si sa longueur doit
     *       �tre modifi�e.
     */
    private void trimToSize() {
        points = XArray.resize(points, validLength); // Copy wanted if length changes.
    }

    /**
     * R�duit la longueur de la cha�ne {@link #points} avant d'enregistrer cet objet.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        trimToSize();
        out.defaultWriteObject();
    }

    /**
     * Recalcule la valeur de {@link #validLength} apr�s la lecture.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        validLength = points.length;
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
    public boolean contains(Point2D p) {
        return false;
    }

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains the
     * specified <code>Rectangle2D</code>.
     */
    public boolean contains(Rectangle2D r) {
        return false;
    }

    /**
     * Tests if the specified coordinates are inside the boundary of the
     * <code>Shape</code>.
     */
    public boolean contains(double x, double y) {
        return false;
    }

    /**
     * Tests if the interior of the <code>Shape</code> entirely contains
     * the specified rectangular area.
     */
    public boolean contains(double x, double y, double w, double h) {
        return false;
    }

    /**
     * Tests if the interior of the <code>Shape</code> intersects the
     * interior of a specified <code>Rectangle2D</code>.
     */
    public boolean intersects(Rectangle2D r) {
        return getBounds2D().intersects(r);
    }

    /**
     * Tests if the interior of the <code>Shape</code> intersects the
     * interior of a specified rectangular area.
     */
    public boolean intersects(double x, double y, double w, double h) {
        return intersects(new Rectangle2D.Double(x,y,w,h));
    }

    /**
     * Returns an integer {@link Rectangle} that completely encloses the
     * <code>Shape</code>.
     */
    public Rectangle getBounds() {
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
    public Rectangle2D getBounds2D() {
        final double xmin = Math.toDegrees(this.xmin);
        final double ymin = Math.toDegrees(this.ymin);
        final double xmax = Math.toDegrees(this.xmax);
        final double ymax = Math.toDegrees(this.ymax);
        return new XRectangle2D(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Returns an iterator object that iterates along the
     * <code>Shape</code> boundary and provides access to the geometry of the
     * <code>Shape</code> outline.
     */
    public PathIterator getPathIterator(final AffineTransform at) {
        return new Iterator(points, validLength, at);
    }

    /**
     * Returns an iterator object that iterates along the <code>Shape</code>
     * boundary and provides access to a flattened view of the
     * <code>Shape</code> outline geometry.
     */
    public PathIterator getPathIterator(final AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    /**
     * Balaie les points de la trajectoire.
     */
    private static final class Iterator implements PathIterator {
        /**
         * Longueur valide du tableau {@link #points}.
         */
        private final int validLength;

        /**
         * Coordonn�es (<var>x</var>,<var>y</var>) de cet animal le long de
         * sa trajectoire, en <u>radians</u> de longitude et de latitude.
         */
        private final float[] points;

        /**
         * Position actuelle.
         */
        private int pos;

        /**
         * Transformation affine � appliquer.
         */
        private final AffineTransform at;

        /**
         * Construit un nouvel it�rateur.
         */
        private Iterator(final float[] points, final int validLength, final AffineTransform at) {
            this.points      = points;
            this.validLength = validLength;
            this.at          = at;
        }

        /**
         * Returns the winding rule for determining the interior of the path.
         */
        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        /**
         * Tests if the iteration is complete.
         */
        public boolean isDone() {
            return pos >= validLength;
        }

        /**
         * Returns the coordinates and type of the current path segment in
         * the iteration.
         */
        public int currentSegment(final double[] coords) {
            coords[0] = Math.toDegrees(points[pos+0]);
            coords[1] = Math.toDegrees(points[pos+1]);
            if (at != null) {
                at.transform(coords, 0, coords, 0, 1);
            }
            return (pos==0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Returns the coordinates and type of the current path segment in
         * the iteration.
         */
        public int currentSegment(final float[] coords) {
            coords[0] = (float)Math.toDegrees(points[pos+0]);
            coords[1] = (float)Math.toDegrees(points[pos+1]);
            if (at != null) {
                at.transform(coords, 0, coords, 0, 1);
            }
            return (pos==0) ? SEG_MOVETO : SEG_LINETO;
        }

        /**
         * Moves the iterator to the next segment of the path forwards
         * along the primary direction of traversal as long as there are
         * more points in that direction.
         */
        public void next() {
            pos += RECORD_LENGTH;
        }
    }
}
