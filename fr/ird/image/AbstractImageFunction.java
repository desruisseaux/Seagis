/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.image;

// Image, colors and geometry
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.media.jai.ImageFunction;
import java.awt.image.ImagingOpException;

// Miscellaneous
import java.util.Arrays;
import java.io.IOException;

// Seagis dependencies
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.HorizontalDatum;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cv.Category;
import net.seagis.cv.CategoryList;
import net.seagis.gc.GridGeometry;
import net.seagis.gc.GridCoverage;
import net.seagis.resources.XMath;
import net.seagis.resources.OpenGIS;


/**
 * An {@link ImageFunction} that construct image from a set
 * of non-gridded data. Subclasses must define the following
 * methods:
 *
 * <ul>
 *   <li>{@link #rewind} (mandatory)</li>
 *   <li>{@link #next} (mandatory)</li>
 *   <li>{@link #getWeight} (optional)</li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class AbstractImageFunction implements ImageFunction
{
    /**
     * The coordinate system, or <code>null</code> for a default
     * cartesien coordinate system.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * Geographic envelope for all data, or <code>null</code>
     * if it has not been computed yet.
     */
    private Rectangle2D geographicArea;

    /**
     * Minimal and maximal values, or {@link Double#NaN}
     * if it has not been computed yet.
     */
    private double minimum=Double.NaN, maximum=Double.NaN;

    /**
     * Construct an {@link ImageFunction}
     * for cartesian coordinate systems.
     */
    protected AbstractImageFunction()
    {this(null);}

    /**
     * Construct an {@link ImageFunction}.
     *
     * @param cs The coordinate system, or <code>null</code> for a default
     *           cartesien coordinate system..
     */
    protected AbstractImageFunction(final CoordinateSystem cs)
    {this.coordinateSystem = cs;}

    /**
     * Positionne le curseur au d�but du flot de donn�es. Lorsque <code>ImageFunction</code>
     * a besoin de conna�tre les donn�es qui constituent une image, il va d'abord appeller
     * <code>rewind()</code>, puis fera une s�rie d'appel � {@link #next}.
     *
     * <p>Cette m�thode n'a pas besoin d'�tre synchronis�e. Il est garantit
     * qu'il n'y aura pas deux lectures simultan�es des donn�es.</p>
     *
     * @throws IOException si cette op�ration a n�cessit� une op�ration d'entr�s/sorties
     *         (par exemple l'ouverture d'un fichier) et que cette op�ration a �chou�.
     *
     * @see #next
     * @see #getWeight
     */
    protected abstract void rewind() throws IOException;

    /**
     * Retourne les coordonn�es (<var>x</var>,<var>y</var>,<var>z</var>) de la donn�e
     * courante, puis passe au point suivant. Cette m�thode doit retourner un tableau
     * d'une longueur d'au moins 3. Ce tableau doit contenir dans l'ordre une longitude
     * (<var>x</var>), une latitude  (<var>y</var>)  et une valeur  (<var>z</var>)  aux
     * coordonn�es (<var>x</var>,<var>y</var>). Une ou plusieurs de ces valeurs peuvent
     * �tre {@link Double#NaN} si elle n'est pas disponible. S'il ne reste plus de point
     * � balayer, alors cette m�thode retourne <code>null</code>.
     * Cette m�thode est typiquement appel�e dans le contexte suivant:
     *
     * <blockquote><pre>
     * synchronized (this)
     * {
     *     {@link #rewind()};
     *     double[] data = null;
     *     while ((data={@link #next next}(data)) != null)
     *     {
     *         final double x = data[0];
     *         final double y = data[1];
     *         final double z = data[2];
     *         // ... do some process
     *     }
     * }
     * </pre></blockquote>
     *
     * <p>Cette m�thode n'a pas besoin d'�tre synchronis�e. Il est garantit
     * qu'il n'y aura pas deux lectures simultan�es des donn�es.</p>
     *
     * @param  data Tableau dans lequel �crire le r�sultat de cette m�thode, ou
     *         <code>null</code> si cette m�thode doit cr�er un nouveau tableau.
     * @return Les coordonn�es (<var>x</var>,<var>y</var>,<var>z</var>) dans un tableau
     *         d'une longueur d'au moins 3,  ou <code>null</code> s'il ne reste plus de
     *         points � balayer.
     * @throws IOException si cette op�ration a n�cessit� une op�ration d'entr�s/sorties
     *         (par exemple la lecture d'un fichier) et que cette op�ration a �chou�.
     *
     * @see #rewind
     * @see #getWeight
     */
    protected abstract double[] next(final double[] array) throws IOException;

    /**
     * Retourne le poid � donner � une valeur en fonction de sa
     * distance. L'impl�mentation par d�faut retourne toujours 1.
     * Les classes d�riv�es peuvent red�finir cette m�thode pour
     * donner un poid qui varie selon la distance, en utilisant
     * typiquement une courbe normale comme suit:
     *
     * <center> C0 * Math.exp(-C1 * distance)  </center>
     *
     * Cette m�thode n'a pas besoin d'�tre synchronis�e. Il est garantit
     * qu'il n'y aura pas deux lectures simultan�es des donn�es.
     *
     * @param  distance Distance entre la position d'une valeur et
     *         le centre du pixel, en m�tres.  Cette distance sera
     *         pr�cise � la condition qu'un syst�me de coordonn�es
     *         appropri�  ait �t� sp�cifi� lors de la construction
     *         de cet objet.
     * @return Un poid � donner aux mesures qui se trouvent � cette
     *         distance. Il peut s'agir de n'importe quel nombre r�el
     *         positif (pas n�cessairement entre 0 et 1).
     *
     * @see #rewind
     * @see #next
     */
    protected double getWeight(final double distance)
    {return 1;}

    /**
     * Returns whether or not each value's elements are complex.
     * Default implementation returns <code>false</code>.
     */
    public boolean isComplex()
    {return false;}

    /**
     * Returns the number of elements per value at each position.
     * Default implementation returns 1.
     */
    public int getNumElements()
    {return 1;}

    /**
     * Retourne la valeur minimale de la plage de valeurs.
     *
     * @throws IOException si une lecture des donn�es a �t�
     *         n�cessaire et que cette op�ration a �chou�e.
     */
    public synchronized double getMinimum() throws IOException
    {
        ensureValid();
        return minimum;
    }

    /**
     * Retourne la valeur maximale de la plage de valeurs.
     *
     * @throws IOException si une lecture des donn�es a �t�
     *         n�cessaire et que cette op�ration a �chou�e.
     */
    public synchronized double getMaximum() throws IOException
    {
        ensureValid();
        return maximum;
    }

    /**
     * Retourne les coordonn�es g�ographique couvertes.
     * Les coordonn�es sont exprim�es selon le syst�me
     * de coordonn�es {@link #getCoordinateSystem}.
     *
     * @throws IOException si une lecture des donn�es a �t�
     *         n�cessaire et que cette op�ration a �chou�e.
     */
    public synchronized Rectangle2D getGeographicArea() throws IOException
    {
        ensureValid();
        return (geographicArea!=null) ? (Rectangle2D) geographicArea.clone() : new Rectangle2D.Double();
    }

    /**
     * Returns the underlying coordinate systems.
     * This coordinate system is usually set at
     * construction time.
     */
    public CoordinateSystem getCoordinateSystem()
    {
        return coordinateSystem;
    }

    /**
     * Calcule automatiquement les valeurs de {@link #geographicArea} et les
     * minimum et maximum, si ces valeurs n'avaient pas d�j� �t� calcul�es.
     * Cette m�thode doit obligatoirement �tre appel�e � partir d'une
     * m�thode <code>synchronized</code>.
     *
     * @throws IOException si une lecture des donn�es a �t�
     *         n�cessaire et que cette op�ration a �chou�e.
     */
    private void ensureValid() throws IOException
    {
        assert Thread.holdsLock(this);
        Rectangle2D            area = geographicArea;
        final boolean minimum_isNaN = Double.isNaN(minimum);
        final boolean maximum_isNaN = Double.isNaN(maximum);
        if (area==null || minimum_isNaN || maximum_isNaN)
        {
            rewind();
            double[] data = null;
            double xmin = Double.POSITIVE_INFINITY;
            double xmax = Double.NEGATIVE_INFINITY;
            double ymin = Double.POSITIVE_INFINITY;
            double ymax = Double.NEGATIVE_INFINITY;
            double zmin = Double.POSITIVE_INFINITY;
            double zmax = Double.NEGATIVE_INFINITY;
            while ((data=next(data)) != null)
            {
                final double x = data[0];
                final double y = data[1];
                final double z = data[2];
                if (x<xmin) xmin=x;
                if (x>xmax) xmax=x;
                if (y<ymin) ymin=y;
                if (y>ymax) ymax=y;
                if (z>zmax) zmax=z;
                if (z<zmin) zmin=z;
            }
            if (xmin<=xmax && ymin<=ymax)
            {
                if (area==null) geographicArea = area = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
            }
            if (zmin<=zmax)
            {
                if (minimum_isNaN) minimum=zmin;
                if (maximum_isNaN) maximum=zmax;
                if (!(minimum < maximum)) // Le '!' prend en compte NaN.
                {
                    minimum = zmin;
                    maximum = zmax;
                }
            }
        }
    }

    /**
     * Returns all values of a given element for a specified set of coordinates.
     *
     * @param startX The X coordinate of the upper left location to evaluate.
     * @param startY The Y coordinate of the upper left location to evaluate.
     * @param deltaX The horizontal increment.
     * @param deltaY The vertical increment.
     * @param countX The number of points in the horizontal direction.
     * @param countY The number of points in the vertical direction.
     * @param element The element to fetch.
     * @param real A pre-allocated float array of length at least <code>countX*countY</code>
     *             in which the real parts of all elements will be returned.
     * @param imag A pre-allocated float array of length at least <code>countX*countY</code>
     *             in which the imaginary parts of all elements will be returned; may be null
     *             for real data, i.e., when {@link #isComplex()} returns <code>false</code>.
     *
     * @throws ArrayIndexOutOfBoundsException If the length of the supplied array(s) is insufficient.
     * @throws ImagingOpException If the operation failed because of an I/O error.
     */
    public void getElements(final float startX, final float startY,
                            final float deltaX, final float deltaY,
                            final int   countX, final int    countY, final int element,
                            final float[] real, final float[]  imag)
    {
        final int length = countX*countY;
        final double[] sumValues = new double[length];
        final double[] sumWeight = new double[length];
        compute(startX, startY, deltaX, deltaY, countX, countY, sumValues, sumWeight);
        for (int i=0; i<length; i++)
        {
            real[i] = (float) (sumValues[i] / sumWeight[i]);
        }
    }

    /**
     * Returns all values of a given element for a specified set of coordinates.
     *
     * @param startX The X coordinate of the upper left location to evaluate.
     * @param startY The Y coordinate of the upper left location to evaluate.
     * @param deltaX The horizontal increment.
     * @param deltaY The vertical increment.
     * @param countX The number of points in the horizontal direction.
     * @param countY The number of points in the vertical direction.
     * @param element The element to fetch.
     * @param real A pre-allocated double array of length at least <code>countX*countY</code>
     *             in which the real parts of all elements will be returned.
     * @param imag A pre-allocated double array of length at least <code>countX*countY</code>
     *             in which the imaginary parts of all elements will be returned; may be null
     *             for real data, i.e., when {@link #isComplex()} returns <code>false</code>.
     *
     * @throws ArrayIndexOutOfBoundsException If the length of the supplied array(s) is insufficient.
     * @throws ImagingOpException If the operation failed because of an I/O error.
     */
    public void getElements(final double startX, final double startY,
                            final double deltaX, final double deltaY,
                            final int    countX, final int    countY, final int element,
                            final double[] real, final double[] imag)
    {
        final int length = countX*countY;
        Arrays.fill(real, 0, length, 0);
        final double[] sumWeight = new double[length];
        compute(startX, startY, deltaX, deltaY, countX, countY, real, sumWeight);
        for (int i=0; i<length; i++)
        {
            real[i] /= sumWeight[i];
        }
    }

    /**
     * Compute the elements.
     *
     * @param startX The X coordinate of the upper left location to evaluate.
     * @param startY The Y coordinate of the upper left location to evaluate.
     * @param deltaX The horizontal increment.
     * @param deltaY The vertical increment.
     * @param countX The number of points in the horizontal direction.
     * @param countY The number of points in the vertical direction.
     * @param sumValues A pre-allocated array of length at least <code>countX*countY</code>
     *                  in which the sum of values of all elements will be returned.
     * @param sumWeight A pre-allocated double array of length at least <code>countX*countY</code>
     *                  in which the sum of weights of all elements will be returned.
     *
     * @throws ArrayIndexOutOfBoundsException If the length of the supplied array(s) is insufficient.
     * @throws ImagingOpException If the operation failed because of an I/O error.
     */
    private synchronized void compute(final double startX, final double startY,
                                      final double deltaX, final double deltaY,
                                      final int    countX, final int    countY,
                                      final double[] sumValues, final double[] sumWeight) throws ImagingOpException
    {
        final HorizontalDatum datum = OpenGIS.getHorizontalDatum(coordinateSystem);
        final Ellipsoid ellipsoid = (datum!=null) ? datum.getEllipsoid() : null;

        final int    length = countX*countY;
        final double scaleX = countX / (deltaX*countX);
        final double scaleY = countY / (deltaY*countY);
        try
        {
            rewind();
            double[] data = null;
            while ((data=next(data)) != null)
            {
                final double value=data[2];
                if (!Double.isNaN(value))
                {
                    double x1 = data[0];
                    double y1 = data[1];
                    if (ellipsoid != null) // Correction for out-of-range longitude
                    {
                        x1 = ((x1 - startX) % 360) + startX;
                    }
                    final double fx = Math.floor((x1-startX)*scaleX);
                    final double fy = Math.floor((y1-startY)*scaleY);
                    final int     x = (int) fx;
                    final int     y = (int) fy;
                    if (x>=0 && y>=0 && x<countX && y<countY)
                    {
                        final double x2 = startX+(fx+0.5)/scaleX;
                        final double y2 = startY+(fy+0.5)/scaleY;
                        final double distance;
                        if (ellipsoid != null)
                        {
                            distance = ellipsoid.orthodromicDistance(x1, y1, x2, y2);
                        }
                        else
                        {
                            distance = XMath.hypot(x1-x2, y1-y2);
                        }
                        final double weight = getWeight(distance);
                        if (!Double.isNaN(weight))
                        {
                            final int  index  = x + y*countX;
                            sumValues[index] += weight*value;
                            sumWeight[index] += weight;
                        }
                    }
                }
            }
        }
        catch (IOException exception)
        {
            final ImagingOpException e = new ImagingOpException("Can't compute image");
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Returns a grid coverage for this image function.
     * A default color palette is used.
     *
     * @param  name  The grid coverage name.
     * @param  width The desired image width.
     * @param  width The desired image height.
     * @return The grid coverage.
     * @throws IOException if an error occured while reading file.
     */
    public synchronized GridCoverage getGridCoverage(final String name, final int width, final int height) throws IOException
    {
        final double          minimum = getMinimum();
        final double          maximum = getMaximum();
        final double            scale = (maximum-minimum)/255;
        final Rectangle2D coordBounds = getGeographicArea();
        final Rectangle   pixelBounds = new Rectangle(0, 0, width, height);
        final GridGeometry   geometry = new GridGeometry(pixelBounds, coordBounds);
        final CategoryList categories = new CategoryList(new Category[]
        {
            new Category("Donn�e manquante", Color.black, 0),
            new Category("Valeur", Utilities.getPaletteFactory().getColors("Rainbow"), 1, 256, minimum-scale, scale)
        });
        return new GridCoverage(name, this, getCoordinateSystem(), geometry,
                                new CategoryList[] {categories}, null);
    }

    /**
     * Free all resources used by this <code>ImageFunction</code>.
     * Trying to use this object after <code>dispose()</code> may
     * fail.
     *
     * @throws IOException If an I/O operation was required and failed.
     */
    public synchronized void dispose() throws IOException
    {
        geographicArea = null;
        minimum = maximum = Double.NaN;
    }
}
