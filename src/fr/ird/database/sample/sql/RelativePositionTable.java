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
package fr.ird.database.sample.sql;

// J2SE
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Seagis.
import fr.ird.database.CatalogException;

/**
 * Table des positions spatio-temporelles relatives aux positions des données de pêches.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class RelativePositionTable
    extends SingletonTable<fr.ird.database.sample.RelativePositionEntry, RelativePositionEntry>
{
    /**
     * La requête SQL à utiliser pour obtnir une position relative.
     */
    static final String SQL_SELECT = configuration.get(Configuration.KEY_POSITIONS);
    // static final String SQL_SELECT =
    //         "SELECT ID, nom, temps, défaut FROM "+POSITIONS+" WHERE ID=? ORDER BY temps DESC";

    /** Numéro de colonne. */ private static final int ID        = 1;
    /** Numéro de colonne. */ private static final int NAME      = 2;
    /** Numéro de colonne. */ private static final int TIME_LAG  = 3;
    /** Numéro de colonne. */ private static final int DEFAULT   = 4;

    /**
     * Nombre de millisecondes dans une journée.
     */
    private static final long DAY = 24*60*60*1000L;

    /**
     * Construit une nouvelle table utilisant la connexion spécifiée.
     *
     * @param  connection Connection vers une base de données des échantillons.
     * @param  type Le type de la requête.
     *         Une des constantes {@link #LIST}, {@link #BY_ID} ou {@link #BY_NAME}.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    protected RelativePositionTable(final Connection connection, final int type) throws RemoteException {
        super(connection, type);
    }

    /**
     * {@inheritDoc}
     */
    protected String getTableName() {
        return POSITIONS;
    }

    /**
     * Retourne l'instruction SQL à utiliser pour obtenir les positions relatives.
     */
    protected String getQuery() {
        return SQL_SELECT;
    }

    /**
     * Retourne une entrée pour la ligne courante de l'objet {@link ResultSet} spécifié.
     */
    protected RelativePositionEntry createEntry(final ResultSet results) throws RemoteException {
        try {
            return new RelativePositionEntry(results.getInt    (ID),
                                             results.getString (NAME),
                                  Math.round(results.getDouble (TIME_LAG)*DAY),
                                             results.getBoolean(DEFAULT));
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }
}
