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
package fr.ird.n1b.image;

// JAI
import javax.media.jai.ParameterList;

/**
 * Cette classe abstraite d�finie un filtre.<BR><BR>
 *
 * Pour d�finir un nouveau filtre, il est n�c�ssaire d'�tendre cette classe et sur-d�finir 
 * les m�thodes <CODE>getEmptyParameterList()</CODE> et <CODE>getEmptyParameterList()</CODE>,
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
     * @param parameter     Liste de param�tres d'initialisation du filtre.
     */
    protected Filter(final ParameterList parameter) 
    {
    }
    
    /** 
     * Retourne <i>true</i> si la valeur doit �tre filtr�e et <i>false> sinon.
     *
     * @param array     Un tableau contenant les param�tres du calcul.
     * @return <i>true</i> si la valeur doit �tre filtr�e et <i>false> sinon.
     */
    public abstract boolean isFiltered(final double[] array);
    
    /**
     * Retourne les param�tres de configuration du filtre.
     * @return les param�tres de configuration du filtre.
     */ 
    public static ParameterList getEmptyParameterList() 
    {
        return null;
    }    
    
    /**
     * Retourne une instance de Filter.
     *
     * @param parameter     Param�tre d'initialisation du filtre.
     * @return une instance Filter.
     */
    public static Filter get(final ParameterList parameter) 
    {
        return null;
    }        
}