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
package fr.ird.database.sql;

// Base de donn�es
import java.sql.Driver;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.TimeZone;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Seagis dependencies
import fr.ird.database.DataBase;
import fr.ird.database.ConfigurationKey;
import fr.ird.database.CatalogException;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Classe de base des connections vers une base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class AbstractDataBase extends UnicastRemoteObject implements DataBase {
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
     * Le nom de la classe du dernier pilote charg�.
     * En g�n�ral, une application ne chargera qu'un seul pilote.
     */
    private static String loadedDriver;

    /**
     * Source de la base de donn�es.
     */
    private final String source;

    /**
     * Connexion vers la base de donn�es. Cette connection
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
     * Le fichier dans lequel seront lu et �crit les propri�t�s.
     */
    private final File propertyFile;

    /**
     * Propri�t�s � utiliser pour extraire les valeurs du fichier de configuration.
     * Ces propri�t�s ont �t� lues � partir du fichier {@link #propertyFile}.
     */
    private final Properties properties = new Properties();

    /**
     * Indique si les propri�t�s ont �t� modifi�es.
     */
    private boolean modified;

    /**
     * Ouvre une connection vers une base de donn�es. Chacun des arguments � ce
     * constructeur peut �tre nul, auquel cas une valeur par d�faut sera utilis�e.
     *
     * @param  propertyFile Fichier contenant les propri�t�s de connection � la base.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @param  source Protocole et nom de la base de donn�es.
     * @param  user Nom d'utilisateur de la base de donn�es.
     * @param  password Mot de passe.
     * @throws IOException si le fichier de configuration existe mais n'a pas pu �tre ouvert.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    protected AbstractDataBase(File propertyFile,
                               TimeZone timezone,
                               String     source,
                               String       user,
                               String   password)
            throws IOException, SQLException
    {
        /*
         * Proc�de d'abord � la lecture du fichier de configuration,  afin de permettre
         * � la m�thode 'getProperty' de fonctionner. Cette derni�re sera utilis�e dans
         * les lignes suivantes, et risque aussi d'�tre surcharg�e.
         */
        if (propertyFile!=null && propertyFile.exists()) {
            final InputStream in = new BufferedInputStream(new FileInputStream(propertyFile));
            properties.loadFromXML(in);
            in.close();
        }
        this.propertyFile = propertyFile;
        if (timezone == null) {
            final String ID = getProperty(TIMEZONE);
            if (ID != null) {
                timezone = TimeZone.getTimeZone(ID);
            }
        }
        this.timezone = timezone;
        if (source == null) {
            source = getProperty(SOURCE);
        }
        this.source = source;
        if (user == null) {
            user = getProperty(USER);
        }
        if (password == null) {
            password = getProperty(PASSWORD);
        }
        if (user!=null && user.trim().length()!=0) {
            this.connection = DriverManager.getConnection(source, user, password);
        } else {
            this.connection = DriverManager.getConnection(source);
        }
    }

    /**
     * Retourne le fuseau horaire des dates
     * exprim�es dans cette base de donn�es.
     */
    public TimeZone getTimeZone() {
        return (TimeZone) timezone.clone();
    }

    /**
     * Retourne une des propri�t�e de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    public String getProperty(final ConfigurationKey key) {
        String value = properties.getProperty(key.name, key.defaultValue);
        if (value == null) {
            if (key.equals(SOURCE)) {
                return source;
            }
            if (key.equals(TIMEZONE)) {
                return timezone.getID();
            }
        }
        return value;
    }

    /**
     * Affecte une nouvelle valeur sous la cl� sp�cifi�e.
     *
     * @param key   La cl�.
     * @param value Nouvelle valeur, ou <code>null</code> pour r�tablir la propri�t�
     *              � sa valeur par d�faut.
     */
    public synchronized void setProperty(final ConfigurationKey key, final String value) {
        if (value != null) {
            if (!value.equals(properties.setProperty(key.name, value))) {
                modified = true;
            }
        } else {
            if (properties.remove(key.name) != null) {
                modified = true;
            }
        }
    }

    /**
     * Ferme la connection avec la base de donn�es.
     *
     * @throws RemoteException si un probl�me est survenu
     *         lors de la fermeture de la connection.
     */
    public synchronized void close() throws IOException {
        if (modified) {
            modified = false;
            if (propertyFile != null) {
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propertyFile));
                properties.storeToXML(out, "Settings for \""+source+'"', "ISO-8859-1");
                out.close();
            } else {
                // TODO: provide a localized message.
                getLogger().warning("Aucun fichier n'a �t� sp�cifi� pour la configuration. "+
                                    "Elle n'a donc pas �t� enregistr�e.");
            }
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    // PAS DE 'finalize'!!! Des connections sont peut-�tre encore utilis�es par
    // des tables.  On laissera JDBC fermer lui-m�me les connections lorsque le
    // ramasse-miettes d�tectera qu'elles ne sont plus utilis�es.

    /**
     * Retourne le logger � utiliser pour enregister d'�ventuels avertissements.
     */
    protected abstract Logger getLogger();

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
