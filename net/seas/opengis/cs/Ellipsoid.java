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
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 */
package net.seas.opengis.cs;

// Input/output
import java.rmi.Remote;
import java.io.Serializable;
import java.rmi.RemoteException;


/**
 * The figure formed by the rotation of an ellipse about an axis. In this context,
 * the axis of rotation is always the minor axis. It is named geodetic ellipsoid
 * if the parameters are derived by the measurement of the shape and the size of
 * the Earth to approximate the geoid as close as possible.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Ellipsoid implements Serializable
{
	/**
	 * Serial number for compatibility with previous versions.
	 * TODO: compure serialver
	 */
	//private static final long serialVersionUID = ?;

	/**
	 * Name of this ellipsoid.
	 * @see #getName
	 */
	private final String name;

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
	 * @see #isIvfDefinitive
	 */
	private final boolean ivfDefinitive;

	/**
	 * Construct a new ellipsoid using the specified axis length.
	 *
	 * @param name          Name of this ellipsoid.
	 * @param semiMajorAxis The equatorial radius in metres.
	 * @param semiMinorAxis The polar radius in metres.
	 */
	public Ellipsoid(final String name, final double semiMajorAxis, final double semiMinorAxis)
	{
		this.name          = name;
		this.semiMajorAxis = semiMajorAxis;
		this.semiMinorAxis = semiMinorAxis;
		inverseFlattening  = semiMajorAxis/(semiMajorAxis-semiMinorAxis);
		ivfDefinitive      = false;
	}

	/**
	 * Get the name of this ellipsoid.
	 */
	public String getName()
	{return name;}

	/**
	 * Gets the equatorial radius, in metres.
	 */
	public double getSemiMajorAxis()
	{return semiMajorAxis;}

	/**
	 * Gets the polar radius, in metres.
	 */
	public double getSemiMinorAxis()
	{return semiMinorAxis;}

	/**
	 * Returns the value of the inverse of the flattening constant. Flattening is a
	 * value used to indicate how closely an ellipsoid approaches a spherical shape.
	 * The inverse flattening is related to the equatorial/polar radius
	 * (<var>r<sub>e</sub></var> and <var>r<sub>p</sub></var> respectively) by the
	 * formula <code>ivf=r<sub>e</sub>/(r<sub>e</sub>-r<sub>p</sub>)</code>. For
	 * perfect spheres, this method returns {@link Double#POSITIVE_INFINITY}
	 * (which is the correct value).
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
	 * Check if two objects are equals. One
	 * or both of the objects may be null.
	 */
	private static boolean equals(final Object o1, final Object o2)
	{return o1==o2 || (o1!=null && o1.equals(o2));}

	/**
	 * Compares the specified object with this ellipsoid for equality.
	 * Two ellipsoids are considered equals if they have the same name
	 * and are defined with the same quantities (semi-major and semi-minor
	 * axis length if {@link #isIvfDefinitive} returns false, or semi-major
	 * axis length and inverse flattening if {@link #isIvfDefinitive}
	 * returns true).
	 */
	public boolean equals(final Object obj)
	{
		if (obj instanceof Ellipsoid)
		{
			final Ellipsoid that = (Ellipsoid) obj;
			if (equals(this.getName(), that.getName()) &&
			    this.getSemiMajorAxis() == that.getSemiMajorAxis())
			{
				final boolean ivfDefinitive = isIvfDefinitive();
				if (ivfDefinitive == that.isIvfDefinitive())
				{
					if (ivfDefinitive)
					{
						return this.getInverseFlattening() ==
							   that.getInverseFlattening();
					}
					else
					{
						return this.getSemiMinorAxis() ==
						       that.getSemiMinorAxis();
					}
				}
			}
		}
		return false;
	}

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
		final StringBuffer buffer=new StringBuffer("Ellipsoid[");
		final String name=getName();
		if (name!=null)
		{
			buffer.append('"');
			buffer.append(name);
			buffer.append("\", ");
		}
		buffer.append("semiMajorAxis=");
		buffer.append(getSemiMajorAxis());
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
		}
		buffer.append(']');
		return buffer.toString();
	}
}
