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
package fr.ird.sql;

// J2SE dependencies
import java.sql.SQLException;


/**
 * Lancée lorsqu'une partie d'une requête SQL devait être exécutée sur une machine distance,
 * et que cette exécution à échouée.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class RemoteException extends SQLException {
    /**
     * Construit une nouvelle exception avec le message et la cause spécifiés.
     *
     * @param message Le message expliquant la cause de l'échec.
     * @param cause La cause de l'exception.
     */
    public RemoteException(final String message, final java.rmi.RemoteException cause) {
        super(message);
        initCause(cause);
    }
}
