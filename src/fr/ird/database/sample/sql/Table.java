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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.prefs.Preferences;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

// Seagis
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.database.sample.SampleDataBase;
import fr.ird.resources.seagis.Resources;


/**
 * Classe de base des tables de la base de donn�es de p�ches.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class Table extends UnicastRemoteObject implements fr.ird.database.Table {
    /** Nom de table. */ static final String ENVIRONMENTS  = "Environnements";
    /** Nom de table. */ static final String LINEAR_MODELS = "Mod�les lin�aires";
    /** Nom de table. */ static final String DISTRIBUTIONS = "Distributions";
    /** Nom de table. */ static final String DESCRIPTORS   = "Descripteurs";
    /** Nom de table. */ static final String PARAMETERS    = "Param�tres";
    /** Nom de table. */ static final String OPERATIONS    = "Op�rations";
    /** Nom de table. */ static final String POSITIONS     = "Positions";
    /** Nom de table. */ static final String SAMPLES       = "Captures";
    /** Nom de table. */ static final String SPECIES       = "Esp�ces";

    /**
     * The database where this table come from.
     */
    protected final SampleDataBase database;

    /**
     * Requ�te SQL � utiliser pour obtenir les donn�es.
     */
    protected PreparedStatement statement;

    /**
     * Construit une table qui interrogera la base de donn�es en utilisant la requ�te sp�cifi�e.
     *
     * @param statement Interrogation � soumettre � la base de donn�es, ou <code>null</code> si
     *        aucune.
     */
    protected Table(final SampleDataBase    database,
                    final PreparedStatement statement) throws RemoteException
    {
        this.database  = database;
        this.statement = statement;
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
        int index = indexOfWord(query, "FROM");
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
     * Replace substring "[?]" by the specified value.
     *
     * @param  query The string where to look for "[?]".
     * @param  The value to substitute to "[?]".
     * @return The modified string.
     */
    static String replaceQuestionMark(final String query, String value) {
        if (Character.isUnicodeIdentifierPart(value.charAt(0))) {
            if (value.indexOf(' ') >= 0) {
                value = '[' + value + ']'; // TODO: Use " instead when we will abandon Access.
            }
        }
        final String PARAM = "[?]";
        final StringBuffer buffer = new StringBuffer(query);
        for (int index=-1; (index=buffer.indexOf(PARAM, index+1))>=0;) {
            buffer.replace(index, index+PARAM.length(), value);
        }
        return buffer.toString();
    }

    /**
     * Recherche une sous-cha�ne dans une cha�ne en ignorant les diff�rences entre majuscules et
     * minuscules. Les racourcis du genre <code>text.toUpperCase().indexOf("SEARCH FOR")</code>
     * ne fonctionne pas car <code>toUpperCase()</code> et <code>toLowerCase()</code> peuvent
     * changer le nombre de caract�res de la cha�ne. De plus, cette m�thode v�rifie que le mot
     * est d�limit� par des espaces ou de la ponctuation.
     */
    static int indexOfWord(final String text, final String searchFor) {
        final int searchLength = searchFor.length();
        final int length = text.length();
        for (int i=0; i<length; i++) {
            if (text.regionMatches(true, i, searchFor, 0, searchLength)) {
                if (i!=0 && Character.isUnicodeIdentifierPart(text.charAt(i-1))) {
                    continue;
                }
                final int upper = i+length;
                if (upper<length && Character.isUnicodeIdentifierPart(text.charAt(upper))) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Create a configuration key for the specified attributes.
     */
    static ConfigurationKey createKey(final String name, final int text, final String SQL) {
        return new ConfigurationKey(name, Resources.formatInternational(text), SQL);
    }

    /**
     * Returns a property value for the specified key. This method work for a null database,
     * which is convenient for testing purpose.
     */
    protected final String getProperty(final ConfigurationKey key) throws RemoteException {
        return (database!=null) ? database.getProperty(key) : key.defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public final SampleDataBase getDataBase() {
        return database;
    }

    /**
     * Lib�re les ressources utilis�es par cette table.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws RemoteException {
        if (statement != null) try {
            statement.close();
            statement = null;
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }
}
