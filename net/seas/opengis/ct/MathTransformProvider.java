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
import net.seas.opengis.pt.Longitude;
import net.seas.opengis.pt.Latitude;

// Parameters
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.ParameterListDescriptorImpl;

// Miscellaneous
import java.util.Locale;
import java.util.Arrays;
import net.seas.util.XClass;
import net.seas.util.XArray;
import net.seas.resources.Resources;



/**
 * <FONT COLOR="#FF6633">Base class for {@link MathTransform} providers.
 * Instance of this class allow the creation of transform objects from a
 * classification name.</FONT>
 * <br><br>
 * <strong>Note: this class is not part of OpenGIS specification and
 * may change in a future version. Do not rely on it.</strong>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class MathTransformProvider
{
    /**
     * The zero value.
     */
    private static final Double ZERO = new Double(0);

    /**
     * Range of positives values. Range goes
     * from 0 exclusive to positive infinity.
     */
    protected static final Range POSITIVE_RANGE = new Range(Double.class, ZERO, false, null, false);

    /**
     * Range of longitude values. Range goes
     * from -180° to +180° inclusives.
     */
    protected static final Range LONGITUDE_RANGE = new Range(Double.class, new Double(Longitude.MIN_VALUE), true, new Double(Longitude.MAX_VALUE), true);

    /**
     * Range of latitude values. Range goes
     * from -90° to +90° inclusives.
     */
    protected static final Range LATITUDE_RANGE = new Range(Double.class, new Double(Latitude.MIN_VALUE), true, new Double(Latitude.MAX_VALUE), true);

    /**
     * Nombre d'éléments par ligne dans le tableau {@link #properties}.
     */
    private static final int RECORD_LENGTH = 4;

    /**
     * A default parameter list descriptor for map projections. This descriptor declare
     * "semi_major", "semi_minor", "central_meridian" and "latitude_of_origin" parameters.
     */
    public static final ParameterListDescriptor DEFAULT_PROJECTION_DESCRIPTOR = getDescriptor(new Object[]
    {
        "semi_major",          Double.class, ParameterListDescriptor.NO_PARAMETER_DEFAULT, POSITIVE_RANGE,
        "semi_minor",          Double.class, ParameterListDescriptor.NO_PARAMETER_DEFAULT, POSITIVE_RANGE,
        "central_meridian",    Double.class, ZERO,                                         LONGITUDE_RANGE,
        "latitude_of_origin",  Double.class, ZERO,                                         LATITUDE_RANGE
    });

    /**
     * The set parameters to use for {@link ParameterListDescriptor} construction,
     * or <code>null</code> if the descriptor is already constructed.
     */
    private Object[] properties;

    /**
     * The parameter list descriptor. This object will
     * be constructed only the first time it is needed.
     */
    private ParameterListDescriptor descriptor;

    /**
     * The classification name. This name do
     * not contains leading or trailing blanks.
     */
    private final String classification;

    /**
     * Resources key for a human readable name. This
     * is used for {@link #getName} implementation.
     */
    private final int nameKey;

    /**
     * Construct a new provider.
     *
     * @param classification The classification name.
     * @param inherit The parameter list descriptor to inherit from, or <code>null</code>
     *        if there is none. All parameter descriptions from <code>inherit</code> will
     *        be copied into this newly created <code>MathTransformProvider</code>.   For
     *        map projections, this argument may be {@link #DEFAULT_PROJECTION_DESCRIPTOR}.
     *        Subclasses may add or change parameters in their constructor by invoking
     *        {@link #put}.
     */
    protected MathTransformProvider(final String classification, final ParameterListDescriptor inherit)
    {this(classification, 0, inherit);}

    /**
     * Construct a new provider.
     *
     * @param classification The classification name.
     * @param nameKey Resources key for a human readable name.
     *        This is used for {@link #getName} implementation.
     * @param inherit The parameter list descriptor to inherit from, or <code>null</code>
     *        if there is none. All parameter descriptions from <code>inherit</code> will
     *        be copied into this newly created <code>MathTransformProvider</code>.   For
     *        map projections, this argument may be {@link #DEFAULT_PROJECTION_DESCRIPTOR}.
     *        Subclasses may add or change parameters in their constructor by invoking
     *        {@link #put}.
     */
    MathTransformProvider(final String classification, final int nameKey, final ParameterListDescriptor inherit)
    {
        this.classification = classification.trim();
        this.nameKey        = nameKey;
        if (inherit!=null)
        {
            final String[]    names = inherit.getParamNames();
            final Class []  classes = inherit.getParamClasses();
            final Object[] defaults = inherit.getParamDefaults();
            properties = new Object[names.length*RECORD_LENGTH];
            for (int i=0; i<names.length; i++)
            {
                final int j=i*RECORD_LENGTH;
                properties[j+0] = names   [i];
                properties[j+1] = classes [i];
                properties[j+2] = defaults[i];
                properties[j+3] = inherit.getParamValueRange(names[i]);
            }
        }
        else properties = new Object[0];
    }

    /**
     * Add or change a parameter to this math transform provider. Default values are provided
     * for "semi_major", "semi_minor", "central_meridian" and "latitude_of_origin". Subclasses
     * may call this method in their constructor for adding or changing parameters.
     *
     * @param parameter    The parameter name.
     * @param defaultValue The default value for this parameter, or {@link Double#NaN} if there is none.
     * @param range        The range of legal values. May be one of the predefined constants
     *                     ({@link #POSITIVE_RANGE}, {@link #LONGITUDE_RANGE}, {@link #LATITUDE_RANGE})
     *                     or any other {@link Range} object. May be <code>null</code> if all values
     *                     are valid for this parameter.
     */
    protected synchronized final void put(String parameter, final double defaultValue, final Range range)
    {
        if (properties==null)
        {
            // Construction is finished.
            throw new IllegalStateException();
        }
        parameter = parameter.trim();
        final int end = properties.length;
        for (int i=0; i<end; i+=RECORD_LENGTH)
        {
            if (parameter.equalsIgnoreCase(properties[i].toString()))
            {
                properties[i+0] = parameter;
                properties[i+1] = Double.class;
                properties[i+2] = wrap(defaultValue);
                properties[i+3] = range;
                return;
            }
        }
        properties = XArray.resize(properties, end+RECORD_LENGTH);
        properties[end+0] = parameter;
        properties[end+1] = Double.class;
        properties[end+2] = wrap(defaultValue);
        properties[end+3] = range;
    }

    /**
     * Wrap the specified double value in an object.
     */
    private static Object wrap(final double value)
    {
        if (Double.isNaN(value)) return ParameterListDescriptor.NO_PARAMETER_DEFAULT;
        if (value ==  Latitude.MIN_VALUE) return  LATITUDE_RANGE.getMinValue();
        if (value ==  Latitude.MAX_VALUE) return  LATITUDE_RANGE.getMaxValue();
        if (value == Longitude.MIN_VALUE) return LONGITUDE_RANGE.getMinValue();
        if (value == Longitude.MAX_VALUE) return LONGITUDE_RANGE.getMaxValue();
        if (value == 0)                   return ZERO;
        return new Double(value);
    }

    /**
     * Returns the parameter list descriptor for the specified properties list.
     */
    private static ParameterListDescriptor getDescriptor(final Object[] properties)
    {
        final String[]    names = new String[properties.length/RECORD_LENGTH];
        final Class []  classes = new Class [names.length];
        final Object[] defaults = new Object[names.length];
        final Range []   ranges = new Range [names.length];
        for (int i=0; i<names.length; i++)
        {
            final int j = i*RECORD_LENGTH;
            names   [i] = (String)properties[j+0];
            classes [i] =  (Class)properties[j+1];
            defaults[i] = (Object)properties[j+2];
            ranges  [i] =  (Range)properties[j+3];
        }
        return new ParameterListDescriptorImpl(null, names, classes, defaults, ranges);
    }

    /**
     * Returns the classification name.
     */
    public String getClassName()
    {return classification;}

    /**
     * Returns a human readable name localized for the specified locale.
     * If no name is available for the specified locale, this method may
     * returns a name in an arbitrary locale.
     */
    public String getName(final Locale locale)
    {return (nameKey!=0) ? Resources.getResources(locale).getString(nameKey) : getClassName();}

    /**
     * Returns a newly created parameter list. The set of parameter
     * depend of the transform this provider is for. Parameters may
     * have default values and a range of validity.
     */
    public synchronized ParameterList getParameterList()
    {
        if (descriptor==null)
        {
            descriptor = getDescriptor(properties);
            properties = null; // No longer needed.
        }
        return new ParameterListImpl(descriptor);
    }

    /**
     * Returns a transform for the specified parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @return A {@link MathTransform} object of this classification.
     */
    public abstract MathTransform create(final ParameterList parameters);

    /**
     * Returns a string representation for this provider.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getName(null)+']';}
}
