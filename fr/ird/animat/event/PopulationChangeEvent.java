/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.animat.event;

// Dependencies
import java.util.EventObject;
import fr.ird.animat.Population;
import fr.ird.animat.Animal;


/**
 * Un �v�nement signalant qu'une {@linkplain Population population} a chang�.
 * Ces changements inclus les animaux qui s'ajoutent ou qui meurent.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see PopulationChangeListener
 */
public class PopulationChangeEvent extends EventObject {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 555996444421587694L;

    /**
     * Construit un nouvel �v�nement.
     *
     * @param source La source.
     */
    public PopulationChangeEvent(final Population source) {
        super(source);
    }

    /**
     * Retourne la source.
     */
    public Population getSource() {
        return (Population) super.getSource();
    }
}
