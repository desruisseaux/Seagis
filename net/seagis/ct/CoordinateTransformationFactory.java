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
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.Projection;
import net.seagis.cs.PrimeMeridian;
import net.seagis.cs.HorizontalDatum;
import net.seagis.cs.AxisOrientation;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.WGS84ConversionInfo;
import net.seagis.cs.ProjectedCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.cs.HorizontalCoordinateSystem;
import net.seagis.pt.Dimensioned;
import net.seagis.resources.Utilities;

// Miscellaneous
import javax.units.Unit;
import java.util.Arrays;
import java.awt.geom.AffineTransform;


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
     * Number for temporary created objects. This number is
     * incremented each time {@link #getTemporaryName} is
     * invoked.
     */
    private static volatile int temporaryID;

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
     * Returns the underlying math transform factory. This factory
     * is used for constructing {@link MathTransform} objects for
     * all {@link CoordinateTransformation}.
     */
    public MathTransformFactory getMathTransformFactory()
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
            if (targetCS instanceof GeographicCoordinateSystem)
            {
                return createTransformationStep(source, (GeographicCoordinateSystem) targetCS);
            }
        }
        // TODO: implement non-2D cases (1D, 3D, 4D...)
        throw new CannotCreateTransformException("Not implemented");
    }

    /**
     * Creates a transformation between two geographic coordinate systems.
     * This method is automatically invoked by <code>createFromCoordinateSystems</code>. The default
     * implementation can adjust axis order and orientation (e.g. transforming from (NORTH,WEST) to
     * (EAST,NORTH)), adjust for prime meridian, performs units conversion and apply Bursa Wolf
     * transformation.
     *
     * @param  sourceCS Input coordinate system.
     * @param  targetCS Output coordinate system.
     * @return A coordinate transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if no transformation path has been found.
     */
    protected CoordinateTransformation createTransformationStep(final GeographicCoordinateSystem sourceCS, final GeographicCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        final AffineTransform matrix = createAffineTransformToWGS84(sourceCS);
        if (matrix==null)
        {
            // TODO: fallback on GeocentricCoordinateSystem
            throw new CannotCreateTransformException("Need Bursa Wolf parameters");
        }
        final AffineTransform targetToWGS84 = createAffineTransformToWGS84(targetCS);
        if (targetToWGS84==null)
        {
            // TODO: fallback on GeocentricCoordinateSystem
            throw new CannotCreateTransformException("Need Bursa Wolf parameters");
        }
        try
        {
            // Apply transform from source to WGS84 first (the current
            // 'matrix' matrix), then the transform from WGS84 to target.
            matrix.preConcatenate(targetToWGS84.createInverse());
        }
        catch (java.awt.geom.NoninvertibleTransformException exception)
        {
            final CannotCreateTransformException e = new CannotCreateTransformException(sourceCS, targetCS);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw e;
        }
        // TODO: Need to bring back the longitude in the [-180,180) range
        //       and the latitude to the [-90,90] range, with longitude==0
        //       if abs(latitude)==90.
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
        final GeographicCoordinateSystem sourceGeo = sourceCS.getGeographicCoordinateSystem();
        final GeographicCoordinateSystem targetGeo = targetCS.getGeographicCoordinateSystem();
        if (Utilities.equals(sourceCS.getProjection(),      targetCS.getProjection())      &&
            Utilities.equals(sourceCS.getHorizontalDatum(), targetCS.getHorizontalDatum()))
        {
            // This special case is necessary for createTransformationStep(GeographicCS,ProjectedCS).
            // 'swapAndScaleAxis(...) takes care of axis orientation and units. Datum and projection
            // have just been checked above. Prime meridien is not checked (TODO: should we do???)
            final Matrix matrix = swapAndScaleAxis(sourceCS, targetCS);
            final MathTransform transform = getMathTransformFactory().createAffineTransform(matrix);
            return createFromMathTransform(sourceCS, targetCS, TransformType.CONVERSION, transform);
        }
        final CoordinateTransformation step1 = createTransformationStep(sourceCS,  sourceGeo);
        final CoordinateTransformation step2 = createTransformationStep(sourceGeo, targetGeo);
        final CoordinateTransformation step3 = createTransformationStep(targetGeo, targetCS );
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
        final ProjectedCoordinateSystem stepProjCS = normalize(targetCS);
        final GeographicCoordinateSystem stepGeoCS = stepProjCS.getGeographicCoordinateSystem();
        final Projection                projection = stepProjCS.getProjection();
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert normalize(stepProjCS) == stepProjCS;
        assert normalize(stepGeoCS, projection) == stepGeoCS;
        assert projection.equals(targetCS.getProjection());
------- END OF JDK 1.4 DEPENDENCIES ---*/

        final MathTransform    mapProjection = getMathTransformFactory().createParameterizedTransform(projection);
        final CoordinateTransformation step1 = createTransformationStep(sourceCS, stepGeoCS);
        final CoordinateTransformation step2 = createFromMathTransform(stepGeoCS, stepProjCS, TransformType.CONVERSION, mapProjection);
        final CoordinateTransformation step3 = createTransformationStep(stepProjCS, targetCS);
        return concatenate(step1, step2, step3);
    }

    /**
     * Creates a transformation between a projected and a geographic coordinate systems. This method is
     * automatically invoked by <code>createFromCoordinateSystems</code>. the default implementation returns
     * <code>{@link #createTransformationStep(GeographicCoordinateSystem, ProjectedCoordinateSystem)
     * createTransformationStep}(targetCS, sourceCS).{@link MathTransform#inverse() inverse()}</code>.
     *
     * @param  sourceCS Input coordinate system.
     * @param  targetCS Output coordinate system.
     * @return A coordinate transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if no transformation path has been found.
     */
    protected CoordinateTransformation createTransformationStep(final ProjectedCoordinateSystem sourceCS, final GeographicCoordinateSystem targetCS) throws CannotCreateTransformException
    {
        try
        {
            return createTransformationStep(targetCS, sourceCS).inverse();
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

    /**
     * Concatenate three steps.
     */
    private CoordinateTransformation concatenate(final CoordinateTransformation step1, final CoordinateTransformation step2, final CoordinateTransformation step3)
    {
        if (!step1.getTargetCS().equivalents(step2.getSourceCS()))
        {
            // Current message is for debugging purpose only.
            throw new IllegalArgumentException(String.valueOf(step1));
        }
        if (!step2.getTargetCS().equivalents(step3.getSourceCS()))
        {
            // Current message is for debugging purpose only.
            throw new IllegalArgumentException(String.valueOf(step3));
        }
        final MathTransformFactory factory = getMathTransformFactory();
        final MathTransform step = factory.createConcatenatedTransform(step1.getMathTransform(),
                                   factory.createConcatenatedTransform(step2.getMathTransform(),
                                                                       step3.getMathTransform()));
        final TransformType type = step1.getTransformType().concatenate(
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
     * Returns an affine transform between an arbitrary geographic coordinate system
     * and the {@link GeographicCoordinateSystem#WGS84}. If no transform can't be
     * computed because the Bursa Wolf transformation is unknow, then this method
     * returns <code>null</code>.
     */
    private AffineTransform createAffineTransformToWGS84(final GeographicCoordinateSystem geoCS) throws CannotCreateTransformException
    {
        final HorizontalDatum    datum = geoCS.getHorizontalDatum();
        final Ellipsoid      ellipsoid = datum.getEllipsoid();
        final WGS84ConversionInfo wolf = datum.getWGS84Parameters();
        final double          meridian = geoCS.getPrimeMeridian().getLongitude(Unit.DEGREE);
        if (wolf==null && !ellipsoid.equals(Ellipsoid.WGS84))
        {
            // Bursa Wolf transformation is unknow.
            return null;
        }
        final AffineTransform matrix = swapAndScaleAxis(geoCS, GeographicCoordinateSystem.WGS84).toAffineTransform2D();
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert !geoCS.equals(GeographicCoordinateSystem.WGS84) || matrix.isIdentity();
------- END OF JDK 1.4 DEPENDENCIES ---*/
        // The following is equivalents to swap axis order first (the previous 'matrix' operation,
        // which ensures that axis are in (x,y) order), and then apply Bursa Wolf transformation.
        // The last operation rotate the system to bring the prime meridian on Greenwich.
        if (wolf!=null)
        {
            // WRONG!!! Bursa Wolf parameters my be applied on GeocentricCoordinateSystem!
            // matrix.preConcatenate(wolf.getAffineTransform2D());
            throw new CannotCreateTransformException("Not implemented"); // TODO
        }
        if (meridian!=0) matrix.preConcatenate(AffineTransform.getTranslateInstance(meridian, 0));
        return matrix;
    }

    /**
     * Returns an affine transform between two coordinate systems. Only units and
     * axis order (e.g. transforming from (NORTH,WEST) to (EAST,NORTH)) are taken
     * in account. Other attributes (especially the datum) must be checked before
     * invoking this method.
     *
     * @param sourceCS The source coordinate system. If <code>null</code>, then
     *        (x,y,z,t) axis order is assumed.
     * @param targetCS The target coordinate system. If <code>null</code>, then
     *        (x,y,z,t) axis order is assumed.
     */
    private Matrix swapAndScaleAxis(final CoordinateSystem sourceCS, final CoordinateSystem targetCS) throws CannotCreateTransformException
    {
        final AxisOrientation[] sourceAxis = getAxisOrientations(sourceCS, targetCS);
        final AxisOrientation[] targetAxis = getAxisOrientations(targetCS, sourceCS);
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
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert !Arrays.equals(sourceAxis, targetAxis) || matrix.isIdentity();
------- END OF JDK 1.4 DEPENDENCIES ---*/

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
     * Returns the axis orientation for the specified coordinate system.
     * If <code>cs</code> is <code>null</code>, then an array of length
     * <code>dim.getDimension()</code> is created and filled with
     * <code>(x,y,z,t)</code> axis orientation.
     */
    private static AxisOrientation[] getAxisOrientations(final CoordinateSystem cs, final Dimensioned dim)
    {
        final AxisOrientation[] axis;
        if (cs!=null)
        {
            axis = new AxisOrientation[cs.getDimension()];
            for (int i=0; i<axis.length; i++)
                axis[i] = cs.getAxis(i).orientation;
        }
        else
        {
            axis = new AxisOrientation[dim.getDimension()];
            switch (axis.length)
            {
                default: for (int i=4; i<axis.length; i++) axis[i]=AxisOrientation.OTHER; // fall through
                case 4:  axis[3] = AxisOrientation.FUTURE; // fall through
                case 3:  axis[2] = AxisOrientation.UP;     // fall through
                case 2:  axis[1] = AxisOrientation.NORTH;  // fall through
                case 1:  axis[0] = AxisOrientation.EAST;   // fall through
                case 0:  break;
            }
        }
        return axis;
    }

    /**
     * Makes sure that the specified {@link GeographicCoordinateSystem} use standard axis
     * (longitude and latitude in degrees), Greenwich prime meridian and an ellipsoid
     * matching projection's parameters. If <code>cs</code> already meets all those
     * conditions, then it is returned unchanged. Otherwise, a new normalized geographic
     * coordinate system is created and returned.
     */
    private static GeographicCoordinateSystem normalize(GeographicCoordinateSystem cs, final Projection projection)
    {
        final double semiMajor = projection.getValue("semi_major");
        final double semiMinor = projection.getValue("semi_minor");
        HorizontalDatum  datum = cs.getHorizontalDatum();
        Ellipsoid    ellipsoid = datum.getEllipsoid();
        if (!hasStandardAxis(cs, Unit.DEGREE) || cs.getPrimeMeridian().getLongitude(Unit.DEGREE)!=0)
        {
            cs = null; // Signal that it needs to be reconstructed.
        }
        if (ellipsoid.getSemiMajorAxis()!=semiMajor || ellipsoid.getSemiMinorAxis()!=semiMinor)
        {
            // Those objects are temporary. We assume it is not
            // a big deal if their name are not very explicit...
            ellipsoid = new Ellipsoid(getTemporaryName(), semiMajor, semiMinor, Unit.METRE);
            datum     = new HorizontalDatum(getTemporaryName(), ellipsoid);
            cs        = null; // Signal that it needs to be reconstructed.
        }
        if (cs != null)
        {
            return cs;
        }
        return new GeographicCoordinateSystem(getTemporaryName(), datum);
    }

    /**
     * Makes sure that a {@link ProjectedCoordinateSystem} use standard axis
     * (x and y in metres) and a normalized {@link GeographicCoordinateSystem}.
     * If <code>cs</code> already meets all those conditions, then it is
     * returned unchanged. Otherwise, a new normalized projected coordinate
     * system is created and returned.
     */
    private static ProjectedCoordinateSystem normalize(final ProjectedCoordinateSystem cs)
    {
        final Projection                      projection = cs.getProjection();
        final GeographicCoordinateSystem           geoCS = cs.getGeographicCoordinateSystem();
        final GeographicCoordinateSystem normalizedGeoCS = normalize(geoCS, projection);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert normalize(normalizedGeoCS, projection) == normalizedGeoCS;
------- END OF JDK 1.4 DEPENDENCIES ---*/

        if (hasStandardAxis(cs, Unit.METRE) && normalizedGeoCS==geoCS)
        {
            return cs;
        }
        return new ProjectedCoordinateSystem(getTemporaryName(), normalizedGeoCS, projection);
    }

    /**
     * Returns <code>true</code> if the specified coordinate
     * system use standard axis and standard units.
     *
     * @param cs   The coordinate system to test.
     * @paral unit The standard units.
     */
    private static boolean hasStandardAxis(final HorizontalCoordinateSystem cs, final Unit unit)
    {
        return unit                 .equals(cs.getUnits(0))             &&
               unit                 .equals(cs.getUnits(1))             &&
               AxisOrientation.EAST .equals(cs.getAxis (0).orientation) &&
               AxisOrientation.NORTH.equals(cs.getAxis (1).orientation);
    }

    /**
     * Returns a temporary name for generated objects. The first object
     * has a name like "Temporary-1", the second is "Temporary-2", etc.
     */
    private static String getTemporaryName()
    {return "Temporary-" + (++temporaryID);}
}
