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
 * Connexion vers une table des s�ries. Un objet <code>SeriesTable</code> est capable de retrouver
 * les param�tres et op�rations qui forment les s�ries, et de placer ces informations dans une
 * arborescence avec des chemins de la forme "<code>param�tre/op�ration/s�rie</code>".
 *
 * @see CoverageDataBase#getSeriesTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SeriesTable extends Table {
    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les s�ries.
     */
    public static final int SERIES_LEAF = 3;

   /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les sous-s�ries (apr�s les s�ries).
     */
    public static final int SUBSERIES_LEAF = 4;

    /**
     * Argument pour {@link #getTree} indiquant que l'arborescence
     * ne doit pas aller plus loin que les cat�gories (apr�s les sous-s�ries).
     */
    public static final int CATEGORY_LEAF = 7;

    /**
     * Retourne une r�f�rence vers un enregistrement de la table des s�ries.
     *
     * @param  name Nom de la s�rie recherch�e.
     * @return Une s�rie qui porte le nom <code>name</code>, ou <code>null</code>
     *         si aucune s�rie de ce nom n'a �t� trouv�e.
     * @throws RemoteException si le catalogue n'a pas pu �tre interrog�e.
     * @throws IllegalRecordException Si plusieurs s�ries portent le m�me nom.
     */
    public abstract SeriesEntry getEntry(final String name) throws RemoteException;

    /**
     * Retourne l'ensemble des s�ries pr�sentes dans la base de donn�es.
     * Cette m�thode ne retournera que les s�ries marqu�es "visibles".
     *
     * @throws RemoteException si l'interrogation du catalogue a �chou�e.
     */
    public abstract Set<SeriesEntry> getEntries() throws RemoteException;

    /**
     * Retourne une arborescence qui pourra �tre affich�e dans une composante {@link javax.swing.JTree}.
     * Cette arborescence contiendra les param�tres, op�rations, s�ries et cat�gories trouv�s dans la base
     * de donn�es. Pour la plupart des noeuds de cette arborescence, la m�thode {@link TreeNode#getUserObject}
     * retournera un objet {@link Entry} (ou {@link SeriesEntry} pour les noeuds qui repr�sentent des
     * s�ries).
     *
     * @param  leafType Un des arguments {@link #SERIES_LEAF}, {@link #SUBSERIES_LEAF} ou
     *         {@link #CATEGORY_LEAF}.
     * @return Arborescence des s�ries de la base de donn�es.
     * @throws RemoteException si l'interrogation du catalogue a �chou�e.
     */
    public abstract TreeModel getTree(final int leafType) throws RemoteException;

    /**
     * Retourne le format d'une s�rie.
     *
     * @param  series La s�rie pour laquelle on veut le format.
     * @return Le format utilis� par la s�rie sp�cifi�e, ou <code>null</code> si aucun.
     * @throws RemoteException si l'interrogation du catalogue a �chou�e.
     */
    public FormatEntry getFormat(final SeriesEntry series) throws RemoteException;
}
