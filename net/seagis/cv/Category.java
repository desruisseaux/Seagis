/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 */
package net.seagis.cv;

// Miscellaneous
import java.awt.Color;
import java.util.Arrays;
import java.util.Locale;
import java.io.Serializable;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.gcs.Resources;
import net.seagis.resources.gcs.ResourceKeys;


/**
 * A category delimited by a range of sample values.
 * A category may represent qualitative or quantitative information, for
 * example geomorphologic structures or geophysics parameters. Some image
 * mixes both qualitative and quantitative categories. For example, images
 * of Sea Surface Temperature (SST) may have a quantitative category for
 * temperature with values ranging from –2 to 35°C, and three qualitative
 * categories for cloud, land and ice.
 * <br><br>
 * All categories must have a human readable name. In addition, quantitative
 * categories may define a transformation between sample values <var>p</var>
 * and geophysics values <var>v</var>. This transformation is usually (but
 * not always) a linear equation in the form <code>v=offset+scale*p</code>.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Category implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 3398663690240325702L;

    /**
     * The category name (may not be localized).
     */
    private final String name;

    /**
     * The lower sample value (inclusive) as an integer. This category is
     * made of all sample values in the range {@link #lower} inclusive to
     * {@link #upper} exclusive.
     */
    public final int lower;

    /**
     * The upper sample value (exclusive) as an integer. This category is
     * made of all sample values in the range {@link #lower} inclusive to
     * {@link #upper} exclusive.
     */
    public final int upper;

    /**
     * Offset is the value to add to grid values for this category.
     * This attribute is typically used when the category represents
     * elevation data (or any other geophysics parameter).
     *
     * For qualitative categories, this value is <code>NaN</code>.
     */
    protected final double offset;

    /**
     * Scale is the value which is multiplied to grid values for this
     * category.  This attribute is typically used when the category
     * represents elevation data (or any other geophysics parameter).
     *
     * For qualitative categories, this value is <code>NaN</code>.
     */
    protected final double scale;

    /**
     * The minimal geophysics value, inclusive. This value is usually (but not
     * always) equals to <code>{@link #toValue toValue}({@link #lower})</code>.
     *
     * For qualitative categories, this value is <code>NaN</code>.
     */
    protected final double minimum;

    /**
     * The maximal geophysics value, exclusive. This value is usually (but not
     * always) equals to <code>{@link #toValue toValue}({@link #upper})</code>.
     *
     * For qualitative categories, this value is <code>NaN</code>.
     */
    protected final double maximum;

    /**
     * Codes ARGB des couleurs de la catégorie. Les couleurs par
     * défaut seront un gradient allant du noir au blanc opaque.
     */
    private final int[] ARGB;

    /**
     * Codes ARGB par défaut. On utilise un examplaire unique
     * pour toutes les création d'objets {@link Category}.
     */
    private static final int[] DEFAULT = {0xFF000000, 0xFFFFFFFF};

    /**
     * A set of default category colors.
     */
    private static final Color[] CYCLE =
    {
        Color.blue,    Color.red,   Color.orange, Color.yellow,    Color.pink,
        Color.magenta, Color.green, Color.cyan,   Color.lightGray, Color.gray
    };

    /**
     * Construct a qualitative category for sample value <code>index</code>.
     *
     * @param  name    The category name.
     * @param  color   The category color, or <code>null</code> for a default color.
     * @param  index   The sample value as integer, usually in the range 0 to 255.
     */
    public Category(final String name, final Color color, final int index)
    {this(name, new Color[]{(color!=null) ? color : CYCLE[Math.abs(index)%CYCLE.length]}, index, index+1);}

    /**
     * Construct a qualitative category for sample values ranging
     * from <code>lower</code> to <code>upper</code>.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. This array may have any length;
     *                 colors will be interpolated as needed. An array of length 1 means
     *                 that an uniform color should be used for all sample values. An array
     *                 of length 0 or a <code>null</code> array means that some default colors
     *                 should be used (usually a gradient from opaque black to opaque white).
     * @param  lower   The lower sample value, inclusive.
     * @param  upper   The upper sample value, exclusive.
     *
     * @throws IllegalArgumentException if <code>lower</code> is not smaller than <code>upper</code>.
     */
    public Category(final String name, final Color[] colors, final int lower, final int upper) throws IllegalArgumentException
    {this(name, colors, lower, upper, toNaN(lower));}

    /**
     * Construct a qualitative category with the specified NaN value.
     */
    private Category(final String name, final Color[] colors, final int lower, final int upper, final float NaN) throws IllegalArgumentException
    {this(name, colors, lower, upper, NaN, NaN, false);}

    /**
     * Construct a quantitative category for sample values ranging from <code>lower</code>
     * to <code>upper</code>. Integer sample values will be converted into geophysics values
     * (usually floating-point) through a linear equation.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. This array may have any length;
     *                 colors will be interpolated as needed. An array of length 1 means
     *                 that an uniform color should be used for all sample values. An array
     *                 of length 0 or a <code>null</code> array means that some default colors
     *                 should be used (usually a gradient from opaque black to opaque white).
     * @param  lower   The lower sample value, inclusive.
     * @param  upper   The upper sample value, exclusive.
     * @param  offset  The <code>offset</code> coefficient in the linear equation <code>v=offset+scale*p</code>.
     * @param  scale   The <code>scale</code>  coefficient in the linear equation <code>v=offset+scale*p</code>.
     *
     * @throws IllegalArgumentException if <code>lower</code> is not smaller than <code>upper</code>,
     *         or if <code>offset</code> or <code>scale</code> coefficients are illegal.
     */
    public Category(final String name, final Color[] colors, final int lower, final int upper, final double offset, final double scale) throws IllegalArgumentException
    {this(name, colors, lower, upper, offset, scale, true);}

    /**
     * Construct a category with the specified scale and offset.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category.
     * @param  lower   The lower sample value, inclusive.
     * @param  upper   The upper sample value, exclusive.
     * @param  offset  The <code>offset</code> coefficient in the linear equation <code>v=offset+scale*p</code>.
     * @param  scale   The <code>scale</code>  coefficient in the linear equation <code>v=offset+scale*p</code>.
     * @param  isQuantitative <code>true</code> if this category is a quantitative one.
     *
     * @throws IllegalArgumentException if <code>lower</code> is not smaller than <code>upper</code>,
     *         or if <code>offset</code> or <code>scale</code> coefficients are illegal.
     */
    private Category(final String name, final Color[] colors, final int lower, final int upper,
                     final double offset, final double scale, final boolean isQuantitative) throws IllegalArgumentException
    {
        this.name   = name.trim();
        this.lower  = lower;
        this.upper  = upper;
        this.offset = offset;
        this.scale  = scale;
        // Use '!' in order to catch NaN (previous version used float type).
        if (!(lower<upper)/* || Float.isInfinite(lower) || Float.isInfinite(upper)*/)
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_RANGE_$2, new Integer(lower), new Integer(upper-1)));
        }
        if (Double.isNaN(offset)==isQuantitative || Double.isInfinite(offset))
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_COEFFICIENT_$2, "offset", new Double(offset)));
        }
        if (Double.isNaN(scale)==isQuantitative || Double.isInfinite(scale))
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_COEFFICIENT_$2, "scale", new Double(scale)));
        }
        final double min = toValue(lower);
        final double max = toValue(upper);
        if (min > max)
        {
            this.minimum = max;
            this.maximum = min;
        }
        else
        {
            this.minimum = min;
            this.maximum = max;
        }
        if (colors!=null && colors.length!=0)
        {
            ARGB = new int[colors.length];
            for (int i=0; i<ARGB.length; i++)
                ARGB[i] = colors[i].getRGB();
        }
        else ARGB = DEFAULT;
    }

    /**
     * Returns the category name localized in the specified locale. If no name is
     * available for the specified locale, then an arbitrary locale may be used.
     * The default implementation returns the <code>name</code> argument specified
     * at construction time.
     *
     * @param  locale The desired locale, or <code>null</code> for the default locale.
     * @return The category name, localized if possible.
     */
    public String getName(final Locale locale)
    {return name;}

    /**
     * Returns the set of colors for this category.
     * Change to the returned array will not affect
     * this category.
     *
     * @see CategoryList#getColorModel
     */
    public Color[] getColors()
    {
        final Color[] colors=new Color[ARGB.length];
        for (int i=0; i<colors.length; i++)
            colors[i] = new Color(ARGB[i], true);
        return colors;
    }

    /**
     * Compute the geophysics value from an index value. The <code>index</code>
     * value should be in the range <code>[{@link #lower}..{@link #upper}]</code>.
     * However, this methods will not performs range check. Index out of range
     * may lead to extrapolation, which may or may not have a physical maining.
     *
     * @see CategoryList#toValue
     */
    protected double toValue(final int index)
    {return offset + scale*index;}

    /**
     * Compute the index value from a geophysics value.  If the resulting index is outside
     * this category's range (<code>[{@link #lower}..{@link #upper}]</code>), then it will
     * be clamp to <code>lower</code> or <code>upper-1</code> as necessary.
     *
     * @see CategoryList#toIndex
     */
    protected int toIndex(final double value)
    {
        final double index = Math.rint((value-offset)/scale);
        return (index>=lower) ? ((index<upper) ? (int)index : upper-1) : lower;
    }

    /**
     * Returns <code>true</code> if this category is quantitative. The
     * method {@link #toValue} returns real numbers for quantitative
     * categories and <code>NaN</code> for qualitative categories.
     *
     * @return <code>true</code> if this category is quantitative, or
     *         <code>false</code> if this category is qualitative.
     */
    public boolean isQuantitative()
    {return !Double.isNaN(offset) && !Double.isNaN(scale);}

    /**
     * Returns <code>true</code> if {@link #toValue} just returns his argument with no change.
     * This method always returns <code>false</code> for qualitative categories,
     * since qualitative categories change sample values into <code>NaN</code>
     * values.
     */
    public boolean isIdentity()
    {return offset==0 && scale==1;}

    /**
     * Returns a hash value for this category.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {return name.hashCode();}

    /**
     * Compares the specified object with
     * this category for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true; // Slight optimization
        if (object!=null && object.getClass().equals(getClass()))
        {
            final Category that = (Category) object;
            return Double.doubleToRawLongBits(this.lower  ) == Double.doubleToRawLongBits(that.lower  ) &&
                   Double.doubleToRawLongBits(this.upper  ) == Double.doubleToRawLongBits(that.upper  ) &&
                   Double.doubleToRawLongBits(this.offset ) == Double.doubleToRawLongBits(that.offset ) &&
                   Double.doubleToRawLongBits(this.scale  ) == Double.doubleToRawLongBits(that.scale  ) &&
                   Double.doubleToRawLongBits(this.minimum) == Double.doubleToRawLongBits(that.minimum) &&
                   Double.doubleToRawLongBits(this.maximum) == Double.doubleToRawLongBits(that.maximum) &&
                   Utilities.equals(this.name, that.name) &&
                      Arrays.equals(this.ARGB, that.ARGB);
        }
        return false;
    }

    /**
     * Returns a string representation of this category.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(name);
        buffer.append("\":[");
        buffer.append(lower);
        buffer.append("..");
        buffer.append(upper-1); // Inclusive
        buffer.append("]]");
        return buffer.toString();
    }

    /**
     * Returns a NaN number for the specified category number. Valid NaN numbers have
     * bit fields ranging from <code>0x7f800001</code> through <code>0x7fffffff</code>
     * or <code>0xff800001</code> through <code>0xffffffff</code>. The standard {@link
     * Float#NaN} has bit fields <code>0x7fc00000</code>.
     *
     * @param  index The category number, from -2097152 to 2097151 inclusive. This number
     *               doesn't need to matches sample values.    Different categories don't
     *               need to use increasing,  different or contiguous numbers. This is up
     *               to the user to manage his category numbers.  Category numbers may be
     *               anything like 1 for "cloud", 2 for "ice", 3 for "land", etc.
     * @return       The NaN value as a float. We limit ourself to the float type instead
     *               of double because the underlying image storage type way be float.
     * @throws IndexOutOfBoundsException if the specified index is out of bounds.
     */
    private static float toNaN(int index) throws IndexOutOfBoundsException
    {
        index += 0x200000;
        if (index>=0 && index<=0x3FFFFF)
        {
            final float value = Float.intBitsToFloat(0x7FC00000 + index);
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert Float.isNaN(value);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            return value;
        }
        else throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    /**
     * Construct a list of categories.
     */
    static Category[] list(final String[] names)
    {
        final Color[] colors = new Color[names.length];
        final double scale = 255.0/colors.length;
        for (int i=0; i<colors.length; i++)
        {
            final int r = (int)Math.round(scale*i);
            colors[i] = new Color(r,r,r);
        }
        return list(names, colors);
    }

    /**
     * Construct a list of categories.
     */
    static Category[] list(final String[] names, final Color[] colors)
    {
        if (names.length!=colors.length)
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_MISMATCHED_ARRAY_LENGTH));
        }
        final Category[] categories = new Category[names.length];
        for (int i=0; i<categories.length; i++)
            categories[i] = new Category(names[i], colors[i], i);
        return categories;
    }
}
