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
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class Table implements fr.ird.sql.Table {
    /* Nom de table. */ static final String ENVIRONMENTS = "Environnements";
    /* Nom de table. */ static final String PARAMETERS   = "Paramètres";
    /* Nom de table. */ static final String LONGLINES    = "Captures";
    /* Nom de table. */ static final String SEINES       = "Captures";
    /* Nom de table. */ static final String SPECIES      = "Espèces";

    /**
     * Journal des évènements.
     */
    static final Logger logger = Logger.getLogger("fr.ird.sql");
    static {
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
    protected Table(final PreparedStatement statement) {
        this.statement=statement;
    }

    /**
     * Complète la requète SQL en ajouter des noms de colonnes à celles qui existe déjà.
     * Les noms seront ajoutées juste avant la première clause "FROM" dans la requête SQL.
     *
     * @param  query Requête à modifier.
     * @param  colunms Noms de colonnes à ajouter.
     * @return La requête modifiée.
     */
    static String completeSelect(String query, final String[] columns) {
        int index = indexOf(query, "FROM");
        if (index >= 0) {
            while (index>=1 && Character.isWhitespace(query.charAt(index-1))) index--;
            final StringBuffer buffer = new StringBuffer(query.substring(0, index));
            for (int i=0; i<columns.length; i++) {
                final String name = columns[i];
                if (name != null) {
                    buffer.append(", ");
                    buffer.append(name);
                }
            }
            buffer.append(query.substring(index));
            query = buffer.toString();
        }
        return query;
    }

    /**
     * Recherche une sous-chaîne dans une chaîne en ignorant les différences entre majuscules et
     * minuscules. Les racourcis du genre <code>text.toUpperCase().indexOf("SEARCH FOR")</code>
     * ne fonctionne pas car <code>toUpperCase()</code> et <code>toLowerCase()</code> peuvent
     * changer le nombre de caractères de la chaîne.
     */
    static int indexOf(final String text, final String searchFor) {
        final int searchLength = searchFor.length();
        final int length = text.length();
        for (int i=0; i<length; i++) {
            if (text.regionMatches(true, 0, searchFor, 0, searchLength)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        if (statement != null) {
            statement.close();
            statement = null;
        }
    }
}
