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

// J2SE dependencies
import java.rmi.RemoteException;


/**
 * Interface de base des entrées dans la base de données.
 * Une entrée peut représenter une série d'images ou une
 * image individuelle par exemple.
 *
 * <P>Quelques interfaces sont conçues pour fonctionner avec les RMI.
 * C'est pourquoi toutes les méthodes peuvent lancer une exception de
 * type <code>RemoteException</code>.</P>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Entry {
    /**
     * Retourne le nom de cette entrée. Ce nom peut être arbitraire.
     * Dans le cas d'une image (par exemple), il s'agira le plus souvent
     * du nom du fichier de l'image.
     *
     * @throws RemoteException si cette méthode nécessitait un appel sur un objet
     *         distant, et que cet appel a échoué.
     */
    public abstract String getName() throws RemoteException;

    /**
     * Retourne des remarques s'appliquant à cette entrée,
     * ou <code>null</code> s'il n'y en a pas. Ces remarques
     * sont souvent une chaîne descriptives qui peuvent être
     * affichées comme "tooltip text".
     *
     * @throws RemoteException si cette méthode nécessitait un appel sur un objet
     *         distant, et que cet appel a échoué.
     */
    public abstract String getRemarks() throws RemoteException;
}
