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
 */
package fr.ird.animat;

// J2SE standard
import java.util.Set;
import java.awt.Color;
import java.util.Locale;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Représentation d'une espèce animale. Chaque {@linkplain Animal animal} devra obligatoirement
 * appartenir à une et une seule espèce. Le terme "espèce" est ici utilisé au sens large: bien
 * que le nom <code>Species</code> suggère qu'il se réfère à la classification des espèces, ce
 * n'est pas obligatoirement le cas. Le programmeur est libre d'utiliser plusieurs objets
 * <code>Species</code> pour représenter par exemple des groupes d'individus qui appartiennent
 * à la même espèce animale, mais qui sont de tailles différentes (par exemple les juvéniles
 * versus les adultes).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Species extends Remote {
    /**
     * Constante désignant la langue "Latin".
     * Souvent utilisée pour nommer les espèces.
     *
     * @see Locale#ENGLISH
     * @see Locale#FRENCH
     * @see Locale#SPANISH
     */
    Locale LATIN = new Locale("la", "");

    /**
     * Constante désignant les codes de la FAO. Il ne s'agit pas d'une langue à proprement
     * parler. Toutefois, cette constante est utile pour désignant la façon de représenter
     * le {@linkplain #getName nom d'une espèce}.
     */
    Locale FAO = new Locale("fao", "");

    /**
     * Retourne les langues dans lesquelles peuvent être exprimées le nom de cette espèce.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Locale[] getLocales() throws RemoteException;

    /**
     * Retourne le nom de cette espèce dans la langue spécifiée. Cette langue peut être typiquement
     * {@linkplain Locale#ENGLISH l'anglais}, {@linkplain Locale#FRENCH le français} ou {@linkplain
     * Locale#SPANISH l'espagnol}. La "langue" {@link #FAO} fait partie des valeurs légales. Elle
     * signifie que la chaîne désirée est un code représentant l'espèce. Par exemple, le code de
     * la FAO pour l'albacore (<cite>Thunnus albacares</cite>, ou <cite>Yellowfin tuna</cite> en
     * anglais) est "YFT".
     * <br><br>
     * Si la langue spécifiée est <code>null</code>, alors cette méthode tentera de retourner
     * un nom dans la {@linkplain Locale#getDefault() langue par défaut du système}. Si
     * aucun nom n'est disponible dans la langue du système, alors cette méthode tentera
     * de retourner un nom dans une autre langue. Le code de l'espèce (tel que retourné
     * par <code>getName(FAO)</code>) ne sera retourné qu'en dernier recours.
     *
     * @param  locale Langue désirée pour le nom de l'espèce, or <code>null</code> pour
     *         un nom dans une langue par défaut.
     * @return Le nom de l'espèce dans la langue spécifiée, ou <code>null</code> si
     *         aucun nom n'est disponible dans la langue spécifiée.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    String getName(Locale locale) throws RemoteException;

    /**
     * Construit un nouvel icone représentant cette espèce.
     *
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Icon getIcon() throws RemoteException;

    /**
     * Icône représentant une espèce. Un icône peut servir à positionner
     * sur une carte plusieurs individus d'une même espèce, et peut aussi
     * apparaître devant une étiquette dans les listes déroulantes.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public interface Icon extends javax.swing.Icon {
        /**
         * Retourne l'espèce associée à cet icône.
         */
        Species getSpecies();

        /**
         * Retourne la couleur de cet icône.
         */
        Color getColor();

        /**
         * Change la couleur de cet icône.
         */
        void setColor(Color color);
    }

    /**
     * Retourne tous les {@linkplain Parameter paramètres} susceptibles d'intéresser les
     * {@linkplain Animal animaux} de cette espèce. Cet ensemble de paramètres doit être
     * immutable.  Les {@linkplain Animal#getObservations observations des animaux} à un
     * pas de temps donné peuvent ne couvrir qu'un sous-ensemble de ces paramètres, mais
     * ne devraient jamais contenir de paramètres extérieur à cet ensemble.
     *
     * @return L'ensemble des paramètres suceptibles d'intéresser les animaux de cette espèce
     *         durant les pas de temps passés, pendant le pas de temps courant ou dans un pas
     *         de temps futur.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     */
    Set<? extends Parameter> getObservedParameters() throws RemoteException;
}
