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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.resources;

// Parameters
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;

// Collections
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.media.jai.util.CaselessStringKey;

// Resources
import net.seagis.resources.Utilities;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Methods for binding names to {@link ParameterListDescriptor}s. For example,
 * {@link net.seagis.cs.Projection} using this class for binding classification
 * name to parameter list descriptors.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Naming
{
    /**
     * The naming to use for mapping projection's
     * classification name to parameter descriptor.
     */
    public static final Naming PROJECTIONS = new Naming("net.seagis.ct.MathTransformFactory", "net.seagis.css");

    /**
     * Map classification name to {@link ParameterListDescriptor}
     * objects. Keys are {@link CaselessStringKey} object, while
     * values are {@link ParameterListDescriptor} objects.
     */
    private Map descriptors;

    /**
     * The fully qualified name of the class to load for initializing binding,
     * or <code>null</code> if none. If non-null, then the static initializer
     * of this class should invokes {@link #bind} for binding a default set of
     * descriptors.
     */
    private final String initializer;

    /**
     * The logger to use if initialization failed.
     */
    private final String logger;

    /**
     * Construct a <code>Naming</code> object.
     *
     * @param initializer The fully qualified name of the class
     *                    to load for initializing binding.
     * @param logger The logger to use if initialization failed.
     */
    private Naming(final String initializer, final String logger)
    {
        this.initializer = initializer;
        this.logger      = logger;
    }

    /**
     * Try to bind a set of default projections. Those default projections are binded
     * during the static initialization of {net.seagis.ct.MathTransformFactory} class.
     * If the operation fail, a warning is logged but the process continue.
     */
    private void bindDefaults(final String method)
    {
        try
        {
            Class.forName(initializer);
        }
        catch (ClassNotFoundException exception)
        {
            Utilities.unexpectedException(logger, "Naming", method, exception);
        }
    }

    /**
     * Binds a classification name to a parameter list descriptor.
     *
     * @param  classification The classification name.
     * @param  descriptor the parameter list descriptor.
     * @throws IllegalArgumentException if a descriptor is already
     *         bounds for the specified classification name.
     */
    public synchronized void bind(final String classification, final ParameterListDescriptor descriptor) throws IllegalArgumentException
    {
        if (descriptors==null)
        {
            descriptors = new HashMap();
            bindDefaults("bind");
        }
        final CaselessStringKey key = new CaselessStringKey(classification);
        if (descriptors.containsKey(key))
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.PROJECTION_ALREADY_BOUNDS_$1, classification));
        }
        descriptors.put(key, descriptor);
    }

    /**
     * Returns a default parameter descriptor
     * for the specified classification name,
     * or <code>null</code> if none is found.
     *
     * @param  classification The classification to look for.
     * @return The descriptor for the specified classification,
     *         or <code>null</code> if none.
     */
    public synchronized ParameterListDescriptor lookup(final String classification)
    {
        if (descriptors==null)
        {
            descriptors = new HashMap();
            bindDefaults("lookup");
        }
        return (ParameterListDescriptor) descriptors.get(new CaselessStringKey(classification));
    }

    /**
     * Returns a parameter list for the specified classification. If
     * there is no explicit parameter descriptor for the specified
     * classification, then a default descriptor is used.
     *
     * @param  classification The classification to look for.
     * @param  fallback The default parameter list descriptor to use if no
     *         descriptor has been found for the specified classification.
     * @return A parameter list to use for the specified classification
     */
    public ParameterList getParameterList(final String classification, final ParameterListDescriptor fallback)
    {
        ParameterListDescriptor descriptor = lookup(classification);
        if (descriptor==null)
        {
            descriptor = fallback;
        }
        return new ParameterListImpl(descriptor);
    }

    /**
     * Returns the list of classification names.
     */
    public synchronized String[] list()
    {
        if (descriptors==null)
        {
            descriptors = new HashMap();
            bindDefaults("list");
        }
        int count = 0;
        final String[] names = new String[descriptors.size()];
        for (final Iterator it=descriptors.keySet().iterator(); it.hasNext();)
        {
            names[count++] = it.next().toString();
        }
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert count == names.length;
------- END OF JDK 1.4 DEPENDENCIES ---*/
        return names;
    }
}
