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

// J2SE et JAI
import java.rmi.RemoteException;
import java.util.Set;
import java.util.Date;
import java.util.Collection;
//import java.sql.SQLException;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.cs.CoordinateSystem;

// Seagis
import fr.ird.database.Table;
import fr.ird.animat.Species;


/**
 * Interface interrogeant la base de données pour obtenir la liste des échantillons
 * qu'elle contient. Ces échantillons pourront être sélectionnées dans une certaine
 * région géographique et à certaines dates.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleTable extends Table {
    /**
     * Retourne l'ensemble des espèces comprises dans la requête de cette table.
     *
     * @throws RemoeteException si un accès au catalogue était nécessaire et a échoué.
     */
    public abstract Set<Species> getSpecies() throws RemoteException;

    /**
     * Spécifie l'ensemble des espèces à prendre en compte lors des interrogations de
     * la base de données. Les objets {@link SampleEntry} retournés par cette table ne
     * contiendront des informations que sur ces espèces, et la méthode {@link SampleEntry#getValue()}
     * (qui retourne la quantité totale d'individus observés ou capturés) ignorera toute espèce qui
     * n'apparait pas dans l'ensemble <code>species</code>.
     *
     * @param species Ensemble des espèces à prendre en compte.
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public void setSpecies(final Set<Species> species) throws RemoteException;

    /**
     * Retourne le système de coordonnées utilisées
     * pour les positions de pêches dans cette table.
     *
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract CoordinateSystem getCoordinateSystem() throws RemoteException;

    /**
     * Retourne les coordonnées géographiques de la région des échantillons. Cette région
     * ne sera pas plus grande que la région qui a été spécifiée lors du dernier appel
     * de la méthode {@link #setGeographicArea}.  Elle peut toutefois être plus petite
     * de façon à n'englober que les échantillons présents dans la base de données.
     *
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract Rectangle2D getGeographicArea() throws RemoteException;

    /**
     * Définit les coordonnées géographiques de la région dans laquelle on veut rechercher des
     * échantillons. Les coordonnées doivent être exprimées en degrés de longitude et de latitude
     * selon l'ellipsoïde WGS&nbsp;1984. Tous les échantillons qui interceptent cette région seront
     * pris en compte lors du prochain appel de {@link #getEntries}.
     *
     * @param  geographicArea Coordonnées géographiques de la région, en degrés de longitude et de latitude.
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract void setGeographicArea(final Rectangle2D geographicArea) throws RemoteException;

    /**
     * Retourne la plage de dates des échantillons. Cette plage de dates ne sera pas plus grande que
     * la plage de dates spécifiée lors du dernier appel de la méthode {@link #setTimeRange}. Elle
     * peut toutefois être plus petite de façon à n'englober que les données des échantillons
     * présentes dans la base de données.
     *
     * @param  La plage de dates des données de pêches. Cette plage sera constituée d'objets {@link Date}.
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract Range getTimeRange() throws RemoteException;

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données des échantillons.
     * Tous les échantillons qui interceptent cette plage de temps seront pris en compte lors
     * du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des données.
     *         Cette plage doit être constituée d'objets {@link Date}.
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract void setTimeRange(final Range timeRange) throws RemoteException;

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données des échantillons.
     * Tous les échantillons qui interceptent cette plage de temps seront pris en compte lors
     * du prochain appel de {@link #getEntries}.
     *
     * @param  startTime Date de début (inclusive) de la période d'intérêt.
     * @param  startTime Date de fin   (inclusive) de la période d'intérêt.
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws RemoteException;

    /**
     * Retourne la plage de valeurs des échantillons d'intérêt.
     * Il peut s'agit par exemple de la quantité de poissons pêchés
     * en tonnes ou en nombre d'individus.
     */
    public abstract Range getValueRange() throws RemoteException;

    /**
     * Définit la plage de valeurs d'échantillons d'intérêt. Seules les échantillons dont la valeur
     * (le tonnage ou le nombre d'individus, dépendament du type de pêche) est compris dans cette
     * plage seront retenus.
     */
    public abstract void setValueRange(final Range valueRange) throws RemoteException;

    /**
     * Définit la plage de valeurs des échantillons d'intérêt. Cette méthode est équivalente
     * à {@link #setValueRange(Range)}.
     *
     * @param minimum Valeur minimale, inclusif.
     * @param maximum Valeur maximale, inclusif.
     */
    public abstract void setValueRange(double minimum, final double maximum) throws RemoteException;

    /**
     * Retourne la liste des échantillons connus dans la région et dans la plage de
     * dates préalablement sélectionnées. Ces plages auront été spécifiées à l'aide
     * des différentes méthodes <code>set...</code> de cette classe. Cette méthode
     * ne retourne jamais <code>null</code>, mais peut retourner un ensemble vide.
     *
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public abstract Collection<SampleEntry> getEntries() throws RemoteException;

    /**
     * Définie une valeur réelle pour un échantillon spécifié. Cette méthode peut être utilisée
     * pour mettre à jour certaine informations relatives à l'échantillon. L'échantillon spécifié
     * doit exister dans la base de données.
     *
     * @param sample     Echantillon à mettre à jour. Cetargument définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de l'échantillon
     *                   <code>sample</code>, colonne <code>columnName</code>.
     * @throws RemoteException si l'échantillon spécifié n'existe pas, ou si la mise à jour
     *                   du catalogue a échoué pour une autre raison.
     */
    public abstract void setValue(final SampleEntry sample, final String columnName, final float value) throws RemoteException;

    /**
     * Définie une valeur booléenne pour un échantillon spécifié. Cette méthode peut être utilisée
     * pour mettre à jour certaine informations relatives à l'échantillon. L'échantillon spécifié
     * doit exister dans la base de données.
     *
     * @param sample     Echantillon à mettre à jour. Cetargument définit la ligne à mettre à jour.
     * @param columnName Nom de la colonne à mettre à jour.
     * @param value      Valeur à inscrire dans la base de données à la ligne de l'échantillon
     *                   <code>sample</code>, colonne <code>columnName</code>.
     * @throws RemoteException si l'échantillon spécifié n'existe pas, ou si la mise à jour
     *                   du catalogue a échoué pour une autre raison.
     */
    public abstract void setValue(final SampleEntry sample, final String columnName, final boolean value) throws RemoteException;
}
