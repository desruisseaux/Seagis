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
package fr.ird.database.sample.sql;

// J2SE
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;


/**
 * Impl�mentation d'une entr� d�signant une campagne d'�chantillonage. 
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CruiseEntry implements fr.ird.database.sample.CruiseEntry, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -1097308166553134900L;

    /**
     * Num�ro identifiant la campagne d'�chantillonage.
     */
    private final int ID;

    /**
     * Construit une entr� pour le num�ro de campagne sp�cifi�.
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
     * Compare cette campagne avec l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        return (object instanceof CruiseEntry) && ((CruiseEntry)object).ID==ID;
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant cette campagne.
     */
    public String toString() {
        return Utilities.getShortClassName(this) + '[' + ID + ']';
    }
}
