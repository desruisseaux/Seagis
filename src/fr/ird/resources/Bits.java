/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.resources;


/**
 * Utilitaires de manipulation de bits.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Bits {
    /**
     * Interdit la création d'instance de cette classe.
     */
    private Bits() {
    }

    /**
     * Retourne le nombre de bits ayant la valeur 1.
     */
    public static int count(int value) {
        int count = 0;
        while (value != 0) {
            if ((value & 1) != 0) {
                count++;
            }
            value >>>= 1;
        }
        return count;
    }
}
