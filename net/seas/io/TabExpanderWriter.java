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

// Miscellaneous
import net.seas.util.XString;


/**
 * Write characters to a stream while expanding tabulations into spaces.
 * Tabulations are aligned at fixed positions. Its mean that the number
 * of spaces replacing a <code>'\t'</code> character will vary depending
 * the horizontal position where they appear.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TabExpanderWriter extends FilterWriter
{
	/**
	 * Tabulation frequency (in number of spaces).
	 */
	private int tabWidth = 8;
	
	/**
	 * Current column position.
	 * Columns are numbered from 0.
	 */
	private int column = 0;
	
	/**
	 * Construit un filtre qui remplacera les tabulations par des espaces. Par défaut,
	 * on considère que les taquets de tabulations sont espacés de 8 caractères. Ce
	 * nombre pourra être modifiés par un appel à la méthode {@link #setTabWidth}.
	 *
	 * @param out a Writer object to provide the underlying stream.
	 */
	public TabExpanderWriter(final Writer out)
	{super(out);}
	
	/**
	 * Construit un filtre qui remplacera les tabulations par des espaces. On considèrera
	 * que les taquets de tabulations sont espacés de <code>tabWidth</code> espaces. Ce
	 * nombre pourra être modifiés par un appel à la méthode {@link #setTabWidth}.
	 *
	 * @param  out a Writer object to provide the underlying stream.
	 * @param  tabWidth Nombre de caractères que représente chaque
	 *         tabulations. Ce nombre doit être supérieur à 0.
	 * @throws IllegalArgumentException if <code>tabWidth</code>
	 *         is not greater than 0.
	 */
	public TabExpanderWriter(final Writer out, final int tabWidth) throws IllegalArgumentException
	{
		super(out);
		setTabWidth(tabWidth);
	}
	
	/**
	 * Spécifie le nombre d'espaces que représente chaque tabulation. Lors de l'écriture, les
	 * tabulations ne seront pas nécessairement remplacés par exactement <code>tabWidth</code>
	 * espaces. Par exemple si chaque tabulations représente 8 espaces, alors une tabulation
	 * sera remplacée par 3 espaces si elle apparait à la colonne #5 ou par 7 espaces si elle
	 * apparait à la colonne #9.
	 *
	 * @param  tabWidth Nombre de caractères que représente chaque
	 *        tabulations. Ce nombre doit être supérieur à 0.
	 * @throws IllegalArgumentException si <code>tabWidth</code>
	 *        est inférieur ou égal à 0.
	 */
	public void setTabWidth(final int tabWidth) throws IllegalArgumentException
	{
		synchronized (lock)
		{
			if (tabWidth>0) this.tabWidth=tabWidth;
			else throw new IllegalArgumentException(Integer.toString(tabWidth));
		}
	}
	
	/**
	 * Renvoie le nombre d'espaces que
	 * représente chaque tabulation.
	 */
	public int getTabWidth()
	{return tabWidth;}
	
	/**
	 * Write spaces for a tabulation.
	 *
	 * @throws IOException If an I/O error occurs
	 */
	private void expand() throws IOException
	{
		final int width = tabWidth - (column % tabWidth);
		super.write(XString.spaces(width));
		column += width;
	}
	
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
				case '\r': // fall through
				case '\n': column=0; break;
				case '\t': expand(); return;
				default  : column++; break;
			}
			super.write(c);
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
	public void write(final char[] buffer, final int offset, int length) throws IOException
	{
		synchronized (lock)
		{
			int start=offset;
			length += offset;
			for (int end=offset; end<length; end++)
			{
				final char c=buffer[end];
				switch (c)
				{
					case '\r': // fall through
					case '\n': column=0;
					           break;

					case '\t': super.write(buffer, start, end-start);
					           start=end+1;
					           expand();
					           break;
					
					default  : column++;
					           break;
				}
			}
			super.write(buffer, start, length-start);
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
	public void write(final String string, final int offset, int length) throws IOException
	{
		synchronized (lock)
		{
			int start=offset;
			length += offset;
			for (int end=offset; end<length; end++)
			{
				final char c=string.charAt(end);
				switch (c)
				{
					case '\r': // fall through
					case '\n': column=0;
					           break;
					
					case '\t': super.write(string, start, end-start);
					           start=end+1;
					           expand();
					           break;
					
					default  : column++;
					           break;
				}
			}
			super.write(string, start, length-start);
		}
	}
}
