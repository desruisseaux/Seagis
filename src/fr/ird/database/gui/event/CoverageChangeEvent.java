/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
 * Evénement représentant un changement d'image.
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
     * Construit un événement représentant un changement d'images.
     *
     * @param source   Source de cet événement.
     * @param entry    Description de l'image (peut être nulle).
     * @param coverage Nouvelle image (peut être nulle).
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
     * Retourne l'entrée décrivant l'image qui vient d'être lue. Si cette
     * entrée n'est pas connue ou qu'il n'y en a pas, alors cette méthode
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
