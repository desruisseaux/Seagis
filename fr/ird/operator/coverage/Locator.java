/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.operator.coverage;

// Miscellaneous
import java.awt.Shape;
import org.geotools.gc.GridCoverage;


/**
 * Chercheur d'une position sur une surface à 2 dimensions. Les objets qui
 * implémentent cette interface ont pour tache de repérer la position d'un
 * pixel sur une image, en utilisant des critères propres à l'implémentation.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Locator
{
    /**
     * Retourne la position d'un pixel.
     *
     * @param  coverage La couverture sur laquelle appliquer la fonction.
     * @param  area La région géographique dans laquelle rechercher un pixel.
     *         Les coordonnées de cette région doivent être exprimées selon
     *         le système de coordonnées de <code>coverage</code>.
     * @return La position du pixel recherché selon le système de coordonnées
     *         de l'image. Cette méthode retourne une position pour chaque
     *         bande.
     */
    public abstract Values[] locate(final GridCoverage coverage, final Shape area);
}
