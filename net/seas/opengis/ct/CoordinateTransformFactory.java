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
import net.seas.opengis.cs.AxisInfo;
import net.seas.opengis.cs.Projection;
import net.seas.opengis.cs.AxisOrientation;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.ProjectedCoordinateSystem;
import net.seas.opengis.cs.GeographicCoordinateSystem;

// Miscellaneous
import javax.units.Unit;


/**
 * Creates coordinate transformations.
 *
 * @version 1.0
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
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
     * <FONT COLOR="#FF6633">Returns the underlying math transform factory.</FONT>
     */
    public MathTransformFactory getMathTransformFactory()
    {return factory;}

    /**
     * Creates a transformation between two coordinate systems.
     * This method will examine the coordinate systems in order to construct a
     * transformation between them. This method may fail if no path between the
     * coordinate systems is found.
     *
     * @param sourceCS Input coordinate system.
     * @param targetCS Output coordinate system.
     */
    public CoordinateTransform createFromCoordinateSystems(final CoordinateSystem sourceCS, final CoordinateSystem targetCS)
    {
        if (sourceCS instanceof GeographicCoordinateSystem)
        {
            final GeographicCoordinateSystem source = (GeographicCoordinateSystem) sourceCS;
            if (targetCS instanceof GeographicCoordinateSystem)
            {
                return create(source, (GeographicCoordinateSystem) targetCS);
            }
            if (targetCS instanceof ProjectedCoordinateSystem)
            {
                return create(source, (ProjectedCoordinateSystem) targetCS);
            }
        }
        if (sourceCS instanceof ProjectedCoordinateSystem)
        {
            final ProjectedCoordinateSystem source = (ProjectedCoordinateSystem) sourceCS;
            if (targetCS instanceof GeographicCoordinateSystem)
            {
            //  return create(source, (GeographicCoordinateSystem) targetCS);
            }
            if (targetCS instanceof ProjectedCoordinateSystem)
            {
                return create(source, (ProjectedCoordinateSystem) targetCS);
            }
        }
        throw new UnsupportedOperationException(); // TODO
    }

    /**
     * <FONT COLOR="#FF6633">Creates a transformation between two geographic coordinate systems.</FONT>
     * This method is automatically invoked by <code>createFromCoordinateSystems</code>.
     */
    protected CoordinateTransform create(final GeographicCoordinateSystem sourceCS, final GeographicCoordinateSystem targetCS)
    {
        if (sourceCS.equals(targetCS)) // TODO: "equals" is to severe, since it also check name and remaks.
        {
            return new CoordinateTransformProxy(TransformType.CONVERSION, sourceCS, targetCS, AffineTransform2D.IDENTITY);
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * <FONT COLOR="#FF6633">Creates a transformation between two projected coordinate systems.</FONT>
     * This method is automatically invoked by <code>createFromCoordinateSystems</code>.
     */
    protected CoordinateTransform create(final ProjectedCoordinateSystem sourceCS, final ProjectedCoordinateSystem targetCS)
    {
        if (sourceCS.equals(targetCS)) // TODO: "equals" is to severe, since it also check name and remaks.
        {
            return new CoordinateTransformProxy(TransformType.CONVERSION, sourceCS, targetCS, AffineTransform2D.IDENTITY);
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * <FONT COLOR="#FF6633">Creates a transformation between a geographic and a projected coordinate systems.</FONT>
     * This  method is automatically invoked by <code>createFromCoordinateSystems</code>.
     */
    protected CoordinateTransform create(final GeographicCoordinateSystem sourceCS, final ProjectedCoordinateSystem targetCS)
    {
        if (!sourceCS.equals(targetCS.getGeographicCoordinateSystem()))
        {
            // TODO: call "create(GeographicCS, GeographicCS) instead.
            throw new UnsupportedOperationException("Not implemented");
        }
        if (sourceCS.getPrimeMeridian().getLongitude() != 0                  ||
            !Unit.DEGREE            .equals(sourceCS.getAngularUnit())       ||
            !Unit.METRE             .equals(targetCS.getLinearUnit())        ||
            !AxisOrientation.EAST   .equals(sourceCS.getAxis(0).orientation) ||
            !AxisOrientation.EAST   .equals(targetCS.getAxis(0).orientation) ||
            !AxisOrientation.NORTH  .equals(sourceCS.getAxis(1).orientation) ||
            !AxisOrientation.NORTH  .equals(targetCS.getAxis(1).orientation))
        {
            throw new UnsupportedOperationException("Not implemented");
        }
        final Projection   projection = targetCS.getProjection();
        final MathTransform transform = getMathTransformFactory().createParameterizedTransform(projection.getClassName(), projection.getParameters());
        if (transform instanceof MapProjection)
        {
            final MapProjection proj = (MapProjection) transform;
            proj.sourceCS = sourceCS;
            proj.targetCS = targetCS;
            return proj;
        }
        return new CoordinateTransformProxy(TransformType.CONVERSION, sourceCS, targetCS, transform);
    }
}
