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
package fr.ird.database;

// Divers
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Exception signalant qu'un enregistrement d'une des tables n'est pas valide.
 * Cette erreur peut survenir par exemple si les donn�es contenues dans l'enregistrement
 * ne sont pas coh�rentes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class IllegalRecordException extends CatalogException {
    /**
     * Nom de la table qui contient l'enregistrement invalide.
     */
    private final String table;

    /**
     * Construit une exception signalant qu'un enregistrement n'est pas valide.
     *
     * @param table Nom de la table qui contient l'enregistrement invalide.
     * @param message Message d�crivant l'erreur.
     */
    public IllegalRecordException(final String table, final String message) {
        super(message);
        this.table = table;
    }

    /**
     * Construit une exception signalant qu'un enregistrement n'est pas valide.
     *
     * @param table Nom de la table qui contient l'enregistrement invalide.
     * @param exception Exception rencontr� lors de l'analyse de l'enregistrement.
     */
    public IllegalRecordException(final String table, final Exception exception) {
        this(table, exception.getLocalizedMessage());
        initCause(exception);
    }

    /**
     * Retourne le nom de la table qui contient un enregistrement invalide.
     */
    public String getTable() {
        return table;
    }

    /**
     * Retourne une cha�ne de caract�re qui contiendra le
     * nom de la table et un message d�crivant l'erreur.
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
