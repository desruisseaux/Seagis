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
 * Procedure used to measure positions on the surface of the Earth.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_Datum
 */
public class HorizontalDatum extends Datum
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 3506060221517273330L;

    /**
     * The ellipsoid for this datum.
     */
    private final Ellipsoid ellipsoid;

    /**
     * Preferred parameters for a Bursa Wolf transformation.
     */
    private final WGS84ConversionInfo parameters;

    /**
     * Construct a new datum with the
     * specified name and datum type.
     *
     * @param name The datum name.
     * @param type The datum type.
     * @param ellipsoid Ellipsoid to use in horizontal datum.
     * @param Suggested approximate conversion to WGS84, or
     *        <code>null</code> if there is none.
     */
    protected HorizontalDatum(final String name, final DatumType.Horizontal type, final Ellipsoid ellipsoid,  final WGS84ConversionInfo parameters)
    {
        super(name, type);
        this.ellipsoid  = ellipsoid;
        this.parameters = (parameters!=null) ? parameters.clone() : null;
        ensureNonNull("ellipsoid", ellipsoid);
    }

    /**
     * Gets the type of the datum as an enumerated code.
     */
    public DatumType.Horizontal getDatumType()
    {return (DatumType.Horizontal) super.getDatumType();}

    /**
     * Returns the ellipsoid.
     */
    public Ellipsoid getEllipsoid()
    {return ellipsoid;}

    /**
     * Gets preferred parameters for a Bursa Wolf transformation into WGS84. 
     * The 7 returned values correspond to (dx,dy,dz) in meters, (ex,ey,ez)
     * in arc-seconds, and scaling in parts-per-million.  This method will
     * always returns <code>null</code> for horizontal datums with type
     * {@link DatumType.Horizontal#OTHER}. This method may also returns
     * <code>null</code> if no suitable transformation is available.
     */
    public WGS84ConversionInfo getWGS84Parameters()
    {return (parameters!=null) ? parameters.clone() : null;}

    /**
     * Compares the specified object
     * with this datum for equality.
     */
    public boolean equals(final Object object)
    {return (object instanceof HorizontalDatum) && equals((HorizontalDatum)object);}

    /**
     * Compares the specified object
     * with this datum for equality.
     */
    final boolean equals(final HorizontalDatum that)
    {
        return super.equals(that) &&
               XClass.equals(this.getEllipsoid(),       that.getEllipsoid()) &&
               XClass.equals(this.getWGS84Parameters(), that.getWGS84Parameters());
    }
}
