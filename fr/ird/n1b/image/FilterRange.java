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
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.util.Range;

/**
 * Implèmente un filtre qui filtre les valeurs n'appartenant pas à un intervalle de 
 * valeurs. Toutes les valeurs n'appartenant pas à cet intervalle doivent être filtrées.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class FilterRange extends Filter
{              
    /** Identifiant des paramètres du filtre. */
    public static String RANGE = "RANGE";    
    
    /** Intervalle définissant les valeurs à ne pas filtrer. */
    private final Range range;
                
    /**
     * Constructeur.
     *
     * @param parameter     Liste de paramètres d'initialisation du filtre.
     */
    private FilterRange(final ParameterList parameter) 
    {        
        super(parameter);
        range = (Range)parameter.getObjectParameter(RANGE);
    }
    
    /** 
     * Retourne <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     *
     * @param array     Un tableau contenant les paramètres du calcul. Les valeurs 
     *                  attendues sont les coordonnées <i>x</i> et 
     *                  <i>y</i> du pixel et la températire du pixel.
     * @return <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     */
    public boolean isFiltered(final double[] array) 
    {
        if (array == null)
            throw new IllegalArgumentException("Array is null.");
        if (array.length < 3)
            throw new IllegalArgumentException("Array needs three arguments.");        
        if (Double.isNaN(array[2]))
            return false;
        return !(range.contains(new Double(array[2])));
    }
    
    /**
     * Retourne les paramètres d'initialisation du filtre.
     * @return les paramètres d'initialisation du filtre.
     */ 
    public static ParameterList getEmptyParameterList() 
    {
        final ParameterListImpl parameters;
        final String descriptor       = "FILTER RANGE";
        final String[] paramNames     = {RANGE};
        final Class[]  paramClasses   = {Range.class};
        final Object[]  paramDefaults = {null};            
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                       paramNames,
                                                                       paramClasses,
                                                                       paramDefaults,
                                                                       null));
        return parameters;
    }    
    
    /**
     * Retourne une instance FilterRange.
     *
     * @param parameter     Paramètre d'initialisation du filtre.
     * @return une instance FilterRange.
     */
    public static Filter get(final ParameterList parameter) 
    {
        return new FilterRange(parameter);
    }
}