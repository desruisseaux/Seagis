/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 */
package net.seas.util;

// Miscellaneous
import java.util.Arrays;


/**
 * Simple operations on {@link java.lang.String} and {@link java.lang.StringBuffer} objects.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class XString
{
	/**
	 * Toute construction d'objet
	 * de cette classe est interdites.
	 */
	private XString()
	{}

	/**
	 * Liste de chaînes de caractères ne contenant que des
	 * espaces blancs. Ces chaînes auront différentes longueurs
	 */
	private static final String[] spacesFactory = new String[16];

	/**
	 * Renvoie une chaîne de caractères ne contenant que des espaces. Cette
	 * chaîne pourra être transmise en argument à des méthodes telles que
	 * {@link java.lang.StringBuffer#insert(int,char[])} pour aligner les
	 * enregistrements d'un tableau écrit avec une police non-proportionelle.
	 *
	 * Afin d'améliorer la performance, cette méthode tient une liste spéciale
	 * de chaînes courtes (moins de 16 caractères), qui seront retournées d'un
	 * appel à l'autre plutôt que de créer de nouvelles chaînes à chaque fois.
	 * 
	 * @param  length Longueur souhaitée de la chaîne. Cette longueur peut
	 *         être négative. Dans ce cas, la chaîne retournée aura une
	 *         longueur de 0.
	 * @return Chaîne de caractères de longueur <code>length</code>
	 *         ne contenant que des espaces.
	 */
	public static String spaces(int length)
	{
		final int last=spacesFactory.length-1;
		if (length<0) length=0;
		if (length <= last)
		{
			if (spacesFactory[length]==null)
			{
				synchronized (spacesFactory)
				{
					if (spacesFactory[last]==null)
					{
						char[] blancs = new char[last];
						Arrays.fill(blancs, ' ');
						spacesFactory[last]=new String(blancs).intern();
					}
					spacesFactory[length] = spacesFactory[last].substring(0,length).intern();
				}
			}
			return spacesFactory[length];
		}
		else
		{
			char[] blancs = new char[length];
			Arrays.fill(blancs, ' ');
			return new String(blancs);
		}
	}
}
