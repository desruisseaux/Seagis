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
import java.util.Date;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;


/**
 * Un événement signalant que l'{@linkplain Environment environnement} a changé.
 * Un environnement peut changer suite à un changement de date, ainsi que suite à
 * l'ajout ou la supression de populations. Toutefois, cela n'inclu pas les ajouts
 * ou suppressions d'animaux au sein d'une population; ces derniers changements étant
 * plutôt signalés par {@link PopulationChangeEvent}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see EnvironmentChangeListener
 */
public class EnvironmentChangeEvent extends ChangeEvent {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
//    private static final long serialVersionUID = -5883647949371506640L;

    /**
     * Drapeau indiquant que la {@linkplain Environment#getClock date de l'environnement}
     * a changée.
     *
     * @see #getType
     */
    public static final int DATE_CHANGED = 1;

    /**
     * Drapeau indiquant qu'au moins une population a été ajoutée à l'environnement.
     *
     * @see #getType
     * @see #getPopulationAdded
     */
    public static final int POPULATIONS_ADDED = DATE_CHANGED << 1;

    /**
     * Drapeau indiquant qu'au moins une population ont été supprimée de l'environnement.
     *
     * @see #getType
     * @see #getPopulationRemoved
     */
    public static final int POPULATIONS_REMOVED = POPULATIONS_ADDED << 1;

    /**
     * La dernière constante utilisée. Cette information est utilisée
     * pour enchaîner avec les constantes de {@link Population}.
     */
    static final int LAST = POPULATIONS_REMOVED;

    /**
     * La nouvelle date de l'environnement, ou {@link Long#MIN_VALUE} si elle n'a pas changé.
     */
    private final long time;

    /**
     * Les populations qui ont été ajoutées. Ce champ devrait être nul
     * si le drapeau {@link #POPULATIONS_ADDED} n'est pas défini à 1.
     */
    private final Set<Population> added;

    /**
     * Les populations qui ont été supprimées. Ce champ devrait être nul
     * si le drapeau {@link #POPULATIONS_REMOVED} n'est pas défini à 1.
     */
    private final Set<Population> removed;

    /**
     * Construit un nouvel événement.
     *
     * @param source  La source.
     * @param type    Le {@linkplain #getType type de changement} qui est survenu.
     *                Ce type peut être n'importe quelle combinaison de
     *
     *                {@link #DATE_CHANGED},
     *                {@link #POPULATIONS_ADDED} et
     *                {@link #POPULATIONS_REMOVED}.
     *
     * @param date    La nouvelle date de l'environnement, ou <code>null</code>
     *                si elle n'a pas changé.
     * @param added   Les populations qui ont été ajoutées à l'environnement,
     *                ou <code>null</code> si cet argument ne s'applique pas.
     * @param removed Les populations qui ont été supprimées de l'environnement,
     *                ou <code>null</code> si cet argument ne s'applique pas.
     */
    public EnvironmentChangeEvent(final Environment     source,
                                  int                   type,
                                  final Date            date,
                                  final Set<Population> added,
                                  final Set<Population> removed)
    {
        super(source, control(type, date, added, removed));
        this.time    = (date!=null) ? date.getTime() : Long.MIN_VALUE;
        this.added   = added;
        this.removed = removed;
    }

    /**
     * Ajuste la valeur du drapeau en fonction des arguments. Cet ajustement devrait être
     * fait directement dans le constructeur si seulement Sun voulait bien donner suite au
     * RFE #4093999.
     */
    private static final int control(int type,
                                     final Date            date,
                                     final Set<Population> added,
                                     final Set<Population> removed)
    {
        if (date != null) {
            type |= DATE_CHANGED;
        } else {
            type &= ~DATE_CHANGED;
        }
        if (added != null) {
            type |= POPULATIONS_ADDED;
        } else {
            type &= ~POPULATIONS_ADDED;
        }
        if (removed != null) {
            type |= POPULATIONS_REMOVED;
        } else {
            type &= ~POPULATIONS_REMOVED;
        }
        return type;
    }

    /**
     * Retourne la source.
     */
    public Environment getSource() {
        return (Environment) super.getSource();
    }

    /**
     * Retourne la nouvelle date de l'environnement, ou <code>null</code> si elle n'a pas changé.
     */
    public Date getEnvironmentDate() {
        return (time != Long.MIN_VALUE) ? new Date(time) : null;
    }

    /**
     * Retourne les populations qui ont été ajoutées à l'environnement. Cette méthode retournera
     * un ensemble non-nul si et seulement si le {@link #getType type de changement} comprend le
     * drapeau {@link #POPULATIONS_ADDED}.
     */
    public Set<Population> getPopulationAdded() {
        assert (added != null) == ((type & POPULATIONS_ADDED) != 0) : type;
        return added;
    }

    /**
     * Retourne les populations qui ont été supprimées de l'environnement. Cette méthode retournera
     * un ensemble non-nul si et seulement si le {@link #getType type de changement} comprend le
     * drapeau {@link #POPULATIONS_REMOVED}.
     */
    public Set<Population> getPopulationRemoved() {
        assert (removed != null) == ((type & POPULATIONS_REMOVED) != 0) : type;
        return removed;
    }
}
