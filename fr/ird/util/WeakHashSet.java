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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.util;


/**
 * A temporary wrapper around {@link org.geotools.util.WeakHashSet} leveraging
 * generic type safety. This temporary wrapper will be removed when generic type
 * will be available in JDK 1.5.
 * <br><br>
 * Note: depart from this name, this class do not implements
 *       the {@link java.util.Set} interface. It may be done
 *       if a future version if it seem worth.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class WeakHashSet<Element>
{
    /**
     * The underlying weak hash set.
     */
    private final org.geotools.util.WeakHashSet set = new org.geotools.util.WeakHashSet();

    /**
     * Construit un ensemble avec une
     * capacité initiale par défaut.
     */
    public WeakHashSet()
    {}

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
    public final Element intern(final Element object)
    {return (Element) set.canonicalize(object);}

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
    public final void intern(final Element[] objects)
    {set.canonicalize(objects);}

    /**
     * Returns the count of element in this set.
     */
    public final int size()
    {return set.size();}

    /**
     * Removes all of the elements from this set.
     */
    public final void clear()
    {set.clear();}

    /**
     * Returns a view of this set as an array. Elements will be in an arbitrary
     * order. Note that this array contains strong reference.  Consequently, no
     * object reclamation will occurs as long as a reference to this array is hold.
     */
    public final Element[] toArray()
    {return (Element[]) set.toArray();}
}
