/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.gp;

// Images (Java2D)
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

// Java Advanced Imaging
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.RenderedOp;
import javax.media.jai.PlanarImage;
import javax.media.jai.Interpolation;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.InterpolationNearest;

// OpenGIS (SEAGIS-GCS) dependencies
import net.seagis.cv.Category;
import net.seagis.gc.GridCoverage;
import net.seagis.gc.GridGeometry;
import net.seagis.cv.CategoryList;
import net.seagis.cv.SampleDimension;

// OpenGIS (SEAGIS-CSS) dependencies
import net.seagis.pt.Envelope;
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.MathTransform;
import net.seagis.ct.MathTransform2D;
import net.seagis.ct.TransformException;
import net.seagis.ct.MathTransformFactory;
import net.seagis.ct.CoordinateTransformation;
import net.seagis.ct.CoordinateTransformationFactory;

// Resources
import java.util.List;
import java.util.Locale;
import net.seagis.resources.Images;
import net.seagis.resources.OpenGIS;
import net.seagis.resources.gcs.Resources;
import net.seagis.resources.gcs.ResourceKeys;


/**
 * Resample a grid coverage using a different grid geometry.
 * This operation provides the following functionality:<br>
 * <br>
 * <strong>Resampling</strong><br>
 * The grid coverage can be resampled at a different cell resolution. Some implementations
 * may be able to do resampling efficiently at any resolution. This can be determined from
 * the {@link GridCoverageProcessor} metadata <code>HasArbitraryResolutions</code> keyword.
 * Also a non-rectilinear grid coverage can be accessed as rectilinear grid coverage with
 * this operation.<br>
 * <br>
 * <strong>Reprojecting</strong><br>
 * The new grid geometry can have a different coordinate system than the underlying grid
 * geometry. For example, a grid coverage can be reprojected from a geodetic coordinate
 * system to Universal Transverse Mercator coordinate system.<br>
 * <br>
 * <strong>Subsetting</strong><br>
 * A subset of a grid can be viewed as a separate coverage by using this operation with a
 * grid geometry which as the same geoferencing and a region. Grid range in the grid geometry
 * defines the region to subset in the grid coverage.<br>
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
final class Resampler extends GridCoverage
{
    /**
     * Construct a new grid coverage performing
     * the specified coordinate transformation.
     *
     * @param  sourceCoverage     The original grid coverage.
     * @param  transformation     The transformation to apply. <code>sourceCS</code> <strong>must</strong>
     *                            be equals to the source grid coverage coordinate system.
     * @param  targetGridGeometry The target grid geometry, or <code>null</code> for default.
     * @param  interpolation      The interpolation to use.
     * @param  geophysics         Tells if the projection should be applied on the geophysics image or the
     *                            indexed image for the specified source coverage and interpolation type.
     * @param  factory            The factory to use for constructing math transforms.
     * @throws TransformException if a transformation failed.
     */
    private Resampler(final GridCoverage             sourceCoverage,
                      final CoordinateTransformation transformation,
                      final GridGeometry         targetGridGeometry,
                      final Interpolation             interpolation,
                      final boolean                      geophysics,
                      final MathTransformFactory            factory) throws TransformException
    {
        super(sourceCoverage.getName(null),
              getRenderedImage(sourceCoverage, geophysics), transformation.getTargetCS(),
              OpenGIS.transform(transformation.getMathTransform(), sourceCoverage.getEnvelope()),
              getCategories(sourceCoverage), geophysics, new GridCoverage[] {sourceCoverage}, null);

/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert sourceCoverage.getCoordinateSystem().equivalents(transformation.getSourceCS());
------- END OF JDK 1.4 DEPENDENCIES ---*/

        if (targetGridGeometry!=null)
        {
            // TODO
            throw new CannotReprojectException("'GridGeometry' parameter not yet implemented");
        }

        MathTransform transform = transformation.getMathTransform();
        if (!(transform instanceof MathTransform2D))
        {
            // TODO: Generalize to cases where 'sourceToTarget'
            // doesn't map two-dimensional coordinate systems.
            throw new CannotReprojectException("Only 2D transforms are currently implemented");
        }
        final RenderedOp      operation = (RenderedOp) getRenderedImage(geophysics);
        final RenderedImage sourceImage = operation.getSourceImage(0);
        final Warp                 warp = new WarpTransform(sourceCoverage.getGridGeometry(), (MathTransform2D) transform, gridGeometry, factory);
        final ParameterBlock      param = new ParameterBlock().addSource(sourceImage).add(warp).add(interpolation);
        operation.setParameterBlock(param); // Must be invoked before setOperationName.
        operation.setOperationName("Warp");
        // We had to set the operation's parameters last  because the construction of
        // 'WarpTransform' requires the geometry of this grid coverage. The trick was
        // to initialize this 'Resampler' with a null operation, and change the
        // operation here.

        final RenderingHints hints = operation.getRenderingHints();
        hints.add(Images.getRenderingHints(operation));
        operation.setRenderingHints(hints);

/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert sourceImage == sourceCoverage.getRenderedImage(geophysics);
        assert operation.getBounds().equals(PlanarImage.wrapRenderedImage(sourceImage).getBounds());
------- END OF JDK 1.4 DEPENDENCIES ---*/
    }

    /**
     * Create a new coverage with a different coordinate reference system.
     *
     * @param  sourceCoverage The source grid coverage.
     * @param  targetCS Coordinate system of the new grid coverage.
     * @param  targetGridGeometry The target grid geometry, or <code>null</code> for default.
     * @param  interpolation The interpolation to use.
     * @param  factory The transformation factory to use.
     * @return The new grid coverage, or <code>sourceCoverage</code> if no resampling was needed.
     * @throws CannotReprojectException if the grid coverage can't be reprojected.
     */
    private static GridCoverage reproject(final GridCoverage             sourceCoverage,
                                          final CoordinateSystem               targetCS,
                                          final GridGeometry         targetGridGeometry,
                                          final Interpolation             interpolation,
                                          final CoordinateTransformationFactory factory) throws CannotReprojectException
    {
        final CoordinateSystem sourceCS = sourceCoverage.getCoordinateSystem();
        if (sourceCS==targetCS && targetGridGeometry==null) // May be both null.
        {
            return sourceCoverage;
        }
        if (sourceCS==null || targetCS==null)
        {
            throw new CannotReprojectException(Resources.format(ResourceKeys.ERROR_UNSPECIFIED_COORDINATE_SYSTEM));
        }
        if (sourceCS.equivalents(targetCS) && targetGridGeometry==null)
        {
            return sourceCoverage;
        }
        // Tells if the projection should be applied on the geophysics image  or
        // the indexed image for the specified source coverage and interpolation
        // type.
        boolean geophysics = true;
        if (interpolation instanceof InterpolationNearest || isLinear(sourceCoverage))
        {
            final List sources = sourceCoverage.getRenderedImage(true).getSources();
            if (sources!=null)
            {
                final RenderedImage indexed = sourceCoverage.getRenderedImage(false);
                if (sources.contains(indexed)) geophysics = false;
            }       
        }
        try
        {
            final CoordinateTransformation transformation = factory.createFromCoordinateSystems(sourceCS, targetCS);
            return new Resampler(sourceCoverage, transformation, targetGridGeometry, interpolation, geophysics, factory.getMathTransformFactory());
        }
        catch (TransformException exception)
        {
            CannotReprojectException e = new CannotReprojectException(Resources.format(ResourceKeys.ERROR_CANT_REPROJECT_$1, sourceCoverage.getName(null)));
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw e;
        }
    }

    /**
     * Returns a grid coverage rendered image as a {@link RenderedOp}. This method wrap
     * <code>GridCoverage.getRenderedImage(geophysics)</code> in a "Null" operation.
     */
    private static RenderedOp getRenderedImage(final GridCoverage sourceCoverage, final boolean geophysics)
    {
        final RenderedImage image=sourceCoverage.getRenderedImage(geophysics);
        return JAI.create("Null", image, Images.getRenderingHints(image));
    }

    /**
     * Gets the source coverage category lists. The same
     * categories will be used for the transformed image.
     */
    private static CategoryList[] getCategories(final GridCoverage sourceCoverage)
    {
        final SampleDimension[] samplesDim = sourceCoverage.getSampleDimensions();
        final CategoryList[]    categories = new CategoryList[samplesDim.length];
        for (int i=0; i<categories.length; i++)
        {
            categories[i] = samplesDim[i].getCategoryList();
        }
        return categories;
    }

    /**
     * Check if the mapping between pixel values and geophysics value
     * is a linear relation for all bands in the specified coverage.
     */
    private static boolean isLinear(final GridCoverage sourceCoverage)
    {
        final SampleDimension[] samplesDim = sourceCoverage.getSampleDimensions();
        for (int i=samplesDim.length; --i>=0;)
        {
            final CategoryList categories = samplesDim[i].getCategoryList();
            if (categories==null)
            {
                // If there is no categories,  we assume that there is
                // no classification. It should be okay to interpolate
                // pixel values.
                continue;
            }
            if (categories.size()==1)
            {
                final Category category = categories.get(0);
                if (category.isQuantitative() && category.getClass().equals(Category.class))
                {
                    // If there is categories,  we require that there is only
                    // one category and this category must be translatable in
                    // numbers using a linear relation.
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the coverage name, localized for the supplied locale.
     * Default implementation fallback to the first source coverage.
     */
    public String getName(final Locale locale)
    {
        final GridCoverage[] sources = getSources();
        if (sources!=null && sources.length!=0)
            return sources[0].getName(locale);
        return super.getName(locale);
    }




    /**
     * The "Resample" operation. See package description for more details.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static final class Operation extends net.seagis.gp.Operation
    {
        /**
         * The coordinate transform factory to use when
         * coordinate transformation are required.
         */
        private final CoordinateTransformationFactory factory;

        /**
         * Construct a "Resample" operation.
         */
        public Operation(final CoordinateTransformationFactory factory)
        {
            super("Resample", new ParameterListDescriptorImpl(
                  null,         // the object to be reflected upon for enumerated values.
                  new String[]  // the names of each parameter.
                  {
                      "Source",
                      "InterpolationType",
                      "CoordinateSystem",
                      "GridGeometry"
                  },
                  new Class[]   // the class of each parameter.
                  {
                      GridCoverage.class,
                      Object.class,
                      CoordinateSystem.class,
                      GridGeometry.class
                  },
                  new Object[] // The default values for each parameter.
                  {
                      ParameterListDescriptor.NO_PARAMETER_DEFAULT,
                      "NearestNeighbor",
                      null, // Same as source grid coverage
                      null  // Automatic
                  },
                  null // Defines the valid values for each parameter.
            ));
            this.factory = factory;
        }

        /**
         * Resample a grid coverage. This method is invoked by
         * {@link GridCoverageProcessor} for the "Resample" operation.
         */
        protected GridCoverage doOperation(final ParameterList parameters)
        {
            final GridCoverage   source = (GridCoverage)     parameters.getObjectParameter("Source");
            final Interpolation  interp = toInterpolation   (parameters.getObjectParameter("InterpolationType"));
            final CoordinateSystem   cs = (CoordinateSystem) parameters.getObjectParameter("CoordinateSystem");
            final GridGeometry gridGeom = (GridGeometry)     parameters.getObjectParameter("GridGeometry");
            return reproject(source, (cs!=null) ? cs : source.getCoordinateSystem(), gridGeom, interp, factory);
        }
    }
}
