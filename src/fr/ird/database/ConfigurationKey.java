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
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;

// GeoAPI dependencies
import org.opengis.util.InternationalString;


/**
 * A key for an aspect of a {@link DataBase} configuration. For example it may be
 * the {@linkplain SQLDataBase#DRIVER JDBC driver} to use for a connection to a SQL
 * database.
 *
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaux
 */
public final class ConfigurationKey implements Serializable {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 4719725873634041733L;

    /**
     * The set of key already created in this virtual machine.
     * Keys are created once for ever, since they are usually
     * created as part of class initialisation.
     */
    private static final Map<String,ConfigurationKey> POOL = new HashMap<String,ConfigurationKey>();

    /**
     * The name of this key.
     */
    public final String name;

    /**
     * A default value for this key. This hard-coded value is used only if the
     * user don't provides an explicit value.
     */
    public final transient String defaultValue;

    /**
     * A human-readable description of this key.
     */
    public final transient InternationalString description;

    /**
     * Construct a new key.
     *
     * @param name          The name of this key.
     * @param description   A human-readable description of this key.
     * @param defaultValue  A default value for this key. This is used only
     *                      if the user didn't provided explicitly a value.
     */
    public ConfigurationKey(final String name,
                            final InternationalString description,
                            final String defaultValue)
    {
        this.name         = name;
        this.defaultValue = defaultValue;
        this.description  = description;
        synchronized (POOL) {
            if (!equals(POOL.put(name, this))) {
                throw new IllegalStateException("Doublon dans les noms de clés.");
            }
        }
    }

    /**
     * Returns a canonical instance of this key after deserialization.
     * This is needed for RMI usage for example.
     */
    protected Object readResolve() throws ObjectStreamException {
        synchronized (POOL) {
            final Object r = POOL.get(name);
            if (r != null) {
                return r;
            }
        }
        throw new InvalidObjectException("Clé inconnue: "+name);
    }

    /**
     * Returns a string representation of this key.
     */
    public String toString() {
        return name;
    }
}
