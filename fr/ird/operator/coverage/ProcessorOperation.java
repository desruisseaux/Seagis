/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.operator.coverage;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;
import org.geotools.gp.GridCoverageProcessor;

// Java Advanced Imaging
import javax.media.jai.ParameterList;


/**
 * Une opération effectuée par un {@link GridCoverageProcessor}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ProcessorOperation extends Operation {
    /**
     * Le processeur.
     */
    private final GridCoverageProcessor processor;

    /**
     * Le nom de l'opération.
     */
    private final String operation;

    /**
     * Les paramètres, ou <code>null</code> si aucun.
     */
    private final ParameterList parameters;

    /**
     * Construit une opération sans paramètres.
     *
     * @param processor   Le processeur.
     * @param operation   Le nom de l'opération.
     * @param parameters  Les paramètres, ou <code>null</code> si aucun.
     * @param description Une description à faire apparaître dans le menu.
     */
    public ProcessorOperation(final GridCoverageProcessor processor,
                              final String                operation,
                              final String              description)
    {
        this(processor, operation, null, description);
    }

    /**
     * Construit une opération.
     *
     * @param processor   Le processeur.
     * @param operation   Le nom de l'opération.
     * @param parameters  Les paramètres, ou <code>null</code> si aucun.
     * @param description Une description à faire apparaître dans le menu.
     */
    public ProcessorOperation(final GridCoverageProcessor processor,
                              final String                operation,
                              final ParameterList        parameters,
                              final String              description)
    {
        super(description);
        this.processor  = processor;
        this.operation  = operation;
        this.parameters = parameters;
    }

    /**
     * Applique l'opération sur une image.
     */
    protected synchronized GridCoverage doFilter(final GridCoverage coverage) {
        if (parameters != null) {
            return processor.doOperation(operation, parameters.setParameter("Source", coverage));
        } else {
            return processor.doOperation(operation, coverage);
        }
    }
}
