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
package fr.ird.io.image;

// Entrés/sorties
import java.io.File ;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

// Java Advanced Imaging et divers
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

/**
 * Utilitaires pour les lectures et écritures d'images. Ces utilitaires
 * sont temporaires. Elles seront peut-être retirées lorsque la prochaine
 * version de Java Advanced Imaging (1.2) sera disponible.
 *
 * @version $Id$
 * @author Laurent Demagistri
 */
public class Utilities {
    /**
     * Enregistre une image en binaire. Chaque pixel sera codé
     * avec le type <code>float</code>.
     *
     * @param  image L'image à enregistrer.
     * @param  file Le fichier dans lequel écrire l'image.
     * @throws IOException si l'écriture des données a échouée.
     */
    public static void writeRawFloat(RenderedImage image, File file) throws IOException {
        FileOutputStream fileOutput = new FileOutputStream(file);
        DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(fileOutput));
        final RectIter rectIter = RectIterFactory.create(image, null);
        rectIter.startLines();
        while (!rectIter.finishedLines()) {
            rectIter.startPixels();
            while (!rectIter.finishedPixels()) {
                float fValue = rectIter.getSampleFloat();
                dataOutput.writeFloat(fValue);
                rectIter.nextPixel();
            }
            rectIter.nextLine();
        }
        dataOutput.close();
    }
}
