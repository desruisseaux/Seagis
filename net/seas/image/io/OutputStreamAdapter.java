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

// Entr�s/sorties
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.stream.ImageOutputStream;


/**
 * Wrap an {@link ImageOutputStream} into a
 * standard {@link java.io.OutputStream}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class OutputStreamAdapter extends OutputStream
{
    /**
     * The wrapped image output stream.
     */
    private final ImageOutputStream output;

    /**
     * Construct a new output stream.
     */
    public OutputStreamAdapter(final ImageOutputStream output)
    {this.output=output;}

    /**
     * Writes the specified byte to this output stream.
     * @throws IOException if an I/O error occurs.
     */
    public void write(final int b) throws IOException
    {output.write(b);}

    /**
     * Writes <code>b.length</code> bytes from the specified byte array.
     * @throws IOException if an I/O error occurs.
     */
    public void write(final byte[] b) throws IOException
    {output.write(b);}

    /**
     * Writes <code>len</code> bytes from the specified byte array.
     * @throws IOException if an I/O error occurs.
     */
    public void write(final byte[] b, final int off, final int len) throws IOException
    {output.write(b, off, len);}

    /**
     * Forces any buffered output bytes to be written out.
     * @throws IOException if an I/O error occurs.
     */
    public void flush() throws IOException
    {output.flush();}

    /**
     * Closes this output stream.
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException
    {output.close();}
}
