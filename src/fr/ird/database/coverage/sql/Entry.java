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
package fr.ird.database.coverage.sql;

// J2SE
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;


/**
 * R�f�rence vers un param�tre, une op�ration, une s�rie ou un groupe de la base de donn�es d'images.
 * Les r�f�rences sont repr�sent�es par des num�ros {@link #ID} qui apparaisent dans plusieurs tables
 * de la base de donn�es. Par exemple chaque enregistrement de la table "Series" (donc chaque s�rie)
 * est identifi� par un num�ro ID unique. Pour s�lectionner un enregistrement, il faut conna�tre �
 * la fois la table o� l'enregistrement appara�t et son num�ro ID. Une telle s�lection peut �tre
 * faite avec l'instruction SQL suivante:
 *
 * <blockquote><pre>
 * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
 * </pre></blockquote>
 *
 * Par exemple si on recherche la s�rie #5, alors l'instruction SQL sera:
 *
 * <blockquote><pre>
 * SELECT * FROM Series WHERE Series.ID=5
 * </pre></blockquote>
 *
 * Des objets <code>Entry</code> sont contenus dans l'arborescence
 * construite par {@link SeriesTable#getTree} et peuvent �tre obtenus par
 * des appels � {@link org.geotools.gui.swing.tree.TreeNode#getUserObject}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class Entry extends java.rmi.server.UnicastRemoteObject implements fr.ird.database.Entry, Serializable {
    /**
     * Num�ro de s�ries pour compatibilit�s entre diff�rentes versions.
     */
    private static final long serialVersionUID = -5586641247240138600L;

    /**
     * Nom de la table qui contient l'enregistrement r�f�renc�.
     */
    public final String table;

    /**
     * Nom de la r�f�rence. Ce nom provient du champ "name" de la table {@link #table}. Ce
     * nom pourrait servir � identifier l'enregistrement r�f�renc�, mais on utilisera plut�t
     * {@link #ID} � cette fin.
     */
    public final String name;

    /**
     * Remarques s'appliquant � cette entr�e.
     */
    public final String remarks;

    /**
     * Num�ro de r�f�rence. Ce num�ro provient du champ "ID" de la table {@link #table}.
     * Il sert � obtenir l'enregistrement r�f�renc� avec une instruction SQL comme suit:
     *
     * <blockquote><pre>
     * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
     * </pre></blockquote>
     */
    public final int ID;

    /**
     * Construit une r�ference vers un enregistrement de la base de donn�es d'image.
     * L'enregistrement r�f�renc� peut �tre un format, un groupe, une s�rie, un param�tre,
     * une op�ration, etc.
     *
     * @param table   Nom de la table qui contient l'enregistrement r�f�renc�.
     * @param name    Nom de la r�f�rence. Ce nom provient du champ "name" de la table <code>table</code>.
     * @param ID      Num�ro de r�f�rence. Ce num�ro provient du champ "ID" de la table <code>table</code>.
     * @param remarks Remarques s'appliquant � cette r�f�rences.
     */
    protected Entry(final String table, final String name, final int ID, final String remarks) throws RemoteException {
        this.table   = table.trim();
        this.name    = name.trim();
        this.remarks = remarks; // May be null
        this.ID      = ID;
    }

    /**
     * Retourne un num�ro unique identifiant cette s�rie.
     */
    public int getID() throws RemoteException {
        return ID;
    }

    /**
     * Retourne le nom de l'enregistrement r�f�renc�,
     * c'est-�-dire le champ {@link #name}.
     */
    public String getName() throws RemoteException {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getRemarks() throws RemoteException {
        return remarks;
    }

    /**
     * Retourne le nom de l'enregistrement r�f�renc�, comme {@link #getName}. Cette
     * m�thode est appel�e par {@link javax.swing.JTree} pour construire les noms
     * des noeuds dans l'arborescence.
     */
    public String toString() {
        return name;
    }

    /**
     * Retourne le num�ro de r�f�rence, c'est-�-dire le code {@link #getID}.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * V�rifie si cette r�f�rence est identique � l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            final Entry that = (Entry) object;
            return this.ID       ==  that.ID    &&
                   this.name .equals(that.name) &&
                   this.table.equals(that.table);
        }
        return false;
    }
}
