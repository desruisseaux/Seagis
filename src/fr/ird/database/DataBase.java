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

// J2SE
import java.util.TimeZone;
import java.util.logging.Level;
import java.sql.SQLException;


/**
 * Interface de base des connections vers une base de donn�es.
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
     * Cl� de la propri�t�e repr�sentant le fuseau horaire de
     * la base de donn�es. Cette cl� peut �tre utilis�e avec
     * {@link #getProperty}.
     */
    //protected static final String DATABASE = "FileOfConfiguration";

    /**
     * Retourne une des propri�t�e de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    public abstract String getProperty(final String name);

    /**
     * Retourne une des propri�t�e de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link #DRIVER}, {@link #SOURCE} ou
     * {@link #TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    /*public abstract Configuration.Key getKey(final String name);*/

    /**
     * Retourne le fuseau horaire des dates exprim�es dans cette base de donn�es.
     */
    public abstract TimeZone getTimeZone();

    /**
     * Ferme la connection et lib�re les ressources utilis�es par cette base de donn�es.
     * Appelez cette m�thode lorsque vous n'aurez plus besoin de consulter cette base.
     *
     * @throws SQLException si un probl�me est survenu lors de la disposition des ressources.
     */
    public abstract void close() throws SQLException;
}
