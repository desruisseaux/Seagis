/*
 * OpenGIS implementation in Java
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
package net.seas.opengis.gc;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.cv.Coverage;
import net.seas.opengis.cv.CategoryList;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.pt.MismatchedDimensionException;

// Images
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

// Java Advanced Imaging
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.Histogram;
import javax.media.jai.ImageMIPMap;
import javax.media.jai.PlanarImage;
import javax.media.jai.GraphicsJAI;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.util.Range;

// Geometry
import java.awt.Point;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import net.seas.util.XAffineTransform;
import net.seas.util.XDimension2D;

// Collections
import java.util.List;
import java.util.Collections;

// Weak references
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

// Events
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// Miscellaneous
import java.util.Date;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.FieldPosition;
import net.seas.awt.ExceptionMonitor;
import net.seas.util.XClass;


/**
 * Basic access to grid data values. Grid coverages are backed by
 * {@link RenderedImage}. Each band in an image is represented as
 * a sample dimension.
 * <br><br>
 * Grid coverages are usually two-dimensional. However, their envelope may
 * have more than two dimensions.  For example, a remote sensing image may
 * be valid only over some time range (the time of satellite pass over the
 * observed area). Envelope for such grid coverage may have three dimensions:
 * the two usual ones (horizontal extends along <var>x</var> and <var>y</var>),
 * and a third one for start time and end time (time extends along <var>t</var>).
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public abstract class GridCoverage extends Coverage
{
    /**
     * Indique si on doit tenter une optimisation utilisant des images pyramidales.
     * L'implémentation par défaut n'utilise pas les images pyramidales, parce qu'il
     * faut garder l'image originale en mémoire de toute façon.
     */
    private static final boolean USE_PYRAMID=false;

    /**
     * Facteur par lequel diminuer la taille des
     * images pour diminuer leurs résolutions.
     */
    private static final double DOWN_SAMPLER = 0.5;

    /**
     * Logarithme naturel du facteur par lequel diminuer la
     * taille des images pour diminuer leurs résolutions.
     */
    private static final double LOG_DOWN_SAMPLER = Math.log(DOWN_SAMPLER);

    /**
     * Largeur ou hauteur minimale (en pixel) en dessous de laquelle on
     * considère que ça ne vaut plus la peine de continuer à diminuer
     * la résolution de l'image.
     */
    private static final int MIN_SIZE = 128;

    /**
     * Quantité à ajouter à certains entiers pour
     * éviter des erreurs d'arrondissements.
     */
    private static final double EPS = 1E-6;

    /**
     * Image représenté par cet objet <code>RemoteImage</code>.
     * Une référence directe est utilisée afin d'accélérer les
     * interrogations des pixels.
     */
    private final PlanarImage image;

    /**
     * Image {@link RenderedImage} qui avait été retournée
     * lors du dernier appel de {@link #getNumericImage()}.
     */
    private transient Reference numeric;

    /**
     * Liste des images. L'image de niveau 0 est l'image spécifiée au constructeur,
     * celle qui a la résolution maximale. Les images suivantes sont la même image
     * mais à des résolutions plus faibles.
     */
    private final ImageMIPMap images;

    /**
     * Nombre maximal de niveaux qu'on se permettra. En dessous de
     * ce niveau, ça ne vaut plus vraiment la peine de continuer à
     * réduire la résolution des images.
     */
    private final int maxLevel;

    /**
     * The grid geometry.
     */
    private final GridGeometry gridGeometry;

    /**
     * Coordonnées de la région couverte par l'image. Cette envelope
     * a au moins deux dimensions. Elle peut toutefois en avoir plus
     * pour mémoriser, par exemple, la date de début et de fin ou la
     * profondeur de l'image. Le nombre de dimensions de cette image
     * doit correspondre au nombre de dimensions du système de
     * coordonnées.
     */
    private final Envelope envelope;

    /**
     * List of sample dimension information for the grid coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * Indices d'un pixel de l'image. Ce point est utilisé temporairement pour convertir les coordonnées logiques
     * en indices de pixels.    L'objet utilisé est une sous-classe de {@link Point} dans laquelle on a redéfinit
     * {@link Point#setLocation(double,double)}, car il nous faut contrôler précisement les arrondissements. Nous
     * devons arrondir vers le bas plutôt que vers l'entier le plus près. Par exemple la position (4.8, 5.7)
     * appartient encore au pixel (4,5).
     */
    private final transient Point pixel=new Point()
    {
        public void setLocation(final double x, final double y)
        {
            this.x = (int) Math.floor(x);
            this.y = (int) Math.floor(y);
        }
    };

    /**
     * Construct a grid coverage with the specified envelope.
     *
     * @param name                The grid coverage name.
     * @param coordinateSystem    The coordinate system. This specifies the coordinate system used
     *                            when accessing a grid coverage with the “evaluate” methods.  The
     *                            number of dimensions must matches the number of dimensions for
     *                            <code>envelope</code>.
     * @param envelope            The grid coverage cordinates. This envelope must have at least two
     *                            dimensions.   The two first dimensions describe the image location
     *                            along <var>x</var> and <var>y</var> axis. The other dimensions are
     *                            optional and may be used to locate the image on a vertical axis or
     *                            on the time axis.
     * @param image               The image.
     * @param categories          A list of categories which allows for the transformation from pixel
     *                            values to real world geophysics value.
     *
     * @throws MismatchedDimensionException If the envelope's dimension
     *         is not the same than the coordinate system's dimension.
     */
    public GridCoverage(final String name, final CoordinateSystem coordinateSystem, final Envelope envelope,
                        final RenderedImage image, final List<CategoryList> categories) throws MismatchedDimensionException
    {
        this(name, coordinateSystem, envelope.clone(), image, getTransform(image, envelope), categories);
    }

    /**
     * Construct a two dimensional grid coverage with the specified bounds.
     *
     * @param name                The grid coverage name.
     * @param coordinateSystem    The coordinate system. This specifies the coordinate system used
     *                            when accessing a grid coverage with the “evaluate” methods.  The
     *                            number of dimensions must be 2.
     * @param envelope            The grid coverage cordinates.
     * @param image               The image.
     * @param categories          A list of categories which allows for the transformation from pixel
     *                            values to real world geophysics value.
     *
     * @throws MismatchedDimensionException If the coordinate system's dimension is not 2.
     */
    public GridCoverage(final String name, final CoordinateSystem coordinateSystem, final Rectangle2D envelope,
                        final RenderedImage image, final List<CategoryList> categories) throws MismatchedDimensionException
    {
        this(name, coordinateSystem, new Envelope(envelope), image, categories);
    }

    /**
     * Construct a two dimensional grid coverage with the specified affine transform.
     *
     * @param name                The grid coverage name.
     * @param coordinateSystem    The coordinate system. This specifies the coordinate system used
     *                            when accessing a grid coverage with the “evaluate” methods. This
     *                            coordinate system should have two dimensions.
     * @param image               The image.
     * @param gridToCoordinateJAI An affine transform which allows for the transformations from grid
     *                            coordinates to real world earth coordinates. This affine transform
     *                            must convert pixel's <em>upper left corner</em> coordinates into
     *                            real world earth coordinates.
     * @param categories          A list of categories which allows for the transformation from pixel
     *                            values to real world geophysics value.
     *
     * @throws MismatchedDimensionException If the coordinate system's dimension is not 2.
     */
    public GridCoverage(final String        name,  final CoordinateSystem coordinateSystem,
                        final RenderedImage image, final AffineTransform  gridToCoordinateJAI, final List<CategoryList> categories)
    {
        this(name, coordinateSystem, getEnvelope(image, gridToCoordinateJAI), image, new AffineTransform(gridToCoordinateJAI), categories);
    }

    /**
     * Construct a grid coverage using the specified coordinate system.
     *
     * @param name                The grid coverage name.
     * @param coordinateSystem    The coordinate system. This specifies the coordinate system used
     *                            when accessing a grid coverage with the “evaluate” methods.  The
     *                            number of dimensions must matches the number of dimensions for
     *                            <code>envelope</code>.
     * @param envelope            The grid coverage cordinates. This envelope must have at least two
     *                            dimensions.   The two first dimensions describe the image location
     *                            along <var>x</var> and <var>y</var> axis. The other dimensions are
     *                            optional and may be used to locate the image on a vertical axis or
     *                            on the time axis.
     * @param image               The image.
     * @param gridToCoordinateJAI An affine transform which allows for the transformations from grid
     *                            coordinates to real world earth coordinates. This affine transform
     *                            must convert pixel's <em>upper left corner</em> coordinates into
     *                            real world earth coordinates.
     * @param categories          A list of categories which allows for the transformation from pixel
     *                            values to real world geophysics value.
     *
     * @throws MismatchedDimensionException If the envelope's dimension
     *         is not the same than the coordinate system's dimension.
     */
    private GridCoverage(final String        name,  final CoordinateSystem coordinateSystem,    final Envelope           envelope,
                         final RenderedImage image, final AffineTransform  gridToCoordinateJAI, final List<CategoryList> categories)
    {
        super(name, coordinateSystem);
        if (categories == null) throw new NullPointerException("categories");
        if (envelope.isEmpty()) throw new IllegalArgumentException(String.valueOf(envelope));
        if (envelope.getDimension() != coordinateSystem.getDimension())
        {
            throw new MismatchedDimensionException(coordinateSystem, envelope);
        }

        this.envelope     = envelope;
        this.image        = PlanarImage.wrapRenderedImage(image);
        this.images       = USE_PYRAMID ? new ImageMIPMap(image, AffineTransform.getScaleInstance(DOWN_SAMPLER, DOWN_SAMPLER), null) : null;
        this.gridGeometry = new GridGeometry(new GridRange(image, envelope.getDimension()), gridToCoordinateJAI);
        this.sampleDimensions = null; // TODO
//      this.categories   = categories;
        this.maxLevel     = Math.max((int) (Math.log((double)MIN_SIZE/(double)Math.max(image.getWidth(), image.getHeight()))/LOG_DOWN_SAMPLER), 0);
    }

    /**
     * Retourne une transformation affine pour géoréférencer l'image spécifiée.
     * Cette transformation affine convertira les indices (<var>i</var>,<var>j</var>)
     * des pixels en coordonnées utilisateurs (<var>x</var>,<var>y</var>).
     */
    private static AffineTransform getTransform(final RenderedImage image, final Envelope envelope)
    {
        final double dx = envelope.getLength(0) / image.getWidth();
        final double dy = envelope.getLength(1) / image.getHeight();
        return new AffineTransform(dx, 0, 0, -dy,
                                   envelope.getMinimum(0) - dx*image.getMinX(),
                                   envelope.getMaximum(1) + dy*image.getMinY());
    }

    /**
     * Retourne les coordonnées projetées des bords de l'image spécifiée.
     */
    private static Envelope getEnvelope(final RenderedImage image, final AffineTransform gridToCoordinateJAI)
    {
        if (gridToCoordinateJAI.getDeterminant()==0) throw new IllegalArgumentException(String.valueOf(gridToCoordinateJAI));
        final Rectangle2D rect=new Rectangle2D.Double(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
        return new Envelope(XAffineTransform.transform(gridToCoordinateJAI, rect, rect));
    }

    /**
     * Retourne les indices d'une boîte qui englobe tous les pixels qui interceptent la région spécifiée.
     *
     * @param  bounds Les coordonnées logiques de la région dont on veut les indices.
     * @return Boîte englobant tous les pixels qui interceptent la région <code>geographicArea</code>.
     * @throws NoninvertibleTransformException si le géoréférencement n'a pas pu être inversé.
     */
    private Rectangle2D inverseTransform(Rectangle2D bounds) throws NoninvertibleTransformException
    {
        bounds = XAffineTransform.inverseTransform(gridGeometry.getGridToCoordinateJAI(), bounds, null);
        final double xmin = Math.floor(bounds.getMinX() + EPS);
        final double ymin = Math.floor(bounds.getMinY() + EPS);
        final double xmax = Math.ceil (bounds.getMaxX() - EPS);
        final double ymax = Math.ceil (bounds.getMaxY() - EPS);
        bounds.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
        return bounds;
    }

    /**
     * Returns <code>true</code> if grid data can be edited.
     * The default implementation returns <code>false</code>.
     */
    public boolean isDataEditable()
    {return false;}

    /**
     * Returns information for the grid coverage geometry. Grid geometry
     * includes the valid range of grid coordinates and the georeferencing.
     */
    public GridGeometry getGridGeometry()
    {return gridGeometry;}

    /**
     * Returns The bounding box for the coverage domain in coordinate
     * system coordinates.
     */
    public Envelope getEnvelope()
    {return envelope.clone();}

    /**
     * Retrieve sample dimension information for the coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension. A coverage must have at least one sample dimension.
     */
    public List<SampleDimension> getSampleDimensions()
    {return sampleDimensions;}

    /**
     * Return a sequence of strongly typed values for a block.
     * A value for each sample dimension will be returned. The return value is an
     * <CODE>N+1</CODE> dimensional array, with dimensions. For 2 dimensional
     * grid coverages, this array will be accessed as (sample dimension, column,
     * row). The index values will be based from 0. The indices in the returned
     * <CODE>N</CODE> dimensional array will need to be offset by grid range
     * minimum coordinates to get equivalent grid coordinates.
     */
//  public abstract DoubleMultiArray getDataBlockAsDouble(final GridRange range)
//  {
        // TODO: Waiting for multiarray package (JSR-083)!
        //       Same for setDataBlock*
//  }

    /**
     * Determine the histogram of grid values for this coverage.
     */
    public Histogram getHistogram()
    {
        final List<SampleDimension> samples = getSampleDimensions();
        final int    dimension = samples.size();
        final double[] minimum = new double[dimension];
        final double[] maximum = new double[dimension];
        Arrays.fill(minimum, Double.POSITIVE_INFINITY);
        Arrays.fill(maximum, Double.NEGATIVE_INFINITY);
        for (int i=0; i<dimension; i++)
        {
            final CategoryList categories = samples.get(i).getCategoryList();
            if (categories!=null)
            {
                final Range range = categories.getRange(true);
                if (range!=null)
                {
                    minimum[i] = ((Number)range.getMinValue()).doubleValue();
                    maximum[i] = ((Number)range.getMaxValue()).doubleValue();
                }
            }
        }
        // TODO
        return null;
    }

    /**
     * Determine the histogram of grid values for this coverage.
     *
     * @param  miniumEntryValue Minimum value stored in the first histogram entry.
     * @param  maximumEntryValue Maximum value stored in the last histogram entry.
     * @param  numberEntries Number of entries in the histogram.
     * @return The histogram.
     */
    public Histogram getHistogram(double minimumEntryValue, double maximumEntryValue, int numberEntries)
    {return null;}
}
