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

// Coordinates
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.ProjectedCoordinateSystem;
import net.seas.opengis.cs.GeographicCoordinateSystem;
import net.seas.opengis.cs.Projection;
import net.seas.opengis.cs.Ellipsoid;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import net.seas.awt.geom.Geometry;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XMath;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import net.seas.text.CoordinateFormat;
import javax.media.jai.ParameterList;


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
abstract class MapProjection extends CoordinateTransform
{
    /**
     * Erreur maximale (en mètres) tolérées lorsque l'on fait une
     * transformation directe suivit d'une transformation inverse
     * (ou vis-versa). Si les "assertions" sont activées et qu'une
     * erreur supérieure est détectée, une exception
     * {@link AssertionError} sera lancée.
     */
    static final double MAX_ERROR = 1;

    /**
     * Marge de tolérance pour les comparaisons de nombre réels.
     */
    static final double EPS=1.0E-6;

    /**
     * Marge de tolérance pour les calculs itératifs.
     */
    static final double TOL=1E-10;

    /**
     * Longitude minimale, en degrés.
     */
    static final double XMIN = Longitude.MIN_VALUE;

    /**
     * Longitude maximale, en degrés.
     */
    static final double XMAX = Longitude.MAX_VALUE;

    /**
     * Latitude minimale, en degrés.
     */
    static final double YMIN = Latitude.MIN_VALUE;

    /**
     * Latitude maximale, en degrés.
     */
    static final double YMAX = Latitude.MAX_VALUE;

    /**
     * Default semi-major axis length (from WGS 1984).
     */
    static final double SEMI_MAJOR = 6378137;

    /**
     * Default semi-minor axis length (from WGS 1984).
     */
    static final double SEMI_MINOR = SEMI_MAJOR*(1-1/298.257223563);

    /**
     * The source coordinate system. If <code>null</code>, then {@link #getSourceCS()}
     * will initialize it to the <code>targetCS</code>'s geographic coordinate system.
     * This field is not final in order to allow caller to initialize it after construction.
     * But once this field is initialized, it should be considered as final.
     */
    protected GeographicCoordinateSystem sourceCS;

    /**
     * The destination coordinate system. This field is not final in
     * order to allow caller to initialize it after construction.
     * But once this field is initialized, it should be considered
     * as final.
     */
    protected ProjectedCoordinateSystem targetCS;

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
        super(parameters.getName(null));
        this.a                =                    parameters.getValue("semi_major");
        this.b                =                    parameters.getValue("semi_minor");
        this.centralLongitude = longitudeToRadians(parameters.getValue("central_meridian",   0), true);
        this.centralLatitude  =  latitudeToRadians(parameters.getValue("latitude_of_origin", 0), true);
        this.isSpherical      = (a==b);
        this.es = 1.0 - (b*b)/(a*a);
        this.e  = Math.sqrt(es);
    }

    /**
     * Gets the semantic type of transform.
     */
    public TransformType getTransformType()
    {return TransformType.CONVERSION;}

    /**
     * Gets the source coordinate system.
     */
    public final synchronized GeographicCoordinateSystem getSourceCS()
    {
        if (sourceCS==null)
        {
            final ProjectedCoordinateSystem targetCS = getTargetCS();
            if (targetCS!=null)
            {
                sourceCS = targetCS.getGeographicCoordinateSystem();
            }
        }
        return sourceCS;
    }

    /**
     * Gets the target coordinate system.
     */
    public final ProjectedCoordinateSystem getTargetCS()
    {return targetCS;}

    /**
     * Gets the dimension of input points, which is 2.
     */
    public final int getDimSource()
    {return 2;}

    /**
     * Gets the dimension of output points, which is 2.
     */
    public final int getDimTarget()
    {return 2;}

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
        // On utilise pas {@link #XMIN} et {@link #XMAX} car c'est derniers peuvent être volontairement décalés de {@link #EPS}.
        if (edge ? (x>=Longitude.MIN_VALUE && x<=Longitude.MAX_VALUE) : (x>Longitude.MIN_VALUE && x<Longitude.MAX_VALUE)) return Math.toRadians(x);
        else throw new IllegalArgumentException(Resources.format(Clé.LONGITUDE_OUT_OF_RANGE¤1, new Longitude(x)));
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
        // On utilise pas {@link #YMIN} et {@link #YMAX} car c'est derniers peuvent être volontairement décalés de {@link #EPS}.
        if (edge ? (y>=Latitude.MIN_VALUE && y<=Latitude.MAX_VALUE) : (y>Latitude.MIN_VALUE && y<Latitude.MAX_VALUE)) return Math.toRadians(y);
        else throw new IllegalArgumentException(Resources.format(Clé.LATITUDE_OUT_OF_RANGE¤1, new Latitude(y)));
    }

    /**
     * Construit une exception indiquant que la longitude <var>x</var>
     * spécifiée est en dehors des limites permises.
     */
    static TransformException badLongitudeException(final double x)
    {return new TransformException(Resources.format(Clé.LONGITUDE_OUT_OF_RANGE¤1, new Longitude(x)));}

    /**
     * Construit une exception indiquant que la latitude <var>y</var>
     * spécifiée est en dehors des limites permises.
     */
    static TransformException badLatitudeException(final double y)
    {return new TransformException(Resources.format(Clé.LATITUDE_OUT_OF_RANGE¤1, new Latitude(y)));}

    /**
     * Returns the distance between two points. Point (<var>x</var>,<var>y</var>) will be
     * transformed before to compute distance to <code>ptSrc</code>.   The transformation
     * will use {@link #transform(Point2D,Point2D)} if <code>inverse</code> is false,  or
     * {@link #inverseTransform(Point2D,Point2D)} if <code>inverse</code> is true.
     *
     * @param  ptSrc Source point. Must be in metres if <code>inverse</code> is false, or
     *         in degrees if <code>inverse</code> is true. This point will not be overwritten.
     * @param  x,y Ordinates of the resulting point. The (<var>x</var>,<var>y</var>)
     *         point must be in degrees if <code>inverse</code> is false, or in metres if
     *         <code>inverse</code> is true (this is the opposite of <code>ptSrc</code>).
     * @param  inverse <code>true</code> if an inverse transformation must be applied
     *         on (<var>x</var>,<var>y</var>) instead of a direct transformation.
     * @return The distance in metres.
     * @throws TransformException if a transformation failed.
     */
    final double distance(final Point2D ptSrc, final double x, final double y, final boolean inverse) throws TransformException
    {
        Point2D ptDst;
        if (ptSrc instanceof CheckPoint)
        {
            // Avoid never-ending loop.
            ptDst = ptSrc;
        }
        else
        {
            ptDst = new CheckPoint(x,y);
            ptDst = (inverse) ? inverseTransform(ptDst, ptDst) : transform(ptDst, ptDst);
        }
        if (!inverse)
        {
            // Compute cartesian distance in metres.
            return ptSrc.distance(ptDst);
        }
        else
        {
            // Compute orthodromic distance (spherical model) in metres.
            final double y1 = Math.toRadians(ptSrc.getY());
            final double y2 = Math.toRadians(ptDst.getY());
            final double dx = Math.toRadians(Math.abs(ptDst.getX()-ptSrc.getX()) % 360);
            return Math.acos(Math.sin(y1)*Math.sin(y2) + Math.cos(y1)*Math.cos(y2)*Math.cos(dx))*a;
        }
    }

    /**
     * Check point for private use by {@link #distance}. This class is necessary in order to
     * avoid never-ending loop in <code>assert</code> statements (when an <code>assert</code>
     * statement calls <code>transform</code>, which calls <code>inverseTransform</code>, which
     * calls <code>transform</code>, etc.).
     */
    private static final class CheckPoint extends Point2D.Double
    {
        public CheckPoint(final double x, final double y)
        {super(x,y);}
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
        throw new TransformException(Resources.format(Clé.NO_CONVERGENCE_FOR_LATITUDE¤1, new Latitude(ts)));
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
    ////                          TRANSFORMS                          ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

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
    public abstract Point2D transform(final Point2D ptSrc, Point2D ptDst) throws TransformException;

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
                final double x = src[srcOffset++];
                final double y = src[srcOffset++];
                if (!(x>=XMIN && x<=XMAX)) throw badLongitudeException(x);
                if (!(y>=YMIN && y<=YMAX)) throw badLatitudeException (y);

                point.x = x;
                point.y = y;
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
                final double x = src[srcOffset++];
                final double y = src[srcOffset++];
                if (!(x>=XMIN && x<=XMAX)) throw badLongitudeException(x);
                if (!(y>=YMIN && y<=YMAX)) throw badLatitudeException (y);

                point.x = x;
                point.y = y;
                transform(point, point);

                dest[dstOffset++] = (float) x;
                dest[dstOffset++] = (float) y;
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
    {return createTransformedShape(shape, null, null, Geometry.HORIZONTAL);}




    //////////////////////////////////////////////////////////////////////
    ////                                                              ////
    ////                      INVERSE TRANSFORMS                      ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

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
    public abstract Point2D inverseTransform(Point2D ptSrc, Point2D ptDst) throws TransformException;

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
                final double x = point.x;
                final double y = point.y;

                if (!(x>=XMIN && x<=XMAX)) throw badLongitudeException(x);
                if (!(y>=YMIN && y<=YMAX)) throw badLatitudeException (y);
                dest[dstOffset++] = x;
                dest[dstOffset++] = y;
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
                final double x = point.x;
                final double y = point.y;

                if (!(x>=XMIN && x<=XMAX)) throw badLongitudeException(x);
                if (!(y>=YMIN && y<=YMAX)) throw badLatitudeException (y);
                dest[dstOffset++] = (float) x;
                dest[dstOffset++] = (float) y;
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




    //////////////////////////////////////////////////////////////////////
    ////                                                              ////
    ////                        MISCELLANEOUS                         ////
    ////                                                              ////
    //////////////////////////////////////////////////////////////////////

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
        StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
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
            final Point2D        origin = new Point2D.Double(); // Initialized at (0,0).
            final CoordinatePoint coord = new CoordinatePoint(inverseTransform(origin, origin));
            buffer.append("origin=(");
            buffer.append(new CoordinateFormat().format(coord));
            buffer.append(')');
        }
        catch (TransformException exception)
        {
            // Ignore.
        }
    }

    /**
     * Inverse of a map projection. Note: Do not use the same name than
     * CoordinateTransform's inner class. It throw a VerifyError at run
     * time.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Inverse extends AbstractInverse
    {
        public Point2D transform(final Point2D source, final Point2D dest) throws TransformException
        {return MapProjection.this.inverseTransform(source, dest);}

        public void transform(final double[] source, final int srcOffset, final double[] dest, final int dstOffset, final int length) throws TransformException
        {MapProjection.this.inverseTransform(source, srcOffset, dest, dstOffset, length);}

        public void transform(final float[] source, final int srcOffset, final float[] dest, final int dstOffset, final int length) throws TransformException
        {MapProjection.this.inverseTransform(source, srcOffset, dest, dstOffset, length);}
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
         * be {@link MathTransform}, but as of JDK 1.4-beta2, it force
         * class loading for all projection classes (MercatorProjection,
         * etc.) before than necessary. Changing the returns type to
         * Object is a trick to avoir too early class loading...
         */
        protected abstract Object create(final Projection parameters);
    }
}
