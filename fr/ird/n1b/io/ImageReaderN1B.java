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


/**
 * D?codeur de fichier au format N1B pour les ensembles de donn?es
 * <cite>Local Area Coverage</cite> (LAC) et
 * <cite>High Resolution Picture Transmission</cite> (HRPT).
 *
 * <h3>Format des fichiers N1B :</h3>
 * <UL>
 *  <LI>TBM de 122 octets</LI>
 *  <LI>En-t?te de 14800 octets (contient des informations g?n?rales sur les donn?es)</LI>
 *  <LI><var>n</var> enregistrements de 14800 octets chacun (contiennent les donnees vid?o acquises)</LI>
 * </UL>
 *
 *
 * <h3>Les points de contr?les :</h3>
 * Pour une ligne scann?e par le satellite, nous obtenons 2048 valeurs repr?sentant 2048 mesures
 * contigues r?alis?es par le capteur. Pour pouvoir situer ces points lors d'une projection sur
 * une carte, nous disposons des points de contr?les. Ces points nous permettent de conna?tre les
 * coordonn?es pr?cises de certains points mesur?s et d'approcher les coordonn?es des autres points
 * par une simple interpolation lineaire. Sur 2048 mesures r?alisees, nous connaissons 51 points de
 * controles repartis comme suit :
 * <UL>
 *   <LI>Le premier point de contr?le nous renseigne sur la 25ieme mesure obtenue par le capteur.</LI>
 *   <LI>Ensuite, un point de controle toutes les 40 mesures.</LI>
 * </UL>
 * Les informations disponibles pour chaque point de controle sont :
 * <UL>
 *   <LI>latitude</LI>
 *   <LI>longitude</LI>
 *   <LI>angle zenithal</LI>
 * </UL>
 *
 *
 * <h3>Les mesures realisees par les capteurs ou "packed video data" : </h3>
 * Les capteurs sont au nombre de 5 (5 bandes de donn?es) et les donn?es 
 * acquises sont stock?es selon le format suivant :
 * <br>
 *      Word 1 (4 octets) :
 *      <UL>
 *        <LI>Bits  0 ?  9 : bande 3 du point 1</LI>
 *        <LI>Bits 10 a 19 : bande 2 du point 1</LI>
 *        <LI>Bits 20 a 29 : bande 1 du point 1</LI>
 *      </UL>
 *      Word 2 :
 *      <UL>
 *        <LI>Bits  0 a  9 : bande 1 du point 2</LI>
 *        <LI>Bits 10 a 19 : bande 5 du point 1</LI>
 *        <LI>Bits 20 a 29 : bande 4 du point 1</LI>
 *      </UL>
 *      <UL>
 *      Word 3 :
 *        <LI>0 a 9 : bande 4 du point 2</LI>
 *        <LI>10 a 19 : bande 3 du point 2</LI>
 *        <LI>20 a 29 : bande 2 du point 2</LI>
 *      </UL>
 *      <UL>
 *      Word 4 (4 octets) :
 *        <LI>0 a 9 : bande 2 du point 1</LI>
 *        <LI>10 a 19 : bande 1 du point 1</LI>
 *        <LI>20 a 29 : bande 5 du point 1</LI>
 *      </UL>
 *      <UL>
 *      Word 5 :
 *        <LI>0 a 9 : bande 5 du point 2</LI>
 *        <LI>10 a 19 : bande 4 du point 1</LI>
 *        <LI>20 a 29 : bande 3 du point 1</LI>
 *      </UL>
 *      <UL>
 *      Word 6 :    
 *          <LI>0 a 9 : bande 3 du point 2</LI>
 *          <LI>10 a 19 : bande 2 du point 2</LI>
 *          <LI>20 a 29 : bande 1 du point 2</LI>
 *      </UL>
 *
 * On constate que tous les 5 word de 4 bytes, on a un cycle. Chacune des 
 * donnees video a une taille de 10 bits. Il est necessaire de lire 4 bytes 
 * pour en extraire ensuite la valeur mesuree.
 * <BR>Pour plus d'informations concernant le format N1B, vous pouvez visiter
 * la page <a href="http://www.sat.dundee.ac.uk/noaa1b.html">NOAA Level 1B File Format (Dundee)</a>
 *
 *
 * TODO : 
 * <UL>
 *  <LI>Le format N1B étant plus ou moins respecté, la méthode <a href="get(final FileImageInputStream input)">
 *      get(final FileImageInputStream input)</a> retourne l'ImageReaderN1B au bon format (AJ ou 
 *      POD, KLM, ...). Cependant, il est trés difficile de reconnaître les formats. Il serait cependant 
 *      judicieux de trouver des critères plus fiables que ceux utilisés permettant de distinguer les 
 *      différents formats avec certitude.</LI>
 *  <LI>Il serait utile d'utiliser les nouvelles classes NIO pour gérer les flux d'entrée/sortie afin 
 *      d'améliorer les performances.</LI>
 * </UL>
 *
 *
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaux
 */
public abstract class ImageReaderN1B extends ImageReader
{    
    /** Nombre de points de localisation par ligne. */
    public static final int NB_CONTROL_POINT_LINE = 51;
    
    /** Nombre de pixels par ligne. */
    public static final int NB_PIXEL_LINE = 2048;
    
    /** Offset du premier point de localisation en x. */
    public static final int OFFSET_FIRST_CONTROL_POINT  = 25;
    
    /** 
     * Intervalle entre deux points de localisation consécutifs appartenant 
     * à une même ligne. 
     */
    public static final int INTERVAL_NEXT_CONTROL_POINT = 40;

    /** Identifiant de la direction du satellite durant l'acquisition. */
    public static final int SOUTH_TO_NORTH = 0,
                            NORTH_TO_SOUTH = 1;
        
    /**
     * M?ta donn?es lues. Cet objet est initialement nul, et
     * construit une fois la lecture de l'en-t?te effectu?e.
     */
    protected Metadata metadata;
    
    /** Format du fichier. */
    private int format;
    
    /** Format du TBM. */
    private final Format TBM;

    /** Format du Header Record. */    
    private final Format HEADER;

    /** Format d'un Data Record. */
    private final Format DATA;    

    /** 
     * Constructeur du decodeur N1B. 
     *
     * @param provider the service provider that is invoking this constructor,
     *        or <code>null</code> if none.
     * @param format The format of data N1B (KLM or AJ)
     */
    protected ImageReaderN1B(final ImageReaderSpi provider, final int format) 
    {
        super(provider);
        this.format = format;
        TBM    = Format.getTBM(format);
        HEADER = Format.getHeader(format);
        DATA   = Format.getData(format);
    }

    /**
     * Extraction d'une grille contenant l'ensemble des points de localisation. 
     *
     * @return      Une grille de points de localisation.
     * @exception   IOException si une input ou output exception survient.
     */
    public abstract LocalizationGridN1B getGridLocalization() throws IOException;

    /**
     * Extraction des <i>packed-video data</i> d'un canal.
     *
     * @param iterator      Un itérateur sur une RenderedImage.
     * @param param         Paramètres de l'image.
     * @exception   IOException si une input ou output exception survient.
     */
    public abstract void extractPackedVideoData(final WritableRectIter  iterator, 
                                                final ImageReadParam    param) 
                                                                throws IOException;
    
    /**
     * Retourne une chaîne de caractère identifiant le satellite utilisé pour l'acquisition.
     * @return une chaîne de caractère identifiant le satellite utilisé pour l'acquisition.
     */
    protected abstract String getSpacecraft() throws IOException;
    
    /**
     * Retourne la date de début de l'acquisition.
     * Return la date de début de l'acquisition.
     */
    protected abstract Date getStartTime() throws IOException;

    /**
     * Retourne la date de fin de l'acquisition.
     * @return la date de fin de l'acquisition.
     */
    protected abstract Date getEndTime() throws IOException;
    
    /**
     * Retourne la hauteur de l'image.
     * @return la hauteur de l'image.
     */
    public abstract int getHeight() throws IOException;
    
    /**
     * Retourne la direction du satellite lors de l'acquisition <CODE>NORTH_TO_SOUTH</CODE>
     * ou <CODE>SOUTH_TO_NORTH</CODE>.
     * @return la direction du satellite lors de l'acquisition <CODE>NORTH_TO_SOUTH</CODE>
     * ou <CODE>SOUTH_TO_NORTH</CODE>.
     */
    public abstract int getDirection() throws IOException;    

    /**
     * Retourne la date extraite d'un <i>Data Record</i>.
     *
     * @param field     Champs à extraitre.
     * @param index     Index dans le flux.
     * @return la date extraite d'un <i>Data Record</i>.
     */
    protected abstract Date extractDateFromData(final Field field, 
                                                final long  base) throws IOException;    
    
    /**
     * Retourne une liste de paramètres contenant les paramètres de calibration.
     *
     * @param channel   Canal désiré.
     * @return une liste de paramètres contenant les paramètres de calibration.
     */
    public abstract ParameterList getCalibrationParameter(final int channel) 
                                                                throws IOException;

    /**
     * Retourne le nombre de canaux disponibles.
     * @return le nombre de canaux disponibles.
     */
    public abstract int getChannelsNumber();

    /**
     * Retourne une description du canal.
     *
     * @param channel   Le canal désiré.
     * Retourne une description du canal.
     */
    public abstract String toString(final int channel);

    /**
     * Sets the input source to use to the given ImageInputStream or other Object.
     *
     * @param input the ImageInputStream or other Object to use for future decoding.
     * @param seekForwardOnly if true, images and metadata may only be read in 
     *                        ascending order from this input source.
     * @param ignoreMetadata if true, metadata may be ignored during reads.
     */
    public void setInput(final Object  input,
                         final boolean seekForwardOnly,
                         final boolean ignoreMetadata) 
    {        
        super.setInput(input, seekForwardOnly, ignoreMetadata);        
        try 
        {
            parseMetadata();
        }
        catch (IOException ioe) 
        {
            throw new IllegalArgumentException("An exception has occured when extracting Metadata.");            
        }
   }
                          
    /**
     * Implémentation de la méthode <CODE>getHeight(final int imageIndex)</CODE> de la 
     * classe abstraite <COCE>ImageReader</CODE>.
     *
     * @param imageIndex    Index of the image to be queried.
     * @exception   IOException if an error occurs reading the height information 
     *              from the input source.
     */
    public int getHeight(final int imageIndex) throws IOException 
    {
        checkIndex(imageIndex);        
        return ((Integer)metadata.get(Metadata.HEIGHT)).intValue();
    }
    
    /**
     * Implémentation de la méthode <CODE>getImageMetadata(int imageIndex)</CODE> de la 
     * classe abstraite <CODE>ImageReader</CODE>.
     *
     * @param       imageIndex the index of the image to be queried.
     * @return      Returns an IIOMetadata object containing metadata associated with 
     *              the given image, or null if the reader does not support reading 
     *              metadata, is set to ignore metadata, or if no metadata is available.
     * @exception   IOException if an error occurs during reading.
     */    
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException 
    {
        checkIndex(imageIndex);        
        return metadata;
    }
        
    /**
     * Implémentation de la méthode <CODE>getImageTypes(final int imageIndex)</CODE> issue 
     * de la classe abstraite <CODE>ImageReader</CODE>.
     *
     * @param       imageIndex the index of the image to be retrieved.
     * @return      Returns an Iterator containing possible image types to which the 
     *              given image may be decoded, in the form of ImageTypeSpecifierss.  
     *              At least one legal image type will be returned.
     * @exception   IOException if an error occurs reading the format information from the 
     *              input source.
     */    
    public Iterator getImageTypes(final int imageIndex) throws IOException 
    {
        checkIndex(imageIndex);        
        List liste = new ArrayList();
        liste.add(ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, false));
        
        return liste.iterator();
    }    

    /**
     * Implémentation de la méthode <CODE>getNumImages(final boolean allowSearch)</CODE> 
     * issue de la classe abstraite <CODE>ImageReader</CODE>.
     *
     * @param       allowSearch if true, the true number of images will be returned 
     *              even if a search is required.  If false, the reader may 
                    return -1 without performing the search.
     * @return      Returns the number of images, not including thumbnails, available
     *              from the current input source.
     *@exception    IOException if an error occurs reading the information from the input source.
     */
    public int getNumImages(final boolean allowSearch) throws IOException 
    {
        return 1;
    }
    
    /**
     * Implémentation de la méthode <CODE>getStreamMetadata()</CODE> issue de la classe 
     * abstraite <CODE>ImageReader</CODE>.
     *
     * @return      Returns an IIOMetadata object representing the metadata associated 
     *              with the input source as a whole (i.e., not associated with 
     *              any particular image), or null if the reader does not support 
     *              reading metadata, is set to ignore metadata, or if no 
     *              metadata is available.
     * @exception   IOException if an error occurs during reading.
     */    
    public IIOMetadata getStreamMetadata() throws IOException     
    {
        return null;
    }
    
    /**
     * Implémentation de la méthode <CODE>getWidth(final int imageIndex)</CODE> de 
     * la classe abstraite <CODE>ImageReader</CODE>.
     *
     * @return      Returns the width in pixels of the given image within the input
     *              source.
     * @exception   IOException if an error occurs reading the width information from the 
     *              input source.
     */    
    public int getWidth(final int imageIndex) throws IOException 
    {
        checkIndex(imageIndex);        
        return NB_PIXEL_LINE;
    }
    
    /**    
     * Retourne une image contenant les <i>packed-video data</i> d'un canal. 
     *
     * @param imageIndex    Index de l'image à extraire.
     * @param param         Paramètres de l'extraction.
     * @return      une image contenant les <i>packed-video data</i> d'un canal. 
     * @exception   IOException si une input ou output exception survient.
     */            
    public BufferedImage read(final int imageIndex, final ImageReadParam param) 
                                                                    throws IOException 
    {
        checkIndex(imageIndex);        
        BufferedImage image = new BufferedImage(NB_PIXEL_LINE,
                                                getHeight(imageIndex), 
                                                BufferedImage.TYPE_USHORT_GRAY);        
        WritableRectIter rif = RectIterFactory.createWritable(image, null);
        extractPackedVideoData(rif, param);
        return image;
    }        
        
    /**
     * Retourne une image contenant les <i>packed-video data</i> d'un canal. 
     *
     * @param imageIndex    Index de l'image à extraire.
     * @param param         Paramètres de l'extraction.
     * @return      une image contenant les <i>packed-video data</i> d'un canal. 
     * @exception   IOException si une input ou output exception survient.
     */
    public RenderedImage readAsRenderedImage(final int            imageIndex, 
                                             final ImageReadParam param) throws IOException
    {        
        return new TiledImage(read(imageIndex, param), 512, 512);
    }
    
    /**
     * Vérifie l'index de l'image.
     * @exception IndexOutOfBoundsException Si l'index de l'image n'est pas correcte.
     */
    private void checkIndex(final int imageIndex) throws IndexOutOfBoundsException
    {
        if (imageIndex != 0)
            throw new IndexOutOfBoundsException("bad index");
    }
    
    /** 
     * Convertie un tableau de byte en chaîne de caractères.
     *
     * @param   b tableau de byte à convertir.
     * @return  une chaine de caractères.
     **/
    private static String byteArrayToString(final byte[] b) 
    {
        StringBuffer s = new StringBuffer();
        
        for (int i=0 ; i<b.length ; i++) 
            s.append(b[i]);                
        return s.toString();
    }
    
    /** 
     * Convertie un tableau de byte en double.
     *
     * @param   b tableau de byte à transformer.
     * @return  un double.
     */
    private static double byteArrayToDouble(final byte[] b) 
    {
        return Double.parseDouble(byteArrayToString(b));                        
    }    
    
    /** 
     * Convertie un tableau de byte en long.
     *
     * @param   b tableau de byte à transformer.
     * @return  un long.
     */
    private static long byteArrayToLong(final byte[] b) 
    {
        return Long.parseLong(byteArrayToString(b));        
    }        
    
    /** 
     * Convertie un tableau de byte en short.
     *
     * @param   b tableau de byte à transformer.
     * @return  un short.
     */
    public static short byteArrayToShort(final byte[] b) 
    {
        return Short.parseShort(byteArrayToString(b));        
    }           
    
    /** 
     * Convertie un tableau de byte en float.
     *
     * @param   b tableau de byte à transformer.
     * @return  un float.
     */
    private static float byteArrayToFloat(final byte[] b) 
    {
        return Float.parseFloat(byteArrayToString(b));        
    }           

    /** 
     * Convertie un tableau de byte en int.
     *
     * @param   b tableau de byte à transformer.
     * @return  un int.
     */
    public static int byteArrayToInt(final byte[] b) 
    {
        return Integer.parseInt(byteArrayToString(b));        
    }               
    
    /**
     * Retourne un ImageReaderN1B.
     *
     * @param input  Flux contenant des donnees au format N1B.
     * @return un imageReader capable d'extraire les donnees du flux.
     */
    public static ImageReader get(final FileImageInputStream input) throws IOException
    {
        if (input == null)
            throw new IllegalArgumentException("Input stream is null.");
        
        // test début du fichier.
        final byte[] buffer = new byte[14];
        ImageReader reader = null;        
        input.readFully(buffer);         
        boolean flagAJStd     = true,
                flagAJCanaries= true,
                flagKLMStd    = true;
        for (int i=0 ;i<buffer.length ; i++)       
        {
            if (buffer[i] == (byte)0)
                flagAJStd = (flagAJStd == false) ? false : true;
            else
                flagAJStd = false;
            
            if (buffer[i] == (byte)32)
                flagAJCanaries = (flagAJCanaries == false) ? false : true;
            else
                flagAJCanaries = false;            

            if (buffer[i] == (byte)48)
                flagKLMStd = (flagKLMStd == false) ? false : true;
            else
                flagKLMStd = false;            
        }

        if ((flagAJStd   && flagAJCanaries) || 
            (flagAJStd   && flagKLMStd)     ||
            (flagKLMStd && flagAJCanaries)  ||
            (!flagAJStd && !flagAJCanaries && !flagKLMStd))
            throw new IllegalArgumentException("This format is not identified.");
        
        if (flagAJStd)     
            reader = new ImageReaderN1BAJ(null);   
        else if (flagAJCanaries)     
            reader = new ImageReaderN1BAJCanaries(null);   
        else if (flagKLMStd)
            reader = new ImageReaderN1BKLM(null);           
        reader.setInput(input);        
        return reader;
    }
        
    /**
     * Lecture des principales informations du Header et du TBM.
     * @exception   IOException si une input ou output exception survient.
     */
    private void parseMetadata() throws IOException 
    {
        metadata = new Metadata();
        metadata.put(Metadata.SPACECRAFT, getSpacecraft());        
        metadata.put(Metadata.START_TIME, getStartTime());
        metadata.put(Metadata.HEIGHT,     getHeight());
        metadata.put(Metadata.END_TIME,   getEndTime());        
    }    

    /**
     * Retourne le format <CODE>Format.FORMAT_AJ</CODE> ou <CODE>Format.FORMAT_KLM</CODE> 
     * du fichier.
     * @return le format  <CODE>Format.FORMAT_AJ</CODE> ou <CODE>Format.FORMAT_KLM</CODE> 
     * du fichier.
     */
    public int getFormat() 
    {
        return format;
    }    
    
    /** 
     * Retourne le format du <i>TBM</i>.
     * @return le format du <i>TBM</i>. 
     */
    protected Format getTBM()
    {
        return TBM;
    }

    /** 
     * Retourne le format du <i>Header</i>. 
     * @return le format du <i>Header</i>. 
     */        
    protected Format getHeader()
    {
        return HEADER;
    }

    /** 
     * Retourne le format d'un <i>Data Record</i>. 
     * @return le format d'un <i>Data Record</i>. 
     */
    protected Format getData()
    {
        return DATA;    
    }   
}