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
package fr.ird.n1b.image;

// JAI
import javax.media.jai.ParameterList;

/**
 * Cette classe abstraite définie un filtre.<BR><BR>
 *
 * Pour définir un nouveau filtre, il est nécéssaire d'étendre cette classe et sur-définir 
 * les méthodes <CODE>getEmptyParameterList()</CODE> et <CODE>getEmptyParameterList()</CODE>,
 * et <CODE>get(final ParameterList parameter)</CODE>.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public abstract class Filter
{   
    
    /**
     * Constructeur.
     *
     * @param parameter     Liste de paramètres d'initialisation du filtre.
     */
    protected Filter(final ParameterList parameter) 
    {
    }
    
    /** 
     * Retourne <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     *
     * @param array     Un tableau contenant les paramètres du calcul.
     * @return <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     */
    public abstract boolean isFiltered(final double[] array);
    
    /**
     * Retourne les paramètres de configuration du filtre.
     * @return les paramètres de configuration du filtre.
     */ 
    public static ParameterList getEmptyParameterList() 
    {
        return null;
    }    
    
    /**
     * Retourne une instance de Filter.
     *
     * @param parameter     Paramètre d'initialisation du filtre.
     * @return une instance Filter.
     */
    public static Filter get(final ParameterList parameter) 
    {
        return null;
    }        
}