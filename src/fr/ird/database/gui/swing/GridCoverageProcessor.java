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

// JAI
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gp.Operation;
import org.geotools.gc.GridCoverage;

// Seagis
import fr.ird.database.coverage.sql.CoverageDataBase;


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
     * <code>true</code> si le processeur a été initialisé.
     */
    private static boolean initialized;

    /**
     * Initialize le processeur.
     */
    public static synchronized void initialize() {
        if (!initialized) {
            CoverageDataBase.setDefaultGridCoverageProcessor(new GridCoverageProcessor());
            initialized = true;
        }
    }
    
    /**
     * Construit un nouveau processeur.
     */
    private GridCoverageProcessor() {
        super(CoverageDataBase.getDefaultGridCoverageProcessor(), null);
    }

    /**
     * Applique une opération.
     */
    public GridCoverage doOperation(final Operation     operation,
                                    final ParameterList parameters)
    {
        final String prefix = NODATA_FILTER + '-';
        String name = operation.getName();
        if (name.startsWith(prefix)) {
            GridCoverage coverage = (GridCoverage) parameters.getObjectParameter("Source");
            coverage = doOperation(NODATA_FILTER, coverage);
            parameters.setParameter("Source", coverage);
            name = name.substring(prefix.length());
        }
        return super.doOperation(operation, parameters);
    }
}
