/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.io.text;

// J2SE / JAI.
import java.io.File;
import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;

// GEOTOOLS.
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.ct.MathTransform1D;

/**
 * Analyse un fichier contenant la description d'une palette (category/SampleDimension). 
 * L'analyse permet de construire un objet de type <CODE>SampleDimension</CODE> contenant 
 * la palette d�finie dans le fichier.
 *
 * @author  R�mi EVE
 * @version $Id$
 */
public class ParsePalette extends Parse
{      
    /** Identifiant � analyser. */
    private static final String  CATEGORY = "CATEGORY",
                                 MINIMUM  = "MINIMUM",
                                 MAXIMUM  = "MAXIMUM",
                                 C0       = "C0",
                                 C1       = "C1",
                                 COLOR    = "COLOR";
                                                                
    /** Identifiant de la liste de param�tre. */
    public static final String  SAMPLE_DIMENSION = "SAMPLE DIMENSION";
     
    /**
     * Retourne un objet de type <CODE>ParameterList</CODE> contenant les param�tres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     *
     * @param parameter   Les param�tres de configuration du parser.
     * @return un objet de type <CODE>ParameterList</CODE> contenant les param�tres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     */
    public static ParameterList parse(final ParameterList parameter) throws IOException
    {
        final File file                   = extractFileToParse(parameter);
        final ParameterList parameterList = getOutputDefaultParameterList(parameter); 
        final BufferedReader input        = new BufferedReader(new FileReader(file));        

        // Liste des cat�gories extraites.
        final Vector vector = new Vector(); 
        
        // Ensemble des param�tres permettant de d�finir une cat�gorie.
        String category = null;
        int minimum     = Integer.MIN_VALUE,
            maximum     = Integer.MIN_VALUE;        
        double c0       = Double.NaN,
               c1       = Double.NaN;                  
        Color[] color   = null;
        
        // Parcours ligne � ligne.
        String line;
        while ((line=input.readLine())!=null)
        {
            // Ligne vide ou commentaire.
            line = line.trim();
            if (line.length() == 0 ||line.startsWith("#"))
                continue;       

            StringTokenizer token = new StringTokenizer(line);            
            String key            = token.nextToken().trim();                                    
            if (key.equals(CATEGORY)) 
            {
                // Nouvelle cat�gorie. Remise � z�ro des variables de la cat�gorie.                
                category = null;
                minimum  = Integer.MIN_VALUE;
                maximum  = Integer.MIN_VALUE;
                c0       = Double.NaN;
                c1       = Double.NaN;                  
                color    = null;    
                
                token    = new StringTokenizer(line);            
                token.nextToken();
                category = parseString(token).trim();        
            } else if (key.equals(MINIMUM)) 
            {
                minimum = parseInteger(token, 1)[0].intValue();                
            } else if (key.equals(MAXIMUM)) 
            {
                maximum = parseInteger(token, 1)[0].intValue();                
            } else if (key.equals(C0))
            {
                c0 = parseDouble(token, 1)[0].doubleValue();                
            } else if (key.equals(C1))
            {
                c1 = parseDouble(token, 1)[0].doubleValue();                
            } else if (key.equals(COLOR))
            {
                final StringBuffer buffer = new StringBuffer();
                while (token.hasMoreTokens())
                    buffer.append(token.nextToken() + " ");                
                String type = buffer.toString().trim();
                
                // La couleur est unique, elle est alors d�finie comme suit   : #A0D2C3.
                // La couleur d�signe un fichier, elle est d�finie comme suit : file:palette.pal
                if (type.startsWith("file:"))
                {
                    final String palette = type.substring(type.indexOf(":")+1);
                    color = (fr.ird.util.Utilities.getPaletteFactory()).getColors(palette);
                }
                else
                {
                    color    = new Color[1];
                    color[0] = extractColor(type);
                }
                
                // Tous les param�tres doivent avoir �t� renseign�s.
                // Cr�ation de la cat�gorie.
                if (category != null &&
                    minimum  != Integer.MIN_VALUE &&
                    maximum  != Integer.MIN_VALUE && 
                    color    != null)
                {
                    // La cat�gorie � des correspondances physiques <-> samples.
                    if (!Double.isNaN(c0) && !Double.isNaN(c1))
                    {
                        vector.add(new Category(category, 
                                                color, 
                                                minimum, 
                                                maximum+1,
                                                c1, 
                                                c0).geophysics(true));                        
                    }
                    else
                    {
                        if (minimum == maximum)
                        {
                            // La cat�gorie n'est pas physique.
                            vector.add(new Category(category, 
                                                    color[0], 
                                                    minimum).geophysics(true));                                                
                        }
                        else
                        {
                            // Cat�gorie n'est pas physique.
                            final Range range = new Range(Integer.class, 
                                                          new Integer(minimum), 
                                                          new Integer(maximum));
                            vector.add(new Category(category, 
                                                    color, 
                                                    range,
                                                    (MathTransform1D)null).geophysics(true));                                                          
                        }
                    }
                }
            }
        } 

        // Le fichier est compl�tement analys�.
        // Les cat�gories sont rassembl�es pour former un SampleDimension.
        final int count = vector.size();
        if (count != 0)
        {
            final Category[] array  = new Category[count];
            final Iterator iterator = vector.iterator();
            int i=0;            
            while (iterator.hasNext())
                array[i++] = (Category)iterator.next();
            final SampleDimension[] sample = {new SampleDimension(array, null)};
            parameterList.setParameter(SAMPLE_DIMENSION, sample);                    
        }
        return parameterList;
    }
        
    /**
     * Retourne les param�tres � extraire du fichier par d�faut. 
     *
     * @param parameter  Param�tres de configuration du parser. 
     * @return les param�tres � extraire du fichier par d�faut. 
     */
    private static ParameterList getOutputDefaultParameterList(final ParameterList parameter) 
    {        
        final ParameterList parameters;
        final String descriptor       = "PALETTE";
        final String[] paramNames     = {SAMPLE_DIMENSION};                                             
        final Class[]  paramClasses   = {SampleDimension[].class};
        final Object[]  paramDefaults = {null};
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        return parameters;
    }    
    
    /**
     * Retourne un tableau de valeurs d�cimales contenant les 3 couleurs RGB.
     *
     * @param color     Cha�ne contenant une couleur RVB au format <i>#C3D4A8</i>.
     * @return un tableau de valeurs d�cimales contenant les 3 couleurs RGB.
     */
    private static Color extractColor(String color)
    {
        final int array[] = new int[3];
        // Supprime le '#'.
        color = color.substring(1, color.length());        
        for (int i=0 ; i<3 ; i++)
            array[i] = decimalToHexa(color.substring(2*i, 2*(i+1)));                
        return new Color(array[0], array[1], array[2]);
    }
    
    /**
     * Retourne la valeur d�cimale de la cha�ne <CODE>hexa</CODE>.
     *
     * @param hexa  Cha�ne contenant une couleur hexa-d�cimale de la forme : D0.
     * @return la valeur d�cimale de la cha�ne <CODE>hexa</CODE>.
     */
    private static int decimalToHexa(final String hexa)
    {
        int value = 0;
        for (int i=0 ; i<hexa.length() ; i++)
        {
            final char c = hexa.charAt(i);
            final int coef = 4*(hexa.length()-i - 1);
            switch (c)
            {
                case 'A' : case 'a' : 
                    value += 10<<coef;
                    break;
                case 'B' : case 'b' : 
                    value += 11<<coef;
                    break;
                case 'C' : case 'c' : 
                    value += 12<<coef;
                    break;
                case 'D' : case 'd' : 
                    value += 13<<coef;
                    break;
                case 'E' : case 'e' : 
                    value += 14<<coef;
                    break;
                case 'F' : case 'f' : 
                    value += 15<<coef;
                    break;     
                case '0' : case '1' : 
                case '2' : case '3' : 
                case '4' : case '5' : 
                case '6' : case '7' : 
                case '8' : case '9' : 
                    value += Character.getNumericValue(c)<<coef;
                    break;                    
            }
        }
        return value;
    }
            
    /**
     * Retourne la liste des param�tres de configuration du parser.
     * @return la liste des param�tres de configuration du parser.
     */
    public static ParameterList getInputDefaultParameterList() 
    {        
        final ParameterList parameters;
        final String descriptor       = "CONFIGURATION";        
        final String[] paramNames     = {FILE};                                                                                      
        final Class[]  paramClasses   = {File.class};
        final Object[]  paramDefaults = {null};
        parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        return parameters;
    }        
}