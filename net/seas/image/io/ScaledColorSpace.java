/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.image.io;

// Couleurs
import java.awt.color.ColorSpace;


/**
 * Espace de couleurs pour les images dont les valeurs de pixels se situent entre deux
 * nombre réels. Cette classe enveloppe un autre espace de couleurs {@link ColorSpace},
 * mais transformera toutes les valeurs de pixels pour que la plage spécifiée au
 * constructeur soit représentable dans la plage de valeurs du {@link ColorSpace}
 * enveloppé.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ScaledColorSpace extends ColorSpace
{
    /**
     * Espace de couleur enveloppé.
     */
    private final ColorSpace proxy;

    /**
     * Facteur par lequel multiplier les pixels.
     */
    private final float scale;

    /**
     * Nombre à aditionner aux pixels après
     * les avoir multiplier par {@link #scale}.
     */
    private final float offset;

    /**
     * Tableau temporaire pour les conversions.
     */
    private final float[] array;

    /**
     * Construit un modèle de couleurs.
     *
     * @param minimum Valeur minimale des nombres réels à décoder.
     * @param maximum Valeur maximale des nombres réels à décoder.
     */
    public ScaledColorSpace(final float minimum, final float maximum)
    {this(ColorSpace.getInstance(CS_GRAY), minimum, maximum);}

    /**
     * Construit un modèle de couleurs.
     *
     * @param colors  Un espace de couleurs de type gris.
     * @param minimum Valeur minimale des nombres réels à décoder.
     * @param maximum Valeur maximale des nombres réels à décoder.
     */
    private ScaledColorSpace(final ColorSpace colors, final float minimum, final float maximum)
    {
        super(TYPE_GRAY, colors.getNumComponents());
        final float proxyMin =  colors.getMinValue(0);
        final float proxyMax =  colors.getMaxValue(0);
        this.proxy   = colors;
        this.scale   = (proxyMax-proxyMin)/(maximum-minimum);
        this.offset  = minimum + proxyMin/scale;
        this.array   = new float[getNumComponents()];
    }

    /**
     * Convertit les valeurs du tableau spécifié en valeurs
     * valides pour le modèle de couleur {@link #proxy}.
     */
    private float[] convert(final float[] input)
    {
        for (int i=array.length; --i>=0;)
        {
            float value = input[i]*scale + offset;
            if (Float.isNaN(value)) value= offset;
            array[i] = value;
        }
        return array;
    }

    /**
     * Effectue une conversion inverse à partir
     * des valeurs retournées par {@link #proxy}.
     */
    private float[] inverseConvert(final float[] output)
    {
        for (int i=output.length; --i>=0;)
            output[i] = (output[i]-offset)/scale;
        return output;
    }

    /**
     * Normalise les valeurs de couleurs spécifiées,
     * puis convertit les valeurs dans l'espace RGB.
     */
    public float[] toRGB(final float[] colorvalue)
    {return proxy.toRGB(convert(colorvalue));}
    
    /**
     * Convertit les valeurs de couleurs à partir de l'espace RGB,
     * puis étend les valeurs normalisées sur la plage de valeurs
     * de cet espace de couleurs.
     */
    public float[] fromRGB(final float[] rgbvalue)
    {return inverseConvert(proxy.fromRGB(rgbvalue));}
    
    /**
     * Normalise les valeurs de couleurs spécifiées,
     * puis convertit les valeurs dans l'espace CIEXYZ.
     */
    public float[] toCIEXYZ(final float[] colorvalue)
    {return proxy.toCIEXYZ(convert(colorvalue));}
    
    /**
     * Convertit les valeurs de couleurs à partir de l'espace CIEXYZ,
     * puis étend les valeurs normalisées sur la plage de valeurs
     * de cet espace de couleurs.
     */
    public float[] fromCIEXYZ(float[] colorvalue)
    {return inverseConvert(proxy.fromCIEXYZ(colorvalue));}

    /**
     * Retourne la valeur minimale autorisée.
     */
    public float getMinValue(final int component)
    {return (proxy.getMinValue(component)-offset)/scale;}

    /**
     * Retourne la valeur maximale autorisée.
     */
    public float getMaxValue(final int component)
    {return (proxy.getMaxValue(component)-offset)/scale;}
}
