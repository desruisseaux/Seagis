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
import java.io.InputStream;
import javax.imageio.stream.ImageInputStream;


/**
 * Wrap an {@link ImageInputStream} into a
 * standard {@link java.io.InputStream}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class InputStreamAdapter extends InputStream
{
    /**
     * The wrapped image input stream.
     */
    private final ImageInputStream input;

    /**
     * Construct a new input stream.
     */
    public InputStreamAdapter(final ImageInputStream input)
    {this.input=input;}

    /**
     * Reads the next byte of data from the input stream.
     * @throws IOException if an I/O error occurs.
     */
    public int read() throws IOException
    {return input.read();}

    /**
     * Reads some number of bytes from the input stream.
     * @throws IOException if an I/O error occurs.
     */
    public int read(final byte[] b) throws IOException
    {return input.read(b);}

    /**
     * Reads up to <code>len</code> bytes of data from the input stream.
     * @throws IOException if an I/O error occurs.
     */
    public int read(final byte[] b, final int off, final int len) throws IOException
    {return input.read(b, off, len);}

    /**
     * Skips over and discards <code>n</code> bytes of data from this input stream.
     * @throws IOException if an I/O error occurs.
     */
    public long skip(final long n) throws IOException
    {return input.skipBytes(n);}

    /**
     * Returns always <code>true</code>.
     * @throws IOException if an I/O error occurs.
     */
    public boolean markSupported()
    {return true;}

    /**
     * Marks the current position in this input stream.
     * @throws IOException if an I/O error occurs.
     */
    public void mark(final int readlimit)
    {input.mark();}

    /**
     * Repositions this stream to the position at the time
     * the <code>mark</code> method was last called.
     * @throws IOException if an I/O error occurs.
     */
    public void reset() throws IOException
    {input.reset();}

    /**
     * Closes this input stream.
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException
    {input.close();}
}