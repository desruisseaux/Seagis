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
 */
package fr.ird.database.coverage.sql;

// J2SE dependencies
import java.util.Arrays;
import java.rmi.RemoteException;


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
    private static final long serialVersionUID = -6345332433214206310L;

    /**
     * La p�riode "normale" des images de cette s�rie (en nombre
     * de jours), ou {@link Double#NaN} si elle est inconnue.
     */
    private final double period;

    /**
     * Les sous-s�ries, ou <code>null</code> si cette information n'a pas encore �t� d�finie.
     */
    Entry[] subseries;

    /**
     * Construit une nouvelle r�f�rence.
     */
    protected SeriesEntry(final String     table,
                          final Comparable identifier,
                          final String     remarks,
                          final double     period) throws RemoteException
    {
        super(table, identifier, remarks);
        this.period = period;
    }

    /**
     * {@inheritDoc}
     */
    public double getPeriod() throws RemoteException {
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
            return Double.doubleToLongBits(period) == Double.doubleToLongBits(that.period) &&
                   Arrays.equals(subseries, that.subseries);
        }
        return false;
    }
}
