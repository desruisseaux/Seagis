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
    /** Nombre de points de controles par ligne. */
    public static final int NB_CONTROL_POINT_LINE       = 51;
    
    /** Nombre de pixels par ligne ou passe du satellite. */
    public static final int NB_PIXEL_LINE               = 2048;
    
    /** Offset du premier point de controle. */
    public static final int OFFSET_FIRST_CONTROL_POINT  = 25;
    
    /** Interval entre deux points de controles. */
    public static final int INTERVAL_NEXT_CONTROL_POINT = 40;

    /**
     * Identifiant de la direction du satellite durant l'acqisition.
     */
    public static final int SOUTH_TO_NORTH = 0,
                            NORTH_TO_SOUTH = 1;
        
    /**
     * M?ta donn?es lues. Cet objet est initialement nul, et
     * construit une fois la lecture de l'en-t?te effectu?e.
     */
    protected Metadata metadata;
    
    /**
     * Memorise le format de donnees. 
     */
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
            System.out.println("An exception has occured when extracting Metadata.");            
        }
   }
                       
    /**
     * Extraction des informations relatives au points de controles. Les donnees
     * extraites sont stockees dans matrixCP et retournees.
     *
     * @return      la matrice de points de controles.
     * @exception   IOException si une input ou output exception survient.
     */
    public abstract LocalizationGridN1B getGridLocalization() throws IOException;
   
    /**
     * Extraction des packed video data ou mesures realisees par l'un des
     * capteurs.
     *
     * @param       iterateur un iterateur sur une RenderedImage.
     * @param       param parametres de l'image.
     * @exception   IOException si une input ou output exception survient.
     */
    public abstract void extractPackedVideoData(final WritableRectIter iterateur, 
                                                final ImageReadParam param) throws IOException;

    /**
     * Implementation de la methode getHeight issue de la classe ImageReader.
     *
     * @param       imageIndex the index of the image to be queried.
     * @exception   IOException if an error occurs reading the height information 
     *              from the input source.
     */
    public int getHeight(final int imageIndex) throws IOException 
    {
        checkIndex(imageIndex);        
        return ((Integer)metadata.get(Metadata.HEIGHT)).intValue();
    }

    
    /**
     * Implementation de la methode getImageMetadata issue de la classe ImageReader.
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
     * Implementation de la methode getImageTypes issue de la classe ImageReader.
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
     * Implementation de la methode getNumImages issue de la classe ImageReader.
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
     * Implementation de la methode getStreamMetadata issue de la classe ImageReader.
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
     * Implementation de la methode getWidth issue de la classe ImageReader.
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
     * Extraction des mesures realisees par un capteur. 5 canaux sont disponibles. 
     * Chacun de ces canaux offre 2048 mesures par ligne scannee. 
     *
     * @param       imageIndex l'index de l'image a extraire.
     * @param       param les parametres de l'extraction.
     * @return      les mesures extraites sous la forme d'une image.
     * @exception   IOException si une input ou output exception survient.
     */            
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException 
    {
        checkIndex(imageIndex);        
        BufferedImage image = new BufferedImage(NB_PIXEL_LINE,getHeight(imageIndex), BufferedImage.TYPE_USHORT_GRAY);        
        Rectangle rec = null;
        WritableRectIter rif = RectIterFactory.createWritable(image,null);
        extractPackedVideoData(rif, param);

        return image;
    }        
        
    /**
     * Retourne une image en tuile.
     *
     * @param       imageIndex l'index de l'image a extraire.
     * @param       param les parametres de l'extraction.
     * @return      une image en tuile.  
     * @exception   IOException si une input ou output exception survient.
     */
    public RenderedImage readAsRenderedImage(final int imageIndex, final ImageReadParam param) 
                                                                                throws IOException
    {        
        return new TiledImage(read(imageIndex, param), 512, 512);
    }
    
    /**
     * Verifie que l'index de l'image est correcte.
     *
     * @exception IndexOutOfBoundsException Si l'index de l'image n'est pas correcte.
     */
    private void checkIndex(final int imageIndex) throws IndexOutOfBoundsException
    {
        if (imageIndex != 0)
            throw new IndexOutOfBoundsException("bad index");
    }
    
    /** 
     * Convertie un tableau de byte en chaine de caracteres.
     *
     * @param   b tableau de byte a transformer.
     * @return  une chaine de caracteres equivalente au tableau de byte.
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
     * @param   b tableau de byte a transformer.
     * @return  un double equivalent au tableau de byte.
     */
    private static double byteArrayToDouble(final byte[] b) 
    {
        return Double.parseDouble(byteArrayToString(b));                        
    }    
    
    /** 
     * Convertie un tableau de byte en long.
     *
     * @param   b tableau de byte a transformer.
     * @return  un long equivalent au tableau de byte.
     */
    private static long byteArrayToLong(final byte[] b) 
    {
        return Long.parseLong(byteArrayToString(b));        
    }        
    
    /** 
     * Convertie un tableau de byte en short.
     *
     * @param   b tableau de byte a transformer.
     * @return  un short equivalent au tableau de byte.
     */
    public static short byteArrayToShort(final byte[] b) 
    {
        return Short.parseShort(byteArrayToString(b));        
    }           
    
    /** 
     * Convertie un tableau de byte en float.
     *
     * @param   b tableau de byte a transformer.
     * @return  un float equivalent au tableau de byte.
     */
    private static float byteArrayToFloat(final byte[] b) 
    {
        return Float.parseFloat(byteArrayToString(b));        
    }           

    /** 
     * Convertie un tableau de byte en int.
     *
     * @param   b tableau de byte a transformer.
     * @return  un int equivalent au tableau de byte.
     */
    public static int byteArrayToInt(final byte[] b) 
    {
        return Integer.parseInt(byteArrayToString(b));        
    }               
    
    /**
     * Retourne un ImageReaderN1B.
     *
     * Note : Ce code est un peu du bricolage car il n'est pas facile de determiner le format du fichier 
     *        N1B avec certitude. 
     *
     * @param input  Flux contenant des donnees au format N1B.
     * @return un imageReader capable d'extraire les donnees du flux.
     */
    public static ImageReader get(final FileImageInputStream input) throws IOException
    {
        if (input == null)
            throw new IllegalArgumentException("Input stream is null.");
        
        // on test l'entete du fichier (TBM ou ARS).
        final byte[] buffer = new byte[14];
        ImageReader reader = null;        
        input.readFully(buffer);         

        // test entete du fichier.
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
     *
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
     * Return the string that identify the satellite used for acquisition of data.
     * 
     * Example : "NM", "NK", "NH", ...
     *
     * @return the string that identify the satellite used for acquisition of data.
     */
    protected abstract String getSpacecraft() throws IOException;
    
    /**
     * Return the start time of data acquisition.
     */
    protected abstract Date getStartTime() throws IOException;

    /**
     * Return the end time of data acquisition.
     *
     * @return the end time of data acquisition.
     */
    protected abstract Date getEndTime() throws IOException;
    
    /**
     * Return the height of the image.
     *
     * @return the height of the image.
     */
    public abstract int getHeight() throws IOException;
    
    /**
     * Retourne la direction du satellite lors de l'acquisition (nord-sud) ou (sud-nord).
     *
     * @return la direction du satellite lors de l'acquisition (nord-sud) ou (sud-nord).
     */
    public abstract int getDirection() throws IOException;    

    /**
     * Return a date using the adequat method for extracting date in data record. This method is 
     * necessary because the format used for representing a date is different for noaa klm, noaa aj,
     * ...
     *
     * @param field the field to extract.
     * @param index in stream.
     * @return the date.
     */
    protected abstract Date extractDateFromData(final Field field, final long base) 
                                                              throws IOException;    
    
    /**
     * Extract paramters from file and return them.
     *
     * @param channel The channel.
     */
    public abstract ParameterList getCalibrationParameter(final int channel) throws IOException;

    /**
     * Retourne le nombre de canaux disponibles.
     *
     * Retourne le nombre de canaux disponibles.
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
     * Retourne le format (FORMAT_AJ or FORMAT_KLM) du fichier.
     *
     * @return le format (FORMAT_AJ or FORMAT_KLM) du fichier.
     */
    public int getFormat() 
    {
        return format;
    }    
    
    /** 
     * Retourne le format du TBM.
     * 
     * @return le format du TBM. 
     */
    protected Format getTBM()
    {
        return TBM;
    }

    /** 
     * Retourne le format du Header Record. 
     *
     * @return le format du Header Record. 
     */        
    protected Format getHeader()
    {
        return HEADER;
    }

    /** 
     * Retourne le format d'un Data Record. 
     *
     * @return le format d'un Data Record. 
     */
    protected Format getData()
    {
        return DATA;    
    }   
}