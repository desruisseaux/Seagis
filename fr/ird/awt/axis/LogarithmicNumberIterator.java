/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 P�ches et Oc�ans Canada
 *               2000 Institut de Recherche pour le D�veloppement
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
 *           Institut Maurice Lamontagne           Institut de Recherche pour le D�veloppement
 *           850 de la Mer, C.P. 1000              500 rue Jean-Fran�ois Breton
 *           Mont-Joli (Qu�bec)                    34093 Montpellier
 *           G5H 3Z4                               France
 *           Canada
 *
 *           mailto:osl@osl.gc.ca                  mailto:Michel.Petit@teledetection.fr
 */
package fr.ird.awt.axis;

// Divers
import java.util.Locale;
import org.geotools.resources.XMath;


/**
 * It�rateur balayant les barres et �tiquettes de graduation d'un axe logarithmique.
 * Cet it�rateur retourne les positions des graduations � partir de la valeur minimale
 * jusqu'� la valeur maximale.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class LogarithmicNumberIterator extends NumberIterator
{
    /**
     * Construit un it�rateur par d�faut. La m�thode {@link #init}
     * <u>doit</u> �tre appel�e avant que cet it�rateur ne soit
     * utilisable.
     *
     * @param locale Conventions � utiliser pour le formatage des nombres.
     */
    protected LogarithmicNumberIterator(final Locale locale)
    {super(locale);}

    /**
     * Retourne la valeur de la graduation courante. Cette m�thode
     * peut �tre appel�e pour une graduation majeure ou mineure.
     */
    public double getValue()
    {return XMath.log10(super.getValue());}
}
