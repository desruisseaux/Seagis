/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seagis.pt;

// Miscellaneous
import java.util.Locale;
import java.text.Format;
import java.io.Serializable;
import java.text.ParseException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

// Resources
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * An angle in degrees. An angle is the amount of rotation needed
 * to bring one line or plane into coincidence with another,
 * generally measured in degrees, sexagesimal degrees or grads.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see Latitude
 * @see Longitude
 * @see AngleFormat
 */
public class Angle implements Comparable, Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 1158747349433104534L;

    /**
     * A shared instance of {@link AngleFormat}.
     */
    private static Reference format;

    /**
     * D�finit de quelle fa�on les angles
     * peuvent �tre convertis en nombres.
     */
//  static
//  {
//      ClassChanger.register(new ClassChanger(Angle.class, Double.class)
//      {
//          protected Number convert(final Comparable o)
//          {return new Double(((Angle) o).theta);}
//
//          protected Comparable inverseConvert(final Number value)
//          {return new Angle(value.doubleValue());}
//      });
//  }

    /**
     * Angle value in degres.
     */
    private final double theta;

    /**
     * Contruct a new angle with the specified value.
     *
     * @param theta Angle in degrees.
     */
    public Angle(final double theta)
    {this.theta=theta;}

    /**
     * Constructs a newly allocated <code>Angle</code> object that
     * represents the angle value represented by the string.   The
     * string should represents an angle in either fractional degrees
     * (e.g. 45.5�) or degrees with minutes and seconds (e.g. 45�30').
     *
     * @param  string A string to be converted to an <code>Angle</code>.
     * @throws NumberFormatException if the string does not contain a parsable angle.
     */
    public Angle(final String string) throws NumberFormatException
    {
        try
        {
            final Angle theta = (Angle) getAngleFormat().parseObject(string);
            if (getClass().isAssignableFrom(theta.getClass()))
            {
                this.theta = theta.theta;
            }
            else throw new NumberFormatException(Resources.format(ResourceKeys.UNPARSABLE_ANGLE_$1, string));
        }
        catch (ParseException exception)
        {
            NumberFormatException e=new NumberFormatException(exception.getLocalizedMessage());
/* ---- BEGIN JDK 1.4 DEPENDENCIES ----
            e.initCause(exception);
   ---- END OF JDK 1.4 DEPENDENCIES ---- */
            throw e;
        }
    }

    /**
     * Returns the angle value in degrees.
     */
    public double degrees()
    {return theta;}
    
    /**
     * Returns the angle value in radians.
     */
    public double radians()
    {return Math.toRadians(theta);}

    /**
     * Returns a hash code for this <code>Angle</code> object.
     */
    public int hashCode()
    {
        final long code = Double.doubleToLongBits(theta);
        return (int) code ^ (int) (code >>> 32);
    }

    /**
     * Compares the specified object
     * with this angle for equality.
     */
    public boolean equals(final Object that)
    {
        if (that==this) return true;
        if (that!=null && getClass().equals(that.getClass()))
        {
            return Double.doubleToLongBits(theta) == Double.doubleToLongBits(((Angle) that).theta);
        }
        else return false;
    }
    
    /**
     * Compares two <code>Angle</code> objects numerically. The comparaison
     * is done as if by the {@link Double#compare(double,double)} method.
     */
    public int compareTo(final Object that)
    {
/* ---- BEGIN JDK 1.4 DEPENDENCIES ----
        return Double.compare(this.theta, ((Angle)that).theta);
   ---- END OF JDK 1.4 DEPENDENCIES ---- */
        final double d1 = this.theta;
        final double d2 = ((Angle)that).theta;
        if (d1  < d2) return -1;
        if (d1  > d2) return +1;
        if (d1 == d2) return  0;
        final long bits1 = Double.doubleToLongBits(d1);
        final long bits2 = Double.doubleToLongBits(d2);
        if (bits1 < bits2) return -1; // (-0.0, 0.0) or (!NaN, NaN)
        if (bits1 > bits2) return +1; // (0.0, -0.0) or (NaN, !NaN)
        return 0;
// ---- END OF JDK 1.3 FALLBACK
    }

    /**
     * Returns a string representation of this <code>Angle</code> object.
     */
    public String toString()
    {return getAngleFormat().format(this, new StringBuffer(), null).toString();}

    /**
     * Returns a shared instance of {@link AngleFormat}.
     * The return type is {@link Format} in order to
     * avoid class loading before necessary.
     */
    private static synchronized Format getAngleFormat()
    {
        if (format!=null)
        {
            final Format angleFormat = (Format) format.get();
            if (angleFormat!=null) return angleFormat;
        }
        final Format newFormat = new AngleFormat("D�MM.m'", Locale.US);
        format = new SoftReference(newFormat);
        return newFormat;
    }
}
