/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// J2SE et JAI
import java.io.Serializable;
import java.rmi.RemoteException;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gp.GridCoverageProcessor;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.CoverageTable;


/**
 * Une opération à appliquer sur les données environnementales.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getOperations
 */
public interface OperationEntry extends Entry {
    /**
     * Retourne un numéro unique identifiant cette opération.
     */
    public abstract int getID();

    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne le nom de colonne de l'opération. Cette colonne apparaît dans la table
     * "Environnement", par exemple "pixel" ou "sobel3".
     */
    public abstract String getColumn();

    /**
     * Retourne le préfix à utiliser dans les noms composites. Les noms composites seront
     * de la forme "operation - paramètre - temps", par exemple "grSST-15".
     */
    public abstract String getPrefix();

    /**
     * Retourne le nom de l'opération à utiliser avec {@link GridCoverageProcessor}.
     * Si aucune opération ne doit être appliquée (autre que l'interpolation par défaut
     * appliquée par {@link CoverageTable}, alors cette méthode retourne <code>null</code>.
     */
    public abstract String getProcessorOperation();

    /**
     * Retourne la valeur du paramètre spécifié, ou <code>null</code> si aucun.
     *
     * @param  name Le nom du paramètre. Il s'agira d'un des noms du {@link ParameterList}
     *         pour l'opération désignée par {@link #getProcessorOperation}.
     * @return La valeur du paramètre, ou <code>null</code> pour la valeur par défaut.
     */
    public abstract Object getParameter(String name);

    /**
     * Une implémentation de {@link OperationEntry} qui délègue sont travail à une autre
     * instance de <code>OperationEntry</code>. Cette classe est utile lorsque l'on ne
     * souhaite redéfinir qu'une ou deux méthodes, notamment {@link #getParameter}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static class Proxy implements OperationEntry, Serializable {
        /**
         * Numéro de série pour compatibilitée entre différentes versions.
         */
        private static final long serialVersionUID = -2285791043646792332L;

        /**
         * L'opération envelopée.
         */
        protected final OperationEntry parent;

        /**
         * Construit une nouvelle opération enveloppant l'opération spécifiée.
         */
        protected Proxy(final OperationEntry parent) {
            this.parent = parent;
        }

        /** Délègue l'appel au parent. */ public int    getID       ()            {return parent.getID();}
        /** Délègue l'appel au parent. */ public String getName     ()            {return parent.getName();}
        /** Délègue l'appel au parent. */ public String getPrefix   ()            {return parent.getPrefix();}
        /** Délègue l'appel au parent. */ public String getColumn   ()            {return parent.getColumn();}
        /** Délègue l'appel au parent. */ public String getProcessorOperation()   {return parent.getProcessorOperation();}
        /** Délègue l'appel au parent. */ public Object getParameter(String name) {return parent.getParameter(name);}
        /** Délègue l'appel au parent. */ public String getRemarks  ()            {return parent.getRemarks();}
        /** Délègue l'appel au parent. */ public String toString    ()            {return parent.toString();}
        /** Délègue l'appel au parent. */ public int    hashCode    ()            {return parent.hashCode();}

        /**
         * Retourne <code>true</code> si l'objet spécifié est aussi une instance de
         * <code>Proxy</code> et que leurs {@linkplain #parent parents} sont égaux.
         */
        public boolean equals(final Object object) {
            return object!=null && object.getClass().equals(getClass()) &&
                   parent.equals(((Proxy)object).parent);
        }
    }
}
