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

// J2SE dependencies
import java.util.NoSuchElementException;


/**
 * Informations à propos d'un satellite. Chaque satellite porte un nom
 * (par exemple "NOAA 16") ainsi qu'un numéro l'identifiant. Des méthodes
 * statiques <code>get(...)</code> permettent d'obtenir un satellite à
 * partir de son nom ou de son numéro.
 * 
 * @version $Id$
 * @author Remi EVE
 * @author Martin Desruisseaux
 */
final class Satellite
{
    /**
     * Les noms des satellites.
     */
    private final static String[] NAMES = new String[18];
    static
    {
        NAMES[ 0] = "TIROS N";
        NAMES[ 2] = "NOAA B";
        NAMES[ 6] = "NOAA 6";
        NAMES[ 7] = "NOAA 7";
        NAMES[ 8] = "NOAA 8";
        NAMES[ 9] = "NOAA 9";
        NAMES[10] = "NOAA 10";
        NAMES[11] = "NOAA 11";
        NAMES[12] = "NOAA 12";
        NAMES[13] = "NOAA 13";
        NAMES[14] = "NOAA 14";
        NAMES[15] = "NOAA 15";
        NAMES[16] = "NOAA 16";
        NAMES[17] = "NOAA M";
    }

    /**
     * Liste des satellites déjà construit. Cette liste sera remplie par
     * {@link #get(int)} au fur et à mesure que des satellites seront demandées.
     */
    private static final Satellite[] satellites = new Satellite[NAMES.length];

    /**
     * Numéro identifiant ce satellite. Le nom du satellite
     * pourra être obtenus avec <code>NAMES[id]</code>.
     */
    private final int id;

    /**
     * Construit un nouveau satellite portant le numéro spécifié.
     * Ce constructeur privé n'est appelé que par {@link #get(int)}
     * pour construire un nouveau satellite la première fois où il
     * est demandé.
     */
    private Satellite(final int id)
    {
        this.id = id;
    }

    /**
     * Retourne un satellite pour le numéro spécifié.
     *
     * @param  Le numéro du satellite désiré.
     * @return Le satellite du numéro spécifié.
     * @throws NoSuchElementException si le numéro spécifié ne correspond pas à un satellite connu.
     */
    public static synchronized Satellite get(final int id) throws NoSuchElementException
    {
        if (id<0 || id>=NAMES.length || NAMES[id]==null)
        {
            throw new NoSuchElementException("Numéro de satellite inconnu: "+id);
        }
        if (satellites[id] == null)
        {
             satellites[id] = new Satellite(id);
        }
        return satellites[id];
    }

    /**
     * Retourne un satellite pour le nom spécifié. Le nom peut être
     * une chaîne de caractères telle que "NOAA 16".
     */
    public static Satellite get(String name) throws NoSuchElementException
    {
        name = name.trim().toUpperCase();
        for (int i=0; i<NAMES.length; i++)
        {
            if (name.equals(NAMES[i]))
            {
                return get(i);
            }
        }
        throw new NoSuchElementException("Nom de satellite inconnu: "+name);
    }

    /**
     * Retourne le numéro identifiant ce satellite.
     */
    public int getID()
    {
        return id;
    }

    /**
     * Retourne le nom de ce satellite.
     */
    public String getName()
    {
        return NAMES[id];
    }

    /**
     * Retourne le nom de code de ce satellite, tel qu'il apparaît
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
}
