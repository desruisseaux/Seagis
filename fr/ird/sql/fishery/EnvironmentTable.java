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

// Divers
import java.util.Date;
import java.sql.SQLException;
import fr.ird.sql.Table;


/**
 * Interface interrogeant ou modifiant un paramètre
 * de la base de données d'environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table {
    /**
     * Constante désignant la position de départ sur la ligne de pêche.
     */
    public static final int START_POINT = 0;

    /**
     * Constante désignant la position centrale sur la ligne de pêche.
     */
    public static final int CENTER = 50;

    /**
     * Constante désignant la position finale sur la ligne de pêche.
     */
    public static final int END_POINT = 100;

    /**
     * Retourne la liste des paramètres disponibles. Ces paramètres peuvent
     * être spécifié en argument à la méthode {@link #setParameter}.
     *
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public String[] getAvailableParameters() throws SQLException;

    /**
     * Définit le paramètre examinée par cette table. Le paramètre doit être un nom
     * de la table "Paramètres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Le paramètre à définir (exemple: "SST").
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract void setParameter(final String parameter) throws SQLException;

    /**
     * Définit la position relative sur la ligne de pêche où l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract void setPosition(final int position) throws SQLException;

    /**
     * Définit le décalage de temps (en jours). La valeur par défaut est 0.
     *
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract void setTimeLag(final int timeLag) throws SQLException;

    /**
     * Retourne le paramètre correspondant à une capture. Cette méthode retourne la valeur
     * de la colonne <code>column</code> (spécifiée lors de la construction) à la ligne qui
     * répond aux critères suivants:
     * <ul>
     *   <li>La capture est l'argument <code>capture</code> spécifié à cette méthode.</li>
     *   <li>Le nom du paramètre ("SST", "CHL", etc.) est celui qui a été spécifié lors du
     *       dernier appel de {@link #setParameter}.</li>
     *   <li>La position ({@link #START_POINT}, {@link #CENTER}, {@link #END_POINT}, etc.)
     *       est celle qui a été spécifiée lors du dernier appel de {@link #setPosition}.</li>
     *   <li>L'écart de temps être la pêche et la mesure environnementale est celui qui a
     *       été spécifié lors du dernier appel de {@link #setTimeLag}.</li>
     * </ul>
     *
     * @param  capture La capture.
     * @param  value Valeur du paramètre.
     * @throws SQLException si un problème est survenu lors de l'accès à la base de données.
     */
    public abstract float get(final CatchEntry capture) throws SQLException;

    /**
     * Met à jour le paramètre correspondant à une capture. Cette méthode met à jour la colonne
     * <code>column</code> (spécifiée lors de la construction) de la ligne qui répond aux mêmes
     * critères que pour la méthode {@link #get}, à quelques exceptions près:
     * <ul>
     *   <li>Si la capture a été prise à un seul point (c'est-à-dire si {@link CatchEntry#getShape}
     *       retourne <code>null</code>), alors cette méthode met à jour la ligne correspondant à
     *       la position {@link #CENTER}, quelle que soit la position spécifiée lors du dernier
     *       appel de {@link #setPosition}.</li>
     * </ul>
     *
     * @param  capture La capture.
     * @param  value La valeur du paramètre. Si cette valeur est <code>NaN</code>,
     *         alors cette méthode ne fait rien. L'ancien paramètre environnemental
     *         sera conservé.
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public abstract void set(final CatchEntry capture, final float value) throws SQLException;

    /**
     * Met à jour le paramètre correspondant à une capture. Cette méthode est similaire à
     * {@link #set(CatchEntry, float)}, excepté que l'écart de temps sera calculée à partir
     * de la date spécifiée. Ce décalage sera utilisé à la place de la dernière valeur spécifiée
     * à {@link #setTimeLag}.
     *
     * @param  capture La capture.
     * @param  value La valeur du paramètre.
     * @param  time La date à laquelle a été évaluée la valeur <code>value</code>.
     *         Si cet argument est non-nul, alors l'écart de temps entre cette date
     *         et la date de la capture sera calculée et utilisé à la place de la valeur
     *         spécifiée lors du dernier appel de {@link #setTimeLag}.
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public abstract void set(final CatchEntry capture, final float value, final Date time) throws SQLException;
}
