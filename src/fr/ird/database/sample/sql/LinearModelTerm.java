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
package fr.ird.database.sample.sql;

// J2SE
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.AbstractList;
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;


/**
 * Impl�mentaiton d'un terme dans un mod�le lin�aire.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see ParameterEntry#getLinearModel
 */
final class LinearModelTerm extends AbstractList<fr.ird.database.sample.DescriptorEntry>
                         implements fr.ird.database.sample.LinearModelTerm, Serializable
{
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -8990932193707468648L;

    /**
     * Le param�tre dans lequel seront stock�es les valeurs de la variable
     * d�pendante <var>y</var>.
     */
    private final ParameterEntry target;

    /**
     * Les descripteurs du paysage oc�anique composant ce terme.
     * Ce champ est acc�d� en lecture seul par {@link LinearModelTable}.
     *
     * @see #getDescriptors
     */
    final DescriptorEntry[] descriptors;

    /**
     * Le coefficient <var>C</var> de ce terme
     */
    private final double coefficient;

    /**
     * Construit un terme d'un mod�le lin�aire.
     *
     * @param target      Le param�tre dans lequel seront stock�es les valeurs
     *                    de la variable d�pendante <var>y</var>.
     * @param descriptors Les descripteurs du paysage oc�anique composant ce terme.
     *                    Ce tableau ne sera pas clon�.
     * @param coefficient Le coefficient <var>C</var> de ce terme.
     */
    public LinearModelTerm(final ParameterEntry    target,
                           final DescriptorEntry[] descriptors,
                           final double            coefficient)
    {
        this.target      = target;
        this.descriptors = descriptors;
        this.coefficient = coefficient;
    }

    /**
     * {@inheritDoc}
     */
    public ParameterEntry getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    public double getCoefficient() {
        return coefficient;
    }

    /**
     * {@inheritDoc}
     */
    public List<fr.ird.database.sample.DescriptorEntry> getDescriptors() {
        return this;
    }

    /**
     * Retourne le nombre de descripteurs.
     */
    public int size() {
        return descriptors.length;
    }

    /**
     * Retourne le descripteur � l'index sp�cifi�.
     */
    public DescriptorEntry get(final int index) {
        return descriptors[index];
    }

    /**
     * Retourne les descripteurs qui compose ce terme.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(coefficient);
        for (int i=0; i<descriptors.length; i++) {
            buffer.append('\u00D7'); // Multiplication sign
            buffer.append(descriptors[i]);
        }
        return buffer.toString();
    }

    /**
     * Retourne un num�ro � peu pr�s unique repr�sentant ce terme.
     */
    public int hashCode() {
        final long coeff = Double.doubleToLongBits(coefficient);
        int code = (int)coeff ^ (int)(coeff >>> 32);
        for (int i=0; i<descriptors.length; i++) {
            code ^= descriptors[i].hashCode();
        }
        return code;
    }

    /**
     * V�rifie si cet objet est �gal � l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (object instanceof LinearModelTerm) {
            final LinearModelTerm that = (LinearModelTerm) object;
            if (Double.doubleToLongBits(this.coefficient) ==
                Double.doubleToLongBits(that.coefficient))
            {
                return Utilities.equals(this.target     , that.target) &&
                          Arrays.equals(this.descriptors, that.descriptors);
            }
        }
        return false;
    }
}
