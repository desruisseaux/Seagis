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


/**
 * Un �v�nement signalant qu'une population a chang�e.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class PopulationChangeEvent extends EventObject
{
    /**
     * Constante indiquant qu'au moins un animal a �t�
     * ajout� � la population.
     */
    public static final int ANIMAL_ADDED = +1;

    /**
     * Constante indiquant qu'au moins un animal a �t�
     * retir� de la population.
     */
    public static final int ANIMAL_KILLED = -1;

    /**
     * Constante indiquant qu'au moins un animal a boug�.
     */
    public static final int ANIMAL_MOVED = 0;

    /**
     * Le type de cet �v�nement: {@link #ANIMAL_ADDED},
     * {@link #ANIMAL_KILLED} ou {@link #ANIMAL_MOVED}.
     */
    private final int type;

    /**
     * Construit un nouvel �v�nement.
     *
     * @param source La source.
     * @param type Le type de cet �v�nement: {@link #ANIMAL_ADDED},
     *             {@link #ANIMAL_KILLED} ou {@link #ANIMAL_MOVED}.
     */
    public PopulationChangeEvent(final Population source, final int type)
    {
        super(source);
        this.type=type;
        if (type<ANIMAL_KILLED || type>ANIMAL_ADDED)
        {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    /**
     * Retourne la source.
     */
    public Population getSource()
    {
        return (Population) super.getSource();
    }

    /**
     * Retourne le type de cet �v�nement: {@link #ANIMAL_ADDED},
     * {@link #ANIMAL_KILLED} ou {@link #ANIMAL_MOVED}.
     */
    public int getType()
    {
        return type;
    }
}
