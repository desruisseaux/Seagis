package fr.ird.util;

// J2SE.
import java.util.Arrays;

// GEOTOOLS.
import org.geotools.pt.CoordinatePoint;

/**
 * D�finie une grille d'enregistrements. Chaque enregitrement est compos� d'un nombre 
 * fixe d'�l�ments flottants (tableau � deux dimensions).
 *
 * @author  Remi EVE
 * @version 1.0
 */
public final class ElementGrid 
{  
    /** Grille. */
    private final double[] grid;    

    /** Nombre d'enregistrements de la grille. */
    private final int size;
                   
    /** Nombre d'�l�ments par enregistrement. */
    private final int dimension;
    
    /**
     * Construit une grille vide. 
     *
     * @param size          Nombre d'enregitrements.
     * @param dimension     Nombre d'�l�ments par enregistrement.
     */
    public ElementGrid(final int size, final int dimension) 
    {
        this.size          = size;
        this.dimension     = dimension;
        this.grid          = new double[size * dimension];
        Arrays.fill(grid, 0);
    }

    /**
     * Calcule l'indice d'un �l�ment dans la grille.
     *
     * @param  index  Indice de l'�l�ment.
     * @return l'indice du d�but de l'�l�ment dans la grille.
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
     * Retourne le nombre d'�l�ments par enregistrement.
     * @return le nombre d'�l�ments par enregistrement.
     */
    public int getDimension() 
    {
        return dimension;
    }

    /**
     * Retourne l'enregistrement d�sir�. 
     *
     * @param  index    Indice de l'enregistrement.
     * @return l'enregistrement d�sir�.
     */
    public synchronized CoordinatePoint getRecord(final int index) 
    {
        final int offset = computeOffset(index);
        final double[] tgt = new double[dimension];
        for (int i=0; i<dimension ; i++)
            tgt[i] = grid[offset + i];
        return new CoordinatePoint(tgt);
    }

    /**
     * Affecte l'enregitrement � la position <CODE>index</CODE>.
     *
     * @param  index   Index de l'enregistrement dans la grille.
     * @param  record  Enregistrement � affecter.
     */
    public void setElement(final int index, final CoordinatePoint record) 
    {
        final int offset = computeOffset(index);
        for (int i=0 ; i<dimension ; i++)
            grid[offset + i] = record.getOrdinate(i);
    }        
}