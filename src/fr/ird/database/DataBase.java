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
package fr.ird.database;

// J2SE
import java.util.TimeZone;
import java.util.logging.Level;
import java.sql.SQLException;


/**
 * Interface de base des connections vers une base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface DataBase {
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
     * Clé de la propriétée représentant le fuseau horaire de
     * la base de données. Cette clé peut être utilisée avec
     * {@link #getProperty}.
     */
    //protected static final String DATABASE = "FileOfConfiguration";

    /**
     * Retourne une des propriétée de la base de données. La clé <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette méthode retourne <code>null</code> si la propriété
     * demandée n'est pas définie.
     */
    public abstract String getProperty(final String name);

    /**
     * Retourne une des propriétée de la base de données. La clé <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette méthode retourne <code>null</code> si la propriété
     * demandée n'est pas définie.
     */
    /*public abstract Configuration.Key getKey(final String name);*/

    /**
     * Retourne le fuseau horaire des dates exprimées dans cette base de données.
     */
    public abstract TimeZone getTimeZone();

    /**
     * Ferme la connection et libère les ressources utilisées par cette base de données.
     * Appelez cette méthode lorsque vous n'aurez plus besoin de consulter cette base.
     *
     * @throws SQLException si un problème est survenu lors de la disposition des ressources.
     */
    public abstract void close() throws SQLException;
}
