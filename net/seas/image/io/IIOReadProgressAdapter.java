/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.image.io;

// Image I/O
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;


/**
 * An abstract adapter class for receiving image progress events.
 * The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class IIOReadProgressAdapter implements IIOReadProgressListener
{
    public void sequenceStarted  (ImageReader source, int minIndex)                       {}
    public void sequenceComplete (ImageReader source)                                     {}
    public void imageStarted     (ImageReader source, int imageIndex)                     {}
    public void imageProgress    (ImageReader source, float percentageDone)               {}
    public void imageComplete    (ImageReader source)                                     {}
    public void thumbnailStarted (ImageReader source, int imageIndex, int thumbnailIndex) {}
    public void thumbnailProgress(ImageReader source, float percentageDone)               {}
    public void thumbnailComplete(ImageReader source)                                     {}
    public void readAborted      (ImageReader source)                                     {}
}
