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
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table {
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
     * Retourne la liste des param�tres disponibles. Ces param�tres peuvent
     * �tre sp�cifi� en argument � la m�thode {@link #setParameter}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public String[] getAvailableParameters() throws SQLException;

    /**
     * D�finit le param�tre examin�e par cette table. Le param�tre doit �tre un nom
     * de la table "Param�tres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Le param�tre � d�finir (exemple: "SST").
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract void setParameter(final String parameter) throws SQLException;

    /**
     * D�finit la position relative sur la ligne de p�che o� l'on veut les valeurs.
     * Les principales valeurs permises sont {@link #START_POINT}, {@link #CENTER}
     * et {@link #END_POINT}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract void setPosition(final int position) throws SQLException;

    /**
     * D�finit le d�calage de temps (en jours). La valeur par d�faut est 0.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract void setTimeLag(final int timeLag) throws SQLException;

    /**
     * Retourne le param�tre correspondant � une capture. Cette m�thode retourne la valeur
     * de la colonne <code>column</code> (sp�cifi�e lors de la construction) � la ligne qui
     * r�pond aux crit�res suivants:
     * <ul>
     *   <li>La capture est l'argument <code>capture</code> sp�cifi� � cette m�thode.</li>
     *   <li>Le nom du param�tre ("SST", "CHL", etc.) est celui qui a �t� sp�cifi� lors du
     *       dernier appel de {@link #setParameter}.</li>
     *   <li>La position ({@link #START_POINT}, {@link #CENTER}, {@link #END_POINT}, etc.)
     *       est celle qui a �t� sp�cifi�e lors du dernier appel de {@link #setPosition}.</li>
     *   <li>L'�cart de temps �tre la p�che et la mesure environnementale est celui qui a
     *       �t� sp�cifi� lors du dernier appel de {@link #setTimeLag}.</li>
     * </ul>
     *
     * @param  capture La capture.
     * @param  value Valeur du param�tre.
     * @throws SQLException si un probl�me est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract float get(final CatchEntry capture) throws SQLException;

    /**
     * Met � jour le param�tre correspondant � une capture. Cette m�thode met � jour la colonne
     * <code>column</code> (sp�cifi�e lors de la construction) de la ligne qui r�pond aux m�mes
     * crit�res que pour la m�thode {@link #get}, � quelques exceptions pr�s:
     * <ul>
     *   <li>Si la capture a �t� prise � un seul point (c'est-�-dire si {@link CatchEntry#getShape}
     *       retourne <code>null</code>), alors cette m�thode met � jour la ligne correspondant �
     *       la position {@link #CENTER}, quelle que soit la position sp�cifi�e lors du dernier
     *       appel de {@link #setPosition}.</li>
     * </ul>
     *
     * @param  capture La capture.
     * @param  value La valeur du param�tre. Si cette valeur est <code>NaN</code>,
     *         alors cette m�thode ne fait rien. L'ancien param�tre environnemental
     *         sera conserv�.
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public abstract void set(final CatchEntry capture, final float value) throws SQLException;

    /**
     * Met � jour le param�tre correspondant � une capture. Cette m�thode est similaire �
     * {@link #set(CatchEntry, float)}, except� que l'�cart de temps sera calcul�e � partir
     * de la date sp�cifi�e. Ce d�calage sera utilis� � la place de la derni�re valeur sp�cifi�e
     * � {@link #setTimeLag}.
     *
     * @param  capture La capture.
     * @param  value La valeur du param�tre.
     * @param  time La date � laquelle a �t� �valu�e la valeur <code>value</code>.
     *         Si cet argument est non-nul, alors l'�cart de temps entre cette date
     *         et la date de la capture sera calcul�e et utilis� � la place de la valeur
     *         sp�cifi�e lors du dernier appel de {@link #setTimeLag}.
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public abstract void set(final CatchEntry capture, final float value, final Date time) throws SQLException;
}
