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
package net.seas.util;

// Divers
import java.io.Serializable;
import java.awt.geom.Dimension2D;


/** 
 * Apporte quelques fonctionalitées qui manquent à la classe {@link Dimension2D}
 * standard du Java. Cette classe n'est que temporaire. Elle disparaîtra dans une
 * version future si <em>JavaSoft</em> définit des classes <code>Dimension2D.Float</code>
 * et <code>Dimension2D.Double</code>.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class XDimension2D
{
    /**
     * Interdit la création d'objets
     * de cette classe.
     */
    private XDimension2D()
    {}

    /**
     * Objet {@link Dimension2D} utilisant des valeurs de type <code>float</code>.
     * Cette classe n'est que temporaire. Elle disparaîtra dans une version future si
     * <em>JavaSoft</em> définit une classe <code>Dimension2D.Float</code> (RFE #4189647).
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static final class Float extends Dimension2D implements Serializable
    {
        /**
         * Largeur de la dimension.
         */
        public float width;

        /**
         * Hauteur de la dimension.
         */
        public float height;

        /**
         * Construit un objet avec les dimensions (0,0).
         */
        public Float()
        {}

        /**
         * Construit un objet avec les dimensions spécifiées.
         * @param w largeur.
         * @param h hauteur.
         */
        public Float(final float w, final float h)
        {width=w; height=h;}

        /**
         * Change les dimensions de cet objet.
         * @param w largeur.
         * @param h hauteur.
         */
        public void setSize(final double w, final double h)
        {width=(float) w; height=(float) h;}

        /**
         * Retourne la largeur.
         */
        public double getWidth()
        {return width;}

        /**
         * Retourne la hauteur.
         */
        public double getHeight()
        {return height;}

        /**
         * Retourne la dimension sous forme de chaîne de caractères.
         * La chaîne sera de la forme "<code>Dimension2D[45,76]</code>".
         */
        public String toString()
        {return "Dimension2D["+width+','+height+']';}
    }

    /**
     * Objet {@link Dimension2D} utilisant des valeurs de type <code>double</code>.
     * Cette classe n'est que temporaire. Elle disparaîtra dans une version future si
     * <em>JavaSoft</em> définit une classe <code>Dimension2D.Double</code> (RFE #4189647).
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static final class Double extends Dimension2D implements Serializable
    {
        /**
         * Largeur de la dimension.
         */
        public double width;

        /**
         * Hauteur de la dimension.
         */
        public double height;

        /**
         * Construit un objet avec les dimensions (0,0).
         */
        public Double()
        {}

        /**
         * Construit un objet avec les dimensions spécifiées.
         * @param w largeur.
         * @param h hauteur.
         */
        public Double(final double w, final double h)
        {width=w; height=h;}

        /**
         * Change les dimensions de cet objet.
         * @param w largeur.
         * @param h hauteur.
         */
        public void setSize(final double w, final double h)
        {width=w; height=h;}

        /**
         * Retourne la largeur.
         */
        public double getWidth()
        {return width;}

        /**
         * Retourne la hauteur.
         */
        public double getHeight()
        {return height;}

        /**
         * Retourne la dimension sous forme de chaîne de caractères.
         * La chaîne sera de la forme "<code>Dimension2D[45,76]</code>".
         */
        public String toString()
        {return "Dimension2D["+width+','+height+']';}
    }
}
