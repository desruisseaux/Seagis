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

// Images
import java.awt.Color;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.WritableRectIter;

// Miscellaneous
import javax.units.Unit;
import java.util.Locale;
import java.util.Arrays;
import java.io.Serializable;
import java.util.AbstractList;
import java.text.NumberFormat;
import java.text.FieldPosition;
import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * <FONT COLOR="#FF6633">A list of categories.</FONT> A category is a range of sample
 * values reserved for a structure or a geophysics parameter.  For example, an image
 * may use some sample values for clouds, lands and ices, and a range of sample values
 * for sea surface temperature. Such an image may be build of four categories: three
 * qualitative (clouds, lands and ices) and one quantitative (temperature in Celsius
 * degrees).  The ability to mix qualitative and quantitative categories in the same
 * sample dimension is an important feature for remote sensing in oceanography.
 * Unfortunately, many commercial GIS software lack this feature since they are mostly
 * designed for terrestrial use.
 * <br><br>
 * For space and performance raisons, images often store samples as integer values.
 * For such images, quantitative categories need an equation converting sample values
 * into geophysics values (e.g. temperature in Celsius degrees). Each category may
 * have his own equation. <code>CategoryList</code> is responsible for selecting the
 * right category from a sample value, and consequently the right transformation equation.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CategoryList extends AbstractList<Category> implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    // private static final long serialVersionUID = ?; // TODO

    /**
     * Liste des catégories constituant cet objet <code>CategoryList</code>.
     * Cette liste doit être en ordre croissant de {@link Category#lower},
     * c'est-à-dire classée selon le comparateur {@link CategoryComparator#BY_INDEX}.
     */
    private final Category[] byIndex;

    /**
     * Liste des thèmes constituant cet objet <code>IndexedThemeMapper</code>.
     * Cette liste doit être en ordre croissant de {@link Category#minimum},
     * c'est-à-dire classée selon le comparateur {@link CategoryComparator#BY_VALUES}.
     */
    private final Category[] byValues;

    /**
     * Tableau des index {@link Category#lower} ou {@link Category#minimum}
     * des tableaux {@link #byIndex} et {@link #byValues} respectivement.
     */
    private final float[] index, values;

    /**
     * Unités des mesures géophysiques représentées par les thèmes.
     * Ce champ peut être nul s'il ne s'applique pas ou si les
     * unités ne sont pas connues.
     */
    private final Unit unit;

    /**
     * Nombre de chiffres significatifs après la virgule.
     * Cette information est utilisée pour les écritures
     * des valeurs géophysiques des thèmes.
     */
    private final int ndigits;

    /**
     * Locale used for creating {@link #format}.
     * May be <code>null</code> if default locale
     * was requested.
     */
    private transient Locale locale;

    /**
     * Format à utiliser pour écrire les
     * valeurs géophysiques des thèmes.
     */
    private transient NumberFormat format;

    /**
     * Objet temporaire pour {@link NumberFormat}.
     */
    private transient FieldPosition dummy;

    /**
     * Modèle de couleurs suggéré pour l'affichage des catégories. Ce modèle de couleurs
     * sera construit à partir des couleurs qui ont été définies dans les différentes
     * catégories du tableau {@link #byIndex}.
     */
    private transient IndexColorModel colors;

    /**
     * Catégorie utilisée lors du dernier encodage ou décodage d'un pixel.  Avant de rechercher
     * la catégorie appropriée pour un nouveau encodage ou décodage, on vérifiera d'abord si la
     * catégorie désirée n'est pas la même que la dernière fois, c'est-à-dire {@link #lastCategory}.
     */
    private transient Category lastCategory;

    /**
     * Construct a category list for a sample dimension (band) with no unit.
     * This constructor is appropriate if the category list contains only
     * qualitative categories.
     *
     * @param  categories The category list.
     *
     * @throws IllegalArgumentException if two category ranges
     *         <code>[{@link Category#lower lower}..{@link Category#upper upper}]</code> overlap.
     */
    public CategoryList(final Category[] categories) throws IllegalArgumentException
    {this(categories, null, 0);}

    /**
     * Construct a category list for a sample dimension (band). This constructor
     * is appropriate if the category list contains at least one quantitative
     * category.
     *
     * @param  categories The category list.
     * @param  units      The unit information for this category list's dimension.
     *                    May be <code>null</code> is this dimension has no units.
     * @param  ndigits    Number of significant digits after the dot. This is used
     *                    when formatting quantity values. For example, if this
     *                    category list contains a category for temperature and
     *                    <code>ndigits</code> equals 2, then temperature values
     *                    may be formatted as "12.80°C". The exact formatting is
     *                    locale-dependent.
     *
     * @throws IllegalArgumentException If two category ranges
     *         <code>[{@link Category#lower lower}..{@link Category#upper upper}]</code> overlap.
     */
    public CategoryList(final Category[] categories, final Unit units, final int ndigits) throws IllegalArgumentException
    {
        this.unit     = units;
        this.ndigits  = ndigits;
        this.byIndex  = (Category[]) categories.clone();
        this.byValues = (Category[])    byIndex.clone();
        this.index    = CategoryComparator.BY_INDEX .sort(byIndex );
        this.values   = CategoryComparator.BY_VALUES.sort(byValues);
        /*
         * Vérifie que les thèmes ne se chevauchent pas.
         */
        for (int j=0; j<byIndex.length; j++)
        {
            final Category categ = byIndex[j];
            for (int i=j+1; i<byIndex.length; i++)
            {
                final Category check = byIndex[i];
                if (!(categ.lower>check.upper || categ.upper<check.lower)) // Do not accept NaN
                    throw new IllegalArgumentException(Resources.format(Clé.RANGE_OVERLAP¤4,
                                                       new Float(categ.lower), new Float(categ.upper),
                                                       new Float(check.lower), new Float(check.upper)));

                if (categ.minimum<=check.maximum && categ.maximum>=check.minimum) // Accept NaN
                    throw new IllegalArgumentException(Resources.format(Clé.RANGE_OVERLAP¤4,
                                                       new Float(categ.minimum), new Float(categ.maximum),
                                                       new Float(check.minimum), new Float(check.maximum)));
            }
        }
        assert(CategoryComparator.BY_INDEX .isSorted(byIndex ));
        assert(CategoryComparator.BY_VALUES.isSorted(byValues));
    }

    /**
     * Returns the number of categories in this list.
     */
    public int size()
    {return byIndex.length;}

    /**
     * Returns the category at the specified index.
     */
    public Category get(final int index)
    {return byIndex[index];}

    /**
     * Returns the unit information for quantitative categories in this list.
     * May returns <code>null</code>  if there is no quantitative categories
     * in this list, or if there is no unit information.
     */
    public Unit getUnits()
    {return unit;}

    /**
     * Returns a color model for this category list. The default implementation
     * build up the color model from each category's colors (as returned by
     * {@link Category#getColors}).
     */
    public synchronized IndexColorModel getColorModel()
    {
        if (colors==null)
        {
            if (byIndex.length==0)
            {
                // Construct a gray scale palette.
                final byte[] RGB = new byte[256];
                for (int i=0; i<RGB.length; i++) RGB[i] = (byte)i;
                colors = new IndexColorModel(8, RGB.length, RGB, RGB, RGB);
            }
            else
            {
                /*
                 * Calcule le nombre de couleurs de la palette
                 * en cherchant l'index le plus élevé des thèmes.
                 */
                assert(CategoryComparator.BY_INDEX.isSorted(byIndex));
                final int mapSize = Math.round(byIndex[byIndex.length-1].upper)+1;
                final int[]  ARGB = new int[mapSize];
                /*
                 * Interpole les codes de couleurs dans la palette. Les couleurs
                 * correspondantes aux plages non-définies par un thème seront transparentes.
                 */
                for (int i=0; i<byIndex.length; i++)
                {
                    final Category category = byIndex[i];
                    PaletteFactory.expand(category.getColors(), ARGB, Math.round(category.lower), Math.round(category.upper)+1);
                }
                colors = PaletteFactory.getIndexColorModel(ARGB);
            }
        }
        return colors;
    }

    /**
     * Retourne un thème à utiliser pour représenter les données manquantes.
     * Si aucun thème ne représente une valeur <code>NaN</code>, alors cette
     * méthode retourne un thème arbitraire.
     */
    final Category getBlank()
    {
        for (int i=0; i<byIndex.length; i++)
            if (Float.floatToRawIntBits(byIndex[i].minimum) == Float.floatToRawIntBits(Float.NaN))
                return byIndex[i];
        for (int i=0; i<byIndex.length; i++)
            if (Float.isNaN(byIndex[i].minimum))
                return byIndex[i];
        return Category.NODATA;
    }

    /**
     * Convertit une image de valeurs de pixels en image de nombres réels.
     *
     * @param image Image de valeurs de pixels. Les pixels de cette image
     *              doivent correspondre aux thèmes de <code>this</code>.
     * @return Image de nombres réels. Toutes les valeurs de cette image
     *         seront exprimées selon les unités {@link #getUnit}. Les
     *         pixels qui ne correspondent pas au paramètre géophysique
     *         auront une valeur <code>NaN</code>.
     */
    public RenderedImage toNumeric(final RenderedImage image)
    {
        if (image instanceof ImageAdapter)
        {
            final ImageAdapter adapter = (ImageAdapter) image;
            if (equals(adapter.categories))
                return adapter.getNumeric();
        }
        return new ImageDecoder(image, this);
    }

    /**
     * Convertit une image de nombres réels en valeurs de pixels.
     *
     * @param image Image de nombres réels. Toutes les valeurs de cette image
     *              doivent être exprimées selon les unités {@link #getUnit}.
     *              Les pixels qui ne correspondent pas au paramètre géophysique
     *              peuvent avoir une des valeurs <code>NaN</code>.
     * @return Image de valeurs de pixels. Les pixels de cette image
     *         correspondront aux thèmes de <code>this</code>.
     */
    public RenderedImage toThematic(final RenderedImage image)
    {
        if (image instanceof ImageAdapter)
        {
            final ImageAdapter adapter = (ImageAdapter) image;
            if (equals(adapter.categories))
                return adapter.getThematic();
        }
        return new ImageEncoder(image, this);
    }

    /**
     * Retourne la catégorie appropriée pour convertir la valeur du pixel
     * spécifié en valeur géophysique. Si aucune catégorie ne convient,
     * alors cette méthode retourne <code>null</code>.
     *
     * @param  sample Valeur à transformer.
     * @param  category Catégorie présumée du pixel, ou <code>null</code>.
     *         Il n'est pas nécessaire que cette information soit exacte,
     *         mais cette méthode sera plus rapide si elle l'est.
     * @return La catégorie du pixel, ou <code>null</code>.
     */
    final Category getDecoder(final float sample, Category category)
    {
        if (category==null || !(sample>=category.lower && sample<=category.upper)) // Le '!' est important à cause des NaN.
        {
            /*
             * Si la catégorie n'est pas la même que la dernière fois,
             * recherche à quelle autre catégorie pourrait appartenir le pixel.
             */
            int i=Arrays.binarySearch(index, sample);
            if ((i>=0 || (i=~i-1)>=0) && i<byIndex.length)
            {
                category=byIndex[i];
                assert(index[i]==category.lower);
                if (!(sample>=category.lower && sample<=category.upper))
                {
                    return null;
                }
            }
            else return null;
        }
        return category;
    }

    /**
     * Retourne la catégorie appropriée pour convertir la valeur géophysique
     * spécifiée en valeur de pixel. Si aucune catégorie ne convient,  alors
     * cette méthode retourne <code>null</code>.
     *
     * @param  value Valeur à transformer.
     * @param  category Catégorie présumée de la valeur, ou <code>null</code>.
     *         Il n'est pas nécessaire que cette information soit exacte, mais
     *         cette méthode sera plus rapide si elle l'est.
     * @return La catégorie de la valeur, ou <code>null</code>.
     */
    final Category getEncoder(final float value, Category category)
    {
        if (category==null || (!(value>=category.minimum && value<=category.maximum)) && // Le '!' est important à cause des NaN
                               Float.floatToRawIntBits(value)!=Float.floatToRawIntBits(category.minimum))
        {
            /*
             * Si la catégorie n'est pas la même que la dernière fois,
             * recherche à quelle catégorie pourrait appartenir la valeur.
             * Note: Les valeurs 'NaN' sont à la fin du tableau 'values'. Donc:
             *
             * 1) Si 'value' est réel, alors 'i' pointera forcément sur une catégorie de valeurs réelles.
             * 2) Si 'value' est NaN,  alors 'i' peut pointer sur une des catégories NaN ou sur la dernière
             *    catégorie de nombres réels.
             */
            int i=binarySearch(values, value); // Special 'binarySearch' for NaN values.
            if (i>=0)
            {
                category = byValues[i];
            }
            else
            {
                if (Float.isNaN(value))
                {
                    return null; // 'value' est un NaN inconnu.
                }
                i = ~i-1;
                if (i<0)
                {
                    if (byValues.length==0) return null;
                    category = byValues[0];
                    if (Float.isNaN(category.minimum)) return null;
                    // 'value' est inférieure à la plus petite valeur représentable.
                }
                else
                {
                    category = byValues[i];
                    if (value > category.maximum  &&  i+1 < byValues.length)
                    {
                        // Vérifie si la valeur ne serait pas plus proche du prochain thème.
                        final Category upper = byValues[i+1];
                        // assert: if 'upper.minimum' was smaller than 'value',
                        //         it should has been found by 'binarySearch'.
                        assert(upper.minimum > value);
                        if (upper.minimum-value < value-category.maximum)
                        {
                            category = upper;
                        }
                    }
                }
            }
        }
        // assert: after converting geophysics value to sample
        //         value, it should stay in the same category.
        assert(category==null || category==getDecoder(category.toSample(value), category));
        return category;
    }

    /**
     * Convert a sample value into a geophysics value.
     *
     * @param  sample The sample value, usually (but not always) an integer.
     * @return The geophysics value, in the units {@link #getUnits}. This
     *         value may be one of the many <code>NaN</code> values if the
     *         sample do not belong to a quantative category. Many
     *         <code>NaN</code> values are possibles, which make it possible
     *         to differenciate among many qualitative categories.
     *
     * @see Category#toValue
     */
    public float toValue(final float sample)
    {
        final Category category = getDecoder(sample, lastCategory);
        if (category!=null)
        {
            lastCategory = category;
            return category.toValue(sample);
        }
        return Float.NaN;
    }

    /**
     * Convert a geophysics value into a sample value. <code>value</code> must be
     * in the units {@link #getUnits}. If <code>value</code> is <code>NaN</code>,
     * then this method returns the sample value for one of the qualitative categories.
     * Many different <code>NaN</code> are alowed, which make it possible to differenciate
     * among many qualitative categories.
     *
     * @param  value The geophysics value (may be <code>NaN</code>). If this value is
     *               outside the allowed range of values, it will be clamped to the
     *               minimal or the maximal value.
     * @return The sample value.
     *
     * @see Category#toSample
     */
    public float toSample(final float value)
    {
        final Category category = getEncoder(value, lastCategory);
        if (category!=null)
        {
            lastCategory = category;
            return category.toSample(value);
        }
        return Float.NaN;
    }

    /**
     * Format a geophysics value. If <code>value</code> is a real number, then the value is
     * formatted with the appropriate number of digits and the units symbol.  Otherwise, if
     * <code>value</code> is <code>NaN</code>, then the category name is returned.
     *
     * @param  value  The geophysics value (may be <code>NaN</code>).
     * @param  locale Locale to use for formatting, or <code>null</code>
     *                for the default locale.
     * @return A string representation of the geophysics value.
     */
    public String format(final float value, final Locale locale)
    {
        if (Float.isNaN(value))
        {
            final Category category = getEncoder(value, lastCategory);
            if (category!=null)
            {
                lastCategory = category;
                return category.getName(locale);
            }
        }
        return format(value, true, locale, new StringBuffer()).toString();
    }

    /**
     * Convert a sample value into a geophysics value, and format it. If <code>sample</code>
     * belong to a quantitative category, then the value is formatted as a number with the
     * appropriate number of digits and the units symbol. Otherwise, if <code>sample</code>
     * belong to a qualitative category, then the category name is returned. Otherwise,
     * <code>null</code> is returned.
     *
     * @param  sample The sample value, usually (but not always) an integer.
     * @param  locale Locale to use for formatting, or <code>null</code>
     *                for the default locale.
     * @return A string representation of the geophysics value, or <code>null</code>
     *         if the sample don't belong to a known category.
     */
    final String formatConverted(final float sample, final Locale locale)
    {
        final Category category = getDecoder(sample, lastCategory);
        if (category!=null)
        {
            lastCategory = category;
            final float value = category.toValue(sample);
            if (Float.isNaN(value))
            {
                return category.getName(locale);
            }
            return format(value, true, locale, new StringBuffer()).toString();
        }
        else return null;
    }

    /**
     * Formatte la valeur spécifiée selon les conventions locales. Le nombre sera
     * écrit avec un nombre de chiffres après la virgule approprié pour cette catégorie.
     * Le symbole des unités sera ajouté après le nombre si <code>writeUnit</code>
     * est <code>true</code>.
     *
     * @param  value Valeur du paramètre géophysique à formatter.
     * @param  writeUnit Indique s'il faut écrire le symbole des unités après le nombre.
     *         Cet argument sera ignoré si aucune unité n'avait été spécifiée au constructeur.
     * @param  locale Conventions locales à utiliser, ou <code>null</code> pour les conventions par défaut.
     * @param  buffer Le buffer dans lequel écrire la valeur.
     * @return Le buffer <code>buffer</code> dans lequel auront été écrit la valeur et les unités.
     */
    private synchronized StringBuffer format(final float value, final boolean writeUnits, final Locale locale, StringBuffer buffer)
    {
        if (format==null || !XClass.equals(this.locale, locale))
        {
            this.locale = locale;
            format=(locale!=null) ? NumberFormat.getNumberInstance(locale) : NumberFormat.getNumberInstance();
            format.setMinimumFractionDigits(ndigits);
            format.setMaximumFractionDigits(ndigits);
            dummy=new FieldPosition(0);
        }
        buffer = format.format(value, buffer, dummy);
        if (writeUnits && unit!=null)
        {
            final int position = buffer.length();
            buffer.append('\u00A0'); // No-break space
            buffer.append(unit);
            if (buffer.length()==position+1)
            {
                buffer.setLength(position);
            }
        }
        return buffer;
    }

    /**
     * Returns a hash value for this category list.
     * This value need not remain consistent between
     * different implementations of the same class.
     */
    public int hashCode()
    {
        int code = 458783261;
        for (int i=0; i<byIndex.length; i++)
            code = code*37 + byIndex[i].hashCode();
        if (unit!=null)
            code = code*37 + unit.hashCode();
        return code;
    }

    /**
     * Compares the specified object with
     * this category list for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && object.getClass().equals(getClass()))
        {
            final CategoryList that = (CategoryList) object;
            if (this.ndigits == that.ndigits              &&
                XClass.equals(this.unit,    that.unit   ) &&
                Arrays.equals(this.byIndex, that.byIndex))
            {
                assert(Arrays.equals(this.byValues, that.byValues) &&
                       Arrays.equals(this.index,    that.index   ) &&
                       Arrays.equals(this.values,   that.values  ));
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a string representation of this category list.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes.
     */
    public String toString()
    {
        assert(CategoryComparator.BY_VALUES.isSorted(byValues));
        assert(CategoryComparator.BY_INDEX .isSorted(byIndex ));
        final float minimum;
        final float maximum;
        if (byValues.length!=0)
        {
            minimum = byValues[0                ].minimum;
            maximum = byValues[byValues.length-1].maximum;
        }
        else
        {
            minimum = Float.NaN;
            maximum = Float.NaN;
        }
        final String lineSeparator = System.getProperty("line.separator", "\n");
        StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        if (minimum < maximum)
        {
            buffer=format(minimum, false, null, buffer);
            buffer.append("..");
            buffer=format(maximum, true,  null, buffer);
        }
        else if (unit!=null)
        {
            buffer.append(unit);
        }
        buffer.append(']');
        buffer.append(lineSeparator);
        /*
         * Ecrit la liste des catégories en dessous.
         */
        for (int i=0; i<byIndex.length; i++)
        {
            buffer.append("    ");
            buffer.append(byIndex[i]);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }

    /**
     * Effectue une recherche bi-linéaire de la valeur spécifiée. Cette
     * méthode est identique à {@link Arrays#binarySearch(float[],float)},
     * excepté qu'elle distingue les différentes valeurs NaN.
     */
    private static int binarySearch(final float[] array, final float key)
    {
        int low  = 0;
        int high = array.length-1;
        while (low <= high)
        {
            final int mid = (low + high)/2;
            final float midVal = array[mid];

            final int cmp;
            if      (midVal < key) cmp = -1; // Neither val is NaN, thisVal is smaller
            else if (midVal > key) cmp = +1; // Neither val is NaN, thisVal is larger
            else
            {
                final int midBits = Float.floatToRawIntBits(midVal);
                final int keyBits = Float.floatToRawIntBits(key);
                cmp = (midBits == keyBits ?  0 :  // Values are equal
                      (midBits  < keyBits ? -1 :  // (-0.0, 0.0) or (!NaN, NaN)
                                            +1)); // (0.0, -0.0) or (NaN, !NaN)
            }

            if      (cmp < 0) low  = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
}
