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
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table
{
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
     * Définit la position relative sur la ligne de pêche où l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract void setPosition(final int position) throws SQLException;

    /**
     * Retourne le paramètre correspondant à une capture.
     *
     * @param  capture La capture.
     * @param  value Valeur du paramètre.
     * @throws SQLException si un problème est survenu lors de l'accès à la base de données.
     */
    public abstract float get(final CatchEntry capture) throws SQLException;

    /**
     * Met à jour le paramètre correspondant à une capture.
     * Rien ne sera fait si <code>value</code> et une valeur NaN.
     *
     * @param  capture La capture.
     * @param  value La valeur du paramètre.
     * @param  time La date à laquelle a été évaluée la valeur <code>value</code>.
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public abstract void set(final CatchEntry capture, final float value, final Date time) throws SQLException;
}
