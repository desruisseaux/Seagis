/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2004 Institut de Recherche pour le Développement
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
import java.sql.SQLException;
import java.rmi.RemoteException;
import java.rmi.ServerException;


/**
 * Exception levée lorsqu'une exception autre que les exceptions standard héritant
 * de la classe {@link RemoteException} est levée lors de l'accés à une ressource.
 *
 * Par exemple, si une connection est réalisée sur un serveur et qu'une exception
 * du type {@link SQLException} est levée lors de l'execution d'une requête, alors
 * elle sera enveloppée dans une <code>CatalogException</code>.
 *
 * @version $Id$
 * @author Remi Eve
 */
public class CatalogException extends ServerException {
    /** 
     * Construit une exception enveloppant l'erreur spécifiée.
     *
     * @param cause La cause de l'exception.
     */
    public CatalogException(final Exception cause) {
        super(cause.getLocalizedMessage(), cause);
    }
    
    /** 
     * Construit une exception avec le message spécifié.
     *
     * @param message Message de l'exception.
     */
    public CatalogException(final String message) {
        super(message);
    }    
}
