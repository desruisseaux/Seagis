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

// J2SE standard
import java.util.Set;
import java.util.NoSuchElementException;
import java.rmi.RemoteException;
import java.rmi.Remote;

// OpenGIS
import org.opengis.cv.CV_Coverage;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Repr�sentation de l'environnement dans lequel �volueront les animaux. Cet environnement peut
 * contenir un nombre arbitraire de {@linkplain Population populations}, qui contiendront chacune
 * un nombre arbitraire {@linkplain Animal d'animaux}. L'�volution d'un environnement est soumis
 * au rythme d'une {@linkplain Clock horloge}, qui imposera son rythme � tous les animaux n�s
 * dans cet environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Environment extends Remote {
    /**
     * Retourne l'ensemble des populations �voluant dans cet environnement.
     * Les populations &quot;{@linkplain Population#kill mortes}&quot; ne
     * sont pas comprises dans cet ensemble.
     *
     * @return Les populations �voluant dans cet environnement.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     *
     * @see Population#getEnvironment
     */
    Set<Population> getPopulations() throws RemoteException;

    /**
     * Retourne l'ensemble des param�tres compris dans cet environnement. Les {@link Animal animaux}
     * vont g�n�ralement observer au moins quelque uns de ces param�tres � chaque pas de temps de la
     * simulation. Chaque animal ne va pas n�cessairement observer tous les param�tres, et chaque
     * animal peut aussi observer des param�tres internes (par exemple la temp�rature de ses muscles)
     * qui ne font pas partie des param�tres de l'environnement retourn�s par cette m�thode.
     *
     * @return Les param�tres compris dans cet environnement.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     *
     * @see Animal#getObservations
     */
    Set<Parameter> getParameters() throws RemoteException;

    /**
     * Retourne toute la {@linkplain CV_Coverage couverture spatiale des donn�es} � la
     * {@linkplain Clock#getTime date courante} pour un param�tre sp�cifi�.
     *
     * @param  parameter Le param�tre d�sir�.
     * @return La couverture spatiale des donn�es pour le param�tre sp�cifi�.
     *
     * @throws NoSuchElementException si le param�tre sp�cifi� n'existe pas dans cet environnement.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     *
     * @see Animal#getObservations
     */
    CV_Coverage getCoverage(Parameter parameter) throws NoSuchElementException, RemoteException;

    /**
     * Retourne l'horloge de la simulation. Cet horloge tient � jour la date et heure (virtuelle)
     * courante ainsi que l'�ge de la simulation. Cette horloge contr�le le rythme de l'ensemble
     * de la simulation. Bien que chaque animal peut avoir {@linkplain Animal#getClock sa propre
     * horloge}, ces horloges individuelles sont toutes synchronis�es sur celle de l'environnement
     * et ne diff�rent que par l'�ge de l'animal.
     *
     * @return L'horloge de la simulation.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Clock getClock() throws RemoteException;

    /**
     * D�clare un objet � informer des changements survenant dans cet environnement.
     * Ces changements surviennent � chaque fois que la simulation avance d'un pas de temps.
     *
     * @param  listener �couteur � informer de tout changement dans cet environnement.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void addEnvironmentChangeListener(EnvironmentChangeListener listener) throws RemoteException;

    /**
     * Retire un objet qui ne souhaite plus �tre inform� des changements survenant
     * dans cet environnement.
     *
     * @param  listener �couteur ne d�sirant plus �tre inform� des changements.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    void removeEnvironmentChangeListener(EnvironmentChangeListener listener) throws RemoteException;
}
