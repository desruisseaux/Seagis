/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.database.coverage;

// J2SE dependencies.
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

// Geotools dependencies.
import org.geotools.util.RangeSet;
import org.geotools.measure.Latitude;
import org.geotools.measure.Longitude;


/**
 * Contient les plages de temps et de coordonnées couvertes par les images. Ces informations
 * sont fournies sous forme d'objets {@link RangeSet}, ce qui permet de connaître les trous
 * dans les données. Un objet <code>CoverageRanges</code> peut aussi de manière opportuniste
 * contenir la liste des entrés {@link CoverageEntry}.
 *
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaux
 */
public class CoverageRanges implements Serializable {
    /**
     * Numéro de série pour compatibilités avec différentes versions.
     */
    private static final long serialVersionUID = -4275093602674070015L;

    /**
     * Les plages de longitudes, ou <code>null</code> si cette information n'a pas été demandée.
     */
    public final RangeSet x;
    
    /**
     * Les plages de latitudes, ou <code>null</code> si cette information n'a pas été demandée.
     */
    public final RangeSet y;
    
    /**
     * Les plages de temps, ou <code>null</code> si cette information n'a pas été demandée.
     */    
    public final RangeSet t;
    
    /**
     * Liste des images, ou <code>null</code> si cette information n'a pas été demandée.
     */
    public final List<CoverageEntry> entries;

    /** 
     * Construit des plages initialement vides pour les dimensions spécifiées.
     *
     * @param x <code>true</code> pour obtenir les plages de longitudes.
     * @param y <code>true</code> pour obtenir les plages de latitudes.
     * @param t <code>true</code> pour obtenir les plages de temps.
     * @param entries <code>true</code> pour obtenir les entrés
     *        (comme dans à un appel à {@link CoverageTable#getEntries}).
     */
    public CoverageRanges(final boolean x, final boolean y, final boolean t, final boolean entries) {
        this.x       = x       ? new RangeSet(Longitude.class)  : null;
        this.y       = y       ? new RangeSet(Latitude .class)  : null;
        this.t       = t       ? new RangeSet(Date     .class)  : null;
        this.entries = entries ? new ArrayList<CoverageEntry>() : null;
    }
}
