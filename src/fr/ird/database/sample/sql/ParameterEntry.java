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
    private static final long serialVersionUID = -6274380414853033347L;

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
}
