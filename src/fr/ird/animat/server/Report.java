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
package fr.ird.animat.server;

// J2SE
import java.io.Serializable;


/**
 * Rapport sur l'état actuel de la simulation. Ce rapport comprend le nombre total d'animaux,
 * le nombre de tentatives d'observations en dehors de la couverture spatiale des données, le
 * pourcentage de données manquantes, etc.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Report implements fr.ird.animat.Report, Serializable {
    /**
     * Nombre total d'animaux
     */
    int numAnimals;

    /**
     * Nombre de fois qu'un point a été demandé en dehors de la couverture, et nombre total
     * de points.
     */
    int numPointOutside, numPoints;

    /**
     * Nombre de données manquantes (pondérés par leur poids) et somme des poids depuis le début
     * de la simulation. Utilisés pour calculer le pourcentage de données manquantes parmis les
     * données qui servent à choisir la trajectoire.
     */
    double sumMissingData, sumWeight;

    /**
     * Construit un objet initialement vide.
     */
    public Report() {
    }

    /**
     * Ajoute à ce rapport les statistiques du rapport spécifié.
     */
    public void add(final Report report) {
        numPointOutside += report.numPointOutside;
        numPoints       += report.numPoints;
        sumMissingData  += report.sumMissingData;
        sumWeight       += report.sumWeight;
    }

    /**
     * Retourne le nombre total d'animaux.
     */
    public int numAnimals() {
        return numAnimals;
    }

    /**
     * Retourne le pourcentage de tentatives d'observations qui sont tombées en dehors de la
     * couverture spatiale des données. Cette statistique ne comprend pas les tentatives
     * d'observations en dehors de la couverture temporelle.
     *
     * @return Un pourcentage compris entre 0 et 1.
     */
    public float percentOutsideSpatialBounds() {
        return (numPoints!=0) ? (float)((double)numPointOutside/(double)numPoints) : 0f;
    }

    /**
     * Retourne le pourcentage de données manquantes. Cette statistique comprend les tentatives
     * d'observations en dehors de la couverture spatio-temporelle ainsi que les données manquantes
     * dues à la présence de nuages, le tout pondéré par le poids qui était accordé à chacune de ces
     * données.
     *
     * @return Un pourcentage compris entre 0 et 1.
     */
    public float percentMissingData() {
        return (sumWeight!=0) ? (float)(sumMissingData/sumWeight) : 0f;
    }
}
