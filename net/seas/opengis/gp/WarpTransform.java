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
package net.seas.opengis.gp;

// Coordinate transforms
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.ct.TransformException;

// Miscellaneous
import javax.media.jai.Warp;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import net.seas.awt.ExceptionMonitor;


/**
 * Objet à utiliser avec <i>Java Advanced Imaging</i> pour projeter une image.
 * La méthode {@link #warpSparseRect} calculera les coordonnées sources qui
 * correspondent aux coordonnées des pixels de l'image de destination (c'est-à-dire
 * qu'elle effectuera la transformation <em>inverse</em>, en appellant une des méthodes
 * <code>CoordinateTransform.inverseProject(...)</code>).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class WarpTransform extends Warp
{
    /**
     * Transformation à appliquer.
     */
    private final CoordinateTransform transform;

    /**
     * Transformation affine servant à convertir les coordonnées géographiques
     * de l'image source en indices de pixels. Les coordonnées géographiques
     * devront être exprimées selon le système de coordonnées de l'image source.
     */
    private final AffineTransform mapSourcePixel;

    /**
     * Transformation affine servant à convertir les indices de pixels de
     * l'image destination en coordonnées géographiques.  Ces coordonnées
     * géographiques seront exprimées  selon le système de coordonnées de
     * l'image destination.
     */
    private final AffineTransform mapDestCoord;

    /**
     * Construit un nouvel objet <code>WarpTransform</code> qui
     * enveloppe la transformation de coordonnées spécifiée.
     */
    public WarpTransform(final CoordinateTransform transform, final AffineTransform mapSourcePixel, final AffineTransform mapDestCoord)
    {
        // Rappel sur le fonctionnement de AffineTransform:
        // L'ordre des opétations sera l'inverse de l'ordre
        // du code ci-dessous (lire de bas en haut).

        this.mapSourcePixel = AffineTransform.getTranslateInstance(-0.5, -0.5);
        this.mapSourcePixel.concatenate(mapSourcePixel);

        this.transform = transform;

        this.mapDestCoord = new AffineTransform(mapDestCoord);
        this.mapDestCoord.translate(0.5, 0.5);
    }

    /**
     * Retourne les coordonnées représentant la projections inverses
     * des pixels se trouvant dans le rectangle spécifié.
     */
    public float[] warpSparseRect(final int xmin, final int ymin, final int width, final int height, final int periodX, final int periodY, float[] destRect)
    {
        if (periodX<1) throw new IllegalArgumentException(String.valueOf(periodX));
        if (periodY<1) throw new IllegalArgumentException(String.valueOf(periodY));

        final int xmax  = xmin+width;
        final int ymax  = ymin+height;
        final int count = ((width+(periodX-1))/periodX) * ((height+(periodY-1))/periodY);
        if (destRect==null) destRect = new float[2*count];

        int index = 0;
        for (int y=ymin; y<ymax; y+=periodY)
        {
            for (int x=xmin; x<xmax; x+=periodX)
            {
                destRect[index++] = x;
                destRect[index++] = y;
            }
        }
        mapDestCoord.transform(destRect, 0, destRect, 0, count);
        try
        {
            transform.inverse().transform(destRect, 0, destRect, 0, count);
        }
        catch (TransformException exception)
        {
            // La transformation a échouée. Les point qui n'ont
            // pas pu être projetés auront la valeur NaN.
            ExceptionMonitor.unexpectedException("net.seas.opengis.gc", "WarpTransform", "warpSparseRect", exception);
        }
        mapSourcePixel.transform(destRect, 0, destRect, 0, count);
        return destRect;
    }
}
