/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.science.math;

/**
 * Cette fonction retourne des valeurs comprises dans l'intervalle [0..1]. Cette fonction
 * est définie comme suit : 
 * <UL>
 *  <LI>Pour tout <i>x</i> appartenant à l'intervalle <i>]min..max[</i>, 
 *  <i><i>f(x)</i> appartient à l'intervalle <i>]0..1[</i>.</LI>
 *  <LI>Pour tout <i>x<= min, f(x)=0</i>.</LI>
 *  <LI>Pour tout <i>x>=max, f(x)=1.</i>.</LI>
 * </UL><BR><BR>
 * 
 * La fonction utilisée est de la forme : <BR><BR>
 *
 * <CENTER><i> f(x) = 1 / (1 + e(cx - a)) <i></CENTER><BR>
 * <IMG SRC="doc-files/function.png"><BR><BR>
 *
 * @version $Id$
 * @author Remi EVE
 */
public class TransfertFunction 
{   
    /**
     * Pour obtenir une fonction de transfert, nous nous sommes basés sur la 
     * fonction de transfert f(x) = 1 / (1+e(x)). Cette fonction de transfert
     * permet d'obtenir des valeurs comprises dans l'intervalle [0..1]. 
     *
     * Avec cette fonction, f(7.6) = 0.9999999 et f(-7,6) = 0.000001
     *                      f(7.7) = 1         et f(-7.7) = 0
     *                      f(180) = 1         et f(-213) = 0
     */        
     private final double min,  // valeur minimum   => f(min) = 0.000000001 
                          max;  // valeur maximum   => f(max) = 0.999999999 
    
     /** Indique si les bornes sont consideres */
     private boolean isMinIncluded, // if (true), f(min) = 0 else f(min) = 0.000001
                     isMaxIncluded; // if (true), f(max) = 1 else f(max) = 0.9999999
     
    /** Coefficients de notre fonction de transfert. */
    private final double C,
                         A;        
    
    
    /**
     * Constructeur.
     *
     * @param   min             Borne inférieur.
     * @param   isMinIncluded   <i>true</i> si la borne inférieur est comprise et <i>false</i>
     *                          sinon.
     * @param   max             Borne supérieur.
     * @param   isMaxIncluded   <i>true</i> si la borne supérieur est comprise et <i>false</i>
     *                          sinon.
     */
    public TransfertFunction (final double min, final boolean isMinIncluded, 
                              final double max, final boolean isMaxIncluded) 
    {        
        this.min = min;
        this.max = max;
        this.isMinIncluded = isMinIncluded;
        this.isMaxIncluded = isMaxIncluded;        
        final double t1 = 7.6, 
                     t2 = -7.6, 
                     t3 = min, 
                     t4 = max;
        C = (t2 - t1) / (t4 - t3);                
        A = t1 - C*t3;        
    }        

    /**
     * Retourne f(x) appartenant à l'intervalle [0..1].
     *
     * @param   x   Valeur à injecter dans la fonction.
     * @return f(x) appartenant à l'intervalle [0..1].
     */
    public double compute(final double x)     
    {
        if ((x > max) || ((x == max) && (isMaxIncluded)))
            return 1.0;
        else if ((x < min) || ((x == min) && (isMinIncluded)))
            return 0.0;
        return (1.0/(1.0 + Math.exp(C*x+A)));
    }
}