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
 * Fonction � �valuer sur une surface � 2 dimensions. Cette fonction est
 * appliqu�e sur une r�gion g�ographique d'une image {@link GridCoverage}.
 * Par exemple la fonction {@link AverageEvaluator} calcule la moyenne des
 * valeurs de pixels trouv�es � l'int�rieur de la r�gion g�ographique
 * sp�cifi�e.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Evaluator
{
    /**
     * Evalue la fonction dans une r�gion g�ographique sp�cifi�e.
     * Cette fonction est �valu�e pour chaque bande de la couverture (ou image).
     *
     * @param  coverage La couverture sur laquelle appliquer la fonction.
     * @param  area La r�gion g�ographique dans laquelle �valuer la fonction.
     *         Les coordonn�es de cette r�gion doivent �tre exprim�es selon
     *         le syst�me de coordonn�es de <code>coverage</code>.
     * @return La valeur de cette fonction pour chaque bande de l'image.
     */
    public abstract ParameterValue[] evaluate(final GridCoverage coverage, final Shape area);
}
