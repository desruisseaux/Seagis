/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D?veloppement
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
 *          Maison de la t?l?d?tection
 *          Institut de Recherche pour le d?veloppement
 *          500 rue Jean-Fran?ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.io;

// J2SE dependencies
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.io.IOException;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;


/**
 * Enumeration class for fields in N1B files.
 *
 * @version $Id$
 * @author Remi Eve
 */
final class Field
{
    /**
     * The field offest, in bytes
     * relative to the file begining.
     */
    public final int offset;

    /**
     * The field width, in bytes.
     */
    public final int size;

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
     * Retourne la valeur d'un champ sous forme de cha?ne de caract?res.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Une chaine de caracteres contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public String getString(final ImageInputStream in, final long base) throws IOException 
    {       
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        in.readFully(buffer);
        return new String(buffer, "US-ASCII").trim();
    }   

   /**  
     * Retourne la valeur d'un champ sous forme d'un short.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Un short contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public short getShort(final ImageInputStream in, final long base) throws IOException 
    {       
        in.seek(base + offset);
        return in.readShort();
    }    
    
   /**  
     * Retourne la valeur d'un champ sous forme d'un unsigned short.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Un unsigned short contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public int getUnsignedShort(final ImageInputStream in, final long base) throws IOException 
    {       
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        return in.readUnsignedShort();
    }    

    /**  
     * Retourne la valeur d'un champ sous forme d'un integer.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Un integer contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public int getInteger(final ImageInputStream in, final long base) throws IOException 
    {       
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        return in.readInt();
    }    

    /**  
     * Retourne la valeur d'un champ sous forme d'un unsigned integer.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Un long contenant un unsigned integer.
     * @throws IOException si une input ou output exception survient.
     */
    public long getUnsignedInteger(final ImageInputStream in, final long base) throws IOException 
    {       
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        return in.readUnsignedInt();
    }    

    /**  
     * Retourne la valeur d'un champ sous forme d'un tableau de byte.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Un tableau de byte contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public byte[] getByteArray(final ImageInputStream in, final long base) throws IOException 
    {        
        in.seek(base + offset);
        final byte[] buffer = new byte[size];
        in.readFully(buffer);        
        return buffer;
    }       
        
    /**  
     * Retourne la valeur d'un champ sous forme d'un tableau de byte.
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Un tableau de byte contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public byte getByte(final ImageInputStream in, final long base) throws IOException 
    {        
        in.seek(base + offset);
        return in.readByte();
    }    

    /**  
     * Retourne la valeur d'un champ TimeCodeDate sous forme d'une date.
     * <UL>
     * La date est contenue dans 6 bytes et composee de la maniere 
     * suivante :     
     *   <LI>7 bits for years </LI>
     *   <LI>9 bits for julian day </LI>
     *   <LI>5 bits unused </LI>
     *   <LI>27 bits for milisec (UTC time of day) </LI>
     * </UL>
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Une date contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public Date getDateFormatv4(final ImageInputStream in, final long base) throws IOException 
    {        
        int year       = 2000;
        int dayOfYear  = 0;
        long milisec   = 0;
        int hour       = 0;
        int minute     = 0;
        int sec        = 0;
        int msec       = 0;
        in.seek(base + offset);        
        
        // An        
        for (int i=0; i<7 ; i++)
            year += in.readBit() * (int)(Math.pow(2.0,6-i));
        
        // Jour julien
        for (int i=0; i<9 ; i++)
            dayOfYear += in.readBit() * (int)(Math.pow(2.0,8 - i));
        
        // On passe les bits inutilises
        for (int i=0; i<5 ; i++)
            in.readBit();
        
        // Milisec
        for (int i=0; i<27 ; i++)         
            milisec += in.readBit() * (int)(Math.pow(2.0,26 - i));

        // calcule des autres valeurs de temps 
        hour    = (int) Math.floor(milisec / (1000*3600));       
        minute  = (int) Math.floor((milisec / 1000 - hour * 3600) / 60);
        sec     = (int) Math.floor(milisec / 1000 - hour * 3600 - minute * 60);        
        msec    = (int) Math.floor(milisec - hour*3600*1000 - minute*60*1000 - sec*1000);        
        
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);        
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, msec);
        return calendar.getTime();
    }    

    /**  
     * Retourne la valeur d'un champ TimeCodeDate sous forme d'une date.
     * <UL>
     * La date est contenue dans 6 bytes et composee de la maniere 
     * suivante :     
     *   <LI>7 bits for years </LI>
     *   <LI>9 bits for julian day </LI>
     *   <LI>5 bits unused </LI>
     *   <LI>27 bits for milisec (UTC time of day) </LI>
     * </UL>
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Une date contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public Date getDateFormatv3(final ImageInputStream in, final long base) throws IOException 
    {        
        int year       = 1900;
        int dayOfYear  = 0;
        long milisec   = 0;
        int hour       = 0;
        int minute     = 0;
        int sec        = 0;
        int msec       = 0;
        in.seek(base + offset);        
        
        // An        
        for (int i=0; i<7 ; i++)
            year += in.readBit() * (int)(Math.pow(2.0,6-i));
        
        // Jour julien
        for (int i=0; i<9 ; i++)
            dayOfYear += in.readBit() * (int)(Math.pow(2.0,8 - i));
        
        // On passe les bits inutilises
        for (int i=0; i<5 ; i++)
            in.readBit();
        
        // Milisec
        for (int i=0; i<27 ; i++)         
            milisec += in.readBit() * (int)(Math.pow(2.0,26 - i));

        // calcule des autres valeurs de temps 
        hour    = (int) Math.floor(milisec / (1000*3600));       
        minute  = (int) Math.floor((milisec / 1000 - hour * 3600) / 60);
        sec     = (int) Math.floor(milisec / 1000 - hour * 3600 - minute * 60);        
        msec    = (int) Math.floor(milisec - hour*3600*1000 - minute*60*1000 - sec*1000);        
        
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);        
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, msec);
        return calendar.getTime();
    }    
    
    /**  
     * Retourne la valeur d'un champ sous forme d'une date.
     * <UL>
     * La date est contenue dans 8 bytes et composee de la maniere 
     * suivante :     
     *   <LI>2 bytes pour l'annee</LI>
     *   <LI>2 bytes pour le jour de l'annee</LI>
     *   <LI>4 bytes pour le temps en millisecond dans la journee</LI>
     * </UL>
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Une date contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public Date getDateFormatv1(final ImageInputStream in, final long base) throws IOException 
    {                
        in.seek(base + offset);        
        final int year       = in.readUnsignedShort();
        final int dayOfYear  = in.readUnsignedShort();
        final long timeOfDay = in.readUnsignedInt();
 
        // calcule des autres valeurs de temps 
        int hour    = (int) Math.floor(timeOfDay / (1000*3600));       
        int minute  = (int) Math.floor((timeOfDay / 1000 - hour * 3600) / 60);
        int sec     = (int) Math.floor(timeOfDay / 1000 - hour * 3600 - minute * 60);        
        int msec    = (int) Math.floor(timeOfDay - hour*3600*1000 - minute*60*1000 - sec*1000);                
        
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);        
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, msec);
        return calendar.getTime();
    }        
    
    /**  
     * Retourne la valeur d'un champ sous forme d'une date.
     * <UL>
     * La date est contenue dans 8 bytes et composee de la maniere 
     * suivante :     
     *   <LI>2 bytes pour l'annee</LI>
     *   <LI>2 bytes pour le jour de l'annee</LI>
     *   <LI>2 bytes inutile</LI>
     *   <LI>4 bytes pour le temps en millisecond dans la journee</LI>
     * </UL>
     *
     * @param  in Le flot ? lire.
     * @param  base Indice du curseur dans le flux.
     * @return Une date contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public Date getDateFormatv2(final ImageInputStream in, final long base) throws IOException 
    {                         
        in.seek(base + offset);      
        
        int year      = 0;
        for (int i=0 ; i<16 ; i++)
        {
            year = year << 1;
            year += in.readBit();
        }
        
        int dayOfYear  = 0;
        for (int i=0 ; i<16 ; i++)
        {
            dayOfYear = dayOfYear << 1;
            dayOfYear += in.readBit();
        }
        
        for (int i=0 ; i<16 ; i++)
            in.readBit();

        long timeOfDay = 0;
        for (int i=0 ; i<32 ; i++)
        {
            timeOfDay = timeOfDay << 1;
            timeOfDay += in.readBit();
        }       
        
        // calcule des autres valeurs de temps 
        int hour    = (int) Math.floor(timeOfDay / (1000*3600));       
        int minute  = (int) Math.floor((timeOfDay / 1000 - hour * 3600) / 60);
        int sec     = (int) Math.floor(timeOfDay / 1000 - hour * 3600 - minute * 60);        
        int msec    = (int) Math.floor(timeOfDay - hour*3600*1000 - minute*60*1000 - sec*1000);        
                
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);        
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, msec);
        return calendar.getTime();
    }            
    
    /**  
     * Ecrit la date.
     * <UL>
     * La date est contenue dans 8 bytes et composee de la maniere 
     * suivante :     
     *   <LI>2 bytes pour l'annee</LI>
     *   <LI>2 bytes pour le jour de l'annee</LI>
     *   <LI>4 bytes pour le temps en millisecond dans la journee</LI>
     * </UL>
     *
     * @param  out    Le flot ? a ecrire.
     * @param  base  Indice du curseur dans le flux.
     * @param  date  La date a ecrire.
     * @throws IOException si une input ou output exception survient.
     */
    public void setDateFormatv1(final ImageOutputStream out, 
                                final long              base, 
                                final Date              date) throws IOException 
    {           
        out.seek(base + offset);    
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));        
        calendar.setTime(date);
        final int year          = calendar.get(Calendar.YEAR);
        final int dayOfYear     = calendar.get(Calendar.DAY_OF_YEAR);
        final int timeOfDay     = calendar.get(Calendar.HOUR_OF_DAY) * 3600 * 1000 + 
                                  calendar.get(Calendar.MINUTE) * 60 * 1000 +
                                  calendar.get(Calendar.SECOND) * 1000 +
                                  calendar.get(Calendar.MILLISECOND);
        
        out.writeShort(year);
        out.writeShort(dayOfYear);
        out.writeInt(timeOfDay);
    }        

    /**
     * Ecrit un tableau de byte equivalent a la date au format.<BR><BR>
     *
     * Format de la date :<BR>
     * <UL>
     * La date est contenue dans 6 bytes et composee de la maniere 
     * suivante :     
     *   <LI>7 bits for years </LI>
     *   <LI>9 bits for julian day </LI>
     *   <LI>5 bits unused </LI>
     *   <LI>27 bits for milisec (UTC time of day) </LI>
     * </UL>
     *
     * @param  out    Le flot ? a ecrire.
     * @param  base  Indice du curseur dans le flux.
     * @param  date  La date a ecrire.
     */
    public void setDateFormatv3(final ImageOutputStream out, final long base, final Date date) 
            throws IOException
    {
         out.seek(base + offset);        
         final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
         calendar.setTime(date);
         int year      = calendar.get(Calendar.YEAR) - 1900;
         int julianDay = calendar.get(Calendar.DAY_OF_YEAR);         
         long msec     = calendar.get(Calendar.HOUR_OF_DAY) * 3600 * 1000 + 
                         calendar.get(Calendar.MINUTE) * 60 * 1000 +
                         calendar.get(Calendar.SECOND) * 1000 +
                         calendar.get(Calendar.MILLISECOND);
         
         // An        
        for (int i=0; i<7 ; i++)
        {
            final int flag = year / (int)(Math.pow(2.0,6-i));            
            out.writeBit(flag);            
            year -= flag*(int)(Math.pow(2.0,6-i));
        }
        // Jour julien
        for (int i=0; i<9 ; i++)
        {
            final int flag = julianDay / (int)(Math.pow(2.0,8-i));            
            out.writeBit(flag);            
            julianDay -= flag*(int)(Math.pow(2.0,8-i));
        }
        
        // On passe les bits inutilises
        for (int i=0; i<5 ; i++)
            out.writeBit(0);
        
        // Milisec
        for (int i=0; i<27 ; i++)         
        {
            final int flag = (int)(msec / (int)(Math.pow(2.0,26-i)));            
            out.writeBit(flag);            
            msec -= flag*(int)(Math.pow(2.0,26-i));
        }
    }
    
   /**  
     * Ecrit la valeur sous forme d'un integer.
     *
     * @param  out   Le flot ? ecrire.
     * @param  base  Indice du curseur dans le flux.
     * @param  value Un integer contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public void setInteger(final ImageOutputStream out, final long base, final int value) 
                                                                                throws IOException 
    {       
        out.seek(base + offset);
        out.writeInt(value);
    }        

    /**  
     * Ecrit la valeur sous forme d'un short.
     *
     * @param  out   Le flot ? ecrire.
     * @param  base  Indice du curseur dans le flux.
     * @param  value Un short contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public void setShort(final ImageOutputStream out, final long base, final int value) 
                                                                                throws IOException 
    {       
        out.seek(base + offset);
        out.writeShort(value);
    }        
    
   /**  
     * Ecrit la valeur sous forme d'un unsigned short.
     *
     * @param  out   Le flot ? ecrire.
     * @param  base  Indice du curseur dans le flux.
     * @param  value Un short contenant la valeur du champ.
     * @throws IOException si une input ou output exception survient.
     */
    public void setUnsignedShort(final ImageOutputStream out, final long base, final int value) 
                                                                                throws IOException 
    {               
        out.seek(base + offset);        
        out.write(value & 0xFF);
        out.write((value>>8) & 0xFF );        
    }        
}