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
package fr.ird.sql.fishery;

// Pêches et base de données
import fr.ird.sql.Table;
import fr.ird.animat.Species;
import java.sql.SQLException;

// Ensembles
import java.util.Set;
import java.util.List;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;


/**
 * Interface interrogeant la base de données pour obtenir la liste des pêches
 * qu'elle contient. Ces pêches pourront être sélectionnées dans une certaine
 * région géographique et à certaines dates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface CatchTable extends Table
{
    /**
     * Retourne l'ensemble des espèces comprises dans la requête de cette table.
     *
     * @throws SQLException si un accès à la base de données était nécessaire et a échoué.
     */
    public abstract Set<Species> getSpecies() throws SQLException;

    /**
     * Retourne les coordonnées géographiques de la région des captures.  Cette région
     * ne sera pas plus grande que la région qui a été spécifiée lors du dernier appel
     * de la méthode {@link #setGeographicArea}.  Elle peut toutefois être plus petite
     * de façon à n'englober que les données de pêches présentes dans la base de données.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract Rectangle2D getGeographicArea() throws SQLException;

    /**
     * Définit les coordonnées géographiques de la région dans laquelle on veut rechercher des pêches.
     * Les coordonnées doivent être exprimées en degrés de longitude et de latitude selon l'ellipsoïde
     * WGS&nbsp;1984. Toutes les pêches qui interceptent cette région seront prises en compte lors du
     * prochain appel de {@link #getEntries}.
     *
     * @param  geographicArea Coordonnées géographiques de la région, en degrés de longitude et de latitude.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setGeographicArea(final Rectangle2D geographicArea) throws SQLException;

    /**
     * Retourne la plage de dates des pêches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates spécifiée lors du dernier appel de la méthode {@link #setTimeRange}.
     * Elle peut toutefois être plus petite de façon à n'englober que les données de pêches
     * présentes dans la base de données.
     *
     * @param  La plage de dates des données de pêches. Cette plage sera constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract Range getTimeRange() throws SQLException;

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données de pêches.
     * Toutes les pêches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des données.
     *         Cette plage doit être constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setTimeRange(final Range timeRange) throws SQLException;

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données de pêches.
     * Toutes les pêches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws SQLException;

    /**
     * Retourne la liste des captures connues dans la région et dans la plage de
     * dates préalablement sélectionnées. Ces plages auront été spécifiées à l'aide
     * des différentes méthodes <code>set...</code> de cette classe. Cette méthode
     * ne retourne jamais <code>null</code>, mais peut retourner une liste de
     * longueur 0.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract List<CatchEntry> getEntries() throws SQLException;

    /**
     * Définie une valeur réelle pour une capture données.  Cette méthode peut être utilisée
     * pour mettre à jour certaine informations relatives à la capture. La capture spécifiée
     * doit exister dans la base de données.
     *
     * @param capture    Capture à mettre à jour. Cette capture définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture spécifiée n'existe pas, ou si la mise à jour
     *         de la base de données a échouée pour une autre raison.
     */
    public abstract void setValue(final CatchEntry capture, final String columnName, final float value) throws SQLException;

    /**
     * Définie une valeur booléenne pour une capture données. Cette méthode peut être utilisée
     * pour mettre à jour certaine informations relatives à la capture.   La capture spécifiée
     * doit exister dans la base de données.
     *
     * @param capture    Capture à mettre à jour. Cette capture définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture spécifiée n'existe pas, ou si la mise à jour
     *         de la base de données a échouée pour une autre raison.
     */
    public abstract void setValue(final CatchEntry capture, final String columnName, final boolean value) throws SQLException;
}
