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
package fr.ird.database.coverage.sql;


/**
 * Référence vers une série.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class SeriesEntry extends Entry implements fr.ird.database.coverage.SeriesEntry {
    /**
     * Numéro de séries pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -8066761678115148515L;

    /**
     * Numéro ID du format.
     */
    final int format;

    /**
     * Numéro ID de la séries des aperçus. S'il n'y en a pas, alors ce numéro
     * doit être le même que {@link #getID}.
     */
    final int quicklook;

    /**
     * La période "normale" des images de cette série (en nombre
     * de jours), ou {@link Double#NaN} si elle est inconnue.
     */
    private final double period;

    /**
     * Construit une nouvelle référence.
     */
    protected SeriesEntry(final String table,
                          final String name,
                          final int    ID,
                          final String remarks,
                          final int    format,
                          final double period,
                          final int    quicklook)
    {
        super(table, name, ID, remarks);
        this.format    = format;
        this.period    = period;
        this.quicklook = quicklook;
    }

    /**
     * {@inheritDoc}
     */
    public double getPeriod() {
        return period;
    }

    /**
     * Vérifie si l'objet identifiée est identique à cette série.
     */
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (super.equals(object)) {
            final SeriesEntry that = (SeriesEntry) object;
            return format == that.format &&
                   Double.doubleToLongBits(period) == Double.doubleToLongBits(that.period);
        }
        return false;
    }
}
