/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.cs;

// OpenGIS dependencies
import org.opengis.cs.CS_Ellipsoid;
import org.opengis.cs.CS_LinearUnit;

// Miscellaneous
import java.util.Map;
import java.lang.Double; // For JavaDoc
import javax.units.Unit;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;

// Resources
import net.seagis.resources.XMath;
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * The figure formed by the rotation of an ellipse about an axis.
 * In this context, the axis of rotation is always the minor axis. It is named geodetic
 * ellipsoid if the parameters are derived by the measurement of the shape and the size
 * of the Earth to approximate the geoid as close as possible.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Ellipsoid
 */
public class Ellipsoid extends Info
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1047804526105439230L;

    /**
     * WGS 1984 ellipsoid. This ellipsoid is used in GPS system
     * and is the default for most <code>net.seagis</code> packages.
     */
    public static final Ellipsoid WGS84 = (Ellipsoid) pool.intern(
                        createFlattenedSphere("WGS84", 6378137.0, 298.257223563, Unit.METRE));

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
     * The inverse of the flattening value, or
     * {@link Double#POSITIVE_INFINITY} if the
     * ellipsoid is a sphere.
     *
     * @see #getInverseFlattening
     */
    private final double inverseFlattening;

    /**
     * Is the Inverse Flattening definitive for this ellipsoid?
     *
     * @see #isIvfDefinitive
     */
    private final boolean ivfDefinitive;

    /**
     * The units of the semi-major
     * and semi-minor axis values.
     */
    private final Unit unit;

    /**
     * Construct a new sphere using the specified radius.
     *
     * @param name   Name of this sphere.
     * @param radius The equatorial and polar radius.
     * @param unit   The units of the semi-major and semi-minor axis values.
     */
    public Ellipsoid(final String name, final double radius, final Unit unit)
    {this(name, check("radius", radius), radius, Double.POSITIVE_INFINITY, false, unit);}

    /**
     * Construct a new ellipsoid using the specified axis length.
     *
     * @param name          Name of this ellipsoid.
     * @param semiMajorAxis The equatorial radius.
     * @param semiMinorAxis The polar radius.
     * @param unit          The units of the semi-major and semi-minor axis values.
     *
     * @see org.opengis.cs.CS_CoordinateSystemFactory#createEllipsoid
     */
    public Ellipsoid(final String name, final double semiMajorAxis, final double semiMinorAxis, final Unit unit)
    {this(name, semiMajorAxis, semiMinorAxis, semiMajorAxis/(semiMajorAxis-semiMinorAxis), false, unit);}

    /**
     * Construct a new ellipsoid using the specified axis length.
     *
     * @param name              Name of this ellipsoid.
     * @param semiMajorAxis     The equatorial radius.
     * @param semiMinorAxis     The polar radius.
     * @param inverseFlattening The inverse of the flattening value.
     * @param ivfDefinitive     Is the Inverse Flattening definitive for this ellipsoid?
     * @param unit              The units of the semi-major and semi-minor axis values.
     */
    private Ellipsoid(final String name, final double semiMajorAxis, final double semiMinorAxis, final double inverseFlattening, final boolean ivfDefinitive, final Unit unit)
    {
        super(name);
        this.unit = unit;
        this.semiMajorAxis     = check("semiMajorAxis",     semiMajorAxis);
        this.semiMinorAxis     = check("semiMinorAxis",     semiMinorAxis);
        this.inverseFlattening = check("inverseFlattening", inverseFlattening);
        this.ivfDefinitive     = ivfDefinitive;
        ensureNonNull ("unit", unit);
        ensureLinearUnit(unit);
    }

    /**
     * Construct a new ellipsoid using the specified axis length.
     *
     * @param properties        The set of properties (see {@link Info}).
     * @param semiMajorAxis     The equatorial radius.
     * @param semiMinorAxis     The polar radius.
     * @param inverseFlattening The inverse of the flattening value.
     * @param ivfDefinitive     Is the Inverse Flattening definitive for this ellipsoid?
     * @param unit              The units of the semi-major and semi-minor axis values.
     */
    Ellipsoid(final Map properties, final double semiMajorAxis, final double semiMinorAxis, final double inverseFlattening, final boolean ivfDefinitive, final Unit unit)
    {
        super(properties);
        this.unit = unit;
        this.semiMajorAxis     = semiMajorAxis;
        this.semiMinorAxis     = semiMinorAxis;
        this.inverseFlattening = inverseFlattening;
        this.ivfDefinitive     = ivfDefinitive;
        // Accept null values.
    }

    /**
     * Construct a new ellipsoid using the specified axis length
     * and inverse flattening value.
     *
     * @param name              Name of this ellipsoid.
     * @param semiMajorAxis     The equatorial radius.
     * @param inverseFlattening The inverse flattening value.
     * @param unit              The units of the semi-major and semi-minor axis values.
     *
     * @see org.opengis.cs.CS_CoordinateSystemFactory#createFlattenedSphere
     */
    public static Ellipsoid createFlattenedSphere(final String name, final double semiMajorAxis, final double inverseFlattening, final Unit unit)
    {return new Ellipsoid(name, semiMajorAxis, semiMajorAxis*(1-1/inverseFlattening), inverseFlattening, true, unit);}

    /**
     * Check the argument validity. Argument
     * <code>value</code> should be greater
     * than zero.
     *
     * @param  name  Argument name.
     * @param  value Argument value.
     * @return <code>value</code>.
     * @throws IllegalArgumentException if <code>value</code> is not greater than 0.
     */
    private static double check(final String name, final double value) throws IllegalArgumentException
    {
        if (value>0) return value;
        throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_ILLEGAL_ARGUMENT_$2, name, new Double(value)));
    }

    /**
     * Gets the equatorial radius.
     * The returned length is expressed in this object's axis units.
     *
     * @see org.opengis.cs.CS_Ellipsoid#getSemiMajorAxis()
     */
    public double getSemiMajorAxis()
    {return semiMajorAxis;}

    /**
     * Gets the polar radius.
     * The returned length is expressed in this object's axis units.
     *
     * @see org.opengis.cs.CS_Ellipsoid#getSemiMinorAxis()
     */
    public double getSemiMinorAxis()
    {return semiMinorAxis;}

    /**
     * The ratio of the distance between the center and a focus of the ellipse
     * to the length of its semimajor axis. The eccentricity can alternately be
     * computed from the equation: <code>e=sqrt(2f-f�)</code>.
     */
    public double getEccentricity()
    {
        final double f=1-getSemiMinorAxis()/getSemiMajorAxis();
        return Math.sqrt(2*f - f*f);
    }

    /**
     * Returns the value of the inverse of the flattening constant.
     * Flattening is a value used to indicate how closely an ellipsoid approaches a
     * spherical shape. The inverse flattening is related to the equatorial/polar
     * radius (<var>r<sub>e</sub></var> and <var>r<sub>p</sub></var> respectively)
     * by the formula <code>ivf=r<sub>e</sub>/(r<sub>e</sub>-r<sub>p</sub>)</code>.
     * For perfect spheres, this method returns {@link Double#POSITIVE_INFINITY}
     * (which is the correct value).
     *
     * @see org.opengis.cs.CS_Ellipsoid#getInverseFlattening()
     */
    public double getInverseFlattening()
    {return inverseFlattening;}

    /**
     * Is the Inverse Flattening definitive for this ellipsoid?
     * Some ellipsoids use the IVF as the defining value, and calculate the
     * polar radius whenever asked. Other ellipsoids use the polar radius to
     * calculate the IVF whenever asked. This distinction can be important to
     * avoid floating-point rounding errors.
     */
    public boolean isIvfDefinitive()
    {return ivfDefinitive;}

    /**
     * Returns an <em>estimation</em> of orthodromic distance between two geographic coordinates.
     * The orthodromic distance is the shortest distance between two points on a sphere's surface.
     * The orthodromic path is always on a great circle. An other possible distance measurement
     * is the loxodromic distance, which is a longer distance on a path with a constant direction
     * on the compas.
     *
     * @param  P1 Longitude and latitude of first point (in degrees).
     * @param  P2 Longitude and latitude of second point (in degrees).
     * @return The orthodromic distance (in the units of this ellipsoid).
     */
    public double orthodromicDistance(final Point2D P1, final Point2D P2)
    {return orthodromicDistance(P1.getX(), P1.getY(), P2.getX(), P2.getY());}

    /**
     * Returns an <em>estimation</em> of orthodromic distance between two geographic coordinates.
     * The orthodromic distance is the shortest distance between two points on a sphere's surface.
     * The orthodromic path is always on a great circle. An other possible distance measurement
     * is the loxodromic distance, which is a longer distance on a path with a constant direction
     * on the compas.
     *
     * @param  x1 Longitude of first point (in degrees).
     * @param  y1 Latitude of first point (in degrees).
     * @param  x2 Longitude of second point (in degrees).
     * @param  y2 Latitude of second point (in degrees).
     * @return The orthodromic distance (in the units of this ellipsoid).
     */
    public double orthodromicDistance(double x1, double y1, double x2, double y2)
    {
        /*
         * Le calcul de la distance orthodromique sur une surface ellipso�dale est complexe,
         * sujet � des erreurs d'arrondissements et sans solution � proximit� des p�les.
         * Nous utilisont plut�t un calcul bas� sur une forme sph�rique de la terre. Un
         * programme en Fortran calculant les distances orthodromiques sur une surface
         * ellipso�dale peut �tre t�l�charg� � partir du site de NOAA:
         *
         *            ftp://ftp.ngs.noaa.gov/pub/pcsoft/for_inv.3d/source/
         */
        y1 = Math.toRadians(y1);
        y2 = Math.toRadians(y2);
        final double y  = 0.5*(y1+y2);
        final double dx = Math.toRadians(Math.abs(x2-x1) % 360);
        double rho = Math.sin(y1)*Math.sin(y2) + Math.cos(y1)*Math.cos(y2)*Math.cos(dx);
//----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert Math.abs(rho) < 1.0000001 : rho;
//----- END OF JDK 1.4 DEPENDENCIES ----
        if (rho>+1) rho=+1; // Catch rounding error.
        if (rho<-1) rho=-1; // Catch rounging error.
        return Math.acos(rho)/XMath.hypot(Math.sin(y)/getSemiMajorAxis(), Math.cos(y)/getSemiMinorAxis());
               // 'hypot' calcule l'inverse du rayon **apparent** de la terre � la latitude 'y'.
    }

    /**
     * Returns the units of the semi-major
     * and semi-minor axis values.
     *
     * @see org.opengis.cs.CS_Ellipsoid#getAxisUnit()
     */
    public Unit getAxisUnit()
    {return unit;}

    /**
     * Compares the specified object with
     * this ellipsoid for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final Ellipsoid that = (Ellipsoid) object;
            return this.ivfDefinitive == that.ivfDefinitive &&
                   Double.doubleToLongBits(this.semiMajorAxis)     == Double.doubleToLongBits(that.semiMajorAxis)     &&
                   Double.doubleToLongBits(this.semiMinorAxis)     == Double.doubleToLongBits(that.semiMinorAxis)     &&
                   Double.doubleToLongBits(this.inverseFlattening) == Double.doubleToLongBits(that.inverseFlattening) &&
                   Utilities.equals(this.unit, that.unit);
        }
        return false;
    }

    /**
     * Returns a hash value for this ellipsoid.
     */
    public int hashCode()
    {
        final long longCode=Double.doubleToLongBits(getSemiMajorAxis());
        return (((int)(longCode >>> 32)) ^ (int)longCode) + 37*super.hashCode();
    }

    /**
     * Fill the part inside "[...]".
     * Used for formatting Well Know Text (WKT).
     */
    String addString(final StringBuffer buffer)
    {
        buffer.append(", ");
        buffer.append(semiMajorAxis);
        buffer.append(", ");
        buffer.append(Double.isInfinite(inverseFlattening) ? 0 : inverseFlattening);
        return "SPHEROID";
    }

    /**
     * Returns an OpenGIS interface for this ellipsoid.
     * The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    final Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link Ellipsoid} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends Info.Export implements CS_Ellipsoid
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Gets the equatorial radius.
         */
        public double getSemiMajorAxis() throws RemoteException
        {return Ellipsoid.this.getSemiMajorAxis();}

        /**
         * Gets the polar radius.
         */
        public double getSemiMinorAxis() throws RemoteException
        {return Ellipsoid.this.getSemiMinorAxis();}

        /**
         * Returns the value of the inverse of the flattening constant.
         */
        public double getInverseFlattening() throws RemoteException
        {
            final double ivf=Ellipsoid.this.getInverseFlattening();
            return Double.isInfinite(ivf) ? 0 : ivf;
        }

        /**
         * Is the Inverse Flattening definitive for this ellipsoid?
         */
        public boolean isIvfDefinitive() throws RemoteException
        {return Ellipsoid.this.isIvfDefinitive();}

        /**
         * Returns the LinearUnit.
         */
        public CS_LinearUnit getAxisUnit() throws RemoteException
        {return (CS_LinearUnit) adapters.export(Ellipsoid.this.getAxisUnit());}
    }
}
