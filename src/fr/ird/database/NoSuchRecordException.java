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
package fr.ird.database;

// Divers
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Raised when a record was required but not found in a {@linkplain Table table}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class NoSuchRecordException extends CatalogException {
    /**
     * Nom de la table qui contient l'enregistrement invalide.
     */
    private final String table;

    /**
     * Construit une exception signalant qu'un enregistrement n'a pas été trouvé.
     *
     * @param table Nom de la table de l'enregistrement manquant.
     * @param message Message décrivant l'erreur.
     */
    public NoSuchRecordException(final String table, final String message) {
        super(message);
        this.table=table;
    }

    /**
     * Retourne le nom de la table dans laquelle il manque un enregistrement.
     */
    public String getTable() {
        return table;
    }

    /**
     * Retourne une chaîne de caractère qui contiendra le
     * nom de la table et un message décrivant l'erreur.
     */
    public String getLocalizedMessage() {
        final String table   = getTable();
        final String message = super.getLocalizedMessage();
        if (table == null) {
            return message;
        }
        return Resources.format(ResourceKeys.TABLE_ERROR_$2, table, message);
    }
}
