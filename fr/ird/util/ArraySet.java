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

// Collection
import java.util.Iterator;
import java.util.AbstractSet;
import java.io.Serializable;


/**
 * An immutable set backed by an array. It is the user responsability to ensure
 * that the set do not contains duplicated elements.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class ArraySet<Element> extends AbstractSet<Element> implements Serializable {
    /**
     * Serial number for compatibility between different versions.
     */
    private static final long serialVersionUID = -8316857135034826970L;

    /**
     * The elements.
     */
    private final Element[] elements;

    /**
     * Construct a set initialized with the specified array.
     */
    public ArraySet(final Element[] elements) {
        this.elements = elements;
    }

    /**
     * Returns the number of elements in this collection.
     */
    public int size() {
        return elements.length;
    }

    /**
     * Returns an iterator over the elements in this collection.
     */
    public Iterator<Element> iterator() {
        return new Iterator<Element>() {
            private int index=0;

            public boolean hasNext() {
                return index<elements.length;
            }

            public Element next() {
                return elements[index++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns <code>true</code> if this collection contains the specified element.
     */
    public boolean contains(final Object e) {
        if (e == null) {
            for (int i=0; i<elements.length; i++) {
                if (elements[i] == null) {
                    return true;
                }
            }
        } else {
            for (int i=0; i<elements.length; i++) {
                if (e.equals(elements[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns an array containing all of the elements in this collection.
     */
    public Element[] toArray() {
        return (Element[]) elements.clone();
    }
}
