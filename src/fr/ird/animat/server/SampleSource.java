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
package fr.ird.animat.server;

// D�pendences
import java.util.Set;
import java.util.Collection;
import java.rmi.RemoteException;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;
import fr.ird.database.sample.SampleEntry;


/**
 * Interface des {@linkplain Environment environnement} capable de fournir des valeurs
 * d'{@linkplain SampleEntry �chantillons}.  Ces �chantillons peuvent �tre par exemple
 * des p�ches. Une source d'�chantillon sera typiquement connect�e � une base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleSource {
    /**
     * Retourne toutes les esp�ces se trouvant dans la table.
     *
     * @return Les esp�ces se trouvant dans la table.
     * @throws RemoteException si une erreur est survenue lors de l'interrogation de la base de donn�es.
     */
    public Set<Species> getSpecies() throws RemoteException;

    /**
     * Retourne l'ensemble des captures pour le pas de temps courant.
     *
     * @return Les captures pour le pas de temps courant.
     * @throws RemoteException si une erreur est survenue lors de l'interrogation de la base de donn�es.
     */
    public Collection<SampleEntry> getSamples() throws RemoteException;
}
