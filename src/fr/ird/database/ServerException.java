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
package fr.ird.database;

// J2SE dependencies
import java.sql.SQLException;
import java.rmi.RemoteException;


/**
 * Lanc�e lorsqu'une partie d'une requ�te SQL devait �tre ex�cut�e sur un serveur,
 * et que cette ex�cution � �chou�e. Cette erreur est caus�e par une exception d'un
 * autre type, par exemple {@link RemoteException} si un certain travail devait �tre
 * fait sur une machine distante dans le cadre de l'ex�cution d'une requ�te.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ServerException extends SQLException {
    /**
     * Construit une nouvelle exception avec le message et la cause sp�cifi�s.
     *
     * @param message Le message expliquant la cause de l'�chec.
     * @param cause La cause de l'exception.
     */
    public ServerException(final String message, final Exception cause) {
        super(message);
        initCause(cause);
    }
}
