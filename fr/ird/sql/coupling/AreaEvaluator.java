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
package fr.ird.sql.coupling;

// Miscellaneous
import java.awt.Shape;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.ct.TransformException;


/**
 * Fonction � �valuer sur une surface � 2 dimensions. Cette fonction est
 * appliqu�e sur une r�gion g�ographique d'une image {@link GridCoverage}.
 * Par exemple la fonction {@link #MAIN} calcule la moyenne des valeurs de
 * pixels trouv�es � l'int�rieur de la r�gion g�ographique sp�cifi�e.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface AreaEvaluator
{
    /**
     * Une fonction calculant la valeur moyenne des
     * pixels se trouvant dans la r�gion g�ographique.
     */
    public static final AreaEvaluator MAIN = new AreaAverage();

    /**
     * Evalue la fonction pour une zone g�ographique de la couverture sp�cifi�e.
     * Cette fonction est �valu�e pour chaque bande de la couverture (ou image).
     *
     * @param coverage La couverture sur laquelle appliquer la fonction.
     * @param area La r�gion g�ographique sur laquelle �valuer la fonction.
     *        Les coordonn�es de cette r�gion doivent �tre exprim�es selon
     *        le syst�me de coordonn�es de <code>coverage</code>.
     */
    public abstract double[] evaluate(final GridCoverage coverage, final Shape area) throws TransformException;
}
