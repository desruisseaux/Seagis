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
package fr.ird.database.coverage.sql;

// J2SE dependencies
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// Geotools dependencies
import org.geotools.resources.Utilities;


/**
 * R�f�rence vers un ph�nom�ne, une proc�dure, une s�rie ou un groupe de la base de donn�es d'images.
 * Les r�f�rences sont repr�sent�es par leurs identifiant {@link #ID}, souvent un nom comme objet
 * {@link String} ou un num�ro comme objet {@link Integer}. Par exemple chaque enregistrement de
 * la table <code>"Series"</code> est identifi� par un nom unique. Pour s�lectionner un enregistrement,
 * il faut conna�tre � la fois la table o� l'enregistrement appara�t et le nom de l'enregistrement.
 * Par exemple si on recherche la s�rie <code>"SST Monde"</code>, alors l'instruction SQL peut �tre:
 *
 * <blockquote><pre>
 * SELECT * FROM Series WHERE Series.name='SST Monde';
 * </pre></blockquote>
 *
 * Des objets <code>Entry</code> sont contenus dans l'arborescence
 * construite par {@link SeriesTable#getTree} et peuvent �tre obtenus par
 * des appels � {@link org.geotools.gui.swing.tree.TreeNode#getUserObject}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class Entry extends UnicastRemoteObject implements fr.ird.database.Entry {
    /**
     * Num�ro de s�ries pour compatibilit�s entre diff�rentes versions.
     */
    private static final long serialVersionUID = -5586641247240138600L;

    /**
     * Nom de la table qui contient l'enregistrement r�f�renc�.
     */
    public final String table;

    /**
     * Nom ou num�ro ID de la r�f�rence. Il peut s'agir d'un objet {@link String} provenant du
     * champ "name" de la table {@link #table}, ou un objet {@link Integer} provenant du champ
     * "ID" ou "oid" de la m�me table.
     */
    public final Comparable identifier;

    /**
     * Remarques s'appliquant � cette entr�e.
     */
    public final String remarks;

    /**
     * Construit une r�ference vers un enregistrement de la base de donn�es d'image.
     * L'enregistrement r�f�renc� peut �tre un format, un groupe, une s�rie, un param�tre,
     * une op�ration, etc.
     *
     * @param table      Nom de la table qui contient l'enregistrement r�f�renc�.
     * @param identifier Nom ou num�ro de r�f�rence.
     * @param remarks    Remarques s'appliquant � cette r�f�rences.
     */
    protected Entry(final String     table,
                    final Comparable identifier,
                    final String     remarks)
            throws RemoteException
    {
        this.table      = table.trim();
        this.identifier = identifier;
        this.remarks    = remarks; // May be null
    }

    /**
     * {@inheritDoc}
     */
    public final String getName() {
        return identifier.toString();
    }

    /**
     * {@inheritDoc}
     */
    public final String getRemarks() {
        return remarks;
    }

    /**
     * Retourne le nom de l'enregistrement r�f�renc�, comme {@link #getName}. Cette
     * m�thode est appel�e par {@link javax.swing.JTree} pour construire les noms
     * des noeuds dans l'arborescence.
     */
    public final String toString() {
        return getName();
    }

    /**
     * Returns a hash code value.
     */
    public int hashCode() {
        return (int)serialVersionUID ^ identifier.hashCode();
    }

    /**
     * V�rifie si cette r�f�rence est identique � l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            final Entry that = (Entry) object;
            return Utilities.equals(this.identifier, that.identifier) &&
                   Utilities.equals(this.table,      that.table     ) &&
                   Utilities.equals(this.remarks,    that.remarks   );
        }
        return false;
    }
}
