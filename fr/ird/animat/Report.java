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
package fr.ird.animat;


/**
 * Rapport sur l'état actuel de la simulation. Ce rapport comprend le nombre total d'animaux,
 * le nombre de tentatives d'observations en dehors de la couverture spatiale des données, le
 * pourcentage de données manquantes, etc.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Report {
    /**
     * Retourne le nombre total d'animaux.
     */
    int numAnimals();

    /**
     * Retourne le pourcentage de tentatives d'observations qui sont tombées en dehors de la
     * couverture spatiale des données. Cette statistique ne comprend pas les tentatives
     * d'observations en dehors de la couverture temporelle.
     *
     * @return Un pourcentage compris entre 0 et 1.
     */
    float percentOutsideSpatialBounds();

    /**
     * Retourne le pourcentage de données manquantes. Cette statistique comprend les tentatives
     * d'observations en dehors de la couverture spatio-temporelle ainsi que les données manquantes
     * dues à la présence de nuages, le tout pondéré par le poids qui était accordé à chacune de ces
     * données.
     *
     * @return Un pourcentage compris entre 0 et 1.
     */
    float percentMissingData();
}
