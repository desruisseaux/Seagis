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
import net.seas.opengis.gc.GridCoverage;

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


/**
 * Wrap an {@link OperationDescriptor} for interoperability with
 * <A HREF="http://java.sun.com/products/java-media/jai/">Java Advanced Imaging</A>.
 * This class help to leverage the rich set of JAI operators in an OpenGIS framework.
 * <code>OperationJAI</code> inherits operation name and argument types  from {@link
 * OperationDescriptor}.    Source arguments will be set to the {@link GridCoverage}
 * type with name "Source" if there is only one source, or "Source1", "Source2", etc.
 * if there is many sources.
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
        final Class [] parentClasses  = parent.getParamClasses();
        final String[] parentNames    = parent.getParamNames();
        final Object[] parentDefaults = parent.getParamDefaults();

        final int    numSources = descriptor.getNumSources();
        final Class [] classes  = new Class [parentClasses .length+numSources];
        final String[] names    = new String[parentNames   .length+numSources];
        final Object[] defaults = new Object[parentDefaults.length+numSources];
        final Range[]    ranges = new Range [defaults.length];
        for (int i=0; i<ranges.length; i++)
        {
            if (i<numSources)
            {
                names  [i] = (numSources==1) ? "Source" : "Source"+(i+1);
                classes[i] = GridCoverage.class;
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
     * Apply a process operation to a grid coverage.
     *
     * @param  parameters List of name value pairs for the
     *         parameters required for the operation.
     * @return The result as a grid coverage.
     */
    protected GridCoverage doOperation(final ParameterList parameters)
    {
        final ParameterBlockJAI block = new ParameterBlockJAI(descriptor, RENDERED_MODE);
        final String[] names = parameters.getParameterListDescriptor().getParamNames();
        for (int i=0; i<names.length; i++)
        {
            final Object param = parameters.getObjectParameter(names[i]);
            if (param instanceof GridCoverage)
            {
                block.addSource(((GridCoverage)param).getRenderedImage(true));
            }
            else
            {
                block.setParameter(names[i], param);
            }
        }
        final RenderedImage image = JAI.create(descriptor.getName(), block);
        return null; // TODO
    }
}
