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
package fr.ird.n1b.io;

// J2SE dependencies
import org.w3c.dom.Node;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import javax.media.jai.ParameterList;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.stream.FileImageInputStream;

import java.io.IOException;
import javax.imageio.metadata.IIOInvalidTreeException;

// SEAGIS.
import fr.ird.util.CoefficientGrid;

/**
 * Recupère les meta données contenues dans les fichiers
 * au format level N1B sous forme d'une arborescence XML.
 *
 * @version $Id$
 * @author Remi EVE
 */
public final class Metadata extends IIOMetadata 
{
    /**
     * ImageReaderN1B auquel est associé cet objet Metadata. Cette référence
     * est nécéssaire car les metadatas sont trop nombreuses dans le format N1B ou 
     * level 1B pour être toutes mise en mémoire. Cette référence permettra d'extraire
     * les metadatas nécessitant beaucoup d'espace mémoire à la volée.
     */
    private ImageReaderN1B reader;
    
    /** 
     * Mots clés des méta-données contenues dans l'arborescence XML.
     * Ces identifiants sont associés à une valeur dans les méta-données.
     */
    public static final String FORMAT         = "FORMAT",
                               START_TIME     = "START TIME",
                               END_TIME       = "END TIME",
                               SPACECRAFT     = "SPACECRAFT",
                               DIRECTION      = "DIRECTION",
                               HEIGHT         = "HEIGHT";

    /** Keyword/value pairs. */
    private final Map<String,Object> data = new HashMap<String,Object>();
    
    /**
     * Construit un ensemble de méta-données initialement vide.
     *
     * @param reader    L'image associée a ce metadata.
     */
    public Metadata(final ImageReaderN1B reader) {
        super(false,                          // standard metadata format (not) supported
              MetadataFormat.ROOT_NAME,       // native metadata format name
              "fr.ird.io.n1b.MetadataFormat", // native metadata format class name
              null,                           // extra metadata format names
              null);                          // extra metadata format class names
        this.reader= reader;
    }
    
    /**
     * Informe la classe que l'ImageReader va être détruit.
     */
    public void dispose()
    {
        this.reader = null;
    }
    
    /**
     * renvoit le format des méta-données utilisé. Il s'agit de notre propre
     * format, "fr_ird_N1B_1.0". Ce sera la seule valeur de <code>formatName</code>
     * acceptée.
     *
     * @param formatName The desired metadata format.
     * @return an IIOMetadataFormat object.
     */
    public IIOMetadataFormat getMetadataFormat(final String formatName) 
    {        
        if (!formatName.equalsIgnoreCase(nativeMetadataFormatName))
        {
            throw new IllegalArgumentException(formatName);
        }
        return MetadataFormat.getDefaultInstance();
    }
    
    /**
     * Renvoit les metadonnées sous forme d'arborecence XML.
     * Le parametre <code>formatName</code> doit etre egal à
     * <code>"fr_ird_N1B_1.0"</code>.
     *
     * @param  formatName The desired metadata format.
     * @return Metadata as an XML tree.
     */
    public Node getAsTree(final String formatName) 
    {
        if (!formatName.equals(nativeMetadataFormatName)) 
        {
            throw new IllegalArgumentException(formatName);
        }
        
        // Create a root node
        final IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);
        
        // Add a child to the root node for each keyword/value pair
        for (final Iterator<Map.Entry<String,Object>> it=data.entrySet().iterator(); it.hasNext();) 
        {
            final Map.Entry<String,Object> entry = it.next();
            final IIOMetadataNode node = new IIOMetadataNode(MetadataFormat.ATTRIBUTE_NAME);
            node.setAttribute("name",  entry.getKey());
            node.setAttribute("value", entry.getValue().toString());
            root.appendChild(node);
        }
        return root;
    }
    
    /**
     * Returns true if this object does not support the {@link #mergeTree},
     * {@link #setFromTree}, and {@link #reset} methods.
     */
    public boolean isReadOnly()
    {
        return true;
    }
    
    /**
     * Alters the internal state of this IIOMetadata object from a tree of XML 
     * DOM Nodes whose syntax is defined by the given metadata format.  
     * The previous state is altered only as necessary to accomodate the nodes 
     * that are present in the given tree.  If the tree structure or contents
     * are invalid, an {@link IIOInvalidTreeException} will be thrown.
     *
     * @param formatName the desired metadata format.
     * @param root an XML DOM Node object forming the root of a tree.
     */
    public void mergeTree(final String str, final Node node) 
    {
        // Not supported, since we are read-only.
        throw new IllegalStateException();
    }
    
    /**
     * Resets all the data stored in this object to default values, usually to 
     * the state this object was in immediately after construction, though the 
     * precise semantics are plug-in specific.
     */
    public void reset() 
    {
        data.clear();
    }
    
    /**
     * Ajoute un couple name/value a la hierarchie des méta-données.
     *
     * @param name  keyword associate to value.
     * @param value value associate to keyword.
     */
    final void put(final String name, final Object value)
    {
        data.put(name, value);
    }
    
    /**
     * Ajoute un couple name/value a la hierarchie des méta-données.
     *
     * @param name  keyword associate to value.
     * @param value value associate to keyword.
     */
    final void put(final String name, final double value)
    {
        data.put(name, new Double(value));
    }    
    
    /**
     * Ajoute un couple name/value a la hierarchie des méta-données.
     *
     * @param name  keyword associate to value.
     * @param value value associate to keyword.
     */
    final void put(final String name, final int value)
    {
        data.put(name, new Integer(value));
    }

    /**
     * Ajoute un couple keyword/value a la hierarchie des meta donnees.
     *
     * @param key   keyword associate to value val.
     * @param val   value associate to keyword.
     */
    final void put(final String name, final short value)
    {
        data.put(name, new Short(value));
    }            

    /**
     * Retourne l'objet associe au nom spécifié.
     */
    final Object get(final String name)
    {
        return data.get(name);
    }
        
    /**
     * Retourne l'identifiant du satellite.
     * @return l'identifiant du satellite.
     */
    public String getSpacecraft() 
    {
        return ((String)get(Metadata.SPACECRAFT));
    }
    
    /**
     * Retourne la date de debut de l'acquisition.
     * @return la date de debut de l'acquisition.
     */
    public Date getStartTime() 
    {
        return ((Date)get(Metadata.START_TIME));
    }
    
    /**
     * Retourne le nombre de ligne de l'image.
     * @return le nombre de ligne de l'image.
     */
    public int getHeight() 
    {
        return ((Integer)get(Metadata.HEIGHT)).intValue();
    }

    /**
     * Retourne la date de fin de l'acquisition.
     * @return la date de fin de l'acquisition.
     */
    public Date getEndTime() 
    {
        return ((Date)get(Metadata.END_TIME));
    }

    /**
     * Retourne la direction du satellite lors de l'acquisition <CODE>ImageReaderN1B.NORTH_TO_SOUTH</CODE>
     * ou <CODE>ImageReaderN1B.SOUTH_TO_NORTH</CODE>.
     * @return la direction du satellite lors de l'acquisition <CODE>ImageReaderN1B.NORTH_TO_SOUTH</CODE>
     * ou <CODE>ImageReaderN1B.SOUTH_TO_NORTH</CODE>.
     */
    public int getDirection() 
    {
        return ((Integer)get(Metadata.DIRECTION)).intValue();
    }       
    
    /**
     * Retourne le format du fichier N1B <CODE>Format.FORMAT_AJ</CODE> ou 
     * <CODE>Format.FORMAT_KLM</CODE>.
     * @return le format du fichier N1B <CODE>Format.FORMAT_AJ</CODE> ou 
     * <CODE>Format.FORMAT_KLM</CODE>.
     */
    public int getFormat() 
    {
        return ((Integer)get(Metadata.FORMAT)).intValue();
    }         
    
    /**
     * Retourne la grille des points de localisation. 
     * @return la grille des points de localisation. 
     */
    public LocalizationGridN1B getGridLocalization() throws IOException
    {        
        checkReader();
        return reader.getGridLocalization();
    }
    
    /**
     * Retourne une liste contenant les coefficients de calibration
     * nécessaire à la calibration du canal désiré.
     *
     * @param channel   Canal désiré.
     * @return une liste contenant les coefficients de calibration
     *         nécessaire à la calibration du canal désiré.
     */
    public ParameterList getCalibrationParameter(final Channel channel) throws IOException
    {
        checkReader();
        return reader.getCalibrationParameter(channel);
    }
    
    /**
     * Retourne le FileImageInputStream.
     * @return le FileImageInputStream.
     */
    public FileImageInputStream getFileImageInputStream() throws IOException
    {
        return (FileImageInputStream)reader.getInput();
    }
    
    /**
     * Vérifie la présence du Reader.
     * @exception IllegalStateException Si le reader n'est plus disponible.
     */
    private void checkReader() throws IllegalStateException
    {
        if (reader == null)
            throw new IllegalStateException("Reader n'est plus accessible.");
    }    
    
    /**
     *
     */
    public CoefficientGrid getBackScan(final Channel channel) throws IOException
    {
        checkReader();
        return ((ImageReaderN1BKLM)reader).getBackScan(channel);
    }
    
    /**
     *
     */
    public CoefficientGrid getSpaceData(final Channel channel) throws IOException
    {
        checkReader();
        return ((ImageReaderN1BKLM)reader).getSpaceData(channel);
    }
    
    /**
     *
     */
    public CoefficientGrid getTargetTemperatureData() throws IOException
    {
        checkReader();
        return ((ImageReaderN1BKLM)reader).getInternalTargetTemperatureData();
    }

    /**
     *
     */
    public CoefficientGrid getIrOperational(final Channel channel) throws IOException
    {
        checkReader();
        return ((ImageReaderN1BKLM)reader).getIrOperational(channel);
    }
}