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
package fr.ird.database.coverage;

// Base de donn�es
import fr.ird.database.Entry;


/**
 * Interface des entr�es repr�sentant une s�rie d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SeriesEntry extends Entry {
    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne la p�riode "normale" des images de cette s�rie
     * (en nombre de jours), ou {@link Double#NaN} si elle est inconnue.
     */
    public abstract double getPeriod();
}
