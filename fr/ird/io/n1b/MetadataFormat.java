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
package fr.ird.io.n1b;

// J2SE dependencies
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;


/**
 * Format des méta-données lorsqu'elles sont stockées sous forme d'arborecence XML. 
 * Les meta données sont stockées sous forme d'attributs. Chacun de ces attributs
 * est composé d'un nom (name) et d'une valeur (value).
 *
 * @version $Id$
 * @author Remi EVE
 */
final class MetadataFormat extends IIOMetadataFormatImpl 
{
    /** 
     * Create a single instance of this class (singleton pattern).
     */
    private static final MetadataFormat DEFAULT = new MetadataFormat();

    /**
     * The root name for this metadata format.
     */
    static final String ROOT_NAME = "fr_ird_N1B_1.0";

    /**
     * The name for the attribute.
     */
    static final String ATTRIBUTE_NAME = "Attribute";
    
    /**
     * Construct the meta-data format. This constructor
     * is private to enforce the singleton pattern.
     */
    private MetadataFormat() 
    {
        // Set the name of the root node.
        // The root node has a single child node type that may repeat.
        super(ROOT_NAME, CHILD_POLICY_REPEAT);
        
        // Set up the "Attribute" node, which has no children.
        addElement(ATTRIBUTE_NAME,
                   ROOT_NAME,
                   CHILD_POLICY_EMPTY);
        
        // Set up attribute "name" which is a String that is required
        // and has no default value.
        addAttribute(ATTRIBUTE_NAME, 
                     "name", 
                     DATATYPE_STRING,
                     true, 
                     null);
        
        // Set up attribute "value" which is a String that is required
        // and has no default value.
        addAttribute(ATTRIBUTE_NAME, 
                     "value", 
                     DATATYPE_STRING,
                     true, 
                     null);
    } 
    
    
    /**
     * Verifie que le nom d'élément est bien acceptable, c'est a dire dans notre cas,
     * qu'il s'agit bien de "Attribute" puisque c'est le seul nom d'élément que nous
     * autorisons.
     *
     * @param  elementName the name of the element being queried.
     * @param  imageType an ImageTypeSpecifier indicating the type of the image 
     *         that will be associated with themetadata
     * @return Vrai si un noeud peut apparaitre dans la hierarchie
     *         des métadonnees. Faux dans le cas contraire.
     */
    public boolean canNodeAppear(final String elementName, final ImageTypeSpecifier imageType) 
    {
        return elementName.equalsIgnoreCase(ATTRIBUTE_NAME);
    }
    
    /**
     * Return the singleton instance.
     *
     * @return the singleton instance.
     */
    public static IIOMetadataFormat getDefaultInstance() 
    {
        return DEFAULT;
    }
}
