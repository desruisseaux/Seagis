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
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.Remote;


/**
 * Base interface for connections to a database. An application usually has a single
 * <code>DataBase</code> object, which is the starting point for working with the
 * <code>fr.ird.database</code> package. Each <code>DataBase</code> object provides
 * various methods for fetching many {@link Table} objects. Each <code>Table</code>
 * object can in turn produces many {@link Entry} objects.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 * @author Remi Eve
 */
public interface DataBase extends Remote {
    /**
     * Key for fetching the JDBC driver for a connection to a SQL database.
     *
     * @see #getProperty
     */
    public static final ConfigurationKey DRIVER = new ConfigurationKey("Driver", null, "sun.jdbc.odbc.JdbcOdbcDriver");

    /**
     * Key for fetching the URL of a SQL database.
     *
     * @see #getProperty
     */
    public static final ConfigurationKey SOURCE = new ConfigurationKey("Sources", null, "jdbc:odbc:SEAGIS");

    /**
     * Key for fetching the user name during a connection to a database.
     *
     * @see #getProperty
     */
    public static final ConfigurationKey USER = new ConfigurationKey("User", null, null);

    /**
     * Key for fetching the user name during a connection to a database.
     * <strong>WARNING:</strong> This information is not encrypted.
     *
     * @see #getProperty
     */
    public static final ConfigurationKey PASSWORD = new ConfigurationKey("Password", null, null);

    /**
     * Key for fetching the default timezone for dates in the database.
     *
     * @see #getProperty
     */
    public static final ConfigurationKey TIMEZONE = new ConfigurationKey("TimeZone", null, "UTC");

    /**
     * Returns a property from the database. The <code>key</code> argument is a key like
     * {@link #DRIVER}, {@link #SOURCE} or {@link #TIMEZONE}. This method returns
     * <code>null</code> if the property is undefined and has no default value.
     *
     * @throws RemoteException if a problem occured while querying the backing store.
     */
    public abstract String getProperty(final ConfigurationKey key) throws RemoteException;

    /**
     * Returns the timezone for dates in the database.
     *
     * @throws RemoteException if a problem occured while querying the backing store.
     */
    public abstract TimeZone getTimeZone() throws RemoteException;

    /**
     * Close the connection and release any resources used by this object.
     *
     * @throws RemoteException if a problem occured while disposing ressources.
     */
    public abstract void close() throws IOException;
}
