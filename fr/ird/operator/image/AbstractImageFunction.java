/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.operator.image;

// Image, colors and geometry
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImagingOpException;
import java.util.Arrays;
import java.io.IOException;
import javax.swing.JFrame;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Java Advanced Imaging
import javax.media.jai.ImageFunction;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.Category;
import org.geotools.gc.GridGeometry;
import org.geotools.gc.GridCoverage;
import org.geotools.gui.swing.MapPane;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XMath;

// Seagis
import fr.ird.resources.Utilities;
import fr.ird.resources.experimental.ResourceKeys;
import fr.ird.resources.experimental.Resources;


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
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class AbstractImageFunction implements ImageFunction {
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
     * The color palette.
     */
    private Color[] colors;

    /**
     * The value's unit.
     */
    private Unit unit;

    /**
     * Construct an {@link ImageFunction}
     * for cartesian coordinate systems.
     */
    protected AbstractImageFunction() {
        this(null);
    }

    /**
     * Construct an {@link ImageFunction}.
     *
     * @param cs The coordinate system, or <code>null</code> for a default
     *           cartesien coordinate system..
     */
    protected AbstractImageFunction(final CoordinateSystem cs) {
        this.coordinateSystem = cs;
        try {
            colors = Utilities.getPaletteFactory().getColors("Rainbow");
        } catch (IOException exception) {
            final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
            record.setSourceClassName("AbstractImageFunction");
            record.setSourceMethodName("<init>");
            record.setThrown(exception);
            Logger.getLogger("fr.ird.operator.image").log(record);
            colors = new Color[] {
                Color.BLACK,
                Color.WHITE
            };
        }
    }

    /**
     * Returns the underlying coordinate systems.
     * This coordinate system is usually set at
     * construction time.
     */
    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Retourne les coordonnées géographique couvertes.
     * Les coordonnées sont exprimées selon le système
     * de coordonnées {@link #getCoordinateSystem}.
     *
     * @throws IOException si une lecture des données a été
     *         nécessaire et que cette opération a échouée.
     */
    public synchronized Rectangle2D getGeographicArea() throws IOException {
        if (geographicArea == null) {
            findExtremas();
        }
        return (geographicArea!=null) ? (Rectangle2D) geographicArea.clone() : new Rectangle2D.Double();
    }

    /**
     * Set the geographic area. A <code>null</code> value
     * will reset the default area, which is computed from
     * available data.
     */
    public synchronized void setGeographicArea(final Rectangle2D area) {
        geographicArea = (area!=null) ? (Rectangle2D) area.clone() : null;
    }

    /**
     * Retourne la valeur minimale de la plage de valeurs.
     *
     * @throws IOException si une lecture des données a été
     *         nécessaire et que cette opération a échouée.
     */
    public synchronized double getMinimum() throws IOException {
        if (Double.isNaN(minimum)) {
            findExtremas();
        }
        return minimum;
    }

    /**
     * Set the minimum value range. If this value is greater than
     * the current maximum, then the maximum will also be set to
     * this value.
     */
    public synchronized void setMinimum(final double value) {
        minimum = value;
        if (maximum < value) {
            maximum = value;
        }
    }

    /**
     * Retourne la valeur maximale de la plage de valeurs.
     *
     * @throws IOException si une lecture des données a été
     *         nécessaire et que cette opération a échouée.
     */
    public synchronized double getMaximum() throws IOException {
        if (Double.isNaN(maximum)) {
            findExtremas();
        }
        return maximum;
    }

    /**
     * Set the maximum value range. If this value is less than
     * the current minimum, then the minimum will also be set
     * to this value.
     */
    public synchronized void setMaximum(final double value) {
        maximum = value;
        if (minimum > value) {
            minimum = value;
        }
    }

    /**
     * Returns the color palette to use.
     *
     * @throws IOException If this information can't
     *         be fetch from the datafile.
     */
    public synchronized Color[] getColorPalette() throws IOException {
        return (Color[]) colors.clone();
    }

    /**
     * Set the color palette by name. The name must be one of valid
     * color palettes in the <code>"application-data/colors"</code>
     * directory.
     *
     * @throws IOException if the color palette can't be read.
     */
    public synchronized void setColorPalette(final String palette) throws IOException {
        final Color[] candidate = Utilities.getPaletteFactory().getColors(palette);
        if (candidate == null) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NO_PARAMETER_$1, palette));
        }
        colors = candidate;
    }

    /**
     * Returns the value's unit, or <code>null</code> if none.
     *
     * @throws IOException If this information can't
     *         be fetch from the datafile.
     */
    public Unit getUnit() throws IOException {
        return unit;
    }

    /**
     * Set the value's unit.
     */
    public void setUnit(final Unit unit) {
        this.unit = unit;
    }

    /**
     * Positionne le curseur au début du flot de données. Lorsque <code>ImageFunction</code>
     * a besoin de connaître les données qui constituent une image, il va d'abord appeller
     * <code>rewind()</code>, puis fera une série d'appel à {@link #next}.
     *
     * <p>Cette méthode n'a pas besoin d'être synchronisée. Il est garantit
     * qu'il n'y aura pas deux lectures simultanées des données.</p>
     *
     * @throws IOException si cette opération a nécessité une opération d'entrés/sorties
     *         (par exemple l'ouverture d'un fichier) et que cette opération a échoué.
     *
     * @see #next
     * @see #getWeight
     */
    protected abstract void rewind() throws IOException;

    /**
     * Retourne les coordonnées (<var>x</var>,<var>y</var>,<var>z</var>) de la donnée
     * courante, puis passe au point suivant. Cette méthode doit retourner un tableau
     * d'une longueur d'au moins 3. Ce tableau doit contenir dans l'ordre une longitude
     * (<var>x</var>), une latitude  (<var>y</var>)  et une valeur  (<var>z</var>)  aux
     * coordonnées (<var>x</var>,<var>y</var>). Une ou plusieurs de ces valeurs peuvent
     * être {@link Double#NaN} si elle n'est pas disponible. S'il ne reste plus de point
     * à balayer, alors cette méthode retourne <code>null</code>.
     * Cette méthode est typiquement appelée dans le contexte suivant:
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
     * <p>Cette méthode n'a pas besoin d'être synchronisée. Il est garantit
     * qu'il n'y aura pas deux lectures simultanées des données.</p>
     *
     * @param  data Tableau dans lequel écrire le résultat de cette méthode, ou
     *         <code>null</code> si cette méthode doit créer un nouveau tableau.
     * @return Les coordonnées (<var>x</var>,<var>y</var>,<var>z</var>) dans un tableau
     *         d'une longueur d'au moins 3,  ou <code>null</code> s'il ne reste plus de
     *         points à balayer.
     * @throws IOException si cette opération a nécessité une opération d'entrés/sorties
     *         (par exemple la lecture d'un fichier) et que cette opération a échoué.
     *
     * @see #rewind
     * @see #getWeight
     */
    protected abstract double[] next(final double[] array) throws IOException;

    /**
     * Retourne le poid à donner à une valeur en fonction de sa
     * distance. L'implémentation par défaut retourne toujours 1.
     * Les classes dérivées peuvent redéfinir cette méthode pour
     * donner un poid qui varie selon la distance, en utilisant
     * typiquement une courbe normale comme suit:
     *
     * <center> C0 * Math.exp(-C1 * distance)  </center>
     *
     * Cette méthode n'a pas besoin d'être synchronisée. Il est garantit
     * qu'il n'y aura pas deux lectures simultanées des données.
     *
     * @param  distance Distance entre la position d'une valeur et
     *         le centre du pixel, en mètres.  Cette distance sera
     *         précise à la condition qu'un système de coordonnées
     *         approprié  ait été spécifié lors de la construction
     *         de cet objet.
     * @return Un poid à donner aux mesures qui se trouvent à cette
     *         distance. Il peut s'agir de n'importe quel nombre réel
     *         positif (pas nécessairement entre 0 et 1).
     *
     * @see #rewind
     * @see #next
     */
    protected double getWeight(final double distance) {
        return 1;
    }

    /**
     * Returns whether or not each value's elements are complex.
     * Default implementation returns <code>false</code>.
     */
    public boolean isComplex() {
        return false;
    }

    /**
     * Returns the number of elements per value at each position.
     * Default implementation returns 1.
     */
    public int getNumElements() {
        return 1;
    }

    /**
     * Trouve les valeurs de {@link #geographicArea} et les minimum et maximum.
     * Cette méthode doit obligatoirement être appelée à partir d'une méthode
     * synchronisée.
     *
     * @throws IOException si une lecture des données a été
     *         nécessaire et que cette opération a échouée.
     */
    private void findExtremas() throws IOException {
        assert Thread.holdsLock(this);
        double xmin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        double zmin = Double.POSITIVE_INFINITY;
        double zmax = Double.NEGATIVE_INFINITY;
        rewind();
        double[] data = null;
        while ((data=next(data)) != null) {
            final double x = data[0];
            final double y = data[1];
            final double z = data[2];
            if (x<xmin) xmin=x;
            if (x>xmax) xmax=x;
            if (y<ymin) ymin=y;
            if (y>ymax) ymax=y;
            if (z<zmin) zmin=z;
            if (z>zmax) zmax=z;
        }
        // Update internal fields only if they were not set.
        // It allows to preserve user-defined settings.
        if (xmin<=xmax && ymin<=ymax) {
            if (geographicArea == null) {
                geographicArea = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
            }
        }
        if (zmin <= zmax) {
            if (Double.isNaN(minimum)) minimum=zmin;
            if (Double.isNaN(maximum)) maximum=zmax;
            if (!(minimum < maximum)) { // Le '!' prend en compte NaN.
                minimum = zmin;
                maximum = zmax;
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
                            final float[] real, final float[] imag)
    {
        final int length = countX*countY;
        final double[] sumValues = new double[length];
        final double[] sumWeight = new double[length];
        compute(startX, startY, deltaX, deltaY, countX, countY, sumValues, sumWeight);
        for (int i=0; i<length; i++) {
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
        for (int i=0; i<length; i++) {
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
        final int length = countX*countY;
        final Ellipsoid ellipsoid = CTSUtilities.getEllipsoid(coordinateSystem);
        try {
            rewind();
            double[] data = null;
            while ((data=next(data)) != null) {
                final double z = data[2];
                if (!Double.isNaN(z)) {
                    double x = data[0];
                    double y = data[1];
                    if (ellipsoid != null) {
                        // Correction for out-of-range longitude:
                        // keep longitude in range [startX ... startX+360]
                        x = ((x - startX) % 360) + startX;
                    }
                    // Compute the pixel location (px,py) for the measurement at (x,y).
                    // Location (px,py) is the pixel's upper left corner (not the center).
                    final int px = (int)Math.floor((x-startX)/deltaX);
                    final int py = (int)Math.floor((y-startY)/deltaY);
                    if (px>=0 && py>=0 && px<countX && py<countY) {
                        final double xr = startX + (px+0.5)*deltaX;
                        final double yr = startY + (py+0.5)*deltaY;
                        final double distance;
                        if (ellipsoid != null) {
                            distance = ellipsoid.orthodromicDistance(x, y, xr, yr);
                        } else {
                            distance = XMath.hypot(x-xr, y-yr);
                        }
                        final double weight = getWeight(distance);
                        if (!Double.isNaN(weight)) {
                            final int index   = px + py*countX;
                            sumValues[index] += weight*z;
                            sumWeight[index] += weight;
                        }
                    }
                }
            }
        } catch (IOException exception) {
            final ImagingOpException e = new ImagingOpException("Can't compute image");
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Returns a grid coverage for this image function.
     *
     * @param  name  The grid coverage name.
     * @param  width The desired image width.
     * @param  width The desired image height.
     * @return The grid coverage.
     * @throws IOException if an error occured while reading file.
     */
    public synchronized GridCoverage getGridCoverage(final String name, final int width, final int height) throws IOException {
        final double          minimum = getMinimum();
        final double          maximum = getMaximum();
        final double            scale = (maximum-minimum)/255;
        final Rectangle2D coordBounds = getGeographicArea();
        final Rectangle   pixelBounds = new Rectangle(0, 0, width, height);
        final GridGeometry   geometry = new GridGeometry(pixelBounds, coordBounds);
        final SampleDimension    band = new SampleDimension(new Category[] {
            new Category("Donnée manquante", Color.BLACK, 0),
            new Category(name, getColorPalette(), 1, 256, scale, minimum-scale)
        }, getUnit()).geophysics(true);
        return new GridCoverage(name, this, getCoordinateSystem(), geometry,
                                new SampleDimension[] {band}, null);
    }

    /**
     * Convenience method displaying this image function.
     *
     * @param  name  The grid coverage name.
     * @param  width The desired image width.
     * @param  width The desired image height.
     * @return The grid coverage.
     * @throws IOException if an error occured while reading file.
     */
    public GridCoverage show(final String name, final int width, final int height) throws IOException {
        final GridCoverage coverage = getGridCoverage(name, width, height);
        final MapPane map = new MapPane(getCoordinateSystem());
        map.getRenderer().addLayer(new RenderedGridCoverage(coverage));
        map.setPreferredSize(new Dimension(width, height));
        final JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(map);
        frame.pack();
        frame.show();
        return coverage;
    }

    /**
     * Free all resources used by this <code>ImageFunction</code>.
     * Trying to use this object after <code>dispose()</code> may
     * fail.
     *
     * @throws IOException If an I/O operation was required and failed.
     */
    public synchronized void dispose() throws IOException {
        unit           = null;
        colors         = null;
        geographicArea = null;
        minimum = maximum = Double.NaN;
    }
}
