/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D?veloppement
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
 *          Maison de la t?l?d?tection
 *          Institut de Recherche pour le d?veloppement
 *          500 rue Jean-Fran?ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.io;

// J2SE dependencies
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.FileImageInputStream;

// JAI dependencies
import javax.media.jai.ParameterList;
import javax.media.jai.TiledImage;
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

// SEAGIS
import fr.ird.util.CoefficientGrid;
import fr.ird.n1b.io.Satellite;
import fr.ird.io.text.ParseSatellite;

// GEOTOOLS
import org.geotools.pt.CoordinatePoint;

/**
 * Cette classe définit les méthodes spécifiques aux images N1B (ou level 1B) standard 
 * pour le format KLM (nouveau format). Les données issues des satellites KLM (satellite 
 * 15, 16 et 17) doivent en général être codé selon le format KLM ci-dessous.
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class ImageReaderN1BKLM extends ImageReaderN1B 
{
    /** Nombre de bandes contenues dans l'image. */ 
    private static final int NUM_BANDE = 5;   
    
    /** Taille du TBM. */
    private final int SIZE_TBM;
    
    /** Taille du Header. */
    private final int SIZE_HEADER;
    
    /** Taille d'un data record. */
    private final int SIZE_DATA;
        
    /** 
     * Constructeur.
     *
     * @param provider the service provider that is invoking this constructor,
     *        or <code>null</code> if none.
     */
    public ImageReaderN1BKLM(final ImageReaderSpi provider) 
    {
        super(provider, Format.FORMAT_KLM);
        
        SIZE_TBM    = getTBM().getSize();
        SIZE_HEADER = getHeader().getSize();
        SIZE_DATA   = getData().getSize();        
    }
        
    /////////////////////////////////////////////////    
    /////////////////////////////////////////////////
    // Extraction des coefficients de calibration. //
    /////////////////////////////////////////////////
    /////////////////////////////////////////////////    
    /**
     * Retourne la liste des paramètres de calibration du <CODE>channel</CODE>.
     *
     * @param channel   Canal désiré.
     * @return la liste des paramètres de calibration du <CODE>channel</CODE>.
     */
    public ParameterList getCalibrationParameter(final Channel channel) throws IOException
    {
        final String descriptor = "AVHRR_KLM";
        ParameterList parameters = null;
        if (channel.isThermal())
        {            
            // Thermique.
            final String[] paramNames     = {"THERMAL CALIBRATION COEFFICIENT", 
                                             "CENTRAL WAVE LENGHT",
                                             "RADIANCE CONSTANT"};
            final Class[]  paramClasses   = {CoefficientGrid.class,
                                             Double.class, 
                                             double[].class};
            final Object[]  paramDefaults = {null,
                                             null, 
                                             null};
            parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                               paramNames,
                                                                               paramClasses,
                                                                               paramDefaults,
                                                                               null));                                                         
            parameters.setParameter("THERMAL CALIBRATION COEFFICIENT", 
                                    getIrOperational(channel));        
            parameters.setParameter("CENTRAL WAVE LENGHT", 
                                    getCentralWave(channel));
            parameters.setParameter("RADIANCE CONSTANT", 
                                    getConstant(channel));
            
            /* Temporairement, les valeurs THERMAL CALIBRATION COEFFICIENT seront celle
               recalculé par l'algorithme de calibration des données HRPT. */
            final ParameterList paramInCalib = CalibrationKLM.getInputDefaultParameterList();
            paramInCalib.setParameter("SATELLITE", Satellite.get(metadata.getSpacecraft()));
            paramInCalib.setParameter("CHANNEL", channel);
            paramInCalib.setParameter("BACK SCAN", getBackScan(channel));
            paramInCalib.setParameter("SPACE DATA", getSpaceData(channel));
            paramInCalib.setParameter("TARGET TEMPERATURE DATA", getInternalTargetTemperatureData());
            paramInCalib.setParameter("THERMAL", getIrOperational(channel));
            parameters.setParameter("THERMAL CALIBRATION COEFFICIENT", CalibrationKLM.calibrate(paramInCalib));
        }
        else
        {
            // Visible.
            final String[] paramNames     = {"THERMAL CALIBRATION COEFFICIENT", 
                                             "SLOPE INTERCEPT COEFFICIENTS"};
            final Class[]  paramClasses   = {CoefficientGrid.class,
                                             CoefficientGrid.class};
            final Object[]  paramDefaults = {null,
                                             null};
            parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                               paramNames,
                                                                               paramClasses,
                                                                               paramDefaults,
                                                                               null));                                                         
            parameters.setParameter("SLOPE INTERCEPT COEFFICIENTS", 
                                    getSlopeIntercept(channel));
            parameters.setParameter("THERMAL CALIBRATION COEFFICIENT", 
                                    getIrOperational(channel));        
        }
        return parameters;        
    }    

    /**
     * Retourne la longueur d'onde du <CODE>channel</CODE>.
     *
     * @param channel   Le canal désiré.
     * @return la longueur d'onde du <CODE>channel</CODE>.
     */
    private double getCentralWave(final Channel channel) throws IOException
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field;
        double sf = 1;
        
        if (channel.equals(Channel.CHANNEL_3B))
        {
            field = getHeader().get(Format.HEADER_KLM_WAVE_CHANNEL_3B); 
            sf = 1E2; 
        } else if (channel.equals(Channel.CHANNEL_4))
        {
            field = getHeader().get(Format.HEADER_KLM_WAVE_CHANNEL_4);  
            sf = 1E3; 
        } else if (channel.equals(Channel.CHANNEL_5)) 
        {
            field = getHeader().get(Format.HEADER_KLM_WAVE_CHANNEL_5);  
            sf = 1E3; 
        } else throw new IllegalArgumentException("Erreur de canal.");        
        
        return ((double)field.getInteger(input, base))/sf;
    }    
    
    /**
     * Retourne les constantes <i>constant1</i> et <i>constant2</i> associé au 
     * <i>channel</i>. Les constantes sont retournées dans un tableau contenant deux
     * valeurs : 
     * <UL>
     *  <LI>Constante 1.</LI>
     *  <LI>Constante 2.</LI>
     * </UL>
     *
     * @param channel   Le canal désiré.
     * @return les constantes <i>constant1</i> et <i>constant2</i>.
     */
    private double[] getConstant(final Channel channel) throws IOException
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field1, 
                    field2;
        
        if (channel.equals(Channel.CHANNEL_3B))
        {
            field1 = getHeader().get(Format.HEADER_KLM_CONSTANT1_CHANNEL_3B); 
            field2 = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_3B); 
        } else if (channel.equals(Channel.CHANNEL_4))
        {
            field1 = getHeader().get(Format.HEADER_KLM_CONSTANT1_CHANNEL_4); 
            field2 = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_4); 
        } else if (channel.equals(Channel.CHANNEL_5))
        {
            field1 = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_5);                 
            field2 = getHeader().get(Format.HEADER_KLM_CONSTANT1_CHANNEL_5); 
        } else throw new IllegalArgumentException("Erreur de canal.");
        
        final double[] array = {((double)field1.getInteger(input, base))/1E5, 
                                ((double)field2.getInteger(input, base))/1E6};
        return array;
    }    

    /**
     * Retourne les coefficients thermique du <CODE>channel</CODE> désiré. Les coefficients 
     * sont retournées dans une grille de coefficients contenant pour chaque data record 
     * les coefficients suivants : 
     * <UL>
     *  <LI>IR Operational Cal Coefficient 1 (a0)</LI>
     *  <LI>IR Operational Cal Coefficient 2 (a1)</LI>
     *  <LI>IR Operational Cal Coefficient 3 (a2)</LI>
     * </UL>
     *
     * @param channel   Le canal.
     * @return les coefficients thermique du <CODE>channel</CODE> désiré.
     */
    public CoefficientGrid getIrOperational(final Channel channel) throws IOException     
    {
        /* A chaque enregistrement et pour chaque canal, 3 valeurs sont disponibles : 
           a0, a1 et a2.
           Elles sont stockées dans le fichier de la facon suivante (pour le canal 3b) :
                - IR Operational Cal Ch 3b Coefficient 1 (a0)
                - IR Operational Cal Ch 3b Coefficient 2 (a1)
                - IR Operational Cal Ch 3b Coefficient 3 (a2)
           La constante ci-dessous indique le nombre de coefficients de calibration 
           thermique par canal. */
        final int count = 3;          
        
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[] array         = new double[count];        
        final CoefficientGrid grid   = new CoefficientGrid(getHeight(0), count);
        final Field[] field          = new Field[count];
        
        if (channel.equals(Channel.CHANNEL_3B))
        {
            field[0] = getData().get(Format.DATA_KLM_COEFFICIENT1_CHANNEL_3B);
            field[1] = getData().get(Format.DATA_KLM_COEFFICIENT2_CHANNEL_3B);
            field[2] = getData().get(Format.DATA_KLM_COEFFICIENT3_CHANNEL_3B);
        } else if (channel.equals(Channel.CHANNEL_4))
        {
            field[0] = getData().get(Format.DATA_KLM_COEFFICIENT1_CHANNEL_4);
            field[1] = getData().get(Format.DATA_KLM_COEFFICIENT2_CHANNEL_4);
            field[2] = getData().get(Format.DATA_KLM_COEFFICIENT3_CHANNEL_4);
        } else if (channel.equals(Channel.CHANNEL_5)) 
        { 
            field[0] = getData().get(Format.DATA_KLM_COEFFICIENT1_CHANNEL_5);
            field[1] = getData().get(Format.DATA_KLM_COEFFICIENT2_CHANNEL_5);
            field[2] = getData().get(Format.DATA_KLM_COEFFICIENT3_CHANNEL_5);
        } else throw new IllegalArgumentException("Erreur de canal.");
                        
        long base = SIZE_TBM + SIZE_HEADER;        
        for (int row=0 ; row<getHeight(0) ; row++, base+=SIZE_DATA) 
        {
            for (int index=0 ; index<count ; index++)
                array[index] = ((double)field[index].getUnsignedInteger(input, base))/1.0E6;            
            grid.setElement(row,  array);            
        }
        return grid;        
    }
    
    /**
     * Retourne les coeffcients de calibration SLOPE et INTERCEPT des canaux thermiques.
     * Les coefficients sont retournées dans une grille de coefficients contenant pour 
     * chaque data record les coefficients suivants : 
     * <UL>
     *  <LI>Visible prelaunch slope 1.</LI>
     *  <LI>Visible prelaunch intercept 1.</LI>
     *  <LI>Visible prelaunch slope 2.</LI>
     *  <LI>Visible prelaunch intercept 2.</LI>
     *  <LI>Visible prelaunch intersection.</LI>
     * </UL>
     *
     * @param channel   Le canal désiré.
     * @return les coeffcients de calibration de canaux du visible ou proche du visible. 
     */
    public CoefficientGrid getSlopeIntercept(final Channel channel) throws IOException     
    {
        /* A chaque enregistrement et pour chaque canal, 5 coefficients sont disponibles :
                - Visible prelaunch cal ch3a slope 1
                - Visible prelaunch cal ch3a intercept 1
                - Visible prelaunch cal ch3a slope 2
                - Visible prelaunch cal ch3a intercept 2
                - Visible prelaunch cal ch3a intersection
           La constante ci-dessous indique le nombre de coefficients de calibration 
           thermique par canal. */
        final int count = 5;

        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[] array         = new double[count];        
        final CoefficientGrid grid   = new CoefficientGrid(getHeight(0), count);
        final Field[] field          = new Field[count];
        
        if (channel.equals(Channel.CHANNEL_1))             
        {
            field[0] = getData().get(Format.DATA_KLM_SLOPE_1_CHANNEL_1);
            field[1] = getData().get(Format.DATA_KLM_INTERCEPT_1_CHANNEL_1);
            field[2] = getData().get(Format.DATA_KLM_SLOPE_2_CHANNEL_1);
            field[3] = getData().get(Format.DATA_KLM_INTERCEPT_2_CHANNEL_1);
            field[4] = getData().get(Format.DATA_KLM_INTERSECTION_CHANNEL_1);
        } else if (channel.equals(Channel.CHANNEL_2))             
        {
            field[0] = getData().get(Format.DATA_KLM_SLOPE_1_CHANNEL_2);
            field[1] = getData().get(Format.DATA_KLM_INTERCEPT_1_CHANNEL_2);
            field[2] = getData().get(Format.DATA_KLM_SLOPE_2_CHANNEL_2);
            field[3] = getData().get(Format.DATA_KLM_INTERCEPT_2_CHANNEL_2);
            field[4] = getData().get(Format.DATA_KLM_INTERSECTION_CHANNEL_2);                     
        } else if (channel.equals(Channel.CHANNEL_3A))
        {   
            field[0] = getData().get(Format.DATA_KLM_SLOPE_1_CHANNEL_3A);
            field[1] = getData().get(Format.DATA_KLM_INTERCEPT_1_CHANNEL_3A);
            field[2] = getData().get(Format.DATA_KLM_SLOPE_2_CHANNEL_3A);
            field[3] = getData().get(Format.DATA_KLM_INTERCEPT_2_CHANNEL_3A);
            field[4] = getData().get(Format.DATA_KLM_INTERSECTION_CHANNEL_3A);                     
        } else throw new IllegalArgumentException("Erreur de canal.");
                        
        long base = SIZE_TBM + SIZE_HEADER;        
        for (int row=0 ; row<getHeight(0) ; row++, base+=SIZE_DATA) 
        {
            array[0] = field[0].getUnsignedInteger(input, base)/1E7;            
            array[1] = field[1].getUnsignedInteger(input, base)/1E6;            
            array[2] = field[2].getUnsignedInteger(input, base)/1E7;            
            array[3] = field[3].getUnsignedInteger(input, base)/1E6;            
            array[4] = field[4].getUnsignedInteger(input, base);            
            grid.setElement(row,  array);            
        }
        return grid;        
    }
    
    /**
     * Retourne la température interne des "platinum resistance thermometers". Les 
     * coefficients sont retournées dans une grille de coefficients contenant pour 
     * chaque data record les coefficients suivants : 
     * <UL>
     *  <LI>PRT Reading 1.</LI>
     *  <LI>PRT Reading 2.</LI>
     *  <LI>PRT Reading 3.</LI>
     * </UL><BR><BR>
     *
     * Tous les 5 scans, les PRTs contiennent une valeur de référence 0.
     *
     * @return le compte numérique PRT nécessaire au calcul des coefficients
     * de calibration .
     */
    public CoefficientGrid getInternalTargetTemperatureData() throws IOException
    {
        /* A chaque enregistrement on extrait :
               - PRT Reading 1
               - PRT Reading 2
               - PRT Reading 3
           La constante ci-dessous indique le nombre de coefficients PRT(Platinium 
           Resistance Thermometers) traités dans le bloc DATA. */
        final int count = 3;     
        
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[] array = new double[count] ;
        final CoefficientGrid grid = new CoefficientGrid(getHeight(0),count);
        final Field[] field = {getData().get(Format.DATA_KLM_PRT1),        
                               getData().get(Format.DATA_KLM_PRT2),
                               getData().get(Format.DATA_KLM_PRT3)};

        long base = SIZE_TBM + SIZE_HEADER;                               
        for(int row=0; row<getHeight(0); row++, base+=SIZE_DATA)
        {
            array[0] = field[0].getUnsignedShort(input, base) & 1023;
            array[1] = field[1].getUnsignedShort(input, base) & 1023;
            array[2] = field[2].getUnsignedShort(input, base) & 1023;            
            grid.setElement(row, array);                   
        }
        return grid;        
    }    
    
    /**
     * Retourne les "Back Scan" associé à un canal thermique. Les coefficients sont 
     * retournées dans une grille de coefficients contenant pour chaque data record les 
     * coefficients suivants : 
     * <UL>
     *  <LI>Word 1.</LI>
     *  <LI>Word 2.</LI>
     *  <LI>...</LI>
     *  <LI>Word 9.</LI>
     *  <LI>Word 10.</LI>
     * </UL>
     *
     * @param channel Canal désiré.
     * @return les comptes numeriques du Back Scan nécessaire au calcul des coefficients 
     * de calibration.
     */
    public CoefficientGrid getBackScan(final Channel channel) throws IOException
    {         
        final int numWord = 10;
        final ImageInputStream input = (FileImageInputStream)this.input;                             
        final CoefficientGrid grid   = new CoefficientGrid(getHeight(0), numWord); 
        final double[] array = new double[numWord] ;
        final Field fieldBackScan = getData().get(Format.DATA_KLM_BACK_SCAN);        
                
        DataInputStream bis  = null;
        byte[] buffer = new byte[fieldBackScan.size];
        long base     = SIZE_TBM + SIZE_HEADER ;     
        int compteur  = 0;
        int word      = 0;
         
        for (int row= 0; row<getHeight(0) ; row++, base+=SIZE_DATA) 
        {             
            input.seek(base + fieldBackScan.offset);
            input.readFully(buffer);
            bis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer)));                          
            compteur = 0;
            int i    = 0;
            while (compteur <30) 
            {                              
                // Lecture du mot de 2 octets.
                word = bis.readUnsignedShort();  
                if (channel.equals(Channel.CHANNEL_3B))
                {
                    if ((compteur%3)==0)
                        array[i++] = word;
                } else if (channel.equals(Channel.CHANNEL_4))
                {
                    if ((compteur-1)%3 == 0)
                        array[i++] = word;
                } else if (channel.equals(Channel.CHANNEL_5))
                {
                    if ((compteur-2)%3 == 0 ) 
                        array[i++] = word;
                } else throw new IllegalArgumentException("Erreur de canal.");
                compteur++;                    
            }
            grid.setElement(row, array);       
            if (false)
            {
                for (int k=0; k<array.length ; k++)
                    System.out.print(array[k] + "\t");
                System.out.println("");
            }
            
        } 
        return grid;         
    }     
     
    /**
     * Retourne les "Space Data" d'un canal. Les coefficients sont retournées dans une 
     * grille de coefficients contenant pour chaque data record les coefficients suivants : 
     * <UL>
     *  <LI>Word 1.</LI>
     *  <LI>Word 2.</LI>
     *  <LI>...</LI>
     *  <LI>Word 9.</LI>
     *  <LI>Word 10.</LI>
     * </UL>
     *
     * @param channel   Canal désiré (1,2,3,4,5).
     * @return les comptes numeriques du Space Data nécessaire pour calculer les 
     * coefficients de calibrations.
     */
    public CoefficientGrid getSpaceData(final Channel channel) throws IOException 
    {
        final int numWord = 10;
        final ImageInputStream input = (FileImageInputStream)this.input;
        final CoefficientGrid  grid  = new CoefficientGrid(getHeight(0), numWord); 
        final double[] array = new double[numWord] ;
        final Field[] field = new Field[numWord];
        
        DataInputStream bis = null;
        Field fieldBackScan = getData().get(Format.DATA_KLM_SPACE_DATA);
        byte[] buffer = new byte[fieldBackScan.size];
        long base = SIZE_TBM + SIZE_HEADER ;     
        int compteur = 0;
        int word     = 0;
         
        for (int row=0; row<getHeight(0) ; row++, base+=SIZE_DATA) 
        {
            input.seek(base + fieldBackScan.offset);
            input.readFully(buffer);
            bis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer)));
             
            compteur = 0;
            int i = 0;
            while (compteur <50) 
            {  
                // Lecture du mot de 2 octets.
                word = bis.readUnsignedShort();  
                if (channel.equals(Channel.CHANNEL_1))
                {
                    if ((compteur)%5==0)
                        array[i++] = word;                    
                } else if (channel.equals(Channel.CHANNEL_2))
                {
                    if ((compteur-1)%5==0)
                        array[i++] = word;
                } else if (channel.equals(Channel.CHANNEL_3)  || 
                           channel.equals(Channel.CHANNEL_3A) ||
                           channel.equals(Channel.CHANNEL_3B))
                {
                    if ((compteur-2)%5==0)
                        array[i++] = word;
                } else if (channel.equals(Channel.CHANNEL_4))
                {
                    if ((compteur-3)%5 == 0)
                        array[i++] = word;
                } else if (channel.equals(Channel.CHANNEL_5))
                {
                    if ((compteur-4)%5 == 0 ) 
                        array[i++] = word;                            
                }
                compteur++;                    
            }
            grid.setElement(row, array);                   
            if (false)
            {
                for (int k=0; k<array.length ; k++)
                    System.out.print(array[k] + "\t");
                System.out.println("");
            }            
        }
        return grid;         
    }   
    
    /**
     *  Retourne les coefficients de température du corps noir. Les coefficients sont 
     * retournées dans une grille de coefficients contenant pour chaque data record les 
     * coefficients suivants : 
     * <UL>
     *  <LI>IR Target Temperature 1 Conversion Coefficient 1.</LI>
     *  <LI>IR Target Temperature 1 Conversion Coefficient 2.</LI>
     *  <LI>IR Target Temperature 1 Conversion Coefficient 3.</LI>
     *  <LI>IR Target Temperature 1 Conversion Coefficient 4.</LI>
     *  <LI>IR Target Temperature 1 Conversion Coefficient 5.</LI>
     *  <LI>IR Target Temperature 1 Conversion Coefficient 6.</LI>
     * </UL>
     *
     *  Note : les coeff d4 et d5 sont nulles.
     *  @return les coefficients de température du corps noir. 
     **/
    public double[][] getIrTemperatureCoef() throws IOException
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[][] array = new double[4][6];        
        final Field[] field = {getHeader().get(Format.HEADER_KLM_TEMPERATURE1_COEFF1),
                               getHeader().get(Format.HEADER_KLM_TEMPERATURE1_COEFF2),
                               getHeader().get(Format.HEADER_KLM_TEMPERATURE1_COEFF3),
                               getHeader().get(Format.HEADER_KLM_TEMPERATURE1_COEFF4),
                               getHeader().get(Format.HEADER_KLM_TEMPERATURE1_COEFF5),
                               getHeader().get(Format.HEADER_KLM_TEMPERATURE1_COEFF6)};
        
        long base = SIZE_TBM ;     
        for(int row=0; row<4 ; row++, base+=12)
        {
            array[row][0] = (double)field[0].getUnsignedShort(input, base)/1.0E2;        
            array[row][1] = (double)field[1].getUnsignedShort(input, base)/1.0E5;            
            array[row][2] = (double)field[2].getUnsignedShort(input, base)/1.0E8;            
            array[row][3] = (double)field[3].getUnsignedShort(input, base)/1.0E11;            
            array[row][4] = (double)field[4].getUnsignedShort(input, base)/1.0E14;
            array[row][5] = (double)field[4].getUnsignedShort(input, base)/1.0E17;
        }      
        return array;        
    }           
    
    ////////////////////////////////////////
    ////////////////////////////////////////
    // Extraction des principales données //
    ////////////////////////////////////////
    ////////////////////////////////////////
    /**
     * Extraction des <i>packed-video data</i>.
     *
     * @param iterator      Un itérateur sur une RenderedImage.
     * @param param         Paramètres de l'image.
     */
    public void extractPackedVideoData(final WritableRectIter iterator, 
                                        final ImageReadParam   param) throws IOException 
    {
        /* Par defaut, l'ensemble des bandes sont extraites. */
        int[] bandSrc = {0, 1, 2, 3, 4},
              bandTgt = {0, 1, 2, 3, 4};
              
        /* Paramètres .*/
        if (param!=null)
        {
            if (param.getSourceBands()!=null)
                bandSrc = param.getSourceBands();
            if (param.getDestinationBands()!=null)
                bandTgt = param.getDestinationBands();
        }
                
        /* On charge successivement chacune des bandes sources dans sa bande de destination. */        
        final int length = bandSrc.length;
        if (bandTgt.length!=length)
            throw new IllegalArgumentException("Les nombres de bandes source et destination sont différents.");
        //processImageStarted(0);        
        for (int index=0 ; index<length ; index++)
            extractBand(iterator, bandSrc[index], bandTgt[index]);
        //processImageComplete();                    
    }

    /**
     * Extraction d'une bande source vers une bande destination.
     *
     * @param iterator      Un itérateur sur une RenderedImage.
     * @param bandSrc       Bande source.
     * @param bandTgt       Bande destination.
     */    
    public void extractBand(final WritableRectIter iterator, 
                            final int              bandSrc,
                            final int              bandTgt) throws IOException
    {
        /* Vérification des bandes. */
        if (bandSrc<0 || bandSrc>=getNumBands())
            throw new IllegalArgumentException("La bande source est inexistante.");
        
        final ImageInputStream input = (FileImageInputStream)this.input;
        final Field field            = getData().get(Format.DATA_PACKED_VIDEO_DATA);        

        /* On se place sur la bande de destination. */
        int band = 0;
        iterator.startBands();
        while (band<bandTgt && !iterator.finishedBands())
        {
            iterator.nextBand();    
            band++;
        }
        
        if (band!=bandTgt)
            throw new IllegalArgumentException("La bande cible est inexistante.");

        /* Extraction des informations de la bande. */
        int row      = 0,
            compteur = 0;             
        long base = SIZE_TBM + SIZE_HEADER;        
        iterator.startLines();
        iterator.startPixels();
        while(iterator.finishedLines() == false)       
        {
            iterator.startPixels();
            input.seek(base + field.offset);                                   
            row++;
            compteur =0;

            // Mise en mémoire des données correspondants à la ligne.
            final byte[] buffer = new byte[field.size];                
            input.readFully(buffer);
            final DataInputStream bis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer)));
            switch (bandSrc) 
            {
                case 0 : compteur = 0; break;
                case 1 : compteur = 3; break;                        
                case 2 : compteur = 1; break;
                case 3 : compteur = 4; break;
                case 4 : compteur = 2; break;                                                                    
            }

            while (!iterator.finishedPixels()) 
            {                              
                /* Lecture des données par 4 octets. */
                final int word = bis.readInt();                
                int decalage = 0;
                switch (compteur) 
                {
                    case 0 : decalage = 20; break;
                    case 1 : decalage = 0;  break;
                    case 2 : case 4 : compteur = (compteur +1) % 5; continue;
                    case 3 : decalage = 10; break;
                }
                iterator.setSample((int)((word >>>decalage) & 1023));
                iterator.nextPixel();
                compteur = (compteur+1)%5;
            }
            iterator.nextLine();
            /*if (row % 50 == 0)
                processImageProgress((float)(100.0/getHeight(0) * (row+1)) 
                                / Math.min(bandSrc.length, bandTgt.length));*/
            base += SIZE_DATA;
        }           
    }
    
    /**
     * Extraction des <i>packed-video data</i> d'un canal.
     *
     * @param iterator      Un itérateur sur une RenderedImage.
     * @param channel       Canal désiré.
     * @exception   IOException si une input ou output exception survient.
     */
    public void extractPackedVideoData(final WritableRectIter iterateur, 
                                       final Channel          channel) throws IOException 
    {
        long base = SIZE_TBM + SIZE_HEADER;
        final ImageInputStream input = (FileImageInputStream)this.input;
        final int imageIndex = 0;
        int[] bandeSrc = {0},
              bandeDst = {0};
        final Field field = getData().get(Format.DATA_PACKED_VIDEO_DATA);
        byte[] buffer = new byte[field.size];
        int compteur   = 0, 
            word       = 0,
            decalage   = 0,
            row = 0;
        byte videoData = 0;                        
        
        processImageStarted(0);        
        
        for (int indiceBande=0 ; indiceBande<bandeSrc.length && indiceBande<bandeDst.length ; indiceBande++)
        {
            if (bandeSrc[indiceBande] >= NUM_BANDE)
                throw new IllegalArgumentException("Index de la bande source en dehors des limites de l'image.");        

            iterateur.startBands();
            iterateur.startLines();
            iterateur.startPixels();

            // On se positionne sur la bonne bande.
            while(bandeDst[indiceBande] > 0 && iterateur.finishedBands() == false)
                iterateur.nextBand();

            if (bandeDst[indiceBande] > 0)
                throw new IllegalArgumentException("Index de la bande de destination en dehors des limites de l'image.");        

            // Parcours des lignes.
            while(iterateur.finishedLines() == false)       
            {
                boolean extract = true;
                if ((channel.equals(Channel.CHANNEL_3A) && !(getStateChannel3(row)==1)) || 
                    (channel.equals(Channel.CHANNEL_3B) && !(getStateChannel3(row)==0)))
                    extract = false;                
                
                if (extract) 
                {
                    iterateur.startPixels();

                    // Pour optimiser le temps de lecture de ces données, elles sont lues en 
                    // direct dans le fichier.
                    input.seek(base + field.offset);                                   
                    compteur =0;

                    // Positionne le data record en mémoire tampon.
                    input.readFully(buffer);
                    DataInputStream bis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer)));

                    if (channel.equals(Channel.CHANNEL_1))                         
                            compteur = 0;
                    else if (channel.equals(Channel.CHANNEL_2))
                            compteur = 3;
                    else if (channel.equals(Channel.CHANNEL_3))                        
                            compteur = 1;
                    else if (channel.equals(Channel.CHANNEL_4))
                            compteur = 4;
                    else if (channel.equals(Channel.CHANNEL_5))
                            compteur = 2;

                    while (iterateur.finishedPixels() == false) 
                    {                              
                        // Lecture du mot de 4 octets.
                        word = bis.readInt();                

                        switch (compteur) 
                        {
                            case 0 : 
                                decalage = 20;
                                break;

                            case 1 :          
                                decalage = 0;
                                break;

                            case 2 : case 4 :
                                compteur = (compteur +1) % 5;
                                continue;

                            case 3 :     
                                decalage = 10;
                                break;
                        }
                        int tmp = (int)((word >>>decalage) & 1023);
                        iterateur.setSample(tmp);
                        iterateur.nextPixel();
                        compteur = (compteur +1) % 5;
                    }                
                }
                iterateur.nextLine();
                row++;                
                if (row % 50 == 0)
                    processImageProgress((float)(100.0/getHeight(imageIndex) * (row+1)) 
                                    / Math.min(bandeSrc.length, bandeDst.length));
                base += SIZE_DATA;
            }
        }
        processImageComplete();            
    }

    /**
     * Retourne une image contenant les <i>packed-video data</i> d'un canal. L'image
     * retournée contiendra uniquement les informations du canal désiré.
     *
     * @param imageIndex    Index de l'image à extraire.
     * @param channel       Canal désiré.
     * @return      une image contenant les <i>packed-video data</i> d'un canal.
     */
    public BufferedImage read(final int imageIndex, final Channel channel) throws IOException
    {
        final int[] bandeSrc = {getBand(channel)},
                    bandeTgt = {0};
        final ImageReadParam param = new ImageReadParam();
        param.setSourceBands(bandeSrc);                                                           
        param.setDestinationBands(bandeTgt);                                                           
        BufferedImage image = new BufferedImage(NB_PIXEL_LINE,
                                                getHeight(imageIndex), 
                                                BufferedImage.TYPE_USHORT_GRAY);        
        WritableRectIter rif = RectIterFactory.createWritable(image, null);
        extractPackedVideoData(rif, param);
        return image;
    }           

    /**
     * Extraction d'une grille contenant l'ensemble des points de localisation. 
     *
     * @return      Une grille de points de localisation.
     * @exception   IOException si une input ou output exception survient.
     */
    public LocalizationGridN1B getGridLocalization() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final int imageIndex = 0;
        long base = SIZE_TBM + SIZE_HEADER;        
        DataInputStream bis = null;        
        final LocalizationGridN1B grid = new LocalizationGridN1B(NB_CONTROL_POINT_LINE, 
                                                                 getHeight(imageIndex));        
        final Field fieldEarthLocalization = getData().get(Format.DATA_EARTH_LOCATION),
                                 fieldTime = getData().get(Format.DATA_TIME),              
                                 fieldAltitude = getData().get(Format.DATA_KLM_SPACECRAFT_ALTITUDE);        
        final byte[] buffer = new byte[(getData().get(Format.DATA_EARTH_LOCATION)).size];                                
        
        for (int row=0 ; row<getHeight(imageIndex) ; row++) 
        {                                  
            // Extraction de la date d'acquisition de l'enregistrement.
            grid.setTime(row, extractDateFromData(fieldTime, base));

            // Extraction de l'altitude du satellite.
            grid.setAltitude(row, (float)(fieldAltitude.getUnsignedShort(input, base)/1E1));                                    
            
            input.seek(base + fieldEarthLocalization.offset);
            input.readFully(buffer);            
            input.seek(base);
            bis = new DataInputStream((new BufferedInputStream(new ByteArrayInputStream(buffer))));

            // extraction des points de localisations Latitude, longitude 
            // Format : 2 bytes Lat + 2 bytes Long par point.
            for (int i=0 ; i<ImageReaderN1B.NB_CONTROL_POINT_LINE ; i++) 
            {                
                final double latitude  = (double)(bis.readInt())/1E4;
                final double longitude = (double)(bis.readInt())/1E4;                
                grid.setLocalizationPoint(i, row, longitude, latitude);                
            }                                         
            base += SIZE_DATA;
        }          
        
        grid.removeSingularities();
        assert grid.isMonotonic(false);        
        return grid;                
    }  
    
    /////////////////////////////
    /////////////////////////////
    // Extraction des Metadata //
    /////////////////////////////
    /////////////////////////////        
    /**
     * Retourne la date de fin de l'acquisition.
     * @return la date de fin de l'acquisition.
     */
    protected Date getEndTime() throws IOException 
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getHeader().get(Format.HEADER_STOP_TIME);
        return field.getDateFormatv1(input, base);
    }
    
    /**
     * Retourne la hauteur de l'image.
     * @return la hauteur de l'image.
     */
    protected int getHeight() throws IOException 
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getHeader().get(Format.HEADER_NUMBER_OF_SCANS);        
        return (short)field.getUnsignedShort(input, base);
    }
    
    /**
     * Retourne une chaîne de caractère identifiant le satellite utilisé pour l'acquisition.
     * @return une chaîne de caractère identifiant le satellite utilisé pour l'acquisition.
     */
    protected String getSpacecraft() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;                
        final Field field = getTBM().get(Format.TBM_SPACECRAFT_ID);        
        return field.getString(input, 0);
    }
    
    /**
     * Retourne la date de début de l'acquisition.
     * Return la date de début de l'acquisition.
     */
    protected Date getStartTime() throws IOException 
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getHeader().get(Format.HEADER_START_TIME);
        return field.getDateFormatv1(input, base);
    }    

    /**
     * Retourne la direction du satellite lors de l'acquisition <CODE>ImageReaderN1B.NORTH_TO_SOUTH</CODE>
     * ou <CODE>ImageReaderN1B.SOUTH_TO_NORTH</CODE>.
     * @return la direction du satellite lors de l'acquisition <CODE>ImageReaderN1B.NORTH_TO_SOUTH</CODE>
     * ou <CODE>ImageReaderN1B.SOUTH_TO_NORTH</CODE>.
     */
    protected int getDirection() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final long base1 = SIZE_TBM + SIZE_HEADER,
                   base2 = SIZE_TBM + SIZE_HEADER + (getHeight()-1)*SIZE_DATA;        
        final Field field = getData().get(Format.DATA_EARTH_LOCATION);                
        final double latitude1 = field.getInteger(input, base1)/1.0E4;        
        final double latitude2 = field.getInteger(input, base2)/1.0E4;    
        return ((latitude1 < latitude2) ? SOUTH_TO_NORTH : NORTH_TO_SOUTH);            
    }  

    /////////////////////
    /////////////////////
    // Autres méthodes //
    /////////////////////
    /////////////////////    
    /**
     * Retourne le nombre de bandes de l'image.
     * @return le nombre de bandes de l'image.
     */
    public int getNumBands() 
    {
        return 5;
    }      

    /////////////////////////
    /////////////////////////    
    // Méthodes internes . //
    /////////////////////////
    /////////////////////////            
    /**
     * Retourne la date extraite d'un <i>Data Record</i>.
     *
     * @param field     Champs à extraitre.
     * @param index     Index dans le flux.
     * @return la date extraite d'un <i>Data Record</i>.
     */
    protected Date extractDateFromData(final Field field, final long base) 
                                                                    throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;        
        return field.getDateFormatv2(input, base);        
    }    
            
    /**
     * Retourne <i>0</i> si le canal 3b est sélectionné, <i>1</i> si le canal 3a est 
     * sélectionné et <i>2</i> pour la transition d'un canal à l'autre.
     *
     * @param row   Numéro de la ligne.
     * @return <i>0</i> si le canal 3b est sélectionné, <i>1</i> si le canal 3a est 
     * sélectionné et <i>2</i> pour la transition d'un canal à l'autre.
     */
    private int getStateChannel3(final int row) throws IOException
    {
        final long base = SIZE_TBM + SIZE_HEADER + row*SIZE_DATA;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getData().get(Format.DATA_KLM_AVHRR_DIGITAL_B_DATA);        
        return (field.getUnsignedShort(input, base) & 128) / 128;        
    }
        
    /**
     * Retourne la bande contenant les données du canal.
     *
     * @param channel   Le canal désiré.
     * @return la bande contenant les données du canal.
     */
    private int getBand(final Channel channel)
    {
        final int band;
         if (channel.equals(Channel.CHANNEL_1))
            band = 0;
        else if (channel.equals(Channel.CHANNEL_2))
            band = 1;
        else if (channel.equals(Channel.CHANNEL_3))            
            band = 2;
        else if (channel.equals(Channel.CHANNEL_3A))            
            band = 2;
        else if (channel.equals(Channel.CHANNEL_3B))            
            band = 2;
        else if (channel.equals(Channel.CHANNEL_4))            
            band = 3;
        else if (channel.equals(Channel.CHANNEL_5))                        
            band = 4;
        else throw new IllegalArgumentException("Canal inexistant.");
        return band;
    }
}