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
package net.seagis.cs;

// OpenGIS dependencies
import org.opengis.cs.CS_Info;
import org.opengis.cs.CS_Unit;
import org.opengis.cs.CS_LinearUnit;
import org.opengis.cs.CS_AngularUnit;

// Miscellaneous
import javax.units.Unit;
import java.util.Locale;
import java.io.Serializable;

// Collections
import java.util.Map;
import java.util.HashMap;

// References
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

// Remote Method Invocation
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.io.ObjectStreamException;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.WeakHashSet;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * A base class for metadata applicable to coordinate system objects.
 * The metadata items "Abbreviation", "Alias", "Authority", "AuthorityCode",
 * "Name" and "Remarks" were specified in the Simple Features interfaces,
 * so they have been kept here.
 *
 * This specification does not dictate what the contents of these items
 * should be. However, the following guidelines are suggested:
 * <ul>
 *   <li>When {@link net.seagis.cs.CoordinateSystemAuthorityFactory}
 *       is used to create an object, the "Authority" and "AuthorityCode"
 *       values should be set to the authority name of the factory object,
 *       and the authority code supplied by the client, respectively. The
 *       other values may or may not be set. (If the authority is EPSG,
 *       the implementer may consider using the corresponding metadata values
 *       in the EPSG tables.)</li>
 *   <li>When {@link net.seagis.cs.CoordinateSystemFactory} creates an
 *       object, the "Name" should be set to the value supplied by the client.
 *       All of the other metadata items should be left empty.</li>
 * </ul>
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Info
 */
public class Info implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -771181600202966524L;

    /**
     * Set of weak references to existing coordinate systems.
     * This set is used in order to return pre-existing object
     * instead of creating new one.
     */
    static final WeakHashSet pool=new WeakHashSet();

    /**
     * The non-localized object name.
     */
    private final String name;

    /**
     * Properties for all methods except {@link #getName}.  For
     * example the method {@link #getAuthorityCode} returns the
     * value of property <code>"authorityCode"</code>.   May be
     * null if there is no properties for this object.
     */
    private final Map properties;

    /**
     * OpenGIS object returned by {@link #cachedOpenGIS}.
     * It may be a hard or a weak reference.
     */
    private transient Object proxy;

    /**
     * Create an object with the specified name.
     *
     * @param name This object name.
     */
    public Info(final String name)
    {
        this.name       = name;
        this.properties = null;
        ensureNonNull("name", name);
    }

    /**
     * Create an object with the specified properties.
     * Property keys are any of the following strings:
     * <ul>
     *   <li>"name" (mandatory)</li>
     *   <li>"authority"</li>
     *   <li>"authorityCode"</li>
     *   <li>"alias"</li>
     *   <li>"abbreviation"</li>
     *   <li>"remarks"</li>
     * </ul>
     * Values are usually {@link String}, or may be <code>null</code>
     * if a particular property is not defined. The "name" property
     * is mandatory.
     *
     * @param properties The set of properties.
     * @param name This object name.
     */
    Info(final Map properties)
    {
        ensureNonNull("properties", properties);
        this.properties = properties;
        this.name       = (String)properties.get("name");
        this.proxy      = (Object)properties.get("proxy");
    }

    /**
     * Gets the name of this object. The default implementation
     * returns the non-localized name given at construction time.
     *
     * @param locale The desired locale, or <code>null</code> for a default locale.
     *        If no string is available for the specified locale, an arbitrary locale
     *        is used.
     *
     * @see org.opengis.cs.CS_Info#getName()
     */
    public String getName(final Locale locale)
    {return name;}

    /**
     * Gets the authority name, or <code>null</code> if unspecified.
     * An Authority is an organization that maintains definitions of Authority
     * Codes.  For example the European Petroleum Survey Group (EPSG) maintains
     * a database of coordinate systems, and other spatial referencing objects,
     * where each object has a code number ID.  For example, the EPSG code for a
     * WGS84 Lat/Lon coordinate system is '4326'.
     *
     * @param locale The desired locale, or <code>null</code> for the default locale.
     *        If no string is available for the specified locale, an arbitrary locale
     *        is used.
     *
     * @see org.opengis.cs.CS_Info#getAuthority()
     */
    public String getAuthority(final Locale locale)
    {return (properties!=null) ? (String)properties.get("authority") : null;}

    /**
     * Gets the authority-specific identification code, or <code>null</code> if unspecified.
     * The AuthorityCode is a compact string defined by an Authority to reference
     * a particular spatial reference object.  For example, the European Survey
     * Group (EPSG) authority uses 32 bit integers to reference coordinate systems,
     * so all their code strings will consist of a few digits.  The EPSG code for
     * WGS84 Lat/Lon is '4326'.
     *
     * @param locale The desired locale, or <code>null</code> for the default locale.
     *        If no string is available for the specified locale, an arbitrary locale
     *        is used.
     *
     * @see org.opengis.cs.CS_Info#getAuthorityCode()
     */
    public String getAuthorityCode(final Locale locale)
    {return (properties!=null) ? (String)properties.get("authorityCode") : null;}

    /**
     * Gets the alias, or <code>null</code> if there is none.
     *
     * @param locale The desired locale, or <code>null</code> for the default locale.
     *        If no string is available for the specified locale, an arbitrary locale
     *        is used.
     *
     * @see org.opengis.cs.CS_Info#getAlias()
     */
    public String getAlias(final Locale locale)
    {return (properties!=null) ? (String)properties.get("alias") : null;}

    /**
     * Gets the abbreviation, or <code>null</code> if there is none.
     *
     * @param locale The desired locale, or <code>null</code> for the default locale.
     *        If no string is available for the specified locale, an arbitrary locale
     *        is used.
     *
     * @see org.opengis.cs.CS_Info#getAbbreviation()
     */
    public String getAbbreviation(final Locale locale)
    {return (properties!=null) ? (String)properties.get("abbreviation") : null;}

    /**
     * Gets the provider-supplied remarks,
     * or <code>null</code> if there is none.
     *
     * @param locale The desired locale, or <code>null</code> for the default locale.
     *        If no string is available for the specified locale, an arbitrary locale
     *        is used.
     *
     * @see org.opengis.cs.CS_Info#getRemarks()
     */
    public String getRemarks(final Locale locale)
    {return (properties!=null) ? (String)properties.get("remarks") : null;}

    /**
     * Returns a hash value for this info.
     */
    public int hashCode()
    {
        final String name = getName(null);
        return (name!=null) ? name.hashCode() : 369781;
    }

    /**
     * Compares the specified object
     * with this info for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && getClass().equals(object.getClass()))
        {
            final Info that = (Info) object;
            return Utilities.equals(this.name,       that.name) &&
                   Utilities.equals(this.properties, that.properties);
        }
        return false;
    }

    /**
     * Returns a <em>Well Know Text</em> (WKT) for this info.
     * "Well know text" are part of OpenGIS's specification.
     */
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a <em>Well Know Text</em> (WKT) for this info.
     *
     * @param context The contextual unit. Most subclasses will
     *        ignore this argument, except {@link PrimeMeridian}.
     */
    final String toString(final Object context)
    {
        final StringBuffer buffer = new StringBuffer(40);
        buffer.append("[\"");
        buffer.append(getName(null));
        buffer.append('"');
        buffer.insert(0, addString(buffer, context));
        if (properties!=null)
        {
            final Object authority = properties.get("authority");
            if (authority!=null)
            {
                buffer.append(", AUTHORITY[");
                buffer.append(authority);
                // TODO: Add code (as in AUTHORITY["EPSG","8901"])
                buffer.append("\"]");
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Add more information inside the "[...]" part of {@link #toString}.
     * The default implementation add nothing. Subclasses will override
     * this method in order to complete string representation.
     *
     * @param  buffer The buffer to add string to.
     * @param  context The contextual unit. Most subclasses will
     *         ignore this argument, except {@link PrimeMeridian}.
     * @return The WKT code name (e.g. "GEOGCS").
     */
    String addString(final StringBuffer buffer, Object context)
    {return Utilities.getShortClassName(this);}

    /**
     * Add a unit in WKT form.
     */
    final void addUnit(final StringBuffer buffer, final Unit unit)
    {
        if (unit!=null)
        {
            buffer.append("UNIT[\"");
            buffer.append(unit.getLocalizedName());
            buffer.append('"');
            Unit base=null;
            if (Unit.METRE.canConvert(unit))
            {
                base = Unit.METRE;
            }
            else if (Unit.RADIAN.canConvert(unit))
            {
                base = Unit.RADIAN;
            }
            else if (Unit.SECOND.canConvert(unit))
            {
                base = Unit.SECOND;
            }
            if (unit!=null)
            {
                buffer.append(',');
                buffer.append(base.convert(1, unit));
            }
            buffer.append(']');
        }
    }

    /**
     * Make sure an argument is non-null. This is a
     * convenience method for subclasses constructors.
     *
     * @param  name   Argument name.
     * @param  object User argument.
     * @throws IllegalArgumentException if <code>object</code> is null.
     */
    protected static void ensureNonNull(final String name, final Object object) throws IllegalArgumentException
    {
        if (object==null)
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NULL_ARGUMENT_$1, name));
    }

    /**
     * Make sure an array element is non-null.
     *
     * @param  name  Argument name.
     * @param  array User argument.
     * @param  index Element to check.
     * @throws IllegalArgumentException if <code>array[i]</code> is null.
     */
    static void ensureNonNull(final String name, final Object[] array, final int index) throws IllegalArgumentException
    {
        if (array[index]==null)
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NULL_ARGUMENT_$1, name+'['+index+']'));
    }

    /**
     * Make sure that the specified unit is a temporal one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not a temporal unit.
     */
    static void ensureTimeUnit(final Unit unit) throws IllegalArgumentException
    {
        if (!Unit.SECOND.canConvert(unit))
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NON_TEMPORAL_UNIT_$1, unit));
    }

    /**
     * Make sure that the specified unit is a linear one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not a linear unit.
     */
    static void ensureLinearUnit(final Unit unit) throws IllegalArgumentException
    {
        if (!Unit.METRE.canConvert(unit))
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NON_LINEAR_UNIT_$1, unit));
    }

    /**
     * Make sure that the specified unit is an angular one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not an angular unit.
     */
    static void ensureAngularUnit(final Unit unit) throws IllegalArgumentException
    {
        if (!Unit.DEGREE.canConvert(unit))
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_NON_ANGULAR_UNIT_$1, unit));
    }

    /**
     * Returns a reference to an unique instance of this <code>Info</code>.
     * This method is automatically invoked during deserialization.
     *
     * NOTE ABOUT ACCESS-MODIFIER:      This method can't be private,
     * because it would prevent it from being invoked from subclasses
     * in this package (e.g. {@link CoordinateSystem}).   This method
     * <em>will not</em> be invoked for classes outside this package,
     * unless we give it <code>protected</code> access.   TODO: Would
     * it be a good idea?
     */
    Object readResolve() throws ObjectStreamException
    {return pool.intern(this);}

    /**
     * Returns an OpenGIS interface for this info.
     * The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}

    /**
     * Returns an OpenGIS interface for this info.
     * This method first look in the cache. If no
     * interface was previously cached, then this
     * method create a new adapter  and cache the
     * result.
     *
     * @param adapters The originating {@link Adapters}.
     */
    final synchronized Object cachedOpenGIS(final Object adapters)
    {
        if (proxy!=null)
        {
            if (proxy instanceof Reference)
            {
                final Object ref = ((Reference) proxy).get();
                if (ref!=null) return ref;
            }
            else return proxy;
        }
        final Object opengis = toOpenGIS(adapters);
        proxy = new WeakReference(opengis);
        return opengis;
    }




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link Info} object for use with OpenGIS. This wrapper is a
     * good place to check for non-implemented OpenGIS methods  (just check
     * for methods throwing {@link UnsupportedOperationException}). This
     * class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    class Export extends RemoteObject implements CS_Info
    {
        /**
         * The originating adapter.
         */
        protected final Adapters adapters;

        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {this.adapters = (Adapters)adapters;}

        /**
         * Returns the underlying implementation.
         */
        public final Info unwrap()
        {return Info.this;}

        /**
         * Gets the name.
         */
        public String getName() throws RemoteException
        {return Info.this.getName(null);}

        /**
         * Gets the authority name.
         */
        public String getAuthority() throws RemoteException
        {return Info.this.getAuthority(null);}

        /**
         * Gets the authority-specific identification code.
         */
        public String getAuthorityCode() throws RemoteException
        {return Info.this.getAuthorityCode(null);}

        /**
         * Gets the alias.
         */
        public String getAlias() throws RemoteException
        {return Info.this.getAlias(null);}

        /**
         * Gets the abbreviation.
         */
        public String getAbbreviation() throws RemoteException
        {return Info.this.getAbbreviation(null);}

        /**
         * Gets the provider-supplied remarks.
         */
        public String getRemarks() throws RemoteException
        {return Info.this.getRemarks(null);}

        /**
         * Gets a Well-Known text representation of this object.
         */
        public String getWKT() throws RemoteException
        {return Info.this.toString();}

        /**
         * Gets an XML representation of this object.
         */
        public String getXML() throws RemoteException
        {throw new UnsupportedOperationException("XML parsing not yet implemented");}

        /**
         * Returns a string representation of this info.
         */
        public String toString()
        {return Info.this.toString();}
    }

    /**
     * OpenGIS abstract unit.
     */
    class AbstractUnit extends Export implements CS_Unit
    {
        /**
         * Number of meters per linear unit or
         *          radians per angular unit.
         */
        final double scale;

        /**
         * Construct an abstract unit.
         */
        public AbstractUnit(final Adapters adapters, final double scale)
        {
            super(adapters);
            this.scale = scale;
        }

        /**
         * Returns a Well Know Text for this unit.
         */
        public final String toString()
        {return "UNIT[\""+name+"\","+scale+']';}
    }

    /**
     * OpenGIS linear unit.
     */
    final class LinearUnit extends AbstractUnit implements CS_LinearUnit
    {
        /** Construct a linear unit. */
        public LinearUnit(final Adapters adapters, final double metersPerUnit)
        {super(adapters, metersPerUnit);}

        /** Returns the number of meters per linear unit. */
        public double getMetersPerUnit() throws RemoteException
        {return scale;}
    }

    /**
     * OpenGIS angular unit.
     */
    final class AngularUnit extends AbstractUnit implements CS_AngularUnit
    {
        /** Construct an angular unit. */
        public AngularUnit(final Adapters adapters, final double radiansPerUnit)
        {super(adapters, radiansPerUnit);}
        
        /** Returns the number of radians per angular unit. */
        public double getRadiansPerUnit() throws RemoteException
        {return scale;}
    }
}
