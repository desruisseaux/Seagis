/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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
 * Chercheur d'une position sur une surface � 2 dimensions. Les objets qui
 * impl�mentent cette interface ont pour tache de rep�rer la position d'un
 * pixel sur une image, en utilisant des crit�res propres � l'impl�mentation.
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
     * @param  area La r�gion g�ographique dans laquelle rechercher un pixel.
     *         Les coordonn�es de cette r�gion doivent �tre exprim�es selon
     *         le syst�me de coordonn�es de <code>coverage</code>.
     * @return La position du pixel recherch� selon le syst�me de coordonn�es
     *         de l'image. Cette m�thode retourne une position pour chaque
     *         bande.
     */
    public abstract Values[] locate(final GridCoverage coverage, final Shape area);
}
