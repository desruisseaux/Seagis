/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.bufr;

// Divers
import java.io.Serializable;
import org.geotools.resources.XMath;


/**
 * Param�tre cod� dans la section 3 des fichiers BUFR. Pour convertir un entier
 * binaire <var>b</var> en valeur g�ophysique <var>x</var>, il faut calculer
 *
 *           <code>(b+reference)/(10^scale)</code>.
 */
final class Descriptor implements Serializable
{
    /** Nom du param�tre.             */ public final String   name;
    /** Unit�s.                       */ public final String  units;
    /** Puissance de 10 de l'�chelle. */ public final int     scale;
    /** Valeur binaire minimale.      */ public final int reference;
    /** Nombre de bits.               */ public final int     width;

    /**
     * Construit un param�tre.
     */
    public Descriptor(final String name, final String units, final int scale, final int reference, final int width)
    {
        this.name      = name .trim();
        this.units     = units.trim();
        this.scale     = scale;
        this.reference = reference;
        this.width     = width;
    }

    /**
     * Transforme une valeur g�ophysique en valeur cod�e dans le fichier BUFR.
     * Les valeurs {@link Float#NaN} sont cod�es comme valeurs manquantes.
     */
    public long encore(final float value)
    {
        if (Float.isNaN(value)) return (1L << width)-1;
        return Math.round(value * XMath.pow10(scale)) - reference;
    }

    /**
     * Transforme une valeur cod�e dans le fichier BUFR en valeur g�ophysique.
     * Les valeurs manquantes sont retourn�es comme valeur {@link Float#NaN}.
     */
    public float decode(final long value)
    {
        if (value == (1L << width)-1) return Float.NaN;
        return (float) ((value + reference) / XMath.pow10(scale));
    }

    /**
     * Retourne un descripteur semblable � <code>this</code>, mais o� les valeurs
     * <code>scale</code> et <code>width</code> ont �t� ajout�es aux champs du m�me
     * nom.
     */
    final Descriptor rescale(final int scale, final int width)
    {
        if (scale==0 && width==0) return this;
        return new Descriptor(name, units, this.scale+scale, reference, this.width+width);
    }

    /**
     * Retourne une cha�ne de caract�re
     * repr�sentant ce param�tre.
     */
    public String toString()
    {return name.toLowerCase()+" ("+units+')';}
}
