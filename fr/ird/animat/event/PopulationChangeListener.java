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

// J2SE dependencies
import java.util.EventListener;


/**
 * D�finit un objet qui �coutera les changements d'une population.
 * Ces changements inclus les animaux qui s'ajoutent ou qui meurent,
 * ainsi que les changements de positions des animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface PopulationChangeListener extends EventListener {
    /**
     * Appel�e quand une population a chang�e.
     *
     * @param  event L'�v�nement d�crivant le changement de population.
     */
    void populationChanged(final PopulationChangeEvent event);
}
