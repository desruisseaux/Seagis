/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2004 Institut de Recherche pour le Développement
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
 */
package fr.ird.image.tools;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;


/**
 * Display an image histogram.
 *
 * @author Martin Desruisseaux
 */
public class Histogram {
    /**
     * Run from the command line. The only argument is the file to load.
     */
    public static void main(final String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Expected filename.");
            return;
        }
        final BufferedImage image = ImageIO.read(new File(args[0]));
        final Raster raster = image.getRaster();
        final int[] count = new int[256];
        final int xmin = raster.getMinX();
        final int ymin = raster.getMinY();
        final int xmax = raster.getWidth()  + xmin;
        final int ymax = raster.getHeight() + ymin;
        for (int y=ymin; y<ymax; y++) {
            for (int x=xmin; x<xmax; x++) {
                count[raster.getSample(x,y,0)]++;
            }
        }
        for (int i=0; i<count.length; i++) {
            System.out.print(i);
            System.out.print('\t');
            System.out.println(count[i]);
        }
    }
}
