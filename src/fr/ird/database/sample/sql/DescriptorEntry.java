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
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;


/**
 * Implémentation d'un descripteur du paysage océanique. Cette implémentation par défaut
 * suppose que les données suivent une distribution normale, ce qui implique que la
 * méthode {@link #normalize} n'a pas besoin de faire de transformation. Les classes
 * dérivées {@link Scaled} et {@link LogScaled} implémenteront d'autres distributions.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class DescriptorEntry implements fr.ird.database.sample.DescriptorEntry, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -128569961683213789L;

    /**
     * Le nom court du descripteur.
     */
    private final String name;

    /**
     * Le paramètre environnemental. La table {@link DescriptorTable} construira d'abord un
     * paramètre temporaire, puis complètera sa construction en effectant ses champs non-finaux
     * plus tard. Ce bricolage (via les méthodes {@link SingletonTable#postCreateEntry}) est
     * nécessaire parce qu'un objet <code>ParameterEntry</code> peut contenir indirectement
     * plusieurs <code>DescriptorEntry</code>, chacun contenant lui-même un objet
     * <code>ParameterEntry</code>, etc. Il en résulte des appels recursifs. Or, les pilotes
     * JDBC ne supportent pas tous la création de plusieurs {@link java.sql.ResultSet}s pour
     * le même {@link java.sql.Statement}.
     */
    final ParameterEntry parameter;

    /**
     * La position relative.
     */
    private final RelativePositionEntry position;

    /**
     * L'opération à appliquer.
     */
    private final OperationEntry operation;

    /**
     * La distribution des données. Les codes comprennent (mais ne se limite pas à):
     * 0=normale, 1=log-normale, 2=chi2 à deux degrés de libertés.
     */
    private final int distribution;

    /**
     * Construit un nouveau descripteur.
     *
     * @param name         Le nom court du descripteur.
     * @param parameter    Le paramètre environnemental.
     * @param position     La position relative.
     * @param operation    L'opération à appliquer.
     * @param distribution La distribution des données.
     */
    protected DescriptorEntry(final String                name,
                              final ParameterEntry        parameter,
                              final RelativePositionEntry position,
                              final OperationEntry        operation,
                              final int                   distribution)
    {
        this.name         = name;
        this.parameter    = parameter;
        this.position     = position;
        this.operation    = operation;
        this.distribution = distribution;
    }

    /**
     * Vérifie si ce descripteur est identique à l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object!=null && getClass().equals(object.getClass())) {
            final DescriptorEntry that = (DescriptorEntry) object;
            return Utilities.equals(this.name,       that.name    ) &&
                   Utilities.equals(this.parameter, that.parameter) &&
                   Utilities.equals(this.position,  that.position ) &&
                   Utilities.equals(this.operation, that.operation) &&
                   this.distribution == that.distribution;
        }
        return false;
    }

    /**
     * Retourne un numéro à peu près unique identifiant ce descripteur. L'implémentation par
     * défaut suppose que les numéro ID du paramètre, de la position relative, de l'opération
     * et de la distribution sont tous sur 8 bits.
     */
    public final int getID() {
        return (parameter.getID() << 24) ^
               (position .getID() << 16) ^
               (operation.getID() <<  8) ^
               (distribution);
    }

    /**
     * Retourne un code à peu près unique pour cet objet.
     */
    public int hashCode() {
        return getID();
    }

    /**
     * Retourne une description de cet objet sous forme de chaîne de caractères.
     */
    public String toString() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    public final String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public final String getRemarks() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public final ParameterEntry getParameter() {
        return parameter;
    }

    /**
     * {@inheritDoc}
     */
    public final RelativePositionEntry getRelativePosition() {
        return position;
    }

    /**
     * {@inheritDoc}
     */
    public final OperationEntry getOperation() {
        return operation;
    }

    /**
     * Retourne <code>true</code> si ce descripteur est un <cite>descripteur identité</cite>.
     * Si c'est le cas, alors ce descripteur peut être omis dans les termes du modèle linéaire.
     */
    public final boolean isIdentity() {
        return parameter.isIdentity();
    }

    /**
     * Applique un changement de variable, si nécessaire.
     * L'implémentation par défaut n'effectue aucun changement de variable.
     */
    public double normalize(final double value) {
        return value;
    }

    /**
     * Un descripteur utilisant une relation linéaire pour transformer les données.
     * Cette transformation linéaire ne rendra pas normale une distribution qui n'était
     * pas déjà normale, mais peux servir à réduire et centrer une distribution (ce qui
     * facilite la comparaison des coefficients de corrélation multiple par la suite).
     * Le principal intérêt de cette classe est surtout de servir de classe parente à
     * la classe {@link LogScaled}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    static class Scaled extends DescriptorEntry {
        /**
         * Numéro de série pour compatibilité entre différentes versions.
         */
        private static final long serialVersionUID = -1941331508345926679L;

        /**
         * Le facteur par lequel multiplier les données.
         */
        private final double scale;

        /**
         * La constante à ajouter aux données.
         */
        private final double offset;

        /**
         * Construit un descripteur avec la transformation linéaire spécifiée.
         *
         * @param name         Le nom court du descripteur.
         * @param parameter    Le paramètre environnemental.
         * @param position     La position relative.
         * @param operation    L'opération à appliquer.
         * @param distribution La distribution des données.
         * @param scale        Le facteur par lequel multiplier les données.
         * @param offset       La constante à ajouter aux données.
         */
        protected Scaled(final String                name,
                         final ParameterEntry        parameter,
                         final RelativePositionEntry position,
                         final OperationEntry        operation,
                         final int                   distribution,
                         final double                scale,
                         final double                offset)
        {
            super(name, parameter, position, operation, distribution);
            this.scale  = scale;
            this.offset = offset;
        }

        /**
         * Vérifie si ce descripteur est identique à l'objet spécifié.
         */
        public boolean equals(final Object object) {
            if (super.equals(object)) {
                final Scaled that = (Scaled) object;
                return Double.doubleToLongBits(this.scale)  == Double.doubleToLongBits(that.scale ) &&
                       Double.doubleToLongBits(this.offset) == Double.doubleToLongBits(that.offset);
            }
            return false;
        }

        /**
         * Retourne un numéro à peu près unique, en tenant compte
         * des paramètres de la transformation linéaire.
         */
        public int hashCode() {
            final long code = Double.doubleToLongBits(scale) +
                           37*Double.doubleToLongBits(offset);
            return (int)code ^ (int)(code >>> 32) ^ super.hashCode();
        }

        /**
         * Applique un changement de variable. Cette méthode calcule
         * <code>value&times;scale + offset</code>.
         */
        public double normalize(final double value) {
            return scale*value + offset;
        }
    }

    /**
     * Un descripteur de données suivant une distribution log-normale.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    static final class LogScaled extends Scaled {
        /**
         * Numéro de série pour compatibilité entre différentes versions.
         */
        private static final long serialVersionUID = 5456692173042146623L;

        /**
         * Construit un descripteur avec la transformation linéaire spécifiée.
         *
         * @param name         Le nom court du descripteur.
         * @param parameter    Le paramètre environnemental.
         * @param position     La position relative.
         * @param operation    L'opération à appliquer.
         * @param distribution La distribution des données.
         * @param scale        Le facteur par lequel multiplier les données.
         * @param offset       La constante à ajouter aux données.
         */
        protected LogScaled(final String                name,
                            final ParameterEntry        parameter,
                            final RelativePositionEntry position,
                            final OperationEntry        operation,
                            final int                   distribution,
                            final double                scale,
                            final double                offset)
        {
            super(name, parameter, position, operation, distribution, scale, offset);
        }

        /**
         * Retourne un numéro à peu près unique, en tenant compte
         * des la transformation logarithmique.
         */
        public int hashCode() {
            return super.hashCode() ^ (int)serialVersionUID;
        }

        /**
         * Applique un changement de variable. Cette méthode calcule
         * <code>log(value&times;scale + offset)</code>.
         */
        public double normalize(final double value) {
            return Math.log(super.normalize(value));
        }
    }
}
