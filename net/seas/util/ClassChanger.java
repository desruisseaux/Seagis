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

// Standard set of Java objects.
import java.lang.Number;
import java.lang.Long;
import java.util.Date;


/** 
 * Transforme un objet d'une classe vers une autre. Cette classe sert principalement
 * à convertir en {@link Number} des objets d'une autre classe, par exemple {@link Date}. Une méthode
 * statique, {@link #toNumber}, se charge d'effectuer ce genre de conversion en prenant en compte toutes
 * les classes qui auront été déclarées à <code>ClassChanger</code>.
 *
 * <p>Pour déclarer une nouvelle classe, on peut procéder comme suit. L'exemple ci-dessous
 * inscrit une classe qui convertira des objets {@link Date} en objets {@link Long}. Notez qu'il ne s'agit
 * que d'un exemple. Ce convertisseur n'a pas besoin d'être déclaré car <code>ClassChange</code> comprend
 * déjà les objets {@link Date} par défaut.</p>
 *
 * <blockquote><pre>
 * &nbsp;ClassChanger.register(new ClassChanger(Date.class)
 * &nbsp;{
 * &nbsp;    protected Number convert(final Object o)
 * &nbsp;    {return new Long(((Date) o).getTime());}
 * &nbsp;});
 * </pre></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class ClassChanger
{
	/**
	 * Liste des classes d'objets pouvant être convertis en nombre. Cette liste contiendra
	 * par défaut quelques instances de {@link ClassChanger} pour quelques classes standards
	 * du Java, telle que {@link Date}. Toutefois, d'autres objets pourront être ajoutés par
	 * la suite. Cette liste est <u>ordonnée</u>. Les classe le plus hautes dans la hierarchie
	 * (les classes parentes) doivent apparaître à la fin.
	 */
	private static ClassChanger[] list=new ClassChanger[]
	{
		new ClassChanger(Date.class)
		{
			protected Number convert(final Object object)
			{return new Long(((Date) object).getTime());}
		}
	};

	/**
	 * Classe parente des objets sur lesquels pourra
	 * s'appliquer la méthode {@link #convert}.
	 */
	private final Class classe;

	/**
	 * Construit un objet <code>ClassChanger</code> qui
	 * convertira des objets de la classe spécifiée.
	 *
	 * @param classe Classe parente des objets sur lesquels pourra
	 *               s'appliquer la méthode {@link #convert}.
	 * @throws NullPointerException si <code>classe</code> est nul.
	 */
	protected ClassChanger(final Class classe) throws NullPointerException
	{if ((this.classe=classe)==null) throw new NullPointerException();}

	/**
	 * Inscrit un nouvel objet <code>ClassChanger</code>. Les objets <code>ClassChanger</code> inscrits
	 * ici seront pris en compte par la méthode {@link #toNumber}. Si un objet <code>ClassChanger</code>
	 * existait déjà pour une même classe, une exception sera lancée. Cette spécification est justifiée
	 * par le fait qu'on enregistre souvent un objet <code>ClassChanger</code> lors de l'initialisation
	 * d'une classe qui vient d'être chargée pour la première fois. En interdisant tout changements aux
	 * objets <code>ClassChanger</code> après l'initialisation d'une classe, on évite que la façon de
	 * convertir des objets en nombres réels ne change au cours d'une exécution de la machine virtuelle.
	 * Notez que si <code>converter</code> ne peut pas prendre en charge une même classe que celle d'un
	 * autre objet <code>ClassChanger</code>, il peut toutefois prendre en charge une classe parente ou
	 * une classe fille.
	 *
	 * @param  converter Convertisseur à ajouter à la liste des convertisseurs déjà existants.
	 * @throws IllegalStateException si un autre objet <code>ClassChanger</code> prennait déjà
	 *         en charge la même classe (l'argument <code>classe</code> déclaré au constructeur)
	 *         que <code>converter</code>.
	 */
	public static synchronized void register(final ClassChanger converter) throws IllegalStateException
	{
		int i;
		for (i=0; i<list.length; i++)
		{
			if (list[i].classe.isAssignableFrom(converter.classe))
			{
				/*
				 * On a trouvé un convertisseur qui utilisait
				 * une classe parente. Le nouveau convertisseur
				 * devra s'insérer avant son parent. Mais on va
				 * d'abord s'assurer qu'il n'existait pas déjà
				 * un convertisseur pour cette classe.
				 */
				for (int j=i; j<list.length; j++)
				{
					if (list[j].classe.equals(converter.classe))
					{
						throw new IllegalStateException(list[j].toString());
					}
				}
				break;
			}
		}
		list = XArray.insert(list, i, 1);
		list[i] = converter;
	}

	/**
	 * Convertit en {@link Number} l'objet <code>object</code> spécifié.
	 * Si <code>ClassChanger</code> ne sait pas faire la conversion (par
	 * exemple parce que <code>objet</code> est d'une classe qui n'a pas
	 * été enregistrée avec {@link #register}), ou si <code>object</code>
	 * était nul, alors cette méthode retourne <code>null</code>.
	 *
	 * @param  object Objet à convertir.
	 * @return <code>object</code> s'il était déjà de la classe {@link Number},
	 *         sinon un objet {@link Number} contenant la même valeur numérique
	 *         que <code>object</code>, ou <code>null</code> si la conversion
	 *         n'a pas pu se faire.
	 */
	public static Number toNumber(final Object object)
	{
		if (object!=null)
		{
			if (object instanceof Number)
			{
				return (Number) object;
			}
			final Class classe=object.getClass();
			synchronized (ClassChanger.class)
			{
				for (int i=0; i<list.length; i++)
				{
					if (list[i].classe.isAssignableFrom(classe))
					{
						final Number n=list[i].convert(object);
						if (n!=null) return n;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Convertie en nombre l'objet <code>object</code> spécifié. Si l'objet <code>object</code> n'est pas
	 * de la classe attendue, cette méthode lance un {@link ClassCastException}.  Sinon, si cette méthode
	 * n'a pas pu faire la conversion pour une autre raison, retourne <code>null</code>.
	 *
	 * @param  object Objet à convertir en nombre.
	 * @return Valeur numérique de l'objet, ou <code>null</code> si la conversion n'a pas pu se faire.
	 * @throws ClassCastException si l'objet n'est pas de la classe spécifiée au constructeur.
	 */
	protected abstract Number convert(final Object object) throws ClassCastException;

	/**
	 * Retourne une chaîne de caractère représentant ce convertisseur. La
	 * chaîne retournée sera de la forme <code>ClassChanger[Date]</code>
	 * par exemple.
	 */
	public String toString()
	{return "ClassChanger["+XClass.getShortName(classe)+']';}

	/**
	 * Envoie sur le périphérique de sortie standard tous les
	 * objets <code>ClassChanger</code> qui sont inscrits.
	 */
	public static void main(final String[] args)
	{
		for (int i=0; i<list.length; i++)
			System.out.println(list[i]);
	}
}
