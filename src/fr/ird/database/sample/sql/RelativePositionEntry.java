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
 * Impl�mentation de la position spatio-temporelle relative � un �chantillon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class RelativePositionEntry implements fr.ird.database.sample.RelativePositionEntry,
                                             Comparable<RelativePositionEntry>, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -5711725430224190689L;

    /**
     * Une entr�e � utiliser � la place de <code>null</code> dans {@link EnvironmentTable}.
     */
    public static final RelativePositionEntry NULL = new RelativePositionEntry(0, "", 0, false);

    /**
     * Un num�ro unique identifiant cette entr�e.
     */
    private final int ID;

    /**
     * Le nom de cette entr�e.
     */
    private final String name;

    /**
     * Ecart de temps (en nombre de millisecondes) entre la date de l'�chantillon
     * et la date � prendre en compte dans les param�tres environnementaux.
     */
    private final long timeOffset;

    /**
     * Indique si cette position relative devrait �tre s�lectionn�e par d�faut.
     * Cette information peut �tre utilis�e dans une interface utilisateur afin
     * de pr�-selectionner un jeu de positions courrament utilis�.
     */
    private final boolean isDefault;

    /**
     * Construit une nouvelle entr�.
     *
     * @param ID         Un num�ro unique identifiant cette entr�e.
     * @param name       Le nom de cette entr�e.
     * @param timeOffset Ecart de temps (en nombre de millisecondes) entre la date de l'�chantillon
     *                   et la date � prendre en compte dans les param�tres environnementaux.
     * @param isDefault Indique si cette position relative devrait �tre s�lectionn�e par d�faut.
     */
    public RelativePositionEntry(final int     ID,
                                 final String  name,
                                 final long    timeOffset,
                                 final boolean isDefault)
    {
        this.ID         = ID;
        this.name       = name;
        this.timeOffset = timeOffset;
        this.isDefault  = isDefault;
    }

    /**
     * {@inheritDoc}
     */
    public int getID() {
        return ID;
    }

    /**
     * Retourne le nom de cette entr�e.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Date getTime(final SampleEntry sample) {
        final Date time = sample.getTime();
        time.setTime(time.getTime() + timeOffset);
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
    public void applyOffset(final Point2D coordinate, final Date time) {
        if (time != null) {
            time.setTime(time.getTime() + timeOffset);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void applyOppositeOffset(Point2D coordinate, Date time) {
        if (time != null) {
            time.setTime(time.getTime() - timeOffset);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getTypicalTimeOffset() {
        return timeOffset / (float)(24*60*60*1000);
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
     * Retourne le nom de cette entr�, comme {@link #getName}. Ce nom est
     * souvent destin� � appara�tre dans une interface <cite>Swing</cite>.
     */
    public String toString() {
        return (name==null || name.length()==0) ? Resources.format(ResourceKeys.UNNAMED) : name;
    }

    /**
     * Retourne le num�ro {@link #getID ID} de cette entr�.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Compare l'objet sp�cifi� avec cette entr�.
     */
    public boolean equals(final Object object) {
        if (object instanceof RelativePositionEntry) {
            final RelativePositionEntry that = (RelativePositionEntry) object;
            return this.ID         == that.ID         &&
                   this.isDefault  == that.isDefault  &&
                   this.timeOffset == that.timeOffset &&
                   Utilities.equals(this.name, that.name);
        }
        return false;
    }

    /**
     * Compare l'objet sp�cifi� avec cette entr�.
     */
    public int compareTo(final RelativePositionEntry that) {
        if (this.timeOffset < that.timeOffset) return -1;
        if (this.timeOffset > that.timeOffset) return +1;
        return 0;
    }
}
