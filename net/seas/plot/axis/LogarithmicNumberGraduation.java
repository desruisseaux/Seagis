/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contacts: Observatoire du Saint-Laurent         Michel Petit
 *           Institut Maurice Lamontagne           Institut de Recherche pour le Développement
 *           850 de la Mer, C.P. 1000              500 rue Jean-François Breton
 *           Mont-Joli (Québec)                    34093 Montpellier
 *           G5H 3Z4                               France
 *           Canada
 *
 *           mailto:osl@osl.gc.ca                  mailto:Michel.Petit@teledetection.fr
 */
package net.seas.plot.axis;

// Divers
import java.util.Locale;
import org.geotools.units.Unit;

/**
 * A graduation using numbers on a logarithmic axis.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class LogarithmicNumberGraduation extends NumberGraduation
{
    /**
     * Contruct a new logarithmic graduation with the supplied units.
     */
    public LogarithmicNumberGraduation(final Unit unit)
    {super(unit);}

    /**
     * Construct or reuse an iterator. This method override
     * the default {@link NumberGraduation} implementation.
     */
    NumberIterator getTickIterator(final TickIterator reuse, final Locale locale)
    {
        if (reuse!=null && reuse.getClass().equals(LogarithmicNumberIterator.class))
        {
            final NumberIterator it = (NumberIterator) reuse;
            it.setLocale(locale);
            return it;
        }
        else
        {
            return new LogarithmicNumberIterator(locale);
        }
    }
}
