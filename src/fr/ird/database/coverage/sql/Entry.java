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
package fr.ird.database.coverage.sql;

// J2SE dependencies
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// Geotools dependencies
import org.geotools.resources.Utilities;


/**
 * Référence vers un phénomène, une procédure, une série ou un groupe de la base de données d'images.
 * Les références sont représentées par leurs identifiant {@link #ID}, souvent un nom comme objet
 * {@link String} ou un numéro comme objet {@link Integer}. Par exemple chaque enregistrement de
 * la table <code>"Series"</code> est identifié par un nom unique. Pour sélectionner un enregistrement,
 * il faut connaître à la fois la table où l'enregistrement apparaît et le nom de l'enregistrement.
 * Par exemple si on recherche la série <code>"SST Monde"</code>, alors l'instruction SQL peut être:
 *
 * <blockquote><pre>
 * SELECT * FROM Series WHERE Series.name='SST Monde';
 * </pre></blockquote>
 *
 * Des objets <code>Entry</code> sont contenus dans l'arborescence
 * construite par {@link SeriesTable#getTree} et peuvent être obtenus par
 * des appels à {@link org.geotools.gui.swing.tree.TreeNode#getUserObject}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class Entry extends UnicastRemoteObject implements fr.ird.database.Entry {
    /**
     * Numéro de séries pour compatibilités entre différentes versions.
     */
    private static final long serialVersionUID = -5586641247240138600L;

    /**
     * Nom de la table qui contient l'enregistrement référencé.
     */
    public final String table;

    /**
     * Nom ou numéro ID de la référence. Il peut s'agir d'un objet {@link String} provenant du
     * champ "name" de la table {@link #table}, ou un objet {@link Integer} provenant du champ
     * "ID" ou "oid" de la même table.
     */
    public final Comparable identifier;

    /**
     * Remarques s'appliquant à cette entrée.
     */
    public final String remarks;

    /**
     * Construit une réference vers un enregistrement de la base de données d'image.
     * L'enregistrement référencé peut être un format, un groupe, une série, un paramètre,
     * une opération, etc.
     *
     * @param table      Nom de la table qui contient l'enregistrement référencé.
     * @param identifier Nom ou numéro de référence.
     * @param remarks    Remarques s'appliquant à cette références.
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
     * Retourne le nom de l'enregistrement référencé, comme {@link #getName}. Cette
     * méthode est appelée par {@link javax.swing.JTree} pour construire les noms
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
     * Vérifie si cette référence est identique à l'objet spécifié.
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
