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

// Miscellaneous
import java.util.Locale;
import java.awt.geom.Point2D;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.cs.Ellipsoid;
import net.seas.resources.Resources;


/**
 * Projection cylindrique de Mercator. Les parall�les et les m�ridients apparaissent
 * comme des lignes droites et se croisent � angles droits; cette projection produit
 * donc des cartes rectangulaires. L'�chelle est vrai le long de l'�quateur (par d�faut)
 * ou le long de deux parall�les �quidistants de l'�quateur. Cette projection est utilis�e
 * pour repr�senter des r�gions pr�s de l'�quateur. Elle est aussi souvent utilis�e pour la
 * navigation maritime parce que toutes les lignes droites sur la carte sont des lignes
 * <em>loxodromiques</em>, c'est-�-dire qu'un navire suivant cette ligne garderait azimuth
 * constant sur son compas.
 *
 * @version 1.0
 * @author Andr� Gosselin
 * @author Martin Desruisseaux
 */
final class MercatorProjection extends CylindricalProjection
{
    /*
     * R�f�rence des formules: John P. Snyder (Map Projections - A Working Manual,
     *                         U.S. Geological Survey Professional Paper 1395, 1987)
     */

    /**
     * Facteur d'�chelle global. Ce facteur
     * sera multipli� par {@link #a}.
     */
    private final double k0;

    /**
     * Latitude � laquelle l'�chelle est vrai, en radians.
     * La valeur par d�faut est 0 (l'�quateur).
     */
    private final double lat_ts;

    /**
     * Construit une projection cartographique
     * qui utilisera l'ellipso�de sp�cifi�.
     */
    public MercatorProjection(final Ellipsoid ellipsoid, final Point2D centroid)
    {this(ellipsoid, centroid, centroid.getY());}

    /**
     * Construit une projection cartographique
     * qui utilisera l'ellipso�de sp�cifi�.
     *
     * @param ellipsoid Ellipso�de � utiliser pour cette projection cartographique.
     * @param centroid  Coordonn�es g�ographique du centre de la projection.
     * @param latitudeTrueScale Latitude o� les �chelles seront exactes.
     */
    public MercatorProjection(final Ellipsoid ellipsoid, final Point2D centroid, final double latitudeTrueScale)
    {
        super(ellipsoid, centroid);
        latitudeToRadians(centroid.getY(), false); // Argument check.
        lat_ts = Math.abs(latitudeToRadians(latitudeTrueScale, false));
        if (isSpherical)
        {
            k0 = a*Math.cos(lat_ts);
        }
        else
        {
            k0 = a*msfn(Math.sin(lat_ts), Math.cos(lat_ts));
        }
    }

    /**
     * Returns a human readable name localized for the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(Cl�.CYLINDRICAL_MERCATOR);}

    /**
     * Retourne la latitude � laquelle l'�chelle est exacte.
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

        if (Math.abs(y) > (Math.PI/2 - EPS))
        {
            throw new TransformException(Resources.format(Cl�.POLE_PROJECTION�1, new Latitude(ptSrc.getY())));
        }

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        x = (x-centralLongitude)*k0;
        if (isSpherical)
        {
            y =  k0*Math.log(Math.tan((Math.PI/4) + 0.5*y));
        }
        else
        {
            y = -k0*Math.log(tsfn(y, Math.sin(y)));
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
        double x = ptSrc.getX();
        double y = ptSrc.getY();

        //////////////////////////
        //   Transformation     //
        //////////////////////////
        x = x/k0 + centralLongitude;
        y = Math.exp(-y/k0);
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
        final long code = Double.doubleToLongBits(lat_ts);
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
               Double.doubleToLongBits(this.lat_ts) == Double.doubleToLongBits(that.lat_ts);
    }

    /**
     * Impl�mentation de la partie entre crochets
     * de la cha�ne retourn�e par {@link #toString()}.
     */
    void toString(final StringBuffer buffer)
    {
        super.toString(buffer);
        buffer.append(", Truescale=");
        buffer.append(getLatitudeTrueScale());
        buffer.append(']');
    }
}
