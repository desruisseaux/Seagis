/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seagis.io;

// Standards I/O
import java.io.Writer;
import java.io.FilterWriter;
import java.io.IOException;

// Miscellaneous
import net.seagis.resources.Utilities;


/**
 * Write characters to a stream while expanding
 * tabulations (<code>'\t'</code>) into spaces.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class ExpandedTabWriter extends FilterWriter
{
    /**
     * Tabulation width (in number of spaces).
     */
    private int tabWidth = 8;

    /**
     * Current column position.
     * Columns are numbered from 0.
     */
    private int column = 0;
    
    /**
     * Construct a filter replacing tabulation characters (<code>'\t'</code>)
     * into spaces. Tabulations width default to 8 characters.
     *
     * @param out a Writer object to provide the underlying stream.
     */
    public ExpandedTabWriter(final Writer out)
    {super(out);}
    
    /**
     * Construct a filter replacing tabulation characters (<code>'\t'</code>)
     * into spaces, using the specified tabulation width.
     *
     * @param  out a Writer object to provide the underlying stream.
     * @param  tabWidth The tabulation width. Must be greater than 0.
     * @throws IllegalArgumentException if <code>tabWidth</code>
     *         is not greater than 0.
     */
    public ExpandedTabWriter(final Writer out, final int tabWidth) throws IllegalArgumentException
    {
        super(out);
        setTabWidth(tabWidth);
    }
    
    /**
     * Set the tabulation width.
     *
     * @param  tabWidth The tabulation width. Must be greater than 0.
     * @throws IllegalArgumentException if <code>tabWidth</code>
     *         is not greater than 0.
     */
    public void setTabWidth(final int tabWidth) throws IllegalArgumentException
    {
        synchronized (lock)
        {
            if (tabWidth>0)
            {
                this.tabWidth=tabWidth;
            }
            else throw new IllegalArgumentException(Integer.toString(tabWidth));
        }
    }
    
    /**
     * Returns the tabulation width.
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
        out.write(Utilities.spaces(width));
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
            out.write(c);
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

                    case '\t': out.write(buffer, start, end-start);
                               start=end+1;
                               expand();
                               break;
                    
                    default  : column++;
                               break;
                }
            }
            out.write(buffer, start, length-start);
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
                    
                    case '\t': out.write(string, start, end-start);
                               start=end+1;
                               expand();
                               break;
                    
                    default  : column++;
                               break;
                }
            }
            out.write(string, start, length-start);
        }
    }
}