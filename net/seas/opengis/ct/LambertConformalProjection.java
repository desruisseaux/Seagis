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
 * Projection conique conforme de Lambert. Les aires et les formes sont déformées
 * à mesure que l'on s'éloigne de parallèles standards. Les angles sont vrais dans
 * une région limitée. Cette projection est utilisée pour les cartes de l'Amérique
 * du Nord. Elle utilise par défaut une latitude centrale de 40°N.
 * <br><br>
 *
 * Référence: John P. Snyder (Map Projections - A Working Manual,
 *            U.S. Geological Survey Professional Paper 1395, 1987)
 *
 * @version 1.0
 * @author André Gosselin
 * @author Martin Desruisseaux
 */
final class LambertConformalProjection extends ConicProjection
{
    /**
     * Variables internes
     * pour les calculs.
     */
    private final double n,F,rho0;

    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected LambertConformalProjection(final Parameter[] parameters) throws MissingParameterException
    {
        //////////////////////////
        //   Fetch parameters   //
        //////////////////////////
        super(parameters);
        final double defaultLatitude =       Parameter.getValue(parameters, "latitude_of_origin", 0);
        final double phi1= latitudeToRadians(Parameter.getValue(parameters, "standard_parallel1", defaultLatitude), true);
        final double phi2= latitudeToRadians(Parameter.getValue(parameters, "standard_parallel2", defaultLatitude), true);

        //////////////////////////
        //  Compute constants   //
        //////////////////////////
        if (Math.abs(phi1 + phi2) < EPS)
            throw new IllegalArgumentException(Resources.format(Clé.ANTIPODE_LATITUDES¤2, new Latitude(Math.toDegrees(phi1)), new Latitude(Math.toDegrees(phi2))));

        final double  cosphi = Math.cos(phi1);
        final double  sinphi = Math.sin(phi1);
        final boolean secant = Math.abs(phi1-phi2) > EPS;
        if (isSpherical)
        {
            if (secant)
            {
                n = Math.log(cosphi / Math.cos(phi2)) /
                    Math.log(Math.tan((Math.PI/4) + 0.5*phi2) /
                    Math.tan((Math.PI/4) + 0.5 * phi1));
            }
            else n = sinphi;
            F = cosphi * Math.pow(Math.tan((Math.PI/4) + 0.5*phi1), n) / n;
            if (Math.abs(Math.abs(centralLatitude) - (Math.PI/2)) >= EPS)
            {
                rho0 = F * Math.pow(Math.tan((Math.PI/4) + 0.5*centralLatitude), -n);
            }
            else rho0 = 0.0;
        }
        else
        {
            final double m1 = msfn(sinphi, cosphi);
            final double t1 = tsfn(phi1, sinphi);
            if (secant)
            {
                final double sinphi2 = Math.sin(phi2);
                final double m2 = msfn(sinphi2, Math.cos(phi2));
                final double t2 = tsfn(phi2, sinphi2);
                n = Math.log(m1/m2) / Math.log(t1/t2);
            }
            else n = sinphi;
            F = m1 * Math.pow (t1, -n) / n;
            if (Math.abs(Math.abs(centralLatitude) - (Math.PI/2)) >= EPS)
            {
                rho0 = F * Math.pow(tsfn(centralLatitude, Math.sin(centralLatitude)), n);
            }
            else rho0 = 0.0;
        }
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(Clé.LAMBERT_CONFORMAL);}

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

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        double rho;
        if (Math.abs(Math.abs(y) - (Math.PI/2)) < EPS)
        {
            if (y*n <= 0)
                throw new TransformException(Resources.format(Clé.POLE_PROJECTION¤1, new Latitude(y)));
            else rho = 0;
        }
        else
        {
            if (isSpherical)
                rho = Math.pow(Math.tan((Math.PI/4) + 0.5*y), -n);
            else
                rho = Math.pow(tsfn(y, Math.sin(y)), n);
            rho *= F;
        }
        x = n * (x-centralLongitude);
        y = a * (rho0 - rho * Math.cos(x));
        x = a * (rho * Math.sin(x));

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
        double x   =        ptSrc.getX() / a;
        double y   = rho0 - ptSrc.getY()/a;
        double rho = Math.sqrt(x*x + y*y);

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        if (rho > EPS)
        {
            if (n < 0)
            {
                rho = -rho;
                x = -x;
                y = -y;
            }
            x = centralLongitude + Math.atan2(x, y)/n;
            if (isSpherical)
                y = 2.0 * Math.atan(Math.pow(F/rho, 1.0/n)) - (Math.PI/2);
            else
                y = cphi2(Math.pow(rho/F, 1.0/n));
        }
        else
        {
            x = centralLongitude;
            y = n < 0 ? -(Math.PI/2) : (Math.PI/2);
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
        final long code = Double.doubleToLongBits(F);
        return (int) code ^ (int) (code >>> 32) ^ super.hashCode();
    }

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final LambertConformalProjection that = (LambertConformalProjection) object;
            return Double.doubleToLongBits(this.n)    == Double.doubleToLongBits(that.n) &&
                   Double.doubleToLongBits(this.F)    == Double.doubleToLongBits(that.F) &&
                   Double.doubleToLongBits(this.rho0) == Double.doubleToLongBits(that.rho0);
        }
        return false;
    }

    /**
     * Informations about a {@link LambertConformalProjection}.
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
        {super("Lambert_Conformal_Conic_2SP", Clé.LAMBERT_CONFORMAL);}

        /**
         * Create a new map projection.
         */
        public MathTransform create(final Parameter[] parameters)
        {return new LambertConformalProjection(parameters);}

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
                new Parameter("central_meridian",    0),
                new Parameter("standard_parallel1",  0),
                new Parameter("standard_parallel2",  0)
            };
        }
    }
}
