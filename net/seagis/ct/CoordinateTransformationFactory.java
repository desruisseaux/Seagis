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
package net.seagis.ct;

// OpenGIS (SEAGIS) dependencies
import net.seagis.pt.Matrix;
import net.seagis.cs.AxisInfo;
import net.seagis.cs.Projection;
import net.seagis.cs.PrimeMeridian;
import net.seagis.cs.AxisOrientation;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.ProjectedCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.cs.HorizontalCoordinateSystem;

// Miscellaneous
import javax.units.Unit;
import net.seagis.resources.Utilities;


/**
 * Creates coordinate transformations.
 *
 * @version 1.0
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_CoordinateTransformationFactory
 */
public class CoordinateTransformationFactory
{
    /**
     * The default coordinate transformation factory.
     * Will be constructed only when first needed.
     */
    private static CoordinateTransformationFactory DEFAULT;

    /**
     * The underlying math transform factory.
     */
    private final MathTransformFactory factory;

    /**
     * Construct a coordinate transformation factory.
     *
     * @param factory The math transform factory to use.
     */
    public CoordinateTransformationFactory(final MathTransformFactory factory)
    {this.factory = factory;}

    /**
     * Returns the default coordinate transformation factory.
     */
    public static synchronized CoordinateTransformationFactory getDefault()
    {
        if (DEFAULT==null)
        {
            DEFAULT = new CoordinateTransformationFactory(MathTransformFactory.getDefault());
        }
        return DEFAULT;
    }

    /**
     * Returns the underlying math transform factory.
     */
    protected MathTransformFactory getMathTransformFactory()
    {return factory;}

    /**
     * Creates a transformation between two coordinate systems.
     * This method will examine the coordinate systems in order to construct a
     * transformation between them. This method may fail if no path between the
     * coordinate systems is found.
     *
     * @param  sourceCS Input coordinate system.
     * @param  targetCS Output coordinate system.
     * @return A coordinate transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if no transformation path has been found.
     *
     * @see org.opengis.ct.CT_CoordinateTransformationFactory#createFromCoordinateSystems
     */
    public CoordinateTransformation createFromCoordinateSystems(final CoordinateSystem sourceCS, final CoordinateSystem targetCS) throws CannotCreateTransformException
    {
        /////////////////////////////////////////////////////////
        ////                                                 ////
        ////     Geographic  -->  Geographic or Projected    ////
        ////                                                 ////
        /////////////////////////////////////////////////////////
        if (sourceCS instanceof GeographicCoordinateSystem)
        {
            final GeographicCoordinateSystem source = (GeographicCoordinateSystem) sourceCS;
            if (targetCS instanceof GeographicCoordinateSystem)
            {
                return createTransformationStep(source, (GeographicCoordinateSystem) targetCS);
            }
            if (targetCS instanceof ProjectedCoordinateSystem)
            {
                return createTransformationStep(source, (ProjectedCoordinateSystem) targetCS);
            }
        }
        /////////////////////////////////////////////////////////
        ////                                                 ////
        ////     Projected  -->  Projected or Geographic     ////
        ////                                                 ////
        /////////////////////////////////////////////////////////
        if (sourceCS instanceof ProjectedCoordinateSystem)
        {
            final ProjectedCoordinateSystem source = (ProjectedCoordinateSystem) sourceCS;
            if (targetCS instanceof ProjectedCoordinateSystem)
            {
                return createTransformationStep(source, (ProjectedCoordinateSystem) targetCS);
            }
            if (targetCS instanceof GeographicCoordinateSystem) try
            {
                return createTransformationStep((GeographicCoordinateSystem) targetCS, source).inverse();
            }
            catch (NoninvertibleTransformException exception)
            {
                final CannotCreateTransformException e = new CannotCreateTransformException(sourceCS, targetCS);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
                e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
                throw e;
            }
        }
        // TODO: implement non-2D cases (1D, 3D, 4D...)
        throw new CannotCreateTransformException("Not implemented");
    }

    /**
     * Returns an affine transform between two coordinate systems. Only units and
     * axis order (e.g. transforming from (NORTH,WEST) to (EAST,NORTH)) are taken
     * in account. Other attributes (especially the datum) must be checked before
     * invoking this method.
     */
    private Matrix createAffineTransform(final CoordinateSystem sourceCS, final CoordinateSystem targetCS) throws CannotCreateTransformException
    {
        final AxisOrientation[] sourceAxis = new AxisOrientation[sourceCS.getDimension()];
        final AxisOrientation[] targetAxis = new AxisOrientation[targetCS.getDimension()];
        for (int i=0; i<sourceAxis.length; i++) sourceAxis[i]=sourceCS.getAxis(i).orientation;
        for (int i=0; i<targetAxis.length; i++) targetAxis[i]=targetCS.getAxis(i).orientation;
        final Matrix matrix;
        try
        {
            matrix = Matrix.createAffineTransform(sourceAxis, targetAxis);
        }
        catch (RuntimeException exception)
        {
            final CannotCreateTransformException e = new CannotCreateTransformException(sourceCS, targetCS);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw e;
        }

        // Convert units (Optimized case where the conversion
        // can be applied right into the AffineTransform).
        final int dimension = matrix.getNumRows()-1;
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert dimension == matrix.getNumColumns()-1;
------- END OF JDK 1.4 DEPENDENCIES ---*/
        for (int i=0; i<dimension; i++)
        {
            // TODO: check if units conversion is really linear.
            final Unit sourceUnit = sourceCS.getUnits(i);
            final Unit targetUnit = targetCS.getUnits(i);
            final double offset = targetUnit.convert(0, sourceUnit);
            final double scale  = targetUnit.convert(1, sourceUnit)-offset;
            matrix.set(i,i,         scale*matrix.get(i,i));
            matrix.set(i,dimension, scale*matrix.get(i,dimension)+offset);
        }
        return matrix;
    }

    /**
     * Creates a transformation between two geographic coordinate systems.
     * This method is automatically invoked by <code>createFromCoordinateSystems</code>. The default
     * implementation can adjust axis order and orientation (e.g. transforming from (NORTH,WEST) to
     * (EAST,NORTH)), adjust for prime meridian and perform units conversion.
     *
     * @param  sourceCS Input coordinate system.
     * @param  targetCS Output coordinate system.
     * @return A coordinate transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if no transformation path has been found.
     */
    protected CoordinateTransformation createTransformationStep(final GeographicCoordinateSystem sourceCS, final GeographicCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        if (!Utilities.equals(sourceCS.getHorizontalDatum(), targetCS.getHorizontalDatum()))
        {
            // TODO: implement transformation using WGS84ConversionInfo.
            throw new CannotCreateTransformException("Not implemented");
        }
        final Matrix matrix = createAffineTransform(sourceCS, targetCS);
        for (int i=targetCS.getDimension(); --i>=0;)
        {
            // Find longitude ordinate, and apply a rotation if prime meridian are different.
            final AxisOrientation orientation = targetCS.getAxis(i).orientation;
            if (AxisOrientation.EAST.equals(orientation.absolute()))
            {
                final Unit              unit = targetCS.getUnits(i);
                final double sourceLongitude = sourceCS.getPrimeMeridian().getLongitude(unit);
                final double targetLongitude = targetCS.getPrimeMeridian().getLongitude(unit);
                final int   lastMatrixColumn = matrix.getNumColumns()-1;
                double rotate = targetLongitude - sourceLongitude;
                if (AxisOrientation.WEST.equals(orientation)) rotate = -rotate;
                matrix.set(i, lastMatrixColumn, matrix.get(i, lastMatrixColumn)-rotate);
            }
        }
        final MathTransform transform = getMathTransformFactory().createAffineTransform(matrix);
        return createFromMathTransform(sourceCS, targetCS, TransformType.CONVERSION, transform);
    }

    /**
     * Creates a transformation between two projected coordinate systems.
     * This method is automatically invoked by <code>createFromCoordinateSystems</code>. The default
     * implementation can adjust axis order and orientation. It also performs units conversion if it
     * is the only extra change needed. Otherwise, it performs three steps:
     *
     * <ol>
     *   <li>Unproject <code>sourceCS</code>.</li>
     *   <li>Transform from <code>sourceCS.geographicCS</code> to <code>targetCS.geographicCS</code>.</li>
     *   <li>Project <code>targetCS</code>.</li>
     * </ol>
     *
     * @param  sourceCS Input coordinate system.
     * @param  targetCS Output coordinate system.
     * @return A coordinate transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if no transformation path has been found.
     */
    protected CoordinateTransformation createTransformationStep(final ProjectedCoordinateSystem sourceCS, final ProjectedCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        if (Utilities.equals(sourceCS.getProjection(),      targetCS.getProjection()) &&
            Utilities.equals(sourceCS.getHorizontalDatum(), targetCS.getHorizontalDatum()))
        {
            // This special case is necessary for createTransformationStep(GeographicCS,ProjectedCS)
            final MathTransformFactory factory = getMathTransformFactory();
            final MathTransform transform = factory.createAffineTransform(createAffineTransform(sourceCS, targetCS));
            return createFromMathTransform(sourceCS, targetCS, TransformType.CONVERSION, transform);
        }
        final GeographicCoordinateSystem sourceGeo = sourceCS.getGeographicCoordinateSystem();
        final GeographicCoordinateSystem targetGeo = targetCS.getGeographicCoordinateSystem();
        final CoordinateTransformation step1 = createFromCoordinateSystems(sourceCS,  sourceGeo);
        final CoordinateTransformation step2 = createFromCoordinateSystems(sourceGeo, targetGeo);
        final CoordinateTransformation step3 = createFromCoordinateSystems(targetGeo, targetCS );
        return concatenate(step1, step2, step3);
    }

    /**
     * Creates a transformation between a geographic and a projected coordinate systems.
     * This method is automatically invoked by <code>createFromCoordinateSystems</code>.
     *
     * @param  sourceCS Input coordinate system.
     * @param  targetCS Output coordinate system.
     * @return A coordinate transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if no transformation path has been found.
     */
    protected CoordinateTransformation createTransformationStep(final GeographicCoordinateSystem sourceCS, final ProjectedCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        final Projection      projection = targetCS.getProjection();
        GeographicCoordinateSystem geoCS = targetCS.getGeographicCoordinateSystem();
        ProjectedCoordinateSystem  prjCS = targetCS;
        if (!isStandard(geoCS, Unit.DEGREE) || geoCS.getPrimeMeridian().getLongitude(Unit.DEGREE)!=0)
        {
            geoCS = new GeographicCoordinateSystem(geoCS.getName(null), geoCS.getHorizontalDatum());
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert isStandard(geoCS, Unit.DEGREE);
------- END OF JDK 1.4 DEPENDENCIES ---*/
        }
        if (!isStandard(prjCS, Unit.METRE))
        {
            prjCS = new ProjectedCoordinateSystem(prjCS.getName(null), geoCS, projection);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert isStandard(prjCS, Unit.METRE);
------- END OF JDK 1.4 DEPENDENCIES ---*/
        }
        final MathTransform    mapProjection = factory.createParameterizedTransform(projection.getClassName(), projection.getParameters());
        final CoordinateTransformation step1 = createFromCoordinateSystems(sourceCS, geoCS);
        final CoordinateTransformation step2 = createFromMathTransform(geoCS, prjCS, TransformType.CONVERSION, mapProjection);
        final CoordinateTransformation step3 = createFromCoordinateSystems(prjCS, targetCS);
        return concatenate(step1, step2, step3);
    }

    /**
     * Concatenate three steps.
     */
    private CoordinateTransformation concatenate(final CoordinateTransformation step1, final CoordinateTransformation step2, final CoordinateTransformation step3)
    {
        if (!step1.getTargetCS().equivalents(step2.getSourceCS()) ||
            !step2.getTargetCS().equivalents(step3.getSourceCS()))
        {
            throw new IllegalArgumentException();
        }
        final MathTransformFactory factory = getMathTransformFactory();
        final MathTransform step  = factory.createConcatenatedTransform(step1.getMathTransform(),
                                    factory.createConcatenatedTransform(step2.getMathTransform(),
                                                                        step3.getMathTransform()));
        final TransformType type  = step1.getTransformType().concatenate(
                                    step2.getTransformType().concatenate(
                                    step3.getTransformType()));
        return createFromMathTransform(step1.getSourceCS(), step3.getTargetCS(), type, step);
    }

    /**
     * Create a coordinate transform from a math transform.
     * If the specified math transform is already a coordinate transform,  and if source
     * and target coordinate systems match, then <code>transform</code> is returned with
     * no change. Otherwise, a new coordinate transform is created.
     *
     * @param  sourceCS  The source coordinate system.
     * @param  targetCS  The destination coordinate system.
     * @param  type      The transform type.
     * @param  transform The math transform.
     * @return A coordinate transform using the specified math transform.
     */
    private static CoordinateTransformation createFromMathTransform(final CoordinateSystem sourceCS, final CoordinateSystem targetCS,
                                                                    final TransformType type, final MathTransform transform)
    {
        if (transform instanceof CoordinateTransformation)
        {
            final CoordinateTransformation ct = (CoordinateTransformation) transform;
            if (Utilities.equals(ct.getSourceCS(), sourceCS) &&
                Utilities.equals(ct.getTargetCS(), targetCS))
            {
                return ct;
            }
        }
        return (CoordinateTransformation) MathTransformFactory.pool.intern(new CoordinateTransformation(null, sourceCS, targetCS, type, transform));
    }

    /**
     * Returns <code>true</code> if the specified coordinate system
     * use standard axis and standard units.
     *
     * @param cs   The coordinate system to test.
     * @paral unit The standard units.
     */
    private static boolean isStandard(final HorizontalCoordinateSystem cs, final Unit unit)
    {
        return unit                 .equals(cs.getUnits(0))             &&
               unit                 .equals(cs.getUnits(1))             &&
               AxisOrientation.EAST .equals(cs.getAxis (0).orientation) &&
               AxisOrientation.NORTH.equals(cs.getAxis (1).orientation);
    }
}
