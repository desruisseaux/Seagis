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

// Base de données
import java.sql.Driver;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

// Divers
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import fr.ird.resources.gui.ResourceKeys;
import fr.ird.resources.gui.Resources;


/**
 * Connexion vers une base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class DataBase
{
    /**
     * The level for logging SELECT instructions.
     */
    public static final Level SQL_SELECT = SQLLevel.SQL_SELECT;

    /**
     * The level for logging UPDATE instructions.
     */
    public static final Level SQL_UPDATE = SQLLevel.SQL_UPDATE;

    /**
     * Clé de la propriétée représentant le pilote JDBC
     * à utiliser pour se connecter à la base de données.
     * Cette clé peut être utilisée avec {@link #getProperty}.
     */
    public static final String DRIVER = "Driver";

    /**
     * Clé de la propriétée représentant la source des données.
     * Cette clé peut être utilisée avec {@link #getProperty}.
     */
    public static final String SOURCE = "Sources";

    /**
     * Clé de la propriétée représentant le fuseau horaire de
     * la base de données. Cette clé peut être utilisée avec
     * {@link #getProperty}.
     */
    public static final String TIMEZONE = "TimeZone";

    /**
     * Source de la base de données.
     */
    private final String source;

    /**
     * Connection vers la base de données. Cette connection
     * est établie au moment de la construction de cet objet.
     */
    protected final Connection connection;

    /**
     * Fuseau horaire des dates inscrites dans la base de données.
     * Cette information est utilisée pour convertir en heure GMT
     * les dates apparaissant dans la base de données.
     */
    protected final TimeZone timezone;

    /**
     * Ouvre une connection avec une base de données.
     *
     * @param  url Protocole et nom de la base de données.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates apparaissant dans la base de données.
     * @throws SQLException Si on n'a pas pu se connecter à la base de données.
     */
    protected DataBase(final String url, final TimeZone timezone) throws SQLException
    {
        this.connection = DriverManager.getConnection(url);
        this.timezone   = timezone;
        this.source     = url;
    }

    /**
     * Ouvre une connection avec une base de données.
     *
     * @param  url Protocole et nom de la base de données.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates apparaissant dans la base de données.
     * @param  user Nom d'utilisateur de la base de données.
     * @param  password Mot de passe.
     * @throws SQLException Si on n'a pas pu se connecter à la base de données.
     */
    protected DataBase(final String url, final TimeZone timezone, final String user, final String password) throws SQLException
    {
        this.connection = DriverManager.getConnection(url, user, password);
        this.timezone   = timezone;
        this.source     = url;
    }

    /**
     * Retourne une des propriétée de la base de données. La clé <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette méthode retourne <code>null</code> si la propriété
     * demandée n'est pas définie.
     */
    public String getProperty(final String name)
    {
        if (name!=null)
        {
            if (name.equalsIgnoreCase(SOURCE))   return source;
            if (name.equalsIgnoreCase(TIMEZONE)) return timezone.getID();
        }
        return null;
    }

    /**
     * Retourne le fuseau horaire des dates
     * exprimées dans cette base de données.
     */
    public TimeZone getTimeZone()
    {return (TimeZone) timezone.clone();}

    /**
     * Ferme la connection avec la base de données.
     * @throws SQLException si un problème est survenu
     *         lors de la fermeture de la connection.
     */
    public synchronized void close() throws SQLException
    {connection.close();}

    // PAS DE 'finalize'!!! Des connections sont peut-être encore utilisées par
    // des tables.  On laissera JDBC fermer lui-même les connections lorsque le
    // ramasse-miettes détectera qu'elles ne sont plus utilisées.

    /**
     * Tente de charger un pilote JDBC, s'il est disponible. Cette méthode
     * retourne normalement même si le chargement du pilote a échoué.
     *
     * @param  driverClassName Le nom de la classe du pilote JDBC.
     * @return Un enregistrement pour le journal. Cet enregistrement contient
     *         un message indiquant que le pilote a été chargé (avec le numéro
     *         de version du pilote), ou que son chargement a échoué.
     */
    protected static LogRecord loadDriver(final String driverClassName)
    {
        LogRecord record;
        try
        {
            final Driver driver = (Driver)Class.forName(driverClassName).newInstance();
            record = Resources.getResources(null).getLogRecord(Level.CONFIG, ResourceKeys.LOADED_JDBC_DRIVER_$3);
            record.setParameters(new Object[]
            {
                driver.getClass().getName(),
                new Integer(driver.getMajorVersion()),
                new Integer(driver.getMinorVersion())
            });
        }
        catch (Exception exception)
        {
            record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
            record.setThrown(exception);
        }
        record.setSourceClassName("DataBase");
        record.setSourceMethodName("<init>");
        return record;
    }
}
