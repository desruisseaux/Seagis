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

// Collections
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;

// R�f�rences
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

// Exceptions
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;


/**
 * Ensemble d'objets r�f�renc�s par des liens faibles.  La m�thode {@link #add} permet d'ajouter
 * un nouvel �l�ment � cet ensemble, et la m�thode {@link #remove} d'en retirer. Toutefois, chaque �l�ment ajout�
 * � cet ensemble pourra �tre automatiquement retir� (sans appel explicite � {@link #remove}) si aucune r�f�rence
 * forte vers cet �l�ment n'est tenue ailleurs dans cette machine virtuelle Java. Par exemple, supposons que l'on
 * veut tenir une liste de tous les objets {@link fr.ird.units.Unit} cr��s et en cours d'utilisation. A chaque fois
 * que l'on a besoin d'une unit�, on souhaite retourner un objet <code>Unit</code> pr�c�demment cr�� plut�t que de
 * gaspiller de la m�moire avec des dizaines d'exemplaires identiques de la m�me chose. On pourrait alors impl�menter
 * une m�thode similaire � {@link java.lang.String#intern} de la fa�on suivante:
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
 * Si nous avions utilis� la classe {@link java.util.HashSet} au lieu de <code>WeakHashSet</code>,
 * chaque �l�ment ajout�s � l'ensemble <code>pool</code> y serait rest� m�me s'il n'�tait finalement plus utilis�.
 * Mais avec la classe <code>WeakHashSet</code>, les �l�ments inutilis�s disparaissent d'eux-m�me. De plus, cette
 * classe offre une m�thode supl�mentaire: {@link #get}. Un appel � <code>get(object)</code> retournera l'�l�ment
 * de l'ensemble qui r�pond � la condition <code>object_returned.equals(object)</code> (s'il y en a un), ce qui
 * n'implique pas forc�ment que <code>object_returned==object</code>.
 *
 * <p>Les m�thodes de cette classe ne sont qu'en partie synchronis�es pour un environnement multi-threads.
 * C'est pourquoi le code donn� en exemple plus-haut utilisait un bloc <code>synchronized</code>. Notez aussi qu'il est
 * fortement conseill� que tout �l�ment ajout� � cet ensemble soit immutable, afin d'�viter qu'un �l�ment pr�c�demment
 * ajout� ne deviennent irr�cup�rable si sa valeur retourn�e par {@link java.lang.Object#hashCode} change.</p>
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
	 * Capacit� minimale de la table {@link #table}.
	 */
	private static final int MIN_CAPACITY = 17;

	/**
	 * Facteur servant � d�terminer �
	 * quel moment la table doit �tre
	 * reconstruite.
	 */
	private static final float LOAD_FACTOR = 0.75f;

	/**
	 * Liste des r�f�rences qui viennent d'�tre d�truites par le ramasse-miettes.
	 * Ces informations seront utilis�es par la m�thode {@link #processQueue}.
	 */
	private final ReferenceQueue referenceQueue=new ReferenceQueue();

	/**
	 * La liste des entr�s de cette table. Cette
	 * table sera agrandie selon les besoins.
	 */
	private WeakElement table[];

	/**
	 * Une estimation du nombre d'�l�ments non-nul dans la
	 * table {@link #table}. Notez que cette information peut
	 * ne pas �tre exacte, puisque des r�f�rences ont pu �tre
	 * nettoy�es par le ramasse-miettes sans avoir encore �t�
	 * retir�es de la table {@link #table}.
	 */
	private int count;

	/**
	 * La table sera reconstruire lorsque {@link #count} devient
	 * sup�rieur � la valeur de ce champs. La valeur de ce champs
	 * est <code>{@link #table}.length*{@link #loadFactor}</code>.
	 */
	private int threshold;

	/**
	 * Nombre que cet ensemble a �t� modifi�e. Notez que cet
	 * ensemble peut �tre modifi�e m�me lors d'op�rations
	 * qui ne sont pas sens�es modifier l'ensemble, puisque
	 * des �l�ments ont pu �tre r�clam�s par le ramasse-miettes.
	 */
	private int modCount;

	/**
	 * Construit un ensemble avec une
	 * capacit� initiale par d�faut.
	 */
	public WeakHashSet()
	{
		table=new WeakElement[MIN_CAPACITY];
		threshold=Math.round(table.length*LOAD_FACTOR);
	}

	/**
	 * Un �l�ment de l'ensemble {@link WeakHashSet}. La valeur de chaque �l�ment
	 * n'est retenue que par un lien faible, de sorte que le ramasse-miettes les
	 * d�truira s'ils ne sont plus utilis�s nul part ailleurs.
	 *
	 * @version 1.0
	 * @author Mark Reinhold
	 * @author Martin Desruisseaux
	 */
	private static final class WeakElement extends WeakReference
	{
		/**
		 * L'entr� suivante, ou <code>null</code>
		 * s'il n'y en a pas.
		 */
		WeakElement next;

		/**
		 * L'index dans le tableau {@link #table} ou se trouvait
		 * cette r�f�rence. Cet index sera utilis� pour retirer
		 * rapidement la r�f�rence dans la m�thode {@link #processQueue}.
		 * Cet index devra �tre remis � jour � chaque appel de la m�thode
		 * {@link #rehash}.
		 */
		int index;

		/**
		 * Construit une entr� pour l'objet sp�cifi�.
		 */
		WeakElement(final Object obj, final WeakElement next, final ReferenceQueue referenceQueue, final int index)
		{
			super(obj, referenceQueue);
			this.next=next;
			this.index=index;
		}
	}

	/**
	 * M�thode � appeller lorsque l'on veut retirer de cet ensemble
	 * les �l�ments qui ont �t� d�truits par le ramasse-miettes.
	 */
	private void processQueue()
	{
		WeakElement toRemove;
process:while ((toRemove = (WeakElement) referenceQueue.poll()) != null)
		{
			final int i=toRemove.index;
			// L'index 'i' peut ne pas �tre valide si la r�f�rence
			// 'toRemove' est un "cadavre" abandonn� par 'rehash'.
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
			// Si on atteint ce point, c'est que la r�f�rence n'a pas �t� trouv�e.
			// Ca peut arriver si l'�l�ment a d�j� �t� supprim� par {@link #rehash}.
		}
		/**
		 * Si le nombre d'�l�ments dans la table a diminu� de
		 * fa�on significative, on r�duira la longueur de la table.
		 */
		if (count <= (threshold >> 1))
		{
			rehash();
		}
	}

	/**
	 * Redistribue les �l�ments de la table {@link #table}.
	 */
	private void rehash()
	{rehash(Math.max(Math.round(count/(0.75f*LOAD_FACTOR)), count+MIN_CAPACITY));}

	/**
	 * Redistribue les �l�ments de la table {@link #table}.
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
	 * Retourne le nombre d'�l�ments
	 * compris dans cet ensemble.
	 */
	public synchronized int size()
	{
		processQueue();
		return count;
	}

	/**
	 * Copie tous les �l�ments de
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
	 * Retourne les �l�ments de cet ensemble
	 * � l'int�rieur d'un tableau, sans ordre
	 * particulier.
	 */
	public synchronized Object[] toArray()
	{return toArrayList().toArray();}

	/**
	 * Retourne les �l�ments de cet ensemble
	 * � l'int�rieur du tableau sp�cifi�,
	 * sans ordre particulier.
	 */
	public synchronized Object[] toArray(final Object[] array)
	{return toArrayList().toArray(array);}

	/**
	 * Retourne un it�rateur balayant
	 * tous les �l�ments de cet ensemble.
	 */
	public synchronized Iterator iterator()
	{
		processQueue();
		return new WeakIterator();
	}

	/**
	 * Retourne <code>true</code> si cet ensemble contient l'�l�ment
	 * sp�cifi�. Par d�faut cette m�thode est simplement impl�ment�e
	 * par <code>return {@link #get get}(obj)!=null</code>.
	 *
	 * @param obj El�ment (ne doit pas �tre nul).
	 * @return <code>true</code> si l'�l�ment
	 *         est compris dans cet ensemble.
	 *
	 * @throws NullPointerException si <code>obj</code> est nul.
	 */
	public boolean contains(final Object obj) throws NullPointerException
	{return get(obj)!=null;}

	/**
	 * Retourne l'�l�ment de cet ensemble qui est �gal � l'�l�ment sp�cifi�, ou <code>null</code>
	 * si cet �l�ment n'y apparait pas. Soit <code>returnedElement</code> l'�l�ment retourn� par
	 * cette m�thode. Si <code>returnedElement</code> est non-nul, alors cette m�thode garantie
	 * que <code>returnedElement.{@link Object#equals equals}(obj)</code>, mais ne garantie pas
	 * que <code>returnedElement==obj</code>. Cette propri�t� permet de retrouver un unique
	 * exemplaire d'un objet � l'aide de cet ensemble plut�t que de conserver de multiples
	 * copies d'un m�me objet. Voyez la description de cette classe pour un exemple.
	 *
	 * @param obj L'�l�ment recherch� (ne doit pas �tre nul).
	 * @return L'�l�ment compris dans cet ensemble qui est �gal
	 *         � <code>obj</code>, ou <code>null</code> s'il n'y
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
	 * Ajoute l'�l�ment <code>obj</code> � cet ensemble, s'il n'�tait pas
	 * d�j� pr�sent. Si l'�l�ment <code>obj</code> �tait d�j� pr�sent, cette
	 * m�thode retourne <code>false</code> sans rien faire.
	 *
	 * @param obj �l�ment � ajouter (ne doit pas �tre nul).
	 * @return <code>true</code> si cet ensemble ne contenait
	 *         pas d�j� l'�l�ment <code>obj</code>.
	 *
	 * @throws NullPointerException si <code>obj</code> est nul.
	 */
	public synchronized boolean add(Object obj) throws NullPointerException
	{
		processQueue();
		/*
		 * V�rifie si l'objet <code>obj</code> n'apparait pas d�j� dans
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
		 * V�rifie si la table a besoin d'�tre agrandie. Si oui, on
		 * cr�era un nouveau tableau dans lequel on copiera les �l�ments
		 * de l'ancien tableau.
		 */
		if (count>=threshold)
		{
			rehash();
			index = hash % table.length;
		}
		/*
		 * Ajoute l'�l�ment <code>obj</code>
		 * aux �l�ments de cet ensemble.
		 */
		table[index]=new WeakElement(obj, table[index], referenceQueue, index);
		count++;
		modCount++;
		return true;
	}

	/**
	 * Retourne un exemple identique � <code>obj</code> s'il en existait un
	 * dans cet ensemble, ou sinon ajoute <code>obj</code> � cet ensemble.
	 * Cette m�thode est �quivalente au code suivant:
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
	 * Supprime de cet ensemble l'�l�ment sp�cifi�.
	 *
	 * @p�ram obj �l�ment � supprimer (ne doit pas �tre nul).
	 * @return <code>true</code> si cet ensemble contenait
	 *         l'�l�ment <code>obj</code>
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
	 * Retire tous les �l�ments
	 * de cet ensemble.
	 */
	public synchronized void clear()
	{
		count=0;
		modCount++;
		Arrays.fill(table, null);
	}

	/**
	 * Ajoute l'objet sp�cifi� � l'ensemble <code>this</code> si un exemplaire identique (au sens
	 * de la m�thode <code>equals</code>) n'existait pas d�j�. Si un exemplaire identique existait
	 * d�j�, il sera retourn� plut�t que d'ajouter <code>object</code> � l'ensemble <code>this</code>.
	 * Cette m�thode est �quivalente au code suivant:
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
	 * Ajoute les objets sp�cifi�s � l'ensemble <code>this</code> si des exemplaires identiques (au sens
	 * de la m�thode <code>equals</code>) n'existaient pas d�j�. Si des exemplaires identiques existaient
	 * d�j�, ils remplaceront les �l�ments correspondants dans le tableau <code>objects</code>.
	 * Cette m�thode est �quivalente au code suivant:
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
	 * It�rateur utilis� pour
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
		 * de l'entr� {@link #entry}.
		 */
		private int index = table.length;

		/**
		 * Entr� � retourner lors du prochain appel de la m�thode {@link #next}.
		 * Si <code>null</code>, les m�thodes {@link #next} et {@link #hasNext}
		 * chercheront la prochaine entr� non-nul. Si <code>null</code> et que
		 * {@link #index} est en dehors des limites des index valides du tableau
		 * {@link #table}, alors il ne reste plus d'�l�ments � extraire de cet
		 * ensemble.
		 */
		private WeakElement entry = null;

		/**
		 * Entr� retourn�e par le dernier appel de la m�thode
		 * {@link #next}, ou <code>null</code> si {@link #next}
		 * n'a encore jamais �t� appell� ou que la m�thode
		 * {@link #remove} a �t� appell�e.
		 */
		private WeakElement lastReturned = null;

		/**
		 * Index dans le tableau {@link #table} du dernier
		 * �l�ment retourn� par {@link #next}.
		 */
		private int lastReturnedIndex = index;

		/**
		 * Un indicateur v�rifiant si l'ensemble
		 * n'a pas �t� modifi�e pendant son balayage.
		 */
		private int expectedModCount = modCount;

		/**
		 * V�rifie s'il reste au moins
		 * un autre �l�ment � retourner.
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
		 * �l�ment de l'ensemble.
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
		 * �l�ment de la liste.
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
