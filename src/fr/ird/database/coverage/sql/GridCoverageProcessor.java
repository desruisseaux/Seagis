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
package fr.ird.database.coverage.sql;

// J2SE et JAI
import java.awt.RenderingHints;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gp.Operation;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.OperationNotFoundException;


/**
 * Implémentation de {@link org.geotools.gp.GridCoverageProcessor} qui peut
 * appliquer l'opération &quot;NodataFilter&quot; avant certaines autres opérations.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class GridCoverageProcessor extends org.geotools.gp.GridCoverageProcessor {
    /**
     * Le prefix à ajouter devant les noms des opérations pour ajouter l'opération
     * &quot;NodataFilter&quot;.
     */
    public static final String NODATA_FILTER = "NodataFilter";

    /**
     * Le séparateur à utiliser entre les noms d'opérations.
     */
    public static final char SEPARATOR = ';';

    /**
     * Construit un nouveau processeur.
     */
    protected GridCoverageProcessor() {
        super(org.geotools.gp.GridCoverageProcessor.getDefault(), null);
        addOperation(new Concatenated(getOperation("Interpolate")));
        addOperation(new Concatenated(getOperation("GradientMagnitude")));
    }

    /**
     * Opération déléguant son travail à une autre opération, mais après avoir appliqué
     * l'opération &quot;NodataFilter&quot;.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Concatenated extends Operation {
        /**
         * L'opération qui effectuera le travail.
         */
        private final Operation operation;

        /**
         * Construit une opération qui envelopera l'opération spécifiée.
         */
        public Concatenated(final Operation operation) {
            super(NODATA_FILTER + SEPARATOR + operation.getName(), operation.getParameterListDescriptor());
            this.operation = operation;
        }

        /**
         * Effectue l'opération.
         */
        protected GridCoverage doOperation(final ParameterList parameters, final RenderingHints hints) {
            final org.geotools.gp.GridCoverageProcessor processor = getGridCoverageProcessor(hints);
            GridCoverage coverage = (GridCoverage) parameters.getObjectParameter("Source");
            coverage = processor.doOperation(NODATA_FILTER, coverage);
            parameters.setParameter("Source", coverage);
            return processor.doOperation(operation, parameters);
        }
    }
}
