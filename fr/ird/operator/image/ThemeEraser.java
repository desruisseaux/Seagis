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
 * Opération qui filtrera un thème dans les images.
 * Cette opération sert notamment à supprimer le quadrillage des cartes SST.
 * La décision d'incorporer à certaines images une grille de latitudes et de
 * longitudes est une décision malheureuse qui nuit à l'affichage et aux zooms.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ThemeEraser
{
    /**
     * Nom de la catégorie {@link Category} à faire disparaître.
     */
    private final String toFilter;

    /**
     * Nombre de pixels à examiner dans le voisinage
     * des pixels à faire disparaître (habituellement 1).
     */
    private final int neighbor;

    /**
     * Construit une opération qui filtrera un thème dans les images. Le thème ne doit pas
     * être un paramètre géophysique. Il s'agira le plus souvent d'un grillage que l'on
     * souhaite faire disparaître.
     *
     * @param  toFilter Nom du theme {@link Category} à faire disparaître.
     * @param  neighbor Nombre de pixels à examiner dans le voisinage des
     *                  pixels à filtrer (habituellement 1).
     */
    public ThemeEraser(final String toFilter, final int neighbor)
    {
        this.toFilter = toFilter;
        this.neighbor = neighbor;
    }

    /**
     * Filtre l'image spécifiée.
     *
     * @param  image Image à filtrer
     * @return image filtrée.
     */
    public static void main(final String[] args) throws IOException
    {
        final int toReplace =  12; // Valeur des pixels à remplacer
        final int lower     =   0; // Début (inclusive) de la plage de valeurs du paramètre géophysique.
        final int upper     =   0; // fin   (exclusive) de la plage de valeurs du paramètre géophysique.
        final int band      =   0; // Bande à traiter.
        final int neighbor  =   4; // Nombre de pixel de voisinage à prendre en compte.

        RenderedImage image = ImageIO.read(new File(args[0]));
        image = new Operation(image, band, neighbor, lower, upper, toReplace);
        ImageIO.write(image, "png", new File(args[1]));
    }

    /**
     * Filtre l'image spécifiée.
     *
     * @param  image Image à filtrer
     * @return image filtrée.
     */
//  protected RemoteImage doFilter(final RemoteImage image)
//  {
//      int toReplace = -1; // Valeur des pixels à remplacer
//      int lower     = -1; // Début (inclusive) de la plage de valeurs du paramètre géophysique.
//      int upper     = -1; // fin   (exclusive) de la plage de valeurs du paramètre géophysique.
//      final ThemeMapper mapper = image.getThemeMapper();
//      final int           band = (mapper instanceof IndexedThemeMapper) ? ((IndexedThemeMapper) mapper).getBands()[0] : 0;
//      final Theme[]     themes = mapper.getThemes();
//      for (int i=0; i<themes.length; i++)
//      {
//          final Theme theme=themes[i];
            /*
             * Obtient l'index des valeurs de pixels à remplacer.  Il s'agira de la valeur
             * représentée par le thème <code>toFilter</code>. Ce thème ne doit apparaître
             * qu'une seule fois dans la liste des thèmes,   et ne doit représenter qu'une
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
             * Obtient les index des valeurs géophysiques. L'implémentation actuelle n'accepte
             * qu'une seule plage de valeurs géophysiques. De plus, elle exige que les index
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
         * Retourne l'image filtrée, ou l'image
         * inchangée s'il n'y a rien à filtrer.
         */
//      if (toReplace<0) return image;
//      if (upper<=lower) lower=upper=0;
//      return image.replace(new Operation(image.getImage(), band, neighbor, lower, upper, toReplace));
//  }




    /**
     * Opération remplaçant certains pixel par la moyenne des pixels l'entourant.
     * Cette opération sert notamment à supprimer le quadrillage des cartes SST.
     * La décision d'incorporer à certaines images une grille de latitudes et de
     * longitudes est une décision malheureuse qui nuit à l'affichage et aux zooms.
     * Cette opération aide à supprimer une telle grille en effectuant une des
     * opérations suivantes sur tous les pixels qui ont la valeur <code>toReplace</code>:
     *
     * <ul>
     *   <li>Si le pixel est entouré d'au moins une donnée (une <em>donnée</em> est
     *       définie comme étant un pixel dont la valeur est comprise entre <code>lower</code>
     *       et <code>upper</code>), remplace la valeur de ce pixel par la valeur moyenne des
     *       données qui l'entourent.</li>
     *   <li>Si le pixel n'est entouré d'aucune donnée (par exemple s'il se trouve au milieu
     *       d'un nuage), remplace la valeur de ce pixel par la valeur la plus fréquente des
     *       pixels qui l'entourent.<li>
     * </ul>
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Operation extends AreaOpImage
    {
        /**
         * Numéro de la bande à traiter. En général, ce sera la bande 0.
         */
        private final int band;

        /**
         * Nombre de pixels constituant le "voisinage" d'un pixel. Cet argument sera typiquement 1.
         */
        private final int size;

        /**
         * Valeur minimale (inclusive) des pixels représentant des données.
         */
        private final int lower;

        /**
         * Valeur maximale (exclusive) des pixels représentant des données.
         */
        private final int upper;

        /**
         * Valeur des pixels à remplacer.
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
         * Buffer temporaire pour déterminer
         * le pixel le plus fréquent.
         */
        private int[] histogram;

        /**
         * Construit un opérateur qui supprimera le grillage de l'image spécifiée.
         *
         * @param source    Image dans laquelle effectuer la suppression du quadrillage.
         * @param band      Numéro de la bande à traiter. En général, ce sera la bande 0.
         * @param size      Nombre de pixels constituant le "voisinage" d'un pixel. Cet argument sera typiquement 1.
         * @param lower     Valeur minimale (inclusive) des pixels représentant des données.
         * @param upper     Valeur maximale (exclusive) des pixels représentant des données.
         * @param toReplace Valeur des pixels à remplacer.
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
         * Procède au filtrage. Cette méthode est appelée automatiquement
         * par <em>Java Advanced Imaging</em> pour effectuer le filtrage.
         *
         * @param source   Données source. Ce tableau aura normalement une longueur de 1.
         * @param dest     Image dans laquelle enregistrer le résultat.
         * @param destRect Coordonnées de la région à traiter.
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
                         * On a trouvé un pixel à remplacer. Calcule la moyenne
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
                             * On n'a trouvé aucune valeur avoisinant le pixel.
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
