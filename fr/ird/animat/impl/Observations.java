/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 * Un ensemble d'observations � un pas de temps donn�. Cet ensemble est construit chaque fois que
 * {@link Animal#getObservations} est appel�e avec un pas de temps diff�rent. Il peut �tre transmis
 * � travers le r�seau vers des machines distances (typiquement des stations qui afficheront le
 * r�sultat de la simulation, pendant que la simulation elle-m�me se poursuit sur un serveur).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see Animal#getObservations
 */
final class Observations extends AbstractMap<Parameter,Observation> implements Serializable
{
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 2435997994848029037L;

    /**
     * Index (relatif au d�but d'un enregistrement) de la valeur d'une observation.
     */
    static final int VALUE_OFFSET = 0;

    /**
     * Index (relatif au d�but d'un enregistrement) de la longitude d'une observation.
     * Note: pour le fonctionnement correct de {@link Animal#getObservations}, les
     *       coordonn�es (x,y) doivent toujours �tre les deux derniers �l�ments.
     */
    static final int X_OFFSET = 1;

    /**
     * Index (relatif au d�but d'un enregistrement) de la latitude d'une observation.
     * Note: pour le fonctionnement correct de {@link Animal#getObservations}, les
     *       coordonn�es (x,y) doivent toujours �tre les deux derniers �l�ments.
     */
    static final int Y_OFFSET = 2;

    /**
     * La liste des param�tres observ�s. La longeur de ce tableau est le nombre
     * d'�l�ments de cet objet <code>Map</code>.
     */
    private final fr.ird.animat.impl.Parameter[] parameters;

    /**
     * L'ensemble des observations pour le pas de temps examin�. Ce tableau n'est qu'un extrait du
     * tableau {@link Animal#observations}, qui contient la totalit� des observations de l'animal.
     * Nous n'utilisons qu'un extrait afin d'acc�l�rer les transfert via le r�seau dans le cas d'une
     * utilisation avec les RMI.
     */
    private final float[] observations;

    /**
     * Les entr�s de type {@link Entry}. Ne seront construit que la premi�re fois o� ils
     * seront demand�s.
     */
    private transient Entry[] entries;

    /**
     * L'ensemble retourn� par {@link #keySet}.
     * Ne sera construit que la premi�re fois o� il sera demand�.
     */
    private transient Set<Parameter> keySet;

    /**
     * L'ensemble retourn� par {@link #entrySet}.
     * Ne sera construit que la premi�re fois o� il sera demand�.
     */
    private transient Set<Map.Entry<Parameter, Observation>> entrySet;

    /**
     * Construit un ensemble d'observations pour les param�tres sp�cifi�s.
     * Aucun des tableaux donn�s en argument ne sera clon�. Ils ne devront
     * donc pas �tre modifi�s.
     *
     * @param parameter    Les param�tres observ�s.
     * @param observations Les observations.
     */
    public Observations(final fr.ird.animat.impl.Parameter[] parameters, final float[] observations) {
        this.parameters   = parameters;
        this.observations = observations;
    }

    /**
     * Retourne le nombre d'entr�s.
     */
    public int size() {
	return parameters.length;
    }

    /**
     * Indique si cet objet contient la valeur sp�cifi�e.
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
     * Indique si cet objet contient la cl� sp�cifi�e.
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
     * Retourne l'observation correspondant au param�tre sp�cifi�.
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
     * Retourne l'ensemble des cl�s.
     */
    public Set<Parameter> keySet() {
        if (keySet == null) {
            keySet = new ArraySet<Parameter>(parameters);
        }
        return keySet;
    }

    /**
     * Retourne une vue contenant les entr�s.
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
     * Une observation construite � partir de l'ensemble {@link Observations}.
     * Les objets <code>Entry</code> ne sont construit que la premi�re fois o�
     * ils sont n�cessaires.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Entry implements Map.Entry<Parameter, Observation>, Observation {
        /**
         * Le param�tre observ�.
         */
        private final fr.ird.animat.impl.Parameter parameter;

        /**
         * La position dans {@link Observations#observations} � partir d'o� extraire les donn�es.
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
         * Retourne la cl� correspondant � cette entr�e.
         */
        public Parameter getKey() {
            return parameter;
        }

        /**
         * Retourne la valeur correspondant � cette entr�e.
         */
        public Observation getValue() {
            return this;
        }
        
        /**
         * Remplace la valeur correspondant � cette entr�e. Cette op�ration n'est
         * pas support�e puisque les objets {@link Observations} sont immutables.
         */
        public Observation setValue(final Observation value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Retoune sous forme de bits la valeur � l'index sp�cifi�e. Cette m�thode est utilis�e
         * pour l'impl�mentation des m�thodes {@link #equals} et {@link #hashCode} seulement.
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
         * Retourne une position repr�sentative de l'observation, ou <code>null</code>
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
         * V�rifie si cette observation est identique � l'objet sp�cifi�.
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
         * Retourne une repr�sentation de cet observation sous forme de cha�ne de caract�res.
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
     * Retourne une repr�sentation de cet ensemble sous forme de cha�ne de caract�res.
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
