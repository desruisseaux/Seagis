/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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

// J2SE dependencies
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.rmi.Remote;

// OpenGIS dependencies
import org.opengis.cv.CV_Coverage;


/**
 * Un param�tre observ� par les {@linkplain Animal animaux}. L'interface <code>Parameter</code>
 * ne contient pas les valeurs des observations, mais donnent plut�t des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * Les observations sont g�n�ralement extraites � partir d'une {@linkplain CV_Coverage couverture}
 * de donn�es pour ce param�tre, couverture elle-m�me issue de l'{@linkplain Environment environnement}
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
     * Retourne le nom de ce param�tre. En g�n�ral, la m�thode {@link #toString}
     * retournera aussi ce m�me nom afin de faciliter l'insertion des param�tres
     * dans une interface graphique <cite>Swing</cite> (par exemple une liste
     * d�roulante).
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    String getName() throws RemoteException;

    /**
     * Retourne le poids de ce param�tre dans le choix de la trajectoire de l'{@linkplain Animal
     * animal} sp�cifi�.
     *
     * @param  L'animal pour lequel on veut le poids de ce param�tre.
     * @return Un poids �gal ou sup�rieur � 0.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    float getWeight(Animal animal) throws RemoteException;
}
