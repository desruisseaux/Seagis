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
import java.io.*;
import java.util.Arrays;
import java.util.NoSuchElementException;

// SEAGIS
import fr.ird.util.*;
import fr.ird.n1b.io.*;
import fr.ird.io.text.ParseSatellite;

// JAI
import javax.media.jai.*;

/**
 * Informations ? propos d'un satellite. Chaque satellite porte un nom
 * (par exemple "NOAA 16") ainsi qu'un num?ro l'identifiant. Des m?thodes
 * statiques <code>get(...)</code> permettent d'obtenir un satellite ?
 * partir de son nom ou de son num?ro.
 * 
 * @version $Id$
 * @author Remi EVE
 * @author Martin Desruisseaux
 */
public final class Satellite
{        
    /**
     * Descriptions des satellites. Chaque ?l?ment de ce tableau d?crit un satellite portant
     * le num?ro de cet index. Par exemple <code>SATELLITES[12]</code> donne une description
     * du satellite NOAA 12. Les ?l?ments de ce tableau seront cr??s par {@link #get(int)} au
     * fur et ? mesure que des satellites seront demand?es.
     */
    private final static Satellite[] SATELLITES = new Satellite[18];

    /**
     * Num?ro identifiant ce satellite.
     */
    private int id;

    /**
     * Code sur 2 lettres du satellite.
     */
    private final String code;

    /**
     * Nom long du satellite.
     */
    private final String name;

    /**
     * Construit un nouveau satellite portant le nom sp?cifi?.
     * Ce constructeur priv? n'est appel? que par {@link #get(int)}
     * pour construire un nouveau satellite la premi?re fois o? il
     * est demand?.
     */
    private Satellite(final String code, final String name)
    {
        this.code = code;
        this.name = name;
    }

    /**
     * Retourne un satellite pour le num?ro sp?cifi?. Si aucun satellite n'est
     * d?finie pour le num?ro, alors cette m?thode retourne <code>null</code>.
     */
    private static synchronized Satellite getInternal(final int id)
    {
        Satellite sat = SATELLITES[id];
        if (sat == null)
        {
            switch (id)
            {
                case  0: sat=new Satellite("TN", "TIROS N"); break;                         
                case  2: sat=new Satellite("NB", "NOAA B" ); break;
                case  6: sat=new Satellite("NA", "NOAA 6" ); break;                         
                case  7: sat=new Satellite("NC", "NOAA 7" ); break;
                case  8: sat=new Satellite("NE", "NOAA 8" ); break;
                case  9: sat=new Satellite("NF", "NOAA 9" ); break;
                case 10: sat=new Satellite("NG", "NOAA 10"); break;
                case 11: sat=new Satellite("NH", "NOAA 11"); break;
                case 12: sat=new Satellite("ND", "NOAA 12"); break;
                case 13: sat=new Satellite("NI", "NOAA 13"); break;
                case 14: sat=new Satellite("NJ", "NOAA 14"); break;
                case 15: sat=new Satellite("NK", "NOAA 15"); break;                
                case 16: sat=new Satellite("NL", "NOAA 16"); break;
                case 17: sat=new Satellite("NM", "NOAA M" ); break;
            }
            if (sat != null)
            {
                sat.id = id;
                SATELLITES[id] = sat;                
            }
        }
        return sat;
    }

    /**
     * Retourne un satellite pour le num?ro sp?cifi?.
     *
     * @param  Le num?ro du satellite d?sir?.
     * @return Le satellite du num?ro sp?cifi?.
     * @throws NoSuchElementException si le num?ro sp?cifi? ne correspond pas ? un satellite connu.
     */
    public static Satellite get(final int id) throws NoSuchElementException
    {
        if (id>=0 && id<SATELLITES.length)
        {
            final Satellite sat = getInternal(id);
            if (sat != null)
            {
                return sat;
            }
        }
        throw new NoSuchElementException("Num?ro de satellite inconnu: "+id);
    }

    /**
     * Retourne un satellite pour le nom sp?cifi?. Le nom peut ?tre
     * une cha?ne de caract?res telle que "NOAA 16" or "NL".
     */
    public static Satellite get(String name) throws NoSuchElementException
    {
        name = name.trim().toUpperCase();
        for (int i=0; i<SATELLITES.length; i++)
        {
            final Satellite sat = getInternal(i);
            if (sat!=null && (name.equals(sat.name) || name.equals(sat.code)))
            {
                return sat;
            }
        }
        throw new NoSuchElementException("Nom de satellite inconnu: "+name);
    }

    /**
     * Retourne le num?ro identifiant ce satellite.
     */
    public int getID()
    {
        return id;
    }

    /**
     * Retourne le nom de code de ce satellite, tel qu'il appara?t
     * dans les noms de fichiers tel que "satpos_noaa16_20020222.txt".
     */
    final String getCodeName()
    {
        final StringBuffer buffer = new StringBuffer("noaa");
        if (id>=0 && id<10)
        {
            buffer.append('0');
        }
        buffer.append(id);
        return buffer.toString();
    }

    /**
     * Indique si ce satellite est NOAA 15, 16 ou 17. Ces satellites
     * ont un format de donn?es diff?rent des satellites pr?c?dents.
     */
    public final boolean isKLM()
    {
        return id >= 15;
    }

    /**
     * Retourne le nom de ce satellite.
     */
    public String toString()
    {
        return name;
    }

    /**
     * Retourne un code "hash value" pour ce satellite.
     */
    public int hashCode()
    {
        return id;
    }

    /**
     * Indique si ce satellite est identique ? l'objet sp?cifi?.
     * L'impl?mentation par d?faut ne v?rifie que les num?ros ID,
     * qui sont sens? ?tre unique pour chaque satellite.
     */
    public boolean equals(final Object other)
    {
        return (other instanceof Satellite) && ((Satellite) other).id == id;
    }    
}