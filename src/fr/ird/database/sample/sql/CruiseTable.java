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
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Seagis
import fr.ird.database.sample.CruiseEntry;
import fr.ird.database.sample.SampleDataBase;


/**
 * Table des campagne d'échantillonage.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CruiseTable extends Table {
    /**
     * Ensemble des objets {@link CruiseEntry} déjà créé.
     */
    private final Map<CruiseEntry,CruiseEntry> pool = new HashMap<CruiseEntry,CruiseEntry>();

    /**
     * La dernière entrée retournée par {@link #getEntry}. Utilisé comme optimisation seulement,
     * étant donné qu'on va souvent demander la même campagne plusieurs fois de suite.
     */
    private transient CruiseEntry last;
    
    /**
     * Construit une référence vers la table des campagnes.
     *
     * @param  connection Connexion vers la base de données.
     * @throws SQLException si cette table n'a pas pu construire sa requête SQL.
     */
    protected CruiseTable(final SampleDataBase database,
                          final Connection   connection) throws RemoteException
    {
        // Pour l'instant, on ignore la connexion. Elle sera prise en compte dans une version
        // future si on ajoute réellement une table des campagnes dans la base des données.
        super(database, null);
    }

    /**
     * Retourne une campagne pour le numéro ID spécifié.
     */
    public CruiseEntry getEntry(final int ID) throws RemoteException {
        if (last!=null && last.getID()==ID) {
            return last;
        }
        final CruiseEntry key = new fr.ird.database.sample.sql.CruiseEntry(ID);
        last = pool.get(key);
        if (last != null) {
            return last;
        }
        last = key;
        pool.put(key, key);
        return key;
    }
}
