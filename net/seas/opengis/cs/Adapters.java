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
package net.seas.opengis.cs;

// OpenGIS dependencies
import org.opengis.cs.CS_Info;
import org.opengis.cs.CS_Ellipsoid;
import org.opengis.cs.CS_PrimeMeridian;
import org.opengis.cs.CS_CoordinateSystem;
import org.opengis.cs.CS_LocalCoordinateSystem;
import org.opengis.cs.CS_CompoundCoordinateSystem;
import org.opengis.cs.CS_VerticalCoordinateSystem;
import org.opengis.cs.CS_HorizontalCoordinateSystem;
import org.opengis.cs.CS_GeocentricCoordinateSystem;
import org.opengis.cs.CS_GeographicCoordinateSystem;
import org.opengis.cs.CS_ProjectedCoordinateSystem;
import org.opengis.cs.CS_CoordinateSystemFactory;

import org.opengis.cs.CS_Datum;
import org.opengis.cs.CS_DatumType;
import org.opengis.cs.CS_LocalDatum;
import org.opengis.cs.CS_VerticalDatum;
import org.opengis.cs.CS_HorizontalDatum;

import org.opengis.cs.CS_AxisInfo;
import org.opengis.cs.CS_AxisOrientationEnum;
import org.opengis.cs.CS_WGS84ConversionInfo;
import org.opengis.cs.CS_ProjectionParameter;
import org.opengis.cs.CS_Projection;

import org.opengis.cs.CS_Unit;
import org.opengis.cs.CS_LinearUnit;
import org.opengis.cs.CS_AngularUnit;

// OpenGIS dependencies (SEAS)
import net.seas.opengis.ct.Parameter;

// Remote Method Invocation
import javax.units.Unit;
import java.rmi.RemoteException;


/**
 * Provide static methods for interoperability with
 * <code>org.opengis.cs</code> package. All static
 * methods accept null argument.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Adapters
{
    /**
     * Do not allow creation of
     * instance of this class.
     */
    private Adapters()
    {}

    /**
     * Returns an OpenGIS interface for a coordinate system factory.
     */
    public static CS_CoordinateSystemFactory export(final CoordinateSystemFactory factory)
    {return (factory!=null) ? factory.toOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for an info. If the argument is an
     * <code>Info</code> subclass, the returned object will implements
     * the corresponding interface. For example a call with an argument
     * of type {@link GeographicCoordinateSystem} will returns an object
     * implementing the {@link CS_GeographicCoordinateSystem} interface.
     */
    public static CS_Info export(final Info info)
    {return (info!=null) ? (CS_Info)info.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a coordinate system. If the argument
     * is a <code>CoordinateSystem</code> subclass, the returned object will
     * implements the corresponding interface.
     */
    public static CS_CoordinateSystem export(final CoordinateSystem cs)
    {return (cs!=null) ? (CS_CoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a compound coordinate system.
     */
    public static CS_CompoundCoordinateSystem export(final CompoundCoordinateSystem cs)
    {return (cs!=null) ? (CS_CompoundCoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a local coordinate system.
     */
    public static CS_LocalCoordinateSystem export(final LocalCoordinateSystem cs)
    {return (cs!=null) ? (CS_LocalCoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a vertical coordinate system.
     */
    public static CS_VerticalCoordinateSystem export(final VerticalCoordinateSystem cs)
    {return (cs!=null) ? (CS_VerticalCoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a horizontal coordinate system.
     */
    public static CS_HorizontalCoordinateSystem export(final HorizontalCoordinateSystem cs)
    {return (cs!=null) ? (CS_HorizontalCoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a geographic coordinate system.
     */
    public static CS_GeographicCoordinateSystem export(final GeographicCoordinateSystem cs)
    {return (cs!=null) ? (CS_GeographicCoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a projected coordinate system.
     */
    public static CS_ProjectedCoordinateSystem export(final ProjectedCoordinateSystem cs)
    {return (cs!=null) ? (CS_ProjectedCoordinateSystem)cs.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a projection.
     */
    public static CS_Projection export(final Projection projection)
    {return (projection!=null) ? (CS_Projection)projection.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a prime meridien.
     */
    public static CS_PrimeMeridian export(final PrimeMeridian meridian)
    {return (meridian!=null) ? (CS_PrimeMeridian)meridian.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for an ellipsoid.
     */
    public static CS_Ellipsoid export(final Ellipsoid ellipsoid)
    {return (ellipsoid!=null) ? (CS_Ellipsoid)ellipsoid.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS enumeration for a datum type.
     */
    public static CS_DatumType export(final DatumType type)
    {return (type!=null) ? new CS_DatumType(type.value) : null;}

    /**
     * Returns an OpenGIS interface for a datum.
     */
    public static CS_Datum export(final Datum datum)
    {return (datum!=null) ? (CS_Datum)datum.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a datum.
     */
    public static CS_LocalDatum export(final LocalDatum datum)
    {return (datum!=null) ? (CS_LocalDatum)datum.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a datum.
     */
    public static CS_HorizontalDatum export(final HorizontalDatum datum)
    {return (datum!=null) ? (CS_HorizontalDatum)datum.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS interface for a datum.
     */
    public static CS_VerticalDatum export(final VerticalDatum datum)
    {return (datum!=null) ? (CS_VerticalDatum)datum.cachedOpenGIS() : null;}

    /**
     * Returns an OpenGIS enumeration for an axis orientation.
     */
    public static CS_AxisOrientationEnum export(final AxisOrientation orientation)
    {return (orientation!=null) ? new CS_AxisOrientationEnum(orientation.value) : null;}

    /**
     * Returns an OpenGIS structure for an axis info.
     */
    public static CS_AxisInfo export(final AxisInfo axis)
    {return (axis!=null) ? new CS_AxisInfo(axis.name, export(axis.orientation)) : null;}

    /**
     * Returns an OpenGIS structure for conversion info.
     */
    public static CS_WGS84ConversionInfo export(final WGS84ConversionInfo info)
    {
        if (info==null) return null;
        final CS_WGS84ConversionInfo nf = new CS_WGS84ConversionInfo();
        nf.dx        = info.dx;
        nf.dy        = info.dy;
        nf.dz        = info.dz;
        nf.ex        = info.ex;
        nf.ey        = info.ey;
        nf.ez        = info.ez;
        nf.ppm       = info.ppm;
        nf.areaOfUse = info.areaOfUse;
        return nf;
    }

    /**
     * Returns an OpenGIS interface for an unit. The returned interface may
     * extends {@link CS_LinearUnit} or {@link CS_AngularUnit} according
     * the specified unit.
     */
    public static CS_Unit export(final Unit unit)
    {
        if (unit==null) return null;
        if (unit.canConvert(Unit.METRE))
        {
            return new Info(unit.toString()).new LinearUnit(unit.convert(1, Unit.METRE));
        }
        if (unit.canConvert(Unit.DEGREE))
        {
            return new Info(unit.toString()).new AngularUnit(unit.convert(Math.PI/180, Unit.DEGREE));
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Returns info for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static Info wrap(final CS_Info info) throws RemoteException
    {
        if (info==null) return null;
        if (info instanceof CS_Datum)            return wrap((CS_Datum)           info);
        if (info instanceof CS_CoordinateSystem) return wrap((CS_CoordinateSystem)info);
        if (info instanceof Info.Export)
            return ((Info.Export)info).unwrap();
        return new Info(info);
    }

    /**
     * Returns a coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static CoordinateSystem wrap(final CS_CoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof   CS_CompoundCoordinateSystem) return wrap(  (CS_CompoundCoordinateSystem)cs);
        if (cs instanceof      CS_LocalCoordinateSystem) return wrap(     (CS_LocalCoordinateSystem)cs);
        if (cs instanceof   CS_VerticalCoordinateSystem) return wrap(  (CS_VerticalCoordinateSystem)cs);
        if (cs instanceof CS_HorizontalCoordinateSystem) return wrap((CS_HorizontalCoordinateSystem)cs);
        if (cs instanceof CoordinateSystem.Export)
            return (CoordinateSystem) ((CoordinateSystem.Export)cs).unwrap();
        throw new UnsupportedOperationException("Not implemented"); // CoordinateSystem is abstract
    }

    /**
     * Returns a compound coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static CompoundCoordinateSystem wrap(final CS_CompoundCoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof CoordinateSystem.Export)
            return (CompoundCoordinateSystem) ((CoordinateSystem.Export)cs).unwrap();
        return new CompoundCoordinateSystem(cs);
    }

    /**
     * Returns a local coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static LocalCoordinateSystem wrap(final CS_LocalCoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof CoordinateSystem.Export)
            return (LocalCoordinateSystem) ((CoordinateSystem.Export)cs).unwrap();
        return new LocalCoordinateSystem(cs);
    }

    /**
     * Returns a vertical coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static VerticalCoordinateSystem wrap(final CS_VerticalCoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof CoordinateSystem.Export)
            return (VerticalCoordinateSystem) ((CoordinateSystem.Export)cs).unwrap();
        return new VerticalCoordinateSystem(cs);
    }

    /**
     * Returns a horizontal coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static HorizontalCoordinateSystem wrap(final CS_HorizontalCoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof CS_GeographicCoordinateSystem) return wrap((CS_GeographicCoordinateSystem)cs);
        if (cs instanceof  CS_ProjectedCoordinateSystem) return wrap( (CS_ProjectedCoordinateSystem)cs);
        if (cs instanceof HorizontalCoordinateSystem.Export)
            return (HorizontalCoordinateSystem) ((HorizontalCoordinateSystem.Export)cs).unwrap();
        throw new UnsupportedOperationException("Not implemented"); // HorizontalCoordinateSystem is abstract
    }

    /**
     * Returns a geographic coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static GeographicCoordinateSystem wrap(final CS_GeographicCoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof HorizontalCoordinateSystem.Export)
            return (GeographicCoordinateSystem) ((HorizontalCoordinateSystem.Export)cs).unwrap();
        return new GeographicCoordinateSystem(cs);
    }

    /**
     * Returns a projected coordinate system for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static ProjectedCoordinateSystem wrap(final CS_ProjectedCoordinateSystem cs) throws RemoteException
    {
        if (cs==null) return null;
        if (cs instanceof HorizontalCoordinateSystem.Export)
            return (ProjectedCoordinateSystem) ((HorizontalCoordinateSystem.Export)cs).unwrap();
        return new ProjectedCoordinateSystem(cs);
    }

    /**
     * Returns a projection for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static Projection wrap(final CS_Projection projection) throws RemoteException
    {
        if (projection==null) return null;
        if (projection instanceof Info.Export)
            return (Projection) ((Info.Export)projection).unwrap();
        return new Projection(projection);
    }

    /**
     * Returns a prime meridian for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static PrimeMeridian wrap(final CS_PrimeMeridian meridian) throws RemoteException
    {
        if (meridian==null) return null;
        if (meridian instanceof Info.Export)
            return (PrimeMeridian) ((Info.Export)meridian).unwrap();
        return new PrimeMeridian(meridian);
    }

    /**
     * Returns an ellipsoid for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static Ellipsoid wrap(final CS_Ellipsoid ellipsoid) throws RemoteException
    {
        if (ellipsoid==null) return null;
        if (ellipsoid instanceof Info.Export)
            return (Ellipsoid) ((Info.Export)ellipsoid).unwrap();
        return new Ellipsoid(ellipsoid);
    }

    /**
     * Returns a datum type for an OpenGIS enumeration.
     */
    public static DatumType wrap(final CS_DatumType type)
    {return (type!=null) ? DatumType.getEnum(type.value) : null;}

    /**
     * Returns a datum for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static Datum wrap(final CS_Datum datum) throws RemoteException
    {
        if (datum==null) return null;
        if (datum instanceof      CS_LocalDatum) return wrap(     (CS_LocalDatum)datum);
        if (datum instanceof   CS_VerticalDatum) return wrap(  (CS_VerticalDatum)datum);
        if (datum instanceof CS_HorizontalDatum) return wrap((CS_HorizontalDatum)datum);
        if (datum instanceof Datum.Export)
            return (Datum) ((Datum.Export)datum).unwrap();
        return new Datum(datum);
    }

    /**
     * Returns a local datum for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static LocalDatum wrap(final CS_LocalDatum datum) throws RemoteException
    {
        if (datum==null) return null;
        if (datum instanceof Datum.Export)
            return (LocalDatum) ((Datum.Export)datum).unwrap();
        return new LocalDatum(datum);
    }

    /**
     * Returns a horizontal datum for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static HorizontalDatum wrap(final CS_HorizontalDatum datum) throws RemoteException
    {
        if (datum==null) return null;
        if (datum instanceof Datum.Export)
            return (HorizontalDatum) ((Datum.Export)datum).unwrap();
        return new HorizontalDatum(datum);
    }

    /**
     * Returns a vertical datum for an OpenGIS interface.
     * @throws RemoteException if a remote call failed.
     */
    public static VerticalDatum wrap(final CS_VerticalDatum datum) throws RemoteException
    {
        if (datum==null) return null;
        if (datum instanceof Datum.Export)
            return (VerticalDatum) ((Datum.Export)datum).unwrap();
        return new VerticalDatum(datum);
    }

    /**
     * Returns an axis orientation for an OpenGIS enumeration.
     */
    public static AxisOrientation wrap(final CS_AxisOrientationEnum orientation)
    {return (orientation!=null) ? AxisOrientation.getEnum(orientation.value) : null;}

    /**
     * Returns an axis info for an OpenGIS structure.
     */
    public static AxisInfo wrap(final CS_AxisInfo axis)
    {return (axis!=null) ? new AxisInfo(axis.name, wrap(axis.orientation)) : null;}

    /**
     * Returns an axis array for an OpenGIS structure array.
     */
    static AxisInfo[] wrap(final CS_AxisInfo[] axis)
    {
        if (axis==null) return null;
        final AxisInfo[] a=new AxisInfo[axis.length];
        for (int i=0; i<axis.length; i++)
            a[i] = wrap(axis[i]);
        return a;
    }

    /**
     * Returns a parameter for an OpenGIS structure.
     */
    public static Parameter wrap(final CS_ProjectionParameter parameter)
    {return (parameter!=null) ? new Parameter(parameter.name, parameter.value) : null;}

    /**
     * Returns a parameter array for an OpenGIS structure array.
     */
    static Parameter[] wrap(final CS_ProjectionParameter[] parameters)
    {
        if (parameters==null) return null;
        final Parameter[] p=new Parameter[parameters.length];
        for (int i=0; i<parameters.length; i++)
            p[i] = wrap(parameters[i]);
        return p;
    }

    /**
     * Returns conversion info for an OpenGIS structure.
     */
    public static WGS84ConversionInfo wrap(final CS_WGS84ConversionInfo info)
    {
        if (info==null) return null;
        final WGS84ConversionInfo nf = new WGS84ConversionInfo();
        nf.dx = info.dx;
        nf.dy = info.dy;
        nf.dz = info.dz;
        nf.ex = info.ex;
        nf.ey = info.ey;
        nf.ez = info.ez;
        nf.ppm = info.ppm;
        nf.areaOfUse = info.areaOfUse;
        return nf;
    }

    /**
     * Returns an unit for an OpenGIS structure.
     * @throws RemoteException if a remote call failed.
     */
    public static Unit wrap(final CS_Unit unit) throws RemoteException
    {
        if (unit==null) return null;
        if (unit instanceof CS_LinearUnit)
        {
            final double metersPerUnit = ((CS_LinearUnit)unit).getMetersPerUnit();
            if (metersPerUnit==1) return Unit.METRE; // TODO
        }
        if (unit instanceof CS_AngularUnit)
        {
            final double radiansPerUnit = ((CS_AngularUnit)unit).getRadiansPerUnit();
            if (radiansPerUnit==Math.PI/180) return Unit.DEGREE; // TODO
        }
        throw new UnsupportedOperationException("Not implemented");
    }
}
