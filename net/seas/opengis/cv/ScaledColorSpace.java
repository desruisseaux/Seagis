/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.cv;

// Miscellaneous
import net.seas.util.XClass;
import java.awt.color.ColorSpace;
import javax.media.jai.util.Range;


/**
 * Espace de couleurs pour les images dont les valeurs
 * de pixels se situent entre deux nombre r�els.
 *
 * NOTE: Actual implementation is a copy of net.seas.image.io.ScaledColorSpace.
 *       Future implementation will be differents (interpolate in a color table
 *       instead of computing grayscales).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ScaledColorSpace extends ColorSpace
{
    /**
     * Minimal normalized RGB value.
     */
    private static final float MIN_VALUE = 0f;

    /**
     * Maximal normalized RGB value.
     */
    private static final float MAX_VALUE = 1f;

    /**
     * Facteur par lequel multiplier les pixels.
     */
    private final float scale;

    /**
     * Nombre � aditionner aux pixels apr�s
     * les avoir multiplier par {@link #scale}.
     */
    private final float offset;

    /**
     * Construit un mod�le de couleurs.
     *
     * @param numComponents Nombre de composante (seule la premi�re sera prise en compte).
     * @param range La plage de valeurs g�ophysiques, ou <code>null</code> s'il n'y en a pas.
     */
    public ScaledColorSpace(final int numComponents, final Range range)
    {
        super(TYPE_GRAY, numComponents);
        double minimum = 0;
        double maximum = 1;
        if (range!=null)
        {
            final Comparable minNumber = range.getMinValue();
            final Comparable maxNumber = range.getMaxValue();
            if (minNumber instanceof Number) minimum = ((Number)minNumber).doubleValue();
            if (maxNumber instanceof Number) maximum = ((Number)maxNumber).doubleValue();
        }
        final double scale  = (maximum-minimum)/(MAX_VALUE-MIN_VALUE);
        final double offset = minimum - MIN_VALUE*scale;
        this.scale  = (float)scale;
        this.offset = (float)offset;
    }

    /**
     * Retourne une couleur RGB en tons de
     * gris pour le nombre r�el sp�cifi�.
     */
    public float[] toRGB(final float[] values)
    {
        float value = (values[0]-offset)/scale;
        if (Float.isNaN(value)) value=MIN_VALUE;
        return new float[] {value, value, value};
    }
    
    /**
     * Retourne une valeur r�elle pour
     * le ton de gris sp�cifi�.
     */
    public float[] fromRGB(final float[] RGB)
    {
        final float[] values = new float[getNumComponents()];
        values[0] = (RGB[0]+RGB[1]+RGB[2])/3*scale + offset;
        return values;
    }
    
    /**
     * Convertit les valeurs en couleurs dans l'espace CIEXYZ.
     */
    public float[] toCIEXYZ(final float[] values)
    {
        float value = (values[0]-offset)/scale;
        if (Float.isNaN(value)) value=MIN_VALUE;
        return new float[]
        {
            value*0.9642f,
            value*1.0000f,
            value*0.8249f
        };
    }
    
    /**
     * Convertit les couleurs de l'espace CIEXYZ en valeurs.
     */
    public float[] fromCIEXYZ(final float[] RGB)
    {
        final float[] values = new float[getNumComponents()];
        values[0] = (RGB[0]/0.9642f + RGB[1] + RGB[2]/0.8249f)/3*scale + offset;
        return values;
    }

    /**
     * Retourne la valeur minimale autoris�e.
     */
    public float getMinValue(final int component)
    {return MIN_VALUE*scale + offset;}

    /**
     * Retourne la valeur maximale autoris�e.
     */
    public float getMaxValue(final int component)
    {return MAX_VALUE*scale + offset;}

    /**
     * Returns a string representation of this color model.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getMinValue(0)+", "+getMaxValue(0)+']';}
}
