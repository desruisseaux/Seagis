/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 */
package net.seas.util;

// Standard set of Java objects.
import java.lang.Number;
import java.lang.Long;
import java.util.Date;


/** 
 * Transforme un objet d'une classe vers une autre. Cette classe sert principalement
 * � convertir en {@link Number} des objets d'une autre classe, par exemple {@link Date}. Une m�thode
 * statique, {@link #toNumber}, se charge d'effectuer ce genre de conversion en prenant en compte toutes
 * les classes qui auront �t� d�clar�es � <code>ClassChanger</code>.
 *
 * <p>Pour d�clarer une nouvelle classe, on peut proc�der comme suit. L'exemple ci-dessous
 * inscrit une classe qui convertira des objets {@link Date} en objets {@link Long}. Notez qu'il ne s'agit
 * que d'un exemple. Ce convertisseur n'a pas besoin d'�tre d�clar� car <code>ClassChange</code> comprend
 * d�j� les objets {@link Date} par d�faut.</p>
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
	 * Liste des classes d'objets pouvant �tre convertis en nombre. Cette liste contiendra
	 * par d�faut quelques instances de {@link ClassChanger} pour quelques classes standards
	 * du Java, telle que {@link Date}. Toutefois, d'autres objets pourront �tre ajout�s par
	 * la suite. Cette liste est <u>ordonn�e</u>. Les classe le plus hautes dans la hierarchie
	 * (les classes parentes) doivent appara�tre � la fin.
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
	 * s'appliquer la m�thode {@link #convert}.
	 */
	private final Class classe;

	/**
	 * Construit un objet <code>ClassChanger</code> qui
	 * convertira des objets de la classe sp�cifi�e.
	 *
	 * @param classe Classe parente des objets sur lesquels pourra
	 *               s'appliquer la m�thode {@link #convert}.
	 * @throws NullPointerException si <code>classe</code> est nul.
	 */
	protected ClassChanger(final Class classe) throws NullPointerException
	{if ((this.classe=classe)==null) throw new NullPointerException();}

	/**
	 * Inscrit un nouvel objet <code>ClassChanger</code>. Les objets <code>ClassChanger</code> inscrits
	 * ici seront pris en compte par la m�thode {@link #toNumber}. Si un objet <code>ClassChanger</code>
	 * existait d�j� pour une m�me classe, une exception sera lanc�e. Cette sp�cification est justifi�e
	 * par le fait qu'on enregistre souvent un objet <code>ClassChanger</code> lors de l'initialisation
	 * d'une classe qui vient d'�tre charg�e pour la premi�re fois. En interdisant tout changements aux
	 * objets <code>ClassChanger</code> apr�s l'initialisation d'une classe, on �vite que la fa�on de
	 * convertir des objets en nombres r�els ne change au cours d'une ex�cution de la machine virtuelle.
	 * Notez que si <code>converter</code> ne peut pas prendre en charge une m�me classe que celle d'un
	 * autre objet <code>ClassChanger</code>, il peut toutefois prendre en charge une classe parente ou
	 * une classe fille.
	 *
	 * @param  converter Convertisseur � ajouter � la liste des convertisseurs d�j� existants.
	 * @throws IllegalStateException si un autre objet <code>ClassChanger</code> prennait d�j�
	 *         en charge la m�me classe (l'argument <code>classe</code> d�clar� au constructeur)
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
				 * On a trouv� un convertisseur qui utilisait
				 * une classe parente. Le nouveau convertisseur
				 * devra s'ins�rer avant son parent. Mais on va
				 * d'abord s'assurer qu'il n'existait pas d�j�
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
	 * Convertit en {@link Number} l'objet <code>object</code> sp�cifi�.
	 * Si <code>ClassChanger</code> ne sait pas faire la conversion (par
	 * exemple parce que <code>objet</code> est d'une classe qui n'a pas
	 * �t� enregistr�e avec {@link #register}), ou si <code>object</code>
	 * �tait nul, alors cette m�thode retourne <code>null</code>.
	 *
	 * @param  object Objet � convertir.
	 * @return <code>object</code> s'il �tait d�j� de la classe {@link Number},
	 *         sinon un objet {@link Number} contenant la m�me valeur num�rique
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
	 * Convertie en nombre l'objet <code>object</code> sp�cifi�. Si l'objet <code>object</code> n'est pas
	 * de la classe attendue, cette m�thode lance un {@link ClassCastException}.  Sinon, si cette m�thode
	 * n'a pas pu faire la conversion pour une autre raison, retourne <code>null</code>.
	 *
	 * @param  object Objet � convertir en nombre.
	 * @return Valeur num�rique de l'objet, ou <code>null</code> si la conversion n'a pas pu se faire.
	 * @throws ClassCastException si l'objet n'est pas de la classe sp�cifi�e au constructeur.
	 */
	protected abstract Number convert(final Object object) throws ClassCastException;

	/**
	 * Retourne une cha�ne de caract�re repr�sentant ce convertisseur. La
	 * cha�ne retourn�e sera de la forme <code>ClassChanger[Date]</code>
	 * par exemple.
	 */
	public String toString()
	{return "ClassChanger["+XClass.getShortName(classe)+']';}

	/**
	 * Envoie sur le p�riph�rique de sortie standard tous les
	 * objets <code>ClassChanger</code> qui sont inscrits.
	 */
	public static void main(final String[] args)
	{
		for (int i=0; i<list.length; i++)
			System.out.println(list[i]);
	}
}
