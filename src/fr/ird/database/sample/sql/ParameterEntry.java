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
    private static final long serialVersionUID = -6274380414853033347L;

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
}
