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
package net.seas.opengis.cv;

// Colors
import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

// Input/output
import java.net.URL;
import java.io.File;
import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import javax.imageio.IIOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import net.seas.text.LineFormat;

// Miscellaneous
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.ArrayList;
import net.seas.util.XArray;
import net.seas.util.Version;
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;


/**
 * A factory class for {@link IndexColorModel} objects.
 * Default implementation for this class create {@link IndexColorModel} objects from
 * palette definition files. Definition files are text files containing an arbitrary
 * number of lines, each line containing RGB components ranging from 0 to 255 inclusive.
 * Empty line and line starting with '#' are ignored. Example:
 *
 * <blockquote><pre>
 * # RGB codes for SeaWiFs images
 * # (chlorophylle-a concentration)
 *
 *   033   000   096
 *   032   000   097
 *   031   000   099
 *   030   000   101
 *   029   000   102
 *   028   000   104
 *   026   000   106
 *   025   000   107
 * <i>etc...</i>
 * </pre></blockquote>
 *
 * The number of RGB codes doesn't have to match an {@link IndexColorModel}'s
 * map size. RGB codes will be automatically interpolated when needed.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class PaletteFactory
{
    /**
     * Petit nombre pour éviter des erreurs d'arrondissements.
     */
    private static final double EPS = 1E-6;

    /**
     * The parent factory, or <code>null</code> if there is none.
     * The parent factory will be queried if a palette was not
     * found in current factory.
     */
    private final PaletteFactory parent;

    /**
     * The class loader from which to load the palette definition files.
     * If <code>null</code>, loading will occurs from the system current
     * working directory.
     */
    private final ClassLoader loader;

    /**
     * The base directory from which to search for palette definition files.
     * If <code>null</code>, then the working directory (".") is assumed.
     */
    private final File directory;

    /**
     * File extension.
     */
    private final String extension;

    /**
     * The charset to use for parsing files, or
     * <code>null</code> for the current default.
     */
    private final Charset charset;

    /**
     * The locale to use for parsing files. or
     * <code>null</code> for the current default.
     */
    private final Locale locale;

    /**
     * Construct a palette factory.
     *
     * @param parent    The parent factory, or <code>null</code> if there is none.
     *                  The parent factory will be queried if a palette was not
     *                  found in current factory.
     * @param loader    The class loader from which to load the palette definition files.
     *                  If <code>null</code>, loading will occurs from the system current
     *                  working directory.
     * @param directory The base directory from which to search for palette definition files.
     *                  If <code>null</code>, then the working directory (".") is assumed.
     * @param extension File name extension. This extension will be automatically
     *                  appended to filename. It should contains the separator '.'.
     * @param charset   The charset to use for parsing files, or
     *                  <code>null</code> for the current default.
     * @param locale    The locale to use for parsing files. or
     *                  <code>null</code> for the current default.
     */
    public PaletteFactory(final PaletteFactory parent, final ClassLoader loader, final File directory, final String extension, final Charset charset, final Locale locale)
    {
        this.parent    = parent;
        this.loader    = loader;
        this.directory = directory;
        this.extension = extension;
        this.charset   = charset;
        this.locale    = locale;
    }

    /**
     * Returns a buffered reader for the specified name.
     *
     * @param  The palette's name to load. This name doesn't need to contains a path
     *         or an extension. Path and extension are set according value specified
     *         at construction time.
     * @return A buffered reader to read <code>name</code>.
     * @throws IOException if an I/O error occured.
     */
    private BufferedReader getReader(String name) throws IOException
    {
        if (extension!=null) name += extension;
        final File file = new File(directory, name);
        final InputStream stream;
        if (loader!=null)
        {
            stream = loader.getResourceAsStream(file.getPath());
        }
        else
        {
            stream = file.exists() ? new FileInputStream(file) : null;
        }
        if (stream==null)
        {
            return null;
        }
        return getReader(stream);
    }

    /**
     * Returns a buffered reader for the specified stream.
     *
     * @param  The input stream.
     * @return A buffered reader to read the input stream.
     * @throws IOException if an I/O error occured.
     */
    private BufferedReader getReader(final InputStream stream) throws IOException
    {
        final Reader reader = (charset!=null) ? new InputStreamReader(stream, charset) : new InputStreamReader(stream);
        return new BufferedReader(reader);
    }

    /**
     * Procède au chargement d'un ensemble de couleurs.    Les couleurs doivent être codées sur trois
     * colonnes dans un fichier texte. Les colonnes doivent être des entiers de 0 à 255 correspondant
     * (dans l'ordre) aux couleurs rouge (R), verte (G) et bleue (B).    Les lignes vierges ainsi que
     * les lignes dont le premier caractère non-blanc est # seront ignorées.
     *
     * @param  input Flot contenant les codes de couleurs de la palette.
     * @return Couleurs obtenues à partir des codes lues.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws IIOException si une erreur est survenue lors de l'interprétation des codes de couleurs.
     */
    private Color[] getColors(final BufferedReader input) throws IOException
    {
        int values[]=new int[3]; // On attend exactement 3 composantes par ligne.
        final LineFormat reader = (locale!=null) ? new LineFormat(locale) : new LineFormat();
        final List<Color> colors=new ArrayList<Color>();
        String line; while ((line=input.readLine())!=null) try
        {
            line=line.trim();
            if (line.length()==0)        continue;
            if (line.charAt(0)=='#')     continue;
            if (reader.setLine(line)==0) continue;
            values = reader.getValues(values);
            colors.add(new Color(byteValue(values[0]), byteValue(values[1]), byteValue(values[2])));
        }
        catch (ParseException exception)
        {
            if (Version.MINOR>=4)
            {
                final IIOException error = new IIOException(exception.getLocalizedMessage());
                error.initCause(exception);
                throw error;
            }
            else throw new IOException(exception.getLocalizedMessage());
            // IOException is the first 1.2 parent of IIOException.
        }
        return colors.toArray(new Color[colors.size()]);
    }

    /**
     * Load colors from a definition file.
     *
     * @param  The palette's name to load. This name doesn't need to contains a path
     *         or an extension. Path and extension are set according value specified
     *         at construction time.
     * @return The set of colors, or <code>null</code> if the set was not found.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if an error occurs during parsing.
     */
    public Color[] getColors(final String name) throws IOException
    {
        final BufferedReader reader = getReader(name);
        if (reader==null)
        {
            return (parent!=null) ? parent.getColors(name) : null;
        }
        final Color[] colors = getColors(reader);
        reader.close();
        return colors;
    }

    /**
     * Load colors from an URL.
     *
     * @param  The palette's URL.
     * @return The set of colors, or <code>null</code> if the set was not found.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if an error occurs during parsing.
     */
    public Color[] getColors(final URL url) throws IOException
    {
        final BufferedReader reader = getReader(url.openStream());
        final Color[] colors = getColors(reader);
        reader.close();
        return colors;
    }

    /**
     * Load an index color model from a definition file.
     * The returned model will use index from 0 to 255 inclusive.
     *
     * @param  The palette's name to load. This name doesn't need to contains a path
     *         or an extension. Path and extension are set according value specified
     *         at construction time.
     * @return The index color model, or <code>null</code> if the palettes was not found.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if an error occurs during parsing.
     */
    public IndexColorModel getIndexColorModel(final String name) throws IOException
    {return getIndexColorModel(name, 0, 256);}

    /**
     * Load an index color model from a definition file.
     * The returned model will use index from <code>lower</code> inclusive to
     * <code>upper</code> exclusive. Other index will have transparent color.
     *
     * @param  The palette's name to load. This name doesn't need to contains a path
     *         or an extension. Path and extension are set according value specified
     *         at construction time.
     * @param  lower Palette's lower index (inclusive).
     * @param  upper Palette's upper index (exclusive).
     * @return The index color model, or <code>null</code> if the palettes was not found.
     * @throws IOException if an error occurs during reading.
     * @throws IIOException if an error occurs during parsing.
     */
    private IndexColorModel getIndexColorModel(final String name, final int lower, final int upper) throws IOException
    {
        final Color[] colors=getColors(name);
        if (colors==null)
        {
            return (parent!=null) ? parent.getIndexColorModel(name, lower, upper) : null;
        }
        final int[] ARGB = new int[1 << getBitCount(upper)];
        expand(colors, ARGB, lower, upper);
        return getIndexColorModel(ARGB);
    }

    /**
     * Copie les couleurs  <code>colors</code>  dans le tableau <code>ARGB</code>  de l'index <code>lower</code>
     * inclusivement jusqu'à <code>upper</code> <u>exclusivement</u>. Si la quantité <code>upper-lower</code> ne
     * correspond pas à la longueur de tableau <code>colors</code>, alors des couleurs peuvent être interpolées.
     */
    static void expand(final Color[] colors, final int[] ARGB, final int lower, final int upper)
    {
        switch (colors.length)
        {
            case 1: Arrays.fill(ARGB, lower, upper, colors[0].getRGB());
            case 0: return; // Return for 0 and 1 cases
        }
        switch (upper-lower)
        {
            case 1: ARGB[lower] = colors[0].getRGB(); // getRGB() is really getARGB()
            case 0: return; // Return for 0 and 1 cases
        }
        final int  maxBase = colors.length-2;
        final double scale = (double)(colors.length-1) / (double)(upper-1-lower);
        for (int i=lower; i<upper; i++)
        {
            final double index = (i-lower)*scale;
            final int     base = Math.min(maxBase, (int)(index+EPS)); // Round toward 0, which is really what we want.
            final double delta = index-base;
            final Color     C0 = colors[base+0];
            final Color     C1 = colors[base+1];
            int A = C0.getAlpha();
            int R = C0.getRed  ();
            int G = C0.getGreen();
            int B = C0.getBlue ();
            ARGB[i] = (round(A+delta*(C1.getAlpha()-A)) << 24) |
                      (round(R+delta*(C1.getRed  ()-R)) << 16) |
                      (round(G+delta*(C1.getGreen()-G)) <<  8) |
                      (round(B+delta*(C1.getBlue ()-B)) <<  0);
        }
    }

    /**
     * Returns an index color model for specified ARGB codes.   If the specified
     * array has not transparent color (i.e. all alpha values are 255), then the
     * returned color model will be opaque. Otherwise, if the specified array has
     * one and only one color with alpha value of 0, the returned color model will
     * have only this transparent color. Otherwise, the returned color model will
     * be translucide.
     *
     * @param  ARGB An array of ARGB values.
     * @return An index color model for the specified array.
     */
    static IndexColorModel getIndexColorModel(final int[] ARGB)
    {
        boolean hasAlpha = false;
        int  transparent = -1;
        /*
         * Examine la palette de couleurs pour
         * déterminer si l'image sera translucide.
         */
        for (int i=0; i<ARGB.length; i++)
        {
            final int alpha = ARGB[i] & 0xFF000000;
            if (alpha!=0xFF000000)
            {
                if (alpha==0x00000000 && transparent<0)
                {
                    transparent=i;
                    continue;
                }
                hasAlpha=true;
                break;
            }
        }
        return new IndexColorModel(getBitCount(ARGB.length), ARGB.length, ARGB, 0, hasAlpha, transparent, getTransferType(ARGB.length));
    }

    /**
     * Retourne le type des données à utiliser pour les transferts, en fonction
     * du nombre de couleurs de la palette. Les deux seuls types autorisés pour
     * {@link IndexColorModel} sont {@link DataBuffer#TYPE_BYTE} et
     * {@link DataBuffer#TYPE_USHORT}.
     */
    private static int getTransferType(final int mapSize)
    {return (mapSize <= 256) ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_SHORT;}

    /**
     * Vérifie que la valeur <code>value</code> spécifiée
     * est dans la plage [0..255] inclusivement.
     *
     * @throws ParseException si le nombre n'est pas dans la plage [0..255].
     */
    private static int byteValue(final int value) throws ParseException
    {
        if (value>=0 && value<256) return value;
        throw new ParseException(Resources.format(Clé.RGB_OUT_OF_RANGE¤1, new Integer(value)), 0);
    }

    /**
     * Arrondie la valeur spécifiée en s'assurant
     * qu'elle reste dans les limites entre 0 et 255.
     */
    private static int round(final double value)
    {return Math.min(Math.max((int)Math.round(value),0),255);}

    /**
     * Propose un nombre de bits à utiliser pour une palette de <code>mapSize</code>
     * couleurs. L'implémentation par défaut retourn 1, 2, 4, 8 ou 16 en fonction de
     * la valeur de <code>mapSize</code>.
     */
    private static int getBitCount(final int mapSize)
    {
        if (mapSize <= 0x00002) return  1;
        if (mapSize <= 0x00004) return  2;
        if (mapSize <= 0x00010) return  4;
        if (mapSize <= 0x00100) return  8;
        if (mapSize <= 0x10000) return 16;
        throw new IllegalArgumentException(Integer.toString(mapSize));
    }
}
