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
import java.util.Date;
import java.io.IOException;

/**
 * Cette classe définie les méthodes spécifiques aux fichiers level 1B(N1B) au format AJ 
 * (ancien format) des images des canaries. La différence se situe au niveau de la date 
 * qui code l'année depuis l'an 2000.
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class ImageReaderN1BAJCanaries extends ImageReaderN1BAJ 
{        
    /** 
     * Constructeur.
     */
    public ImageReaderN1BAJCanaries(final ImageReaderSpi provider) 
    {
        super(provider);
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
        return field.getDateFormatv4(input, base);
    }   
}