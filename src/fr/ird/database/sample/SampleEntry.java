/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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

// Animats et base de donn�es
import fr.ird.database.Entry;
import fr.ird.animat.Species;


/**
 * Echantillon dans une r�gion g�ographique. Un �chantillons peut ne pas avoir �t� fait en un
 * point pr�cis, mais plut�t dans une certaine r�gion. La m�thode {@link #getCoordinate} retourne
 * les coordonn�es d'un point que l'on suppose repr�sentatif (par exemple au milieu de la zone de
 * p�che � la senne), tandis que {@link #getShape} retourne une forme qui repr�sente la r�gion de
 * l'observation (par exemple la ligne d'une p�che � la palangre).
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
     * Retourne la campagne d'�chantillonage pendant laquelle a �t� pris cet �chantillon.
     * Peut retourner <code>null</code> si ne s'applique pas.
     */
    public abstract CruiseEntry getCruise();

    /**
     * Retourne une coordonn�e repr�sentative de l'�chantillons, en degr�s de longitude et latitude.
     */
    public abstract Point2D getCoordinate();

    /**
     * Retourne une forme repr�sentant la zone de l'�chantillons. Par exemple dans le cas d'une
     * p�che � la palangre, la forme retourn�e peut �tre un objet {@link java.awt.geom.Line2D} ou
     * {@link java.awt.geom.QuadCurve2D} repr�sentant la trajectoire de la ligne de p�che. Si les
     * informations disponibles ne permettent pas de conna�tre cette zone, alors cette m�thode
     * retourne <code>null</code>.
     */
    public abstract Shape getShape();

    /**
     * Retourne une date repr�sentative de l'�chantillons. Dans le cas des p�ches
     * qui s'�tendent sur une certaine p�riode de temps, �a pourrait �tre par
     * exemple la date du milieu.
     */
    public abstract Date getTime();

    /**
     * Retourne la plage de temps pendant laquelle a �t� pris l'�chantillons.
     * Les �l�ments de la plage retourn�e seront du type {@link Date}.
     */
    public abstract Range getTimeRange();

    /**
     * Verifie si cet �chantillons intercepte le rectangle sp�cifi�.
     * La r�ponse retourn�e par cette m�thode ne doit �tre prise
     * qu'� titre indicatif. Par exemple dans le cas d'une p�che � la palangre,
     * cette m�thode peut supposer que la palangre a �t� mouill�e
     * en ligne droite (alors que la r�alit� a probablement �t� un
     * peu diff�rente). Dans tous les cas, cette m�thode tente de
     * retourner la meilleure r�ponse d'apr�s les donn�es dont elle
     * dispose.
     */
    public abstract boolean intersects(final Rectangle2D rect);

    /**
     * Retourne l'esp�ce la plus abondante dans cet �chantillons. Par exemple il pourrait s'agir
     * de l'esp�ce de poisson la plus p�ch� � la position de cet �chantillon. Si aucune esp�ce
     * n'a �t� �chantillonn�e, alors cette m�thode retourne <code>null</code>.
     */
    public abstract Species getDominantSpecies();

    /**
     * Retourne l'ensemble des esp�ces �chantillonn�es. Il n'est pas obligatoire que
     * {@link #getValue(Species)} retourne une valeur diff�rente de z�ro pour chacune
     * de ces esp�ces. L'ensemble retourn� est immutable et son {@linkplain Set#iterator it�rateur}
     * garanti qu'il retournera toujours les esp�ces dans le m�me ordre pour un objet
     * {@link SampleEntry} donn�.
     */
    public abstract Set<Species> getSpecies();

    /**
     * Retourne la valeur de l'�chantillonage pour une esp�ce donn�e.
     * Il peut s'agir par exemple de la quantit� de poissons p�ch�s.
     */
    public abstract float getValue(final Species species);

    /**
     * Retourne la valeur totale des �chantillons, toutes esp�ces confondues.
     * La quantit� retourn�e par cette m�thode est la somme des quantit�es
     * <code>{@link #getValue getValue}(i)</code> o� <var>i</var> varie de 0
     * inclusivement jusqu'� <code>{@link #getSpecies()}.size()</code> exclusivement.
     */
    public abstract float getValue();

    /**
     * Retourne les unit�s des �chantillons. Ca peut �tre par exemple des kilogrammes
     * ou des tonnes de poissons p�ch�s, ou plus simplement un comptage du nombre
     * d'individus. Dans ce dernier cas, l'unit� retourn�e sera sans dimension.
     */
    public abstract Unit getUnit();
}
