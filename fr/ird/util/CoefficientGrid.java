/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.util;

// J2SE.
import java.util.Arrays;

/**
 * Définie une grille d'enregistrements. Chaque enregitrement est composé d'un nombre 
 * fixe d'éléments flottants (tableau à deux dimensions). 
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class CoefficientGrid 
{  
    /** Grille. */
    private final double[] grid;    

    /** Nombre d'enregistrements de la grille. */
    private final int size;
                   
    /** Nombre d'éléments par enregistrement. */
    private final int dimension;
    
    /**
     * Construit une grille vide. 
     *
     * @param size          Nombre d'enregitrements.
     * @param dimension     Nombre d'éléments par enregistrement.
     */
    public CoefficientGrid(final int size, final int dimension) 
    {
        this.size          = size;
        this.dimension     = dimension;
        this.grid          = new double[size * dimension];
        Arrays.fill(grid, 0);
    }

    /**
     * Calcule l'indice d'un élément dans la grille.
     *
     * @param  index  Indice de l'élément.
     * @return l'indice du début de l'élément dans la grille.
     */
    private int computeOffset(final int index) 
    {
        if (index<0 || index>=size) 
            throw new IndexOutOfBoundsException(String.valueOf(index));
        return index * dimension;
    }

    /**
     * Retourne le nombre d'enregistrements.
     * @return le nombre d'enregistrements.
     */
    public int getsize() 
    {
        return size;
    }
    
    /**
     * Retourne le nombre d'éléments par enregistrement.
     * @return le nombre d'éléments par enregistrement.
     */
    public int getDimension() 
    {
        return dimension;
    }

    /**
     * Retourne l'enregistrement désiré. 
     *
     * @param  index    Indice de l'enregistrement.
     * @return l'enregistrement désiré.
     */
    public synchronized double[] getRecord(final int index) 
    {        
        final int offset = computeOffset(index);
        final double[] tgt = new double[dimension];
        for (int i=0; i<dimension ; i++)
            tgt[i] = grid[offset + i];
        return tgt;
    }

    /**
     * Affecte l'enregitrement à la position <CODE>index</CODE>.
     *
     * @param  index   Index de l'enregistrement dans la grille.
     * @param  record  Enregistrement à affecter.
     */
    public void setElement(final int index, final double[] record) 
    {
        final int offset = computeOffset(index);
        for (int i=0 ; i<dimension ; i++)
            grid[offset + i] = record[i];
    }        
}