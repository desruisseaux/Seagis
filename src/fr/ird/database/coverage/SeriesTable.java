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
 */
package fr.ird.database.coverage;

// J2SE dependencies
import java.util.Set;
import java.rmi.RemoteException;
import javax.swing.tree.TreeModel;

// Geotools dependencies
import org.geotools.gui.swing.tree.TreeNode;

// Seagis dependencies
import fr.ird.database.Table;
import fr.ird.database.IllegalRecordException;


/**
 * Connexion vers une table des séries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les paramètres et opérations qui forment les séries, et de placer ces informations dans une
 * arborescence avec des chemins de la forme "<code>paramètre/opération/série</code>".
 *
 * @see CoverageDataBase#getSeriesTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SeriesTable extends Table {
    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les séries.
     */
    public static final int SERIES_LEAF = 3;

   /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les sous-séries (après les séries).
     */
    public static final int SUBSERIES_LEAF = 4;

    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les catégories (après les sous-séries).
     */
    public static final int CATEGORY_LEAF = 7;

    /**
     * Retourne une référence vers un enregistrement de la table des séries.
     *
     * @param  name Nom de la série recherchée.
     * @return Une série qui porte le nom <code>name</code>, ou <code>null</code>
     *         si aucune série de ce nom n'a été trouvée.
     * @throws RemoteException si le catalogue n'a pas pu être interrogée.
     * @throws IllegalRecordException Si plusieurs séries portent le même nom.
     */
    public abstract SeriesEntry getEntry(final String name) throws RemoteException;

    /**
     * Retourne l'ensemble des séries présentes dans la base de données.
     * Cette méthode ne retournera que les séries marquées "visibles".
     *
     * @throws RemoteException si l'interrogation du catalogue a échouée.
     */
    public abstract Set<SeriesEntry> getEntries() throws RemoteException;

    /**
     * Retourne une arborescence qui pourra être affichée dans une composante {@link javax.swing.JTree}.
     * Cette arborescence contiendra les paramètres, opérations, séries et catégories trouvés dans la base
     * de données. Pour la plupart des noeuds de cette arborescence, la méthode {@link TreeNode#getUserObject}
     * retournera un objet {@link Entry} (ou {@link SeriesEntry} pour les noeuds qui représentent des
     * séries).
     *
     * @param  leafType Un des arguments {@link #SERIES_LEAF}, {@link #SUBSERIES_LEAF} ou
     *         {@link #CATEGORY_LEAF}.
     * @return Arborescence des séries de la base de données.
     * @throws RemoteException si l'interrogation du catalogue a échouée.
     */
    public abstract TreeModel getTree(final int leafType) throws RemoteException;

    /**
     * Retourne le format d'une série.
     *
     * @param  series La série pour laquelle on veut le format.
     * @return Le format utilisé par la série spécifiée, ou <code>null</code> si aucun.
     * @throws RemoteException si l'interrogation du catalogue a échouée.
     */
    public FormatEntry getFormat(final SeriesEntry series) throws RemoteException;
}
