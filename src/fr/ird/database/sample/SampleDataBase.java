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
package fr.ird.database.sample;

// J2SE
import java.util.Set;
import java.util.Collection;
import java.sql.SQLException;
import java.util.logging.Logger;

// Seagis
import fr.ird.animat.Species;
import fr.ird.database.DataBase;
import fr.ird.database.coverage.SeriesTable;


/**
 * Connection avec la base de donn�es des �chantillons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleDataBase extends DataBase {
    /**
     * Journal des �v�nements.
     *
     * @see #SQL_SELECT
     * @see #SQL_UPDATE
     */
    public static final Logger LOGGER = Logger.getLogger("fr.ird.database");

    /**
     * Retourne les esp�ces �num�r�s dans la base de donn�es.
     *
     * @return Ensemble des esp�ces r�pertori�es dans la base de donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     */
    public abstract Set<Species> getSpecies() throws SQLException;

    /**
     * Retourne la liste des param�tres environnementaux disponibles. Les param�tres
     * environnementaux sont repr�sent�s par des noms courts tels que "CHL" ou "SST".
     * Ces param�tres peuvent �tre sp�cifi�s en argument � la m�thode
     * {@link #getEnvironmentTable}.
     *
     * @param  series La table des s�ries � utiliser pour construire les {@link ParameterEntry}.
     *         Si cet argument est <code>null</code>, alors les objets {@link ParameterEntry} ne
     *         contiendront que les num�ros ID des s�ries, sans les autres informations tels que
     *         leur noms.
     * @return L'ensemble des param�tres environnementaux disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract Set<ParameterEntry> getParameters(SeriesTable series) throws SQLException;

    /**
     * Retourne la liste des op�rations disponibles. Les op�rations sont appliqu�es sur
     * des param�tres environnementaux. Par exemple les op�rations "valeur" et "sobel3"
     * correspondent � la valeur d'un param�tre environnemental et son gradient calcul�
     * par l'op�rateur de Sobel, respectivement. Ces op�rations peuvent �tre sp�cifi�s
     * en argument � la m�thode {@link #getEnvironmentTable}.
     *
     * @return L'ensemble des op�rations disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract Set<OperationEntry> getOperations() throws SQLException;

    /**
     * Retourne la liste des op�rations relatives disponibles.
     */
    public abstract Set<RelativePositionEntry> getRelativePositions() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des �chantillons de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @param  species Esp�ces d'int�r�t dans la table.
     * @return La table des �chantillons pour les esp�ces demand�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract SampleTable getSampleTable(final Collection<Species> species) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des �chantillons de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @param  species Code des esp�ces d'int�r�t dans la table (par exemple "SWO").
     * @return La table des �chantillons pour les esp�ces demand�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract SampleTable getSampleTable(final String[] species) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des �chantillons de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @param  species Esp�ces d'int�r�t dans la table (par exemple "SWO").
     * @return La table des �chantillons pour l'esp�ce demand�e.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract SampleTable getSampleTable(final String species) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des �chantillons de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @return La table des �chantillons pour toute les esp�ces r�pertori�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract SampleTable getSampleTable() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des param�tres environnementaux.
     * Ces param�tres peuvent �tre mesur�s aux coordonn�es spatio-temporelles des captures, ou
     * dans son voisinage (par exemple quelques jours avant ou apr�s la p�che). Chaque param�tre
     * appara�tra dans une colonne. Ces colonnes doivent �tre ajout�es en appelant la m�thode
     * {@link EnvironmentTable#addParameter} autant de fois que n�cessaire.
     *
     * @param  series La table des s�ries, ou <code>null</code> si aucune. Si cet argument est nul,
     *         alors les objets {@link ParameterEntry} ne contiendront que les num�ros ID des
     *         s�ries, sans les autres informations tels que leur nom. Cette table ne sera pas
     *         ferm�e par {@link EnvironmentTable#close}, puisqu'elle n'appartient pas � l'objet
     *         <code>EnvironmentTable</code>.
     * @return La table des param�tres environnementaux pour toute les captures.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public abstract EnvironmentTable getEnvironmentTable(SeriesTable series) throws SQLException;
}
