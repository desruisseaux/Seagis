/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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

// J2SE et JAI
import java.util.Set;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.units.Unit;

// Animats et base de données
import fr.ird.database.Entry;
import fr.ird.animat.Species;


/**
 * Echantillon dans une région géographique. Un échantillons peut ne pas avoir été fait en un
 * point précis, mais plutôt dans une certaine région. La méthode {@link #getCoordinate} retourne
 * les coordonnées d'un point que l'on suppose représentatif (par exemple au milieu de la zone de
 * pêche à la senne), tandis que {@link #getShape} retourne une forme qui représente la région de
 * l'observation (par exemple la ligne d'une pêche à la palangre).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleEntry extends Entry {
    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne la campagne d'échantillonage pendant laquelle a été pris cet échantillon.
     * Peut retourner <code>null</code> si ne s'applique pas.
     */
    public abstract CruiseEntry getCruise();

    /**
     * Retourne une coordonnée représentative de l'échantillons, en degrés de longitude et latitude.
     */
    public abstract Point2D getCoordinate();

    /**
     * Retourne une forme représentant la zone de l'échantillons. Par exemple dans le cas d'une
     * pêche à la palangre, la forme retournée peut être un objet {@link java.awt.geom.Line2D} ou
     * {@link java.awt.geom.QuadCurve2D} représentant la trajectoire de la ligne de pêche. Si les
     * informations disponibles ne permettent pas de connaître cette zone, alors cette méthode
     * retourne <code>null</code>.
     */
    public abstract Shape getShape();

    /**
     * Retourne une date représentative de l'échantillons. Dans le cas des pêches
     * qui s'étendent sur une certaine période de temps, ça pourrait être par
     * exemple la date du milieu.
     */
    public abstract Date getTime();

    /**
     * Retourne la plage de temps pendant laquelle a été pris l'échantillons.
     * Les éléments de la plage retournée seront du type {@link Date}.
     */
    public abstract Range getTimeRange();

    /**
     * Verifie si cet échantillons intercepte le rectangle spécifié.
     * La réponse retournée par cette méthode ne doit être prise
     * qu'à titre indicatif. Par exemple dans le cas d'une pêche à la palangre,
     * cette méthode peut supposer que la palangre a été mouillée
     * en ligne droite (alors que la réalité a probablement été un
     * peu différente). Dans tous les cas, cette méthode tente de
     * retourner la meilleure réponse d'après les données dont elle
     * dispose.
     */
    public abstract boolean intersects(final Rectangle2D rect);

    /**
     * Retourne l'espèce la plus abondante dans cet échantillons. Par exemple il pourrait s'agir
     * de l'espèce de poisson la plus pêché à la position de cet échantillon. Si aucune espèce
     * n'a été échantillonnée, alors cette méthode retourne <code>null</code>.
     */
    public abstract Species getDominantSpecies();

    /**
     * Retourne l'ensemble des espèces échantillonnées. Il n'est pas obligatoire que
     * {@link #getValue(Species)} retourne une valeur différente de zéro pour chacune
     * de ces espèces. L'ensemble retourné est immutable et son {@linkplain Set#iterator itérateur}
     * garanti qu'il retournera toujours les espèces dans le même ordre pour un objet
     * {@link SampleEntry} donné.
     */
    public abstract Set<Species> getSpecies();

    /**
     * Retourne la valeur de l'échantillonage pour une espèce donnée.
     * Il peut s'agir par exemple de la quantité de poissons pêchés.
     */
    public abstract float getValue(final Species species);

    /**
     * Retourne la valeur totale des échantillons, toutes espèces confondues.
     * La quantité retournée par cette méthode est la somme des quantitées
     * <code>{@link #getValue getValue}(i)</code> où <var>i</var> varie de 0
     * inclusivement jusqu'à <code>{@link #getSpecies()}.size()</code> exclusivement.
     */
    public abstract float getValue();

    /**
     * Retourne les unités des échantillons. Ca peut être par exemple des kilogrammes
     * ou des tonnes de poissons pêchés, ou plus simplement un comptage du nombre
     * d'individus. Dans ce dernier cas, l'unité retournée sera sans dimension.
     */
    public abstract Unit getUnit();
}
