/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.coverage.sql;


/**
 * R�f�rence vers une s�rie.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class SeriesEntry extends Entry implements fr.ird.database.coverage.SeriesEntry {
    /**
     * Num�ro de s�ries pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -8066761678115148515L;

    /**
     * Num�ro ID du format.
     */
    final int format;

    /**
     * Num�ro ID de la s�ries des aper�us. S'il n'y en a pas, alors ce num�ro
     * doit �tre le m�me que {@link #getID}.
     */
    final int quicklook;

    /**
     * La p�riode "normale" des images de cette s�rie (en nombre
     * de jours), ou {@link Double#NaN} si elle est inconnue.
     */
    private final double period;

    /**
     * Construit une nouvelle r�f�rence.
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
     * V�rifie si l'objet identifi�e est identique � cette s�rie.
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
