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
package fr.ird.io.text;

// J2SE / JAI.
import java.io.File;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;

// SEAGIS
import fr.ird.n1b.io.Satellite;

/**
 * Analyse le fichier de configuration contenant des informations sur le satellite. Ce 
 * fichier contient des informations sur les canaux des satellites (wave length, ...) ainsi
 * que des coefficents de calibration.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public class ParseSatellite extends Parse
{   
    /** Chemin par défaut du fichier de configuration. */
    public final static String DEFAULT_FILE = "application-data/configuration/Satellite.txt";

    /** Identifiant à analyser. */
    public static final String WAVE_LENGTH               = "Central wave length",
                               SATELLITE                 = "Satellite",
                               CHANNEL                   = "Channel",
                               TEMPERATURE_CONSTANT      = "Temperature constant",
                               LINEAR_SPLIT_WINDOW_DAY   = "Linear split window day coefficients",    
                               LINEAR_SPLIT_WINDOW_NIGHT = "Linear split window night coefficients";    
    
    /**
     * Retourne un objet de type <CODE>ParameterList</CODE> contenant les paramètres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     *
     * @param parameter   Les paramètres de configuration du parser.
     * @return un objet de type <CODE>ParameterList</CODE> contenant les paramètres 
     * extrait du fichier <CODE>FILE</CODE> sous la forme de pairs <I>idenfitifant/valeur</I>. 
     */
    public static ParameterList parse(final ParameterList parameter) throws IOException
    {        
        final File file            = extractFileToParse(parameter);
        final Satellite satellite  = extractSatelliteToParse(parameter);        
        final int channel          = extractChannelToParse(parameter);        
        final BufferedReader input = new BufferedReader(new FileReader(file));        
        final ParameterList parameterOut = getOutputDefaultParameterList(parameter);        
        
        // Parcours ligne à ligne.
        String line;        
        while ((line=input.readLine())!=null)
        {
            // Ligne vide ou commentaire.
            line = line.trim();
            if (line.length() == 0 ||line.startsWith("#"))
                continue;     
            
            final StringTokenizer token = new StringTokenizer(line);            
            String value = token.nextToken().trim();
            if (value.equals("NAME")) 
            {
                final StringBuffer buffer = new StringBuffer();
                buffer.append(token.nextToken().trim());
                while (token.hasMoreTokens())
                    buffer.append(" " + token.nextToken());
                if (!satellite.equals(Satellite.get(buffer.toString().trim())))
                    continue;
                else break;
            }
        }
        
        // Aucun paramètre concernant ce satellite.
        if (line == null) 
            return parameterOut;                        
        
        // Extraction des paramètres du satellite.
        while ((line=input.readLine())!=null)
        {
            // Ligne vide ou commentaire.
            line = line.trim();
            if (line.length() == 0 ||line.startsWith("#"))
                continue;     

            final StringTokenizer token = new StringTokenizer(line);            
            String value = token.nextToken().trim();
            if (value.equals("NAME")) 
                return parameterOut;
            
            if (value.equals("WAVE_LENGTH")) 
            {
                value = token.nextToken().trim();
                if ((channel==2 && value.equals("CHANNEL3")) || 
                    (channel==3 && value.equals("CHANNEL4")) || 
                    (channel==4 && value.equals("CHANNEL5")))
                    parameterOut.setParameter(WAVE_LENGTH, Double.parseDouble(token.nextToken()));
            }
            else if (value.equals("LINEAR_SPLIT_WINDOW_DAY") || 
                     value.equals("LINEAR_SPLIT_WINDOW_NIGHT"))
            {
                final double[] array = new double[4];
                int num = -1;
                while (num++<4 && token.hasMoreTokens())
                    array[num] = Double.parseDouble(token.nextToken().trim());                
                if (value.equals("LINEAR_SPLIT_WINDOW_DAY"))
                    parameterOut.setParameter(LINEAR_SPLIT_WINDOW_DAY, array);
                else
                    parameterOut.setParameter(LINEAR_SPLIT_WINDOW_NIGHT, array);                    
            }
            else if (value.equals("ABC")) 
            {
                value = token.nextToken().trim();                
                if ((channel==2 && value.equals("CHANNEL3")) || 
                    (channel==3 && value.equals("CHANNEL4")) || 
                    (channel==4 && value.equals("CHANNEL5")))                
                {                    
                    final double[] array = new double[3];
                    int num = -1;
                    while (num++<3 && token.hasMoreTokens())
                        array[num] = Double.parseDouble(token.nextToken().trim());                
                    parameterOut.setParameter(TEMPERATURE_CONSTANT, array);
                }
            }                
        } 
        return parameterOut;
    }
    
    /**
     * Retourne les paramètres à extraire du fichier par défaut. 
     *
     * @param parameter  Paramètres de configuration du parser. 
     * @return les paramètres à extraire du fichier par défaut. 
     */
    private static ParameterList getOutputDefaultParameterList(final ParameterList parameter) 
    {        
        final Satellite satellite = extractSatelliteToParse(parameter);
        final ParameterList parameters;
        if (!satellite.isKLM())
        {
            final String descriptor       = "AVHRR_AJ";
            final String[] paramNames     = {WAVE_LENGTH,
                                             LINEAR_SPLIT_WINDOW_DAY,
                                             LINEAR_SPLIT_WINDOW_NIGHT};                                             
            final Class[]  paramClasses   = {Double.class,
                                             double[].class, 
                                             double[].class};
            final Object[]  paramDefaults = {null,
                                             new Double(Double.NaN),
                                             null, 
                                             null};            
            parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));
        }
        else 
        {
            final String descriptor       = "AVHRR_KLM";
            final String[] paramNames     = {WAVE_LENGTH,
                                             TEMPERATURE_CONSTANT,
                                             LINEAR_SPLIT_WINDOW_DAY,
                                             LINEAR_SPLIT_WINDOW_NIGHT};                                             
            final Class[]  paramClasses   = {Double.class, 
                                             double[].class, 
                                             double[].class,
                                             double[].class};
            final Object[]  paramDefaults = {null,
                                             null,
                                             null,
                                             null};
            parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                           paramNames,
                                                                           paramClasses,
                                                                           paramDefaults,
                                                                           null));                                                         
        }
        return parameters;
    }
    
    /**
     * Retourne la liste des paramètres de configuration du parser.
     * @return la liste des paramètres de configuration du parser.
     */
    public static ParameterList getInputDefaultParameterList() 
    {        
        final String descriptor       = "CONFIGURATION";
        final String[] paramNames     = {FILE,
                                         SATELLITE,
                                         CHANNEL};                                             
            final Class[]  paramClasses   = {File.class,
                                             Satellite.class,
                                             Integer.class};
            final Object[]  paramDefaults = {new File(ParseGeoref.class.getClassLoader().getResource(DEFAULT_FILE).getPath()),
                                             null,
                                             null};            
            return new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                         paramNames,
                                                                         paramClasses,
                                                                         paramDefaults,
                                                                         null));
    }    
    
    /**
     * Retourne le satellite à analyser.
     *
     * @param La liste de paramètres de configuration du parser.
     * @return le satellite à analyser.
     */
    protected static Satellite extractSatelliteToParse(final ParameterList parameter)
    {
        return (Satellite)parameter.getObjectParameter(SATELLITE);
    }    
    
    /**
     * Retourne le canal à analyser.
     *
     * @param La liste de paramètres de configuration du parser.
     * @return le canal à analyser.
     */
    protected static int extractChannelToParse(final ParameterList parameter)
    {
        return parameter.getIntParameter(CHANNEL);
    }       
}