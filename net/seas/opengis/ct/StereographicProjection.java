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
 * Projection stéréographique. Les directions à partir du point central sont vrais,
 * mais les aires et les longueurs deviennent de plus en plus déformées à mesure que
 * l'on s'éloigne du centre. Cette projection est utilisée pour représenter des régions
 * polaires. Elle peut être appropriée pour d'autres régions ayant une forme circulaire.
 *
 * @version 1.0
 * @author André Gosselin
 * @author Martin Desruisseaux
 */
final class StereographicProjection extends PlanarProjection
{
    /*
     * Référence des formules: John P. Snyder (Map Projections - A Working Manual,
     *                         U.S. Geological Survey Professional Paper 1395, 1987)
     */

    /**
     * Constante indiquant que cette projection
     * sera appliquée au dessus du pôle nord.
     * @see #SOUTH_POLE
     * @see #EQUATORIAL
     * @see #OBLIQUE
     */
    public static final int NORTH_POLE = +2;

    /**
     * Constante indiquant que cette projection
     * sera appliquée au dessus du pôle sud.
     * @see #NORTH_POLE
     * @see #EQUATORIAL
     * @see #OBLIQUE
     */
    public static final int SOUTH_POLE = -2;

    /**
     * Constante indiquant que cette projection
     * sera appliquée au dessus de l'équateur.
     * @see #NORTH_POLE
     * @see #SOUTH_POLE
     * @see #OBLIQUE
     */
    public static final int EQUATORIAL =  0;

    /**
     * Constante indiquant que cette projection
     * sera appliquée avec un angle oblique.
     * @see #NORTH_POLE
     * @see #SOUTH_POLE
     * @see #EQUATORIAL
     */
    public static final int OBLIQUE = 1;

    /**
     * Nombre maximal d'itérations permises lors
     * du calcul de la projection inverse.
     */
    private static final int MAX_ITER=10;

    /**
     * Latitude des échelles vrais, en radians.
     */
    private final double lat_ts;

    /**
     * Coéfficient multiplicatif pour les projections. Ce coéfficient
     * aura été multiplié par {@link #a}, le rayon de la Terre.
     */
    private final double k0;

    /**
     * Variables internes calculées lors
     * de l'initialisation de la projection.
     */
    private final double sinphi0, cosphi0, chi1, sinChi1, cosChi1;

    /**
     * Mode de cette projection. Ce sera une des constantes suivantes:
     * {@link #NORTH_POLE},
     * {@link #SOUTH_POLE}, 
     * {@link #EQUATORIAL} ou
     * {@link #OBLIQUE}.
     */
    private final int mode;

    /**
     * Construit une projection cartographique
     * qui utilisera l'ellipsoïde spécifié.
     */
    public StereographicProjection(final Ellipsoid ellipsoid, final Point2D centroid)
    {this(ellipsoid, centroid, centroid.getY());}

    /**
     * Construit une projection cartographique
     * qui utilisera l'ellipsoïde spécifié.
     *
     * @param ellipsoid Ellipsoïde à utiliser pour cette projection cartographique.
     * @param centroid  Coordonnées géographique du centre de la projection.
     * @param latitudeTrueScale Latitude où les échelles seront exactes.
     */
    public StereographicProjection(final Ellipsoid ellipsoid, final Point2D centroid, final double latitudeTrueScale)
    {
        super(ellipsoid, centroid);
        latitudeToRadians(centroid.getY(), false); // Argument check.
        lat_ts = Math.abs(latitudeToRadians(latitudeTrueScale, true));

        if (Math.abs(Math.abs(centralLatitude) - (Math.PI/2)) < EPS)
            mode = (centralLatitude<0) ? SOUTH_POLE : NORTH_POLE; // ±90°
        else
            mode = (Math.abs(centralLatitude)<EPS) ? EQUATORIAL : OBLIQUE;

        cosphi0 = Math.cos(centralLatitude);
        sinphi0 = Math.sin(centralLatitude);
        double k0 = 2.0;
        switch (mode)
        {
            default:
            {
                throw new AssertionError(); // Should not happen
            }
            /////////////////////////////////////////////////////
            case NORTH_POLE: // Fall through
            case SOUTH_POLE:
            {
                if (!isSpherical)
                {
                    if (Math.abs(lat_ts-(Math.PI/2)) >= EPS)
                    {
                        final double t = Math.sin(lat_ts);
                        k0 = (Math.cos(lat_ts) / (Math.sqrt(1-es * t*t))) / tsfn(lat_ts, t);
                    }
                    else
                    {
                        // True scale at pole
                        k0 = 2.0 / Math.sqrt(Math.pow(1+e, 1+e)*Math.pow(1-e, 1-e));
                    }
                }
                else
                {
                    if (Math.abs(lat_ts - (Math.PI/2)) >= EPS) 
                        k0 = 1 + Math.sin(lat_ts);
                }
                // fall through. "chi1", "sinChi1" and "cosChi1"
                // are not used for polar projection. We don't
                // care about their value.
            }
            /////////////////////////////////////////////////////
            case EQUATORIAL:
            {
                chi1    = 0.0;
                cosChi1 = 1.0;
                sinChi1 = 0.0;
                break;
            }
            /////////////////////////////////////////////////////
            case OBLIQUE:
            {
                final double t = Math.sin(centralLatitude);
                chi1    = 2.0 * Math.atan(ssfn(centralLatitude, t)) - (Math.PI/2);
                cosChi1 = Math.cos(chi1);
                sinChi1 = Math.sin(chi1);
                if (!isSpherical)
                {
                    k0 = 2.0*Math.cos(centralLatitude)/Math.sqrt(1-es * t*t);
                }
                break;
            }
        }
        this.k0 = k0*a;
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(Clé.STEREOGRAPHIC);}

    /**
     * Retourne la latitude à laquelle l'échelle est exacte.
     */
    public Latitude getLatitudeTrueScale()
    {return new Latitude(Math.toDegrees(lat_ts));}

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
        if (isSpherical)
        {
            //////////////////////////
            // Spherical projection //
            //////////////////////////
            switch (mode)
            {
                case NORTH_POLE:
                {
                    if (!(Math.abs(1+sinlat) >= TOL))
                    {
                        throw new TransformException(Resources.format(Clé.INFINITY_IN_PROJECTION));
                    }
                    // (21-8)
                    final double f= k0 * coslat / (1+sinlat);// == tan (pi/4 - phi/2)
                    x =  f * sinlon; // (21-5)
                    y = -f * coslon; // (21-6)
                    break;
                }
                case SOUTH_POLE:
                {
                    if (!(Math.abs(1-sinlat) >= TOL))
                    {
                        throw new TransformException(Resources.format(Clé.INFINITY_IN_PROJECTION));
                    }
                    // (21-12)
                    final double f= k0 * coslat / (1-sinlat);// == tan (pi/4 + phi/2)
                    x = f * sinlon; // (21-9)
                    y = f * coslon; // (21-10)
                    break;
                }
                case EQUATORIAL:
                {
                    double f = 1 + coslat*coslon;
                    if (!(f >= TOL))
                    {
                        throw new TransformException(Resources.format(Clé.INFINITY_IN_PROJECTION));
                    }
                    f = k0/f;
                    x = f * coslat * sinlon;
                    y = f * sinlat;
                    break;
                }
                case OBLIQUE:
                {
                    double f = 1.0 + sinphi0*sinlat + cosphi0*coslat*coslon; // (21-4)
                    if (!(f >= TOL))
                    {
                        throw new TransformException(Resources.format(Clé.INFINITY_IN_PROJECTION));
                    }
                    f = k0/f;
                    x = f * coslat * sinlon;                               // (21-2)
                    y = f * (cosphi0 * sinlat - sinphi0 * coslat * coslon);// (21-3)
                    break;
                }
                default: throw new AssertionError(); // Should not happen
            }
        }
        else
        {
            ////////////////////////////
            // Ellipsoidal projection //
            ////////////////////////////
            switch (mode)
            {
                case OBLIQUE:
                {
                    final double chi = 2.0 * Math.atan(ssfn(y, sinlat)) - (Math.PI/2);
                    final double sinChi = Math.sin(chi);
                    final double cosChi = Math.cos(chi);
                    final double cosChi_coslon = cosChi*coslon;
                    final double A = k0 / cosChi1 / (1 + sinChi1*sinChi + cosChi1*cosChi_coslon);
                    x = A * cosChi*sinlon;
                    y = A * (cosChi1*sinChi - sinChi1*cosChi_coslon);
                    break;
                }
                case EQUATORIAL:
                {
                    final double chi = 2.0 * Math.atan(ssfn(y, sinlat)) - (Math.PI/2);
                    final double sinChi = Math.sin(chi);
                    final double cosChi = Math.cos(chi);
                    final double A = k0 / (1.0 + cosChi*coslon);
                    x = A * cosChi*sinlon;
                    y = A * sinChi;
                    break;
                }
                case SOUTH_POLE:
                {
                    final double rho = k0 * tsfn(-y, -sinlat);
                    x = rho * sinlon;
                    y = rho * coslon;
                    break;
                }
                case NORTH_POLE:
                {
                    final double rho = k0 * tsfn(y, sinlat);
                    x = rho * sinlon;
                    y = -rho * coslon;
                    break;
                }
                default: throw new AssertionError(); // Should not happen
            }
        }

        //////////////////////////
        //    Store result      //
        //////////////////////////
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
        final double k0 = this.k0/a;
        final double rho = Math.sqrt(x*x + y*y);
        if (isSpherical)
        {
            //////////////////////////
            // Spherical projection //
            //////////////////////////
            final double c = 2.0 * Math.atan(rho/k0);
            final double cosc = Math.cos(c);
            final double sinc = Math.sin(c);
            switch (mode)
            {
                case NORTH_POLE:
                {
                    y = -y;
                    // fallthrough
                }
                case SOUTH_POLE:
                {
                    // (20-17) call atan2(x,y) to properly deal with y==0
                    x = (Math.abs(x)<TOL && Math.abs(y)<TOL) ? centralLongitude : Math.atan2(x, y) + centralLongitude;
                    if (Math.abs(rho)<TOL)
                        y = centralLatitude;
                    else
                        y = (mode==NORTH_POLE) ? Math.asin(cosc) : Math.asin(-cosc); // (20-14) with phi1=90
                    break;
                }
                case EQUATORIAL:
                {
                    if (Math.abs(rho)<TOL)
                    {
                        y = 0.0;
                        x = centralLongitude;
                    }
                    else
                    {
                        y = Math.asin(y * sinc/rho); // (20-14)  with phi1=0
                        final double t  = x*sinc;
                        final double ct = rho*cosc;
                        x = (Math.abs(t)<TOL && Math.abs(ct)<TOL) ? centralLongitude : Math.atan2(t, ct)+centralLongitude;
                    }
                    break;
                }
                case OBLIQUE:
                {
                    if (Math.abs(rho) < TOL)
                    {
                        y = centralLatitude;
                        x = centralLongitude;
                    }
                    else
                    {
                        final double ct = rho*cosphi0*cosc - y*sinphi0*sinc; // (20-15)
                        final double t  = x*sinc;
                        y = Math.asin(cosc*sinphi0 + y*sinc*cosphi0/rho);
                        x = (Math.abs(ct)<TOL && Math.abs(t)<TOL) ? centralLongitude : Math.atan2(t, ct)+centralLongitude;
                    }
                    break;
                }
                default: throw new AssertionError(); // Should not happen
            }
        }
        else
        {
            ////////////////////////////
            // Ellipsoidal projection //
            ////////////////////////////
    choice: switch (mode)
            {
                case OBLIQUE:
                {
                    // fallthrough
                }
                case EQUATORIAL:
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
                    throw new TransformException(Resources.format(Clé.NO_CONVERGENCE_FOR_LATITUDE¤1, new Latitude(ptSrc.getY())));
                }
                case SOUTH_POLE:
                {
                    y = -y;
                    // fallthrough
                }
                case NORTH_POLE:
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
                            y = (mode==NORTH_POLE) ? phi : -phi;
                            break choice;
                        }
                        phi0=phi;
                    }
                    throw new TransformException(Resources.format(Clé.NO_CONVERGENCE_FOR_LATITUDE¤1, new Latitude(ptSrc.getY())));
                }
                default: throw new AssertionError(); // Should not happen
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
        final long code = Double.doubleToLongBits(lat_ts);
        return (int) code ^ (int) (code >>> 32) ^ super.hashCode();
    }

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    public boolean equals(final Object object)
    {return (object instanceof StereographicProjection) && equals((StereographicProjection) object);}

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    final boolean equals(final StereographicProjection that)
    {
        return super.equals(that) &&
               Double.doubleToLongBits(this.lat_ts) == Double.doubleToLongBits(that.lat_ts);
    }

    /**
     * Implémentation de la partie entre crochets
     * de la chaîne retournée par {@link #toString()}.
     */
    void toString(final StringBuffer buffer)
    {
        super.toString(buffer);
        buffer.append(", Truescale=");
        buffer.append(getLatitudeTrueScale());
        buffer.append(']');
    }
}
