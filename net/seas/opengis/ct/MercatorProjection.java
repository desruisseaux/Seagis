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
 *
 *    This package contains formulas from the PROJ package of USGS.
 *    USGS's work is fully acknowledged here.
 */
package net.seas.opengis.ct;

// Miscellaneous
import java.util.Locale;
import java.awt.geom.Point2D;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.cs.Ellipsoid;
import net.seas.resources.Resources;


/**
 * Projection cylindrique de Mercator. Les parallèles et les méridients apparaissent
 * comme des lignes droites et se croisent à angles droits; cette projection produit
 * donc des cartes rectangulaires. L'échelle est vrai le long de l'équateur (par défaut)
 * ou le long de deux parallèles équidistants de l'équateur. Cette projection est utilisée
 * pour représenter des régions près de l'équateur. Elle est aussi souvent utilisée pour la
 * navigation maritime parce que toutes les lignes droites sur la carte sont des lignes
 * <em>loxodromiques</em>, c'est-à-dire qu'un navire suivant cette ligne garderait azimuth
 * constant sur son compas.
 * <br><br>
 *
 * Référence: John P. Snyder (Map Projections - A Working Manual,
 *            U.S. Geological Survey Professional Paper 1395, 1987)
 *
 * @version 1.0
 * @author André Gosselin
 * @author Martin Desruisseaux
 */
final class MercatorProjection extends CylindricalProjection
{
    /**
     * Global scale factor. Value <code>ak0</code>
     * is equals to <code>{@link #a}*k0</code>.
     */
    private final double ak0;

    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    public MercatorProjection(final Parameter[] parameters) throws MissingParameterException
    {
        //////////////////////////
        //   Fetch parameters   //
        //////////////////////////
        super(parameters);
        centralLatitude = latitudeToRadians(Parameter.getValue(parameters, "latitude_of_origin", 0), false);
        final double latitudeTrueScale = Math.abs(centralLatitude);

        //////////////////////////
        //  Compute constants   //
        //////////////////////////
        if (isSpherical)
        {
            ak0 = a*Math.cos(latitudeTrueScale);
        }
        else
        {
            ak0 = a*msfn(Math.sin(latitudeTrueScale), Math.cos(latitudeTrueScale));
        }
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(Clé.CYLINDRICAL_MERCATOR);}

    /**
     * Transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {
        //////////////////////////
        //   Arguments check    //
        //////////////////////////
        double x=ptSrc.getX(); if (!(x>=XMIN && x<=XMAX)) throw badLongitudeException(x); x=Math.toRadians(x);
        double y=ptSrc.getY(); if (!(y>=YMIN && y<=YMAX)) throw badLatitudeException (y); y=Math.toRadians(y);

        if (Math.abs(y) > (Math.PI/2 - EPS))
        {
            throw new TransformException(Resources.format(Clé.POLE_PROJECTION¤1, new Latitude(ptSrc.getY())));
        }

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        x = (x-centralLongitude)*ak0;
        if (isSpherical)
        {
            y =  ak0*Math.log(Math.tan((Math.PI/4) + 0.5*y));
        }
        else
        {
            y = -ak0*Math.log(tsfn(y, Math.sin(y)));
        }

        //////////////////////////
        //    Store result      //
        //////////////////////////
        final double check;
        assert (check=distance(ptSrc, x,y, true))<=MAX_ERROR : check;

        if (ptDst!=null)
        {
            ptDst.setLocation(x,y);
            return ptDst;
        }
        else return new Point2D.Double(x,y);
    }

    /**
     * Transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     */
    public Point2D inverseTransform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {
        double x = ptSrc.getX();
        double y = ptSrc.getY();

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        x = x/ak0 + centralLongitude;
        y = Math.exp(-y/ak0);
        if (isSpherical)
        {
            y = (Math.PI/2) - 2.0*Math.atan(y);
        }
        else
        {
            y = cphi2(y);
        }

        //////////////////////////
        //    Check result      //
        //////////////////////////
        x=Math.toDegrees(x); if (!(x>=XMIN && x<=XMAX)) throw badLongitudeException(x);
        y=Math.toDegrees(y); if (!(y>=YMIN && y<=YMAX)) throw badLatitudeException (y);

        //////////////////////////
        //    Store result      //
        //////////////////////////
        final double check;
        assert (check=distance(ptSrc, x,y, false))<=MAX_ERROR : check;

        if (ptDst!=null)
        {
            ptDst.setLocation(x,y);
            return ptDst;
        }
        else return new Point2D.Double(x,y);
    }

    /**
     * Returns a hash value for this projection.
     */
    public int hashCode()
    {
        final long code = Double.doubleToLongBits(ak0);
        return (int) code ^ (int) (code >>> 32) ^ super.hashCode();
    }

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    public boolean equals(final Object object)
    {return (object instanceof MercatorProjection) && equals((MercatorProjection) object);}

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    final boolean equals(final MercatorProjection that)
    {
        return super.equals(that) &&
               Double.doubleToLongBits(this.ak0) == Double.doubleToLongBits(that.ak0);
    }

    /**
     * Informations about a {@link MercatorProjection}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static final class Provider extends MathTransform.Provider
    {
        /**
         * Construct a new registration.
         */
        public Provider()
        {super("Mercator_1SP", Clé.CYLINDRICAL_MERCATOR);}

        /**
         * Create a new map projection.
         */
        public MathTransform create(final Parameter[] parameters)
        {return new MercatorProjection(parameters);}

        /**
         * Returns the default parameters.
         */
        public Parameter[] getDefaultParameters()
        {
            return new Parameter[]
            {
                new Parameter("semi_major", SEMI_MAJOR),
                new Parameter("semi_minor", SEMI_MINOR),
                new Parameter("latitude_of_origin",  0),
                new Parameter("central_meridian",    0)
            };
        }
    }
}
