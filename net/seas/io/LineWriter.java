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
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 */
package net.seas.io;

// Standards I/O
import java.io.Writer;
import java.io.FilterWriter;
import java.io.IOException;


/**
 * Write characters to a stream while replacing various EOL by a unique string.
 * This class catch all occurrences of <code>"\r"</code>, <code>"\n"</code> and
 * <code>"\r\n"</code>, and replace them by the platform depend EOL string
 * (<code>"\r\n"</code> on Windows, <code>"\n"</code> on Unix). Alternatively,
 * a specific EOL string may be explicitly set at construction time.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class LineWriter extends FilterWriter
{
	/**
	 * Caractère à placer à la fin de chaque lignes.
	 */
	private final String lineSeparator;

	/**
	 * Indique s'il faut ignorer le prochain caractère '\n'. Ce champ
	 * sert à éviter de remplacer "\r\n" par deux retours chariots.
	 */
	private boolean skipCR;

	/**
	 * Construit un objet qui convertira les codes de
	 * fin de ligne avant de les envoyer au flot spécifié.
	 *
	 * @param out a Writer object to provide the underlying stream.
	 */
	public LineWriter(final Writer out)
	{this(out, System.getProperty("line.separator", "\n"));}

	/**
	 * Construit un objet qui convertira les codes de
	 * fin de ligne avant de les envoyer au flot spécifié.
	 *
	 * @param out a Writer object to provide the underlying stream.
	 * @param lineSeparator Caractères à utilisers en fin de lignes.
	 */
	public LineWriter(final Writer out, final String lineSeparator)
	{
		super(out);
		this.lineSeparator = lineSeparator;
	}

	/**
	 * Ecrit un code de fin de ligne.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	private void writeEOL() throws IOException
	{super.write(lineSeparator, 0, lineSeparator.length());}

	/**
	 * Write a single character.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public void write(final int c) throws IOException
	{
		synchronized (lock)
		{
			switch (c)
			{
				case '\r':
				{
					writeEOL();
					skipCR=true;
					break;
				}
				case '\n':
				{
					if (!skipCR)
						writeEOL();
					skipCR=false;
					break;
				}
				default:
				{
					super.write(c);
					skipCR=false;
					break;
				}
			}
		}
    }

	/**
	 * Write a portion of an array of characters.
	 *
	 * @param  buffer  Buffer of characters to be written
	 * @param  offset  Offset from which to start reading characters
	 * @param  length  Number of characters to be written
	 * @throws IOException  If an I/O error occurs
	 */
    public void write(final char cbuf[], int offset, int length) throws IOException
	{
		synchronized (lock)
		{
			int base = offset;
			while (--length>=0)
			{
				switch (cbuf[offset++])
				{
					case '\r':
					{
						super.write(cbuf, base, offset-base-1);
						writeEOL();
						base=offset;
						skipCR=true;
						break;
					}
					case '\n':
					{
						if (!skipCR || (offset-base)!=1)
						{
							super.write(cbuf, base, offset-base-1);
							writeEOL();
						}
						base=offset;
						skipCR=false;
						break;
					}
				}
			}
			super.write(cbuf, base, offset-base);
		}
    }

	/**
	 * Write a portion of an array of a string.
	 *
	 * @param  string  String to be written
	 * @param  offset  Offset from which to start reading characters
	 * @param  length  Number of characters to be written
	 * @throws IOException  If an I/O error occurs
	 */
    public void write(final String string, int offset, int length) throws IOException
	{
		synchronized (lock)
		{
			int base = offset;
			while (--length>=0)
			{
				switch (string.charAt(offset++))
				{
					case '\r':
					{
						super.write(string, base, offset-base-1);
						writeEOL();
						base=offset;
						skipCR=true;
						break;
					}
					case '\n':
					{
						if (!skipCR || (offset-base)!=1)
						{
							super.write(string, base, offset-base-1);
							writeEOL();
						}
						base=offset;
						skipCR=false;
						break;
					}
				}
			}
			super.write(string, base, offset-base);
		}
    }
}
