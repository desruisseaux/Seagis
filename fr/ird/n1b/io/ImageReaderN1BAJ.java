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
 * Cette classe définit les méthodes spécifiques aux images N1B (level 1B) standard pour 
 * le format POD. Ce format est utilisé pour le stockage des  des données issues des 
 * satellites NOAA AJ. <BR><BR>
 *
 * Il est a noté qu'il est fréquent de trouver des images au format POD/AJ contenant
 * des données provenant de satellites KLM.
 *
 * @author Remi EVE
 * @version $Id$
 */
public class ImageReaderN1BAJ extends ImageReaderN1B 
{                                                                     
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
    
    /////////////////////////////////////////////////    
    /////////////////////////////////////////////////
    // Extraction des coefficients de calibration. //
    /////////////////////////////////////////////////
    /////////////////////////////////////////////////    
    /**
     * Retourne une liste de paramètres contenant les paramètres de calibration du
     * <CODE>channel</CODE> désiré.
     *
     * @param channel   Channel désiré.
     * @return une liste de paramètres contenant les paramètres de calibration du 
     *         <CODE>channel</CODE> désiré.
     */
    public ParameterList getCalibrationParameter(final Channel channel) throws IOException
    {        
        /* Temporairement, les valeurs THERMAL CALIBRATION COEFFICIENT seront celle
           recalculé par l'algorithme de calibration des données HRPT pour les satellites 
           KLM enregistré dans un fichier N1B au format POD. */
        final Satellite satellite = Satellite.get(metadata.getSpacecraft());
        if (satellite.isKLM())
        {
            final ParameterList paramInCalib = CalibrationKLM.getInputDefaultParameterList();
            paramInCalib.setParameter("SATELLITE", Satellite.get(metadata.getSpacecraft()));
            paramInCalib.setParameter("CHANNEL", channel);
            paramInCalib.setParameter("BACK SCAN", getBackScan(channel));
            paramInCalib.setParameter("SPACE DATA", getSpaceData(channel));
            paramInCalib.setParameter("TARGET TEMPERATURE DATA", getInternalTargetTemperatureData());
            CalibrationKLM.calibrate(paramInCalib);            
        }
        
        final String descriptor       = "AVHRR_AJ";
        final String[] paramNames     = {"SLOPE INTERCEPT COEFFICIENTS"};
        final Class[]  paramClasses   = {CoefficientGrid.class};
        final Object[]  paramDefaults = {null};
        final ParameterList parameters = new ParameterListImpl(
                                            new ParameterListDescriptorImpl(descriptor,
                                                                            paramNames,
                                                                            paramClasses,
                                                                            paramDefaults,
                                                                            null));        
        parameters.setParameter("SLOPE INTERCEPT COEFFICIENTS", 
                                getSlopeIntercept(channel));
        return parameters;
    }       

    /**
     * Retourne une grille contenant les coefficients de calibration du 
     * <code>channel</code>
     *
     * @param channel   Canal désiré.
     * @return une grille contenant les coefficients de calibration du 
     *         <code>channel</code>.
     */
    public CoefficientGrid getSlopeIntercept(final Channel channel) throws IOException 
    {
        /* A chaque enregistrement et pour chaque canal, deux coefficients sont 
           nécessaires : slope et intercept. La constante ci-dessous indique le nombre de 
           coefficients de calibration par canal et par ligne. */
        final int count = 2;                 
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double div1 = 2<<29;  
        final double div2 = 2<<21;
        final double[] array = new double[count];        
        final CoefficientGrid coefficients = new CoefficientGrid(getHeight(0), count);
        final Field fieldCoeff;
        
        if (channel.equals(Channel.CHANNEL_1))
            fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_1); 
        else if (channel.equals(Channel.CHANNEL_2))
            fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_2); 
        else if (channel.equals(Channel.CHANNEL_3))
            fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_3); 
        else if (channel.equals(Channel.CHANNEL_4))
            fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_4); 
        else if (channel.equals(Channel.CHANNEL_5))
            fieldCoeff = getData().get(Format.DATA_AJ_CALIBRATION_COEF_CHANNEL_5);                 
        else throw new IllegalArgumentException("Erreur de canal.");
                        
        long base = SIZE_TBM + SIZE_HEADER;
        for (int row=0 ; row<getHeight(0) ; row++, base+=SIZE_DATA) 
        {
            input.seek(base + fieldCoeff.offset);                                 
            array[0] = (double)input.readUnsignedInt() / div1; 
            array[1] = (double)input.readUnsignedInt() / div2; 
            coefficients.setElement(row, array);            
        }
        return coefficients;        
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
        
        /* Index des PRTs dans le bloc telemetry. */
        final int PRT1 = 17,
                  PRT2 = 18,
                  PRT3 = 19;
        
        final ImageInputStream input = (FileImageInputStream)this.input;
        final double[] array = new double[count] ;
        final CoefficientGrid grid = new CoefficientGrid(getHeight(0),count);
        final Field field = getData().get(Format.DATA_AJ_HRPT);
        long base = SIZE_TBM + SIZE_HEADER + field.offset;                               
        for(int row=0; row<getHeight(0); row++, base+=SIZE_DATA)
        {
            array[0] = getWordFromPacketData(input, base, PRT1);
            array[1] = getWordFromPacketData(input, base, PRT2);
            array[2] = getWordFromPacketData(input, base, PRT3);
            if (false) System.out.println(array[0] + "\t" + array[1] + "\t" + array[2]);
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
        final Field field    = getData().get(Format.DATA_AJ_HRPT);        
        
        final int startIndex;
        if (channel.equals(Channel.CHANNEL_3A) || 
            channel.equals(Channel.CHANNEL_3B) || 
            channel.equals(Channel.CHANNEL_3))
            startIndex = 22;
        else if (channel.equals(Channel.CHANNEL_4))
            startIndex = 23;
        else if (channel.equals(Channel.CHANNEL_5))
            startIndex = 24;
        else throw new IllegalArgumentException("Erreur de canal.");
        
        long base = SIZE_TBM + SIZE_HEADER + field.offset;                                       
        for (int row=0 ; row<getHeight(0) ; row++, base+=SIZE_DATA)
        {
            for (int index=0 ; index<numWord ; index++)
                array[index] = getWordFromPacketData(input, base, startIndex+index*3);
            if (false)
            {
                for (int i=0; i<array.length ; i++)
                    System.out.print(array[i] + "\t");
                System.out.println("");
            }
            grid.setElement(row, array);
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
        final CoefficientGrid grid   = new CoefficientGrid(getHeight(0), numWord); 
        final double[] array = new double[numWord] ;
        final Field field    = getData().get(Format.DATA_AJ_HRPT);        
        
        final int startIndex;
        if (channel.equals(Channel.CHANNEL_1))
            startIndex = 52;
        else if (channel.equals(Channel.CHANNEL_2))
            startIndex = 53;
        else if (channel.equals(Channel.CHANNEL_3A) || 
                 channel.equals(Channel.CHANNEL_3B) || 
                 channel.equals(Channel.CHANNEL_3))
            startIndex = 54;
        else if (channel.equals(Channel.CHANNEL_4))
            startIndex = 55;
        else if (channel.equals(Channel.CHANNEL_5))
            startIndex = 56;
        else throw new IllegalArgumentException("Erreur de canal.");
        
        long base = SIZE_TBM + SIZE_HEADER + field.offset;                                       
        for (int row=0 ; row<getHeight(0) ; row++, base+=SIZE_DATA)
        {
            for (int index=0 ; index<numWord ; index++)
                array[index] = getWordFromPacketData(input, base, startIndex+index*5);
            if (false)
            {
                for (int i=0; i<array.length ; i++)
                    System.out.print(array[i] + "\t");
                System.out.println("");
            }
            grid.setElement(row, array);            
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
        final Satellite satellite = Satellite.get(metadata.getSpacecraft());
        if (satellite.isKLM())
        {        
            if (satellite.getID() == 15)            
            {
                final double[][] array  = {{276.355, 5.562/1.0E2, -1.590/1.0E5, 2.486/1.0E8, -1.199/1.0E11, 0},
                                           {276.142, 5.605/1.0E2, -1.707/1.0E5, 2.595/1.0E8, -1.224/1.0E11, 0},
                                           {275.996, 5.486/1.0E2, -1.223/1.0E5, 1.862/1.0E8, -0.853/1.0E11, 0},
                                           {276.132, 5.494/1.0E2, -1.344/1.0E5, 2.112/1.0E8, -1.001/1.0E11, 0}};            
                return array;
            }
            else if (satellite.getID() == 16)            
            {
                final double[][] array  = {{276.355, 5.562/1.0E2, -1.590/1.0E5, 2.486/1.0E8, -1.199/1.0E11, 0},
                                           {276.142, 5.605/1.0E2, -1.707/1.0E5, 2.595/1.0E8, -1.224/1.0E11, 0},
                                           {275.996, 5.486/1.0E2, -1.223/1.0E5, 1.862/1.0E8, -0.853/1.0E11, 0},
                                           {276.132, 5.494/1.0E2, -1.344/1.0E5, 2.112/1.0E8, -1.001/1.0E11, 0}};            
                return array;            
            } else if (satellite.getID() == 17)            
            {
                final double[][] array  = {{276.628, 0.05098, 1.371/1.0E6, 0, 0, 0},
                                           {276.538, 0.05098, 1.371/1.0E6, 0, 0, 0},
                                           {276.761, 0.05097, 1.369/1.0E6, 0, 0, 0},
                                           {276.660, 0.05100, 1.348/1.0E6, 0, 0, 0}};            
                return array;
            }
            else throw new IllegalArgumentException("Aucun coefficient défini pour" +
                                                    " ce satellite.");                                                             
        }      
        else throw new IllegalArgumentException("Ces coefficients ne sont pas disponibles " + 
                                                "pour ce type de satellite.");
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
        while(!iterator.finishedLines())       
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
     * Extraction des <i>packed-video data</i> d'une bande.
     *
     * @param iterator      Un itérateur sur une RenderedImage.
     * @param param         Paramètres de l'image.
     * @exception   IOException si une input ou output exception survient.
     */
    public void extractPackedVideoData_(final WritableRectIter   iterateur, 
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

                // Pour optimiser le temps de lecture, les données sont lues en direct 
                // dans le fichier.
                input.seek(base + fieldPackedVideo.offset);                                   
                compteur =0;

                // Mise en mémoire d'un data record.
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
     * Retourne une image contenant les <i>packed-video data</i> d'un canal. L'image
     * retournée contiendra uniquement les informations du canal désiré. 
     *
     * @param imageIndex    Index de l'image à extraire.
     * @param channel       Canal désiré.
     * @return une image contenant les <i>packed-video data</i> d'un canal.
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
        return extractDateFromData(field, base);
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
        return field.getShort(input, base);
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
        return extractDateFromData(field, base);    
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
        
        final double latitude1 = field.getShort(input, base1)/128.0;        
        final double latitude2 = field.getShort(input, base2)/128.0;    
        
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
    protected Date extractDateFromData(final Field field, final long base) throws IOException
    {
        final ImageInputStream input = (FileImageInputStream)this.input;        
        return field.getDateFormatv3(input, base);
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
     * Retourne la bande contenant les données du canal désiré.
     *
     * @param channel   Le canal désiré.
     * @return la bande contenant les données du canal désiré.
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
        else if (channel.equals(Channel.CHANNEL_4))            
            band = 3;
        else if (channel.equals(Channel.CHANNEL_5))                        
            band = 4;
        else throw new IllegalArgumentException("Canal inexistant.");
        return band;
    }    
}