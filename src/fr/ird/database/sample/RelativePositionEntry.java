/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// J2SE
import java.util.Date;
import java.awt.geom.Point2D;

// Seagis
import fr.ird.database.Entry;


/**
 * Position spatio-temporelle relative à un échantillon ({@linkp SampleEntry}}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getRelativePositions
 */
public interface RelativePositionEntry extends Entry {
    /**
     * Retourne un numéro unique identifiant cette procédure.
     */
    public abstract int getID();

    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne la date à laquelle évaluer l'environnement relativement à l'échantillon
     * spécifié. On pourrait par exemple être intéressés à la hauteur de l'eau 15 jours
     * avant une pêche.
     */
    public abstract Date getTime(final SampleEntry sample);

    /**
     * Retourne la coordonnées géographiques à laquelle évaluer l'environnement pour
     * l'échantillon spécifié. Il s'agit souvent (mais pas obligatoirement) de la
     * coordonnées de l'échantillon lui-même.
     */
    public abstract Point2D getCoordinate(final SampleEntry sample);

    /**
     * Applique le déplacement relatif sur les coordonnées spatio-temporelles spécifiées.
     * La coordonnée spatiale et la date sont habituellement obtenues par
     * {@link SampleEntry#getCoordinate} et {@link SampleEntry#getTime} respectivement.
     * Toutefois, si l'échantillon {@link SampleEntry} est disponible, alors il vaut mieux
     * appeler les méthodes {@link #getCoordinate} et {@link #getTime} de cet objet, car elles
     * peuvent faire un travail plus élaboré en fonction de la classe de l'échantillon.
     *
     * @param coordinate La position spatiale de l'échantillon, ou <code>null</code>.
     *                   La nouvelle position écrasera la position courante dans cet objet.
     * @param time       La date de l'échantillon, ou <code>null</code>.
     *                   La nouvelle date écrasera la date courante dans cet objet.
     *
     * @see #getCoordinate
     * @see #getTime
     */
    public abstract void applyOffset(final Point2D coordinate, final Date time);

    /**
     * Applique le même déplacement que <code>applyOffset(...)</code>, mais dans la
     * direction opposée.
     *
     * @param coordinate La position spatiale ou <code>null</code>.
     *                   La nouvelle position écrasera la position courante dans cet objet.
     * @param time       La date ou <code>null</code>.
     *                   La nouvelle date écrasera la date courante dans cet objet.
     */
    public abstract void applyOppositeOffset(final Point2D coordinate, final Date time);

    /**
     * Retourne l'écart de temps typique entre les échantillons et la date à laquelle évaluer le
     * paramètre environnemental. Cet écart de temps n'est qu'à titre indicatif et n'a pas à être
     * précis; la manière la plus précise d'obtenir la date pour un échantillon reste la méthode
     * {@link #getTime}. La méthode <code>getTypicalTimeOffset()</code> ne sert qu'à réduire les
     * temps de calculs en planifiant d'une manière plus optimale l'ordre et la fréquence dans
     * lesquelles les images seront lues.
     *
     * @return Un décalage de temps typique, en nombre de jours.
     */
    public abstract float getTypicalTimeOffset();

    /**
     * Indique si cette position relative devrait être sélectionnée par défaut.
     * Cette information peut être utilisée dans une interface utilisateur afin
     * de pré-selectionner un jeu de positions courrament utilisé.
     */
    public abstract boolean isDefault();
}
