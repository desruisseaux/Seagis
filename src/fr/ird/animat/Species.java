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
 */
package fr.ird.animat;

// J2SE standard
import java.util.Set;
import java.awt.Color;
import java.util.Locale;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Repr�sentation d'une esp�ce animale. Chaque {@linkplain Animal animal} devra obligatoirement
 * appartenir � une et une seule esp�ce. Le terme "esp�ce" est ici utilis� au sens large: bien
 * que le nom <code>Species</code> sugg�re qu'il se r�f�re � la classification des esp�ces, ce
 * n'est pas obligatoirement le cas. Le programmeur est libre d'utiliser plusieurs objets
 * <code>Species</code> pour repr�senter par exemple des groupes d'individus qui appartiennent
 * � la m�me esp�ce animale, mais qui sont de tailles diff�rentes (par exemple les juv�niles
 * versus les adultes).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface Species extends Remote {
    /**
     * Constante d�signant la langue "Latin".
     * Souvent utilis�e pour nommer les esp�ces.
     *
     * @see Locale#ENGLISH
     * @see Locale#FRENCH
     * @see Locale#SPANISH
     */
    Locale LATIN = new Locale("la", "");

    /**
     * Constante d�signant les codes de la FAO. Il ne s'agit pas d'une langue � proprement
     * parler. Toutefois, cette constante est utile pour d�signant la fa�on de repr�senter
     * le {@linkplain #getName nom d'une esp�ce}.
     */
    Locale FAO = new Locale("fao", "");

    /**
     * Retourne les langues dans lesquelles peuvent �tre exprim�es le nom de cette esp�ce.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Locale[] getLocales() throws RemoteException;

    /**
     * Retourne le nom de cette esp�ce dans la langue sp�cifi�e. Cette langue peut �tre typiquement
     * {@linkplain Locale#ENGLISH l'anglais}, {@linkplain Locale#FRENCH le fran�ais} ou {@linkplain
     * Locale#SPANISH l'espagnol}. La "langue" {@link #FAO} fait partie des valeurs l�gales. Elle
     * signifie que la cha�ne d�sir�e est un code repr�sentant l'esp�ce. Par exemple, le code de
     * la FAO pour l'albacore (<cite>Thunnus albacares</cite>, ou <cite>Yellowfin tuna</cite> en
     * anglais) est "YFT".
     * <br><br>
     * Si la langue sp�cifi�e est <code>null</code>, alors cette m�thode tentera de retourner
     * un nom dans la {@linkplain Locale#getDefault() langue par d�faut du syst�me}. Si
     * aucun nom n'est disponible dans la langue du syst�me, alors cette m�thode tentera
     * de retourner un nom dans une autre langue. Le code de l'esp�ce (tel que retourn�
     * par <code>getName(FAO)</code>) ne sera retourn� qu'en dernier recours.
     *
     * @param  locale Langue d�sir�e pour le nom de l'esp�ce, or <code>null</code> pour
     *         un nom dans une langue par d�faut.
     * @return Le nom de l'esp�ce dans la langue sp�cifi�e, ou <code>null</code> si
     *         aucun nom n'est disponible dans la langue sp�cifi�e.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    String getName(Locale locale) throws RemoteException;

    /**
     * Construit un nouvel icone repr�sentant cette esp�ce.
     *
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Icon getIcon() throws RemoteException;

    /**
     * Ic�ne repr�sentant une esp�ce. Un ic�ne peut servir � positionner
     * sur une carte plusieurs individus d'une m�me esp�ce, et peut aussi
     * appara�tre devant une �tiquette dans les listes d�roulantes.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public interface Icon extends javax.swing.Icon {
        /**
         * Retourne l'esp�ce associ�e � cet ic�ne.
         */
        Species getSpecies();

        /**
         * Retourne la couleur de cet ic�ne.
         */
        Color getColor();

        /**
         * Change la couleur de cet ic�ne.
         */
        void setColor(Color color);
    }

    /**
     * Retourne tous les {@linkplain Parameter param�tres} susceptibles d'int�resser les
     * {@linkplain Animal animaux} de cette esp�ce. Cet ensemble de param�tres doit �tre
     * immutable.  Les {@linkplain Animal#getObservations observations des animaux} � un
     * pas de temps donn� peuvent ne couvrir qu'un sous-ensemble de ces param�tres, mais
     * ne devraient jamais contenir de param�tres ext�rieur � cet ensemble.
     *
     * @return L'ensemble des param�tres suceptibles d'int�resser les animaux de cette esp�ce
     *         durant les pas de temps pass�s, pendant le pas de temps courant ou dans un pas
     *         de temps futur.
     * @throws RemoteException Si cette m�thode devait �tre ex�cut�e sur une machine distante
     *         et que cette ex�cution a �chou�e.
     */
    Set<? extends Parameter> getObservedParameters() throws RemoteException;
}
