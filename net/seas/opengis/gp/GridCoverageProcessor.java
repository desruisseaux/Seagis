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

// Dependencies
import net.seas.opengis.gc.GridCoverage;

// Miscellaneous
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.NoSuchElementException;
import javax.media.jai.ParameterList;
import javax.media.jai.util.CaselessStringKey;


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
     */
    private final Map<CaselessStringKey,Operation> operations = new HashMap<CaselessStringKey,Operation>();

    /**
     * An immutable view of operations. This set assume that
     * {@link #operations} do not contains duplicated values.
     */
    private final Set<Operation> setView = Collections.unmodifiableSet(new SetView<Operation>(operations.values()));

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
    {operations.putAll(processor.operations);}

    /**
     * Returns the default grid coverage processor.
     */
    public static synchronized GridCoverageProcessor getDefault()
    {
        if (DEFAULT==null)
        {
            DEFAULT = new GridCoverageProcessor();
            DEFAULT.addOperation(new Interpolator.Operation());
        }
        return DEFAULT;
    }

    /**
     * Add the specified operation to this processor. This method is usually invoked
     * at construction time <strong>before</code> this processor is made accessible.
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
            operations.put(name, operation);
        }
        else throw new IllegalStateException();
    }

    /**
     * Retrieve grid processing operation informations. The operation information
     * will contain the name of the operation as well as a list of its parameters.
     */
    public Set<Operation> getOperations()
    {return setView;}

    /**
     * Returns the operation for the specified name,
     * or <code>null</code> if there is none.
     */
    public Operation getOperation(final String name)
    {return operations.get(new CaselessStringKey(name));}

    /**
     * Apply a process operation to a grid coverage.
     *
     * @param  operationName Name of the operation to be applied to the grid coverage..
     * @param  parameters List of name value pairs for the parameters required for the operation.
     *         The easiest way to construct this list is to invoke <code>{@link #getOperation
     *         getOperation}(name).{@link Operation#getParameterList getParameterList}()</code>
     *         and to modify the returned list.
     * @return The result as a grid coverage.
     * @throws NoSuchElementException if there is no operation named <code>operationName</code>.
     */
    public GridCoverage doOperation(final String operationName, final ParameterList parameters) throws NoSuchElementException
    {
        final Operation operation = getOperation(operationName);
        if (operation!=null)
        {
            return doOperation(operation, parameters);
        }
        else throw new NoSuchElementException(operationName);
    }

    /**
     * Apply a process operation to a grid coverage.
     *
     * @param  operation The operation to be applied to the grid coverage..
     * @param  parameters List of name value pairs for the parameters required for
     *         the operation.  The easiest way to construct this list is to invoke
     *         <code>operation.{@link Operation#getParameterList getParameterList}()</code>
     *         and to modify the returned list.
     * @return The result as a grid coverage.
     */
    public GridCoverage doOperation(final Operation operation, final ParameterList parameters)
    {return operation.doOperation(parameters);}
}
