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
import net.seas.resources.Resources;


/**
 * <FONT COLOR="#FF6633">A category delimited by a range of pixel values.</FONT>
 * A category may represent qualitative or quantitative information, for
 * example geomorphologic structures or geophysics parameters. Some image
 * mixes both qualitative and quantitative categories. For example, images
 * of Sea Surface Temperature (SST) may have a quantitative category for
 * temperature with values ranging from –2 to 35°C, and three qualitative
 * categories for cloud, land and ice.
 * <br><br>
 * All categories must have a human readable name. In addition, some quantitative
 * categories may define a linear equation <code>y=C<sub>0</sub>+C<sub>1</sub>*x</code>
 * converting pixel values <var>x</var> into geophysics values <var>y</var>.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Category implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    // private static final long serialVersionUID = ?; // TODO

    /**
     * The category name.
     */
    private final String name;

    /**
     * The lower pixel value range, inclusive. This category is made of
     * all pixels in the range {@link #lower} to {@link #upper} inclusive.
     */
    protected final float lower;

    /**
     * The upper pixel value range, inclusive. This category is made of
     * all pixels in the range {@link #lower} to {@link #upper} inclusive.
     */
    protected final float upper;

    /**
     * The minimal geophysics value, inclusive. This value is usually (but not
     * always) equals to <code>{@link #toValue toValue}({@link #lower})</code>.
     */
    final float minimum;

    /**
     * The maximal geophysics value, inclusive. This value is usually (but not
     * always) equals to <code>{@link #toValue toValue}({@link #upper})</code>.
     */
    final float maximum;

    /**
     * The C0 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * This coefficient is {@link Double#NaN} if this category is not a
     * quantative one.
     */
    protected final float C0;

    /**
     * The C1 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * This coefficient is {@link Double#NaN} if this category is not a
     * quantative one.
     */
    protected final float C1;

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
     * Construct a qualitative category for pixel values
     * ranging from <code>lower</code> to <code>upper</code>.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. The array's length don't need to
     *                 be equals to <code>upper-lower</code>.  Colors will be interpolated
     *                 as needed. An array of length 1 means that an uniform color should be
     *                 used for all values range. An array of length 0 or a <code>null</code>
     *                 array means that a default color set should be used (usually a gradient
     *                 from opaque black to opaque white).
     * @param  lower   The lower pixel value range, inclusive.
     * @param  upper   The upper pixel value range, inclusive.
     *
     * @throws IllegalArgumentException if <code>lower</code> is greater than <code>upper</code>.
     */
    public Category(final String name, final Color[] colors, final float lower, final float upper) throws IllegalArgumentException
    {this(name, colors, lower, upper, Float.NaN, Float.NaN, false);}

    /**
     * Construct a quantitative category for pixel values ranging from <code>lower</code>
     * to <code>upper</code>. Pixel values (usually integer) will be converted into
     * geophysics values (usually floating-point) through a linear equation.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. The array's length don't need to
     *                 be equals to <code>upper-lower</code>.  Colors will be interpolated
     *                 as needed. An array of length 1 means that an uniform color should be
     *                 used for all values range. An array of length 0 or a <code>null</code>
     *                 array means that a default color set should be used (usually a gradient
     *                 from opaque black to opaque white).
     * @param  lower   The lower pixel value range, inclusive.
     * @param  upper   The upper pixel value range, inclusive.
     * @param  C0      The C0 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * @param  C1      The C1 coefficient in the linear equation <code>y=C0+C1*x</code>.
     *
     * @throws IllegalArgumentException if <code>lower</code> is greater than <code>upper</code>,
     *         or if <code>C0</code> or <code>C1</code> coefficients are illegal.
     */
    public Category(final String name, final Color[] colors, final float lower, final float upper, final float C0, final float C1) throws IllegalArgumentException
    {this(name, colors, lower, upper, C0, C1, true);}

    /**
     * Construct a qualitative or quantitative category for pixel values
     * ranging from <code>lower</code> to <code>upper</code>. Pixel values
     * (usually integer) can be converted into geophysics values (usually
     * floating-point) through a linear equation.
     *
     * @param  name    The category name.
     * @param  colors  A set of colors for this category. The array's length don't need to
     *                 be equals to <code>upper-lower</code>.  Colors will be interpolated
     *                 as needed. An array of length 1 means that an uniform color should be
     *                 used for all values range. An array of length 0 or a <code>null</code>
     *                 array means that a default color set should be used (usually a gradient
     *                 from opaque black to opaque white).
     * @param  lower   The lower pixel value range, inclusive.
     * @param  upper   The upper pixel value range, inclusive.
     * @param  C0      The C0 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * @param  C1      The C1 coefficient in the linear equation <code>y=C0+C1*x</code>.
     * @param  isQuantitative <code>true</code> if this category is a quantitative one.
     *
     * @throws IllegalArgumentException if <code>lower</code> is greater than <code>upper</code>,
     *         or if <code>C0</code> or <code>C1</code> coefficients are illegal.
     */
    private Category(final String name, final Color[] colors, final float lower, final float upper, final float C0, final float C1, boolean isQuantitative) throws IllegalArgumentException
    {
        this.name  = name.trim();
        this.lower = lower;
        this.upper = upper;
        this.C0    = C0;
        this.C1    = C1;
        if (!(lower<=upper && !Float.isInfinite(lower) && !Float.isInfinite(upper)))
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
     * Returns the category name in the specified locale. If no name is available
     * for the specified locale, then an arbitrary locale may be used. The default
     * implementation returns the category name as specified to the constructor.
     */
    public String getName(final Locale locale)
    {return name;}

    /**
     * Returns the set of colors for this category.
     * Change to the returned array will not change this category.
     */
    public Color[] getColors()
    {
        final Color[] colors=new Color[ARGB.length];
        for (int i=0; i<colors.length; i++)
            colors[i] = new Color(ARGB[i], true);
        return colors;
    }

    /**
     * Compute the geophysics value from a pixel value. This method don't
     * need to check if <code>pixel</code> lies in this category's range
     * (<code>[{@link #lower}..{@link #upper}]</code>). Values out of range
     * may lead to extrapolation, which may or may not have a physical maining.
     */
    protected float toValue(final float pixel)
    {return C0+C1*pixel;}

    /**
     * Compute the pixel value from a geophysics value. If the resulting value is
     * outside this category's range (<code>[{@link #lower}..{@link #upper}]</code>),
     * then it will be clamp to <code>lower</code> or <code>upper</code> as necessary.
     * By convention, {@link Float#NaN} values are clamped to {@link #lower}.
     * This method never returns infinity or NaN.
     */
    protected float toPixel(final float value)
    {
        final float pixel = (value-C0)/C1;
        return (pixel>=lower) ? ((pixel<=upper) ? pixel : upper) : lower;
    }

    /**
     * Returns a hash value for this envelope.
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
}
