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

// J2SE
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

// JAI
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;

/**
 * Implémente un filtre qui filtre les nuages en fonction de la tempérautre et de la 
 * température de brillance. Dans ce filtre, la formule suivante est appliquée pour 
 * déterminer si la température doit-être filtrée : <BR><BR>
 *
 * <CENTER><i>si (tb4 - tb5) > (SLOPE * sst + INTERCEPT)) alors le pixel est un nuage</i>
 * </CENTER><BR><BR>
 *
 * Avec tb4 la température de brillance du canal 4,<BR>
 *      tb5 la température de brillance du canal 5,<BR>
 *      sst la température,<BR>
 *      SLOPE et INTERCEPT des constantes.<BR>
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class FilterSST extends Filter
{              
    /** Identifiant des paramètres du filtre. */
    public static String AFFINE_TRANSFORM = "AFFINE TRANSFORM";    
    
    /** Transformée affine. */
    private final AffineTransform transform;
            
    /**
     * Constructeur.
     *
     * @param parameter     Liste de paramètres d'initialisation du filtre.
     */
    private FilterSST(final ParameterList parameter) 
    {        
        super(parameter);
        transform = ((AffineTransform)parameter.getObjectParameter(AFFINE_TRANSFORM));
    }
    
    /** 
     * Retourne <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     *
     * @param array     Un tableau contenant les paramètres du calcul. Les valeurs 
     *                  attendues sont les coordonnées <i>x</i> et 
     *                  <i>y</i> du pixel, la température de brillance du canal 4, 
     *                  la température de brillance du canal5 et la température.
     * @return <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     */    
    public boolean isFiltered(final double[] array) 
    {
        if (array == null)
            throw new IllegalArgumentException("Array is null.");        
        if (array.length < 5)
            throw new IllegalArgumentException("Array needs three arguments.");
        
        final double tb4 = array[2],
                     tb5 = array[3],
                     sst = array[4];
        final Point2D pt = new Point2D.Double(sst, 1);
        transform.transform(pt, pt);
        return ((tb4-tb5) > pt.getX());
    }
    
    /**
     * Retourne les paramètres d'initialisation du filtre.
     * @return les paramètres d'initialisation du filtre.
     */ 
    public static ParameterList getEmptyParameterList() 
    {
        final ParameterListImpl parameters;
        final String descriptor       = "FILTER_SST";
        final String[] paramNames     = {AFFINE_TRANSFORM};
        final Class[]  paramClasses   = {AffineTransform.class};
        final Object[]  paramDefaults = {null};            
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                       paramNames,
                                                                       paramClasses,
                                                                       paramDefaults,
                                                                       null));
        return parameters;
    }    
    
    /**
     * Retourne une instance FilterSST.
     *
     * @param parameter     Paramètre d'initialisation du filtre.
     * @return une instance FilterSST.
     */
    public static Filter get(final ParameterList parameter) 
    {
        return new FilterSST(parameter);
    }
}