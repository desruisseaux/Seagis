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
package net.seas.opengis.ct;

// Miscellaneous
import java.util.Locale;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import javax.media.jai.EnumeratedParameter;
import net.seas.resources.Resources;
import net.seas.resources.Clé;


/**
 * Semantic type of transform used in coordinate transformation.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_TransformType
 */
public final class TransformType extends EnumeratedParameter
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -4024711027359151152L;

    /**
     * Unknown or unspecified type of transform.
     */
    public static final TransformType OTHER = new TransformType("OTHER", 0, Clé.OTHER);

    /**
     * Transform depends only on defined parameters.
     * For example, a cartographic projection.
     */
    public static final TransformType CONVERSION = new TransformType("CONVERSION", 1, Clé.CONVERSION);

    /**
     * Transform depends only on empirically derived parameters.
     * For example a datum transformation.
     */
    public static final TransformType TRANSFORMATION = new TransformType("TRANSFORMATION", 2, Clé.TRANSFORMATION);

    /**
     * Transform depends on both defined and empirical parameters.
     */
    public static final TransformType CONVERSION_AND_TRANSFORMATION = new TransformType("CONVERSION_AND_TRANSFORMATION", 3, Clé.CONVERSION_AND_TRANSFORMATION);

    /**
     * Transform types by value. Used to
     * canonicalize after deserialization.
     */
    private static final TransformType[] ENUMS =
    {
        OTHER,
        CONVERSION,
        TRANSFORMATION,
        CONVERSION_AND_TRANSFORMATION
    };

    /**
     * Resource key, used for building localized name. This key doesn't need to
     * be serialized, since {@link #readResolve} canonicalize enums according their
     * {@link #value}. Furthermore, its value is implementation-dependent (which is
     * an other raison why it should not be serialized).
     */
    private transient final int clé;

    /**
     * Construct a new enum value.
     */
    private TransformType(final String name, final int value, final int clé)
    {
        super(name, value);
        this.clé = clé;
    }

    /**
     * <FONT COLOR="#FF6633">Return the enum for the specified value.
     * This method is provided for compatibility with
     * {@link org.opengis.ct.CT_TransformType}.</FONT>
     *
     * @param  value The enum value.
     * @return The enum for the specified value.
     * @throws NoSuchElementException if there is no enum for the specified value.
     */
    public static TransformType getEnum(final int value) throws NoSuchElementException
    {
        if (value>=1 && value<ENUMS.length) return ENUMS[value];
        throw new NoSuchElementException(String.valueOf(value));
    }

    /**
     * <FONT COLOR="#FF6633">Returns this enum's name in the specified locale.
     * If no name is available for the specified locale, a default one will be
     * used.</FONT>
     *
     * @param  locale The locale, or <code>null</code> for the current default locale.
     * @return Enum's name in the specified locale.
     */
    public String getName(final Locale locale)
    {return Resources.getResources(locale).getString(clé);}

    /**
     * Use a single instance of {@link TransformType} after deserialization.
     * It allow client code to test <code>enum1==enum2</code> instead of
     * <code>enum1.equals(enum2)</code>.
     *
     * @return A single instance of this enum.
     * @throws ObjectStreamException is deserialization failed.
     */
    private Object readResolve() throws ObjectStreamException
    {
        final int value = getValue();
        if (value>=0 && value<ENUMS.length) return ENUMS[value]; // Canonicalize
        else return ENUMS[0]; // Collapse unknow value to a single canonical one
    }
}
