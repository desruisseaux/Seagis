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
package fr.ird.operator.image;

// Images (JDK 1.4)
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import javax.imageio.ImageIO;

// Images (JAI)
import javax.media.jai.JAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.OperationDescriptorImpl;

// Miscellaneous
import java.io.File;
import java.util.Arrays;
import java.awt.Rectangle;
import java.io.IOException;


/**
 * Op�ration qui filtrera un th�me dans les images.
 * Cette op�ration sert notamment � supprimer le quadrillage des cartes SST.
 * La d�cision d'incorporer � certaines images une grille de latitudes et de
 * longitudes est une d�cision malheureuse qui nuit � l'affichage et aux zooms.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ThemeEraser
{
    /**
     * Nom de la cat�gorie {@link Category} � faire dispara�tre.
     */
    private final String toFilter;

    /**
     * Nombre de pixels � examiner dans le voisinage
     * des pixels � faire dispara�tre (habituellement 1).
     */
    private final int neighbor;

    /**
     * Construit une op�ration qui filtrera un th�me dans les images. Le th�me ne doit pas
     * �tre un param�tre g�ophysique. Il s'agira le plus souvent d'un grillage que l'on
     * souhaite faire dispara�tre.
     *
     * @param  toFilter Nom du theme {@link Category} � faire dispara�tre.
     * @param  neighbor Nombre de pixels � examiner dans le voisinage des
     *                  pixels � filtrer (habituellement 1).
     */
    public ThemeEraser(final String toFilter, final int neighbor)
    {
        this.toFilter = toFilter;
        this.neighbor = neighbor;
    }

    /**
     * Filtre l'image sp�cifi�e.
     *
     * @param  image Image � filtrer
     * @return image filtr�e.
     */
    public static void main(final String[] args) throws IOException
    {
        final int toReplace =  12; // Valeur des pixels � remplacer
        final int lower     =   0; // D�but (inclusive) de la plage de valeurs du param�tre g�ophysique.
        final int upper     =   0; // fin   (exclusive) de la plage de valeurs du param�tre g�ophysique.
        final int band      =   0; // Bande � traiter.
        final int neighbor  =   4; // Nombre de pixel de voisinage � prendre en compte.

        RenderedImage image = ImageIO.read(new File(args[0]));
        image = new Operation(image, band, neighbor, lower, upper, toReplace);
        ImageIO.write(image, "png", new File(args[1]));
    }

    /**
     * Filtre l'image sp�cifi�e.
     *
     * @param  image Image � filtrer
     * @return image filtr�e.
     */
//  protected RemoteImage doFilter(final RemoteImage image)
//  {
//      int toReplace = -1; // Valeur des pixels � remplacer
//      int lower     = -1; // D�but (inclusive) de la plage de valeurs du param�tre g�ophysique.
//      int upper     = -1; // fin   (exclusive) de la plage de valeurs du param�tre g�ophysique.
//      final ThemeMapper mapper = image.getThemeMapper();
//      final int           band = (mapper instanceof IndexedThemeMapper) ? ((IndexedThemeMapper) mapper).getBands()[0] : 0;
//      final Theme[]     themes = mapper.getThemes();
//      for (int i=0; i<themes.length; i++)
//      {
//          final Theme theme=themes[i];
            /*
             * Obtient l'index des valeurs de pixels � remplacer.  Il s'agira de la valeur
             * repr�sent�e par le th�me <code>toFilter</code>. Ce th�me ne doit appara�tre
             * qu'une seule fois dans la liste des th�mes,   et ne doit repr�senter qu'une
             * seule valeur de pixel.
             */
//          if (theme.getName().equals(toFilter))
//          {
//              if (!theme.isQuantifiable() && (theme instanceof IndexedTheme) && toReplace<0)
//              {
//                  final IndexedTheme indexed = (IndexedTheme) theme;
//                  if (indexed.lower==indexed.upper)
//                  {
//                      toReplace = (int) indexed.lower;
//                      if (toReplace>=0 && indexed.lower==toReplace) // Check rounding error.
//                          continue;
//                  }
//              }
//              throw new IllegalArgumentException(toFilter);
//          }
            /*
             * Obtient les index des valeurs g�ophysiques. L'impl�mentation actuelle n'accepte
             * qu'une seule plage de valeurs g�ophysiques. De plus, elle exige que les index
             * soient entiers.
             */
//          if (theme.isQuantifiable())
//          {
//              if ((theme instanceof IndexedTheme) && lower==upper)
//              {
//                  final IndexedTheme indexed = (IndexedTheme) theme;
//                  lower = (int) indexed.lower;
//                  upper = (int) indexed.upper;
//                  if (lower>=0 && upper>=lower && lower==indexed.lower && upper==indexed.upper) // Check rounding error
//                  {
//                      upper++; // Pour 'GridRemoveOperation', 'upper' est exclu.
//                      continue;
//                  }
//              }
//              throw new UnsupportedOperationException(theme.toString());
//          }
//      }
        /*
         * Retourne l'image filtr�e, ou l'image
         * inchang�e s'il n'y a rien � filtrer.
         */
//      if (toReplace<0) return image;
//      if (upper<=lower) lower=upper=0;
//      return image.replace(new Operation(image.getImage(), band, neighbor, lower, upper, toReplace));
//  }




    /**
     * Op�ration rempla�ant certains pixel par la moyenne des pixels l'entourant.
     * Cette op�ration sert notamment � supprimer le quadrillage des cartes SST.
     * La d�cision d'incorporer � certaines images une grille de latitudes et de
     * longitudes est une d�cision malheureuse qui nuit � l'affichage et aux zooms.
     * Cette op�ration aide � supprimer une telle grille en effectuant une des
     * op�rations suivantes sur tous les pixels qui ont la valeur <code>toReplace</code>:
     *
     * <ul>
     *   <li>Si le pixel est entour� d'au moins une donn�e (une <em>donn�e</em> est
     *       d�finie comme �tant un pixel dont la valeur est comprise entre <code>lower</code>
     *       et <code>upper</code>), remplace la valeur de ce pixel par la valeur moyenne des
     *       donn�es qui l'entourent.</li>
     *   <li>Si le pixel n'est entour� d'aucune donn�e (par exemple s'il se trouve au milieu
     *       d'un nuage), remplace la valeur de ce pixel par la valeur la plus fr�quente des
     *       pixels qui l'entourent.<li>
     * </ul>
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Operation extends AreaOpImage
    {
        /**
         * Num�ro de la bande � traiter. En g�n�ral, ce sera la bande 0.
         */
        private final int band;

        /**
         * Nombre de pixels constituant le "voisinage" d'un pixel. Cet argument sera typiquement 1.
         */
        private final int size;

        /**
         * Valeur minimale (inclusive) des pixels repr�sentant des donn�es.
         */
        private final int lower;

        /**
         * Valeur maximale (exclusive) des pixels repr�sentant des donn�es.
         */
        private final int upper;

        /**
         * Valeur des pixels � remplacer.
         */
        private final int toReplace;

        /**
         * Buffer temporaire pour contenir
         * toutes les bandes d'un pixel.
         */
        private int[] pixel;

        /**
         * Buffer temporaire dans lequel placer
         * des pixels en cours de traitement.
         */
        private int[] banks;

        /**
         * Buffer temporaire pour d�terminer
         * le pixel le plus fr�quent.
         */
        private int[] histogram;

        /**
         * Construit un op�rateur qui supprimera le grillage de l'image sp�cifi�e.
         *
         * @param source    Image dans laquelle effectuer la suppression du quadrillage.
         * @param band      Num�ro de la bande � traiter. En g�n�ral, ce sera la bande 0.
         * @param size      Nombre de pixels constituant le "voisinage" d'un pixel. Cet argument sera typiquement 1.
         * @param lower     Valeur minimale (inclusive) des pixels repr�sentant des donn�es.
         * @param upper     Valeur maximale (exclusive) des pixels repr�sentant des donn�es.
         * @param toReplace Valeur des pixels � remplacer.
         */
        public Operation(final RenderedImage source, final int band, final int size, final int lower, final int upper, final int toReplace)
        {
            super(source, new ImageLayout(source), JAI.getDefaultInstance().getRenderingHints(), true, new BorderExtenderConstant(new double[] {toReplace}), size, size, size, size);
            this.band      = band;
            this.size      = size;
            this.lower     = lower;
            this.upper     = upper;
            this.toReplace = toReplace;
        }

        /**
         * Retourne un tableau de constantes pour {@link BorderExtenderConstant}.
         */
        private double[] getConstants(final int band, final int toReplace)
        {
            final double constants[]=new double[band+1];
            constants[band]=toReplace;
            return constants;
        }

        /**
         * Proc�de au filtrage. Cette m�thode est appel�e automatiquement
         * par <em>Java Advanced Imaging</em> pour effectuer le filtrage.
         *
         * @param source   Donn�es source. Ce tableau aura normalement une longueur de 1.
         * @param dest     Image dans laquelle enregistrer le r�sultat.
         * @param destRect Coordonn�es de la r�gion � traiter.
         */
        protected void computeRect(final Raster[] sources, final WritableRaster dest, final Rectangle destRect)
        {
            final Raster   source = sources[0];
            final int        band = this.band;
            final int       lower = this.lower;
            final int       upper = this.lower;
            final int   toReplace = this.toReplace;
            final int        xmax = destRect.x+destRect.width;
            final int        ymax = destRect.y+destRect.height;
            final int        size = this.size;
            final int      length = (size*2+1);
            final int     length2 = length*length;
                  int[]     pixel = this.pixel;
                  int[]     banks = this.banks;
                  int[] histogram = this.histogram;
            for (int y=destRect.y; y<ymax; y++)
            {
                for (int x=destRect.x; x<xmax; x++)
                {
                    pixel=source.getPixel(x,y,pixel);
                    if (pixel[band]==toReplace)
                    {
                        /*
                         * On a trouv� un pixel � remplacer. Calcule la moyenne
                         * des pixels entourant celui que l'on vient de trouver.
                         */
                        int sum=0,n=0;
                        banks=source.getSamples(x-size, y-size, length, length, band, banks);
                        for (int i=length2; --i>=0;)
                        {
                            final int value=banks[i];
                            if (i>=lower && i<upper)
                            {
                                sum += value;
                                n++;
                            }
                        }
                        if (n!=0)
                        {
                            pixel[band] = Math.round(((float) sum)/n);
                        }
                        else
                        {
                            /*
                             * On n'a trouv� aucune valeur avoisinant le pixel.
                             * On va donc rechercher le pixel le plus courant.
                             */
                            if (histogram==null)
                            {
                                histogram=new int[length2];
                            }
                            Arrays.fill(histogram, 0);
                            for (int i=length2; --i>=0;)
                            {
                                final int value=banks[i];
                                if (value!=toReplace)
                                {
                                    for (int j=length2; --j>=0;)
                                    {
                                        if (banks[j]==value)
                                        {
                                            histogram[j]++;
                                            break;
                                        }
                                    }
                                }
                            }
                            int max=0;
                            for (int i=length2; --i>=0;)
                            {
                                final int c=histogram[i];
                                if (c>max)
                                {
                                    max=c;
                                    pixel[band]=banks[i];
                                }
                            }
                        }
                    }
                    dest.setPixel(x,y,pixel);
                }
            }
            this.histogram = histogram;
            this.banks     = banks;
            this.pixel     = pixel;
        }
    }
}
