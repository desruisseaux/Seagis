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

// Rendering hints
import java.awt.RenderingHints;


/**
 * Rendering hints for tick's graduation.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class RenderingHintKey extends RenderingHints.Key
{
    /**
     * Construct a rendering hint key.
     */
    protected RenderingHintKey(final int key)
    {super(key);}

    /**
     * Returns <code>true</code> if the specified
     * object is a valid value for this key.
     */
    public boolean isCompatibleValue(final Object value)
    {return (value instanceof Number);}
}
