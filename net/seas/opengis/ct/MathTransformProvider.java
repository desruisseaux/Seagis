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

// OpenGIS (SEAS) dependencies
import net.seas.opengis.cs.Projection;
import net.seas.opengis.cs.Parameter;

// Miscellaneous
import java.util.Locale;
import net.seas.util.XClass;
import net.seas.resources.Resources;



/**
 * Base class for {@link MathTransform} providers. Instance of this class
 * allow the creation of transform objects from a classification name.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class MathTransformProvider
{
    /**
     * The classification name. This name do
     * not contains leading or trailing blanks.
     */
    public final String classification;

    /**
     * Resources key for a human readable name. This
     * is used for {@link #getName} implementation.
     */
    private final int nameKey;

    /**
     * Construct a new provider.
     *
     * @param classification The classification name.
     * @param nameKey Resources key for a human readable name.
     *        This is used for {@link #getName} implementation.
     */
    protected MathTransformProvider(final String classification, final int nameKey)
    {
        this.classification = classification.trim();
        this.nameKey        = nameKey;
    }

    /**
     * Returns a human readable name localized for the specified locale.
     * If no name is available for the specified locale, this method may
     * returns a name in an arbitrary locale.
     */
    public final String getName(final Locale locale)
    {return Resources.getResources(locale).getString(nameKey);}

    /**
     * Returns a set of default parameters. The returns array should contains
     * one element for every parameter supported by the registered transform.
     *
     * @return A set of default parameters.
     */
    public abstract Parameter[] getDefaultParameters();

    /**
     * Returns an objet for the specified parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @return A {@link MathTransform} object of this classification.
     */
    public abstract MathTransform create(final Parameter[] parameters);

    /**
     * Returns a string representation for this provider.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getName(null)+']';}
}
