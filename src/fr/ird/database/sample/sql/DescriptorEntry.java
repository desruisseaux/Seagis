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
import java.io.Serializable;

// Geotools
import org.geotools.resources.Utilities;


/**
 * Impl�mentation d'un descripteur du paysage oc�anique. Cette impl�mentation par d�faut
 * suppose que les donn�es suivent une distribution normale, ce qui implique que la
 * m�thode {@link #normalize} n'a pas besoin de faire de transformation. Les classes
 * d�riv�es {@link Scaled} et {@link LogScaled} impl�menteront d'autres distributions.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class DescriptorEntry implements fr.ird.database.sample.DescriptorEntry, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -128569961683213789L;

    /**
     * Le nom court du descripteur.
     */
    private final String name;

    /**
     * Le param�tre environnemental. La table {@link DescriptorTable} construira d'abord un
     * param�tre temporaire, puis compl�tera sa construction en effectant ses champs non-finaux
     * plus tard. Ce bricolage (via les m�thodes {@link SingletonTable#postCreateEntry}) est
     * n�cessaire parce qu'un objet <code>ParameterEntry</code> peut contenir indirectement
     * plusieurs <code>DescriptorEntry</code>, chacun contenant lui-m�me un objet
     * <code>ParameterEntry</code>, etc. Il en r�sulte des appels recursifs. Or, les pilotes
     * JDBC ne supportent pas tous la cr�ation de plusieurs {@link java.sql.ResultSet}s pour
     * le m�me {@link java.sql.Statement}.
     */
    final ParameterEntry parameter;

    /**
     * La position relative.
     */
    private final RelativePositionEntry position;

    /**
     * L'op�ration � appliquer.
     */
    private final OperationEntry operation;

    /**
     * La distribution des donn�es. Les codes comprennent (mais ne se limite pas �):
     * 0=normale, 1=log-normale, 2=chi2 � deux degr�s de libert�s.
     */
    private final int distribution;

    /**
     * Construit un nouveau descripteur.
     *
     * @param name         Le nom court du descripteur.
     * @param parameter    Le param�tre environnemental.
     * @param position     La position relative.
     * @param operation    L'op�ration � appliquer.
     * @param distribution La distribution des donn�es.
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
     * V�rifie si ce descripteur est identique � l'objet sp�cifi�.
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
     * Retourne un num�ro � peu pr�s unique identifiant ce descripteur. L'impl�mentation par
     * d�faut suppose que les num�ro ID du param�tre, de la position relative, de l'op�ration
     * et de la distribution sont tous sur 8 bits.
     */
    public final int getID() {
        return (parameter.getID() << 24) ^
               (position .getID() << 16) ^
               (operation.getID() <<  8) ^
               (distribution);
    }

    /**
     * Retourne un code � peu pr�s unique pour cet objet.
     */
    public int hashCode() {
        return getID();
    }

    /**
     * Retourne une description de cet objet sous forme de cha�ne de caract�res.
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
     * Retourne <code>true</code> si ce descripteur est un <cite>descripteur identit�</cite>.
     * Si c'est le cas, alors ce descripteur peut �tre omis dans les termes du mod�le lin�aire.
     */
    public final boolean isIdentity() {
        return parameter.isIdentity();
    }

    /**
     * Applique un changement de variable, si n�cessaire.
     * L'impl�mentation par d�faut n'effectue aucun changement de variable.
     */
    public double normalize(final double value) {
        return value;
    }

    /**
     * Un descripteur utilisant une relation lin�aire pour transformer les donn�es.
     * Cette transformation lin�aire ne rendra pas normale une distribution qui n'�tait
     * pas d�j� normale, mais peux servir � r�duire et centrer une distribution (ce qui
     * facilite la comparaison des coefficients de corr�lation multiple par la suite).
     * Le principal int�r�t de cette classe est surtout de servir de classe parente �
     * la classe {@link LogScaled}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    static class Scaled extends DescriptorEntry {
        /**
         * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
         */
        private static final long serialVersionUID = -1941331508345926679L;

        /**
         * Le facteur par lequel multiplier les donn�es.
         */
        private final double scale;

        /**
         * La constante � ajouter aux donn�es.
         */
        private final double offset;

        /**
         * Construit un descripteur avec la transformation lin�aire sp�cifi�e.
         *
         * @param name         Le nom court du descripteur.
         * @param parameter    Le param�tre environnemental.
         * @param position     La position relative.
         * @param operation    L'op�ration � appliquer.
         * @param distribution La distribution des donn�es.
         * @param scale        Le facteur par lequel multiplier les donn�es.
         * @param offset       La constante � ajouter aux donn�es.
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
         * V�rifie si ce descripteur est identique � l'objet sp�cifi�.
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
         * Retourne un num�ro � peu pr�s unique, en tenant compte
         * des param�tres de la transformation lin�aire.
         */
        public int hashCode() {
            final long code = Double.doubleToLongBits(scale) +
                           37*Double.doubleToLongBits(offset);
            return (int)code ^ (int)(code >>> 32) ^ super.hashCode();
        }

        /**
         * Applique un changement de variable. Cette m�thode calcule
         * <code>value&times;scale + offset</code>.
         */
        public double normalize(final double value) {
            return scale*value + offset;
        }
    }

    /**
     * Un descripteur de donn�es suivant une distribution log-normale.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    static final class LogScaled extends Scaled {
        /**
         * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
         */
        private static final long serialVersionUID = 5456692173042146623L;

        /**
         * Construit un descripteur avec la transformation lin�aire sp�cifi�e.
         *
         * @param name         Le nom court du descripteur.
         * @param parameter    Le param�tre environnemental.
         * @param position     La position relative.
         * @param operation    L'op�ration � appliquer.
         * @param distribution La distribution des donn�es.
         * @param scale        Le facteur par lequel multiplier les donn�es.
         * @param offset       La constante � ajouter aux donn�es.
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
         * Retourne un num�ro � peu pr�s unique, en tenant compte
         * des la transformation logarithmique.
         */
        public int hashCode() {
            return super.hashCode() ^ (int)serialVersionUID;
        }

        /**
         * Applique un changement de variable. Cette m�thode calcule
         * <code>log(value&times;scale + offset)</code>.
         */
        public double normalize(final double value) {
            return Math.log(super.normalize(value));
        }
    }
}
