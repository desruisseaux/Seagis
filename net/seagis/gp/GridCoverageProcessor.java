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
package net.seagis.gp;

// OpenGIS dependencies (SEAGIS)
import net.seagis.gc.GridCoverage;
import net.seagis.ct.CoordinateTransformationFactory;

// Collections
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.AbstractSet;

// Parameters
import javax.media.jai.Interpolation;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.util.CaselessStringKey;

// Input/output
import java.io.Writer;
import java.io.IOException;

// Miscellaneous
import java.util.Arrays;
import net.seagis.resources.gcs.Resources;
import net.seagis.resources.gcs.ResourceKeys;


/**
 * Allows for different ways of accessing the grid coverage values.
 * Using one of these operations to change the way the grid is being
 * accessed will not affect the state of the grid coverage controlled
 * by another operations. For example, changing the interpolation method
 * should not affect the number of sample dimensions currently being
 * accessed or value sequence.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 */
public class GridCoverageProcessor
{
    /**
     * The default grid coverage processor. Will
     * be constructed only when first requested.
     */
    private static GridCoverageProcessor DEFAULT;

    /**
     * The set of operation for this grid coverage processor.
     * Keys are operation's name. Values are operations and
     * should not contains duplicated values.
     * <br><br>
     * Generic-Type: <CaselessStringKey,Operation>
     */
    private final Map operations = new HashMap();

    /**
     * Construct a grid coverage processor with no operation.
     * Operation can be added by invoking {@link #addOperation}.
     */
    protected GridCoverageProcessor()
    {}

    /**
     * Construct a grid coverage processor with the same
     * set of operations than the specified processor.
     */
    protected GridCoverageProcessor(final GridCoverageProcessor processor)
    {
        operations.putAll(processor.operations);
    }

    /**
     * Returns the default grid coverage processor.
     */
    public static synchronized GridCoverageProcessor getDefault()
    {
        if (DEFAULT==null)
        {
            final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();
            DEFAULT = new GridCoverageProcessor();
            DEFAULT.addOperation(new Interpolator.Operation());
            DEFAULT.addOperation(new Resampler.Operation(factory));
            DEFAULT.addOperation(new OperationJAI("GradientMagnitude"));
            DEFAULT.addOperation(new OperationJAI("Rescale"));
        }
        return DEFAULT;
    }

    /**
     * Add the specified operation to this processor. This method is usually invoked
     * at construction time <strong>before</strong> this processor is made accessible.
     * Once accessible, all <code>GridCoverageProcessor</code> instances should be
     * immutable.
     *
     * @param operation The operation to add.
     * @param IllegalStateException if an operation already exists
     *        with the same name than <code>operation</code>.
     */
    protected synchronized void addOperation(final Operation operation) throws IllegalStateException
    {
        final CaselessStringKey name = new CaselessStringKey(operation.getName());
        if (!operations.containsKey(name))
        {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
            assert !operations.containsValue(operation);
------- END OF JDK 1.4 DEPENDENCIES ---*/
            operations.put(name, operation);
        }
        else throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_OPERATION_ALREADY_BOUND_$1, operation.getName()));
    }

    /**
     * Retrieve grid processing operation informations. The operation information
     * will contain the name of the operation as well as a list of its parameters.
     */
    public synchronized Operation[] getOperations()
    {return (Operation[]) operations.values().toArray(new Operation[operations.size()]);}

    /**
     * Returns the operation for the specified name.
     *
     * @param  operationName Name of the operation.
     * @return The operation for the given name.
     * @throws OperationNotFoundException if there is no operation for the specified name.
     */
    public Operation getOperation(final String name) throws OperationNotFoundException
    {
        final Operation operation = (Operation) operations.get(new CaselessStringKey(name));
        if (operation!=null)
        {
            return operation;
        }
        else throw new OperationNotFoundException(Resources.format(ResourceKeys.ERROR_OPERATION_NOT_FOUND_$1, name));
    }

    /**
     * Apply a process operation to a grid coverage with default parameters. This
     * is a convenience method for {@link #doOperation(Operation,ParameterList)}.
     *
     * @param  operationName Name of the operation to be applied to the grid coverage..
     * @param  source The source grid coverage.
     * @return The result as a grid coverage.
     * @throws OperationNotFoundException if there is no operation named <code>operationName</code>.
     */
    public GridCoverage doOperation(final String operationName, final GridCoverage source) throws OperationNotFoundException
    {
        final Operation operation = getOperation(operationName);
        return doOperation(operation, operation.getParameterList().setParameter("Source", source));
    }

    /**
     * Apply a process operation to a grid coverage.
     *
     * @param  operationName Name of the operation to be applied to the grid coverage.
     * @param  parameters List of name value pairs for the parameters required for the operation.
     *         The easiest way to construct this list is to invoke <code>{@link #getOperation
     *         getOperation}(name).{@link Operation#getParameterList getParameterList}()</code>
     *         and to modify the returned list.
     * @return The result as a grid coverage.
     * @throws OperationNotFoundException if there is no operation named <code>operationName</code>.
     */
    public GridCoverage doOperation(final String operationName, final ParameterList parameters) throws OperationNotFoundException
    {return doOperation(getOperation(operationName), parameters);}

    /**
     * Apply a process operation to a grid coverage. Default implementation
     * checks if source coverages use an interpolation,    and then invokes
     * {@link Operation#doOperation}. If all source coverages used the same
     * interpolation, the same interpolation is applied to the resulting
     * coverage (except if the resulting coverage has already an interpolation).
     *
     * @param  operation The operation to be applied to the grid coverage.
     * @param  parameters List of name value pairs for the parameters required for
     *         the operation.  The easiest way to construct this list is to invoke
     *         <code>operation.{@link Operation#getParameterList getParameterList}()</code>
     *         and to modify the returned list.
     * @return The result as a grid coverage.
     */
    public GridCoverage doOperation(final Operation operation, final ParameterList parameters)
    {
        Interpolation[] interpolations = null;
        final String[] paramNames = parameters.getParameterListDescriptor().getParamNames();
        for (int i=0; i<paramNames.length; i++)
        {
            final Object param = parameters.getObjectParameter(paramNames[i]);
            if (param instanceof Interpolator)
            {
                // If all sources use the same interpolation,  preserve the
                // interpolation for the resulting coverage. Otherwise, use
                // the default interpolation (nearest neighbor).
                final Interpolation[] interp = ((Interpolator) param).getInterpolations();
                if (interpolations!=null)
                {
                    if (!Arrays.equals(interpolations, interp))
                    {
                        // Set to no interpolation.
                        interpolations = new Interpolation[0];
                    }
                }
                else interpolations = interp;
            }
        }
        GridCoverage coverage = operation.doOperation(parameters);
        if (interpolations!=null && coverage!=null && !(coverage instanceof Interpolator))
        {
            coverage = Interpolator.create(coverage, interpolations);
        }
        return coverage;
    }

    /**
     * Print a description of all operations to the specified stream.
     * The description include operation names and lists of parameters.
     *
     * @param  out The destination stream.
     * @throws IOException if an error occured will writing to the stream.
     */
    public void print(final Writer out) throws IOException
    {
        final String  lineSeparator = System.getProperty("line.separator", "\n");
        final Operation[] operations = getOperations();
        for (int i=0; i<operations.length; i++)
        {
            out.write(lineSeparator);
            operations[i].print(out);
        }
    }
}
