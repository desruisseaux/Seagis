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
package fr.ird.op;

// SEAGIS
import fr.ird.n1b.image.sst.Utilities;

// JAI / J2SE
import javax.media.jai.KernelJAI;
import javax.media.jai.OpImage;
import javax.media.jai.util.Range;
import javax.media.jai.PointOpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;

/**
 * Filtre une image par rapport � un kernel. Si le <i>pattern</i> est identifi� dans l'image 
 * source, en sortie la valeur du pixel <i>Key</i> du pattern sera <CODE>pixel</CODE>. Si le 
 * pattern n'est pas identifi�, la valeur du pixel de l'image source est affect�e au pixel de 
 * l'image de sortie.<BR><BR>
 *
 * Le <i>pattern</i> est construit � partir d'un objet KernelJAI.<BR><BR>
 *
 * Exemple de <i>pattern</i> remplacant tout les pixels isol� par la valeur <i>pixel</i> :
 * <BR><BR>
 *
 * <center>
 * <table border=1>
 *  <TR>
 *      <TD>1</TD>
 *      <TD>1</TD>
 *      <TD>1</TD>
 * </TR>
 *  <TR>
 *      <TD>1</TD>
 *      <TD>0</TD>
 *      <TD>1</TD>
 * </TR>
 *  <TR>
 *      <TD>1</TD>
 *      <TD>1</TD>
 *      <TD>1</TD>
 * </TR>
 * </table>
 * </center><BR><BR>
 *
 * Avec ce <i>pattern</i>, lorsque l'image en entr�e comporte un pixel de valeur 0 entour�e 
 * par des pixels de valeurs 1, alors le pixel de l'image de sortie <i>Key</i> de coordonn�es 
 * (xKey, yKey) vaut <i>pixel</i>.
 *
 * @author  Remi EVE
 * @version $Id$
 */
public final class PatternFilter extends KernelFilter
{  
    /** Valeur du pixel <i>Key</i> lorsque le pattern est identifi�. */
    private double pixel;

    /**
     * Construit un PatternFilter.
     *
     * @param source         L'image SST source en valeur index�e.
     * @param kernel         Kernel.
     * @param pixelOut       Valeur affect�e au pixel de coordonn�e (xKey, yKey) du 
     *                       lorsque le pattern est identifi�.
     * @param layout         D�finition du type de l'image de sortie.
     * @param configuration  Configuration du traitement realise par JAI.
     */
    protected PatternFilter(final RenderedImage source, 
                            final KernelJAI     kernel, 
                            final double        pixel, 
                            final ImageLayout   layout, 
                            final Map           configuration) 
    {                                        
        super(source, kernel, null, configuration);            
    }    
    
    /**
     * Retourne un objet de type <CODE>RenderedImage</CODE> contenant une image dans laquelle
     * les pixels impliqu�s dans le pattern sont modifi�s. 
     *
     * @param source            L'image source.
     * @param pattern           Le kernel.
     * @param pixelOut          Valeur affect�e au pixel de coordonn�e (xKey, yKey) du 
     *                          pattern si le pattern est trouv�.
     * @param sample            Un sampleDimension definissant les categories associees a l'image.
     * @param bound             Limite de l'image de sortie.
     * @param configuration     Configuration du traitement realise par JAI.
     * @return un objet de type <CODE>OpImage</CODE> contenant une image dans laquelle
     *         les pixels impliqu�s dans le pattern sont modifi�s. 
     */
    public static RenderedImage get(final RenderedImage source, 
                                    final KernelJAI     pattern,
                                    final double        pixel,           
                                    final Map           configuration) 
    {
        if (source == null)
            throw new IllegalArgumentException("Source is null."); 
        if (pattern == null)
            throw new IllegalArgumentException("Pattern is null."); 
                           
        // Construction du type de l'image de sortie.
        final ImageLayout layout = new ImageLayout(source);
        
        return new PatternFilter(source, pattern, pixel, layout, configuration);
    }    
    
    /**
     * Retourne la valeur � affecter au pixel <i>Key</i>. Dans cette classe, la valeur sera
     * <i>pixel</i> si le pattern est identifi� et la valeur du pixel <i>Key</i> sinon.
     *
     * @param dataSrc La fen�tre de l'image source.
     * @return Retourne la valeur � affecter au pixel <i>Key</i>.
     */
    protected double process(final double[] dataSrc)
    {
        final double pixelKey = dataSrc[kernel.getYOrigin()*kernel.getWidth() + 
                                        kernel.getXOrigin()];
        for (int row=0 ; row<kernel.getHeight(); row++)
            for (int col=0 ; col<kernel.getWidth() ; col++)
                if (kernel.getElement(col, row) != dataSrc[row*kernel.getWidth() + col])
                    return pixelKey;
        return pixel;
    }
}