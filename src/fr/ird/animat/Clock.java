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
package fr.ird.animat;

// J2SE dependencies
import java.util.Date;
import java.util.TimeZone;
import java.awt.geom.Point2D;

// JAI dependencies
import javax.media.jai.util.Range;


/**
 * Horloge de la simulation. Cette horloge représente un pas de temps dans l'exécution de la
 * simulation. Chaque pas de temps est exprimé par une date centrale et une durée. Pendant
 * un pas de temps, tous les {@linkplain Environment#getParameters paramètres de l'environnement}
 * sont considérés constants. En plus de la {@linkplain #getTime() date du pas de temps courant},
 * l'horloge tient à jour un {@linkplain #getStepSequenceNumber numéro séquentiel de pas de temps}.
 * Ce numéro commence à 0 et est incrémenté de 1 chaque fois que la simulation passe au pas de temps
 * suivant. Le pas de temps 0 correspond au pas de temps au moment ou l'horloge a été créée. Il est
 * possible que plusieurs horloges soient synchronisées de façon à avancer en même temps, mais avec
 * différent numéro séquentiel de pas de temps. L'horloge ayant un numéro de pas de temps plus petit
 * correspondra à {@linkplain Animal#getClock l'horloge d'un animal} plus jeune, qui vit à la même
 * époque que les autres mais qui a "vécu" depuis un plus petit nombre de pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Clock {
    /**
     * Retourne le numéro séquentiel du pas de temps correspondant à la date spécifiée.
     * Ce numéro sera compris de 0 à {@link #getStepSequenceNumber()} inclusivement.
     *
     * @param  time Date pour laquelle on veut le pas de temps.
     * @return Le numéro séquentiel du pas de temps à la date spécifiée.
     * @throws IllegalArgumentException si la date spécifiée est antérieure à la date initiale
     *         de l'horloge ou ultérieure à la date de fin du pas de temps courant.
     */
    int getStepSequenceNumber(Date time) throws IllegalArgumentException;

    /**
     * Retourne le numéro séquentiel du pas de temps courant. Ce numéro commence à 0 et est
     * incrémenté de 1 à chaque fois que l'horloge avance d'un pas de temps.
     */
    int getStepSequenceNumber();

    /**
     * Retourne le temps écoulé depuis la création de cette horloge.
     * Il s'agira de l'âge de l'animal qui est soumis à cette horloge.
     *
     * @return L'âge en nombre de jours.
     */
    float getAge();

    /**
     * Retourne la date pour le numéro séquentiel spécifié.
     */
    Date getTime(int step);

    /**
     * Retourne la date au milieu du pas de temps courant.
     */
    Date getTime();

    /**
     * Retourne la plage de temps du pas de temps courant.
     */
    Range getTimeRange();

    /**
     * Retourne le fuseau horaire recommandé pour l'affichage des dates.
     * Ce fuseau horaire dépend de la région géographique de la simulation.
     */
    TimeZone getTimeZone();

    /**
     * Retourne l'angle d'élévation du soleil, en degrés par rapport à l'horizon.
     * L'élévation est calculée par rapport à la position spécifiée et la date au
     * milieu du pas de temps courant.
     *
     * @param  position Position, en degrés de longitude et de latitude.
     * @return Angle d'élévation du soleil, en degrés par rapport à l'horizon.
     */
    float getSunElevation(Point2D position);
}
