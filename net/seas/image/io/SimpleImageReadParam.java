/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.image.io;

// Miscellaneous
import java.awt.Dimension;
import javax.imageio.ImageReadParam;


/**
 * A class describing how a stream is to be decoded. In the context of
 * {@link SimpleImageReader}, the stream may not contains enough information
 * for an optimal decoding. For example the stream may not contains image's
 * width and height. The <code>SimpleImageReadParam</code> gives a chance
 * to specify those missing informations.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class SimpleImageReadParam extends ImageReadParam
{
    /**
     * The expected image size, or <code>null</code> if unknow.
     */
    private Dimension size;

    /**
     * Construct a new <code>SimpleImageReadParam</code>
     * with default parameters.
     */
    public SimpleImageReadParam()
    {}

    /**
     * Set the expected image size (in pixels), or <code>null</code>
     * if unknow. This is the image's size in the input stream; it is
     * not dependent of any subsampling or scale setting. Some
     * {@link SimpleImageReader} may use this information when
     * they are unable to figure out the image's size by themselves.
     */
    public void setExpectedSize(final Dimension size)
    {this.size = (size!=null) ? new Dimension(size) : null;}

    /**
     * Returns the expected image size set by the last call to
     * {@link #setExpectedSize}. This size is not fetched from
     * the input stream. Consequently, it may not be accurate.
     * This method returns <code>null</code> if the expected
     * size has not been set.
     */
    public Dimension getExpectedSize()
    {return (size!=null) ? (Dimension)size.clone() : null;}
}
