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

// J2SE dependencies
import java.util.EventListener;


/**
 * Interface des objets interess�s � �tre inform�s des changements d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageChangeListener extends EventListener {
    /**
     * Pr�vient qu'une image a chang�e.
     */
    public abstract void coverageChanged(final CoverageChangeEvent event);
}
