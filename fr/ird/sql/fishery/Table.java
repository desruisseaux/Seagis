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
package fr.ird.sql.fishery;

// Base de données
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Divers
import java.util.logging.Logger;
import java.util.prefs.Preferences;


/**
 * Classe de base des tables de la base de données de pêches.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class Table implements fr.ird.sql.Table
{
    /* Nom de table. */ static final String ENVIRONMENTS = "Environnements";
    /* Nom de table. */ static final String LONGLINES    = "Captures";
    /* Nom de table. */ static final String SEINES       = "Captures";
    /* Nom de table. */ static final String SPECIES      = "Espèces";

    /**
     * Journal des évènements.
     */
    static final Logger logger = Logger.getLogger("fr.ird.sql");
    static
    {
        fr.ird.util.InterlineFormatter.init(logger);
    }

    /**
     * Propriétés de la base de données. Ces propriétés peuvent contenir
     * notamment les instructions SQL à utiliser pour interroger la base
     * de données d'images.
     */
    static final Preferences preferences=Preferences.systemNodeForPackage(Table.class);

    /**
     * Requète SQL faisant le lien
     * avec la base de données.
     */
    protected PreparedStatement statement;

    /**
     * Construit une objet qui interrogera la
     * base de données en utilisant la requête
     * spécifiée.
     *
     * @param statement Interrogation à soumettre à la base de données.
     */
    protected Table(final PreparedStatement statement)
    {this.statement=statement;}

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (statement!=null)
        {
            statement.close();
            statement = null;
        }
    }
}
