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
import fr.ird.animat.Environment;


/**
 * Un �v�nement signalant qu'un changement est survenu
 * dans l'environnement.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class EnvironmentChangeEvent extends EventObject
{
    /**
     * Construit un nouvel �v�nement.
     *
     * @param source La source.
     */
    public EnvironmentChangeEvent(final Environment source)
    {
        super(source);
    }

    /**
     * Retourne la source.
     */
    public Environment getSource()
    {return (Environment) super.getSource();}
}
