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

// Collections
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;

// Références
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

// Exceptions
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;


/**
 * Ensemble d'objets référencés par des liens faibles.  La méthode {@link #add} permet d'ajouter
 * un nouvel élément à cet ensemble, et la méthode {@link #remove} d'en retirer. Toutefois, chaque élément ajouté
 * à cet ensemble pourra être automatiquement retiré (sans appel explicite à {@link #remove}) si aucune référence
 * forte vers cet élément n'est tenue ailleurs dans cette machine virtuelle Java. Par exemple, supposons que l'on
 * veut tenir une liste de tous les objets {@link fr.ird.units.Unit} créés et en cours d'utilisation. A chaque fois
 * que l'on a besoin d'une unité, on souhaite retourner un objet <code>Unit</code> précédemment créé plutôt que de
 * gaspiller de la mémoire avec des dizaines d'exemplaires identiques de la même chose. On pourrait alors implémenter
 * une méthode similaire à {@link java.lang.String#intern} de la façon suivante:
 *
 * <blockquote><pre>
 * &nbsp;private static final WeakHashSet pool=new WeakHashSet();
 * &nbsp;
 * &nbsp;final Unit intern()
 * &nbsp;{
 * &nbsp;    synchronized (pool)
 * &nbsp;    {
 * &nbsp;        Unit unit = (Unit) pool.get(this);
 * &nbsp;        if (unit!=null) return unit;
 * &nbsp;        pool.add(this);
 * &nbsp;        return this;
 * &nbsp;    }
 * &nbsp;}
 * </pre></blockquote>
 *
 * Si nous avions utilisé la classe {@link java.util.HashSet} au lieu de <code>WeakHashSet</code>,
 * chaque élément ajoutés à l'ensemble <code>pool</code> y serait resté même s'il n'était finalement plus utilisé.
 * Mais avec la classe <code>WeakHashSet</code>, les éléments inutilisés disparaissent d'eux-même. De plus, cette
 * classe offre une méthode suplémentaire: {@link #get}. Un appel à <code>get(object)</code> retournera l'élément
 * de l'ensemble qui répond à la condition <code>object_returned.equals(object)</code> (s'il y en a un), ce qui
 * n'implique pas forcément que <code>object_returned==object</code>.
 *
 * <p>Les méthodes de cette classe ne sont qu'en partie synchronisées pour un environnement multi-threads.
 * C'est pourquoi le code donné en exemple plus-haut utilisait un bloc <code>synchronized</code>. Notez aussi qu'il est
 * fortement conseillé que tout élément ajouté à cet ensemble soit immutable, afin d'éviter qu'un élément précédemment
 * ajouté ne deviennent irrécupérable si sa valeur retournée par {@link java.lang.Object#hashCode} change.</p>
 *
 * @version 1.0
 * @author Mark Reinhold
 * @author Martin Desruisseaux
 *
 * @see java.util.HashSet
 * @see java.lang.ref.WeakReference
 */
public final class WeakHashSet extends AbstractSet
{
	/**
	 * Capacité minimale de la table {@link #table}.
	 */
	private static final int MIN_CAPACITY = 17;

	/**
	 * Facteur servant à déterminer à
	 * quel moment la table doit être
	 * reconstruite.
	 */
	private static final float LOAD_FACTOR = 0.75f;

	/**
	 * Liste des références qui viennent d'être détruites par le ramasse-miettes.
	 * Ces informations seront utilisées par la méthode {@link #processQueue}.
	 */
	private final ReferenceQueue referenceQueue=new ReferenceQueue();

	/**
	 * La liste des entrés de cette table. Cette
	 * table sera agrandie selon les besoins.
	 */
	private WeakElement table[];

	/**
	 * Une estimation du nombre d'éléments non-nul dans la
	 * table {@link #table}. Notez que cette information peut
	 * ne pas être exacte, puisque des références ont pu être
	 * nettoyées par le ramasse-miettes sans avoir encore été
	 * retirées de la table {@link #table}.
	 */
	private int count;

	/**
	 * La table sera reconstruire lorsque {@link #count} devient
	 * supérieur à la valeur de ce champs. La valeur de ce champs
	 * est <code>{@link #table}.length*{@link #loadFactor}</code>.
	 */
	private int threshold;

	/**
	 * Nombre que cet ensemble a été modifiée. Notez que cet
	 * ensemble peut être modifiée même lors d'opérations
	 * qui ne sont pas sensées modifier l'ensemble, puisque
	 * des éléments ont pu être réclamés par le ramasse-miettes.
	 */
	private int modCount;

	/**
	 * Construit un ensemble avec une
	 * capacité initiale par défaut.
	 */
	public WeakHashSet()
	{
		table=new WeakElement[MIN_CAPACITY];
		threshold=Math.round(table.length*LOAD_FACTOR);
	}

	/**
	 * Un élément de l'ensemble {@link WeakHashSet}. La valeur de chaque élément
	 * n'est retenue que par un lien faible, de sorte que le ramasse-miettes les
	 * détruira s'ils ne sont plus utilisés nul part ailleurs.
	 *
	 * @version 1.0
	 * @author Mark Reinhold
	 * @author Martin Desruisseaux
	 */
	private static final class WeakElement extends WeakReference
	{
		/**
		 * L'entré suivante, ou <code>null</code>
		 * s'il n'y en a pas.
		 */
		WeakElement next;

		/**
		 * L'index dans le tableau {@link #table} ou se trouvait
		 * cette référence. Cet index sera utilisé pour retirer
		 * rapidement la référence dans la méthode {@link #processQueue}.
		 * Cet index devra être remis à jour à chaque appel de la méthode
		 * {@link #rehash}.
		 */
		int index;

		/**
		 * Construit une entré pour l'objet spécifié.
		 */
		WeakElement(final Object obj, final WeakElement next, final ReferenceQueue referenceQueue, final int index)
		{
			super(obj, referenceQueue);
			this.next=next;
			this.index=index;
		}
	}

	/**
	 * Méthode à appeller lorsque l'on veut retirer de cet ensemble
	 * les éléments qui ont été détruits par le ramasse-miettes.
	 */
	private void processQueue()
	{
		WeakElement toRemove;
process:while ((toRemove = (WeakElement) referenceQueue.poll()) != null)
		{
			final int i=toRemove.index;
			// L'index 'i' peut ne pas être valide si la référence
			// 'toRemove' est un "cadavre" abandonné par 'rehash'.
			if (i<table.length)
			{
				WeakElement prev=null;
				WeakElement e=table[i];
				while (e!=null)
				{
					if (e==toRemove)
					{
						count--;
						modCount++;
						if (prev!=null) prev.next=e.next;
						else table[i]=e.next;
						continue process;
						// Il ne faut pas continuer la boucle courante,
						// car la variable 'e' n'est plus valide.
					}
					prev=e;
					e=e.next;
				}
			}
			// Si on atteint ce point, c'est que la référence n'a pas été trouvée.
			// Ca peut arriver si l'élément a déjà été supprimé par {@link #rehash}.
		}
		/**
		 * Si le nombre d'éléments dans la table a diminué de
		 * façon significative, on réduira la longueur de la table.
		 */
		if (count <= (threshold >> 1))
		{
			rehash();
		}
	}

	/**
	 * Redistribue les éléments de la table {@link #table}.
	 */
	private void rehash()
	{rehash(Math.max(Math.round(count/(0.75f*LOAD_FACTOR)), count+MIN_CAPACITY));}

	/**
	 * Redistribue les éléments de la table {@link #table}.
	 * @param capacity Nouvelle longueur du tableau.
	 */
	private void rehash(final int capacity)
	{
		modCount++;
		final WeakElement[] oldTable = table;
		table     = new WeakElement[capacity];
		threshold = Math.min(Math.round(capacity*LOAD_FACTOR), capacity-7);
		assert(capacity>=MIN_CAPACITY);
		for (int i=0; i<oldTable.length; i++)
		{
			for (WeakElement old=oldTable[i]; old!=null;)
			{
				final WeakElement e=old;
				old=old.next; // On retient 'next' tout de suite car sa valeur va changer...
				final Object obj_e=e.get();
				if (obj_e!=null)
				{
					final int index=(obj_e.hashCode() & 0x7FFFFFFF) % table.length;
					e.index = index;
					e.next  = table[index];
					table[index]=e;
				}
			}
		}
	}

	/**
	 * Retourne le nombre d'éléments
	 * compris dans cet ensemble.
	 */
	public synchronized int size()
	{
		processQueue();
		return count;
	}

	/**
	 * Copie tous les éléments de
	 * cet ensemble dans une liste.
	 */
	private Collection toArrayList()
	{
		processQueue();
		final ArrayList list=new ArrayList(count);
		for (int i=0; i<table.length; i++)
		{
			for (WeakElement e=table[i], prev=null; e!=null; prev=e, e=e.next)
			{
				final Object e_obj=e.get();
				if (e_obj!=null) list.add(e_obj);
			}
		}
		return list;
	}

	/**
	 * Retourne les éléments de cet ensemble
	 * à l'intérieur d'un tableau, sans ordre
	 * particulier.
	 */
	public synchronized Object[] toArray()
	{return toArrayList().toArray();}

	/**
	 * Retourne les éléments de cet ensemble
	 * à l'intérieur du tableau spécifié,
	 * sans ordre particulier.
	 */
	public synchronized Object[] toArray(final Object[] array)
	{return toArrayList().toArray(array);}

	/**
	 * Retourne un itérateur balayant
	 * tous les éléments de cet ensemble.
	 */
	public synchronized Iterator iterator()
	{
		processQueue();
		return new WeakIterator();
	}

	/**
	 * Retourne <code>true</code> si cet ensemble contient l'élément
	 * spécifié. Par défaut cette méthode est simplement implémentée
	 * par <code>return {@link #get get}(obj)!=null</code>.
	 *
	 * @param obj Elément (ne doit pas être nul).
	 * @return <code>true</code> si l'élément
	 *         est compris dans cet ensemble.
	 *
	 * @throws NullPointerException si <code>obj</code> est nul.
	 */
	public boolean contains(final Object obj) throws NullPointerException
	{return get(obj)!=null;}

	/**
	 * Retourne l'élément de cet ensemble qui est égal à l'élément spécifié, ou <code>null</code>
	 * si cet élément n'y apparait pas. Soit <code>returnedElement</code> l'élément retourné par
	 * cette méthode. Si <code>returnedElement</code> est non-nul, alors cette méthode garantie
	 * que <code>returnedElement.{@link Object#equals equals}(obj)</code>, mais ne garantie pas
	 * que <code>returnedElement==obj</code>. Cette propriété permet de retrouver un unique
	 * exemplaire d'un objet à l'aide de cet ensemble plutôt que de conserver de multiples
	 * copies d'un même objet. Voyez la description de cette classe pour un exemple.
	 *
	 * @param obj L'élément recherché (ne doit pas être nul).
	 * @return L'élément compris dans cet ensemble qui est égal
	 *         à <code>obj</code>, ou <code>null</code> s'il n'y
	 *         en a pas.
	 *
	 * @throws NullPointerException si <code>obj</code> est nul.
	 */
	public synchronized Object get(final Object obj) throws NullPointerException
	{
		processQueue();
		final int index = (obj.hashCode() & 0x7FFFFFFF) % table.length;
		for (WeakElement e=table[index], prev=null; e!=null; prev=e, e=e.next)
		{
			final Object e_obj=e.get();
			if (e_obj==null)
			{
				count--;
				modCount++;
				if (prev!=null) prev.next=e.next;
				else table[index]=e.next;
			}
			else if (obj.equals(e_obj))
			{
				return e_obj;
			}
		}
		return null;
	}

	/**
	 * Ajoute l'élément <code>obj</code> à cet ensemble, s'il n'était pas
	 * déjà présent. Si l'élément <code>obj</code> était déjà présent, cette
	 * méthode retourne <code>false</code> sans rien faire.
	 *
	 * @param obj Élément à ajouter (ne doit pas être nul).
	 * @return <code>true</code> si cet ensemble ne contenait
	 *         pas déjà l'élément <code>obj</code>.
	 *
	 * @throws NullPointerException si <code>obj</code> est nul.
	 */
	public synchronized boolean add(Object obj) throws NullPointerException
	{
		processQueue();
		/*
		 * Vérifie si l'objet <code>obj</code> n'apparait pas déjà dans
		 * cet ensemble. Si oui, retourne <code>true</code> sans rien
		 * faire.
		 */
		final int hash = obj.hashCode() & 0x7FFFFFFF;
		int index = hash % table.length;
		for (WeakElement e=table[index], prev=null; e!=null; prev=e, e=e.next)
		{
			final Object e_obj=e.get();
			if (obj.equals(e_obj))
				return false;
		}
		/*
		 * Vérifie si la table a besoin d'être agrandie. Si oui, on
		 * créera un nouveau tableau dans lequel on copiera les éléments
		 * de l'ancien tableau.
		 */
		if (count>=threshold)
		{
			rehash();
			index = hash % table.length;
		}
		/*
		 * Ajoute l'élément <code>obj</code>
		 * aux éléments de cet ensemble.
		 */
		table[index]=new WeakElement(obj, table[index], referenceQueue, index);
		count++;
		modCount++;
		return true;
	}

	/**
	 * Retourne un exemple identique à <code>obj</code> s'il en existait un
	 * dans cet ensemble, ou sinon ajoute <code>obj</code> à cet ensemble.
	 * Cette méthode est équivalente au code suivant:
	 *
	 * <blockquote><pre>
	 * &nbsp;  if (object!=null)
	 * &nbsp;  {
	 * &nbsp;      final Object current=get(object);
	 * &nbsp;      if (current!=null) return current;
	 * &nbsp;      else add(object);
	 * &nbsp;  }
	 * &nbsp;  return object;
	 * </pre></blockquote>
	 */
	private Object intern0(final Object obj)
	{
		if (obj!=null)
		{
			final int hash = obj.hashCode() & 0x7FFFFFFF;
			int index = hash % table.length;
			for (WeakElement e=table[index], prev=null; e!=null; prev=e, e=e.next)
			{
				final Object e_obj=e.get();
				if (e_obj==null)
				{
					count--;
					modCount++;
					if (prev!=null) prev.next=e.next;
					else table[index]=e.next;
				}
				else if (obj.equals(e_obj)) return e_obj;
			}
			if (count>=threshold)
			{
				rehash();
				index = hash % table.length;
			}
			table[index]=new WeakElement(obj, table[index], referenceQueue, index);
			count++;
			modCount++;
		}
		return obj;
	}

	/**
	 * Supprime de cet ensemble l'élément spécifié.
	 *
	 * @pâram obj Élément à supprimer (ne doit pas être nul).
	 * @return <code>true</code> si cet ensemble contenait
	 *         l'élément <code>obj</code>
	 *
	 * @throws NullPointerException si <code>obj</code> est nul.
	 */
	public synchronized boolean remove(final Object obj) throws NullPointerException
	{
		processQueue();
		final int index = (obj.hashCode() & 0x7FFFFFFF) % table.length;
		for (WeakElement e=table[index], prev=null; e!=null; prev=e, e=e.next)
		{
			final Object e_obj=e.get();
			if (obj.equals(e_obj))
			{
				e.clear();
				count--;
				modCount++;
				if (prev!=null) prev.next=e.next;
				else table[index]=e.next;
				return true;
			}
		}
		return false;
	}

	/**
	 * Retire tous les éléments
	 * de cet ensemble.
	 */
	public synchronized void clear()
	{
		count=0;
		modCount++;
		Arrays.fill(table, null);
	}

	/**
	 * Ajoute l'objet spécifié à l'ensemble <code>this</code> si un exemplaire identique (au sens
	 * de la méthode <code>equals</code>) n'existait pas déjà. Si un exemplaire identique existait
	 * déjà, il sera retourné plutôt que d'ajouter <code>object</code> à l'ensemble <code>this</code>.
	 * Cette méthode est équivalente au code suivant:
	 *
	 * <blockquote><pre>
	 * &nbsp;  if (object!=null)
	 * &nbsp;  {
	 * &nbsp;      final Object current=get(object);
	 * &nbsp;      if (current!=null) return current;
	 * &nbsp;      else add(object);
	 * &nbsp;  }
	 * &nbsp;  return object;
	 * </pre></blockquote>
	 */
	public synchronized Object intern(final Object object)
	{
		processQueue();
		return intern0(object);
	}

	/**
	 * Ajoute les objets spécifiés à l'ensemble <code>this</code> si des exemplaires identiques (au sens
	 * de la méthode <code>equals</code>) n'existaient pas déjà. Si des exemplaires identiques existaient
	 * déjà, ils remplaceront les éléments correspondants dans le tableau <code>objects</code>.
	 * Cette méthode est équivalente au code suivant:
	 *
	 * <blockquote><pre>
	 * &nbsp;  for (int i=0; i<objects.length; i++)
	 * &nbsp;      objects[i] = intern(objects[i]);
	 * </pre></blockquote>
	 */
	public synchronized void intern(final Object[] objects)
	{
		processQueue();
		rehash(table.length+objects.length);
		for (int i=0; i<objects.length; i++)
			objects[i] = intern0(objects[i]);
	}

	/**
	 * Itérateur utilisé pour
	 * balayer les valeurs de cet ensemble.
	 *
	 * @version 1.0
	 * @author Mark Reinhold
	 * @author Martin Desruisseaux
	 */
	private final class WeakIterator implements Iterator
	{
		/**
		 * Index dans la table {@link #table}
		 * de l'entré {@link #entry}.
		 */
		private int index = table.length;

		/**
		 * Entré à retourner lors du prochain appel de la méthode {@link #next}.
		 * Si <code>null</code>, les méthodes {@link #next} et {@link #hasNext}
		 * chercheront la prochaine entré non-nul. Si <code>null</code> et que
		 * {@link #index} est en dehors des limites des index valides du tableau
		 * {@link #table}, alors il ne reste plus d'éléments à extraire de cet
		 * ensemble.
		 */
		private WeakElement entry = null;

		/**
		 * Entré retournée par le dernier appel de la méthode
		 * {@link #next}, ou <code>null</code> si {@link #next}
		 * n'a encore jamais été appellé ou que la méthode
		 * {@link #remove} a été appellée.
		 */
		private WeakElement lastReturned = null;

		/**
		 * Index dans le tableau {@link #table} du dernier
		 * élément retourné par {@link #next}.
		 */
		private int lastReturnedIndex = index;

		/**
		 * Un indicateur vérifiant si l'ensemble
		 * n'a pas été modifiée pendant son balayage.
		 */
		private int expectedModCount = modCount;

		/**
		 * Vérifie s'il reste au moins
		 * un autre élément à retourner.
		 */
		public boolean hasNext()
		{
			synchronized (WeakHashSet.this)
			{
				if (modCount != expectedModCount)
					throw new ConcurrentModificationException();

				while (entry==null)
				{
					if (--index>=0)
						entry=table[index];
					else return false;
				}
				return true;
			}
		}

		/**
		 * Retourne le prochain
		 * élément de l'ensemble.
		 */
		public Object next()
		{
			synchronized (WeakHashSet.this)
			{
				while (modCount==expectedModCount)
				{
					while (entry==null)
					{
						if (--index>=0)
							entry=table[index];
						else throw new NoSuchElementException();
					}
					final WeakElement returned=entry;
					final Object obj=returned.get();
					entry=entry.next;
					if (obj!=null)
					{
						lastReturnedIndex=index;
						lastReturned=returned;
						return obj;
					}
				}
				throw new ConcurrentModificationException();
			}
		}

		/**
		 * Supprime le dernier
		 * élément de la liste.
		 */
		public void remove()
		{
			synchronized (WeakHashSet.this)
			{
				if (modCount == expectedModCount)
				{
					for (WeakElement e=table[lastReturnedIndex], prev=null; e!=null; prev=e, e=e.next)
					{
						if (lastReturned==null)
						{
							if (e == lastReturned)
							{
								count--;
								if (prev == null)
									table[lastReturnedIndex] = e.next;
								else
									prev.next = e.next;
								expectedModCount = ++modCount;
								lastReturned = null;
								return;
							}
						}
						else throw new IllegalStateException();
					}
				}
				throw new ConcurrentModificationException();
			}
		}
	}
}
