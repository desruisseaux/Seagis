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
package fr.ird.animat.impl;

// J2SE
import java.util.Map;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Geotools dependencies
import org.geotools.cs.Ellipsoid;


/**
 * Représentation d'un animal. Chaque animal doit appartenir à une espèce,
 * décrite par un objet {@link Species}, ainsi qu'à une population décrite
 * par un objet {@link Population}. Le terme "animal" est ici utilisé au
 * sens large. Un développeur pourrait très bien considérer la totalité
 * d'un banc de poissons comme une sorte de méga-animal.
 * <br><br>
 * Toutes les coordonnées spatiales sont exprimées en degrées de longitudes
 * et de latitudes selon l'ellipsoïde {@link Ellipsoid#WGS84}.
 * Les déplacements sont exprimées en milles nautiques, et les directions
 * en degrés géographiques (c'est-à-dire par rapport au nord "vrai").
 * Les intervalles de temps sont exprimées en nombre de jours.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Animal {
    /**
     * La population à laquelle appartient cet animal.
     */
    Population population;

    /**
     * Horloge de l'animal. Chaque animal peut avoir une horloge qui lui est propre.
     * Toutes les horloges avancent au même rythme, mais leur temps 0 (qui correspond
     * à la naissance de l'animal) peuvent être différents.
     */
    protected final Clock clock;

    /**
     * Le chemin suivit par l'animal depuis le début de la simulation. La méthode
     * {@link #move} agira typiquement sur cet objet en utilisant des méthodes telles
     * que {@link Path#rotate}, {@link Path#moveForward} ou {@link Path#moveToward}.
     */
    protected final Path path;

    /**
     * Construit un animal à la position initiale spécifiée.
     *
     * @param position Position initiale de l'animal.
     * @param clock {@linkplain Environment#getClock Horloge de l'environnement} auquel sera soumis
     *        l'animal. L'animal aura {@linkplain #clock sa propre horloge} synchronisée sur celle
     *        de l'environnement, mais avec un temps 0 (qui correspond à la naissance de l'animal)
     *        différent.
     */
    public Animal(final Point2D position, final Clock clock) {
        this.path  = new Path(position);
        this.clock = clock.getNewClock();
    }

    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     */
    public abstract Species getSpecies();

    /**
     * Retourne la population à laquelle appartient cet animal.
     * Seuls les animaux vivants appartiennent à une population.
     *
     * @return La population à laquelle appartient cet animal,
     *         ou <code>null</code> si l'animal est mort.
     */
    public Population getPopulation() {
        return population;
    }

    /**
     * Retourne le chemin suivit par l'animal depuis le début
     * de la simulation jusqu'à maintenant. Les coordonnées
     * sont exprimées en degrés de longitudes et de latitudes.
     */
    public Shape getPath() {
        return path;
    }

    /**
     * Retourne les observations de l'animal à la date spécifiée. Le nombre de {@linkplain Parameter
     * paramètres} observés n'est pas nécessairement égal au nombre de paramètres de l'{@linkplain
     * Environment environnement}, car un animal peut ignorer les paramètres qui ne l'intéresse pas.
     * A l'inverse, un animal peut aussi faire quelques observations "internes" (par exemple la
     * température de ses muscles) qui ne font pas partie des paramètres de son environnement
     * externe. En général, {@linkplain Parameter#HEADING le cap et la position} de l'animal font
     * partis des paramètres observés.
     *
     * @param  time Date pour laquelle on veut les observations,
     *         ou <code>null</code> pour les dernières observations
     *         (c'est-à-dire celle qui ont été faites après le dernier
     *         déplacement).
     * @return Les observations de l'animal, ou <code>null</code> si la
     *         date spécifiée n'est pas pendant la durée de vie de cet animal.
     *         L'ensemble des clés ne comprend que les {@linkplain Parameter
     *         paramètres} qui intéressent l'animal. Si un paramètre intéresse
     *         l'animal mais qu'aucune donnée correspondante n'est disponible
     *         dans son environnement, alors les observations correspondantes
     *         seront <code>null</code>.
     */
    public abstract Map<Parameter,float[]> getObservations(Date time);

    /**
     * Retourne la région jusqu'où s'étend la perception de cet
     * animal. Il peut s'agir par exemple d'un cercle centré sur
     * la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la région perçue,
     *         ou <code>null</code> pour la région actuelle.
     * @param  La région perçue, ou <code>null</code> si la date
     *         spécifiée n'est pas pendant la durée de vie de cet animal.
     */
    public abstract Shape getPerceptionArea(Date time);

    /**
     * Fait avancer l'animal pendant le laps de temps spécifié. La vitesse à laquelle se
     * déplacera l'animal (et donc la distance qu'il parcourera) peuvent dépendre de son
     * état ou des conditions environnementales. Le comportement de l'animal dépendra de
     * l'implémentation. Il peut par exemple {@linkplain Path#rotate changer de cap}  et
     * {@linkplain Path#moveForward se déplacer vers ce cap}.  Il peut aussi {@linkplain
     * Path#moveToward se déplacer vers un certain point}, qu'il peut ne pas atteindre si
     * le laps de temps n'est pas suffisant.
     *
     * @param duration Durée du déplacement, en nombre de jours. Cette valeur est généralement
     *        la même que celle qui a été spécifiée à {@link Population#evoluate}.
     */
    protected abstract void move(float duration);

    /**
     * Tue l'animal. L'animal n'appartiendra plus à aucune population.
     */
    public void kill() {
        synchronized (getTreeLock()) {
            if (population != null) {
                population.kill(this);
                population = null;
            }
        }
    }

    /**
     * Retourne l'objet sur lequel se synchroniser lors des accès à cet animal.
     */
    protected final Object getTreeLock() {
        final Population population = this.population;
        return (population!=null) ? population.getTreeLock() : this;
    }
}
