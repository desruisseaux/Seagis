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
import java.util.List;
import java.util.Collections;
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Impl�mentation d'un param�tre environnemental.
 * Cette impl�mentation suppose que le {@linkplain #isIdentity param�tre identit�}
 * est identifi� par un {@linkplain #getID num�ro ID} �gal � 0.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterEntry implements fr.ird.database.sample.ParameterEntry, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 8601895381494243866L;

    /**
     * Un num�ro unique identifiant cette entr�.
     */
    private final int ID;

    /**
     * Le nom court du param�tre. Une valeur nulle signifie que cet objet
     * <code>ParameterEntry</code> n'a pas encore �t� initialis�, except�
     * pour le champ <code>ID</code>.
     *
     * Ce champ sera initialis� par <code>ParameterTable.completeEntry</code>
     * apr�s la construction de <code>ParameterEntry</code>.
     */
    String name;

    /**
     * La s�rie d'images � utiliser pour ce param�tre, ainsi qu'une s�rie de
     * rechange � utiliser si une image de la s�rie principale n'est pas disponible.
     *
     * Ces champs seront initialis�s par <code>ParameterTable.completeEntry</code>
     * apr�s la construction de <code>ParameterEntry</code>.
     */
    SeriesEntry series0, series1;

    /**
     * Le num�ro de la bande, � partir de 0.
     *
     * Ce champ sera initialis� par <code>ParameterTable.completeEntry</code>
     * apr�s la construction de <code>ParameterEntry</code>.
     */
    int band;

    /**
     * Les termes du mod�le lin�aire, ou <code>null</code> s'il n'y en a pas.
     * Ce champ sera initialis� par {@link ParameterTable#postCreateEntry}
     * apr�s la construction de <code>ParameterEntry</code>.
     */
    List<LinearModelTerm> linearModel;

    /**
     * Construit un param�tre.
     */
    public ParameterEntry(final int ID) {
        this.ID = ID;
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
    public String getName() {
        return name;
    }

    /**
     * Retourne <code>true</code> si ce param�tre est le <cite>param�tre identit�</cite>.
     * L'impl�mentation par d�faut suppose que le param�tre identit� est identifi� par
     * un num�ro ID �gal � 0.
     */
    public boolean isIdentity() {
        return ID == 0;
    }

    /**
     * {@inheritDoc}
     */
    public SeriesEntry getSeries(final int n) {
        switch (n) {
            case 0:  return series0;
            case 1:  return series1;
            default: if (n < 0) {
                         throw new IllegalArgumentException(String.valueOf(n));
                     }
                     return null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int getBand() {
        return band;
    }

    /**
     * {@inheritDoc}
     */
    public List<LinearModelTerm> getLinearModel() {
        return linearModel;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getRemarks() {
        return null;
    }

    /**
     * Retourne le nom de cette entr�e. Si ce param�tre est constitu� d'un certain nombre de
     * composantes, le nombre de composantes sera �crit entre parenth�se. Ce nom est souvent
     * destin� � appara�tre dans une interface <cite>Swing</cite>.
     */
    public String toString() {
        if (linearModel == null) {
            return name;
        }
        final StringBuffer buffer = new StringBuffer(name);
        buffer.append(" (");
        buffer.append(Resources.format(ResourceKeys.COMPONENT_COUNT_$1, new Integer(linearModel.size())));
        buffer.append(')');
        return buffer.toString();
    }

    /**
     * Retourne un num�ro � peu pr�s unique identifiant cette entr�e.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Compare cette entr�e avec l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (object instanceof ParameterEntry) {
            final ParameterEntry that = (ParameterEntry) object;
            return this.ID   == that.ID    &&
                   this.band == that.band  &&
                   Utilities.equals(this.name,        that.name)    &&
                   Utilities.equals(this.series0,     that.series0) &&
                   Utilities.equals(this.series1,     that.series1) &&
                   Utilities.equals(this.linearModel, that.linearModel);
        }
        return false;
    }
}
