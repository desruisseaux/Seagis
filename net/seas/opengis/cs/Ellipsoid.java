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

// Miscellaneous
import javax.units.Unit;
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.util.XMath;
import net.seas.resources.Resources;

// For JavaDOC only.
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * The figure formed by the rotation of an ellipse about an axis. In this context,
 * the axis of rotation is always the minor axis. It is named geodetic ellipsoid
 * if the parameters are derived by the measurement of the shape and the size of
 * the Earth to approximate the geoid as close as possible.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Ellipsoid
 */
public abstract class Ellipsoid extends Info implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3922121355438611749L;

    /**
     * WGS 1984 ellipsoid. This ellipsoid is used in GPS system
     * and is the default one in many places in this package.
     */
    private static final Ellipsoid WGS84 = new FlattenedSphere("WGS84", 6378137.0, 298.257223563, Unit.METRE);

    /**
     * Name of this ellipsoid.
     * @see #getName
     */
    private final String name;

    /**
     * The units of the semi-major
     * and semi-minor axis values.
     */
    private final Unit unit;

    /**
     * Construct a new ellipsoid.
     *
     * @param name Name of this ellipsoid.
     * @param unit The units of the semi-major and semi-minor axis values.
     */
    protected Ellipsoid(final String name, final Unit unit)
    {
        this.name = name;
        this.unit = unit;
        ensureNonNull("name", name);
        ensureNonNull("unit", unit);
        if (!Unit.METRE.canConvert(unit))
            throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_ARGUMENT¤2, "unit", unit));
    }

    /**
     * Check the argument validity. Argument
     * <code>value</code> should be greater
     * than zero.
     *
     * @param  name  Argument name.
     * @param  value Argument value.
     * @throws IllegalArgumentException if <code>value</code> is not greater than 0.
     */
    static void check(final String name, final double value) throws IllegalArgumentException
    {
        if (!(value>0)) // Use '!' in order to catch 'NaN' values.
            throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_ARGUMENT¤2, name, new Double(value)));
    }

    /**
     * Get the name of this ellipsoid.
     */
    public String getName()
    {return name;}

    /**
     * Gets the equatorial radius.
     * The returned length is expressed in this object's axis units.
     */
    public abstract double getSemiMajorAxis();

    /**
     * Gets the polar radius.
     * The returned length is expressed in this object's axis units.
     */
    public abstract double getSemiMinorAxis();

    /**
     * Returns the value of the inverse of the flattening constant. Flattening is a
     * value used to indicate how closely an ellipsoid approaches a spherical shape.
     * The inverse flattening is related to the equatorial/polar radius
     * (<var>r<sub>e</sub></var> and <var>r<sub>p</sub></var> respectively) by the
     * formula <code>ivf=r<sub>e</sub>/(r<sub>e</sub>-r<sub>p</sub>)</code>. For
     * perfect spheres, this method returns {@link Double#POSITIVE_INFINITY}
     * (which is the correct value).
     */
    public abstract double getInverseFlattening();

    /**
     * Is the Inverse Flattening definitive for this ellipsoid?
     * Some ellipsoids use the IVF as the defining value, and calculate the
     * polar radius whenever asked. Other ellipsoids use the polar radius to
     * calculate the IVF whenever asked. This distinction can be important to
     * avoid floating-point rounding errors.
     */
    public abstract boolean isIvfDefinitive();

    /**
     * The ratio of the distance between the center and a focus of the ellipse
     * to the length of its semimajor axis. The eccentricity can alternately be
     * computed from the equation: <code>e=sqrt(2f-f²)</code>.
     */
    public double getEccentricity()
    {
        final double f=1-getSemiMinorAxis()/getSemiMajorAxis();
        return Math.sqrt(2*f - f*f);
    }

    /**
     * Retourne une <em>estimation</em> de la distance orthodromique séparant les deux coordonnées
     * spécifiées.  La distance orthodromique est la plus courte distance séparant deux points sur
     * la surface d'une sphère. Elle correspond toujours à un arc de grand cercle. Une autre mesure
     * de distance parfois utilisée est la distance loxodromique, une distance plus longue mais selon
     * une trajectoire qui permet de garder un cap constant sur le compas.
     *
     * @param  x1 Longitude du premier point (en degrés).
     * @param  y1 Latitude du premier point (en degrés).
     * @param  x2 Longitude du second point (en degrés).
     * @param  y2 Latitude du second point (en degrés).
     * @return La distance orthodromique selon les unités de cet ellipsoïde.
     */
    public double orthodromicDistance(double x1, double y1, double x2, double y2)
    {
        /*
         * Le calcul de la distance orthodromique sur une surface ellipsoîdale est complexe,
         * sujetes à des erreurs d'arrondissements et sans solution à proximité des pôles.
         * Nous utiliseront plutôt un calcul basée sur une forme sphérique de la terre. Un
         * programme en Fortran calculant les distances orthodromiques sur une surface
         * ellipsoîdale peut être téléchargé à partir du site de NOAA:
         *
         *            ftp://ftp.ngs.noaa.gov/pub/pcsoft/for_inv.3d/source/
         */
        y1 = Math.toRadians(y1);
        y2 = Math.toRadians(y2);
        final double y  = 0.5*(y1+y2);
        final double dx = Math.toRadians(Math.abs(x2-x1) % 360);
        return Math.acos(Math.sin(y1)*Math.sin(y2) + Math.cos(y1)*Math.cos(y2)*Math.cos(dx))/
               XMath.hypot(Math.sin(y)/getSemiMajorAxis(), Math.cos(y)/getSemiMinorAxis());
               // 'hypot' calcule l'inverse du rayon **apparent** de la terre à la latitude 'y'.
    }

    /**
     * Returns the units of the semi-major
     * and semi-minor axis values.
     */
    public Unit getAxisUnit()
    {return unit;}

    /**
     * Compares the specified object with this ellipsoid for equality.
     * Two ellipsoids are considered equals if they have the same name
     * and are defined with the same quantities (semi-major and semi-minor
     * axis length if {@link #isIvfDefinitive} returns false, or semi-major
     * axis length and inverse flattening if {@link #isIvfDefinitive}
     * returns true).
     */
    public boolean equals(final Object object)
    {return (object instanceof Ellipsoid) && equals((Ellipsoid)object);}

    /**
     * Compares the specified object with this ellipsoid for equality.
     */
    final boolean equals(final Ellipsoid that)
    {
        if (super.equals(that) &&
            XClass.equals(this.getAxisUnit(),      that.getAxisUnit()) &&
                   equals(this.getSemiMajorAxis(), that.getSemiMajorAxis()))
        {
            final boolean ivfDefinitive = isIvfDefinitive();
            if (ivfDefinitive == that.isIvfDefinitive())
            {
                if (ivfDefinitive)
                {
                    return equals(this.getInverseFlattening(),
                                  that.getInverseFlattening());
                }
                else
                {
                    return equals(this.getSemiMinorAxis(),
                                  that.getSemiMinorAxis());
                }
            }
        }
        return false;
    }

    /**
     * Compare the specified <code>double</code>s for equality.
     */
    private static boolean equals(final double a, final double b)
    {return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);}

    /**
     * Returns a hash value for this ellipsoid.
     */
    public int hashCode()
    {
        final long longCode=Double.doubleToLongBits(getSemiMajorAxis());
        int code = ((int)(longCode >>> 32)) ^ (int)longCode;
        final String name=getName();
        if (name!=null)
            code ^= name.hashCode();
        return code;
    }

    /**
     * Returns a string representation of this ellipsoid.
     * Output is implementation dependent, but may look like:
     * <pre>
     * Ellipsoid["WGS 1984", semiMajorAxis=6378137.0, inverseFlattening=298.257223563];
     * </pre>
     */
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer("Ellipsoid[");
        final String         name = getName();
        final Unit           unit = getAxisUnit();
        final String   unitSymbol = (unit!=null) ? unit.toString() : "";
        if (name!=null)
        {
            buffer.append('"');
            buffer.append(name);
            buffer.append("\", ");
        }
        buffer.append("semiMajorAxis=");
        buffer.append(getSemiMajorAxis());
        if (unitSymbol.length()!=0)
        {
            buffer.append(' ');
            buffer.append(unitSymbol);
        }
        buffer.append(", ");
        if (isIvfDefinitive())
        {
            buffer.append("inverseFlattening=");
            buffer.append(getInverseFlattening());
        }
        else
        {
            buffer.append("semiMinorAxis=");
            buffer.append(getSemiMinorAxis());
            if (unitSymbol.length()!=0)
            {
                buffer.append(' ');
                buffer.append(unitSymbol);
            }
        }
        buffer.append(']');
        return buffer.toString();
    }
}


/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////


/**
 * An ellipsoid whith equal semi-major and semi-minor radius.
 *
 * @version 1.00
 * @author Martin Desruisseaux
 */
final class Sphere extends Ellipsoid
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 7792371296908591235L;

    /**
     * The sphere's radius.
     * @see #getSemiMajorAxis
     * @see #getSemiMinorAxis
     */
    private final double radius;

    /**
     * Construct a new ellipsoid using the specified radius.
     *
     * @param name   Name of this ellipsoid.
     * @param radius The equatorial and polar radius.
     * @param unit   The units of the semi-major and semi-minor axis values.
     */
    protected Sphere(final String name, final double radius, final Unit unit)
    {
        super(name, unit);
        this.radius = radius;
        check("radius", radius);
    }

    /**
     * Gets the equatorial radius.
     */
    public double getSemiMajorAxis()
    {return radius;}

    /**
     * Gets the polar radius.
     */
    public double getSemiMinorAxis()
    {return radius;}

    /**
     * Returns the value of the inverse of the flattening constant.
     */
    public double getInverseFlattening()
    {return Double.POSITIVE_INFINITY;}

    /**
     * Is the Inverse Flattening definitive for this ellipsoid?
     */
    public boolean isIvfDefinitive()
    {return false;}
}


/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////


/**
 * An ellipsoid whith different semi-major and semi-minor radius.
 *
 * @version 1.00
 * @author Martin Desruisseaux
 */
final class Spheroid extends Ellipsoid
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1211202019386203401L;

    /**
     * The equatorial radius.
     * @see #getSemiMajorAxis
     */
    private final double semiMajorAxis;

    /**
     * The polar radius.
     * @see #getSemiMinorAxis
     */
    private final double semiMinorAxis;

    /**
     * Construct a new ellipsoid using the specified axis length.
     *
     * @param name          Name of this ellipsoid.
     * @param semiMajorAxis The equatorial radius.
     * @param semiMinorAxis The polar radius.
     * @param unit          The units of the semi-major and semi-minor axis values.
     */
    protected Spheroid(final String name, final double semiMajorAxis, final double semiMinorAxis, final Unit unit)
    {
        super(name, unit);
        this.semiMajorAxis = Math.max(semiMajorAxis, semiMinorAxis);
        this.semiMinorAxis = Math.min(semiMinorAxis, semiMajorAxis);
        check("semiMajorAxis", semiMajorAxis);
        check("semiMinorAxis", semiMinorAxis);
    }

    /**
     * Gets the equatorial radius.
     */
    public double getSemiMajorAxis()
    {return semiMajorAxis;}

    /**
     * Gets the polar radius.
     */
    public double getSemiMinorAxis()
    {return semiMinorAxis;}

    /**
     * Returns the value of the inverse of the flattening constant.
     */
    public double getInverseFlattening()
    {return semiMajorAxis/(semiMajorAxis-semiMinorAxis);}

    /**
     * Is the Inverse Flattening definitive for this ellipsoid?
     */
    public boolean isIvfDefinitive()
    {return false;}
}


/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////


/**
 * An ellipsoid whith semi-major radius and inverse flattening.
 *
 * @version 1.00
 * @author Martin Desruisseaux
 */
final class FlattenedSphere extends Ellipsoid
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -886902001843075297L;

    /**
     * The equatorial radius.
     * @see #getSemiMajorAxis
     */
    private final double semiMajorAxis;

    /**
     * The inverse of the flattening value, or
     * {@link Double#POSITIVE_INFINITY} if the
     * ellipsoid is a sphere.
     *
     * @see #getInverseFlattening
     */
    private final double inverseFlattening;

    /**
     * Construct a new ellipsoid using the specified axis length
     * and inverse flattening value.
     *
     * @param name              Name of this ellipsoid.
     * @param semiMajorAxis     The equatorial radius.
     * @param inverseFlattening The inverse flattening value.
     * @param unit              The units of the semi-major and semi-minor axis values.
     */
    protected FlattenedSphere(final String name, final double semiMajorAxis, final double inverseFlattening, final Unit unit)
    {
        super(name, unit);
        this.semiMajorAxis     = semiMajorAxis;
        this.inverseFlattening = inverseFlattening;
        check("semiMajorAxis",     semiMajorAxis);
        check("inverseFlattening", inverseFlattening);
    }

    /**
     * Gets the equatorial radius.
     */
    public double getSemiMajorAxis()
    {return semiMajorAxis;}

    /**
     * Gets the polar radius.
     */
    public double getSemiMinorAxis()
    {return semiMajorAxis*(1-1/inverseFlattening);}

    /**
     * Returns the value of the inverse of the flattening constant.
     */
    public double getInverseFlattening()
    {return inverseFlattening;}

    /**
     * Is the Inverse Flattening definitive for this ellipsoid?
     */
    public boolean isIvfDefinitive()
    {return true;}
}
