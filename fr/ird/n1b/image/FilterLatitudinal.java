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
 * Impl�mente un filtre qui filtre latitudinalement les valeurs de temp�rature d'une image 
 * S.S.T. (Sea Surface Temperature) pour �liminer les temp�ratures consid�r�es comme 
 * trop froides et qui sont en r�alit�s des nuages. <BR><BR>
 *
 * Ce filtre est par exemple d�fini comme suit pour la r�union : 
 * <UL> 
 *  <LI>De 5�N   a -10�S de latitude, le seuil est constant et vaut 23�C;</LI>
 *  <LI>De -10�S a -40�S de latitude, le seuil varie de 23�C a 14 �C lin�airement;</LI>
 * </UL><BR><BR>
 *
 * Toutes valeurs en dessous de ce seuil sera consid�r�e comme nuage et de ce fait 
 * filtr�e. Il est n�c�ssaire de conna�tre la position latitudinal � laquelle la 
 * temp�rature � �t� calcul�e ainsi que la temp�rature elle-m�me.
 * 
 * @author  Remi EVE
 * @version $Id$
 */
public final class FilterLatitudinal extends Filter
{              
    /** Identifiant des param�tres du filtre. */
    public static String RANGE     = "RANGE",
                         TRANSFORM = "TRANSFORM";   
    
    /** Contient les filtres latitudinaux � appliquer. */
    private final ThresoldRange[] thresoldRange;
    
    /** 
     * Transform�e permettant de passer du syst�me de coordonn�es de l'image vers le 
     * syst�me de coordonn�es WGS_84. 
     */
    private final MathTransform2D transform;
        
    /**
     * Constructeur.
     *
     * @param parameter     Liste de param�tres d'initialisation du filtre.
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
     * Retourne <i>true</i> si la valeur doit �tre filtr�e et <i>false> sinon.
     *
     * @param array     Un tableau contenant les param�tres du calcul. Les valeurs 
     *                  n�c�ssaires pour une temp�rature sont les coordonn�es <i>x</i> et 
     *                  <i>y</i> du pixel et la temp�rature du pixel.
     * @return <i>true</i> si la valeur doit �tre filtr�e et <i>false> sinon.
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
            // Passage au syst�me de coordonn�es g�ographique.
            computeLatitude(pt);
        }
        catch (TransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }        
        
        /* Parcours de l'ensemble des intervalles de latitude/seuil. si le pixel appartient 
           � l'un de ces intervalles, on v�rifie si la temp�rature est au dessous du seuil. 
           Dans ce cas, le pixel est consider� comme un nuage. */
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
     * Retourne les coordonn�es g�ographique du pixel.
     *
     * @param pt    Coordonn�es <i>(x, y)</i> du pixel.
     * @return les coordonn�es g�ographique du pixel.
     */
    private void computeLatitude(final Point2D pt) throws TransformException
    {
        if (transform != null)
            transform.transform(pt, pt);
    }
    
    /**
     * Retourne les param�tres d'initialisation du filtre.
     * @return les param�tres d'initialisation du filtre.
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
     * @param parameter     Param�tre d'initialisation du filtre.
     * @return une instance FilterLatitudinal.
     */
    public static Filter get(final ParameterList parameter) 
    {
        return new FilterLatitudinal(parameter);
    }    
}