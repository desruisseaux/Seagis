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
import fr.ird.util.ElementGrid;
import fr.ird.n1b.io.Satellite;
import fr.ird.io.text.ParseSatellite;

// GEOTOOLS
import org.geotools.pt.CoordinatePoint;

/**
 * Cette classe définie les méthodes spécifiques aux images N1B (level 1B) standard pour le 
 * format KLM (nouveau format).
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class ImageReaderN1BKLM extends ImageReaderN1B 
{
    /** Identifiant des paramètres accessibles dans la liste de paramètres de calibration.  */
    public static final String WAVE_LENGTH = "Central wave length",
                               RADIANCE_CONSTANT    = "Constant for radiance computation (Constant1 and constant2)",
                               THERMAL_COEFFICIENT         = "Thermal calibration coefficient",                               
                               SLOPE_INTERCEPT_COEFFICIENT = "Slope, intercept coefficients";
    
    /** Description des canaux. */
    private static final String[] CHANNEL_DESCRIPTION = {"Channel 1  (visible)", 
                                                         "Channel 2  (visible)", 
                                                         "Channel 3a (visible)",
                                                         "Cahnnel 3b (thermal)", 
                                                         "Cahnnel 4  (thermal)", 
                                                         "Channel 5  (thermal)"};
                                                         
    /** Nombre de bandes contenues dans l'image. */ 
    private static final int NUM_BANDE = 6;   
    
    /** Taille du TBM. */
    private final int SIZE_TBM;
    
    /** Taille du Header. */
    private final int SIZE_HEADER;
    
    /** Taille d'un data record. */
    private final int SIZE_DATA;

    /** 
     * A chaque enregistrement et pour chaque canal, 3 valeurs sont disponibles et doivent 
     * être utilisées lors de la calibration des canaux thermiques : a0, a1 et a2.<BR><BR>
     *
     * Elles sont stockées dans le fichier de la facon suivante (pour le canal 3b) :
     * <UL>
     *   <LI>IR Operational Cal Ch 3b Coefficient 1 (a0)</LI>
     *   <LI>IR Operational Cal Ch 3b Coefficient 2 (a1)</LI>
     *   <LI>IR Operational Cal Ch 3b Coefficient 3 (a2)</LI>
     * </UL><BR><BR>
     *
     * La constante ci-dessous indique le nombre de coefficients de calibration thermique 
     * par canal.
     */
    private static final int NUM_COEFFICIENT_THERMAL_CALIBRATION = 3;  
    
    /** 
     * A chaque enregistrement et pour chaque canal, 2 valeurs sont disponibles et doivent 
     * être utilisées lors de la calibration des canaux visibles et proche du visible : 
     * slope et intercept.<BR><BR>
     *
     * Elles sont stockées dans le fichier de la facon suivante (pour le canal 3a) :
     * <UL>
     *   <LI>Visible prelaunch cal ch3a slope 1</LI>
     *   <LI>Visible prelaunch cal ch3a intercept 1</LI>     
     *   <LI>Visible prelaunch cal ch3a slope 2</LI>
     *   <LI>Visible prelaunch cal ch3a intercept 2</LI>     
     *   <LI>Visible prelaunch cal ch3a intersection</LI>     
     * </UL><BR><BR>
     *
     * La constante ci-dessous indique le nombre de coefficients de calibration thermique 
     * par canal.
     */
    private static final int NUM_SLOPE_INTERCEPT_COEF = 5;
    
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
        
    /**
     * Extraction des <i>packed-video data</i> d'un canal.
     *
     * @param iterator      Un itérateur sur une RenderedImage.
     * @param param         Paramètres de l'image.
     * @exception   IOException si une input ou output exception survient.
     */
    public void extractPackedVideoData(final WritableRectIter iterateur, 
                                       final ImageReadParam   param) throws IOException 
    {
        long base = SIZE_TBM + SIZE_HEADER;
        final ImageInputStream input = (FileImageInputStream)this.input;
        final int imageIndex = 0;
        int[] bandeSrc = {0},
              bandeDst = {0};
        DataInputStream bis = null;
        Field fieldPackedVideo = getData().get(Format.DATA_PACKED_VIDEO_DATA);
        byte[] bufferMemoire = new byte[fieldPackedVideo.size];
        int compteur   = 0, 
            word       = 0,
            decalage   = 0,
            indiceLine = 0;
        byte videoData = 0;                        
        
        // prise en compte des parametres eventuels
        if (param != null)
        {
            if (param.getSourceBands() != null)
                bandeSrc = param.getSourceBands();
            
            if (param.getDestinationBands() != null)
                bandeDst = param.getDestinationBands();
        }        
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
                indiceLine++;
                iterateur.startPixels();

                // Pour optimiser le temps de lecture de ces données, elles sont lues en 
                // direct dans le fichier.
                input.seek(base + fieldPackedVideo.offset);                                   
                compteur =0;

                // Positionne le data record en mémoire tampon.
                input.readFully(bufferMemoire);
                bis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bufferMemoire)));

                switch (bandeSrc[indiceBande]) 
                {
                    case 0 :
                        compteur = 0;
                        break;

                    case 1 :
                        compteur = 3;
                        break;
                        
                    case 2 :
                        compteur = 1;
                        break;

                    case 3 :
                        compteur = 1;
                        break;

                    case 4 :
                        compteur = 4;
                        break;

                    case 5 :
                        compteur = 2;
                        break;                                            
                }

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
                    //if (indiceLine == 2) System.out.println(tmp);
                    iterateur.setSample(tmp);
                    iterateur.nextPixel();
                    compteur = (compteur +1) % 5;
                }
                iterateur.nextLine();
                if (indiceLine % 50 == 0)
                    processImageProgress((float)(100.0/getHeight(imageIndex) * (indiceLine+1)) 
                                    / Math.min(bandeSrc.length, bandeDst.length));
                base += SIZE_DATA;
            }
        }
        processImageComplete();            
    }

    /**
     * Retourne les coefficients thermique du <CODE>channel</CODE> désiré.
     *
     * @param channel   Le canal.
     * @return les coefficients thermique du <CODE>channel</CODE> désiré.
     */
    private ElementGrid getIrOperationalCoef(final int channel) throws IOException     
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[] array = new double[NUM_COEFFICIENT_THERMAL_CALIBRATION];        
        final ElementGrid coefficients = new ElementGrid(getHeight(0), NUM_COEFFICIENT_THERMAL_CALIBRATION);
        final Field[] field = new Field[NUM_COEFFICIENT_THERMAL_CALIBRATION];
        
        switch (channel) 
        {
            case 3 : 
                     field[0] = getData().get(Format.DATA_KLM_COEFFICIENT1_CHANNEL_3B);
                     field[1] = getData().get(Format.DATA_KLM_COEFFICIENT2_CHANNEL_3B);
                     field[2] = getData().get(Format.DATA_KLM_COEFFICIENT3_CHANNEL_3B);
                     break;
                
            case 4 :
                     field[0] = getData().get(Format.DATA_KLM_COEFFICIENT1_CHANNEL_4);
                     field[1] = getData().get(Format.DATA_KLM_COEFFICIENT2_CHANNEL_4);
                     field[2] = getData().get(Format.DATA_KLM_COEFFICIENT3_CHANNEL_4);
                     break;
                
            case 5 : 
                     field[0] = getData().get(Format.DATA_KLM_COEFFICIENT1_CHANNEL_5);
                     field[1] = getData().get(Format.DATA_KLM_COEFFICIENT2_CHANNEL_5);
                     field[2] = getData().get(Format.DATA_KLM_COEFFICIENT3_CHANNEL_5);
                     break;
                
            default : throw new IllegalArgumentException("Channel error");
        }
                        
        long base = SIZE_TBM + SIZE_HEADER;        
        for (int indiceLine=0 ; indiceLine<getHeight(0) ; indiceLine++, base+=SIZE_DATA) 
        {
            for (int i=0 ; i<NUM_COEFFICIENT_THERMAL_CALIBRATION ; i++)
                array[i] = (double)field[i].getUnsignedInteger(input, base)/1E6;            
            coefficients.setElement(indiceLine,  new CoordinatePoint(array));            
        }
        return coefficients;        
    }
    
    /**
     * Retourne les coeffcients de calibrations de canaux du visible ou proche du visible. 
     *
     * @param channel   Le vanal désiré.
     * @return les coeffcients de calibrations de canaux du visible ou proche du visible. 
     */
    private ElementGrid getSlopeInterceptCoef(final int channel) throws IOException     
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[] array = new double[NUM_SLOPE_INTERCEPT_COEF];        
        final ElementGrid coefficients = new ElementGrid(getHeight(0), NUM_SLOPE_INTERCEPT_COEF);
        final Field[] field = new Field[NUM_SLOPE_INTERCEPT_COEF];
        
        switch (channel) 
        {
            case 0 : 
                     field[0] = getData().get(Format.DATA_KLM_SLOPE_1_CHANNEL_1);
                     field[1] = getData().get(Format.DATA_KLM_INTERCEPT_1_CHANNEL_1);
                     field[2] = getData().get(Format.DATA_KLM_SLOPE_2_CHANNEL_1);
                     field[3] = getData().get(Format.DATA_KLM_INTERCEPT_2_CHANNEL_1);
                     field[4] = getData().get(Format.DATA_KLM_INTERSECTION_CHANNEL_1);
                     break;
                
            case 1 :
                     field[0] = getData().get(Format.DATA_KLM_SLOPE_1_CHANNEL_2);
                     field[1] = getData().get(Format.DATA_KLM_INTERCEPT_1_CHANNEL_2);
                     field[2] = getData().get(Format.DATA_KLM_SLOPE_2_CHANNEL_2);
                     field[3] = getData().get(Format.DATA_KLM_INTERCEPT_2_CHANNEL_2);
                     field[4] = getData().get(Format.DATA_KLM_INTERSECTION_CHANNEL_2);                     
                     break;
                
            case 2 : 
                     field[0] = getData().get(Format.DATA_KLM_SLOPE_1_CHANNEL_3A);
                     field[1] = getData().get(Format.DATA_KLM_INTERCEPT_1_CHANNEL_3A);
                     field[2] = getData().get(Format.DATA_KLM_SLOPE_2_CHANNEL_3A);
                     field[3] = getData().get(Format.DATA_KLM_INTERCEPT_2_CHANNEL_3A);
                     field[4] = getData().get(Format.DATA_KLM_INTERSECTION_CHANNEL_3A);                     
                     break;
                
            default : throw new IllegalArgumentException("Channel error");
        }
                        
        long base = SIZE_TBM + SIZE_HEADER;        
        for (int indiceLine=0 ; indiceLine<getHeight(0) ; indiceLine++, base+=SIZE_DATA) 
        {
            array[0] = (double)field[0].getUnsignedInteger(input, base)/1E7;            
            array[1] = (double)field[1].getUnsignedInteger(input, base)/1E6;            
            array[2] = (double)field[2].getUnsignedInteger(input, base)/1E7;            
            array[3] = (double)field[3].getUnsignedInteger(input, base)/1E6;            
            array[4] = (double)field[4].getUnsignedInteger(input, base);            
            coefficients.setElement(indiceLine,  new CoordinatePoint(array));            
        }
        return coefficients;        
    }

    /**
     * Retourne la date de fin de l'acquisition.
     * @return la date de fin de l'acquisition.
     */
    public Date getEndTime() throws IOException 
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
    public int getHeight() throws IOException 
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
    public String getSpacecraft() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;                
        final Field field = getTBM().get(Format.TBM_SPACECRAFT_ID);        
        return field.getString(input, 0);
    }
    
    /**
     * Retourne la date de début de l'acquisition.
     * Return la date de début de l'acquisition.
     */
    public Date getStartTime() throws IOException 
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getHeader().get(Format.HEADER_START_TIME);
        return field.getDateFormatv1(input, base);
    }    
    
    /**
     * Retourne la date extraite d'un <i>Data Record</i>.
     *
     * @param field     Champs à extraitre.
     * @param index     Index dans le flux.
     * @return la date extraite d'un <i>Data Record</i>.
     */
    protected Date extractDateFromData(final Field field, final long base) throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;        
        return field.getDateFormatv2(input, base);        
    }    
            
    /**
     * Retourne une liste de paramètres contenant les paramètres de calibration.
     *
     * @param channel   Canal désiré.
     * @return une liste de paramètres contenant les paramètres de calibration.
     */
    public ParameterList getCalibrationParameter(int channel) throws IOException
    {
        final String descriptor       = "AVHRR_KLM";
        final String[] paramNames     = {THERMAL_COEFFICIENT, 
                                         SLOPE_INTERCEPT_COEFFICIENT,
                                         WAVE_LENGTH,
                                         RADIANCE_CONSTANT};
        final Class[]  paramClasses   = {ElementGrid.class,
                                         ElementGrid.class,
                                         Double.class, 
                                         double[].class};
        final Object[]  paramDefaults = {null,
                                         null,
                                         new Double(Double.NaN), 
                                         null};
        ParameterList parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                                         paramNames,
                                                                                         paramClasses,
                                                                                         paramDefaults,
                                                                                         null));                                                         
        parameters.setParameter(SLOPE_INTERCEPT_COEFFICIENT, getSlopeInterceptCoef(channel));
        parameters.setParameter(THERMAL_COEFFICIENT, getIrOperationalCoef(channel));        
        parameters.setParameter(WAVE_LENGTH, getCentralWave(channel));
        parameters.setParameter(RADIANCE_CONSTANT, getConstant(channel));
        return parameters;
    }    

    /**
     * Retourne la longueur d'onde du <CODE>channel</CODE>.
     *
     * @param channel   Le canal désiré.
     * @return la longueur d'onde du <CODE>channel</CODE>.
     */
    private double getCentralWave(final int channel) throws IOException
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        Field field;
        double sf = 1;
        
        switch (channel) 
        {
            case 3 : field = getHeader().get(Format.HEADER_KLM_WAVE_CHANNEL_3B); sf = 1E2; break;                
            case 4 : field = getHeader().get(Format.HEADER_KLM_WAVE_CHANNEL_4);  sf = 1E3; break;                
            case 5 : field = getHeader().get(Format.HEADER_KLM_WAVE_CHANNEL_5);  sf = 1E3; break;                
            default : throw new IllegalArgumentException("Channel error");
        }
        return ((double)field.getInteger(input, base)/sf);
    }    
    
    /**
     * Retourne les constantes <i>constant1</i> et <i>constant2</i>.
     *
     * @param channel   Le canal désiré.
     * @return les constantes <i>constant1</i> et <i>constant2</i>.
     */
    private double[] getConstant(final int channel) throws IOException
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        Field field1, field2;
        
        switch (channel) 
        {
            case 3 : field1 = getHeader().get(Format.HEADER_KLM_CONSTANT1_CHANNEL_3B); 
                     field2 = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_3B); 
                     break;                
            case 4 : field1 = getHeader().get(Format.HEADER_KLM_CONSTANT1_CHANNEL_4); 
                     field2 = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_4); 
                     break;                
            case 5 : field1 = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_5);                 
                     field2 = getHeader().get(Format.HEADER_KLM_CONSTANT1_CHANNEL_5); break;                
            default : throw new IllegalArgumentException("Channel error");
        }
        final double[] array = {((double)field1.getInteger(input, base)/1E5), 
                                ((double)field2.getInteger(input, base)/1E6)};
        return array;
    }    

    /**
     * Retourne la <i>constant2</i>.
     *
     * @param channel   Le canal désiré.
     * @return la <i>constant2</i>.
     */
    private double getConstant2(final int channel) throws IOException
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        Field field;
        
        switch (channel) 
        {
            case 3 : field = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_3B); break;                
            case 4 : field = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_4); break;                
            case 5 : field = getHeader().get(Format.HEADER_KLM_CONSTANT2_CHANNEL_5); break;                
            default : throw new IllegalArgumentException("Channel error");
        }
        return ((double)field.getInteger(input, base)/1E6);
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
        final byte[] bufferMemoire = new byte[(getData().get(Format.DATA_EARTH_LOCATION)).size];                                
        
        for (int indiceLine=0 ; indiceLine<getHeight(imageIndex) ; indiceLine++) 
        {                                  
            // Extraction de la date d'acquisition de l'enregistrement.
            grid.setTime(indiceLine, extractDateFromData(fieldTime, base));

            // Extraction de l'altitude du satellite.
            grid.setAltitude(indiceLine, (float)(fieldAltitude.getUnsignedShort(input, base)/1E1));                                    
            
            input.seek(base + fieldEarthLocalization.offset);
            input.readFully(bufferMemoire);            
            input.seek(base);
            bis = new DataInputStream((new BufferedInputStream(new ByteArrayInputStream(bufferMemoire))));
                    
            // extraction des points de localisations Latitude, longitude 
            // Format : 2 bytes Lat + 2 bytes Long par point.
            for (int i=0 ; i<ImageReaderN1B.NB_CONTROL_POINT_LINE ; i++) 
            {                
                final double latitude  = (double)(bis.readInt())/1E4;
                final double longitude = (double)(bis.readInt())/1E4;                
                grid.setLocalizationPoint(i, indiceLine, longitude, latitude);                
            }                                         
            base += SIZE_DATA;
        }          
        
        grid.removeSingularities();//ies();       
        assert grid.isMonotonic(false);        
        return grid;                
    }  
    
    /**
     * Retourne la direction du satellite lors de l'acquisition <CODE>NORTH_TO_SOUTH</CODE>
     * ou <CODE>SOUTH_TO_NORTH</CODE>.
     * @return la direction du satellite lors de l'acquisition <CODE>NORTH_TO_SOUTH</CODE>
     * ou <CODE>SOUTH_TO_NORTH</CODE>.
     */
    public int getDirection() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final long base1 = SIZE_TBM + SIZE_HEADER,
                   base2 = SIZE_TBM + SIZE_HEADER + (getHeight()-1)*SIZE_DATA;        
        final Field field = getData().get(Format.DATA_EARTH_LOCATION);                
        final double latitude1 = field.getInteger(input, base1)/1.0E4;        
        final double latitude2 = field.getInteger(input, base2)/1.0E4;    
        return ((latitude1 < latitude2) ? SOUTH_TO_NORTH : NORTH_TO_SOUTH);            
    }  
    
    /**
     * Retourne une description du canal.
     *
     * @param channel   Le canal désiré.
     * Retourne une description du canal.
     */
    public String toString(int channel) {
        if (channel < 0 ||channel >= getChannelsNumber())
            throw new IllegalArgumentException("This indexed channel doesn't exist.");
        return CHANNEL_DESCRIPTION[channel];
    }
    
    /**
     * Retourne le nombre de canaux disponibles.
     * @return le nombre de canaux disponibles.
     */
    public int getChannelsNumber() {
        return 6;
    }    
}