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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.util;

// Collections
import java.util.List;
import java.util.ArrayList;

// References
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Miscellaneous
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;


/**
 * A set of object hold by weak references.
 * This class is used to implements caches.
 * <br><br>
 * Note: depart from this name, this class do not implements
 *       the {@link java.util.Set} interface. It may be done
 *       if a future version if it seem worth.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class WeakHashSet<Element>
{
    /**
     * A weak reference to an element.
     * TODO: Should be an inner class, but the compiler don't likes it.
     *       Make it inner and remove 'owner' when compiler's bugs will
     *       be fixed.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class WeakElement<Element> extends WeakReference
    {
        /**
         * The outer class.
         */
        private final WeakHashSet<Element> owner;

        /**
         * The next entry, or <code>null</code> if there is none.
         */
        WeakElement<Element> next;

        /**
         * Index for this element in {@link #table}. This index
         * must be updated at every {@link #rehash} call.
         */
        int index;

        /**
         * Construct a new weak reference.
         */
        WeakElement(final WeakHashSet<Element> owner, final Element obj, final WeakElement<Element> next, final int index)
        {
            super(obj, referenceQueue);
            this.owner = owner;
            this.next  = next;
            this.index = index;
        }

        /**
         * Returns the referenced object.
         */
        public Element get()
        {return (Element) super.get();} // unchecked cast

        /**
         * Clear the reference.
         */
        public void clear()
        {
            super.clear();
            owner.remove(this);
        }
    }

    /**
     * Lance un thread en arri�re-plan qui supprimera
     * les r�f�rences r�clam�es par le ramasse-miettes.
     */
    static
    {
        final Thread thread = new Thread("WeakHashSet")
        {
            public void run()
            {
                while (true) try
                {
                    referenceQueue.remove().clear();
                }
                catch (InterruptedException exception)
                {
                    // Somebody doesn't want to lets
                    // us sleep... Go back to work.
                }
                catch (Exception exception)
                {
                    ExceptionMonitor.unexpectedException("net.seas.util", "WeakHashSet", "remove", exception);
                }
                catch (AssertionError exception)
                {
                    ExceptionMonitor.unexpectedException("net.seas.util", "WeakHashSet", "remove", exception);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Liste des r�f�rences qui viennent d'�tre d�truites par le ramasse-miettes.
     */
    private static final ReferenceQueue referenceQueue=new ReferenceQueue();

    /**
     * Capacit� minimale de la table {@link #table}.
     */
    private static final int MIN_CAPACITY = 7;

    /**
     * Facteur servant � d�terminer �
     * quel moment la table doit �tre
     * reconstruite.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * La liste des entr�s de cette table. Cette
     * table sera agrandie selon les besoins.
     */
    private WeakElement<Element> table[];

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
     * Construit un ensemble avec une
     * capacit� initiale par d�faut.
     */
    public WeakHashSet()
    {
        table=new WeakElement<Element>[MIN_CAPACITY];
        threshold=Math.round(table.length*LOAD_FACTOR);
    }

    /**
     * M�thode � appeller lorsque l'on veut retirer de cet ensemble
     * les �l�ments qui ont �t� d�truits par le ramasse-miettes.
     */
    private synchronized void remove(final WeakElement<Element> toRemove)
    {
        if (Version.MINOR>=4)
            assert(count==count());
        final int i=toRemove.index;
        // L'index 'i' peut ne pas �tre valide si la r�f�rence
        // 'toRemove' est un "cadavre" abandonn� par 'rehash'.
        if (i<table.length)
        {
            WeakElement<Element> prev=null;
            WeakElement<Element> e=table[i];
            while (e!=null)
            {
                if (e==toRemove)
                {
                    count--;
                    if (prev!=null) prev.next=e.next;
                    else table[i]=e.next;

                    // Si le nombre d'�l�ments dans la table a diminu� de
                    // fa�on significative, on r�duira la longueur de la table.
                    if (count <= threshold/4) rehash(false);

                    // Il ne faut pas continuer la boucle courante,
                    // car la variable 'e' n'est plus valide.
                    if (Version.MINOR>=4) assert(count==count());
                    return;
                }
                prev=e;
                e=e.next;
            }
        }
        // Si on atteint ce point, c'est que la r�f�rence n'a pas �t� trouv�e.
        // Ca peut arriver si l'�l�ment a d�j� �t� supprim� par {@link #rehash}.
    }

    /**
     * Redistribue les �l�ments de la table {@link #table}.
     *
     * @param augmentation <code>true</code> if this method is invoked
     *        for augmenting {@link #table}, or <code>false</code> if
     *        it is invoked for making the table smaller.
     */
    private void rehash(final boolean augmentation)
    {
        final int capacity = Math.max(Math.round(count/(LOAD_FACTOR/2)), count+MIN_CAPACITY);
        if (Version.MINOR>=4) assert(capacity>=MIN_CAPACITY);
        if (augmentation ? capacity<table.length : capacity>table.length)
        {
            return;
        }
        final WeakElement<Element>[] oldTable = table;
        table     = new WeakElement<Element>[capacity];
        threshold = Math.min(Math.round(capacity*LOAD_FACTOR), capacity-7);
        for (int i=0; i<oldTable.length; i++)
        {
            for (WeakElement<Element> old=oldTable[i]; old!=null;)
            {
                final WeakElement<Element> e=old;
                old=old.next; // On retient 'next' tout de suite car sa valeur va changer...
                final Element obj_e = e.get();
                if (obj_e!=null)
                {
                    final int index=(hashCode(obj_e) & 0x7FFFFFFF) % table.length;
                    e.index = index;
                    e.next  = table[index];
                    table[index]=e;
                }
                else count--;
            }
        }
        if (Version.MINOR>=4)
        {
            final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE, Cl�.CAPACITY_CHANGE�2, new Integer(oldTable.length), new Integer(table.length));
            record.setSourceClassName("WeakHashSet");
            record.setSourceMethodName(augmentation ? "intern" : "remove");
            Logger.getLogger("net.seas.util").log(record);
            assert(count==count());
        }
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
    private Element intern0(final Element obj)
    {
        if (obj!=null)
        {
            /*
             * V�rifie si l'objet <code>obj</code> n'apparait pas d�j� dans
             * cet ensemble. Si oui, retourne l'objet sans rien faire.
             */
            final int hash = hashCode(obj) & 0x7FFFFFFF;
            int index = hash % table.length;
            for (WeakElement<Element> e=table[index], prev=null; e!=null; prev=e, e=e.next)
            {
                final Element e_obj=e.get();
                if (e_obj==null)
                {
                    count--;
                    if (prev!=null) prev.next=e.next;
                    else table[index]=e.next;
                }
                else if (equals(obj, e_obj)) return e_obj;
            }
            /*
             * V�rifie si la table a besoin d'�tre agrandie. Si oui, on
             * cr�era un nouveau tableau dans lequel on copiera les �l�ments
             * de l'ancien tableau.
             */
            if (count>=threshold)
            {
                rehash(true);
                index = hash % table.length;
            }
            /*
             * Ajoute l'�l�ment <code>obj</code>
             * aux �l�ments de cet ensemble.
             */
            table[index]=new WeakElement<Element>(this, obj, table[index], index);
            count++;
        }
        if (Version.MINOR>=4) assert(count==count());
        return obj;
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
    public synchronized Element intern(final Element object)
    {return intern0(object);}

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
    public synchronized void intern(final Element[] objects)
    {
        for (int i=0; i<objects.length; i++)
            objects[i] = intern0(objects[i]);
    }

    /**
     * Returns the count of element in this set.
     */
    public synchronized int size()
    {
        if (Version.MINOR>=4)
            assert(count==count());
        return count;
    }

    /**
     * Count the number of elements. This number
     * should be equals to {@link #count}.
     */
    private int count()
    {
        int n=0;
        for (int i=0; i<table.length; i++)
            for (WeakElement<Element> e=table[i]; e!=null; e=e.next)
                n++;
        return n;
    }

    /**
     * Returns a view of this set as an array. Elements will be in an arbitrary
     * order. Note that this array contains strong reference.  Consequently, no
     * object reclamation will occurs as long as a reference to this array is hold.
     */
    public synchronized Element[] toArray()
    {
        if (Version.MINOR>=4) assert(count==count());
        final Element[] elements = new Element[count];
        int index = 0;
        for (int i=0; i<table.length; i++)
        {
            for (WeakElement<Element> el=table[i]; el!=null; el=el.next)
            {
                if ((elements[index]=el.get()) != null)
                    index++;
            }
        }
        return XArray.resize(elements, index);
    }

    /**
     * Returns a hash code value for the specified object.
     * Default implementation returns {@link Object#hashCode}.
     * Override to compute hash code in a different way.
     */
    protected int hashCode(final Element object)
    {return (object!=null) ? object.hashCode() : 0;}

    /**
     * Check two objects for equality. This method should be overriden
     * if {@link #hashCode(Object)} has been overriden.
     */
    protected boolean equals(final Element object1, final Element object2)
    {return object1==object2 || (object1!=null && object1.equals(object2));}
}
