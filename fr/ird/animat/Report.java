/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat;


/**
 * Rapport sur l'�tat actuel de la simulation. Ce rapport comprend le nombre total d'animaux,
 * le nombre de tentatives d'observations en dehors de la couverture spatiale des donn�es, le
 * pourcentage de donn�es manquantes, etc.
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
     * Retourne le pourcentage de tentatives d'observations qui sont tomb�es en dehors de la
     * couverture spatiale des donn�es. Cette statistique ne comprend pas les tentatives
     * d'observations en dehors de la couverture temporelle.
     *
     * @return Un pourcentage compris entre 0 et 1.
     */
    float percentOutsideSpatialBounds();

    /**
     * Retourne le pourcentage de donn�es manquantes. Cette statistique comprend les tentatives
     * d'observations en dehors de la couverture spatio-temporelle ainsi que les donn�es manquantes
     * dues � la pr�sence de nuages, le tout pond�r� par le poids qui �tait accord� � chacune de ces
     * donn�es.
     *
     * @return Un pourcentage compris entre 0 et 1.
     */
    float percentMissingData();
}
