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
package fr.ird.animat.seas;

// Interfaces
import fr.ird.animat.Environment;


/**
 * Objet qui, en plus d'�tre mobile, est attentif aux signaux de son
 * environnement.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class AttentiveObject extends MobileObject
{
    /**
     * Construit un objet mobile qui n'a pas de position initiale.
     * Appellez {@link #setLocation} apr�s la construction de cet
     * objet pour lui affecter une position.
     */
    public AttentiveObject()
    {}

    /**
     */
    public void observe(final Environment environment)
    {
    }
}
