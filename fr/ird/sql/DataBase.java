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
package fr.ird.sql;

// Base de donn�es
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
 * Connexion vers une base de donn�es.
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
     * Cl� de la propri�t�e repr�sentant le pilote JDBC
     * � utiliser pour se connecter � la base de donn�es.
     * Cette cl� peut �tre utilis�e avec {@link #getProperty}.
     */
    public static final String DRIVER = "Driver";

    /**
     * Cl� de la propri�t�e repr�sentant la source des donn�es.
     * Cette cl� peut �tre utilis�e avec {@link #getProperty}.
     */
    public static final String SOURCE = "Sources";

    /**
     * Cl� de la propri�t�e repr�sentant le fuseau horaire de
     * la base de donn�es. Cette cl� peut �tre utilis�e avec
     * {@link #getProperty}.
     */
    public static final String TIMEZONE = "TimeZone";

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
     * Ouvre une connection avec une base de donn�es.
     *
     * @param  url Protocole et nom de la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    protected DataBase(final String url, final TimeZone timezone) throws SQLException
    {
        this.connection = DriverManager.getConnection(url);
        this.timezone   = timezone;
        this.source     = url;
    }

    /**
     * Ouvre une connection avec une base de donn�es.
     *
     * @param  url Protocole et nom de la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @param  user Nom d'utilisateur de la base de donn�es.
     * @param  password Mot de passe.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    protected DataBase(final String url, final TimeZone timezone, final String user, final String password) throws SQLException
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
     * exprim�es dans cette base de donn�es.
     */
    public TimeZone getTimeZone()
    {return (TimeZone) timezone.clone();}

    /**
     * Ferme la connection avec la base de donn�es.
     * @throws SQLException si un probl�me est survenu
     *         lors de la fermeture de la connection.
     */
    public synchronized void close() throws SQLException
    {connection.close();}

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
