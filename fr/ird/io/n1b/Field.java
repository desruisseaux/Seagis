/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.io.n1b;

// Date and time
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

// Image I/O
import java.io.IOException;
import javax.imageio.stream.ImageInputStream;


/**
 * Descriptor for a field in N1B files. Each field can have an arbitrary width
 * expressed in bytes. Methods like {@link #getString} and {@link #getInteger}
 * can be used to fetch the field's value from an input stream.
 * <br><br>
 * The first field must be constructed with the one-argument constructor. All
 * subsequent fields  <strong>must</strong>  be linked to the previous field,
 * using the two or three arguments constructors. Read operations like {@link
 * #getString} will always expect the stream position of the first field (the
 * <code>base</code>) in argument.
 *
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaux
 */
final class Field
{
    /**
     * The calendar to use for computing dates.
     */
    private static final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.FRANCE);
    static
    {
        calendar.clear();
    }
    
    /**
     * The field offest, in bytes relative to the base.
     */
    private final int offset;

    /**
     * The field width, in bytes.
     */
    private final int size;

    /**
     * Construct the first field.
     *
     * @param The field size.
     */
    public Field(final int size)
    {
        this.offset = 0;
        this.size   = size;
    }

    /**
     * Construct a new field linked to a previous field.
     *
     * @param last The previous field.
     * @param size The field size.
     */
    public Field(final Field previous, final int size)
    {
        this.offset = previous.offset + previous.size;
        this.size   = size;
    }

    /**
     * Construct a new field linked to a previous field.
     *
     * @param last The previous field.
     * @param skip The number of bytes to skip after the previous field.
     * @param size The field size.
     */
    public Field(final Field previous, final int skip, final int size)
    {
        this.offset = previous.offset + previous.size + skip;
        this.size   = size;
    }
        
    /**  
     * Retourne la valeur d'un champ sous forme de chaîne de caractères.
     *
     * @param  in Le flot à lire.
     * @param  base Position dans le flot du premier champ.
     * @return Une chaine de caracteres contenant la valeur du champ.
     * @throws IOException si une erreur survient lors de la lecture.
     */
    public String getString(final ImageInputStream in, final long base) throws IOException 
    {       
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        in.readFully(buffer);
        return new String(buffer, "US-ASCII").trim();
    }   

   /**  
     * Retourne la valeur d'un champ sous forme d'un entier.
     *
     * @param  in Le flot à lire.
     * @param  base Position dans le flot du premier champ.
     * @return Un entier contenant la valeur du champ.
     * @throws IOException si une erreur survient lors de la lecture.
     */
    public int getInteger(final ImageInputStream in, final int base) throws IOException 
    {       
        in.seek(base + offset);
        switch (size)
        {
            case 1:  return in.readByte();
            case 2:  return in.readShort();
            case 4:  return in.readInt();
            default: throw new IllegalStateException(String.valueOf(size));
        }
    }    
    
    /**  
     * Retourne la valeur d'un champ sous forme d'un tableau d'octets.
     *
     * @param  in Le flot à lire.
     * @param  base Position dans le flot du premier champ.
     * @return Un tableau de byte contenant la valeur du champ.
     * @throws IOException si une erreur survient lors de la lecture.
     */
    public byte[] getByteArray(final ImageInputStream in, final int base) throws IOException 
    {        
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        in.readFully(buffer);        
        return buffer;
    }       

    /**  
     * Retourne la valeur d'un champ sous forme d'une date.
     * La date est contenue dans 6 octets organisés de la manière suivante:
     * <UL>
     *   <LI>  7 bits for years </LI>
     *   <LI>  9 bits for julian day </LI>
     *   <LI>  5 bits unused </LI>
     *   <LI> 27 bits for milliseconds (UTC time of day) </LI>
     * </UL>
     *
     * @param  in Le flot à lire.
     * @param  base Indice du curseur dans le flux.
     * @return Une date contenant la valeur du champ.
     * @throws IOException si une erreur survient lors de la lecture.
     */
    public Date getDate(final ImageInputStream in, final int base) throws IOException 
    {        
        in.seek(base + offset);
        final long   year = in.readBits( 7) + 1900;
        final long julian = in.readBits( 9);
        /* unused */        in.readBits( 5);
        final long millis = in.readBits(27);

        final Date time;
        synchronized (calendar)
        {
            calendar.set(Calendar.YEAR, (int)year);
            time = calendar.getTime();
        }
        time.setTime(time.getTime() + (julian-1)*(24*60*60*1000) + millis);
        return time;
    }
}
