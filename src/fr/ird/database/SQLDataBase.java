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
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Classe de base des connections vers une base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class SQLDataBase extends UnicastRemoteObject implements DataBase {
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
     * Ouvre une connection vers une base de donn�es.
     *
     * @param  url Protocole et nom de la base de donn�es.
     * @param  propertyFile Fichier contenant les propri�t�s de connection � la base,
     *         ou <code>null</code> si aucun.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @throws IOException si le fichier de configuration existe mais n'a pas pu �tre ouvert.
     * @throws SQLException Si on n'a pas pu se connecter au catalogue.
     */
    protected SQLDataBase(final String url,
                          final File propertyFile,
                          final TimeZone timezone) throws IOException, SQLException
    {
        this(url, propertyFile, timezone, null, null);
    }

    /**
     * Ouvre une connection vers une base de donn�es.
     *
     * @param  url Protocole et nom de la base de donn�es.
     * @param  propertyFile Fichier contenant les propri�t�s de connection � la base,
     *         ou <code>null</code> si aucun.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates apparaissant dans la base de donn�es.
     * @param  user Nom d'utilisateur de la base de donn�es.
     * @param  password Mot de passe.
     * @throws IOException si le fichier de configuration existe mais n'a pas pu �tre ouvert.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    protected SQLDataBase(final String        url,
                          final File propertyFile,
                          final TimeZone timezone,
                          final String       user,
                          final String   password) throws IOException, SQLException
    {
        if (user!=null && user.trim().length()!=0) {
            this.connection = DriverManager.getConnection(url, user, password);
        } else {
            this.connection = DriverManager.getConnection(url);
        }
        this.timezone     = timezone;
        this.source       = url;
        this.propertyFile = propertyFile;
        if (propertyFile!=null && propertyFile.exists()) {
            final InputStream in = new BufferedInputStream(new FileInputStream(propertyFile));
            properties.loadFromXML(in);
            in.close();
        }
    }

    /**
     * Retourne le fuseau horaire des dates
     * exprim�es dans cette base de donn�es.
     */
    public TimeZone getTimeZone() throws RemoteException {
        return (TimeZone) timezone.clone();
    }

    /**
     * Retourne une des propri�t�e de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    public String getProperty(final Key key) throws RemoteException {
        if (key != null) {
            if (key.name.equalsIgnoreCase(SOURCE  .name)) return source;
            if (key.name.equalsIgnoreCase(TIMEZONE.name)) return timezone.getID();
            return properties.getProperty(key.name, key.defaultValue);
        }
        return null;
    }

    /**
     * Affecte une nouvelle valeur sous la cl� sp�cifi�e.
     *
     * @param key   La cl�.
     * @param value Nouvelle valeur.
     */
    public synchronized void setProperty(final Key key, final String value) {
        if (!value.equals(properties.setProperty(key.name, value))) {
            modified = true;
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
