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
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.pt.MismatchedDimensionException;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cv.Coverage;
import net.seas.opengis.cv.CategoryList;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.cv.PointOutsideCoverageException;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.CoordinateSystem;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.ct.MathTransform;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;

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
import java.awt.Graphics2D;
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
import net.seas.util.WeakHashSet;

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
import net.seas.resources.Resources;
import net.seas.util.Version;
import net.seas.util.OpenGIS;
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
public class GridCoverage extends Coverage
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
     * Pool of created object. Objects in this pool must be immutable.
     * Those objects will be shared among many grid coverages.
     */
    private static final WeakHashSet<Object> pool=new WeakHashSet<Object>();

    /**
     * Version géophysiques de l'image {@link #image}.
     */
    private final PlanarImage numeric;

    /**
     * Image propre à l'affichage.
     */
    private final PlanarImage image;

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
     * Indices d'un pixel de l'image. Ce point est utilisé temporairement pour convertir les
     * coordonnées logiques en indices de pixels. L'objet utilisé est une sous-classe de
     * {@link Point} dans laquelle on a redéfinit {@link Point#setLocation(double,double)},
     * car il nous faut contrôler précisement les arrondissements. Nous devons arrondir vers
     * le bas plutôt que vers l'entier le plus près. Par exemple la position (4.8, 5.7)
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
     * @param name         The grid coverage name.
     * @param image        The image.
     * @param cs           The coordinate system. This specifies the coordinate system used
     *                     when accessing a grid coverage with the “evaluate” methods.  The
     *                     number of dimensions must matches the number of dimensions for
     *                     <code>envelope</code>.
     * @param envelope     The grid coverage cordinates. This envelope must have at least two
     *                     dimensions.   The two first dimensions describe the image location
     *                     along <var>x</var> and <var>y</var> axis. The other dimensions are
     *                     optional and may be used to locate the image on a vertical axis or
     *                     on the time axis.
     * @param categories   Category lists which allows for the transformation from pixel
     *                     values to real world geophysics value. This array's length must
     *                     matches the number of bands in <code>image</code>.
     * @param isGeophysics <code>true</code> if pixel's values are already geophysics values, or
     *                     <code>false</code> if transformation described in <code>categories</code>
     *                     must be applied first.
     *
     * @throws MismatchedDimensionException If the envelope's dimension
     *         is not the same than the coordinate system's dimension.
     * @param  IllegalArgumentException if the number of bands differs
     *         from the number of categories list.
     */
    public GridCoverage(final String         name,       final RenderedImage  image,
                        final CoordinateSystem cs,       final Envelope    envelope,
                        final CategoryList[] categories, final boolean isGeophysics) throws MismatchedDimensionException
    {
        this(name, image, cs, envelope.clone(), null, categories, isGeophysics);
    }

    // TODO: In a future version, we will provide a constructor expecting
    //       a MathTransform argument instead of Envelope.  But first, we
    //       need a MathTransform.toAffineTransform2D() method...

    /**
     * Construct a grid coverage. This private constructor expect both an envelope
     * (<code>envelope</code>) and a math transform (<code>transform</code>).  One
     * of those argument should be null.   The null argument will be computed from
     * the non-null argument.
     */
    private GridCoverage(final String         name,       final RenderedImage image,
                         final CoordinateSystem cs,       Envelope envelope, MathTransform transform,
                         final CategoryList[] categories, final boolean isGeophysics) throws MismatchedDimensionException
    {
        super(name, cs);

        /*
         * Check category lists. The number of lists
         * must match the number of image's bands.
         */
        final int numBands = image.getSampleModel().getNumBands();
        if (numBands != categories.length)
        {
            throw new IllegalArgumentException(Resources.format(Clé.NUMBER_OF_BANDS_MISMATCH¤2, new Integer(numBands), new Integer(categories.length)));
        }

        /*
         * Checks the envelope. The envelope must be non-empty and
         * its dimension must match the coordinate system's dimension.
         */
        if (envelope==null) try
        {
            envelope = new Envelope(getBounds(image));
            for (int i=envelope.getDimension(); --i>=0;)
            {
                // According OpenGIS's specification, GridGeometry maps pixel's center.
                // We want a bounding box for all pixels, not pixel's centers. Offset by
                // 0.5 (use -0.5 for maximum too, not +0.5).
                envelope.setRange(i, envelope.getMinimum(i)-0.5, envelope.getMaximum(i)-0.5);
            }
            envelope = OpenGIS.transform(transform, envelope);
        }
        catch (TransformException exception)
        {
            final IllegalArgumentException e=new IllegalArgumentException(exception.getLocalizedMessage());
            if (Version.MINOR>=4) e.initCause(exception);
            throw e;
        }
        final int dimension = envelope.getDimension();
        if (envelope.isEmpty() || dimension<2)
        {
            throw new IllegalArgumentException(Resources.format(Clé.EMPTY_ENVELOPE));
        }
        if (dimension != cs.getDimension())
        {
            throw new MismatchedDimensionException(cs, envelope);
        }
        this.envelope = (Envelope)pool.intern(envelope);

        /*
         * Compute the grid geometry. If the specified math transform is non-null,
         * it will be used as is. Otherwise, it will be computed from the envelope.
         */
        final GridRange    gridRange = (GridRange)pool.intern(new GridRange(image, dimension));
        final GridGeometry gridGeometry;
        if (transform==null)
        {
            final boolean[] inverse = new boolean[dimension];
            inverse[1] = true; // Inverse 'y' axis only.
            gridGeometry = new GridGeometry(gridRange, envelope, inverse);
        }
        else gridGeometry = new GridGeometry(gridRange, transform);
        this.gridGeometry = (GridGeometry)pool.intern(gridGeometry);

        /*
         * Construct sample dimensions and the image.  We keep two versions of the image.
         * One is suitable for rendering (it uses integer pixels, which are rendered much
         * faster than float value),  and the other is suitable for computation (since it
         * uses real numbers).
         */
        final SampleDimension[] dimensions = new SampleDimension[numBands];
        for (int i=0; i<numBands; i++)
        {
            dimensions[i] = new GridSampleDimension(categories[i]);
        }
        if (isGeophysics)
        {
            this.numeric = PlanarImage.wrapRenderedImage(image);
            this.image   = PlanarImage.wrapRenderedImage(toThematic(this.numeric, categories));
        }
        else
        {
            this.image   = PlanarImage.wrapRenderedImage(image);
            this.numeric = PlanarImage.wrapRenderedImage(toNumeric(this.image, categories));
        }
        this.images           = USE_PYRAMID ? new ImageMIPMap(image, AffineTransform.getScaleInstance(DOWN_SAMPLER, DOWN_SAMPLER), null) : null;
        this.maxLevel         = Math.max((int) (Math.log((double)MIN_SIZE/(double)Math.max(image.getWidth(), image.getHeight()))/LOG_DOWN_SAMPLER), 0);
        this.sampleDimensions = Collections.unmodifiableList(Arrays.asList(dimensions));
    }

    /**
     * Returns the image's bounds as a {@link Rectangle}.
     */
    private static Rectangle getBounds(final RenderedImage image)
    {return new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());}

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
     * Return a sequence of integer values for a given point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
//  public int[] evaluate(final CoordinatePoint coord, final int[] dest) throws PointOutsideCoverageException
//  {
        // TODO
//      return null;
//  }

    /**
     * Return an sequence of double values for a given point in the coverage.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code>.
     * @return An array containing values.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public synchronized double[] evaluate(final CoordinatePoint coord, final double[] dest) throws PointOutsideCoverageException
    {
        final Point2D point = new Point2D.Double(coord.ord[0], coord.ord[1]);
        if (setPoint(point))
        {
            final int x = pixel.x;
            final int y = pixel.y;
            return numeric.getTile(numeric.XToTileX(x), numeric.YToTileY(y)).getPixel(x, y, dest);
        }
        else throw new PointOutsideCoverageException(coord);
    }

    /*
     * Retourne une chaîne de caractère décrivant un pixel. Si le pixel aux coordonnées
     * spécifiées contient une valeur numérique, cette valeur sera retournée sous forme
     * de chaîne de caractères suivit du symbole des unités. Si au contraire le pixel
     * représente une valeur <code>NaN</code>, alors cette méthode retourne le nom du
     * thème du pixel (par exemple "Nuage").
     *
     * @param point Coordonnées logiques du pixel dont on veut la description. Ces
     *              coordonnées doivent être en mètres ou en degrés de longitude et
     *              de latitude dépendament du système de coordonnées de l'image.
     *
     * @return Description du pixel, ou <code>null</code> si le pixel demandé est en
     *         dehors des limites de l'image ou ne correspond pas à un thème connu.
     */
//  public synchronized String getLabel(final Point2D point)
//  {
//      if (!setPoint(point)) return null;
//      final int x = pixel.x;
//      final int y = pixel.y;
//      return themes.getLabel(image.getTile(image.XToTileX(x), image.YToTileY(y)), x, y);
//  }

    /**
     * Convertit les coordonnées logiques <code>point</code> en coordonnées pixel.
     * Le résultat sera placé dans le champ {@link #pixel}. Cette méthode retourne
     * <code>true</code> si la conversion a réussie, ou <code>false</code> si elle
     * a échouée ou si les coordonnées résultantes sont en dehors des limites de
     * l'image.
     */
    private boolean setPoint(Point2D point)
    {
        try
        {
            point=gridGeometry.getGridToCoordinateJAI().inverseTransform(point, pixel);
            assert(point==pixel);
            final int x    = pixel.x;
            final int y    = pixel.y;
            final int xmin = image.getMinX();
            final int ymin = image.getMinY();
            return x>=xmin && y>=ymin && x<xmin+image.getWidth() && y<ymin+image.getHeight();
        }
        catch (NoninvertibleTransformException exception)
        {
            unexpectedException("getValue", exception);
        }
        return false;
    }

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

    /*
     * Determine the histogram of grid values for this coverage.
     */
//  public Histogram getHistogram()
//  {
//      final List<SampleDimension> samples = getSampleDimensions();
//      final int    dimension = samples.size();
//      final double[] minimum = new double[dimension];
//      final double[] maximum = new double[dimension];
//      Arrays.fill(minimum, Double.POSITIVE_INFINITY);
//      Arrays.fill(maximum, Double.NEGATIVE_INFINITY);
//      for (int i=0; i<dimension; i++)
//      {
//          final CategoryList categories = samples.get(i).getCategoryList();
//          if (categories!=null)
//          {
//              final Range range = categories.getRange(true);
//              if (range!=null)
//              {
//                  minimum[i] = ((Number)range.getMinValue()).doubleValue();
//                  maximum[i] = ((Number)range.getMaxValue()).doubleValue();
//              }
//          }
//      }
        // TODO
//      return null;
//  }

    /*
     * Determine the histogram of grid values for this coverage.
     *
     * @param  miniumEntryValue Minimum value stored in the first histogram entry.
     * @param  maximumEntryValue Maximum value stored in the last histogram entry.
     * @param  numberEntries Number of entries in the histogram.
     * @return The histogram.
     */
//  public Histogram getHistogram(double minimumEntryValue, double maximumEntryValue, int numberEntries)
//  {return null;}

    /**
     * Dessine l'image vers le graphique spécifié. Il est de la responsabilité du programmeur de s'assurer
     * que la transformation affine de <code>graphics</code> représente un espace en coordonnées logiques,
     * le même que celui de {@link #getCoordinateSystem}.
     */
    public void paint(final Graphics2D graphics)
    {
        final AffineTransform gridToCoordinate  = gridGeometry.getGridToCoordinateJAI();
        if (images==null)
        {
            graphics.drawRenderedImage(image, gridToCoordinate);
        }
        else
        {
            /*
             * Calcule quelle "niveau" d'image serait la plus appropriée
             * Ce calcul est fait en fonction de la résolution requise.
             */
            AffineTransform transform=graphics.getTransform();
            transform.concatenate(gridToCoordinate);
            final int level = Math.max(0,
                              Math.min(maxLevel,
                                       (int) (Math.log(Math.max(XAffineTransform.getScaleX0(transform),
                                                                XAffineTransform.getScaleY0(transform)))/LOG_DOWN_SAMPLER)));
            /*
             * Si on utilise une résolution inférieure (pour un
             * affichage plus rapide), alors il faut utilisé un
             * géoréférencement ajusté en conséquence.
             */
            if (level!=0)
            {
                transform.setTransform(gridToCoordinate);
                final double scale=Math.pow(DOWN_SAMPLER, -level);
                transform.scale(scale, scale);
            }
            else transform=gridToCoordinate;
            /*
             * Procède maintenant au traçace de l'image. Si on avait pas voulu tenir compte
             * de la résolution d'affichage, alors la seule ligne nécessaire aurait été:
             *
             * graphics.drawRenderedImage(image, gridToCoordinate);
             */
            graphics.drawRenderedImage(images.getImage(level), transform);
        }
    }

    /**
     * Préviens l'image que la région spécifiée peut avoir besoin d'être dessinée dans un
     * futur proche. Certaines implémentations peuvent démarrer des threads en arrière-plan
     * pour préparer l'image à l'avance.
     *
     * @param area Coordonnées logiques de la région à préparer, ou <code>null</code> pour
     *             préparer l'ensemble de l'image. Ces coordonnées doivent être exprimées
     *             selon le système de coordonnées {@link #getCoordinateSystem}.
     */
    public void prefetch(Rectangle2D area)
    {
        try
        {
            if (area!=null)
            {
                area = inverseTransform(area);
                area = new Rectangle((int)area.getX(), (int)area.getY(), (int)area.getWidth(), (int)area.getHeight());
            }
            final Point[] tileIndices=image.getTileIndices((Rectangle) area);
            if (tileIndices!=null) image.prefetchTiles(tileIndices);
        }
        catch (NoninvertibleTransformException exception)
        {
            unexpectedException("prefetch", exception);
            // Si on n'a pas pu calculer les coordonnées pixels de la
            // région à préparer, on laisse tomber. Tout simplement.
            // Ca n'a pas d'impact grave car cette méthode n'est que
            // facultative.
        }
    }

    /**
     * Appelée lorsqu'une exception inatendue est survenue.
     */
    private static void unexpectedException(final String method, final NoninvertibleTransformException exception)
    {ExceptionMonitor.unexpectedException("net.seas.opengis", "GridCoverage", method, exception);}
}
