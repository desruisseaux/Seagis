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

// Coordinates
import net.seagis.pt.Latitude;
import net.seagis.pt.Longitude;
import net.seagis.cs.Projection;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;

// Miscellaneous
import java.util.Locale;
import javax.media.jai.ParameterList;

// Resources
import net.seagis.resources.Geometry;
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Provides transformation services between ellipsoidal and cartographic
 * projections. Ellipsoidal height values remain unchanged.
 *
 * @see AffineTransform
 * @see PerspectiveTransform
 *
 * @version 1.0
 * @author André Gosselin
 * @author Martin Desruisseaux
 */
abstract class MapProjection extends MathTransform2D.Abstract
{
    /**
     * Erreur maximale (en mètres) tolérées lorsque l'on fait une
     * transformation directe suivit d'une transformation inverse
     * (ou vis-versa). Si les "assertions" sont activées et qu'une
     * erreur supérieure est détectée, une exception
     * {@link AssertionError} sera lancée.
     */
    private static final double MAX_ERROR = 1;

    /**
     * Marge de tolérance pour les comparaisons de nombre réels.
     */
    static final double EPS=1.0E-6;

    /**
     * Marge de tolérance pour les calculs itératifs.
     */
    static final double TOL=1E-10;

    /**
     * Indique si le modèle terrestre est sphérique. La valeur <code>true</code>
     * indique que le modèle est sphérique, c'est-à-dire que les champs {@link #a}
     * et {@link #b} ont la même valeur.
     */
    protected final boolean isSpherical;

    /**
     * Excentricité de l'ellipse. L'excentricité est 0
     * si l'ellipsoïde est sphérique, c'est-à-dire si
     * {@link #isSpherical} est <code>true</code>.
     */
    protected final double e;

    /**
     * Carré de l'excentricité de l'ellipse: e² = (a²-b²)/a².
     */
    protected final double es;

    /**
     * Longueur de l'axe majeur de la terre, en mètres.
     * Sa valeur par défaut dépend de l'éllipsoïde par
     * défaut (par exemple "WGS 1984").
     */
    protected final double a;

    /**
     * Longueur de l'axe mineur de la terre, en mètres.
     * Sa valeur par défaut dépend de l'éllipsoïde par
     * défaut (par exemple "WGS 1984").
     */
    protected final double b;

    /**
     * Central longitude in <u>radians</u>.  Default value is 0, the Greenwich
     * meridian. <strong>Consider this field as final</strong>. It is not final
     * only  because {@link TransverseMercatorProjection} need to modify it at
     * construction time.
     */
    protected double centralLongitude;

    /**
     * Central latitude in <u>radians</u>. Default value is 0, the equator.
     * <strong>Consider this field as final</strong>. It is not final only
     * because some class need to modify it at construction time.
     */
    protected double centralLatitude;

    /**
     * The inverse of this map projection.
     * Will be created only when needed.
     */
    private transient MathTransform inverse;

    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     *         The following parameter are recognized:
     *         <ul>
     *           <li>"semi_major"   (default to WGS 1984)</li>
     *           <li>"semi_minor"   (default to WGS 1984)</li>
     *           <li>"central_meridian"   (default to 0°)</li>
     *           <li>"latitude_of_origin" (default to 0°)</li>
     *         </ul>
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected MapProjection(final Projection parameters) throws MissingParameterException
    {
        this.a                =                    parameters.getValue("semi_major");
        this.b                =                    parameters.getValue("semi_minor");
        this.centralLongitude = longitudeToRadians(parameters.getValue("central_meridian",   0), true);
        this.centralLatitude  =  latitudeToRadians(parameters.getValue("latitude_of_origin", 0), true);
        this.isSpherical      = (a==b);
        this.es = 1.0 - (b*b)/(a*a);
        this.e  = Math.sqrt(es);

        final double dx = parameters.getValue("false_easting");
        final double dy = parameters.getValue("false_northing");
        if (!(dx==0 && dy==0))
        {
            throw new UnsupportedOperationException("False easting/northing not yet implemented");
        }
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public abstract String getName(final Locale locale);

    /**
     * Convertit en radians une longitude exprimée en degrés. Au passage, cette méthode vérifiera
     * si la longitude est bien dans les limites permises (±180°). Cette méthode est utile pour
     * vérifier la validité des paramètres de la projection, comme {@link #setCentralLongitude}.
     *
     * @param  x Longitude à vérifier, en degrés.
     * @param  edge <code>true</code> pour accepter les longitudes de ±180°.
     * @return Longitude en radians.
     * @throws IllegalArgumentException si la longitude est invalide.
     */
    static double longitudeToRadians(final double x, boolean edge) throws IllegalArgumentException
    {
        if (edge ? (x>=Longitude.MIN_VALUE && x<=Longitude.MAX_VALUE) : (x>Longitude.MIN_VALUE && x<Longitude.MAX_VALUE))
        {
            return Math.toRadians(x);
        }
        throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_LONGITUDE_OUT_OF_RANGE_$1, new Longitude(x)));
    }

    /**
     * Convertit en radians une latitude exprimée en degrés. Au passage, cette méthode vérifiera
     * si la latitude est bien dans les limites permises (±90°). Cette méthode est utile pour
     * vérifier la validité des paramètres de la projection, comme {@link #setCentralLongitude}.
     *
     * @param  y Latitude à vérifier, en degrés.
     * @param  edge <code>true</code> pour accepter les latitudes de ±90°.
     * @return Latitude en radians.
     * @throws IllegalArgumentException si la latitude est invalide.
     */
    static double latitudeToRadians(final double y, boolean edge) throws IllegalArgumentException
    {
        if (edge ? (y>=Latitude.MIN_VALUE && y<=Latitude.MAX_VALUE) : (y>Latitude.MIN_VALUE && y<Latitude.MAX_VALUE))
        {
            return Math.toRadians(y);
        }
        throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_LATITUDE_OUT_OF_RANGE_$1, new Latitude(y)));
    }

    /**
     * Check point for private use by {@link #checkTransform} and {@link #checkInverseTransform}.
     * This class is necessary in order to avoid never-ending loop in <code>assert</code>
     * statements (when an <code>assert</code> calls <code>transform</code>, which calls
     * <code>inverseTransform</code>, which calls <code>transform</code>, etc.).
     */
    private static final class CheckPoint extends Point2D.Double
    {
        public CheckPoint(final Point2D point)
        {super(point.getX(), point.getY());}
    }

    /**
     * Check if the transform of <code>point</code> is close enough to <code>target</code>.
     * "Close enough" means that the two points are separated by a distance shorter than
     * {@link #MAX_ERROR}. This method is used for assertions with JDK 1.4.
     *
     * @param  point  Point to transform, in degrees if <code>inverse</code> is false.
     * @param  target Point to compare to, in metres if <code>inverse</code> is false.
     * @param inverse <code>true</code> for an inverse transform instead of a direct one.
     * @return <code>true</code> if the two points are close enough.
     * @throws TransformException if a transformation failed.
     */
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
    private boolean checkTransform(Point2D point, final Point2D target, final boolean inverse)
    {
        if (!(point instanceof CheckPoint)) try
        {
            point = new CheckPoint(point);
            final double distance;
            if (inverse)
            {
                point = inverseTransform(point, point);
                final double y1 = Math.toRadians(point .getY());
                final double y2 = Math.toRadians(target.getY());
                final double dx = Math.toRadians(Math.abs(target.getX()-point.getX()) % 360);
                double rho = Math.sin(y1)*Math.sin(y2) + Math.cos(y1)*Math.cos(y2)*Math.cos(dx);
                if (rho>+1) {assert rho<=+(1+EPS) : rho; rho=+1;}
                if (rho<-1) {assert rho>=-(1+EPS) : rho; rho=-1;}
                distance = Math.acos(rho)*a;
                // Computed orthodromic distance (spherical model) in metres.
            }
            else
            {
                point = transform(point, point);
                distance = point.distance(target);
            }
            if (!(distance <= MAX_ERROR)) // Do not accept NaN as valid value.
            {
                throw new AssertionError(distance);
            }
        }
        catch (TransformException exception)
        {
            final AssertionError error = new AssertionError(exception.getLocalizedMessage());
            error.initCause(exception);
            throw error;
        }
        return true;
    }
------- END OF JDK 1.4 DEPENDENCIES ---*/




    //////////////////////////////////////////////////////////////////////
    ////                                                              ////
    ////                          TRANSFORMS                          ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

    /**
     * Transforms the specified coordinate and stores the result in <code>ptDst</code>.
     * This method is guaranteed to be invoked with values of <var>x</var> in the range
     * <code>[-PI..PI]</code> and values of <var>y</var> in the range <code>[-PI/2..PI/2]</code>.
     *
     * @param x     The longitude of the coordinate, in <strong>radians</strong>.
     * @param x     The  latitude of the coordinate, in <strong>radians</strong>.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>. Ordinates will be in metres.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    protected abstract Point2D transform(double x, double y, final Point2D ptDst) throws TransformException;

    /**
     * Transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     *              Ordinates must be in degrees.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>. Ordinates will be in metres.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    public final Point2D transform(final Point2D ptSrc, Point2D ptDst) throws TransformException
    {
        final double x=ptSrc.getX();
        final double y=ptSrc.getY();
        if (!(x>=Longitude.MIN_VALUE && x<=Longitude.MAX_VALUE))
        {
            throw new TransformException(Resources.format(ResourceKeys.ERROR_LONGITUDE_OUT_OF_RANGE_$1, new Longitude(x)));
        }
        if (!(y>=Latitude.MIN_VALUE && y<=Latitude.MAX_VALUE))
        {
            throw new TransformException(Resources.format(ResourceKeys.ERROR_LATITUDE_OUT_OF_RANGE_$1, new Latitude(y)));
        }
        ptDst = transform(Math.toRadians(x), Math.toRadians(y), ptDst);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert checkTransform(ptDst, (ptSrc!=ptDst) ? ptSrc : new Point2D.Double(x,y), true);
------- END OF JDK 1.4 DEPENDENCIES ---*/
        return ptDst;
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     * Ordinates must be (<var>longitude</var>,<var>latitude</var>)
     * pairs in degrees.
     *
     * @throws TransformException if a point can't be transformed. This method try
     *         to transform every points even if some of them can't be transformed.
     *         Non-transformable points will have value {@link Double#NaN}. If more
     *         than one point can't be transformed, then this exception may be about
     *         an arbitrary point.
     */
    public final void transform(final double[] src, int srcOffset, final double[] dest, int dstOffset, int numPts) throws TransformException
    {
        /*
         * Vérifie s'il faudra parcourir le tableau en sens inverse.
         * Ce sera le cas si les tableaux source et destination se
         * chevauchent et que la destination est après la source.
         */
        final boolean reverse = (src==dest && srcOffset<dstOffset && srcOffset+(2*numPts)>dstOffset);
        if (reverse)
        {
            srcOffset += 2*numPts;
            dstOffset += 2*numPts;
        }
        final Point2D.Double point=new Point2D.Double();
        TransformException firstException=null;
        while (--numPts>=0)
        {
            try
            {
                point.x = src[srcOffset++];
                point.y = src[srcOffset++];
                transform(point, point);
                dest[dstOffset++] = point.x;
                dest[dstOffset++] = point.y;
            }
            catch (TransformException exception)
            {
                dest[dstOffset++] = Double.NaN;
                dest[dstOffset++] = Double.NaN;
                if (firstException==null)
                {
                    firstException=exception;
                }
            }
            if (reverse)
            {
                srcOffset -= 4;
                dstOffset -= 4;
            }
        }
        if (firstException!=null) throw firstException;
    }

    /**
     * Transforms a list of coordinate point ordinal values.
     * Ordinates must be (<var>longitude</var>,<var>latitude</var>)
     * pairs in degrees.
     *
     * @throws TransformException if a point can't be transformed. This method try
     *         to transform every points even if some of them can't be transformed.
     *         Non-transformable points will have value {@link Float#NaN}. If more
     *         than one point can't be transformed, then this exception may be about
     *         an arbitrary point.
     */
    public final void transform(final float[] src, int srcOffset, final float[] dest, int dstOffset, int numPts) throws TransformException
    {
        final boolean reverse = (src==dest && srcOffset<dstOffset && srcOffset+(numPts << 1)>dstOffset);
        if (reverse)
        {
            srcOffset += 2*numPts;
            dstOffset += 2*numPts;
        }
        final Point2D.Double point=new Point2D.Double();
        TransformException firstException=null;
        while (--numPts>=0)
        {
            try
            {
                point.x = src[srcOffset++];
                point.y = src[srcOffset++];
                transform(point, point);
                dest[dstOffset++] = (float) point.x;
                dest[dstOffset++] = (float) point.y;
            }
            catch (TransformException exception)
            {
                dest[dstOffset++] = Float.NaN;
                dest[dstOffset++] = Float.NaN;
                if (firstException==null)
                {
                    firstException=exception;
                }
            }
            if (reverse)
            {
                srcOffset -= 4;
                dstOffset -= 4;
            }
        }
        if (firstException!=null) throw firstException;
    }

    /**
     * Transforme la forme géométrique <code>shape</code> spécifiée.
     * Cette projection peut remplacer certaines lignes droites
     * par des courbes. Tous les points de la forme géométrique
     * seront copiés. Cette méthode n'est donc pas à conseiller
     * si <code>shape</code> est volumineux, par exemple s'il
     * représente une bathymétrie entière.
     *
     * @param shape Forme géométrique à transformer. Les coordonnées des points
     *              de cette forme doivent être exprimées en degrés de latitudes
     *              et de longitudes.
     * @return      Forme géométrique transformée. Les coordonnées des points de
     *              cette forme seront exprimées en mètres.
     * @throws TransformException si une transformation a échouée.
     */
    public final Shape createTransformedShape(final Shape shape) throws TransformException
    {return createTransformedShape(shape, null, this, null, Geometry.HORIZONTAL);}




    //////////////////////////////////////////////////////////////////////
    ////                                                              ////
    ////                      INVERSE TRANSFORMS                      ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

    /**
     * Transforms the specified coordinate and stores the result in <code>ptDst</code>.
     * This method shall returns <var>x</var> values in the range <code>[-PI..PI]</code>
     * and <var>y</var> values in the range <code>[-PI/2..PI/2]</code>. It will be checked
     * by the caller, so this method doesn't need to performs this check.
     *
     * @param x     The longitude of the coordinate, in metres.
     * @param x     The  latitude of the coordinate, in metres.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>. Ordinates will be in <strong>radians</strong>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    protected abstract Point2D inverseTransform(double x, double y, final Point2D ptDst) throws TransformException;

    /**
     * Inverse transforms the specified <code>ptSrc</code>
     * and stores the result in <code>ptDst</code>.
     *
     * @param ptSrc the specified coordinate point to be transformed.
     *              Ordinates must be in metres.
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>. Ordinates will be in degrees.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    public final Point2D inverseTransform(final Point2D ptSrc, Point2D ptDst) throws TransformException
    {
        final double x0 = ptSrc.getX();
        final double y0 = ptDst.getY();
        ptDst = inverseTransform(x0, y0, ptDst);
        final double x = Math.toDegrees(ptDst.getX());
        final double y = Math.toDegrees(ptDst.getY());
        ptDst.setLocation(x,y);
        if (!(x>=Longitude.MIN_VALUE && x<=Longitude.MAX_VALUE))
        {
            throw new TransformException(Resources.format(ResourceKeys.ERROR_LONGITUDE_OUT_OF_RANGE_$1, new Longitude(x)));
        }
        if (!(y>=Latitude.MIN_VALUE && y<=Latitude.MAX_VALUE))
        {
            throw new TransformException(Resources.format(ResourceKeys.ERROR_LATITUDE_OUT_OF_RANGE_$1, new Latitude(y)));
        }
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert checkTransform(ptDst, (ptSrc!=ptDst) ? ptSrc : new Point2D.Double(x0, y0), false);
------- END OF JDK 1.4 DEPENDENCIES ---*/
        return ptDst;
    }

    /**
     * Inverse transforms a list of coordinate point ordinal values.
     * Ordinates must be (<var>x</var>,<var>y</var>) pairs in metres.
     *
     * @throws TransformException if a point can't be transformed. This method try
     *         to transform every points even if some of them can't be transformed.
     *         Non-transformable points will have value {@link Double#NaN}. If more
     *         than one point can't be transformed, then this exception may be about
     *         an arbitrary point.
     */
    public final void inverseTransform(final double[] src, int srcOffset, final double[] dest, int dstOffset, int numPts) throws TransformException
    {
        /*
         * Vérifie s'il faudra parcourir le tableau en sens inverse.
         * Ce sera le cas si les tableaux source et destination se
         * chevauchent et que la destination est après la source.
         */
        final boolean reverse = (src==dest && srcOffset<dstOffset && srcOffset+(numPts << 1)>dstOffset);
        if (reverse)
        {
            srcOffset += 2*numPts;
            dstOffset += 2*numPts;
        }
        final Point2D.Double point=new Point2D.Double();
        TransformException firstException=null;
        while (--numPts>=0)
        {
            try
            {
                point.x = src[srcOffset++];
                point.y = src[srcOffset++];
                inverseTransform(point, point);
                dest[dstOffset++] = point.x;
                dest[dstOffset++] = point.y;
            }
            catch (TransformException exception)
            {
                dest[dstOffset++] = Double.NaN;
                dest[dstOffset++] = Double.NaN;
                if (firstException==null)
                {
                    firstException=exception;
                }
            }
            if (reverse)
            {
                srcOffset -= 4;
                dstOffset -= 4;
            }
        }
        if (firstException!=null) throw firstException;
    }

    /**
     * Inverse transforms a list of coordinate point ordinal values.
     * Ordinates must be (<var>x</var>,<var>y</var>) pairs in metres.
     *
     * @throws TransformException if a point can't be transformed. This method try
     *         to transform every points even if some of them can't be transformed.
     *         Non-transformable points will have value {@link Float#NaN}. If more
     *         than one point can't be transformed, then this exception may be about
     *         an arbitrary point.
     */
    public final void inverseTransform(final float[] src, int srcOffset, final float[] dest, int dstOffset, int numPts) throws TransformException
    {
        final boolean reverse = (src==dest && srcOffset<dstOffset && srcOffset+(numPts << 1)>dstOffset);
        if (reverse)
        {
            srcOffset += 2*numPts;
            dstOffset += 2*numPts;
        }
        final Point2D.Double point=new Point2D.Double();
        TransformException firstException=null;
        while (--numPts>=0)
        {
            try
            {
                point.x = src[srcOffset++];
                point.y = src[srcOffset++];
                inverseTransform(point, point);
                dest[dstOffset++] = (float) point.x;
                dest[dstOffset++] = (float) point.y;
            }
            catch (TransformException exception)
            {
                dest[dstOffset++] = Float.NaN;
                dest[dstOffset++] = Float.NaN;
                if (firstException==null)
                {
                    firstException=exception;
                }
            }
            if (reverse)
            {
                srcOffset -= 4;
                dstOffset -= 4;
            }
        }
        if (firstException!=null) throw firstException;
    }



    //////////////////////////////////////////////////////////////////////
    ////                                                              ////
    ////             INTERNAL COMPUTATIONS FOR SUBCLASSES             ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

    /**
     * Iteratively solve equation (7-9) from Snyder.
     */
    final double cphi2(final double ts) throws TransformException
    {
        final double eccnth = 0.5*e;
        double phi = (Math.PI/2) - 2.0*Math.atan(ts);
        for (int i=0; i<16; i++)
        {
            final double con  = e*Math.sin(phi);
            final double dphi = (Math.PI/2) - 2.0*Math.atan(ts * Math.pow((1-con)/(1+con), eccnth)) - phi;
            phi += dphi;
            if (Math.abs(dphi) <= TOL) return phi;
        }
        throw new TransformException(Resources.format(ResourceKeys.ERROR_NO_CONVERGENCE));
    }

    /**
     * Compute function <code>f(s,c,es) = c/sqrt(1 - s²*es)</code>
     * needed for the true scale latitude (Snyder, p. 47), where
     * <var>s</var> and <var>c</var> are the sine and cosine of
     * the true scale latitude, and {@link #es} the eccentricity
     * squared.
     */
    final double msfn(final double s, final double c)
    {return c / Math.sqrt(1.0 - s*s*es);}

    /**
     * Compute function (15-9) from Snyder
     * equivalent to negative of function (7-7).
     */
    final double tsfn(final double phi, double sinphi)
    {
        sinphi *= e;
        /*
         * NOTE: change sign to get the equivalent of Snyder (7-7).
         */
        return Math.tan(0.5 * ((Math.PI/2) - phi)) /
               Math.pow((1-sinphi)/(1+sinphi), 0.5*e);
    }




    //////////////////////////////////////////////////////////////////////
    ////                                                              ////
    ////                        MISCELLANEOUS                         ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

    /**
     * Returns the inverse of this map projection.
     */
    public final synchronized MathTransform inverse()
    {
        if (inverse==null)
            inverse=new Inverse();
        return inverse;
    }

    /**
     * Returns <code>false</code> since map
     * projections are not identity transforms.
     */
    public final boolean isIdentity()
    {return false;}

    /**
     * Returns a hash value for this map projection.
     */
    public int hashCode()
    {
        long code =      Double.doubleToLongBits(a);
        code = code*37 + Double.doubleToLongBits(b);
        code = code*37 + Double.doubleToLongBits(centralLongitude);
        code = code*37 + Double.doubleToLongBits(centralLatitude);
        return (int) code ^ (int) (code >>> 32);
    }

    /**
     * Compares the specified object with
     * this map projection for equality.
     */
    public boolean equals(final Object object)
    {
        // Do not check 'object==this' here, since this
        // optimization is usually done in subclasses.
        if (super.equals(object))
        {
            final MapProjection that = (MapProjection) object;
            return Double.doubleToLongBits(this.a)                == Double.doubleToLongBits(that.a) &&
                   Double.doubleToLongBits(this.b)                == Double.doubleToLongBits(that.b) &&
                   Double.doubleToLongBits(this.centralLongitude) == Double.doubleToLongBits(that.centralLongitude) &&
                   Double.doubleToLongBits(this.centralLatitude)  == Double.doubleToLongBits(that.centralLatitude);
        }
        return false;
    }

    /**
     * Retourne une chaîne de caractères représentant cette projection cartographique.
     * Cette chaîne de caractères contiendra entre autres le nom de la projection, les
     * coordonnées du centre et celles de l'origine.
     */
    public final String toString()
    {
        StringBuffer buffer=new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        toString(buffer);
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Implémentation de la partie entre crochets
     * de la chaîne retournée par {@link #toString()}.
     */
    void toString(final StringBuffer buffer)
    {
        try
        {
            Point2D origin = new Point2D.Double(); // Initialized at (0,0).
            origin = inverseTransform(origin, origin);
            buffer.append("origin=(");
            buffer.append(new Latitude(origin.getY()));
            buffer.append(", ");
            buffer.append(new Longitude(origin.getX()));
            buffer.append(')');
        }
        catch (TransformException exception)
        {
            // Ignore.
        }
    }

    /**
     * Inverse of a map projection.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Inverse extends MathTransform2D.Abstract
    {
        public final MathTransform inverse()
        {return MapProjection.this;}

        public final boolean isIdentity()
        {return MapProjection.this.isIdentity();}

        public Point2D transform(final Point2D source, final Point2D dest) throws TransformException
        {return MapProjection.this.inverseTransform(source, dest);}

        public void transform(final double[] source, final int srcOffset, final double[] dest, final int dstOffset, final int length) throws TransformException
        {MapProjection.this.inverseTransform(source, srcOffset, dest, dstOffset, length);}

        public void transform(final float[] source, final int srcOffset, final float[] dest, final int dstOffset, final int length) throws TransformException
        {MapProjection.this.inverseTransform(source, srcOffset, dest, dstOffset, length);}

        public Shape createTransformedShape(final Shape shape) throws TransformException
        {return super.createTransformedShape(shape, null, this, null, Geometry.HORIZONTAL);}

        public final int hashCode()
        {return ~MapProjection.this.hashCode();}

        public final boolean equals(final Object object)
        {
            if (object==this) return true; // Slight optimization
            if (object instanceof Inverse)
            {
                final Inverse that = (Inverse) object;
                return Utilities.equals(this.inverse(), that.inverse());
            }
            else return false;
        }
    }

    /**
     * Informations about a {@link MapProjection}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    static abstract class Provider extends MathTransformProvider
    {
        /**
         * Construct a new provider.
         *
         * @param classification The classification name.
         * @param nameKey Resources key for a human readable name.
         *        This is used for {@link #getName} implementation.
         */
        protected Provider(final String classname, final int nameKey)
        {super(classname, nameKey, DEFAULT_PROJECTION_DESCRIPTOR);}

        /**
         * Create a new map projection for a parameter list.
         */
        public final MathTransform create(final ParameterList parameters)
        {return (MathTransform)create(new Projection("Generated", getClassName(), parameters));}

        /**
         * Create a new map projection.  NOTE: The returns type should
         * be {@link MathTransform}, but as of JDK 1.4-beta3, it force
         * class loading for all projection classes (MercatorProjection,
         * etc.) before than necessary. Changing the returns type to
         * Object is a trick to avoid too early class loading...
         */
        protected abstract Object create(final Projection parameters);
    }
}
