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
package fr.ird.awt.event;

// Evénements et divers
import java.util.EventObject;
import org.geotools.gc.GridCoverage;
import fr.ird.sql.image.ImageEntry;


/**
 * Evénements représentant un changement d'image. En général, cet
 * événement est lancé par une composante graphique qui enveloppe
 * {@link net.seas.map.MapPanel}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ImageChangeEvent extends EventObject
{
    /**
     * Description de l'image qui vient de changer.
     */
    private final ImageEntry entry;

    /**
     * Image qui vient de changer, ou
     * <code>null</code> s'il n'y en a pas.
     */
    private final GridCoverage coverage;

    /**
     * Construit un événement représentant
     * un changement d'images.
     *
     * @param source Source de cet événement.
     * @param entry  Description de l'image (peut être nulle).
     * @param image  Nouvelle image (peut être nulle).
     */
    public ImageChangeEvent(final Object source, final ImageEntry entry, final GridCoverage coverage)
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
    public ImageEntry getEntry()
    {return entry;}

    /**
     * Image qui vient de changer, ou
     * <code>null</code> s'il n'y en a
     * pas ou qu'elle n'est pas connue.
     */
    public GridCoverage getGridCoverage()
    {return coverage;}
}
