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
 * Une op�ration � appliquer sur les donn�es environnementales.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getOperations
 */
public interface OperationEntry extends Entry {
    /**
     * Retourne un num�ro unique identifiant cette op�ration.
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
     * Retourne le nom de colonne de l'op�ration. Cette colonne appara�t dans la table
     * "Environnement", par exemple "pixel" ou "sobel3".
     */
    public abstract String getColumn();

    /**
     * Retourne le pr�fix � utiliser dans les noms composites. Les noms composites seront
     * de la forme "operation - param�tre - temps", par exemple "grSST-15".
     */
    public abstract String getPrefix();

    /**
     * Retourne le nom de l'op�ration � utiliser avec {@link GridCoverageProcessor}.
     * Si aucune op�ration ne doit �tre appliqu�e (autre que l'interpolation par d�faut
     * appliqu�e par {@link CoverageTable}, alors cette m�thode retourne <code>null</code>.
     */
    public abstract String getProcessorOperation();

    /**
     * Retourne la valeur du param�tre sp�cifi�, ou <code>null</code> si aucun.
     *
     * @param  name Le nom du param�tre. Il s'agira d'un des noms du {@link ParameterList}
     *         pour l'op�ration d�sign�e par {@link #getProcessorOperation}.
     * @return La valeur du param�tre, ou <code>null</code> pour la valeur par d�faut.
     */
    public abstract Object getParameter(String name);

    /**
     * Une impl�mentation de {@link OperationEntry} qui d�l�gue sont travail � une autre
     * instance de <code>OperationEntry</code>. Cette classe est utile lorsque l'on ne
     * souhaite red�finir qu'une ou deux m�thodes, notamment {@link #getParameter}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static class Proxy implements OperationEntry, Serializable {
        /**
         * Num�ro de s�rie pour compatibilit�e entre diff�rentes versions.
         */
        private static final long serialVersionUID = -2285791043646792332L;

        /**
         * L'op�ration envelop�e.
         */
        protected final OperationEntry parent;

        /**
         * Construit une nouvelle op�ration enveloppant l'op�ration sp�cifi�e.
         */
        protected Proxy(final OperationEntry parent) {
            this.parent = parent;
        }

        /** D�l�gue l'appel au parent. */ public int    getID       ()            {return parent.getID();}
        /** D�l�gue l'appel au parent. */ public String getName     ()            {return parent.getName();}
        /** D�l�gue l'appel au parent. */ public String getPrefix   ()            {return parent.getPrefix();}
        /** D�l�gue l'appel au parent. */ public String getColumn   ()            {return parent.getColumn();}
        /** D�l�gue l'appel au parent. */ public String getProcessorOperation()   {return parent.getProcessorOperation();}
        /** D�l�gue l'appel au parent. */ public Object getParameter(String name) {return parent.getParameter(name);}
        /** D�l�gue l'appel au parent. */ public String getRemarks  ()            {return parent.getRemarks();}
        /** D�l�gue l'appel au parent. */ public String toString    ()            {return parent.toString();}
        /** D�l�gue l'appel au parent. */ public int    hashCode    ()            {return parent.hashCode();}

        /**
         * Retourne <code>true</code> si l'objet sp�cifi� est aussi une instance de
         * <code>Proxy</code> et que leurs {@linkplain #parent parents} sont �gaux.
         */
        public boolean equals(final Object object) {
            return object!=null && object.getClass().equals(getClass()) &&
                   parent.equals(((Proxy)object).parent);
        }
    }
}
