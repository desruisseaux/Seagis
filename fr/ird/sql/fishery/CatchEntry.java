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
package fr.ird.sql.fishery;

// Animats et base de données
import fr.ird.sql.Entry;
import fr.ird.animat.Species;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Divers
import java.util.Set;
import javax.units.Unit;
import javax.media.jai.util.Range;


/**
 * Données d'une capture. Une capture peut ne pas avoir été fait en un point précis,
 * mais plutôt dans une certaine région. La méthode {@link #getCoordinate} retourne
 * les coordonnées d'un point que l'on suppose représentatif (par exemple au milieu
 * de la zone de pêche), tandis que {@link #getShape} retourne une forme qui représente
 * la région de la capture (par exemple la ligne d'une pêche à la palangre).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface CatchEntry extends Entry
{
    /**
     * Retourne une coordonnée représentative de la
     * capture, en degrés de longitude et latitude.
     */
    public abstract Point2D getCoordinate();

    /**
     * Retourne une forme représentant la zone de pêche. Par exemple dans le cas d'une
     * palangre, la forme retournée peut être un objet {@link java.awt.geom.Line2D} ou
     * {@link java.awt.geom.QuadCurve2D} représentant la trajectoire de la ligne de
     * pêche. Si les informations disponibles ne permettent pas de connaître cette zone,
     * alors cette méthode retourne <code>null</code>.
     */
    public abstract Shape getShape();

    /**
     * Retourne une date représentative de la pêche. Dans le cas des pêches
     * qui s'étendent sur une certaine période de temps, ça pourrait être par
     * exemple la date du milieu.
     */
    public abstract Date getTime();

    /**
     * Retourne la plage de temps pendant laquelle a été faite la capture.
     * Les éléments de la plage retournée seront du type {@link Date}.
     */
    public abstract Range getTimeRange();

    /**
     * Verifie si cette capture intercepte le rectangle spécifié.
     * La réponse retournée par cette méthode ne doit être prise
     * qu'à titre indicatif. Par exemple dans le cas d'une palangre,
     * cette méthode peut supposer que la palangre a été mouillée
     * en ligne droite (alors que la réalité a probablement été un
     * peu différente). Dans tous les cas, cette méthode tente de
     * retourner la meilleure réponse d'après les données dont elle
     * dispose.
     */
    public abstract boolean intersects(final Rectangle2D rect);

    /**
     * Retourne l'espèce la plus pêchée dans cette capture. Si aucune espèce
     * n'a été capturée, alors cette méthode retourne <code>null</code>.
     */
    public abstract Species getDominantSpecies();

    /**
     * Retourne l'ensemble des espèces pêchées. Il n'est pas obligatoire
     * que {@link #getCatch(Species)} retourne une valeur différente de zéro
     * pour chacune de ces espèces. L'ensemble retourné est immutable et son
     * itérateur {@link Set#iterator}  garanti qu'il retournera toujours les
     * espèces dans le même ordre pour un objet {@link CatchEntry} donné.
     */
    public abstract Set<Species> getSpecies();

    /**
     * Retourne la quantité de poissons pêchés pour une expèce donnée.
     */
    public abstract float getCatch(final Species species);

    /**
     * Retourne la quantité totale de poissons pêchés, toutes espèces confondues.
     * La quantité retournée par cette méthode est la somme des quantitées
     * <code>{@link #getCatch getCatch}(i)</code> où <var>i</var> varie de 0
     * inclusivement jusqu'à <code>{@link #getSpecies()}.size()</code> exclusivement.
     */
    public abstract float getCatch();

    /**
     * Retourne les unités des captures. Ca peut être par exemple des kilogrammes
     * ou des tonnes de poissons pêchés, ou plus simplement un comptage du nombre
     * d'individus. Dans ce dernier cas, l'unité retournée sera sans dimension.
     */
    public abstract Unit getUnit();
}
