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
 * Base class for exceptions that occur while querying a {@link DataBase} or a related object.
 * This exception usually occurs on the server side and is forwarded to the client. It may
 * contains an {@link SQLException} as its cause.
 *
 * @version $Id$
 * @author Remi Eve
 */
public class CatalogException extends ServerException {
    /** 
     * Constructs an exception from the specified cause.
     * The cause is often a {@link SQLException}.
     */
    public CatalogException(final Exception cause) {
        super(cause.getLocalizedMessage(), cause);
    }
    
    /**
     * Constructs an exception with the specified message.
     */
    public CatalogException(final String message) {
        super(message);
    }    
}
