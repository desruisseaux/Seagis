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
import org.opengis.cs.CS_Unit;
import org.opengis.cs.CS_LinearUnit;
import org.opengis.cs.CS_AngularUnit;

// Miscellaneous
import javax.units.Unit;
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.resources.Resources;

// Remote Method Invocation
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;


/**
 * A base class for metadata applicable to coordinate system objects.
 * The metadata items "Abbreviation", "Alias", "Authority", "AuthorityCode",
 * "Name" and "Remarks" were specified in the Simple Features interfaces,
 * so they have been kept here.
 *
 * This specification does not dictate what the contents of these items
 * should be. However, the following guidelines are suggested:
 * <ul>
 *   <li>When {@link net.seas.opengis.cs.CoordinateSystemAuthorityFactory}
 *       is used to create an object, the "Authority" and "AuthorityCode"
 *       values should be set to the authority name of the factory object,
 *       and the authority code supplied by the client, respectively. The
 *       other values may or may not be set. (If the authority is EPSG,
 *       the implementer may consider using the corresponding metadata values
 *       in the EPSG tables.)</li>
 *   <li>When {@link net.seas.opengis.cs.CoordinateSystemFactory} creates an
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
    private static final long serialVersionUID = 2470505154745218555L;

    /**
     * This object name.
     */
    private final String name;

    /**
     * Create an object with the specified name.
     *
     * @param name This object name.
     */
    public Info(final String name)
    {
        this.name=name;
        ensureNonNull("name", name);
    }

    /**
     * Gets the name of this object.
     */
    public String getName()
    {return name;}

    /**
     * Gets the authority name, or <code>null</code> if unspecified.
     * An Authority is an organization that maintains definitions of Authority
     * Codes.  For example the European Petroleum Survey Group (EPSG) maintains
     * a database of coordinate systems, and other spatial referencing objects,
     * where each object has a code number ID.  For example, the EPSG code for a
     * WGS84 Lat/Lon coordinate system is '4326'.
     */
    public String getAuthority()
    {return null;}

    /**
     * Gets the authority-specific identification code, or <code>null</code> if unspecified.
     * The AuthorityCode is a compact string defined by an Authority to reference
     * a particular spatial reference object.  For example, the European Survey
     * Group (EPSG) authority uses 32 bit integers to reference coordinate systems,
     * so all their code strings will consist of a few digits.  The EPSG code for
     * WGS84 Lat/Lon is '4326'.
     */
    public String getAuthorityCode()
    {return null;}

    /**
     * Gets the alias, or <code>null</code> if there is none.
     */
    public String getAlias()
    {return null;}

    /**
     * Gets the abbreviation, or <code>null</code> if there is none.
     */
    public String getAbbreviation()
    {return null;}

    /**
     * Gets the provider-supplied remarks, or
     * <code>null</code> if there is none.
     */
    public String getRemarks()
    {return null;}

    /**
     * Returns a hash value for this info.
     */
    public int hashCode()
    {
        final String name = getName();
        return (name!=null) ? name.hashCode() : 0;
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
            return XClass.equals(this.getName(),          that.getName()         ) &&
                   XClass.equals(this.getAuthority(),     that.getAuthority()    ) &&
                   XClass.equals(this.getAuthorityCode(), that.getAuthorityCode()) &&
                   XClass.equals(this.getAlias(),         that.getAlias()        ) &&
                   XClass.equals(this.getAbbreviation(),  that.getAbbreviation() ) &&
                   XClass.equals(this.getRemarks(),       that.getRemarks()      );
        }
        return false;
    }

    /**
     * Returns a string representation of this info.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getName()+']';}

    /**
     * Returns an OpenGIS interface for this info.
     * The returned object is suitable for RMI use.
     */
    public CS_Info toOpenGIS()
    {return new Export();}

    /**
     * Transform a {@link Unit} object into a {@link CS_Unit}.
     * This method is provided for interoperability with OpenGIS.
     */
    static CS_Unit toOpenGIS(final Unit unit)
    {
        if (unit.canConvert(Unit.METRE))
        {
            final double metersPerUnits = unit.convert(1, Unit.METRE);
            // TODO: returns a LinearUnit
        }
        if (unit.canConvert(Unit.DEGREE))
        {
            final double degreesPerUnits = unit.convert(1, Unit.DEGREE);
            // TODO: returns an AngularUnit
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Make sure an argument is non-null.
     *
     * @param  name   Argument name.
     * @param  object User argument.
     * @throws IllegalArgumentException if <code>object</code> is null.
     */
    static void ensureNonNull(final String name, final Object object) throws IllegalArgumentException
    {if (object==null) throw new IllegalArgumentException(Resources.format(Clé.NULL_ARGUMENT¤1, name));}

    /**
     * Make sure an array element is non-null.
     *
     * @param  name  Argument name.
     * @param  array User argument.
     * @param  index Element to check.
     * @throws IllegalArgumentException if <code>array[i]</code> is null.
     */
    static void ensureNonNull(final String name, final Object[] array, final int index) throws IllegalArgumentException
    {if (array[index]==null) throw new IllegalArgumentException(Resources.format(Clé.NULL_ARGUMENT¤1, name+'['+index+']'));}

    /**
     * Make sure that the specified unit is a linear one.
     *
     * @param  unit Unit to check.
     * @throws IllegalArgumentException if <code>unit</code> is not a linear unit.
     */
    static void ensureLinearUnit(final Unit unit) throws IllegalArgumentException
    {
        if (!Unit.METRE.canConvert(unit))
            throw new IllegalArgumentException(Resources.format(Clé.NON_LINEAR_UNIT¤1, unit));
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
            throw new IllegalArgumentException(Resources.format(Clé.NON_ANGULAR_UNIT¤1, unit));
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
         * Gets the name.
         */
        public String getName() throws RemoteException
        {return Info.this.getName();}

        /**
         * Gets the authority name.
         */
        public String getAuthority() throws RemoteException
        {return Info.this.getAuthority();}

        /**
         * Gets the authority-specific identification code.
         */
        public String getAuthorityCode() throws RemoteException
        {return Info.this.getAuthorityCode();}

        /**
         * Gets the alias.
         */
        public String getAlias() throws RemoteException
        {return Info.this.getAlias();}

        /**
         * Gets the abbreviation.
         */
        public String getAbbreviation() throws RemoteException
        {return Info.this.getAbbreviation();}

        /**
         * Gets the provider-supplied remarks.
         */
        public String getRemarks() throws RemoteException
        {return Info.this.getRemarks();}

        /**
         * Gets a Well-Known text representation of this object.
         */
        public String getWKT() throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Gets an XML representation of this object.
         */
        public String getXML() throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}
    }
}
