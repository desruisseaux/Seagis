/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database;

// Géométrie et image
import java.awt.geom.Point2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

// Divers
import java.util.Date;
import javax.media.jai.util.Range;

// OpenGIS
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;
import org.opengis.coverage.CannotEvaluateException;

// Geotools (CTS)
import org.geotools.pt.Envelope;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransformFactory;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.resources.CTSUtilities;

// Geotools (GCS)
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.PointOutsideCoverageException;

// Geotools (resources)
import org.geotools.resources.geometry.XRectangle2D;

// Seagis
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Classe de base des données à trois dimensions spatio-temporelles. Les méthodes <code>evaluate</code>
 * de cette classes sont fournies en deux versions: soit la version habituelle qui prend une
 * {@linkplain CoordinatePoint coordonnée complète} selon le système de coordonnées de cette
 * couverture, ou soit une version qui prend une {@linkplain Point2D position spatiale} ainsi
 * ainsi qu'une {@linkplain Date date}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Coverage3D extends Coverage {
    /**
     * The temporal coordinate system.
     */
    protected final TemporalCoordinateSystem temporalCS;

    /**
     * The dimension of the temporal coordinate system.
     */
    protected final int temporalDimension;

    /**
     * Construit une couverture utilisant le système de coordonnées spécifié.
     *
     * @param  name Le nom de cette couverture.
     * @param  cs Le système de coordonnées à utiliser pour cet obet {@link Coverage}.
     *         Ce système de coordonnées doit obligatoirement comprendre un axe temporel.
     * @throws IllegalArgumentException si le système de coordonnées spécifié ne comprend
     *         pas un axe temporel.
     */
    public Coverage3D(final String name, final CoordinateSystem cs) throws IllegalArgumentException {
        super(name, cs, null, null);
        temporalCS = CTSUtilities.getTemporalCS(cs);
        if (temporalCS == null) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_COORDINATE_SYSTEM));
        }
        temporalDimension = CTSUtilities.getDimensionOf(cs, temporalCS.getClass());
        assert temporalDimension >= 0 : temporalDimension;
    }

    /**
     * Construit une couverture utilisant les même paramètres que la couverture spécifiée.
     */
    protected Coverage3D(final Coverage3D source) {
        super(source);
        temporalCS = source.temporalCS;
        temporalDimension = source.temporalDimension;
    }

    /**
     * Retourne un rectangle englobant les coordonnées géographiques de toutes les données
     * disponibles. Les coordonnées seront toujours exprimées en degrés de longitude et de
     * latitudes, selon le système de coordonnées {@linkplain GeographicCoordinateSystem#WGS84
     * WGS84}.
     */
    public Rectangle2D getGeographicArea() {
        try {
            return getGeographicArea(getEnvelope());
        } catch (TransformException exception) {
            return XRectangle2D.INFINITY;
        }
    }

    /**
     * Retourne un rectangle englobant les coordonnées géographiques de l'enveloppe spécifiée.
     * Les coordonnées seront toujours exprimées en degrés de longitude et de latitudes, selon
     * le système de coordonnées {@linkplain GeographicCoordinateSystem#WGS84 WGS84}.
     *
     * @param  envelope L'enveloppe dont on veut les coordonnées géographiques.
     * @return Les coordonnées géographiques de l'enveloppe spécifiée.
     * @throws TransformException si une transformation était nécessaire et a échouée.
     */
    protected final Rectangle2D getGeographicArea(Envelope envelope) throws TransformException {
        envelope = envelope.getReducedEnvelope(temporalDimension, temporalDimension+1);
        Rectangle2D geographicArea = envelope.toRectangle2D();
        final CoordinateSystem sourceCS = CTSUtilities.getHorizontalCS(coordinateSystem);
        final CoordinateSystem targetCS = GeographicCoordinateSystem.WGS84;
        if (!targetCS.equals(sourceCS, false)) {
            final CoordinateTransformation      transform;
            final CoordinateTransformationFactory factory;
            factory   = CoordinateTransformationFactory.getDefault();
            transform = factory.createFromCoordinateSystems(sourceCS, targetCS);
            geographicArea = CTSUtilities.transform((MathTransform2D)transform.getMathTransform(),
                                                    geographicArea, geographicArea);
        }
        return geographicArea;
    }

    /**
     * Retourne la plage de temps englobant toutes les données disponibles.
     * La plage contiendra des objets {@link Date}.
     */
    public Range getTimeRange() {
        return getTimeRange(getEnvelope());
    }

    /**
     * Retourne la plage de temps de l'enveloppe spécifiée.
     * La plage contiendra des objets {@link Date}.
     *
     * @param  envelope L'enveloppe dont on veut la plage de temps.
     * @return La plage de temps de l'enveloppe spécifiée.
     */
    protected final Range getTimeRange(final Envelope envelope) {
        return new Range(Date.class, temporalCS.toDate(envelope.getMinimum(temporalDimension)),
                                     temporalCS.toDate(envelope.getMaximum(temporalDimension)));
    }

    /**
     * Returns a coordinate point for the given spatial position and date.
     *
     * @param  point The spatial position.
     * @param  date  The date.
     * @return The coordinate point.
     */
    protected final CoordinatePoint getCoordinatePoint(final Point2D point, final Date date) {
        CoordinatePoint coordinate = new CoordinatePoint(coordinateSystem.getDimension());
        coordinate.ord[temporalDimension!=0 ? 0 : 1] = point.getX();
        coordinate.ord[temporalDimension>=2 ? 1 : 2] = point.getY();
        coordinate.ord[temporalDimension] = temporalCS.toValue(date);
        return coordinate;
    }

    /**
     * Returns a sequence of integer values for a given point in the coverage. A value for each
     * sample dimension is included in the sequence. The default implementation delegates the
     * work to the {@linkplain #evaluate(Point2D,Date,double[]) double version}.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public int[] evaluate(final Point2D point, final Date time, int[] dest)
            throws CannotEvaluateException
    {
        double[] buffer = null;
        buffer = evaluate(point, time, buffer);
        if (dest == null) {
            dest = new int[buffer.length];
        }
        for (int i=0; i<buffer.length; i++) {
            dest[i] = (int) Math.round(buffer[i]);
        }
        return dest;
    }

    /**
     * Returns a sequence of float values for a given point in the coverage. A value for each
     * sample dimension is included in the sequence. The default implementation delegates the
     * work to the {@linkplain #evaluate(Point2D,Date,double[]) double version}.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public float[] evaluate(final Point2D point, final Date time, float[] dest)
            throws CannotEvaluateException
    {
        double[] buffer = null;
        buffer = evaluate(point, time, buffer);
        if (dest == null) {
            dest = new float[buffer.length];
        }
        for (int i=0; i<buffer.length; i++) {
            dest[i] = (float) buffer[i];
        }
        return dest;
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public abstract double[] evaluate(final Point2D point, final Date time, double[] dest)
            throws CannotEvaluateException;

    /**
     * Returns a sequence of integer values for a given point in the coverage.  A value for
     * each sample dimension is included in the sequence.  The default implementation split
     * the coordinate point into a {@linkplain Point2D spatial position} with a {@linkplain
     * Date date} and delegates the work to the {@link #evaluate(Point2D,Date,int[]) evaluate}
     * method.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public int[] evaluate(final CoordinatePoint coord, int[] dest) throws CannotEvaluateException {
        return evaluate(checkDimension(coord), temporalCS.toDate(coord.ord[temporalDimension]), dest);
    }

    /**
     * Returns a sequence of float values for a given point in the coverage. A value for
     * each sample dimension is included in the sequence. The default implementation split
     * the coordinate point into a {@linkplain Point2D spatial position} with a {@linkplain
     * Date date} and delegates the work to the {@link #evaluate(Point2D,Date,float[]) evaluate}
     * method.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public float[] evaluate(final CoordinatePoint coord, float[] dest) throws CannotEvaluateException {
        return evaluate(checkDimension(coord), temporalCS.toDate(coord.ord[temporalDimension]), dest);
    }

    /**
     * Returns a sequence of double values for a given point in the coverage. A value for
     * each sample dimension is included in the sequence. The default implementation split
     * the coordinate point into a {@linkplain Point2D spatial position} with a {@linkplain
     * Date date} and delegates the work to the {@link #evaluate(Point2D,Date,double[]) evaluate}
     * method.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to create a new array.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public double[] evaluate(final CoordinatePoint coord, final double[] dest) throws CannotEvaluateException {
        return evaluate(checkDimension(coord), temporalCS.toDate(coord.ord[temporalDimension]), dest);
    }

    /**
     * Vérifie que le point spécifié a bien la dimension attendue.
     *
     * @param  coord Coordonnée du point dont on veut vérifier la dimension.
     * @throws MismatchedDimensionException si le point n'a pas la dimension attendue.
     */
    private final Point2D checkDimension(final CoordinatePoint coord) throws MismatchedDimensionException {
        if (coord.getDimension() != coordinateSystem.getDimension()) {
            // TODO: provides a message.
            throw new MismatchedDimensionException(/*coord, coordinateSystem*/);
        }
        return new Point2D.Double(coord.ord[temporalDimension!=0 ? 0 : 1],
                                  coord.ord[temporalDimension>=2 ? 1 : 2]);
    }

    /**
     * Returns a 2 dimensional grid coverage for the given date. The grid geometry will be computed
     * in order to produces image with the {@linkplain #getDefaultPixelSize() default pixel size},
     * if any.
     *
     * @param  time The date where to evaluate.
     * @return The grid coverage at the specified time, or <code>null</code>
     *         if the requested date fall in a hole in the data.
     * @throws PointOutsideCoverageException if <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     *
     * @see #getRenderableImage(Date)
     * @see RenderableImage#createDefaultRendering()
     */
    public GridCoverage getGridCoverage2D(final Date time) throws CannotEvaluateException {
        final String              name = getName(null);
        final SampleDimension[]  bands = getSampleDimensions();
        final CoordinateSystem      cs = CTSUtilities.getSubCoordinateSystem(coordinateSystem, 0,2);
        final RenderedImage      image = getRenderableImage(time).createDefaultRendering();
        final MathTransform2D gridToCS = MathTransformFactory.getDefault().createAffineTransform(
                                   (AffineTransform) image.getProperty("gridToCoordinateSystem"));
        return new GridCoverage(name, image, cs, gridToCS, bands, null, null);
    }

    /**
     * Returns 2D view of this grid coverage as the given date. For images produced by the
     * {@linkplain RenderableImage#createDefaultRendering() default rendering}, the size
     * will be computed from the {@linkplain #getDefaultPixelSize() default pixel size},
     * if any.
     *
     * @param  date The date where to evaluate the images.
     * @return The renderable image.
     */
    public RenderableImage getRenderableImage(final Date date) {
        return new Renderable(date);
    }

    /**
     * Returns the default pixel size for images to be produced by {@link #getRenderableImage(Date)}.
     * This method is invoked by {@link RenderableImage#createDefaultRendering()} for computing a
     * default image size. The default implementation for this method always returns <code>null</code>.
     * Subclasses should overrides this method in order to provides a pixel size better suited to
     * their data.
     *
     * @return The default pixel size, or <code>null</code> if no default is provided.
     */
    protected Dimension2D getDefaultPixelSize() {
        return null;
    }

    /**
     * Produit des images sur demande. La méthode {@link Coverage3D#getGridCoverage2D(Date)}
     * utilise cette classe pour produire des couvertures de données à des dates arbitraires.
     * Elle procède en effectuant les étapes suivantes:
     *
     * <ul>
     *   <li>Appeler {@link Coverage3D#getRenderableImage(Date)} pour obtenir une instance de
     *       <code>Renderable</code>.</li>
     *   <li>Appeler {@link #createDefaultRendering()} pour obtenir une couverture avec la
     *       taille par défaut.</li>
     * </ul>
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Renderable extends Coverage.Renderable {
        /**
         * Construit un objet <code>Renderable</code> pour la date spécifiée.
         */
        public Renderable(final Date date) {
            super(temporalDimension!=0 ? 0 : 1,
                  temporalDimension>=2 ? 1 : 2);
            coordinate.ord[temporalDimension] = temporalCS.toValue(date);
        }
        
        /**
         * Returns a rendered image with width and height computed from
         * {@link Coverage3D#getDefaultPixelSize()}.
         */
        public RenderedImage createDefaultRendering() {
            final Dimension2D pixelSize = getDefaultPixelSize();
            if (pixelSize == null) {
                return super.createDefaultRendering();
            }
            return createScaledRendering((int)Math.round(getWidth()  / pixelSize.getWidth()),
                                         (int)Math.round(getHeight() / pixelSize.getHeight()), null);
        }
    }
}
