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
import net.seas.util.XClass;


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
    private static final long serialVersionUID = 5604979072241525415L;

    /**
     * Construct a new datum with the
     * specified name and datum type.
     *
     * @param name The datum name.
     * @param type The datum type.
     */
    protected LocalDatum(final String name, final DatumType.Local type)
    {super(name, type);}

    /**
     * Gets the type of the datum as an enumerated code.
     */
    public DatumType.Local getDatumType()
    {return (DatumType.Local) super.getDatumType();}
}
