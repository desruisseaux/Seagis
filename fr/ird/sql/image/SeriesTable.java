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
package fr.ird.sql.image;

// Divers
import fr.ird.sql.Table;
import java.sql.SQLException;
import javax.swing.tree.TreeModel;
import fr.ird.awt.tree.TreeNode;
import java.util.List;


/**
 * Connection vers une table des séries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les paramètres et opérations qui forment les séries, et de placer ces informations dans une
 * arborescence avec des chemins de la forme "<code>paramètre/opération/série/groupe</code>".
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
     * ne doit pas aller plus loin que les séries.
     */
    public static final int SERIES_LEAF = 3;

    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les groupes (après les séries).
     */
    public static final int GROUP_LEAF = 4;

    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les catégories (après les groupes).
     */
    public static final int CATEGORY_LEAF = 7;

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     *
     * @param  ID Numéro identifiant la série recherchée.
     * @return La série identifiée par le numéro ID, ou <code>null</code>
     *         si aucune série de ce numéro n'a été trouvée.
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     * @throws IllegalRecordException Si plusieurs séries portent le même ID.
     */
    public abstract SeriesEntry getSeries(final int ID) throws SQLException;

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     *
     * @param  name Nom de la série recherchée.
     * @return Une série qui porte le nom <code>name</code>, ou <code>null</code>
     *         si aucune série de ce nom n'a été trouvée.
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     * @throws IllegalRecordException Si plusieurs séries portent le même nom.
     */
    public abstract SeriesEntry getSeries(final String name) throws SQLException;

    /**
     * Retourne la liste des séries présentes dans la base de données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public abstract List<SeriesEntry> getSeries() throws SQLException;

    /**
     * Retourne une arborescence qui pourra être affichée dans une composante {@link javax.swing.JTree}.
     * Cette arborescence contiendra les paramètres, opérations, séries et groupes trouvés dans la base
     * de données. Pour la plupart des noeuds de cette arborescence, la méthode {@link TreeNode#getUserObject}
     * retournera un objet {@link Entry} (ou {@link SeriesEntry} pour les noeuds qui représentent des
     * séries).
     *
     * @param  leafType Un des arguments {@link #SERIES_LEAF}, {@link #GROUP_LEAF} ou {@link #CATEGORY_LEAF}.
     * @return Arborescence des séries de la base de données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public abstract TreeModel getTree(final int leafType) throws SQLException;

    /**
     * Retourne le nombre d'images appartenant à la série spécifiée.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public abstract int getImageCount(final SeriesEntry series) throws SQLException;

    /**
     * Retourne le format d'un groupe d'images.
     *
     * @param  groupID Numéro ID de la table <code>Groups</code> de la base de données.
     * @return Le format utilisé par le groupe spécifié, ou <code>null</code> si aucun
     *         groupe identifié par l'ID specifié n'a été trouvé.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public abstract FormatEntry getFormat(final int groupID) throws SQLException;
}
