/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample.sql;

// J2SE
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;
import org.geotools.gp.GridCoverageProcessor;

// Seagis
import fr.ird.database.Entry;


/**
 * Une op�ration � appliquer sur les donn�es environnemental.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class OperationEntry implements fr.ird.database.sample.OperationEntry, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -4700360283071351634L;

    /**
     * Un num�ro unique identifiant cette entr�.
     */
    private final int ID;

    /**
     * Le nom de colonne de l'op�ration. Cette colonne appara�t dans la table
     * "Environnement", par exemple "pixel" ou "sobel3".
     */
    private final String column;

    /**
     * Le pr�fix � utiliser dans les noms composites. Les noms composites seront
     * de la forme "operation - param�tre - temps", par exemple "grSST-15".
     */
    private final String prefix;

    /**
     * Le nom de l'op�ration � utiliser avec {@link GridCoverageProcessor}.
     */
    private final String operation;

    /**
     * Le nom court de l'op�ration pour les interfaces utilisateurs graphiques.
     */
    private final String name;

    /**
     * Des remarques concernant cette op�ration.
     */
    private final String remarks;

    /**
     * Construit une entr�.
     */
    public OperationEntry(final int    ID,
                          final String column,
                          final String prefix,
                          final String operation,
                          final String name,
                          final String remarks)
    {
        this.ID        = ID;
        this.column    = column;
        this.prefix    = prefix;
        this.operation = operation;
        this.name      = name;
        this.remarks   = remarks;
    }

    /**
     * {@inheritDoc}
     */
    public int getID() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumn() {
        return column;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    public String getProcessorOperation() {
        return operation;
    }

    /**
     * {@inheritDoc}
     */
    public Object getParameter(final String name) {
        return null;
    }

    /**
     * Retourne le nom de cette entr�, comme {@link #getName}. Ce nom est
     * souvent destin� � appara�tre dans une interface <cite>Swing</cite>.
     */
    public String toString() {
        return name;
    }

    /**
     * Retourne un num�ro � peu pr�s unique identifiant cette entr�e.
     */
    public int hashCode() {
        return getID();
    }

    /**
     * Compare cette entr�e avec l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (object instanceof OperationEntry) {
            final OperationEntry that = (OperationEntry) object;
            return Utilities.equals(this.column,    that.column)    &&
                   Utilities.equals(this.prefix,    that.prefix)    &&
                   Utilities.equals(this.operation, that.operation) &&
                   Utilities.equals(this.name,      that.name)      &&
                   Utilities.equals(this.remarks,   that.remarks);
        }
        return false;
    }
}
