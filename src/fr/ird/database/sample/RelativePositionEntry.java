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
     * Indique si cette position relative devrait être sélectionnée par défaut.
     * Cette information peut être utilisée dans une interface utilisateur afin
     * de pré-selectionner un jeu de positions courrament utilisé.
     */
    public abstract boolean isDefault();
}
