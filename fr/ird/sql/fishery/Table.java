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
 *
 *
 * Contact: Michel Petit
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.sql.fishery;

// Base de donn�es
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Divers
import java.util.logging.Logger;
import java.util.prefs.Preferences;


/**
 * Classe de base des tables de la base de donn�es de p�ches.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class Table implements fr.ird.sql.Table {
    /* Nom de table. */ static final String ENVIRONMENTS = "Environnements";
    /* Nom de table. */ static final String PARAMETERS   = "Param�tres";
    /* Nom de table. */ static final String LONGLINES    = "Captures";
    /* Nom de table. */ static final String SEINES       = "Captures";
    /* Nom de table. */ static final String SPECIES      = "Esp�ces";

    /**
     * Journal des �v�nements.
     */
    static final Logger logger = Logger.getLogger("fr.ird.sql");
    static {
        fr.ird.util.InterlineFormatter.init(logger);
    }

    /**
     * Propri�t�s de la base de donn�es. Ces propri�t�s peuvent contenir
     * notamment les instructions SQL � utiliser pour interroger la base
     * de donn�es d'images.
     */
    static final Preferences preferences=Preferences.systemNodeForPackage(Table.class);

    /**
     * Requ�te SQL faisant le lien
     * avec la base de donn�es.
     */
    protected PreparedStatement statement;

    /**
     * Construit une objet qui interrogera la
     * base de donn�es en utilisant la requ�te
     * sp�cifi�e.
     *
     * @param statement Interrogation � soumettre � la base de donn�es.
     */
    protected Table(final PreparedStatement statement) {
        this.statement=statement;
    }

    /**
     * Compl�te la requ�te SQL en ajouter des noms de colonnes � celles qui existe d�j�.
     * Les noms seront ajout�es juste avant la premi�re clause "FROM" dans la requ�te SQL.
     *
     * @param  query Requ�te � modifier.
     * @param  colunms Noms de colonnes � ajouter.
     * @return La requ�te modifi�e.
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
     * Recherche une sous-cha�ne dans une cha�ne en ignorant les diff�rences entre majuscules et
     * minuscules. Les racourcis du genre <code>text.toUpperCase().indexOf("SEARCH FOR")</code>
     * ne fonctionne pas car <code>toUpperCase()</code> et <code>toLowerCase()</code> peuvent
     * changer le nombre de caract�res de la cha�ne.
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
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException {
        if (statement != null) {
            statement.close();
            statement = null;
        }
    }
}
