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

// Base de donn�es
import java.sql.Driver;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

// Divers
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Classe de base des connections vers une base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class SQLDataBase implements DataBase {
    /**
     * Le nom de la classe du dernier pilote charg�.
     * En g�n�ral, une application ne chargera qu'un seul pilote.
     */
    private static String loadedDriver;

    /**
     * Source de la base de donn�es.
     */
    private final String source;

    /**
     * Connection vers la base de donn�es. Cette connection
     * est �tablie au moment de la construction de cet objet.
     */
    protected final Connection connection;

    /**
     * Fuseau horaire des dates inscrites dans la base de donn�es.
     * Cette information est utilis�e pour convertir en heure GMT
     * les dates apparaissant dans la base de donn�es.
     */
    protected final TimeZone timezone;

    /**
     * Ouvre une connection vers une base de donn�es.
     *
     * @param  url Protocole et nom de la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    protected SQLDataBase(final String url, final TimeZone timezone) throws SQLException {
        this.connection = DriverManager.getConnection(url);
        this.timezone   = timezone;
        this.source     = url;
    }

    /**
     * Ouvre une connection vers une base de donn�es.
     *
     * @param  url Protocole et nom de la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @param  user Nom d'utilisateur de la base de donn�es.
     * @param  password Mot de passe.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    protected SQLDataBase(final String url,  final TimeZone timezone,
                          final String user, final String   password)
            throws SQLException
    {
        this.connection = DriverManager.getConnection(url, user, password);
        this.timezone   = timezone;
        this.source     = url;
    }

    /**
     * Retourne une des propri�t�e de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    public String getProperty(final String name) {
        if (name != null) {
            if (name.equalsIgnoreCase(SOURCE))   return source;
            if (name.equalsIgnoreCase(TIMEZONE)) return timezone.getID();
        }
        return null;
    }

    /**
     * Retourne le fuseau horaire des dates
     * exprim�es dans cette base de donn�es.
     */
    public TimeZone getTimeZone() {
        return (TimeZone) timezone.clone();
    }

    /**
     * Ferme la connection avec la base de donn�es.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la fermeture de la connection.
     */
    public synchronized void close() throws SQLException {
        connection.close();
    }

    // PAS DE 'finalize'!!! Des connections sont peut-�tre encore utilis�es par
    // des tables.  On laissera JDBC fermer lui-m�me les connections lorsque le
    // ramasse-miettes d�tectera qu'elles ne sont plus utilis�es.

    /**
     * Tente de charger un pilote JDBC, s'il est disponible. Cette m�thode
     * retourne normalement m�me si le chargement du pilote a �chou�.
     *
     * @param  driverClassName Le nom de la classe du pilote JDBC.
     * @return Un enregistrement pour le journal. Cet enregistrement contient
     *         un message indiquant que le pilote a �t� charg� (avec le num�ro
     *         de version du pilote), ou que son chargement a �chou�.
     */
    protected static LogRecord loadDriver(final String driverClassName) {
        final Level level = driverClassName.equals(loadedDriver) ? Level.FINEST : Level.CONFIG;
        LogRecord record;
        try {
            final Driver driver = (Driver)Class.forName(driverClassName).newInstance();
            record = Resources.getResources(null).getLogRecord(level, ResourceKeys.LOADED_JDBC_DRIVER_$3);
            record.setParameters(new Object[] {
                driver.getClass().getName(),
                new Integer(driver.getMajorVersion()),
                new Integer(driver.getMinorVersion())
            });
            loadedDriver = driverClassName;
        } catch (Exception exception) {
            record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
            record.setThrown(exception);
        }
        record.setSourceClassName("DataBase");
        record.setSourceMethodName("<init>");
        return record;
    }
}
