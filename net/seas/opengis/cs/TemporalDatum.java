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
import java.util.Map;
import java.util.Locale;
import net.seas.resources.Resources;


/**
 * Procedure used to measure time.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TemporalDatum extends Datum
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -3132187036753923869L;

    /**
     * Default datum for Universal Time Clock (UTC).
     * UTC is based on an atomic clock, while GMT is
     * based on astronomical observations.
     */
    public static final TemporalDatum UTC = new TemporalDatum("UTC", DatumType.UTC);

    /**
     * Creates a temporal datum from an enumerated type value.
     *
     * @param name Name to give new object.
     * @param type Type of temporal datum to create.
     */
    public TemporalDatum(final String name, final DatumType.Temporal type)
    {super(name, type);}

    /**
     * Creates a temporal datum.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param localDatumType Type of temporal datum to create.
     */
    TemporalDatum(final Map<String,Object> properties, final DatumType type)
    {super(properties, type);}

    /**
     * Gets the type of the datum as an enumerated code.
     */
    public DatumType.Temporal getDatumType()
    {return (DatumType.Temporal) super.getDatumType();}
}
