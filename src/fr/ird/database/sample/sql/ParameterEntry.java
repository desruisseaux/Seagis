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
 * Un paramètre environnemental.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ParameterEntry implements fr.ird.database.sample.ParameterEntry, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
//    private static final long serialVersionUID = -6274380414853033347L;

    /**
     * Un numéro unique identifiant cette entré.
     */
    private final int ID;

    /**
     * Le nom court du paramètre.
     */
    private final String name;

    /**
     * La série d'images à utiliser pour ce paramètre, ainsi qu'une série de
     * rechange à utiliser si une image de la série principale n'est pas disponible.
     */
    private final SeriesEntry series, series2;

    /**
     * Le numéro de la bande, à partir de 0.
     */
    private final int band;

    /**
     * Les composantes constituant ce paramètres, ou <code>null</code> s'il n'y en a pas.
     */
    private List<fr.ird.database.sample.ParameterEntry.Component> components;

    /**
     * Construit une entré.
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
     * Définit les composantes constituant ce paramètre. Cette méthode ne peut être appelée
     * qu'une seule fois après la construction de cette entrée.
     *
     * @param  c Les composantes constituant ce paramètre, ou <code>null</code>.
     * @throws IllegalStateException si ce paramètre a déjà été initialisé.
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
     * Retourne le nom de cette entrée.
     */
    public String toString() {
        return name;
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
            return this.ID==that.ID   &&   this.band==that.band  &&
                   Utilities.equals(this.name,      that.name)   &&
                   Utilities.equals(this.series,    that.series) &&
                   Utilities.equals(this.series2,   that.series2);
        }
        return false;
    }

    /**
     * Implémentation d'une des composantes d'un {@linkplain ParameterEntry paramètre}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    final class Component implements fr.ird.database.sample.ParameterEntry.Component, Serializable {
        /**
         * Numéro de série pour compatibilité entre différentes versions.
         */
        private static final long serialVersionUID = 9013631151104037206L;

        /**
         * Le paramètre source.
         */
        private final fr.ird.database.sample.ParameterEntry source;

        /**
         * La position relative du paramètre source.
         */
        private final fr.ird.database.sample.RelativePositionEntry position;

        /**
         * L'opération à appliquer sur le paramètre source.
         */
        private final fr.ird.database.sample.OperationEntry operation;

        /**
         * Le poid à donner au paramètre source.
         */
        private final double weight;

        /**
         * Si différent de 0, le facteur par lequelle multiplier la valeur source
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
         * Retourne un numéro unique identifiant cette composante. L'implémentation
         * par défaut suppose que les numéro ID du paramètre cible, source, de la
         * position relative et de l'opération sont tous sur 8 bits.
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
         * Retourne le nom de cette entrée.
         */
        public String toString() {
            return getName();
        }

        /**
         * Retourne un numéro à peu près unique identifiant cette entrée.
         */
        public int hashCode() {
            return getID();
        }

        /**
         * Compare cette entrée avec l'objet spécifié.
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
