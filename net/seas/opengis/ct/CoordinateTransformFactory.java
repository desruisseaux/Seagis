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
package net.seas.opengis.ct;

// OpenGIS (SEAGIS) dependencies
import net.seas.opengis.pt.Matrix;
import net.seas.opengis.cs.AxisInfo;
import net.seas.opengis.cs.Projection;
import net.seas.opengis.cs.PrimeMeridian;
import net.seas.opengis.cs.AxisOrientation;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.ProjectedCoordinateSystem;
import net.seas.opengis.cs.GeographicCoordinateSystem;
import net.seas.opengis.cs.HorizontalCoordinateSystem;

// Miscellaneous
import javax.units.Unit;
import net.seas.util.XClass;
import net.seas.util.Version;


/**
 * Creates coordinate transformations.
 *
 * @version 1.0
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_CoordinateTransformationFactory
 */
public class CoordinateTransformFactory
{
    /**
     * The default coordinate transformation factory.
     */
    public static final CoordinateTransformFactory DEFAULT = new CoordinateTransformFactory(MathTransformFactory.DEFAULT);

    /**
     * The underlying math transform factory.
     */
    private final MathTransformFactory factory;

    /**
     * Construct a coordinate transformation factory.
     *
     * @param factory The math transform factory to use.
     */
    public CoordinateTransformFactory(final MathTransformFactory factory)
    {this.factory = factory;}

    /**
     * Returns the underlying math transform factory.
     */
    public MathTransformFactory getMathTransformFactory()
    {return factory;}

    /**
     * Create a coordinate transform from a math transform.
     * If the specified math transform is already a coordinate transform,  and if source
     * and target coordinate systems match, then <code>transform</code> is returned with
     * no change. Otherwise, a new coordinate transform is created.
     *
     * @param  transform The math transform.
     * @param  type      The transform type.
     * @param  sourceCS  The source coordinate system.
     * @param  targetCS  The destination coordinate system.
     * @return A coordinate transform using the specified math transform.
     */
    public CoordinateTransform createFromMathTransform(final MathTransform transform, final TransformType type, final CoordinateSystem sourceCS, final CoordinateSystem targetCS)
    {
        if (transform instanceof CoordinateTransform)
        {
            final CoordinateTransform ct = (CoordinateTransform) transform;
            if (XClass.equals(ct.getSourceCS(), sourceCS) &&
                XClass.equals(ct.getTargetCS(), targetCS))
            {
                return ct;
            }
        }
        return (CoordinateTransform) MathTransformFactory.pool.intern(new CoordinateTransformProxy(transform, type, sourceCS, targetCS));
    }

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
    public CoordinateTransform createFromCoordinateSystems(final CoordinateSystem sourceCS, final CoordinateSystem targetCS) throws CannotCreateTransformException
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
                // TODO: remove cast if compiler bug gets fixed (see CoordinateTransform.inverse()).
                return (CoordinateTransform) createTransformationStep((GeographicCoordinateSystem) targetCS, source).inverse();
            }
            catch (NoninvertibleTransformException exception)
            {
                final CannotCreateTransformException e = new CannotCreateTransformException(sourceCS, targetCS);
                if (Version.MINOR>=4) e.initCause(exception);
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
            if (Version.MINOR>=4) e.initCause(exception);
            throw e;
        }

        // Convert units (Optimized case where the conversion
        // can be applied right into the AffineTransform).
        final int dimension = matrix.getSize()-1;
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
    protected CoordinateTransform createTransformationStep(final GeographicCoordinateSystem sourceCS, final GeographicCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        if (!XClass.equals(sourceCS.getHorizontalDatum(), targetCS.getHorizontalDatum()))
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
                final int   lastMatrixColumn = matrix.getSize()-1;
                double rotate = targetLongitude - sourceLongitude;
                if (AxisOrientation.WEST.equals(orientation)) rotate = -rotate;
                matrix.set(i, lastMatrixColumn, matrix.get(i, lastMatrixColumn)-rotate);
            }
        }
        return createFromMathTransform(getMathTransformFactory().createAffineTransform(matrix), TransformType.CONVERSION, sourceCS, targetCS);
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
    protected CoordinateTransform createTransformationStep(final ProjectedCoordinateSystem sourceCS, final ProjectedCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        if (XClass.equals(sourceCS.getProjection(),      targetCS.getProjection()) &&
            XClass.equals(sourceCS.getHorizontalDatum(), targetCS.getHorizontalDatum()))
        {
            // This special case is necessary for createTransformationStep(GeographicCS,ProjectedCS)
            final MathTransformFactory factory = getMathTransformFactory();
            final MathTransform transform = factory.createAffineTransform(createAffineTransform(sourceCS, targetCS));
            return createFromMathTransform(transform, TransformType.CONVERSION, sourceCS, targetCS);
        }
        final GeographicCoordinateSystem sourceGeo = sourceCS.getGeographicCoordinateSystem();
        final GeographicCoordinateSystem targetGeo = targetCS.getGeographicCoordinateSystem();
        final CoordinateTransform step1 = createFromCoordinateSystems(sourceCS,  sourceGeo);
        final CoordinateTransform step2 = createFromCoordinateSystems(sourceGeo, targetGeo);
        final CoordinateTransform step3 = createFromCoordinateSystems(targetGeo, targetCS );
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
    protected CoordinateTransform createTransformationStep(final GeographicCoordinateSystem sourceCS, final ProjectedCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        final Projection      projection = targetCS.getProjection();
        GeographicCoordinateSystem geoCS = targetCS.getGeographicCoordinateSystem();
        ProjectedCoordinateSystem  prjCS = targetCS;
        if (!isStandard(geoCS, Unit.DEGREE) || geoCS.getPrimeMeridian().getLongitude(Unit.DEGREE)!=0)
        {
            geoCS = new GeographicCoordinateSystem(geoCS.getName(null), geoCS.getHorizontalDatum());
            assert(isStandard(geoCS, Unit.DEGREE));
        }
        if (!isStandard(prjCS, Unit.METRE))
        {
            prjCS = new ProjectedCoordinateSystem(prjCS.getName(null), geoCS, projection);
            assert(isStandard(prjCS, Unit.METRE));
        }
        final CoordinateTransform step1 = createFromCoordinateSystems(sourceCS, geoCS);
        final MathTransform       step2 = factory.createParameterizedTransform(projection.getClassName(), projection.getParameters());
        final CoordinateTransform step3 = createFromCoordinateSystems(prjCS, targetCS);
        if (step2 instanceof MapProjection)
        {
            final MapProjection proj = (MapProjection) step2;
            proj.sourceCS = geoCS;
            proj.targetCS = prjCS;
        }
        return concatenate(step1, createFromMathTransform(step2, TransformType.CONVERSION, geoCS, prjCS), step3);
    }

    /**
     * Concatenate three steps.
     */
    private CoordinateTransform concatenate(final CoordinateTransform step1, final CoordinateTransform step2, final CoordinateTransform step3)
    {
        final MathTransformFactory factory = getMathTransformFactory();
        final MathTransform step  = factory.createConcatenatedTransform(step1,
                                    factory.createConcatenatedTransform(step2, step3));
        final TransformType type  = step1.getTransformType().concatenate(
                                    step2.getTransformType().concatenate(
                                    step3.getTransformType()));
        return createFromMathTransform(step, type, step1.getSourceCS(), step3.getTargetCS());
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
