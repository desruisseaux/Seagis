/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
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
 * Une op�ration effectu�e par un {@link GridCoverageProcessor}.
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
     * Le nom de l'op�ration.
     */
    private final String operation;

    /**
     * Les param�tres, ou <code>null</code> si aucun.
     */
    private final ParameterList parameters;

    /**
     * Construit une op�ration sans param�tres.
     *
     * @param processor   Le processeur.
     * @param operation   Le nom de l'op�ration.
     * @param parameters  Les param�tres, ou <code>null</code> si aucun.
     * @param description Une description � faire appara�tre dans le menu.
     */
    public ProcessorOperation(final GridCoverageProcessor processor,
                              final String                operation,
                              final String              description)
    {
        this(processor, operation, null, description);
    }

    /**
     * Construit une op�ration.
     *
     * @param processor   Le processeur.
     * @param operation   Le nom de l'op�ration.
     * @param parameters  Les param�tres, ou <code>null</code> si aucun.
     * @param description Une description � faire appara�tre dans le menu.
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
     * Applique l'op�ration sur une image.
     */
    protected synchronized GridCoverage doFilter(final GridCoverage coverage) {
        if (parameters != null) {
            return processor.doOperation(operation, parameters.setParameter("Source", coverage));
        } else {
            return processor.doOperation(operation, coverage);
        }
    }
}
