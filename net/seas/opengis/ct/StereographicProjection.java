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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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

// OpenGIS (SEAS) dependencies
import net.seas.opengis.cs.Projection;
import net.seas.opengis.cs.Ellipsoid;
import net.seas.opengis.cs.Parameter;
import net.seas.opengis.pt.Latitude;

// Miscellaneous
import java.util.Locale;
import java.awt.geom.Point2D;
import net.seas.resources.Resources;


/**
 * Projection st�r�ographique. Les directions � partir du point central sont vrais,
 * mais les aires et les longueurs deviennent de plus en plus d�form�es � mesure que
 * l'on s'�loigne du centre. Cette projection est utilis�e pour repr�senter des r�gions
 * polaires. Elle peut �tre appropri�e pour d'autres r�gions ayant une forme circulaire.
 * <br><br>
 *
 * R�f�rence: John P. Snyder (Map Projections - A Working Manual,
 *            U.S. Geological Survey Professional Paper 1395, 1987)
 *
 * @version 1.0
 * @author Andr� Gosselin
 * @author Martin Desruisseaux
 */
final class StereographicProjection extends PlanarProjection
{
    /**
     * Nombre maximal d'it�rations permises lors
     * du calcul de la projection inverse.
     */
    private static final int MAX_ITER=10;

    /** Projection mode for switch statement. */ private static final int   SPHERICAL_NORTH      = 0;
    /** Projection mode for switch statement. */ private static final int   SPHERICAL_SOUTH      = 1;
    /** Projection mode for switch statement. */ private static final int ELLIPSOIDAL_SOUTH      = 2;
    /** Projection mode for switch statement. */ private static final int ELLIPSOIDAL_NORTH      = 3;
    /** Projection mode for switch statement. */ private static final int   SPHERICAL_OBLIQUE    = 4;
    /** Projection mode for switch statement. */ private static final int   SPHERICAL_EQUATORIAL = 5;
    /** Projection mode for switch statement. */ private static final int ELLIPSOIDAL_EQUATORIAL = 6;
    /** Projection mode for switch statement. */ private static final int ELLIPSOIDAL_OBLIQUE    = 7;

    /**
     * Projection mode. It must be one of the following constants:
     * {@link #SPHERICAL_NORTH}, {@link #SPHERICAL_SOUTH},
     * {@link #ELLIPSOIDAL_NORTH}, {@link #ELLIPSOIDAL_SOUTH}.
     * {@link #SPHERICAL_OBLIQUE}, {@link #SPHERICAL_EQUATORIAL},
     * {@link #ELLIPSOIDAL_OBLIQUE} or {@link #ELLIPSOIDAL_EQUATORIAL}.
     */
    private final int mode;

    /**
     * Global scale factor. Value <code>ak0</code>
     * is equals to <code>{@link #a}*k0</code>.
     */
    private final double k0, ak0;

    /**
     * Facteurs utilis�s lors des projections
     * obliques et equatorialles.
     */
    private final double sinphi0, cosphi0, chi1, sinChi1, cosChi1;

    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected StereographicProjection(final Parameter[] parameters) throws MissingParameterException
    {this(parameters, true, true);}

    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @param  polar <code>true</code> for polar projection.
     * @param  auto  <code>true</code> if projection (polar vs oblique)
     *               can be selected automatically.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    private StereographicProjection(final Parameter[] parameters, final boolean polar, final boolean auto) throws MissingParameterException
    {
        //////////////////////////
        //   Fetch parameters   //
        //////////////////////////
        super(parameters);
        final double defaultLatitude = Parameter.getValue(parameters, "latitude_of_origin", polar ? 90 : 0);
        final double latitudeTrueScale = Math.abs(latitudeToRadians(Parameter.getValue(parameters, "latitude_true_scale", defaultLatitude), true));

        //////////////////////////
        //  Compute constants   //
        //////////////////////////
        if (auto ? (Math.abs(Math.abs(centralLatitude)-(Math.PI/2)) < EPS) : polar)
        {
            if (centralLatitude<0) {centralLatitude = -(Math.PI/2); mode = (isSpherical) ? SPHERICAL_SOUTH : ELLIPSOIDAL_SOUTH;}
            else                   {centralLatitude = +(Math.PI/2); mode = (isSpherical) ? SPHERICAL_NORTH : ELLIPSOIDAL_NORTH;}
        }
        else if (Math.abs(centralLatitude)<EPS)
        {
            centralLatitude = 0;
            mode = (isSpherical) ? SPHERICAL_EQUATORIAL : ELLIPSOIDAL_EQUATORIAL;
        }
        else
        {
            mode = (isSpherical) ? SPHERICAL_OBLIQUE : ELLIPSOIDAL_OBLIQUE;
        }
        switch (mode)
        {
            default:
            {
                cosphi0 = Math.cos(centralLatitude);
                sinphi0 = Math.sin(centralLatitude);
                chi1    = 2.0 * Math.atan(ssfn(centralLatitude, sinphi0)) - (Math.PI/2);
                cosChi1 = Math.cos(chi1);
                sinChi1 = Math.sin(chi1);
                break;
            }
            case SPHERICAL_EQUATORIAL:
            case ELLIPSOIDAL_EQUATORIAL:
            {
                cosphi0 = 1.0;
                sinphi0 = 0.0;
                chi1    = 0.0;
                cosChi1 = 1.0;
                sinChi1 = 0.0;
                break;
            }
        }

        //////////////////////////
        //  Compute k0 and ak0  //
        //////////////////////////
        switch (mode)
        {
            default:
            {
                throw new AssertionError(mode); // Should not happen.
            }

            case ELLIPSOIDAL_NORTH:
            case ELLIPSOIDAL_SOUTH:
            {
                if (Math.abs(latitudeTrueScale-(Math.PI/2)) >= EPS)
                {
                    final double t = Math.sin(latitudeTrueScale);
                    k0 = (Math.cos(latitudeTrueScale) / (Math.sqrt(1-es * t*t))) / tsfn(latitudeTrueScale, t);
                }
                else
                {
                    // True scale at pole
                    k0 = 2.0 / Math.sqrt(Math.pow(1+e, 1+e)*Math.pow(1-e, 1-e));
                }
                break;
            }

            case SPHERICAL_NORTH:
            case SPHERICAL_SOUTH:
            {
                if (Math.abs(latitudeTrueScale - (Math.PI/2)) >= EPS)
                    k0 = 1 + Math.sin(latitudeTrueScale);
                else
                    k0 = 2;
                break;
            }

            case ELLIPSOIDAL_OBLIQUE:
            case ELLIPSOIDAL_EQUATORIAL:
            {
                k0 = 2.0*Math.cos(centralLatitude)/Math.sqrt(1-es * sinphi0*sinphi0);
                break;
            }

            case SPHERICAL_OBLIQUE:
            case SPHERICAL_EQUATORIAL:
            {
                k0 = 2;
                break;
            }
        }
        ak0 = a*k0;
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(Cl�.STEREOGRAPHIC);}

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
        x -= centralLongitude;
        final double coslat = Math.cos(y);
        final double sinlat = Math.sin(y);
        final double coslon = Math.cos(x);
        final double sinlon = Math.sin(x);

        switch (mode)
        {
            default:
            {
                throw new AssertionError(mode); // Should not happen.
            }
            case ELLIPSOIDAL_NORTH:
            {
                final double rho = ak0 * tsfn(y, sinlat);
                x =  rho * sinlon;
                y = -rho * coslon;
                break;
            }
            case ELLIPSOIDAL_SOUTH:
            {
                final double rho = ak0 * tsfn(-y, -sinlat);
                x = rho * sinlon;
                y = rho * coslon;
                break;
            }
            case SPHERICAL_NORTH:
            {
                if (!(Math.abs(1+sinlat) >= TOL))
                {
                    throw new TransformException(Resources.format(Cl�.INFINITY_IN_PROJECTION));
                }
                // (21-8)
                final double f= ak0 * coslat / (1+sinlat);// == tan (pi/4 - phi/2)
                x =  f * sinlon; // (21-5)
                y = -f * coslon; // (21-6)
                break;
            }
            case SPHERICAL_SOUTH:
            {
                if (!(Math.abs(1-sinlat) >= TOL))
                {
                    throw new TransformException(Resources.format(Cl�.INFINITY_IN_PROJECTION));
                }
                // (21-12)
                final double f= ak0 * coslat / (1-sinlat);// == tan (pi/4 + phi/2)
                x = f * sinlon; // (21-9)
                y = f * coslon; // (21-10)
                break;
            }
            case SPHERICAL_EQUATORIAL:
            {
                double f = 1.0 + coslat*coslon;
                if (!(f >= TOL))
                {
                    throw new TransformException(Resources.format(Cl�.INFINITY_IN_PROJECTION));
                }
                f = ak0/f;
                x = f * coslat * sinlon;
                y = f * sinlat;
                break;
            }
            case SPHERICAL_OBLIQUE:
            {
                double f = 1.0 + sinphi0*sinlat + cosphi0*coslat*coslon; // (21-4)
                if (!(f >= TOL))
                {
                    throw new TransformException(Resources.format(Cl�.INFINITY_IN_PROJECTION));
                }
                f = ak0/f;
                x = f * coslat * sinlon;                               // (21-2)
                y = f * (cosphi0 * sinlat - sinphi0 * coslat * coslon);// (21-3)
                break;
            }
            case ELLIPSOIDAL_EQUATORIAL:
            {
                final double chi = 2.0 * Math.atan(ssfn(y, sinlat)) - (Math.PI/2);
                final double sinChi = Math.sin(chi);
                final double cosChi = Math.cos(chi);
                final double A = ak0 / (1.0 + cosChi*coslon);
                x = A * cosChi*sinlon;
                y = A * sinChi;
                break;
            }
            case ELLIPSOIDAL_OBLIQUE:
            {
                final double chi = 2.0 * Math.atan(ssfn(y, sinlat)) - (Math.PI/2);
                final double sinChi = Math.sin(chi);
                final double cosChi = Math.cos(chi);
                final double cosChi_coslon = cosChi*coslon;
                final double A = ak0 / cosChi1 / (1 + sinChi1*sinChi + cosChi1*cosChi_coslon);
                x = A * cosChi*sinlon;
                y = A * (cosChi1*sinChi - sinChi1*cosChi_coslon);
                break;
            }
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
        double x = ptSrc.getX() / a;
        double y = ptSrc.getY() / a;

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        final double rho = Math.sqrt(x*x + y*y);
choice: switch (mode)
        {
            default:
            {
                throw new AssertionError(mode); // Should not happen.
            }
            case SPHERICAL_NORTH:
            {
                y = -y;
                // fallthrough
            }
            case SPHERICAL_SOUTH:
            {
                // (20-17) call atan2(x,y) to properly deal with y==0
                x = (Math.abs(x)<TOL && Math.abs(y)<TOL) ? centralLongitude : Math.atan2(x, y) + centralLongitude;
                if (Math.abs(rho)<TOL)
                    y = centralLatitude;
                else
                {
                    final double c = 2.0 * Math.atan(rho/k0);
                    final double cosc = Math.cos(c);
                    y = (mode==SPHERICAL_NORTH) ? Math.asin(cosc) : Math.asin(-cosc); // (20-14) with phi1=90
                }
                break;
            }
            case SPHERICAL_EQUATORIAL:
            {
                if (Math.abs(rho)<TOL)
                {
                    y = 0.0;
                    x = centralLongitude;
                }
                else
                {
                    final double c = 2.0 * Math.atan(rho/k0);
                    final double cosc = Math.cos(c);
                    final double sinc = Math.sin(c);
                    y = Math.asin(y * sinc/rho); // (20-14)  with phi1=0
                    final double t  = x*sinc;
                    final double ct = rho*cosc;
                    x = (Math.abs(t)<TOL && Math.abs(ct)<TOL) ? centralLongitude : Math.atan2(t, ct)+centralLongitude;
                }
                break;
            }
            case SPHERICAL_OBLIQUE:
            {
                if (Math.abs(rho) < TOL)
                {
                    y = centralLatitude;
                    x = centralLongitude;
                }
                else
                {
                    final double c = 2.0 * Math.atan(rho/k0);
                    final double cosc = Math.cos(c);
                    final double sinc = Math.sin(c);
                    final double ct = rho*cosphi0*cosc - y*sinphi0*sinc; // (20-15)
                    final double t  = x*sinc;
                    y = Math.asin(cosc*sinphi0 + y*sinc*cosphi0/rho);
                    x = (Math.abs(ct)<TOL && Math.abs(t)<TOL) ? centralLongitude : Math.atan2(t, ct)+centralLongitude;
                }
                break;
            }
            case ELLIPSOIDAL_SOUTH:
            {
                y = -y;
                // fallthrough
            }
            case ELLIPSOIDAL_NORTH:
            {
                final double t = rho/k0;
                /*
                 * Compute lat using iterative technique.
                 */
                final double halfe = e / 2.0;
                double phi0=0;
                for (int i=MAX_ITER; --i>=0;)
                {
                    final double esinphi = e * Math.sin(phi0);
                    final double phi = (Math.PI/2) - 2.0*Math.atan(t*Math.pow((1-esinphi)/(1+esinphi), halfe));
                    if (Math.abs(phi-phi0) < TOL)
                    {
                        x = (Math.abs(rho)<TOL) ? centralLongitude : Math.atan2(x, -y) + centralLongitude;
                        y = (mode==ELLIPSOIDAL_NORTH) ? phi : -phi;
                        break choice;
                    }
                    phi0=phi;
                }
                throw new TransformException(Resources.format(Cl�.NO_CONVERGENCE_FOR_LATITUDE�1, new Latitude(ptSrc.getY())));
            }
            case ELLIPSOIDAL_OBLIQUE:
            {
                // fallthrough
            }
            case ELLIPSOIDAL_EQUATORIAL:
            {
                final double ce = 2.0 * Math.atan2(rho*cosChi1, k0);
                final double cosce = Math.cos(ce);
                final double since = Math.sin(ce);
                final double chi = (Math.abs(rho)>=TOL) ? Math.asin(cosce*sinChi1 + (y*since*cosChi1 / rho)) : chi1;
                final double t = Math.tan(Math.PI/4.0 + chi/2.0);
                /*
                 * Compute lat using iterative technique.
                 */
                final double halfe = e/2.0;
                double phi0=chi;
                for (int i=MAX_ITER; --i>=0;)
                {
                    final double esinphi = e*Math.sin(phi0);
                    final double phi = 2.0 * Math.atan (t*Math.pow((1+esinphi)/(1-esinphi), halfe)) - (Math.PI/2);
                    if (Math.abs(phi-phi0) < TOL)
                    {
                        x = (Math.abs(rho)<TOL) ? centralLongitude :
                             Math.atan2(x*since, rho*cosChi1*cosce - y*sinChi1*since) + centralLongitude;
                        y = phi;
                        break choice;
                    }
                    phi0=phi;
                }
                throw new TransformException(Resources.format(Cl�.NO_CONVERGENCE_FOR_LATITUDE�1, new Latitude(ptSrc.getY())));
            }
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
     * Compute part of function (3-1) from Snyder
     */
    private double ssfn(double phi, double sinphi)
    {
        sinphi *= e;
        return Math.tan((Math.PI/4.0) + phi/2.0) *
               Math.pow((1-sinphi) / (1+sinphi), e/2.0);
    }

    /**
     * Returns a hash value for this map projection.
     */
    public int hashCode()
    {
        final long code = Double.doubleToLongBits(k0);
        return ((int)code ^ (int)(code >>> 32)) + 37*super.hashCode();
    }

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    public boolean equals(final Object object)
    {
        if (super.equals(object))
        {
            final StereographicProjection that = (StereographicProjection) object;
            return Double.doubleToLongBits(this.     k0) == Double.doubleToLongBits(that.     k0) &&
                   Double.doubleToLongBits(this.    ak0) == Double.doubleToLongBits(that.    ak0) &&
                   Double.doubleToLongBits(this.sinphi0) == Double.doubleToLongBits(that.sinphi0) &&
                   Double.doubleToLongBits(this.cosphi0) == Double.doubleToLongBits(that.cosphi0) &&
                   Double.doubleToLongBits(this.   chi1) == Double.doubleToLongBits(that.   chi1) &&
                   Double.doubleToLongBits(this.sinChi1) == Double.doubleToLongBits(that.sinChi1) &&
                   Double.doubleToLongBits(this.cosChi1) == Double.doubleToLongBits(that.cosChi1);
        }
        return false;
    }

    /**
     * Informations about a {@link StereographicProjection}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static final class Provider extends MathTransformProvider
    {
        /**
         * <code>true</code> for polar stereographic, or
         * <code>false</code> for equatorial and oblique
         * stereographic.
         */
        private final boolean polar;

        /**
         * <code>true</code> if polar/oblique/equatorial
         * stereographic can be automatically choosen.
         */
        private final boolean auto;

        /**
         * Construct a new registration. The type (polar, oblique
         * or equatorial) will be choosen automatically according
         * the latitude or origin.
         */
        public Provider()
        {
            super("Stereographic", Cl�.STEREOGRAPHIC);
            polar = true;
            auto  = true;
        }

        /**
         * Construct an object for polar or oblique stereographic.
         *
         * @param polar <code>true</code> for polar stereographic, or
         *              <code>false</code> for equatorial and oblique
         *              stereographic.
         */
        public Provider(final boolean polar)
        {
            super(polar ? "Polar_Stereographic" : "Oblique_Stereographic", Cl�.STEREOGRAPHIC);
            this.polar = polar;
            this.auto  = false;
        }

        /**
         * Create a new map projection.
         */
        public MathTransform create(final Parameter[] parameters)
        {return new StereographicProjection(parameters, polar, auto);}

        /**
         * Returns the default parameters.
         */
        public Parameter[] getDefaultParameters()
        {
            final double defaultLatitude = polar ? 90 : 0;
            return new Parameter[]
            {
                new Parameter("semi_major", SEMI_MAJOR),
                new Parameter("semi_minor", SEMI_MINOR),
                new Parameter("latitude_of_origin",   0),
                new Parameter("central_meridian",    defaultLatitude),
                new Parameter("latitude_true_scale", defaultLatitude)
            };
        }
    }
}
