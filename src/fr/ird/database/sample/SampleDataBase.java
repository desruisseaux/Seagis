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
 * Connection avec la base de données des échantillons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleDataBase extends DataBase {
    /**
     * Journal des évènements.
     *
     * @see #SQL_SELECT
     * @see #SQL_UPDATE
     */
    public static final Logger LOGGER = Logger.getLogger("fr.ird.database");

    /**
     * Retourne les espèces énumérés dans la base de données.
     *
     * @return Ensemble des espèces répertoriées dans la base de données.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public abstract Set<Species> getSpecies() throws SQLException;

    /**
     * Retourne la liste des paramètres environnementaux disponibles. Les paramètres
     * environnementaux sont représentés par des noms courts tels que "CHL" ou "SST".
     * Ces paramètres peuvent être spécifiés en argument à la méthode
     * {@link #getEnvironmentTable}.
     *
     * @param  series La table des séries à utiliser pour construire les {@link ParameterEntry}.
     *         Si cet argument est <code>null</code>, alors les objets {@link ParameterEntry} ne
     *         contiendront que les numéros ID des séries, sans les autres informations tels que
     *         leur noms.
     * @return L'ensemble des paramètres environnementaux disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<ParameterEntry> getParameters(SeriesTable series) throws SQLException;

    /**
     * Retourne la liste des opérations disponibles. Les opérations sont appliquées sur
     * des paramètres environnementaux. Par exemple les opérations "valeur" et "sobel3"
     * correspondent à la valeur d'un paramètre environnemental et son gradient calculé
     * par l'opérateur de Sobel, respectivement. Ces opérations peuvent être spécifiés
     * en argument à la méthode {@link #getEnvironmentTable}.
     *
     * @return L'ensemble des opérations disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<OperationEntry> getOperations() throws SQLException;

    /**
     * Retourne la liste des opérations relatives disponibles.
     */
    public abstract Set<RelativePositionEntry> getRelativePositions() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des échantillons de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @param  species Espèces d'intérêt dans la table.
     * @return La table des échantillons pour les espèces demandées.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract SampleTable getSampleTable(final Collection<Species> species) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des échantillons de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @param  species Code des espèces d'intérêt dans la table (par exemple "SWO").
     * @return La table des échantillons pour les espèces demandées.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract SampleTable getSampleTable(final String[] species) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des échantillons de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @param  species Espèces d'intérêt dans la table (par exemple "SWO").
     * @return La table des échantillons pour l'espèce demandée.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract SampleTable getSampleTable(final String species) throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des échantillons de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link SampleTable#close}.
     *
     * @return La table des échantillons pour toute les espèces répertoriées.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract SampleTable getSampleTable() throws SQLException;

    /**
     * Construit et retourne un objet qui interrogera la table des paramètres environnementaux.
     * Ces paramètres peuvent être mesurés aux coordonnées spatio-temporelles des captures, ou
     * dans son voisinage (par exemple quelques jours avant ou après la pêche). Chaque paramètre
     * apparaîtra dans une colonne. Ces colonnes doivent être ajoutées en appelant la méthode
     * {@link EnvironmentTable#addParameter} autant de fois que nécessaire.
     *
     * @param  series La table des séries, ou <code>null</code> si aucune. Si cet argument est nul,
     *         alors les objets {@link ParameterEntry} ne contiendront que les numéros ID des
     *         séries, sans les autres informations tels que leur nom. Cette table ne sera pas
     *         fermée par {@link EnvironmentTable#close}, puisqu'elle n'appartient pas à l'objet
     *         <code>EnvironmentTable</code>.
     * @return La table des paramètres environnementaux pour toute les captures.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public abstract EnvironmentTable getEnvironmentTable(SeriesTable series) throws SQLException;
}
