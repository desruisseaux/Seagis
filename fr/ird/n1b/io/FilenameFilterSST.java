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

// J2SE
import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.io.FilenameFilter;

/**
 * Filtre les images Sea Surface Tremperature (S.S.T.) par passage acquises durant une 
 * période précise. <BR><BR>
 *
 * Le format des images S.S.T. par passage est définie comme suit : 
 * <UL>
 *  <LI>SATyyyyjjjhhmmss</LI>
 *  <LI>exemple : N162003017093751</LI>
 * </UL>
 *
 * @author Remi Eve
 * @version $Id$
 */
public class FilenameFilterSST implements FilenameFilter
{      
    /** Pattern filtrant les fichiers S.S.T. par passage : NXXyyyyjjjhhmmss.png. */
    private static String FILE_PATTERN = "N[0-9]{15}.sst.png";    
    
    /** Date de début de la période. */
    private final Date start;
    
    /** Date de fin de la période. */
    private final Date end;
       
    /**
     * Constructeur.
     *
     * @param start   Date de début de la période.
     * @param end     Date de fin de la période.
     */
    public FilenameFilterSST(final Date start, final Date end) 
    {
        super();
        this.start = start;
        this.end   = end;
    }
    
    /** 
     * Retourne <i>true</i> si le fichier a été acquis durant la période et <i>false</i> 
     * sinon.
     *
     * @param directory   Répertoire.
     * @param name        Nom du fichier.
     * @return <i>true</i> si le fichier a été acquis durant la période et <i>false</i> 
     *         sinon.
     */    
    public boolean accept(final File directory, final String name) 
    {
        if (Pattern.matches(FILE_PATTERN, name))
        {
            /* Le format du nom du fichier est correcte. Il est maintenant nécessaire de 
               garder les fichiers acquis dans la période "start" .. "end". */
            final int index = 2;
            final int year  = Integer.parseInt(name.substring(index+1, index+5)),
                      dayOfYear = Integer.parseInt(name.substring(index+5, index+8)),
                      hour  = Integer.parseInt(name.substring(index+8, index+10)),
                      min   = Integer.parseInt(name.substring(index+10, index+12)),
                      sec   = Integer.parseInt(name.substring(index+12, index+14));           
            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            calendar.set(Calendar.YEAR,         year);
            calendar.set(Calendar.DAY_OF_YEAR,  dayOfYear);
            calendar.set(Calendar.HOUR_OF_DAY,  hour);
            calendar.set(Calendar.MINUTE,       min);
            calendar.set(Calendar.SECOND,       sec);            
            final Date date = calendar.getTime();                        
            if (start.before(date) && date.before(end))
                return true;
        }
        return false;
    }
    
    /** 
     * Retourne un descriptif du filtre.
     * @return un descriptif du filtre.
     */    
    public String getDescription() 
    {
        return "Filtre les fihciers S.S.T. par passage";
    }
}