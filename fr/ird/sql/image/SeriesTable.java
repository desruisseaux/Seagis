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
package fr.ird.sql.image;

// Divers
import fr.ird.sql.Table;
import java.sql.SQLException;
import javax.swing.tree.TreeModel;
import fr.ird.awt.tree.TreeNode;
import java.util.List;


/**
 * Connection vers une table des s�ries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les param�tres et op�rations qui forment les s�ries, et de placer ces informations dans une
 * arborescence avec des chemins de la forme "<code>param�tre/op�ration/s�rie/groupe</code>".
 *
 * @see ImageDataBase#getSeriesTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SeriesTable extends Table
{
    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les s�ries.
     */
    public static final int SERIES_LEAF = 3;

    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les groupes (apr�s les s�ries).
     */
    public static final int GROUP_LEAF = 4;

    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les cat�gories (apr�s les groupes).
     */
    public static final int CATEGORY_LEAF = 7;

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     *
     * @param  ID Num�ro identifiant la s�rie recherch�e.
     * @return La s�rie identifi�e par le num�ro ID, ou <code>null</code>
     *         si aucune s�rie de ce num�ro n'a �t� trouv�e.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     * @throws IllegalRecordException Si plusieurs s�ries portent le m�me ID.
     */
    public abstract SeriesEntry getSeries(final int ID) throws SQLException;

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     *
     * @param  name Nom de la s�rie recherch�e.
     * @return Une s�rie qui porte le nom <code>name</code>, ou <code>null</code>
     *         si aucune s�rie de ce nom n'a �t� trouv�e.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     * @throws IllegalRecordException Si plusieurs s�ries portent le m�me nom.
     */
    public abstract SeriesEntry getSeries(final String name) throws SQLException;

    /**
     * Retourne la liste des s�ries pr�sentes dans la base de donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public abstract List<SeriesEntry> getSeries() throws SQLException;

    /**
     * Retourne une arborescence qui pourra �tre affich�e dans une composante {@link javax.swing.JTree}.
     * Cette arborescence contiendra les param�tres, op�rations, s�ries et groupes trouv�s dans la base
     * de donn�es. Pour la plupart des noeuds de cette arborescence, la m�thode {@link TreeNode#getUserObject}
     * retournera un objet {@link Entry} (ou {@link SeriesEntry} pour les noeuds qui repr�sentent des
     * s�ries).
     *
     * @param  leafType Un des arguments {@link #SERIES_LEAF}, {@link #GROUP_LEAF} ou {@link #CATEGORY_LEAF}.
     * @return Arborescence des s�ries de la base de donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public abstract TreeModel getTree(final int leafType) throws SQLException;

    /**
     * Retourne le nombre d'images appartenant � la s�rie sp�cifi�e.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public abstract int getImageCount(final SeriesEntry series) throws SQLException;

    /**
     * Retourne le format d'un groupe d'images.
     *
     * @param  groupID Num�ro ID de la table <code>Groups</code> de la base de donn�es.
     * @return Le format utilis� par le groupe sp�cifi�, ou <code>null</code> si aucun
     *         groupe identifi� par l'ID specifi� n'a �t� trouv�.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public abstract FormatEntry getFormat(final int groupID) throws SQLException;
}
