/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.database.gui.swing;

// J2SE dependencies
import java.awt.RenderingHints;

// JAI dependencies
import javax.media.jai.ParameterList;

// Geotools dependencies
import org.geotools.gp.Operation;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;


/**
 * An operation wich is the concatenation of two monadic operations.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ConcatenatedOperation extends Operation {
    /**
     * The first operation to apply.
     */
    private final String op1;

    /**
     * The second operation to apply.
     */
    private final String op2;

    /**
     * Construct a concatenated operation.
     *
     * @param name The operation name.
     * @param op1  The first operation to apply.
     * @param op2  The second operation to apply.
     */
    public ConcatenatedOperation(final String name, final String op1, final String op2) {
        super(name, MONADIC);
        this.op1 = op1;
        this.op2 = op2;
    }

    /**
     * Apply the operation.
     */
    protected GridCoverage doOperation(ParameterList parameters, final RenderingHints hints) {
        final GridCoverageProcessor processor = getGridCoverageProcessor(hints);
        GridCoverage coverage;
        coverage   = (GridCoverage) parameters.getObjectParameter("Source");
        parameters = processor.getOperation(op1).getParameterList();
        parameters = parameters.setParameter("Source", coverage);
        coverage   = processor.doOperation(op1, parameters);
        parameters = processor.getOperation(op2).getParameterList();
        parameters = parameters.setParameter("Source", coverage);
        coverage   = processor.doOperation(op2, parameters);
        return coverage;
    }
}
