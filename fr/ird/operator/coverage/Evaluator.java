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
 * Fonction à évaluer sur une surface à 2 dimensions. Cette fonction est
 * appliquée sur une région géographique d'une image {@link GridCoverage}.
 * Par exemple la fonction {@link AverageEvaluator} calcule la moyenne des
 * valeurs de pixels trouvées à l'intérieur de la région géographique
 * spécifiée.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Evaluator
{
    /**
     * Evalue la fonction dans une région géographique spécifiée.
     * Cette fonction est évaluée pour chaque bande de la couverture (ou image).
     *
     * @param  coverage La couverture sur laquelle appliquer la fonction.
     * @param  area La région géographique dans laquelle évaluer la fonction.
     *         Les coordonnées de cette région doivent être exprimées selon
     *         le système de coordonnées de <code>coverage</code>.
     * @return La valeur de cette fonction pour chaque bande de l'image.
     */
    public abstract ParameterValue[] evaluate(final GridCoverage coverage, final Shape area);
}
