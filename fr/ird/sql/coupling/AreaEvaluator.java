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
package fr.ird.sql.coupling;

// Miscellaneous
import java.awt.Shape;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.ct.TransformException;


/**
 * Fonction à évaluer sur une surface à 2 dimensions. Cette fonction est
 * appliquée sur une région géographique d'une image {@link GridCoverage}.
 * Par exemple la fonction {@link #MAIN} calcule la moyenne des valeurs de
 * pixels trouvées à l'intérieur de la région géographique spécifiée.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface AreaEvaluator
{
    /**
     * Une fonction calculant la valeur moyenne des
     * pixels se trouvant dans la région géographique.
     */
    public static final AreaEvaluator MAIN = new AreaAverage();

    /**
     * Evalue la fonction pour une zone géographique de la couverture spécifiée.
     * Cette fonction est évaluée pour chaque bande de la couverture (ou image).
     *
     * @param coverage La couverture sur laquelle appliquer la fonction.
     * @param area La région géographique sur laquelle évaluer la fonction.
     *        Les coordonnées de cette région doivent être exprimées selon
     *        le système de coordonnées de <code>coverage</code>.
     */
    public abstract double[] evaluate(final GridCoverage coverage, final Shape area) throws TransformException;
}
