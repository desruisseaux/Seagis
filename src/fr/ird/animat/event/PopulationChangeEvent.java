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
 */
package fr.ird.animat.event;

// Dependencies
import java.util.Set;
import fr.ird.animat.Animal;
import fr.ird.animat.Population;


/**
 * Un événement signalant qu'une {@linkplain Population population} a changé.
 * Ces changements inclus les animaux qui s'ajoutent ou qui meurent.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see PopulationChangeListener
 */
public class PopulationChangeEvent extends ChangeEvent {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 4299155444374973871L;

    /**
     * Drapeau indiquant qu'au moins un animal a été ajouté à la population.
     *
     * @see #getType
     * @see #getAnimalAdded
     */
    public static final int ANIMALS_ADDED = EnvironmentChangeEvent.LAST << 1;

    /**
     * Drapeau indiquant qu'au moins un animal ont été supprimé de la population.
     *
     * @see #getType
     * @see #getAnimalRemoved
     */
    public static final int ANIMALS_REMOVED = ANIMALS_ADDED << 1;

    /**
     * La dernière constante utilisée. Cette information est utilisée
     * pour enchaîner avec les constantes de {@link Animal}.
     */
    static final int LAST = ANIMALS_REMOVED;

    /**
     * Les animaux qui ont été ajoutés. Ce champ devrait être nul
     * si le drapeau {@link #ANIMALS_ADDED} n'est pas défini à 1.
     */
    private final Set<Animal> added;

    /**
     * Les animaux qui ont été supprimés. Ce champ devrait être nul
     * si le drapeau {@link #ANIMALS_REMOVED} n'est pas défini à 1.
     */
    private final Set<Animal> removed;

    /**
     * Construit un nouvel événement.
     *
     * @param source  La source.
     * @param type    Le {@linkplain #getType type de changement} qui est survenu.
     *                Ce type peut être n'importe quelle combinaison de
     *
     *                {@link #ANIMALS_ADDED} et
     *                {@link #ANIMALS_REMOVED}.
     *
     * @param added   Les animaux qui ont été ajoutées à la population,
     *                ou <code>null</code> si cet argument ne s'applique pas.
     * @param removed Les animaux qui ont été supprimées de la population,
     *                ou <code>null</code> si cet argument ne s'applique pas.
     */
    public PopulationChangeEvent(final Population  source,
                                 int               type,
                                 final Set<Animal> added,
                                 final Set<Animal> removed)
    {
        super(source, control(type, added, removed));
        this.added   = added;
        this.removed = removed;
    }

    /**
     * Ajuste la valeur du drapeau en fonction des arguments. Cet ajustement devrait être
     * fait directement dans le constructeur si seulement Sun voulait bien donner suite au
     * RFE #4093999.
     */
    private static final int control(int type,
                                     final Set<Animal> added,
                                     final Set<Animal> removed)
    {
        if (added != null) {
            type |= ANIMALS_ADDED;
        } else {
            type &= ~ANIMALS_ADDED;
        }
        if (removed != null) {
            type |= ANIMALS_REMOVED;
        } else {
            type &= ~ANIMALS_REMOVED;
        }
        return type;
    }

    /**
     * Retourne la source.
     */
    public Population getSource() {
        return (Population) super.getSource();
    }

    /**
     * Retourne les animaux qui ont été ajoutés à la population. Cette méthode retournera
     * un ensemble non-nul si et seulement si le {@link #getType type de changement} comprend
     * le drapeau {@link #ANIMALS_ADDED}.
     */
    public Set<Animal> getAnimalAdded() {
        assert (added != null) == ((type & ANIMALS_ADDED) != 0) : type;
        return added;
    }

    /**
     * Retourne les animaux qui ont été supprimés de la population. Cette méthode retournera
     * un ensemble non-nul si et seulement si le {@link #getType type de changement} comprend
     * le drapeau {@link #ANIMALS_REMOVED}.
     */
    public Set<Animal> getAnimalRemoved() {
        assert (removed != null) == ((type & ANIMALS_REMOVED) != 0) : type;
        return removed;
    }
}
