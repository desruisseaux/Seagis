/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.cs;

// Miscellaneous
import java.io.Serializable;
import net.seagis.pt.Matrix;
import net.seagis.resources.Utilities;


/**
 * Parameters for a geographic transformation into WGS84.
 * The Bursa Wolf parameters should be applied to geocentric coordinates,
 * where the X axis points towards the Greenwich Prime Meridian, the Y axis
 * points East, and the Z axis points North.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_WGS84ConversionInfo
 */
public class WGS84ConversionInfo implements Cloneable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 3427461418504464735L;

    /** Bursa Wolf shift in meters. */
    public double dx;

    /** Bursa Wolf shift in meters. */
    public double dy;

    /** Bursa Wolf shift in meters. */
    public double dz;

    /** Bursa Wolf rotation in arc seconds. */
    public double ex;

    /** Bursa Wolf rotation in arc seconds. */
    public double ey;

    /** Bursa Wolf rotation in arc seconds. */
    public double ez;

    /** Bursa Wolf scaling in parts per million. */
    public double ppm;

    /** Human readable text describing intended region of transformation. */
    public String areaOfUse;

    /**
     * Construct a conversion info
     * with all parameters set to 0.
     */
    public WGS84ConversionInfo()
    {}

    /**
     * Returns an affine maps that can be used to define this
     * Bursa Wolf transformation. The formula is as follows:
     *
     * <blockquote><pre>
     * S = 1 + {@link #ppm}/1000000
     *
     * [ X� ]    [     S   -{@link #ez}*S   +{@link #ey}*S   {@link #dx} ]  [ X ]
     * [ Y� ]  = [ +{@link #ez}*S       S   -{@link #ex}*S   {@link #dy} ]  [ Y }
     * [ Z� ]    [ -{@link #ey}*S   +{@link #ex}*S       S   {@link #dz} ]  [ Z ]
     * [ 1  ]    [     0       0       0    1 ]  [ 1 ]
     * </pre></blockquote>
     *
     * This affine transform can be applied on <strong>geocentric</strong>
     * coordinates.
     */
    public Matrix getAffineTransform()
    {
        final double S = 1 + ppm/1E+6;
        return new Matrix(4,4, new double[]
        {
                 S,  -ez*S,  +ey*S,  dx,
             +ez*S,      S,  -ex*S,  dy,
             -ey*S,  +ex*S,      S,  dz,
                 0,      0,      0,   1
        });
    }

    /**
     * Returns a hash value for this object.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        long code = 14698129;
        code = code*37 + Double.doubleToLongBits(dx );
        code = code*37 + Double.doubleToLongBits(dy );
        code = code*37 + Double.doubleToLongBits(dz );
        code = code*37 + Double.doubleToLongBits(ex );
        code = code*37 + Double.doubleToLongBits(ey );
        code = code*37 + Double.doubleToLongBits(ez );
        code = code*37 + Double.doubleToLongBits(ppm);
        return (int)(code >>> 32) ^ (int)code;
    }

    /**
     * Returns a copy of this object.
     */
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException exception)
        {
            // Should not happen, since we are cloneable.
            final InternalError error = new InternalError(exception.getMessage());
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            error.initCause(exception);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            throw error;
        }
    }

    /**
     * Compares the specified object with
     * this object for equality.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof WGS84ConversionInfo)
        {
            final WGS84ConversionInfo that = (WGS84ConversionInfo) object;
            return Double.doubleToLongBits(this.dx)  == Double.doubleToLongBits(that.dx)  &&
                   Double.doubleToLongBits(this.dy)  == Double.doubleToLongBits(that.dy)  &&
                   Double.doubleToLongBits(this.dz)  == Double.doubleToLongBits(that.dz)  &&
                   Double.doubleToLongBits(this.ex)  == Double.doubleToLongBits(that.ex)  &&
                   Double.doubleToLongBits(this.ey)  == Double.doubleToLongBits(that.ey)  &&
                   Double.doubleToLongBits(this.ez)  == Double.doubleToLongBits(that.ez)  &&
                   Double.doubleToLongBits(this.ppm) == Double.doubleToLongBits(that.ppm) &&
                   Utilities.equals(this.areaOfUse, that.areaOfUse);
        }
        else return false;
    }

    /**
     * Returns the Well Know Text (WKT) for this object.
     * The WKT is part of OpenGIS's specification and
     * looks like <code>TOWGS84[dx, dy, dz, ex, ey, ez, ppm]</code>.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer("TOWGS84[\"");
        buffer.append(areaOfUse);
        buffer.append(dx); buffer.append(", ");
        buffer.append(dy); buffer.append(", ");
        buffer.append(dz); buffer.append(", ");
        buffer.append(ex); buffer.append(", ");
        buffer.append(ey); buffer.append(", ");
        buffer.append(ez); buffer.append(", ");
        buffer.append(ppm);
        buffer.append(']');
        return buffer.toString();
    }
}