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
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

// Java Advanced Imaging
import javax.media.jai.JAI;
import javax.media.jai.Warp;
import javax.media.jai.Interpolation;

// OpenGIS (SEAGIS-GCS) dependencies
import net.seagis.gc.GridCoverage;
import net.seagis.gc.GridGeometry;
import net.seagis.cv.CategoryList;
import net.seagis.cv.SampleDimension;

// OpenGIS (SEAGIS-CSS) dependencies
import net.seagis.pt.Matrix;
import net.seagis.pt.Envelope;
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.MathTransform;
import net.seagis.ct.MathTransform2D;
import net.seagis.ct.TransformException;
import net.seagis.ct.CoordinateTransformation;
import net.seagis.ct.CoordinateTransformationFactory;

// Resources
import net.seagis.resources.OpenGIS;
import net.seagis.resources.Utilities;
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
final class Resampler
{
    /**
     * The coordinate transform factory to use.
     */
    private final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();

    /**
     * The interpolation to use. Default to nearest neighbor.
     */
    private final Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    /**
     * Create a new coverage with a different coordinate reference system.
     *
     * @param  sourceCoverage The source grid coverage.
     * @param  targetCS Coordinate system of the new grid coverage.
     * @return The new grid coverage.
     * @throws CannotReprojectException if the grid coverage can't be reprojected.
     */
    private GridCoverage reproject(final GridCoverage sourceCoverage, final CoordinateSystem targetCS) throws CannotReprojectException
    {
        final CoordinateSystem sourceCS = sourceCoverage.getCoordinateSystem();
        if (sourceCS==targetCS) // May be both null.
        {
            return sourceCoverage;
        }
        if (sourceCS==null || targetCS==null)
        {
            throw new CannotReprojectException(Resources.format(ResourceKeys.ERROR_UNSPECIFIED_COORDINATE_SYSTEM));
        }
        if (sourceCS.equivalents(targetCS))
        {
            return sourceCoverage;
        }
        try
        {
            final Envelope      sourceEnvelope = sourceCoverage.getEnvelope();
            final GridGeometry  sourceGeometry = sourceCoverage.getGridGeometry();
            final MathTransform sourceToTarget = factory.createFromCoordinateSystems(sourceCS, targetCS).getMathTransform();
            final Envelope      targetEnvelope = OpenGIS.transform(sourceToTarget, sourceEnvelope);
            final GridGeometry  targetGeometry = new GridGeometry(sourceGeometry.getGridRange(), targetEnvelope, inverted(sourceGeometry));

            final SampleDimension[] samplesDim = sourceCoverage.getSampleDimensions();
            final CategoryList[]    categories = new CategoryList[samplesDim.length];
            for (int i=0; i<categories.length; i++)
                categories[i] = samplesDim[i].getCategoryList();

            // TODO: Generalize to cases where 'sourceToTarget' doesn't map two-dimensional coordinate systems.
            final Warp warp = new WarpTransform(sourceGeometry, (MathTransform2D) sourceToTarget, targetGeometry);

            final RenderedImage sourceImage = sourceCoverage.getRenderedImage(true);
            final ParameterBlock param = new ParameterBlock().addSource(sourceImage).add(warp).add(interpolation);
            final RenderedImage projectedImage = JAI.create("Warp", param);

/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert projectedImage.getWidth()  == sourceImage.getWidth();
            assert projectedImage.getHeight() == sourceImage.getHeight();
            assert projectedImage.getMinX()   == sourceImage.getMinX();
            assert projectedImage.getMinY()   == sourceImage.getMinY();
------- END OF JDK 1.4 DEPENDENCIES ---*/

            final String name = sourceCoverage.getName(null)+" reprojected"; // TODO: localize
            return new GridCoverage(name, projectedImage, targetCS, targetEnvelope, categories, true,
                                    new GridCoverage[] {sourceCoverage}, null);
        }
        catch (TransformException exception)
        {
            CannotReprojectException e = new CannotReprojectException("Can't reproject"); // TODO: localize
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw e;
        }
    }

    /**
     * Try to guess which axis are inverted in the specified grid geometry.
     * If this method can't make the guess, it returns <code>null</code>.
     *
     * TODO: Should we move this method into yet an other GridCoverage's constructor?
     */
    private static boolean[] inverted(final GridGeometry geometry)
    {
        final Matrix matrix;
        try
        {
            // Try to get the affine transform, assuming it is
            // insensitive to location (thus the 'null' argument).
            matrix = geometry.getGridToCoordinateSystem().derivative(null);
        }
        catch (NullPointerException exception)
        {
            // The approximate affine transform is location-dependent.
            // We can't guess axis orientation from this.
            return null;
        }
        catch (Exception exception)
        {
            // Some other error occured. We didn't expected it,
            // but it will not prevent 'Resampler' to work.
            Utilities.unexpectedException("net.seagis.gcs", "MathTransform", "derivative", exception);
            return null;
        }
        final int numCols = matrix.getNumColumns();
        final boolean[] inverse = new boolean[matrix.getNumRows()];
        for (int j=0; j<inverse.length; j++)
        {
            for (int i=0; i<numCols; i++)
            {
                final double value = matrix.get(j,i);
                if (i==j)
                {
                    inverse[j] = (value < 0);
                }
                else if (value!=0)
                {
                    // Matrix is not diagonal.
                    // Can't guess axis direction.
                    return null;
                }
            }
        }
        return inverse;
    }
}
