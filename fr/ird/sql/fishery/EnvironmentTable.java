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
package fr.ird.sql.fishery;

// Divers
import java.util.Date;
import java.sql.SQLException;
import fr.ird.sql.Table;


/**
 * Interface interrogeant ou modifiant un param�tre
 * de la base de donn�es d'environnement.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table
{
    /**
     * Constante d�signant la position de d�part sur la ligne de p�che.
     */
    public static final int START_POINT = 0;

    /**
     * Constante d�signant la position centrale sur la ligne de p�che.
     */
    public static final int CENTER = 50;

    /**
     * Constante d�signant la position finale sur la ligne de p�che.
     */
    public static final int END_POINT = 100;

    /**
     * D�finit la position relative sur la ligne de p�che o� l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract void setPosition(final int position) throws SQLException;

    /**
     * Retourne le param�tre correspondant � une capture.
     *
     * @param  capture La capture.
     * @param  value Valeur du param�tre.
     * @throws SQLException si un probl�me est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract float get(final CatchEntry capture) throws SQLException;

    /**
     * Met � jour le param�tre correspondant � une capture.
     * Rien ne sera fait si <code>value</code> et une valeur NaN.
     *
     * @param  capture La capture.
     * @param  value La valeur du param�tre.
     * @param  time La date � laquelle a �t� �valu�e la valeur <code>value</code>.
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public abstract void set(final CatchEntry capture, final float value, final Date time) throws SQLException;
}
