/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 * Interface de base des connections vers une base de donn�es.
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
     * Cl� de la propri�t�e repr�sentant le pilote JDBC
     * � utiliser pour se connecter � la base de donn�es.
     * Cette cl� peut �tre utilis�e avec {@link #getProperty}.
     */
    public static final Key DRIVER = new Key("Driver", null, null);

    /**
     * Cl� de la propri�t�e repr�sentant la source des donn�es.
     * Cette cl� peut �tre utilis�e avec {@link #getProperty}.
     */
    public static final Key SOURCE = new Key("Sources", null, null);

    /**
     * Cl� de la propri�t�e repr�sentant le fuseau horaire de
     * la base de donn�es. Cette cl� peut �tre utilis�e avec
     * {@link #getProperty}.
     */
    public static final Key TIMEZONE = new Key("TimeZone", null, null);

    /**
     * Retourne une des propri�t�e de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    public abstract String getProperty(final Key name) throws RemoteException;

    /**
     * Retourne le fuseau horaire des dates exprim�es dans cette base de donn�es.
     */
    public abstract TimeZone getTimeZone() throws RemoteException;

    /**
     * Ferme la connection et lib�re les ressources utilis�es par cette base de donn�es.
     * Appelez cette m�thode lorsque vous n'aurez plus besoin de consulter cette base.
     *
     * @throws RemoteException si un probl�me est survenu lors de la disposition des ressources.
     */
    public abstract void close() throws IOException;

    /**
     * Une propri�t�s � utiliser lors de la connection � une base de donn�es.
     */
    public static final class Key implements Serializable {
        /**
         * Pour compatibilit� entre diff�rentes versions.
         */
        private static final long serialVersionUID = 4719725873634041733L;

        /**
         * Ensemble des cl�s d�j� cr��s.
         */
        private static final Map<String,Key> POOL = new HashMap<String,Key>();

        /**
         * Nom de la propri�t�.
         */
        final String name;
        
        /**
         * Valeur par defaut de la propri�t�, si celle ci n'est pas d�finie.
         */
        final transient String defaultValue;
        
        /**
         * Description de la propri�t�.
         */
        final transient InternationalString description;
    
        /**
         * Construit une nouvelle cl�.
         *
         * @param name          Nom de la propri�t�.
         * @param description   Description de la propri�t�.
         * @param defaultValue  Valeur par defaut de la propri�t�, si celle ci n'est pas d�finie.
         */
        public Key(final String name, final InternationalString description, final String defaultValue) {
            this.name         = name;
            this.defaultValue = defaultValue;
            this.description  = description;
            synchronized (POOL) {
                if (!equals(POOL.put(name, this))) {
                    throw new IllegalStateException("Doublon dans les noms de cl�s.");
                }
            }
        }

        /**
         * Retourne l'instance � utiliser apr�s une lecture binaire
         * (habituellement � des fins de RMI).
         */
        protected Object readResolve() throws ObjectStreamException {
            synchronized (POOL) {
                final Object r = POOL.get(name);
                if (r != null) {
                    return r;
                }
            }
            throw new InvalidObjectException("Cl� inconnue: "+name);
        }
    }    
}
