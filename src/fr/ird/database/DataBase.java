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
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import java.rmi.RemoteException;
import java.rmi.Remote;

// GeoAPI dependencies
import org.opengis.util.InternationalString;


/**
 * Interface de base des connections vers une base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 * @author Remi Eve
 */
public interface DataBase extends Remote {
    /**
     * Le niveau pour enregistrer les instruction SELECT dans le {@linkplain Logger journal}.
     *
     * @see fr.ird.database.coverage.CoverageDataBase#LOGGER
     * @see fr.ird.database.sample.SampleDataBase#LOGGER
     */
    public static final Level SQL_SELECT = SQLLevel.SQL_SELECT;

    /**
     * Le niveau pour enregistrer les instruction UPDATE dans le {@linkplain Logger journal}.
     *
     * @see fr.ird.database.coverage.CoverageDataBase#LOGGER
     * @see fr.ird.database.sample.SampleDataBase#LOGGER
     */
    public static final Level SQL_UPDATE = SQLLevel.SQL_UPDATE;

    /**
     * Clé de la propriétée représentant le pilote JDBC
     * à utiliser pour se connecter à la base de données.
     * Cette clé peut être utilisée avec {@link #getProperty}.
     */
    public static final Key DRIVER = new Key("Driver", null, null);

    /**
     * Clé de la propriétée représentant la source des données.
     * Cette clé peut être utilisée avec {@link #getProperty}.
     */
    public static final Key SOURCE = new Key("Sources", null, null);

    /**
     * Clé de la propriétée représentant le fuseau horaire de
     * la base de données. Cette clé peut être utilisée avec
     * {@link #getProperty}.
     */
    public static final Key TIMEZONE = new Key("TimeZone", null, null);

    /**
     * Retourne une des propriétée de la base de données. La clé <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette méthode retourne <code>null</code> si la propriété
     * demandée n'est pas définie.
     */
    public abstract String getProperty(final Key name) throws RemoteException;

    /**
     * Retourne le fuseau horaire des dates exprimées dans cette base de données.
     */
    public abstract TimeZone getTimeZone() throws RemoteException;

    /**
     * Ferme la connection et libère les ressources utilisées par cette base de données.
     * Appelez cette méthode lorsque vous n'aurez plus besoin de consulter cette base.
     *
     * @throws RemoteException si un problème est survenu lors de la disposition des ressources.
     */
    public abstract void close() throws IOException;

    /**
     * Une propriétés à utiliser lors de la connection à une base de données.
     */
    public static final class Key implements Serializable {
        /**
         * Pour compatibilité entre différentes versions.
         */
        private static final long serialVersionUID = 4719725873634041733L;

        /**
         * Ensemble des clés déjà créés.
         */
        private static final Map<String,Key> POOL = new HashMap<String,Key>();

        /**
         * Nom de la propriété.
         */
        final String name;
        
        /**
         * Valeur par defaut de la propriété, si celle ci n'est pas définie.
         */
        final transient String defaultValue;
        
        /**
         * Description de la propriété.
         */
        final transient InternationalString description;
    
        /**
         * Construit une nouvelle clé.
         *
         * @param name          Nom de la propriété.
         * @param description   Description de la propriété.
         * @param defaultValue  Valeur par defaut de la propriété, si celle ci n'est pas définie.
         */
        public Key(final String name, final InternationalString description, final String defaultValue) {
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
         * Retourne l'instance à utiliser après une lecture binaire
         * (habituellement à des fins de RMI).
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
    }    
}
