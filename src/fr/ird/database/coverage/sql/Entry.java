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
package fr.ird.database.coverage.sql;

// J2SE
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;


/**
 * Référence vers un paramètre, une opération, une série ou un groupe de la base de données d'images.
 * Les références sont représentées par des numéros {@link #ID} qui apparaisent dans plusieurs tables
 * de la base de données. Par exemple chaque enregistrement de la table "Series" (donc chaque série)
 * est identifié par un numéro ID unique. Pour sélectionner un enregistrement, il faut connaître à
 * la fois la table où l'enregistrement apparaît et son numéro ID. Une telle sélection peut être
 * faite avec l'instruction SQL suivante:
 *
 * <blockquote><pre>
 * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
 * </pre></blockquote>
 *
 * Par exemple si on recherche la série #5, alors l'instruction SQL sera:
 *
 * <blockquote><pre>
 * SELECT * FROM Series WHERE Series.ID=5
 * </pre></blockquote>
 *
 * Des objets <code>Entry</code> sont contenus dans l'arborescence
 * construite par {@link SeriesTable#getTree} et peuvent être obtenus par
 * des appels à {@link org.geotools.gui.swing.tree.TreeNode#getUserObject}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class Entry extends java.rmi.server.UnicastRemoteObject implements fr.ird.database.Entry, Serializable {
    /**
     * Numéro de séries pour compatibilités entre différentes versions.
     */
    private static final long serialVersionUID = -5586641247240138600L;

    /**
     * Nom de la table qui contient l'enregistrement référencé.
     */
    public final String table;

    /**
     * Nom de la référence. Ce nom provient du champ "name" de la table {@link #table}. Ce
     * nom pourrait servir à identifier l'enregistrement référencé, mais on utilisera plutôt
     * {@link #ID} à cette fin.
     */
    public final String name;

    /**
     * Remarques s'appliquant à cette entrée.
     */
    public final String remarks;

    /**
     * Numéro de référence. Ce numéro provient du champ "ID" de la table {@link #table}.
     * Il sert à obtenir l'enregistrement référencé avec une instruction SQL comme suit:
     *
     * <blockquote><pre>
     * "SELECT * FROM "+{@link #table}+" WHERE "+{@link #table}+".ID="+{@link #ID}
     * </pre></blockquote>
     */
    public final int ID;

    /**
     * Construit une réference vers un enregistrement de la base de données d'image.
     * L'enregistrement référencé peut être un format, un groupe, une série, un paramètre,
     * une opération, etc.
     *
     * @param table   Nom de la table qui contient l'enregistrement référencé.
     * @param name    Nom de la référence. Ce nom provient du champ "name" de la table <code>table</code>.
     * @param ID      Numéro de référence. Ce numéro provient du champ "ID" de la table <code>table</code>.
     * @param remarks Remarques s'appliquant à cette références.
     */
    protected Entry(final String table, final String name, final int ID, final String remarks) throws RemoteException {
        this.table   = table.trim();
        this.name    = name.trim();
        this.remarks = remarks; // May be null
        this.ID      = ID;
    }

    /**
     * Retourne un numéro unique identifiant cette série.
     */
    public int getID() throws RemoteException {
        return ID;
    }

    /**
     * Retourne le nom de l'enregistrement référencé,
     * c'est-à-dire le champ {@link #name}.
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
     * Retourne le nom de l'enregistrement référencé, comme {@link #getName}. Cette
     * méthode est appelée par {@link javax.swing.JTree} pour construire les noms
     * des noeuds dans l'arborescence.
     */
    public String toString() {
        return name;
    }

    /**
     * Retourne le numéro de référence, c'est-à-dire le code {@link #getID}.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Vérifie si cette référence est identique à l'objet spécifié.
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
