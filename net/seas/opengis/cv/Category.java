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
 */
package net.seas.opengis.cv;

// Miscellaneous
import java.awt.Color;
import java.util.Arrays;
import java.util.Locale;
import java.util.Comparator;
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.util.XMath;
import net.seas.resources.Resources;


/**
 * A category delimited by a range of sample values.
 * A category may represent qualitative or quantitative information, for
 * example geomorphologic structures or geophysics parameters. Some image
 * mixes both qualitative and quantitative categories. For example, images
 * of Sea Surface Temperature (SST) may have a quantitative category for
 * temperature with values ranging from –2 to 35°C, and three qualitative
 * categories for cloud, land and ice.
 * <br><br>
 * All categories must have a human readable name. In addition, some quantitative
 * categories may define a linear equation <code>v=C<sub>0</sub>+C<sub>1</sub>*p</code>
 * converting sample values <var>p</var> into geophysics values <var>v</var>.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Category implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -4387326509458680963L;

    /**
     * The category name (may not be localized).
     */
    private final String name;

    /**
     * The lower sample value, inclusive. The category is made of all samples in the
     * range {@link #lower} inclusive to {@link #upper} exclusive. Those numbers are
     * usually (but not always) integers.
     */
    protected final float lower;

    /**
     * The upper sample value, exclusive. The category is made of all samples in the
     * range {@link #lower} inclusive to {@link #upper} exclusive. Those numbers are
     * usually (but not always) integers.
     */
    protected final float upper;

    /**
     * The upper sample value, <strong>inclusive</strong>.
     * This is needed by {@link #toSample} for clamping values.
     */
    final float upperInclusive;

    /**
     * The minimal geophysics value, inclusive. This value is usually (but not
     * always) equals to <code>{@link #toValue toValue}({@link #lower})</code>.
     */
    final float minimum;

    /**
     * The maximal geophysics value, exclusive. This value is usually (but not
     * always) equals to <code>{@link #toValue toValue}({@link #upper})</code>.
     */
    final float maximum;

    /**
     * The C0 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * This coefficient is {@link Double#NaN} if this category is not a
     * quantitative one.
     */
    final float C0;

    /**
     * The C1 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * This coefficient is {@link Double#NaN} if this category is not a
     * quantitative one.
     */
    final float C1;

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
            throw new IllegalArgumentException(Resources.format(Clé.MISMATCHED_ARRAY_LENGTH));
        }
        final Category[] categories = new Category[names.length];
        for (int i=0; i<categories.length; i++)
            categories[i] = new Category(names[i], colors[i], i);
        return categories;
    }

    /**
     * Construct a qualitative category for sample value <code>index</code>.
     *
     * @param  name    The category name.
     * @param  color   This category color.
     * @param  index   The category number, from 0 to 4194303 inclusive. This number
     *                 is also the sample value for this category. Common values are
     *                 usually in the range 0 to 255.
     */
    public Category(final String name, final Color color, final int index)
    {this(name, new Color[] {color}, index, index+1, toNaN(index));}

    /**
     * Construct a qualitative category for sample values ranging from
     * <code>lower</code> inclusive to <code>upper</code> exclusive.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. The array's length don't need to
     *                 be equals to <code>upper-lower</code>.  Colors will be interpolated
     *                 as needed. An array of length 1 means that an uniform color should be
     *                 used for all values range. An array of length 0 or a <code>null</code>
     *                 array means that a default color set should be used (usually a gradient
     *                 from opaque black to opaque white).
     * @param  lower   The lower sample value, inclusive. This is usually an integer.
     * @param  upper   The upper sample value, exclusive. This is usually an integer.
     * @param  index   The category number, from 0 to 4194303 inclusive.  This number doesn't
     *                 need to matches sample values. Differents categories don't need to use
     *                 increasing, different or contiguous numbers. This is up to the user to
     *                 manage his category numbers. It is recommended to reserve number 0 for
     *                 "no data". Others numbers may be anything like 1 for "cloud", 2 for
     *                 "ice", 3 for "land", etc.
     *
     * @throws IllegalArgumentException if <code>lower</code> is greater than or equals to <code>upper</code>.
     */
    public Category(final String name, final Color[] colors, final float lower, final float upper, final int index) throws IllegalArgumentException
    {this(name, colors, lower, upper, toNaN(index));}

    /**
     * Construct a qualitative category with the specified NaN value.
     */
    private Category(final String name, final Color[] colors, final float lower, final float upper, final float NaN) throws IllegalArgumentException
    {this(name, colors, lower, upper, NaN, NaN, false);}

    /**
     * Construct a quantitative category for sample values ranging from <code>lower</code>
     * inclusive to <code>upper</code> exclusive.  Sample values (usually integer) will be
     * converted into geophysics values (usually floating-point) through a linear equation.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. The array's length don't need to
     *                 be equals to <code>upper-lower</code>.  Colors will be interpolated
     *                 as needed. An array of length 1 means that an uniform color should be
     *                 used for all values range. An array of length 0 or a <code>null</code>
     *                 array means that a default color set should be used (usually a gradient
     *                 from opaque black to opaque white).
     * @param  lower   The lower sample value, inclusive. This is usually an integer.
     * @param  upper   The upper sample value, exclusive. This is usually an integer.
     * @param  C0      The C0 coefficient in the linear equation <code>v=C0+C1*p</code>.
     * @param  C1      The C1 coefficient in the linear equation <code>v=C0+C1*p</code>.
     *
     * @throws IllegalArgumentException if <code>lower</code> is greater than or equals to <code>upper</code>,
     *         or if <code>C0</code> or <code>C1</code> coefficients are illegal.
     */
    public Category(final String name, final Color[] colors, final float lower, final float upper, final float C0, final float C1) throws IllegalArgumentException
    {this(name, colors, lower, upper, C0, C1, true);}

    /**
     * Construct a qualitative or quantitative category for sample values ranging from
     * <code>lower</code> inclusive to <code>upper</code> exclusive. Sample values
     * (usually integer) can be converted into geophysics values (usually floating-point)
     * through a linear equation.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. The array's length don't need to
     *                 be equals to <code>upper-lower</code>.  Colors will be interpolated
     *                 as needed. An array of length 1 means that an uniform color should be
     *                 used for all values range. An array of length 0 or a <code>null</code>
     *                 array means that a default color set should be used (usually a gradient
     *                 from opaque black to opaque white).
     * @param  lower   The lower sample value, inclusive. This is usually an integer.
     * @param  upper   The upper sample value, exclusive. This is usually an integer.
     * @param  C0      The C0 coefficient in the linear equation <code>v=C0+C1*p</code>.
     * @param  C1      The C1 coefficient in the linear equation <code>v=C0+C1*p</code>.
     * @param  isQuantitative <code>true</code> if this category is a quantitative one.
     *
     * @throws IllegalArgumentException if <code>lower</code> is greater than or equals to
     *         <code>upper</code>, or if <code>C0</code> and/or <code>C1</code> coefficients
     *         are illegal.
     */
    private Category(final String name, final Color[] colors, final float lower, final float upper, final float C0, final float C1, boolean isQuantitative) throws IllegalArgumentException
    {
        this.name  = name.trim();
        this.lower = lower;
        this.upper = upper;
        this.C0    = C0;
        this.C1    = C1;
        if (!(lower<upper && !Float.isInfinite(lower) && !Float.isInfinite(upper)))
        {
            throw new IllegalArgumentException(Resources.format(Clé.BAD_RANGE¤2, new Float(lower), new Float(upper)));
        }
        if (Float.isNaN(C0)==isQuantitative || Float.isInfinite(C0))
        {
            throw new IllegalArgumentException(Resources.format(Clé.BAD_COEFFICIENT¤2, "C0", new Float(C0)));
        }
        if (Float.isNaN(C1)==isQuantitative || Float.isInfinite(C1))
        {
            throw new IllegalArgumentException(Resources.format(Clé.BAD_COEFFICIENT¤2, "C1", new Float(C1)));
        }
        // Default computation for 'upperInclusive'.
        upperInclusive = previousFloat(upper);
        assert(upperInclusive>=lower && upperInclusive<upper);

        // Finish construction.
        final float min = toValue(lower);
        final float max = toValue(upper);
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
     * Returns a NaN number for the specified category number.
     * Valid NaN numbers have bit fields ranging from
     * <code>0x7f800001</code> through <code>0x7fffffff</code> or
     * <code>0xff800001</code> through <code>0xffffffff</code>.
     * The standard {@link Float#NaN} has bit fields <code>0x7fc00000</code>.
     */
    private static float toNaN(final int index) throws IndexOutOfBoundsException
    {
        if (index>=0 && index<=0x3FFFFF)
        {
            return Float.intBitsToFloat(0x7FC00000 + index);
        }
        else throw new IndexOutOfBoundsException(String.valueOf(index));
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
     * Returns <code>true</code> if this category is quantitative. The
     * method {@link #toValue} returns real numbers for quantitative
     * categories and <code>NaN</code> for qualitative categories.
     *
     * @return <code>true</code> if this category is quantitative, or
     *         <code>false</code> if this category is qualitative.
     */
    public boolean isQuantitative()
    {return !Float.isNaN(C0) && !Float.isNaN(C1);}

    /**
     * Returns <code>true</code> if {@link #toValue} just returns his argument with no change.
     * This method always returns <code>false</code> for qualitative categories,
     * since qualitative categories change sample values into <code>NaN</code>
     * values.
     */
    public boolean isIdentity()
    {return C0==0 && C1==1;}

    /**
     * Compute the geophysics value from a sample value. This method doesn't
     * need to check if <code>sample</code> lies in this category's range
     * (<code>[{@link #lower}..{@link #upper}]</code>). Values out of range
     * may lead to extrapolation, which may or may not have a physical maining.
     *
     * @see CategoryList#toValue
     */
    protected float toValue(final float sample)
    {return C0+C1*sample;}

    /**
     * Compute the sample value from a geophysics value. If the resulting value is outside this category's
     * range (<code>[{@link #lower}..{@link #upper}]</code>), then it will be clamp to <code>lower</code>
     * or <code>{@link #previousFloat previousFloat}(upper)</code> as necessary. This method never returns
     * infinity or NaN.
     * <br><br>
     * If an integer value is desired, we suggest rounding toward negative infinity
     * (as <code>{@link Math#floor Math.floor}(sample))</code>).
     *
     * @see CategoryList#toSample
     */
    protected float toSample(final float value)
    {
        final float sample = (value-C0)/C1;
        return (sample>=lower) ? ((sample<=upperInclusive) ? sample : upperInclusive) : lower;
    }

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
            return Float.floatToRawIntBits(this.lower  ) == Float.floatToRawIntBits(that.lower  ) &&
                   Float.floatToRawIntBits(this.upper  ) == Float.floatToRawIntBits(that.upper  ) &&
                   Float.floatToRawIntBits(this.minimum) == Float.floatToRawIntBits(that.minimum) &&
                   Float.floatToRawIntBits(this.maximum) == Float.floatToRawIntBits(that.maximum) &&
                   Float.floatToRawIntBits(this.C0     ) == Float.floatToRawIntBits(that.C0     ) &&
                   Float.floatToRawIntBits(this.C1     ) == Float.floatToRawIntBits(that.C1     ) &&
                   XClass.equals(this.name, that.name) &&
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
        final StringBuffer buffer = new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(name);
        buffer.append(":[");
        final int lo = (int)lower;
        final int up = (int)upper;
        if (lo==lower && up==upper)
        {
            buffer.append(lo);
            buffer.append("..");
            buffer.append(up);
        }
        else
        {
            buffer.append(lower);
            buffer.append("..");
            buffer.append(upper);
        }
        buffer.append("]]");
        return buffer.toString();
    }

    /**
     * Finds the least float greater than <var>f</var>. If <code>NaN</code>, returns same
     * value.  This method may be used at construction time for excluding the lower bound
     * or including the upper bound.
     *
     * @see java.text.ChoiceFormat#nextDouble
     */
    public static final float nextFloat(final float f)
    {return nextFloat(f, true);}

    /**
     * Finds the greatest float less than <var>f</var>.
     * If <code>NaN</code>, returns same value.
     *
     * @see java.text.ChoiceFormat#previousDouble
     */
    public static final float previousFloat(final float f)
    {return nextFloat(f, false);}

    /**
     * Finds the least float greater than d (if positive == true),
     * or the greatest float less than d (if positive == false).
     * If NaN, returns same value. This code is an adaptation of
     * {@link java.text.ChoiceFormat#nextDouble}.
     */
    private static float nextFloat(final float f, final boolean positive)
    {
        final int SIGN             = 0x80000000;
        final int POSITIVEINFINITY = 0x7F800000;

        // Filter out NaN's
        if (Float.isNaN(f)) return f;

        // Zero's are also a special case
        if (f == 0.0)
        {
            final float smallestPositiveFloat = Float.intBitsToFloat(1);
            return (positive) ? smallestPositiveFloat : -smallestPositiveFloat;
        }

        // If entering here, d is a nonzero value.
        // Hold all bits in a int for later use.
        final int bits = Float.floatToIntBits(f);

        // Strip off the sign bit.
        int magnitude = bits & ~SIGN;

        // If next float away from zero, increase magnitude.
        // Else decrease magnitude
        if ((bits > 0) == positive)
        {
            if (magnitude != POSITIVEINFINITY)
            {
                magnitude++;
            }
        }
        else
        {
            magnitude--;
        }

        // Restore sign bit and return.
        final int signbit = bits & SIGN;
        return Float.intBitsToFloat(magnitude | signbit);
    }




    /**
     * A quantitative category converting converting geophysics values
     * into sample values through a logarithmic equation. The reverse operation
     * (convert sample values into geophysics values) is done through the following equation:
     * <code>v=10^(C<sub>0</sub>+C<sub>1</sub>*p)</code> where <var>p</var> is the (usually
     * integer) pixel value and <var>v</var> is the (usually floating-point) geophysics value.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static class Logarithmic extends Category
    {
        /**
         * Serial number for interoperability with different versions.
         */
        private static final long serialVersionUID = -8013388870292811110L;

        /**
         * Construct a logarithmic category.
         */
        public Logarithmic(final String name, final Color[] colors, final float lower, final float upper, final float C0, final float C1) throws IllegalArgumentException
        {super(name, colors, lower, upper, C0, C1);}

        /**
         * Returns <code>false</code>, since {@link #toValue} always change some values.
         */
        public boolean isIdentity()
        {return false;}

        /**
         * Compute the geophysics value from a sample value.
         */
        protected float toValue(final float sample)
        {return (float)XMath.pow10(C0+C1*sample);}

        /**
         * Compute the pixel value from the geophysics value.
         */
        protected float toSample(final float value)
        {
            final float sample = (float) ((XMath.log10(value)-C0)/C1);
            return (sample>=lower) ? ((sample<=upperInclusive) ? sample : upperInclusive) : lower;
        }
    }
}
