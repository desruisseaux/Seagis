/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.coverage.sql;

// J2SE et JAI
import java.awt.RenderingHints;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gp.Operation;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.OperationNotFoundException;


/**
 * Impl�mentation de {@link org.geotools.gp.GridCoverageProcessor} qui peut
 * appliquer l'op�ration &quot;NodataFilter&quot; avant certaines autres op�rations.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class GridCoverageProcessor extends org.geotools.gp.GridCoverageProcessor {
    /**
     * Le prefix � ajouter devant les noms des op�rations pour ajouter l'op�ration
     * &quot;NodataFilter&quot;.
     */
    public static final String NODATA_FILTER = "NodataFilter";

    /**
     * Le s�parateur � utiliser entre les noms d'op�rations.
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
     * Op�ration d�l�guant son travail � une autre op�ration, mais apr�s avoir appliqu�
     * l'op�ration &quot;NodataFilter&quot;.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Concatenated extends Operation {
        /**
         * L'op�ration qui effectuera le travail.
         */
        private final Operation operation;

        /**
         * Construit une op�ration qui envelopera l'op�ration sp�cifi�e.
         */
        public Concatenated(final Operation operation) {
            super(NODATA_FILTER + SEPARATOR + operation.getName(), operation.getParameterListDescriptor());
            this.operation = operation;
        }

        /**
         * Effectue l'op�ration.
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
