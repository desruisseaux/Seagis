/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.impl;

// J2SE
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.awt.geom.Point2D;
import java.io.Serializable;

// Seagis
import fr.ird.animat.Observation;
import fr.ird.animat.Parameter;
import fr.ird.util.ArraySet;

// Geotools
import org.geotools.resources.Utilities;


/**
 * Un ensemble d'observations à un pas de temps donné. Cet ensemble est construit chaque fois que
 * {@link Animal#getObservations} est appelée avec un pas de temps différent. Il peut être transmis
 * à travers le réseau vers des machines distances (typiquement des stations qui afficheront le
 * résultat de la simulation, pendant que la simulation elle-même se poursuit sur un serveur).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see Animal#getObservations
 */
final class Observations extends AbstractMap<Parameter,Observation> implements Serializable
{
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 2435997994848029037L;

    /**
     * Index (relatif au début d'un enregistrement) de la valeur d'une observation.
     */
    static final int VALUE_OFFSET = 0;

    /**
     * Index (relatif au début d'un enregistrement) de la longitude d'une observation.
     * Note: pour le fonctionnement correct de {@link Animal#getObservations}, les
     *       coordonnées (x,y) doivent toujours être les deux derniers éléments.
     */
    static final int X_OFFSET = 1;

    /**
     * Index (relatif au début d'un enregistrement) de la latitude d'une observation.
     * Note: pour le fonctionnement correct de {@link Animal#getObservations}, les
     *       coordonnées (x,y) doivent toujours être les deux derniers éléments.
     */
    static final int Y_OFFSET = 2;

    /**
     * La liste des paramètres observés. La longeur de ce tableau est le nombre
     * d'éléments de cet objet <code>Map</code>.
     */
    private final fr.ird.animat.impl.Parameter[] parameters;

    /**
     * L'ensemble des observations pour le pas de temps examiné. Ce tableau n'est qu'un extrait du
     * tableau {@link Animal#observations}, qui contient la totalité des observations de l'animal.
     * Nous n'utilisons qu'un extrait afin d'accélérer les transfert via le réseau dans le cas d'une
     * utilisation avec les RMI.
     */
    private final float[] observations;

    /**
     * Les entrés de type {@link Entry}. Ne seront construit que la première fois où ils
     * seront demandés.
     */
    private transient Entry[] entries;

    /**
     * L'ensemble retourné par {@link #keySet}.
     * Ne sera construit que la première fois où il sera demandé.
     */
    private transient Set<Parameter> keySet;

    /**
     * L'ensemble retourné par {@link #entrySet}.
     * Ne sera construit que la première fois où il sera demandé.
     */
    private transient Set<Map.Entry<Parameter, Observation>> entrySet;

    /**
     * Construit un ensemble d'observations pour les paramètres spécifiés.
     * Aucun des tableaux donnés en argument ne sera cloné. Ils ne devront
     * donc pas être modifiés.
     *
     * @param parameter    Les paramètres observés.
     * @param observations Les observations.
     */
    public Observations(final fr.ird.animat.impl.Parameter[] parameters, final float[] observations) {
        this.parameters   = parameters;
        this.observations = observations;
    }

    /**
     * Retourne le nombre d'entrés.
     */
    public int size() {
	return parameters.length;
    }

    /**
     * Indique si cet objet contient la valeur spécifiée.
     */
    public boolean containsValue(final Observation value) {
        if (value != null) {
            if (entries == null) {
                entries = new Entry[parameters.length];
            }
            for (int i=0; i<entries.length; i++) {
                if (entries[i] == null) {
                    entries[i] = new Entry(i);
                }
                if (value.equals(entries[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indique si cet objet contient la clé spécifiée.
     */
    public boolean containsKey(final Parameter key) {
        if (key != null) {
            for (int i=0; i<parameters.length; i++) {
                if (key.equals(parameters[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retourne l'observation correspondant au paramètre spécifié.
     */
    public Observation get(final Parameter key) {
        if (key != null) {
            for (int i=0; i<parameters.length; i++) {
                if (key.equals(parameters[i])) {
                    if (entries == null) {
                        entries = new Entry[parameters.length];
                    }
                    if (entries[i] == null) {
                        entries[i] = new Entry(i);
                    }
                    return entries[i];
                }
            }
        }
        return null;
    }

    /**
     * Retourne l'ensemble des clés.
     */
    public Set<Parameter> keySet() {
        if (keySet == null) {
            keySet = new ArraySet<Parameter>(parameters);
        }
        return keySet;
    }

    /**
     * Retourne une vue contenant les entrés.
     */
    public Set<Map.Entry<Parameter, Observation>> entrySet() {
        if (entrySet == null) {
            if (entries == null) {
                entries = new Entry[parameters.length];
            }
            entrySet = new ArraySet<Map.Entry<Parameter, Observation>>(entries) {
                protected Map.Entry<Parameter, Observation> create(final int index) {
                    return new Entry(index);
                }
            };
        }
        return entrySet;
    }

    /**
     * Une observation construite à partir de l'ensemble {@link Observations}.
     * Les objets <code>Entry</code> ne sont construit que la première fois où
     * ils sont nécessaires.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Entry implements Map.Entry<Parameter, Observation>, Observation {
        /**
         * Le paramètre observé.
         */
        private final fr.ird.animat.impl.Parameter parameter;

        /**
         * La position dans {@link Observations#observations} à partir d'où extraire les données.
         */
        private final int offset;

        /**
         * Construit une observation.
         */
        public Entry(final int index) {
            parameter = parameters[index];
            int offset = 0;
            for (int i=index; --i>=0;) {
                offset += parameters[i].getNumSampleDimensions();
                if (entries[i] != null) {
                    offset += entries[i].offset;
                    break;
                }
            }
            this.offset = offset;
        }

        /**
         * Retourne la clé correspondant à cette entrée.
         */
        public Parameter getKey() {
            return parameter;
        }

        /**
         * Retourne la valeur correspondant à cette entrée.
         */
        public Observation getValue() {
            return this;
        }
        
        /**
         * Remplace la valeur correspondant à cette entrée. Cette opération n'est
         * pas supportée puisque les objets {@link Observations} sont immutables.
         */
        public Observation setValue(final Observation value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Retoune sous forme de bits la valeur à l'index spécifiée. Cette méthode est utilisée
         * pour l'implémentation des méthodes {@link #equals} et {@link #hashCode} seulement.
         */
        private int value(final int index) {
            return Float.floatToIntBits(observations[index+offset]);
        }
        
        /**
         * Retourne la valeur de l'observation, ou {@link Float#NaN} si elle n'est pas disponible.
         */
        public float value() {
            return observations[offset + VALUE_OFFSET];
        }
        
        /**
         * Retourne une position représentative de l'observation, ou <code>null</code>
         * si elle n'est pas disponible.
         */
        public Point2D location() {
            if (parameter.getNumSampleDimensions() >= 3) {
                final float x = observations[offset + X_OFFSET];
                final float y = observations[offset + Y_OFFSET];
                if (!Float.isNaN(x) || !Float.isNaN(y)) {
                    return new Point2D.Float(x,y);
                }
            }
            return null;
        }

        /**
         * Vérifie si cette observation est identique à l'objet spécifié.
         */
        public boolean equals(final Object object) {
            if (object instanceof Entry) {
                final Entry that = (Entry) object;
                if (this.offset==that.offset && Utilities.equals(this.parameter, that.parameter)) {
                    for (int i=parameter.getNumSampleDimensions(); --i>=0;) {
                        if (this.value(i) != that.value(i)) {
                            return false;
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Retourne un code pour cette observation.
         */
        public int hashCode() {
            int code = offset;
            for (int i=parameter.getNumSampleDimensions(); --i>=0;) {
                code = 37*code + value(i);
            }
            return code;
        }

        /**
         * Retourne une représentation de cet observation sous forme de chaîne de caractères.
         */
        public String toString() {
            final StringBuffer buffer = new StringBuffer("Observation[");
            final int stop = parameter.getNumSampleDimensions();
            for (int i=0; i<stop; i++) {
                if (i!=0) {
                    buffer.append(", ");
                }
                buffer.append(observations[i+offset]);
            }
            buffer.append(']');
            return buffer.toString();
        }
    }

    /**
     * Retourne une représentation de cet ensemble sous forme de chaîne de caractères.
     */
    public String toString() {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringBuffer buffer = new StringBuffer("Observations:");
        final String[] names = new String[parameters.length];
        int maxLength = 0;
        for (int i=0; i<parameters.length; i++) {
            final int length = (names[i] = parameters[i].getName()).length();
            if (length > maxLength) {
                maxLength = length;
            }
        }
        buffer.append(lineSeparator);
        for (int i=0; i<names.length; i++) {
            buffer.append("  Parameter[");
            buffer.append(names[i]);
            buffer.append(']');
            buffer.append(Utilities.spaces(maxLength - names[i].length()));
            buffer.append(" = ");
            if (entries[i] == null) {
                entries[i] = new Entry(i);
            }
            buffer.append(entries[i]);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
