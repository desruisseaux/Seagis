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
 */
package fr.ird.database.sample.sql;

// J2SE
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;


/**
 * Implémentation d'une entré désignant une campagne d'échantillonage. 
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CruiseEntry implements fr.ird.database.sample.CruiseEntry, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -1097308166553134900L;

    /**
     * Numéro identifiant la campagne d'échantillonage.
     */
    private final int ID;

    /**
     * Construit une entré pour le numéro de campagne spécifié.
     */
    public CruiseEntry(final int ID) {
        this.ID = ID;
    }

    /**
     * {@inheritDoc}
     */
    public int getID() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return String.valueOf(ID);
    }

    /**
     * {@inheritDoc}
     */
    public String getRemarks() {
        return null;
    }

    /**
     * Retourne un code pour cette campagne.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Compare cette campagne avec l'objet spécifié.
     */
    public boolean equals(final Object object) {
        return (object instanceof CruiseEntry) && ((CruiseEntry)object).ID==ID;
    }

    /**
     * Retourne une chaîne de caractères représentant cette campagne.
     */
    public String toString() {
        return Utilities.getShortClassName(this) + '[' + ID + ']';
    }
}
