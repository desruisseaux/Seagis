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
import java.util.Date;
import java.io.Serializable;
import java.awt.geom.Point2D;

// Geotools
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.sample.SampleEntry;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Implémentation de la position spatio-temporelle relative à un échantillon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class RelativePositionEntry implements fr.ird.database.sample.RelativePositionEntry,
                                             Comparable<RelativePositionEntry>, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -606226607519367847L;

    /**
     * Une entrée à utiliser à la place de <code>null</code> dans {@link EnvironmentTable}.
     */
    public static final RelativePositionEntry NULL = new RelativePositionEntry(0, "", 0, false);

    /**
     * Un numéro unique identifiant cette entrée.
     */
    private final int ID;

    /**
     * Le nom de cette entrée.
     */
    private final String name;

    /**
     * Ecart de temps (en nombre de millisecondes) entre la date de l'échantillon
     * et la date à prendre en compte dans les paramètres environnementaux.
     */
    private final long timeLag;

    /**
     * Indique si cette position relative devrait être sélectionnée par défaut.
     * Cette information peut être utilisée dans une interface utilisateur afin
     * de pré-selectionner un jeu de positions courrament utilisé.
     */
    private final boolean isDefault;

    /**
     * Construit une nouvelle entré.
     *
     * @param ID        Un numéro unique identifiant cette entrée.
     * @param name      Le nom de cette entrée.
     * @param timeLag   Ecart de temps (en nombre de millisecondes) entre la date de l'échantillon
     *                  et la date à prendre en compte dans les paramètres environnementaux.
     * @param isDefault Indique si cette position relative devrait être sélectionnée par défaut.
     */
    public RelativePositionEntry(final int     ID,
                                 final String  name,
                                 final long    timeLag,
                                 final boolean isDefault)
    {
        this.ID        = ID;
        this.name      = name;
        this.timeLag   = timeLag;
        this.isDefault = isDefault;
    }

    /**
     * {@inheritDoc}
     */
    public int getID() {
        return ID;
    }

    /**
     * Retourne le nom de cette entrée.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Date getTime(final SampleEntry sample) {
        final Date time = sample.getTime();
        time.setTime(time.getTime() + timeLag);
        return time;
    }

    /**
     * {@inheritDoc}
     */
    public Point2D getCoordinate(final SampleEntry sample) {
        return sample.getCoordinate();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * {@inheritDoc}
     */
    public String getRemarks() {
        return null;
    }

    /**
     * Retourne le nom de cette entré, comme {@link #getName}.
     */
    public String toString() {
        return (name==null || name.length()==0) ? Resources.format(ResourceKeys.UNNAMED) : name;
    }

    /**
     * Retourne le numéro {@link #getID ID} de cette entré.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Compare l'objet spécifié avec cette entré.
     */
    public boolean equals(final Object object) {
        if (object instanceof RelativePositionEntry) {
            final RelativePositionEntry that = (RelativePositionEntry) object;
            return this.ID        == that.ID        &&
                   this.isDefault == that.isDefault &&
                   this.timeLag   == that.timeLag   &&
                   Utilities.equals(this.name, that.name);
        }
        return false;
    }

    /**
     * Compare l'objet spécifié avec cette entré.
     */
    public int compareTo(final RelativePositionEntry that) {
        if (this.timeLag < that.timeLag) return -1;
        if (this.timeLag > that.timeLag) return +1;
        return 0;
    }
}
