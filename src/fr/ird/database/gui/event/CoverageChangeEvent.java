/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 */
package fr.ird.database.gui.event;

// Dependencies
import java.util.EventObject;
import org.geotools.gc.GridCoverage;
import fr.ird.database.coverage.CoverageEntry;


/**
 * Ev�nement repr�sentant un changement d'image.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CoverageChangeEvent extends EventObject {
    /**
     * Description de l'image qui vient de changer.
     */
    private final CoverageEntry entry;

    /**
     * Image qui vient de changer, ou <code>null</code> s'il n'y en a pas.
     */
    private final GridCoverage coverage;

    /**
     * Construit un �v�nement repr�sentant un changement d'images.
     *
     * @param source   Source de cet �v�nement.
     * @param entry    Description de l'image (peut �tre nulle).
     * @param coverage Nouvelle image (peut �tre nulle).
     */
    public CoverageChangeEvent(final Object        source,
                               final CoverageEntry entry,
                               final GridCoverage  coverage)
    {
        super(source);
        this.entry    = entry;
        this.coverage = coverage;
    }

    /**
     * Retourne l'entr�e d�crivant l'image qui vient d'�tre lue. Si cette
     * entr�e n'est pas connue ou qu'il n'y en a pas, alors cette m�thode
     * retourne <code>null</code>.
     */
    public CoverageEntry getEntry() {
        return entry;
    }

    /**
     * Image qui vient de changer, ou <code>null</code> s'il n'y en a
     * pas ou qu'elle n'est pas connue.
     */
    public GridCoverage getGridCoverage() {
        return coverage;
    }
}
