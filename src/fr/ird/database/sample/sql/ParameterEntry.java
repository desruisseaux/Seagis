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
import java.util.Collections;
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;
import org.geotools.gp.GridCoverageProcessor;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;


/**
 * Un param�tre environnemental.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterEntry implements fr.ird.database.sample.ParameterEntry, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
//    private static final long serialVersionUID = -6274380414853033347L;

    /**
     * Un num�ro unique identifiant cette entr�.
     */
    private final int ID;

    /**
     * Le nom court du param�tre.
     */
    private final String name;

    /**
     * La s�rie d'images � utiliser pour ce param�tre, ainsi qu'une s�rie de
     * rechange � utiliser si une image de la s�rie principale n'est pas disponible.
     */
    private final SeriesEntry series, series2;

    /**
     * Le num�ro de la bande, � partir de 0.
     */
    private final int band;

    /**
     * Les composantes constituant ce param�tres, ou <code>null</code> s'il n'y en a pas.
     */
    private List<fr.ird.database.sample.ParameterEntry.Component> components;

    /**
     * Construit une entr�.
     */
    public ParameterEntry(final int         ID,
                          final String      name,
                          final SeriesEntry series,
                          final SeriesEntry series2,
                          final int         band)
    {
        this.ID        = ID;
        this.name      = name;
        this.series    = series;
        this.series2   = series2;
        this.band      = band;
    }

    /**
     * D�finit les composantes constituant ce param�tre. Cette m�thode ne peut �tre appel�e
     * qu'une seule fois apr�s la construction de cette entr�e.
     *
     * @param  c Les composantes constituant ce param�tre, ou <code>null</code>.
     * @throws IllegalStateException si ce param�tre a d�j� �t� initialis�.
     */
    final void initComponents(final List<fr.ird.database.sample.ParameterEntry.Component> c)
            throws IllegalStateException
    {
        if (c != null) {
            if (components != null) {
                throw new IllegalStateException();
            }
            components = Collections.unmodifiableList(c);
        }
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
     * {@inheritDoc}
     */
    public SeriesEntry getSeries(final int n) {
        switch (n) {
            case 0:  return series;
            case 1:  return series2;
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
    public List<fr.ird.database.sample.ParameterEntry.Component> getComponents() {
        return components;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getRemarks() {
        return null;
    }

    /**
     * Retourne le nom de cette entr�e.
     */
    public String toString() {
        return name;
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
            return this.ID==that.ID   &&   this.band==that.band  &&
                   Utilities.equals(this.name,      that.name)   &&
                   Utilities.equals(this.series,    that.series) &&
                   Utilities.equals(this.series2,   that.series2);
        }
        return false;
    }

    /**
     * Impl�mentation d'une des composantes d'un {@linkplain ParameterEntry param�tre}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    final class Component implements fr.ird.database.sample.ParameterEntry.Component, Serializable {
        /**
         * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
         */
        private static final long serialVersionUID = 9013631151104037206L;

        /**
         * Le param�tre source.
         */
        private final fr.ird.database.sample.ParameterEntry source;

        /**
         * La position relative du param�tre source.
         */
        private final fr.ird.database.sample.RelativePositionEntry position;

        /**
         * L'op�ration � appliquer sur le param�tre source.
         */
        private final fr.ird.database.sample.OperationEntry operation;

        /**
         * Le poid � donner au param�tre source.
         */
        private final double weight;

        /**
         * Si diff�rent de 0, le facteur par lequelle multiplier la valeur source
         * avant de prendre son logarithme naturel. Si <code>logarithme</code> est
         * 0, alors le logarithme ne sera pas pris.
         */
        private final double logarithm;

        /**
         * Construit une nouvelle composante.
         */
        public Component(final fr.ird.database.sample.ParameterEntry        source,
                         final fr.ird.database.sample.RelativePositionEntry position,
                         final fr.ird.database.sample.OperationEntry        operation,
                         final double                weight,
                         final double                logarithm)
        {
            this.source    = source;
            this.position  = position;
            this.operation = operation;
            this.weight    = weight;
            this.logarithm = logarithm;
        }

        /**
         * Retourne un num�ro unique identifiant cette composante. L'impl�mentation
         * par d�faut suppose que les num�ro ID du param�tre cible, source, de la
         * position relative et de l'op�ration sont tous sur 8 bits.
         */
        public int getID() {
            return (ParameterEntry.this.getID() << 24) ^
                   (source             .getID() << 16) ^
                   (position           .getID() <<  8) ^
                   (operation          .getID() <<  0);
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return ParameterEntry.this.getName();
        }

        /**
         * {@inheritDoc}
         */
        public String getRemarks() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public fr.ird.database.sample.ParameterEntry getTarget() {
            return ParameterEntry.this;
        }

        /**
         * {@inheritDoc}
         */
        public fr.ird.database.sample.ParameterEntry getSource() {
            return source;
        }

        /**
         * {@inheritDoc}
         */
        public fr.ird.database.sample.RelativePositionEntry getRelativePosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public fr.ird.database.sample.OperationEntry getOperation() {
            return operation;
        }

        /**
         * {@inheritDoc}
         */
        public double getWeight() {
            return weight;
        }

        /**
         * {@inheritDoc}
         */
        public double transform(double value) {
            if (logarithm != 0) {
                value = Math.log(value * logarithm);
            }
            return value;
        }

        /**
         * Retourne le nom de cette entr�e.
         */
        public String toString() {
            return getName();
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
            if (object instanceof Component) {
                final Component that = (Component) object;
                return Utilities.equals(ParameterEntry.this, that.getTarget()) &&
                       Utilities.equals(this.source,         that.source)      &&
                       Utilities.equals(this.position,       that.position)    &&
                       Utilities.equals(this.operation,      that.operation)   &&
                       Double.doubleToLongBits(weight)    == Double.doubleToLongBits(weight) &&
                       Double.doubleToLongBits(logarithm) == Double.doubleToLongBits(logarithm);
            }
            return false;
        }
    }
}
