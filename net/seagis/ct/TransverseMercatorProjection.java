/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
 *              1999, Fisheries and Oceans Canada
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
 *    This package contains formulas from the PROJ package of USGS.
 *    USGS's work is fully acknowledged here.
 */
package net.seagis.ct;

// OpenGIS (SEAS) dependencies
import net.seagis.cs.Projection;
import net.seagis.cs.Ellipsoid;
import net.seagis.pt.Latitude;

// Miscellaneous
import java.util.Locale;
import java.awt.geom.Point2D;

// Resources
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Projections de Mercator tranverses Universelle et Modifiée. Il s'agit de la projection
 * Mercator cylindrique, mais dans lequel le cylindre a subit une rotation de 90°. Au lieu
 * d'être tangeant à l'équateur (ou à une autre latitude standard), le cylindre de la
 * projection tranverse est tangeant à un méridien central. Les déformation deviennent
 * de plus en plus importantes à mesure que l'on s'éloigne du méridien central. Cette
 * projection est appropriée pour les régions qui s'étendent d'avantage dans le sens
 * nord-sud que dans le sens est-ouest.
 *
 * Référence: John P. Snyder (Map Projections - A Working Manual,
 *            U.S. Geological Survey Professional Paper 1395, 1987)
 *
 * @version 1.0
 * @author André Gosselin
 * @author Martin Desruisseaux
 */
final class TransverseMercatorProjection extends CylindricalProjection
{
    /**
     * Functions needed for the UTM (Universal Tranverse Mercator)
     * and MTM (Modified transverse Mercator) forward/inverse
     * projections.
     *
     * NOTE: formulae used below are not those of Snyder, but rather those
     *       from the 'proj' package of the USGS survey, which have been
     *       reproduced verbatim. USGS work is acknowledged here.
     */
    private static final double FC1= 1.00000000000000000000000,  // 1/1
                                FC2= 0.50000000000000000000000,  // 1/2
                                FC3= 0.16666666666666666666666,  // 1/6
                                FC4= 0.08333333333333333333333,  // 1/12
                                FC5= 0.05000000000000000000000,  // 1/20
                                FC6= 0.03333333333333333333333,  // 1/30
                                FC7= 0.02380952380952380952380,  // 1/42
                                FC8= 0.01785714285714285714285;  // 1/56

    /**
     * Functions to compute meridinal distance and
     * inverse on the ellipsoid.
     *
     *    NOTE: formulae differ from those of Snyder.
     *          Algorithms have been taken verbatim from the 'proj' package of
     *          the USGS, whose work is fully acknowledged.
     *
     * meridinal distance for ellipsoid and inverse
     * 8th degree - accurate to < 1e-5 meters when used in conjuction
     * with typical major axis values.
     * Inverse determines phi to EPS11 (1e-11) radians, about 1e-6 seconds.
     */
    private static final double C00= 1.0,
                                C02= 0.25,
                                C04= 0.046875,
                                C06= 0.01953125,
                                C08= 0.01068115234375,
                                C22= 0.75,
                                C44= 0.46875,
                                C46= 0.01302083333333333333,
                                C48= 0.00712076822916666666,
                                C66= 0.36458333333333333333,
                                C68= 0.00569661458333333333,
                                C88= 0.3076171875;

    /**
     * Relative precisions.
     */
    private static final double EPS10=1e-10, EPS11=1e-11;

    /**
     * Constantes nécessaires aux projections.
     */
    private final double en0,en1,en2,en3,en4;

    /**
     * Global scale factor. Value <code>ak0</code>
     * is equals to <code>{@link #a}*k0</code>.
     */
    private final double ak0;

    /**
     * Variante de l'eccentricité, calculée par
     * <code>e'² = (a²-b²)/b² = es/(1-es)</code>.
     */
    private final double esp;

    /**
     * Décalage qui dépendra de la zone choisit.
     */
    private final double x0;

    /**
     * Numéro de la zone, de 1 à 60 pour les projections
     * UTM ou de 1 à 120 pour les projections MTM.
     */
    private final int zone;

    /*
     * Symbolic name for this projection (UTM or MTM).
     */
    private final String name;

    /**
     * Resource key for localized name.
     */
    private final int clé;

    /**
     * Construct a new map projection from the suplied parameters.
     * Projection will default to Universal Transverse Mercator (UTM).
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected TransverseMercatorProjection(final Projection parameters) throws MissingParameterException
    {this(parameters, false);} // Default to UTM.

    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @param  modified <code>true</code> for MTM, <code>false</code> for UTM.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected TransverseMercatorProjection(final Projection parameters, final boolean modified) throws MissingParameterException
    {
        //////////////////////////
        //   Fetch parameters   //
        //////////////////////////
        super(parameters);
        /*
         * Longitude au centre de la zone 1, en degrés.
         * Les longitudes sont mesurées par rapport au méridien de
         * Greenwich, avec les longitudes positives à l'est et les
         * longitudes négatives à l'ouest.
         */
        final double centralLongitudeZone1;
        /*
         * Nombre de degrés de longitude dans une zone. Une valeur positive
         * signifie que les zones sont mesurées d'ouest en est, tandis qu'une valeur
         * négative signifie au contraire que les zones sont mesurées d'est en ouest.
         */
        final double zoneWidth;
        /*
         * Nombre de zones pour l'ensemble de la planète.
         */
        final int zoneCount;

        //////////////////////////
        //  Compute constants   //
        //////////////////////////
        if (modified)
        {
            clé                   = ResourceKeys.MTM;
            name                  = "MTM";
            centralLongitudeZone1 = -52.5; // 52°30'W
            zoneWidth             = -3;    // 3° from East to West
            zoneCount             = 120;   // ==360/3
            x0                    = 304800.0;
            ak0                   = a*0.9999;
        }
        else
        {
            clé                   = ResourceKeys.UTM;
            name                  = "UTM";
            centralLongitudeZone1 = -177;  // 177°W
            zoneWidth             = +6;    // 6° from West to East
            zoneCount             = 60;    // ==360/6
            x0                    = 500000.0;
            ak0                   = a*0.9996;
        }

        double t;
        esp = (a*a) / (b*b) - 1.0;
        en0 = C00 - es * (C02 + es * (C04 + es * (C06 + es * C08)));
        en1 = es * (C22 - es * (C04 + es * (C06 + es * C08)));
        en2 = (t = es * es) * (C44 - es * (C46 + es * C48));
        en3 = (t *= es) * (C66 - es * C68);
        en4 = t * es * C88;

        //////////////////////////
        //  Compute zone code   //
        //   [1..120] for MTM   //
        //   [1.. 60] for UTM   //
        //////////////////////////
        t  = centralLongitudeZone1 - 0.5*zoneWidth; // Longitude du début de la 1ère zone.
        t  = Math.toDegrees(centralLongitude) - t;  // Nombre de degrés de longitudes entre la longitude centrale et la longitude 1.
        t  = Math.floor(t/zoneWidth + EPS);         // Nombre de zones entre la longitudes centrale et la longitude 1.
        t -= zoneCount*Math.floor(t/zoneCount);     // Si négatif, ramène dans l'intervale 0 à (zoneCount-1).
        zone = ((int) t)+1;

        /////////////////////////////////
        //  Compute central longitude  //
        /////////////////////////////////
        t  = centralLongitudeZone1 + (zone-1)*zoneWidth;
        t -= 360*Math.floor((t+180)/360); // Bring back into [-180..+180] range.
        centralLongitude = Math.toRadians(t);
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(clé);}

    /**
     * Calcule la distance méridionale sur un
     * ellipsoïde à la latitude <code>phi</code>.
     */
    private final double mlfn(final double phi, double sphi, double cphi)
    {
        cphi *= sphi;
        sphi *= sphi;
        return en0 * phi - cphi *
              (en1 + sphi *
              (en2 + sphi *
              (en3 + sphi *
              (en4))));
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinate
     * and stores the result in <code>ptDst</code>.
     */
    protected Point2D transform(double x, double y, final Point2D ptDst) throws TransformException
    {
        if (Math.abs(y) > (Math.PI/2 - EPS))
        {
            throw new TransformException(Resources.format(ResourceKeys.POLE_PROJECTION_$1, new Latitude(Math.toDegrees(y))));
        }
        x -= centralLongitude;
        double sinphi = Math.sin(y);
        double cosphi = Math.cos(y);
        if (isSpherical)
        {
            // Spherical model.
            double b = cosphi * Math.sin(x);
            if (Math.abs(Math.abs(b) - 1.0) <= EPS10)
            {
                throw new TransformException(Resources.format(ResourceKeys.INFINITY_IN_PROJECTION));
            }
            double yy = cosphi * Math.cos(x) / Math.sqrt(1.0-b*b);
            x = 0.5*ak0 * Math.log((1.0+b)/(1.0-b)) + x0; /* 8-1 */
            if ((b=Math.abs(yy)) >= 1.0)
            {
                if ((b-1.0) > EPS10)
                {
                    throw new TransformException(Resources.format(ResourceKeys.INFINITY_IN_PROJECTION));
                }
                else
                    yy = 0.0;
            }
            else
                yy = Math.acos(yy);
            if (y<0)
                yy = -yy;
            y = ak0 * yy;
        }
        else
        {
            // Ellipsoidal model.
            double t = Math.abs(cosphi)>EPS10 ? sinphi/cosphi : 0;
            t *= t;
            double al = cosphi*x;
            double als = al*al;
            al /= Math.sqrt(1.0 - es * sinphi*sinphi);
            double n = esp * cosphi*cosphi;

            /* NOTE: meridinal distance at central latitude is always 0 */
            y = ak0*(mlfn(y, sinphi, cosphi) + sinphi*al*x*
                      FC2 * ( 1.0 +
                      FC4 * als * (5.0 - t + n*(9.0 + 4.0*n) +
                      FC6 * als * (61.0 + t * (t - 58.0) + n*(270.0 - 330.0*t) +
                      FC8 * als * (1385.0 + t * ( t*(543.0 - t) - 3111.0))))));

            x = ak0*al*(FC1 + FC3 * als*(1.0 - t + n +
                      FC5 * als * (5.0 + t*(t - 18.0) + n*(14.0 - 58.0*t) +
                      FC7 * als * (61.0+ t*(t*(179.0 - t) - 479.0 ))))) + x0;
        }
        if (ptDst!=null)
        {
            ptDst.setLocation(x,y);
            return ptDst;
        }
        else return new Point2D.Double(x,y);
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinate
     * and stores the result in <code>ptDst</code>.
     */
    protected Point2D inverseTransform(double x, double y, final Point2D ptDst) throws TransformException
    {
        if (isSpherical)
        {
            // Spherical model.
            double t = Math.exp((x-x0)/ak0);
            double d = 0.5 * (t-1/t);
            t = Math.cos(y/ak0);
            double phi = Math.asin(Math.sqrt((1.0-t*t)/(1.0+d*d)));
            y = y<0.0 ? -phi : phi;
            x = (Math.abs(d)>EPS10 || Math.abs(t)>EPS10) ? Math.atan2(d,t)+centralLongitude : centralLongitude;
        }
        else
        {
            // Ellipsoidal projection.
            final double y_ak0 = y/ak0;
            final double k = 1.0-es;
            double phi = y_ak0;
            for (int i=10; true;) // rarely goes over 5 iterations
            {
                if (--i < 0)
                {
                    throw new TransformException(Resources.format(ResourceKeys.NO_CONVERGENCE));
                }
                final double s = Math.sin(phi);
                double t = 1.0 - es * (s*s);
                t = (mlfn(phi, s, Math.cos(phi)) - y_ak0) / ( k * t * Math.sqrt(t));
                phi -= t;
                if (Math.abs(t)<EPS11) break;
            }
            if (Math.abs(phi) >= (Math.PI/2))
            {
                y = y<0.0 ? -(Math.PI/2) : (Math.PI/2);
                x = centralLongitude;
            }
            else
            {
                double sinphi = Math.sin(phi);
                double cosphi = Math.cos(phi);
                double t = Math.abs(cosphi)>EPS10 ? sinphi/cosphi : 0.0;
                double n = esp * cosphi*cosphi;
                double con = 1.0 - es * sinphi*sinphi;
                double d = (x-x0)*Math.sqrt(con)/ak0;
                con *= t;
                t *= t;
                double ds = d*d;
                y = phi - (con*ds / (1.0 - es)) *
                      FC2 * (1.0 - ds *
                      FC4 * (5.0 + t*(3.0 - 9.0*n) + n*(1.0 - 4*n) - ds *
                      FC6 * (61.0 + t*(90.0 - 252.0*n + 45.0*t) + 46.0*n - ds *
                      FC8 * (1385.0 + t*(3633.0 + t*(4095.0 + 1574.0*t))))));

                x = d*(FC1 - ds * FC3 * (1.0 + 2.0*t + n -
                    ds*FC5*(5.0 + t*(28.0 + 24* t + 8.0*n) + 6.0*n -
                    ds*FC7*(61.0 + t*(662.0 + t*(1320.0 + 720.0*t))))))/cosphi + centralLongitude;
            }
        }
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
        final long code = Double.doubleToLongBits(x0);
        return ((int)code ^ (int)(code >>> 32)) + 37*super.hashCode();
    }

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (super.equals(object))
        {
            final TransverseMercatorProjection that = (TransverseMercatorProjection) object;
            return Double.doubleToLongBits(this. x0) == Double.doubleToLongBits(that. x0) &&
                   Double.doubleToLongBits(this.ak0) == Double.doubleToLongBits(that.ak0);
        }
        return false;
    }

    /**
     * Implémentation de la partie entre crochets
     * de la chaîne retournée par {@link #toString()}.
     */
    void toString(final StringBuffer buffer)
    {
        super.toString(buffer);
        buffer.append(", ");
        buffer.append(name);
        buffer.append(" zone=");
        buffer.append(zone);
    }

    /**
     * Informations about a {@link TransverseMercatorProjection}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static final class Provider extends MapProjection.Provider
    {
        /**
         * <code>true</code> for Modified Mercator Projection (MTM), or
         * <code>false</code> for Universal Mercator Projection (UTM).
         */
        private final boolean modified;

        /**
         * Construct a new registration.
         *
         * @param modified <code>true</code> for Modified Mercator Projection (MTM),
         *       or <code>false</code> for Universal Mercator Projection (UTM).
         */
        public Provider(final boolean modified)
        {
            super(modified ? "Modified_Transverse_Mercator" : "Transverse_Mercator",
                  modified ? ResourceKeys.MTM               : ResourceKeys.UTM);
            this.modified = modified;
        }

        /**
         * Create a new map projection.
         */
        protected Object create(final Projection parameters)
        {return new TransverseMercatorProjection(parameters, modified);}
    }
}
