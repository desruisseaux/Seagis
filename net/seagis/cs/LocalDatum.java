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
import org.opengis.cs.CS_LocalDatum;

// Remote Method Invocation
import java.rmi.RemoteException;

// Miscellaneous
import java.util.Map;


/**
 * Local datum.
 * If two local datum objects have the same datum type and name, then they
 * can be considered equal.  This means that coordinates can be transformed
 * between two different local coordinate systems, as long as they are based
 * on the same local datum.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_LocalDatum
 */
public class LocalDatum extends Datum
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 426762179497761085L;

    /**
     * Creates a local datum.
     *
     * @param name Name to give new object.
     * @param localDatumType Type of local datum to create.
     *
     * @see org.opengis.cs.CS_CoordinateSystemFactory#createLocalDatum
     */
    public LocalDatum(final String name, final DatumType.Local type)
    {super(name, type);}

    /**
     * Creates a local datum.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param localDatumType Type of local datum to create.
     */
    LocalDatum(final Map properties, final DatumType type)
    {super(properties, type);}

    /**
     * Gets the type of the datum as an enumerated code.
     *
     * Note: return type will be changed to {@link DatumType.Local}
     *       when we will be able to use generic types (with JDK 1.5).
     *
     * @see org.opengis.cs.CS_LocalDatum#getDatumType()
     */
    public DatumType/*.Local*/ getDatumType()
    {return (DatumType.Local) super.getDatumType();}

    /**
     * Fill the part inside "[...]".
     * Used for formatting Well Know Text (WKT).
     */
    String addString(final StringBuffer buffer)
    {
        super.addString(buffer);
        return "LOCAL_DATUM";
    }

    /**
     * Returns an OpenGIS interface for this datum.
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
     * Wrap a {@link LocalDatum} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends Datum.Export implements CS_LocalDatum
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}
    }
}