/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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

// J2SE dependencies
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.rmi.Remote;

// OpenGIS dependencies
import org.opengis.cv.CV_Coverage;


/**
 * Un paramètre observé par les {@linkplain Animal animaux}. L'interface <code>Parameter</code>
 * ne contient pas les valeurs des observations, mais donnent plutôt des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * Les observations sont généralement extraites à partir d'une {@linkplain CV_Coverage couverture}
 * de données pour ce paramètre, couverture elle-même issue de l'{@linkplain Environment environnement}
 * des animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see Animal#getObservations
 * @see Animal#getParameters
 * @see Environment#getParameters
 * @see Environment#getCoverage
 */
public interface Parameter extends Remote {
    /**
     * Retourne le nom de ce paramètre. En général, la méthode {@link #toString}
     * retournera aussi ce même nom afin de faciliter l'insertion des paramètres
     * dans une interface graphique <cite>Swing</cite> (par exemple une liste
     * déroulante).
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    String getName() throws RemoteException;

    /**
     * Retourne le poids de ce paramètre dans le choix de la trajectoire de l'{@linkplain Animal
     * animal} spécifié.
     *
     * @param  L'animal pour lequel on veut le poids de ce paramètre.
     * @return Un poids égal ou supérieur à 0.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    float getWeight(Animal animal) throws RemoteException;
}
