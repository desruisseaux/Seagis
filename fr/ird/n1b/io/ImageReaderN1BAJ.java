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
import javax.media.jai.ParameterListImpl;
import javax.media.jai.ParameterListDescriptorImpl;
import javax.media.jai.TiledImage;
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
import fr.ird.io.text.ParseSatellite;
import fr.ird.io.text.ParseSatellite;

// GEOTOOLS
import org.geotools.pt.CoordinatePoint;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.TransformException;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeocentricCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.cs.HorizontalDatum;
import org.geotools.cs.PrimeMeridian;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.units.Unit;

/**
 * Cette classe d�finie les m�thodes sp�cifiques aux images N1B (level 1B) standard pour le 
 * format POD (ancien format utilis� pour le stockage des donn�es issues des satellites
 * NOAA A-J).
 *
* @author Remi EVE
 * @version $Id$
 */
public class ImageReaderN1BAJ extends ImageReaderN1B 
{        
    /** Identifiant des param�tres accessibles dans la liste de param�tres de calibration.  */
    public static final String SLOPE_INTERCEPT_COEFFICIENT = "Slope, intercept coefficients";
    
    /** Description des canaux. */
    private static final String[] CHANNEL_DESCRIPTION = {"Channel 1  (visible)", 
                                                         "Channel 2  (visible)", 
                                                         "Channel 3  (visible)",
                                                         "Cahnnel 4  (thermal)", 
                                                         "Channel 5  (thermal)"};
                                                         
    /** Nombre de bandes de l'image. */ 
    private static final int NUM_BANDE = 5;      

    /** Taille du TBM. */
    private final int SIZE_TBM;
    
    /** Taille du Header. */
    private final int SIZE_HEADER;
    
    /** Taille d'un Data record. */
    private final int SIZE_DATA;
    
    /** 
     * Transformation permettant de passer du syst?me de coordonn?es
     * g?ocentrique vers le syst?me g?ographique.
     */
    private final CoordinateTransformation transformation;

    /** 
     * A chaque enregistrement et pour chaque canal, deux valeurs sont disponibles et 
     * utilis�es lors de la calibration. Elles sont stock�es dans le fichier comme suit :
     * <BR><BR>
     * <UL>
     *   <LI><i>slope</i> value</LI>
     *   <LI><i>intercept</i> value</LI>
     * </UL>
     * <BR><BR>
     * La constante ci-dessous indique le nombre de coefficients de calibration par 
     * canal et par ligne.
     */
    private static final int NUM_COEF_SLOPE_INTERCEPT  = 2;  

    /** 
     * Creates a new instance of ImageReaderN1BAJ 
     *
     * @param provider the service provider that is invoking this constructor,
     *        or <code>null</code> if none.
     */
    public ImageReaderN1BAJ(final ImageReaderSpi provider) 
    {
        super(provider, Format.FORMAT_AJ);
        
        SIZE_TBM    = getTBM().getSize();
        SIZE_HEADER = getHeader().getSize();
        SIZE_DATA   = getData().getSize();
        
        /*
         * Le syst?me de coordonn?es g?ocentrique. Le m?ridien d'origine est celui de Greenwich
         * et les unit?s sont les <strong>kilom?tres</strong>. L'axe des <var>x</var> pointe vers
         * le m?ridient de Greenwich, L'axe des <var>y</var> pointe l'est et l'axe des <var>z</var>
         * pointe vers nord.
         */
        final CoordinateSystem sourceCS = new GeocentricCoordinateSystem
              ("G?ocentrique (km)", Unit.KILOMETRE, HorizontalDatum.WGS84, PrimeMeridian.GREENWICH);
        /*
         * Le syst?me de coordonn?es g?ographique (longitude / latitude / altitude)
         */
        final CoordinateSystem targetCS = CompoundCoordinateSystem.WGS84;
        /*
         * Construit la transformation des coordonn?es g?ocentriques vers les coordonn?es
         * g?ographiques. Cette construction ne devrait jamais ?chouer, puisque nous avons
         * d?finis des syst?mes de coordonn?es bien connus.
         */
        try 
        {
            CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();
            transformation = factory.createFromCoordinateSystems(sourceCS, targetCS);
        }
        catch (CannotCreateTransformException exception)
        {
            throw new AssertionError(exception);
        }                        
    }    
    
    /**
     * Extraction des <i>packed-video data</i> d'un canal.
     *
     * @param iterator      Un it�rateur sur une RenderedImage.
     * @param param         Param�tres de l'image.
     * @exception   IOException si une input ou output exception survient.
     */
    public void extractPackedVideoData(final WritableRectIter   iterateur, 
                                       final ImageReadParam     param) throws IOException 
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
        
        // Prise en compte des parametres eventuels.
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
                throw new IllegalArgumentException("Index de la bande source en " + 
                                                   "dehors des limites de l'image.");        

            iterateur.startBands();
            iterateur.startLines();
            iterateur.startPixels();

            // On se positionne sur la bonne bande.
            while(bandeDst[indiceBande] > 0 && iterateur.finishedBands() == false)
                iterateur.nextBand();

            if (bandeDst[indiceBande] > 0)
                throw new IllegalArgumentException("Index de la bande de destination en " + 
                                                  "dehors des limites de l'image.");        

            // Parcours des lignes.
            while(iterateur.finishedLines() == false)       
            {
                indiceLine++;
                iterateur.startPixels();

                // Pour optimiser le temps de lecture, les donn�es sont lues en direct 
                // dans le fichier.
                input.seek(base + fieldPackedVideo.offset);                                   
                compteur =0;

                // Mise en m�moire d'un data record.
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
                        compteur = 4;
                        break;

                    case 4 :
                        compteur = 2;
                        break;                    
                }

                while (iterateur.finishedPixels() == false) 
                {                              
                    // Lecture d'un mot de 4 octets.
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
     * Retourne une grille contenant les coefficients de calibration du <code>channel</code>
     *
     * @param channel   Canal d�sir�.
     * @return une grille contenant les coefficients de calibration du <code>channel</code>.
     */
    private CoefficientGrid getSlopeInterceptCoef(final int channel) throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double div1 = 2<<29;  
        final double div2 = 2<<21;
        final double[] array = new double[NUM_COEF_SLOPE_INTERCEPT];        
        final CoefficientGrid coefficients = new CoefficientGrid(getHeight(0), NUM_COEF_SLOPE_INTERCEPT);
        Field fieldCoeff;
        
        switch (channel) 
        {
            case 0 : fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_1); break;                
            case 1 : fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_2); break;                
            case 2 : fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_3); break;                
            case 3 : fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_4); break;                
            case 4 : fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_5); break;                
            default : throw new IllegalArgumentException("Channel error");
        }
                        
        long base = SIZE_TBM + SIZE_HEADER;
        for (int indiceLine=0 ; indiceLine<getHeight(0) ; indiceLine++, base+=SIZE_DATA) 
        {
            input.seek(base + fieldCoeff.offset);                     
            
            // Slope value.
            array[0] = (double)input.readUnsignedInt() / div1; 
            // Intercept value.
            array[1] = (double)input.readUnsignedInt() / div2; 
            coefficients.setElement(indiceLine, array);            
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
        return extractDateFromData(field, base);
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
        return field.getShort(input, base);
    }
    
    /**
     * Retourne une cha�ne de caract�re identifiant le satellite utilis� pour l'acquisition.
     * @return une cha�ne de caract�re identifiant le satellite utilis� pour l'acquisition.
     */
    public String getSpacecraft() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getTBM().get(Format.TBM_SPACECRAFT_ID);
        return field.getString(input, 0);
    }
    
    /**
     * Retourne la date de d�but de l'acquisition.
     * Return la date de d�but de l'acquisition.
     */
    public Date getStartTime() throws IOException 
    {
        final long base = SIZE_TBM;
        final ImageInputStream input = (FileImageInputStream)this.input;        
        final Field field = getHeader().get(Format.HEADER_START_TIME);
        return extractDateFromData(field, base);    
    }
    
    /**
     * Retourne la date extraite d'un <i>Data Record</i>.
     *
     * @param field     Champs � extraitre.
     * @param index     Index dans le flux.
     * @return la date extraite d'un <i>Data Record</i>.
     */
    protected Date extractDateFromData(final Field field, final long base) throws IOException
    {
        final ImageInputStream input = (FileImageInputStream)this.input;        
        return field.getDateFormatv3(input, base);
    }   
        
    /**
     * Retourne une liste de param�tres contenant les param�tres de calibration.
     *
     * @param channel   Canal d�sir�.
     * @return une liste de param�tres contenant les param�tres de calibration.
     */
    public ParameterList getCalibrationParameter(int channel) throws IOException
    {
        final String descriptor       = "AVHRR_AJ";
        final String[] paramNames     = {SLOPE_INTERCEPT_COEFFICIENT};
        final Class[]  paramClasses   = {CoefficientGrid.class};
        final Object[]  paramDefaults = {null};
        final ParameterList parameters = new ParameterListImpl(new ParameterListDescriptorImpl(descriptor,
                                                                                               paramNames,
                                                                                               paramClasses,
                                                                                               paramDefaults,
                                                                                               null));        
        parameters.setParameter(SLOPE_INTERCEPT_COEFFICIENT, getSlopeInterceptCoef(channel));
        return parameters;
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
                                 fieldTime = getData().get(Format.DATA_TIME);              
        final byte[] bufferMemoire = new byte[(getData().get(Format.DATA_EARTH_LOCATION)).size];                                
        final double altitude = extractAltitude()/1000; // altitude retournee en metre.

        for (int indiceLine=0 ; indiceLine<getHeight(imageIndex) ; indiceLine++) 
        {
            // Extraction de la date d'acquisition de l'enregistrement.
            grid.setTime(indiceLine, extractDateFromData(fieldTime, base));            
            grid.setAltitude(indiceLine, (float)altitude);            
 
            // Optimisation : ici les donnees sont lues directement dans le 
            // flux pour accelerer le traitement.   
            input.seek(base + fieldEarthLocalization.offset);
            input.readFully(bufferMemoire);            
            input.seek(base);
            bis = new DataInputStream((new BufferedInputStream(new ByteArrayInputStream(bufferMemoire))));
                    
            // Extraction des points de localisation latitude, longitude 
            // Format : 2 bytes Lat + 2 bytes Long par point.
            for (int i=0 ; i<ImageReaderN1B.NB_CONTROL_POINT_LINE ; i++) 
            {          
                final double latitude  = (double)bis.readShort()/128.0;
                final double longitude = (double)bis.readShort()/128.0;
                grid.setLocalizationPoint(i, indiceLine, longitude, latitude);                
            }                                         
            base += SIZE_DATA;
        }          
        grid.removeSingularities();
        assert grid.isMonotonic(false);
        return grid;        
    }    

    /**
     * Retourne l'altitude du satellite.
     * @return l'altitude du satellite.
     */
    private double extractAltitude() throws IOException 
    {
        final ImageInputStream input = (FileImageInputStream)this.input;
        final Field field = getHeader().get(Format.HEADER_AJ_CARTESIAN_ELEMENTS);
        final long base = SIZE_TBM;
        final double[] ord = new double[3];
        
        /* X position of satellite.
           Y position of satellite.
           Z position of satellite.
           X dot velocity of satellite (not used).
           Y dot velocity of satellite (not used).
           Z dot velocity of satellite (not used).       
         
           format : integer 4 byte. */        
        input.seek(base + field.offset);
        for (int index=0; index<ord.length ; index++)
            ord[index] = input.readInt()/1E4;       
        
        CoordinatePoint point = new CoordinatePoint(ord);
        point = getGeographicCoordinate(point);
        return point.getOrdinate(2);
    }    
    
    /**
     * Retourne les coordonn?es g?ographiques du satellite ? la date sp?cifi?e.
     * Cette m?thode obtient les coordonn?es g?ocentriques et les transforme en
     * coordonn?es g?ographiques.
     *
     * @param  date Date d?sir?e de la coordonn?e.
     * @return Coordonn?es g?ographiques du satellite ? la date sp?cifi?e.
     */    
    private CoordinatePoint getGeographicCoordinate(final CoordinatePoint point)
    {
        try
        {
            transformation.getMathTransform().transform(point, point);
            return point;
        }
        catch (TransformException exception)
        {
            // Should not happen
            IllegalStateException e = new IllegalStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
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
        
        final double latitude1 = field.getShort(input, base1)/128.0;        
        final double latitude2 = field.getShort(input, base2)/128.0;    
        
        return ((latitude1 < latitude2) ? SOUTH_TO_NORTH : NORTH_TO_SOUTH);            
    }
    
    /**
     * Retourne une description du canal.
     *
     * @param channel   Le canal d�sir�.
     * Retourne une description du canal.
     */
    public String toString(int channel) {
        if (channel < 0 ||channel >= this.getChannelsNumber())
            throw new IllegalArgumentException("This indexed channel doesn't exist.");
        return CHANNEL_DESCRIPTION[channel];
    }
    
    /**
     * Retourne le nombre de canaux disponibles.
     * @return le nombre de canaux disponibles.
     */
    public int getChannelsNumber() 
    {
        return 5;
    }    
}