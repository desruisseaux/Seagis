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
 *
 *
 * Contact: Michel Petit
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.animat.server.tuna;

// J2SE standard
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.rmi.RemoteException;

// Geotools
import org.geotools.resources.XArray;

// Seagis
import fr.ird.animat.Parameter;
import fr.ird.animat.Observation;
import fr.ird.animat.server.Animal;
import fr.ird.resources.XEllipse2D;


/**
 * Repr�sentation d'un animal "thon". En plus d'�tre mobile,
 * cet animal est attentif aux signaux de son environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Tuna extends Animal {
    /**
     * Distance maximale (en miles nautiques) que peut parcourir cet animal en une journ�e.
     */
    private static final double MAXIMUM_DAILY_DISTANCE = 10;

    /**
     * Construit un thon � la position initiale sp�cifi�e.
     *
     * @param  species L'esp�ce de ce thon.
     * @param  population La population � laquelle appartient ce thon.
     * @param  position Position initiale du thon, en degr�s de longitudes et de latitudes.
     * @throws RemoteException si l'exportation de ce thon a �chou�.
     */
    protected Tuna(final fr.ird.animat.server.Species species,
                   final Population population,
                   final Point2D    position)
            throws RemoteException
    {
        super(species, population, position);
        observe();
    }

    /**
     * Fait avancer l'animal pendant le laps de temps sp�cifi�. La vitesse � laquelle se
     * d�placera l'animal (et donc la distance qu'il parcourera) peuvent d�pendre de son
     * �tat ou des conditions environnementales.
     *
     * @param duration Dur�e du d�placement, en nombre de jours. Cette valeur est g�n�ralement
     *        la m�me que celle qui a �t� sp�cifi�e � {@link Population#evoluate}.
     */
    protected void move(float duration) {
        double   x = 0;
        double   y = 0;
        double sum = 0;
        final Species species = (Species) getSpecies();
        final double maximumDistance = species.dailyDistance * duration;
        final Map<Parameter,Observation> observations = getObservations(null);
        for (final Map.Entry<Parameter,Observation> entry : observations.entrySet()) {
            final fr.ird.animat.server.Parameter parameter = (fr.ird.animat.server.Parameter) entry.getKey();
            final Point2D position = entry.getValue().location();
            if (position != null) {
                final double weight = parameter.getWeight(this);
                x += position.getX()*weight;
                y += position.getY()*weight;
                sum += weight;
            }
        }
        if (sum > 0) {
            x /= sum;
            y /= sum;
            path.moveToward(new Point2D.Double(x,y), maximumDistance);
        } else {
            path.rotate(10*random.nextGaussian());
            path.moveForward(maximumDistance * Math.min(Math.max(0.75 + 0.5*random.nextGaussian(), -1), 1));
        }
    }
}
