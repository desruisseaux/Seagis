/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Universidad de Las Palmas de Gran Canaria
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
 * Contact: Antonio Ramos
 *          Departamento de Biologia ULPGC
 *          Campus Universitario de Tafira
 *          Edificio de Ciencias Basicas
 *          35017
 *          Las Palmas de Gran Canaria
 *          Spain
 *
 *          mailto:antonio.ramos@biologia.ulpgc.es
 */
package fr.ird.image.pump;

// Géométrie et images
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;

// Collection
import java.util.Set;
import java.util.Arrays;
import java.util.LinkedHashSet;

// Lecture de fichiers HDF
import ncsa.hdf.hdflib.HDFException;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.resources.Arguments;

// Miscellaneous
import fr.ird.io.hdf4.Parser;
import fr.ird.io.hdf4.DataSet;
import fr.ird.operator.image.AbstractImageFunction;


/**
 * Classe construisant une image représentant l'amplitude
 * du vent mesuré par le satellite QuikScat. Cette classe
 * utilise les fichiers de données de niveau L2B.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class WindImageFunction extends AbstractImageFunction {
    /**
     * Liste des fichiers de données à utiliser.
     */
    private final File[] files;

    /**
     * Index du prochain fichier à lire.
     */
    private int fileIndex;

    /**
     * Ensemble des coordonnéess <var>x</var> des points.
     */
    protected DataSet longitude;

    /**
     * Ensemble des coordonnéess <var>y</var> des points.
     */
    protected DataSet latitude;

    /**
     * Ensemble des vitesses du vent en chaque points.
     */
    protected DataSet windSpeed;

    /**
     * Ensemble des directions du vent en chaque points.
     */
    protected DataSet windDirection;

    /**
     * Nombre de points à lire.
     */
    private int count;

    /**
     * Index du prochain point à lire.
     */
    private int index;

    /**
     * Construit une fonction qui utilisera les données du fichier spécifié.
     * Si le fichier est un répertoire, alors tous les fichiers de ce répertoire
     * seront utilisés.
     *
     * @param  file Fichier de données QuikScat de niveau L1B.
     * @throws IOException si le ne peut pas être lu.
     */
    public WindImageFunction(final File file) {
        this(new File[] {file});
    }

    /**
     * Construit une fonction qui utilisera les données des fichiers spécifiés.
     *
     * @param  files Fichiers de données QuikScat de niveau L1B.
     * @throws IOException si un ou des fichiers ne peuvent pas être lus.
     */
    public WindImageFunction(final File[] files) {
        super(GeographicCoordinateSystem.WGS84);
        final Set<File> list = new LinkedHashSet<File>();
        for (int i=0; i<files.length; i++) {
            final File file = files[i];
            if (file.isDirectory()) {
                list.addAll(Arrays.asList(file.listFiles()));
            } else {
                list.add(file);
            }
        }
        this.files = list.toArray(new File[list.size()]);
    }

    /**
     * Positionne le curseur au début du flot de données. Lorsque <code>ImageFunction</code>
     * a besoin de connaître les données qui constituent une image, il va d'abord appeller
     * <code>rewind()</code>, puis fera une série d'appel à {@link #next}.
     *
     * @throws IOException si l'opération a échouée.
     */
    protected final void rewind() throws IOException {
        index = 0;
        if (fileIndex > 1) {
            count         = 0;
            fileIndex     = 0;
            longitude     = null;
            latitude      = null;
            windSpeed     = null;
            windDirection = null;
        }
    }

    /**
     * Retourne les coordonnées (<var>x</var>,<var>y</var>,<var>z</var>) de la donnée
     * courante, puis passe au point suivant. S'il ne reste plus de point à balayer,
     * alors cette méthode retourne <code>null</code>.
     *
     * @throws IOException si la lecture a échouée.
     */
    protected final double[] next(final double[] data) throws IOException {
        while (index >= count) try {
            if (fileIndex >= files.length) {
                return null;
            }
            final QuikscatParser parser = new QuikscatParser(files[fileIndex]);
            try {
                count = load(parser);
            } finally {
                parser.close();
            }
            index = 0;
            fileIndex++;
        } catch (HDFException exception) {
            final IOException ioe = new IOException(exception.getLocalizedMessage());
            ioe.initCause(exception);
            throw ioe;
        }
        return compute(index++, data);
    }

    /**
     * Procède à la lecture des données. Cette méthode est appelée automatiquement
     * par {@link #next} lorsqu'une nouvelle lecture de données est nécessaire.
     * L'implémentation par défaut obtient les séries de données pour les paramètres
     * <code>"wvc_lon"</code>, <code>"wvc_lat"</code>, <code>"wind_speed_selection"</code>
     * et <code>"wind_dir_selection"</code>. Il est de la responsabilité de l'appellant de
     * fermer l'objet <code>parser</code> après la lecture.
     *
     * @param  parser Décodeur de données à utiliser.
     * @return Le nombre de valeurs qui pourront être calculées à partir des données lues.
     *         Lors des appels à {@link #compute}, l'index variera de 0 inclusivement
     *         jusqu'à <var>n</var> exclusivement, ou <var>n</var> est la valeur retournée
     *         par cette méthode.
     * @throws HDFException si la lecture du fichier a échoué.
     */
    protected int load(final QuikscatParser parser) throws HDFException {
        longitude     = parser.getDataSet("wvc_lon");
        latitude      = parser.getDataSet("wvc_lat");
        windSpeed     = parser.getDataSet("wind_speed_selection");
        windDirection = parser.getDataSet("wind_dir_selection");
        return windSpeed.getRowCount() * windSpeed.getColumnCount();
    }

    /**
     * Calcule la valeur du paramètre à l'index spécifié. La valeur de l'index
     * est comprise dans les limites des tableaux {@link #longitude}, {@link #latitude},
     * {@link #windSpeed} et {@link #windDirection}. L'implémentation par défaut retourne
     * le vitesse du vent. Cette méthode est appelée automatiquement par {@link #next}
     * lorsque nécessaire.
     */
    protected double[] compute(final int index, final double[] data) {
        final double x = longitude.getFlat(index);
        final double y =  latitude.getFlat(index);
        final double z = windSpeed.getFlat(index);
        if (data != null) {
            data[0] = x;
            data[1] = y;
            data[2] = z;
            return data;
        }
        return new double[] {x,y,z};
    }

    /**
     * Free all resources used by this <code>ImageFunction</code>.
     * Trying to use this object after <code>dispose()</code> may
     * fail.
     *
     * @throws IOException If an I/O operation was required and failed.
     */
    public synchronized void dispose() throws IOException {
        super.dispose();
        count         = 0;
        index         = 0;
        fileIndex     = 0;
        longitude     = null;
        latitude      = null;
        windSpeed     = null;
        windDirection = null;
    }

    /**
     * Run the program. Optional arguments are:
     * <ul>
     *   <li><code>-width</code> = <var>[integer]</var></code>
     *       The image width. Default to 1024</code>.</li>
     *   <li><code>-height</code> = <var>[integer]</var></code>
     *       The image height. Default to 512</code>.</li>
     *   <li><code>-output = <var>[PNG file]</var></code>
     *       The file were to write the PNG image. If ommited, then
     *       the image will be displayed to screen.</li>
     * </ul>
     */
    public static void main(String[] args) throws IOException {
        final Arguments arguments = new Arguments(args);
        if (args.length == 0) {
            arguments.out.println("Crée une image représentant l'intensité du vent.");
            arguments.out.println("Arguments optionels:");
            arguments.out.println("  -width  = [entier]    Largeur de l'image (1024 par défaut)");
            arguments.out.println("  -height = [entier]    Hauteur de l'image ( 512 par défaut)");
            arguments.out.println("  -output = [fichier]   Fichier PNG dans lequel enregistrer");
            arguments.out.println();
            arguments.out.println("Les autres arguments sont les noms de répertoires et/ou de fichiers L2B à lire.");
            return;
        }
        final String destination = arguments.getOptionalString("-output");
        final Integer   optWidth = arguments.getOptionalInteger("-width");
        final Integer  optHeight = arguments.getOptionalInteger("-height");
        final int width  = (optWidth != null) ? optWidth.intValue() : 1024;
        final int height = (optWidth != null) ? optWidth.intValue() :  512;
        args = arguments.getRemainingArguments(Integer.MAX_VALUE);
        final File[] files = new File[args.length];
        for (int i=0; i<args.length; i++) {
            files[i] = new File(args[i]);
        }
        final WindImageFunction function = new WindImageFunction(files);
        if (false) {
            function.setGeographicArea(new Rectangle2D.Double(-180, -90, 360, 180));
        }
        final GridCoverage image;
        if (destination != null) {
            image = function.getGridCoverage("Vent", width, height);
            ImageIO.write(image.geophysics(true).getRenderedImage(), "png", new File(destination));
        } else {
            image = function.show("Vent", width, height);
        }
        function.dispose();
    }
}
