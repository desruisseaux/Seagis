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
package net.seas.opengis.gp;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.cs.CoordinateSystem;

// Java Advanced Imaging
import javax.media.jai.JAI;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.ParameterListDescriptorImpl;

// Image (Java2D)
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

// Miscellaneous
import net.seas.resources.Resources;


/**
 * Wrap an {@link OperationDescriptor} for interoperability with
 * <A HREF="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</A>.
 * This class help to leverage the rich set of JAI operators in an OpenGIS framework.
 * <code>OperationJAI</code> inherits operation name and argument types  from {@link
 * OperationDescriptor}, except source argument type which is set to {@link GridCoverage}.
 * If there is only one source argument, il will be renamed "Source" for better compliance
 * to OpenGIS usage.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class OperationJAI extends Operation
{
    /**
     * The rendered mode for JAI operation.
     */
    private static final String RENDERED_MODE = "rendered";

    /**
     * The operation descriptor.
     */
    protected final OperationDescriptor descriptor;

    /**
     * Construct an OpenGIS operation from a JAI operation. This convenience constructor
     * fetch the {@link OperationDescriptor} from the specified operation name using the
     * default {@link JAI} instance.
     *
     * @param operationName JAI operation name (e.g. "GradientMagnitude").
     */
    public OperationJAI(final String operationName)
    {this((OperationDescriptor) JAI.getDefaultInstance().getOperationRegistry().getDescriptor(RENDERED_MODE, operationName));}

    /**
     * Construct an OpenGIS operation from a JAI operation.
     *
     * @param descriptor The operation descriptor. This descriptor
     *        must supports the "rendered" mode (which is the case
     *        for most JAI operations).
     */
    public OperationJAI(final OperationDescriptor descriptor)
    {
        super(descriptor.getName(), getParameterListDescriptor(descriptor));
        this.descriptor = descriptor;
    }

    /**
     * Vérifie que la classe spécifiée implémente l'interface {@link RenderedImage}.
     * Cette méthode est utilisée pour vérifier les classes des images sources et
     * destinations.
     */
    private static final void ensureValid(final Class classe) throws IllegalArgumentException
    {
        if (!RenderedImage.class.isAssignableFrom(classe))
        {
            throw new IllegalArgumentException(classe.getName());
        }
    }

    /**
     * Gets the parameter list descriptor for an operation descriptor.
     * {@link OperationDescriptor} parameter list do not include sources.
     * This method will add them in front of the parameter list.
     */
    private static ParameterListDescriptor getParameterListDescriptor(final OperationDescriptor descriptor)
    {
        ensureValid(descriptor.getDestClass(RENDERED_MODE));
        final Class[] sourceClasses = descriptor.getSourceClasses(RENDERED_MODE);
        for (int i=0; i<sourceClasses.length; i++)
            ensureValid(sourceClasses[i]);

        final ParameterListDescriptor parent = descriptor.getParameterListDescriptor(RENDERED_MODE);
        final String[] sourceNames    = getSourceNames(descriptor);
        final String[] parentNames    = parent.getParamNames();
        final Class [] parentClasses  = parent.getParamClasses();
        final Object[] parentDefaults = parent.getParamDefaults();

        final int    numSources = descriptor.getNumSources();
        final String[]    names = new String[parentNames   .length+numSources];
        final Class []  classes = new Class [parentClasses .length+numSources];
        final Object[] defaults = new Object[parentDefaults.length+numSources];
        final Range[]    ranges = new Range [defaults.length];
        for (int i=0; i<ranges.length; i++)
        {
            if (i<numSources)
            {
                names   [i] = sourceNames[i];
                classes [i] = GridCoverage.class;
                defaults[i] = ParameterListDescriptor.NO_PARAMETER_DEFAULT;
            }
            else
            {
                names   [i] = parentNames   [i-numSources];
                classes [i] = parentClasses [i-numSources];
                defaults[i] = parentDefaults[i-numSources];
                ranges  [i] = parent.getParamValueRange(names[i]);
            }
        }
        return new ParameterListDescriptorImpl(null, names, classes, defaults, ranges);
    }

    /**
     * Returns source name for the specified descriptor. If the descriptor has
     * only one source,  it will be renamed "Source" for better conformance to
     * to OpenGIS usage.
     */
    private static String[] getSourceNames(final OperationDescriptor descriptor)
    {
        if (descriptor.getNumSources()==1)
        {
            return new String[] {"Source"};
        }
        else return descriptor.getSourceNames();
    }

    /**
     * Check if array <code>names</code> contains the element <code>name</code>.
     * Search is done in case-insensitive manner. This method is efficient enough
     * if <code>names</code> is very short (less than 10 entries).
     */
    private static boolean contains(final String[] names, final String name)
    {
        for (int i=0; i<names.length; i++)
            if (name.equalsIgnoreCase(names[i]))
                return true;
        return false;
    }

    /**
     * Apply a process operation to a grid coverage. The default
     * implementation separate sources from parameters and invokes
     * {@link #doOperation(GridCoverage[], ParameterBlockJAI)}.
     *
     * @param  parameters List of name value pairs for the
     *         parameters required for the operation.
     * @return The result as a grid coverage.
     */
    protected GridCoverage doOperation(final ParameterList parameters)
    {
        final ParameterBlockJAI block = new ParameterBlockJAI(descriptor, RENDERED_MODE);
        final String[]     paramNames = parameters.getParameterListDescriptor().getParamNames();
        final String[]    sourceNames = getSourceNames(descriptor);
        final GridCoverage[]  sources = new GridCoverage[descriptor.getNumSources()];
        for (int srcCount=0,i=0; i<paramNames.length; i++)
        {
            final String name  = paramNames[i];
            final Object param = parameters.getObjectParameter(name);
            if (contains(sourceNames, name))
            {
                GridCoverage source = (GridCoverage) param;
                block.addSource(source.getRenderedImage(true));
                sources[srcCount++] = source;
            }
            else
            {
                block.setParameter(name, param);
            }
        }
        return doOperation(sources, block);
    }

    /**
     * Apply a JAI operation to a grid coverage. The default implementation checks if
     * all sources use the same coordinate system and have the same envelope, and then
     * apply the operation using the following line:
     *
     * <blockquote><pre>
     * {@link JAI#create(String,ParameterBlock) JAI.create}({@link #descriptor}.getName(),&nbsp;parameters)
     * </pre></blockquote>
     *
     * @param  sources The source coverages.
     * @param  parameters List of name value pairs for the
     *         parameters required for the operation.
     * @return The result as a grid coverage.
     */
    protected GridCoverage doOperation(final GridCoverage[] sources, final ParameterBlockJAI parameters)
    {
        final GridCoverage source = sources[0];
        final CoordinateSystem cs = source.getCoordinateSystem();
        final Envelope   envelope = source.getEnvelope();
        for (int i=1; i<sources.length; i++)
        {
            if (!cs.equivalents(sources[i].getCoordinateSystem()))
                throw new IllegalArgumentException(Resources.format(Clé.INCOMPATIBLE_COORDINATE_SYSTEM));
            if (!envelope.equals(sources[i].getEnvelope()))
                throw new IllegalArgumentException(Resources.format(Clé.ENVELOPE_MISMATCH));
        }
        RenderedImage data = JAI.create(descriptor.getName(), parameters);
        return new GridCoverage(source.getName(null), // The grid coverage name
                                data,                 // The underlying data
                                cs,                   // The coordinate system.
                                envelope,             // The coverage envelope.
                                null,                 // The category lists
                                true,                 // Data are geophysics values.
                                sources,              // The source grid coverages.
                                null);                // Properties
    }
}
