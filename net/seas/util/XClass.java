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
import java.lang.reflect.Modifier;


/**
 * Simple operations on classes object.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class XClass
{
	/**
	 * Toute construction d'objet
	 * de cette classe est interdites.
	 */
	private XClass()
	{}

	/**
	 * Retourne le nom court de la classe de l'objet spécifié. Cette méthode retournera
	 * par exemple <code>String</code> au lieu de <code>java.lang.String</code>. Si la
	 * classe spécifiée est privée, alors cette méthode remonte la hierarchie jusqu'à ce
	 * qu'il trouve une qui ne l'est pas.
	 */
	public static String getShortClassName(final Object object)
	{return getShortName(object.getClass());}

	/**
	 * Retourne le nom court de la classe spécifiée. Cette méthode retournera
	 * par exemple <code>String</code> au lieu de <code>java.lang.String</code>.
	 * Si la classe spécifiée est privée, alors cette méthode remonte la
	 * hierarchie jusqu'à ce qu'il trouve une qui ne l'est pas.
	 */
	public static String getShortName(Class classe)
	{
		while (Modifier.isPrivate(classe.getModifiers()))
		{
			final Class c=classe.getSuperclass();
			if (c==null) break;
			classe=c;
		}
		String name = classe.getName();
		int   lower = name.lastIndexOf('.');
		int   upper = name.length();
		return name.substring(lower+1, upper).replace('$','.');
	}

	/**
	 * Retourne la classe commune aux deux classes spécifiées.  Si les deux classes sont égales, alors
	 * leur classe est retournée.    Si une des classes est une classe parente de l'autre, alors cette
	 * classe parente est retournée. Sinon, la hierarchie des deux classes est remontée jusqu'à ce que
	 * l'on trouve une classe parente commune aux deux classes.   Si un des arguments est nul, l'autre
	 * classe sera retournée. Si les deux arguments sont nuls, alors cette méthode retourne<code>null</code>.
	 *
	 * @param class1 Première classe.
	 * @param class2 Deuxième classe.
	 * @return Classe parente commune aux deux classes.
	 */
	public static Class getCommonClass(Class class1, Class class2)
	{
		while (true)
		{
			if (class1==null) return class2;
			if (class2==null) return class1;
			if (class1.isAssignableFrom(class2)) return class1;
			if (class2.isAssignableFrom(class1)) return class2;
			class1 = class1.getSuperclass();
			class2 = class2.getSuperclass();
		}
	}

	/**
	 * Convenience method for testing two objects for
	 * equality. One or both object may be null.
	 */
	public static boolean equals(final Object object1, final Object object2)
	{return (object1==object2) || (object1!=null && object1.equals(object2));}
}
