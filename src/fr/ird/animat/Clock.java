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
 */
package fr.ird.animat;

// J2SE dependencies
import java.util.Date;
import java.util.TimeZone;
import java.awt.geom.Point2D;

// JAI dependencies
import javax.media.jai.util.Range;


/**
 * Horloge de la simulation. Cette horloge repr�sente un pas de temps dans l'ex�cution de la
 * simulation. Chaque pas de temps est exprim� par une date centrale et une dur�e. Pendant
 * un pas de temps, tous les {@linkplain Environment#getParameters param�tres de l'environnement}
 * sont consid�r�s constants. En plus de la {@linkplain #getTime() date du pas de temps courant},
 * l'horloge tient � jour un {@linkplain #getStepSequenceNumber num�ro s�quentiel de pas de temps}.
 * Ce num�ro commence � 0 et est incr�ment� de 1 chaque fois que la simulation passe au pas de temps
 * suivant. Le pas de temps 0 correspond au pas de temps au moment ou l'horloge a �t� cr��e. Il est
 * possible que plusieurs horloges soient synchronis�es de fa�on � avancer en m�me temps, mais avec
 * diff�rent num�ro s�quentiel de pas de temps. L'horloge ayant un num�ro de pas de temps plus petit
 * correspondra � {@linkplain Animal#getClock l'horloge d'un animal} plus jeune, qui vit � la m�me
 * �poque que les autres mais qui a "v�cu" depuis un plus petit nombre de pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Clock {
    /**
     * Retourne le num�ro s�quentiel du pas de temps correspondant � la date sp�cifi�e.
     * Ce num�ro sera compris de 0 � {@link #getStepSequenceNumber()} inclusivement.
     *
     * @param  time Date pour laquelle on veut le pas de temps.
     * @return Le num�ro s�quentiel du pas de temps � la date sp�cifi�e.
     * @throws IllegalArgumentException si la date sp�cifi�e est ant�rieure � la date initiale
     *         de l'horloge ou ult�rieure � la date de fin du pas de temps courant.
     */
    int getStepSequenceNumber(Date time) throws IllegalArgumentException;

    /**
     * Retourne le num�ro s�quentiel du pas de temps courant. Ce num�ro commence � 0 et est
     * incr�ment� de 1 � chaque fois que l'horloge avance d'un pas de temps.
     */
    int getStepSequenceNumber();

    /**
     * Retourne le temps �coul� depuis la cr�ation de cette horloge.
     * Il s'agira de l'�ge de l'animal qui est soumis � cette horloge.
     *
     * @return L'�ge en nombre de jours.
     */
    float getAge();

    /**
     * Retourne la date pour le num�ro s�quentiel sp�cifi�.
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
     * Retourne le fuseau horaire recommand� pour l'affichage des dates.
     * Ce fuseau horaire d�pend de la r�gion g�ographique de la simulation.
     */
    TimeZone getTimeZone();

    /**
     * Retourne l'angle d'�l�vation du soleil, en degr�s par rapport � l'horizon.
     * L'�l�vation est calcul�e par rapport � la position sp�cifi�e et la date au
     * milieu du pas de temps courant.
     *
     * @param  position Position, en degr�s de longitude et de latitude.
     * @return Angle d'�l�vation du soleil, en degr�s par rapport � l'horizon.
     */
    float getSunElevation(Point2D position);
}
