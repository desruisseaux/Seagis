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
import java.awt.geom.Point2D;

// JAI
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.util.Range;

// SEAGIS
import fr.ird.n1b.util.ThresoldRange;

// GEOTOOLS
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;


/**
 * Implémente un filtre qui filtre latitudinalement les valeurs de température d'une image 
 * S.S.T. (Sea Surface Temperature) pour éliminer les températures considérées comme 
 * trop froides et qui sont en réalités des nuages. <BR><BR>
 *
 * Ce filtre est par exemple défini comme suit pour la réunion : 
 * <UL> 
 *  <LI>De 5°N   a -10°S de latitude, le seuil est constant et vaut 23°C;</LI>
 *  <LI>De -10°S a -40°S de latitude, le seuil varie de 23°C a 14 °C linéairement;</LI>
 * </UL><BR><BR>
 *
 * Toutes valeurs en dessous de ce seuil sera considérée comme nuage et de ce fait 
 * filtrée. Il est nécéssaire de connaître la position latitudinal à laquelle la 
 * température à été calculée ainsi que la température elle-même.
 * 
 * @author  Remi EVE
 * @version $Id$
 */
public final class FilterLatitudinal extends Filter
{              
    /** Identifiant des paramètres du filtre. */
    public static String RANGE     = "RANGE",
                         TRANSFORM = "TRANSFORM";   
    
    /** Contient les filtres latitudinaux à appliquer. */
    private final ThresoldRange[] thresoldRange;
    
    /** 
     * Transformée permettant de passer du système de coordonnées de l'image vers le 
     * système de coordonnées WGS_84. 
     */
    private final MathTransform2D transform;
        
    /**
     * Constructeur.
     *
     * @param parameter     Liste de paramètres d'initialisation du filtre.
     */
    private FilterLatitudinal(final ParameterList parameter) 
    {
        super(parameter);
        thresoldRange = (ThresoldRange[])parameter.getObjectParameter(RANGE);        
        transform     = (MathTransform2D)parameter.getObjectParameter(TRANSFORM);        
        
        if (thresoldRange == null)
            throw new IllegalArgumentException("There is no filter define.");                        
    }
    
    /** 
     * Retourne <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     *
     * @param array     Un tableau contenant les paramètres du calcul. Les valeurs 
     *                  nécéssaires pour une température sont les coordonnées <i>x</i> et 
     *                  <i>y</i> du pixel et la température du pixel.
     * @return <i>true</i> si la valeur doit être filtrée et <i>false> sinon.
     */
    public boolean isFiltered(final double[] array) 
    {
        if (array == null)            
            throw new IllegalArgumentException("Array is null.");
        if (array.length!=3)
            throw new IllegalArgumentException("Array needs three arguments.");                
        
        final double temperature = array[2];
        final Point2D pt = new Point2D.Double(array[0], array[1]);
        
        try 
        {
            // Passage au système de coordonnées géographique.
            computeLatitude(pt);
        }
        catch (TransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }        
        
        /* Parcours de l'ensemble des intervalles de latitude/seuil. si le pixel appartient 
           à l'un de ces intervalles, on vérifie si la température est au dessous du seuil. 
           Dans ce cas, le pixel est consideré comme un nuage. */
        for (int i=0 ; i<thresoldRange.length ; i++)
        {        
            final Range range = thresoldRange[i].getRange();
            if (range.contains(new Double(pt.getY())))
            {
                /* Calcul du seuil limite en fonction de la latitude du pixel. */
                final double min   = ((Double)range.getMinValue()).doubleValue(),
                             max   = ((Double)range.getMaxValue()).doubleValue();
                final double seuil = (thresoldRange[i].getThresold2()  - 
                                      thresoldRange[i].getThresold1()) *
                                      (pt.getY() - min) / (max - min)  + 
                                      thresoldRange[i].getThresold1();
                if (temperature < seuil)
                    return true;
                return false;
            }
        }
        return false;
    }
    
    /**
     * Retourne les coordonnées géographique du pixel.
     *
     * @param pt    Coordonnées <i>(x, y)</i> du pixel.
     * @return les coordonnées géographique du pixel.
     */
    private void computeLatitude(final Point2D pt) throws TransformException
    {
        if (transform != null)
            transform.transform(pt, pt);
    }
    
    /**
     * Retourne les paramètres d'initialisation du filtre.
     * @return les paramètres d'initialisation du filtre.
     */ 
    public static ParameterList getEmptyParameterList() 
    {
        final ParameterListImpl parameters;
        final String descriptor       = "FILTER LATITUDINAL";
        final String[] paramNames     = {RANGE,
                                         TRANSFORM};  
        final Class[]  paramClasses   = {ThresoldRange[].class,
                                         MathTransform2D.class};
        final Object[]  paramDefaults = {null,
                                         null};            
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                       paramNames,
                                                                       paramClasses,
                                                                       paramDefaults,
                                                                       null));
        return parameters;
    }        
    
    /**
     * Retourne une instance FilterLatitudinal.
     *
     * @param parameter     Paramètre d'initialisation du filtre.
     * @return une instance FilterLatitudinal.
     */
    public static Filter get(final ParameterList parameter) 
    {
        return new FilterLatitudinal(parameter);
    }    
}