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
 * Implémentation d'un paramètre environnemental.
 * Cette implémentation suppose que le {@linkplain #isIdentity paramètre identité}
 * est identifié par un {@linkplain #getID numéro ID} égal à 0.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterEntry implements fr.ird.database.sample.ParameterEntry, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 8601895381494243866L;

    /**
     * Un numéro unique identifiant cette entré.
     */
    private final int ID;

    /**
     * Le nom court du paramètre. Une valeur nulle signifie que cet objet
     * <code>ParameterEntry</code> n'a pas encore été initialisé, excepté
     * pour le champ <code>ID</code>.
     *
     * Ce champ sera initialisé par <code>ParameterTable.completeEntry</code>
     * après la construction de <code>ParameterEntry</code>.
     */
    String name;

    /**
     * La série d'images à utiliser pour ce paramètre, ainsi qu'une série de
     * rechange à utiliser si une image de la série principale n'est pas disponible.
     *
     * Ces champs seront initialisés par <code>ParameterTable.completeEntry</code>
     * après la construction de <code>ParameterEntry</code>.
     */
    SeriesEntry series0, series1;

    /**
     * Le numéro de la bande, à partir de 0.
     *
     * Ce champ sera initialisé par <code>ParameterTable.completeEntry</code>
     * après la construction de <code>ParameterEntry</code>.
     */
    int band;

    /**
     * Les termes du modèle linéaire, ou <code>null</code> s'il n'y en a pas.
     * Ce champ sera initialisé par {@link ParameterTable#postCreateEntry}
     * après la construction de <code>ParameterEntry</code>.
     */
    List<LinearModelTerm> linearModel;

    /**
     * Construit un paramètre.
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
     * Retourne <code>true</code> si ce paramètre est le <cite>paramètre identité</cite>.
     * L'implémentation par défaut suppose que le paramètre identité est identifié par
     * un numéro ID égal à 0.
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
     * Retourne le nom de cette entrée. Si ce paramètre est constitué d'un certain nombre de
     * composantes, le nombre de composantes sera écrit entre parenthèse. Ce nom est souvent
     * destiné à apparaître dans une interface <cite>Swing</cite>.
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
     * Retourne un numéro à peu près unique identifiant cette entrée.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Compare cette entrée avec l'objet spécifié.
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
