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
 */
package fr.ird.database.sample;

// Seagis
import fr.ird.database.Entry;


/**
 * Campagne d'�chantillonage. Plusieurs �chantillons {@link SampleEntry} peuvent provenir de
 * la m�me campagne d'�chantillonage.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CruiseEntry extends Entry {
    /**
     * Retourne le num�ro de la campagne d'�chantillonage.
     */
    public abstract int getID();

    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();
}
