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
import javax.media.jai.PlanarImage;
import javax.imageio.ImageIO;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

// Lecture de fichiers HDF
import fr.ird.io.hdf4.Parser;
import fr.ird.io.hdf4.DataSet;
import ncsa.hdf.hdflib.HDFException;

// Date et heures
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.ct.TransformException;
import org.geotools.resources.Arguments;
import org.geotools.resources.MonolineFormatter;


/**
 * Classe construisant une image représentant le pompage d'Ekman
 * calculé à partir des données de vent mesuré par le satellite
 * QuikScat. Cette classe utilise les fichiers de données de niveau L2B.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EkmanPumpingImageFunction extends WindImageFunction {
    /**
     * Objet ayant la charge de calculer la vorticité
     * à partir des données qu'on lui fournira.
     */
    private final EkmanPumpingCalculator calculator = new EkmanPumpingCalculator4P();

    /**
     * Latitude maximale au delà de laquelle on ne calculera plus
     * la vorticité.
     */
    private static final int maxLatitude = 72;

    /**
     * Gives the step point to calculate vorticity.
     * 1 means use the nearest points in the wind data grid.
     */
    private int deltaIndex = 1;

    /**
     * Nombre de données dans chaque ligne. En d'autres
     * mots, il s'agit du nombre de colonnes.
     */
    private int rowLength;

    /**
     * Nombre de données dans chaque ligne moins le nombre
     * de données nécessaire pour calculer la vorticité
     * (@link #deltaIndex}).
     */
    private int reducedRowLength;
    
    /**
     * Start time for current data.
     */
    private Date beginningTime;

    /**
     * Construit une fonction qui utilisera les données des fichiers spécifiés.
     *
     * @param  files Fichiers de données QuikScat de niveau L1B.
     * @throws IOException si un ou des fichiers ne peuvent pas être lus.
     */
    public EkmanPumpingImageFunction(final File[] files) throws IOException {
        super(files);
        setGeographicArea(new Rectangle2D.Double(-180, -maxLatitude, 360, 2*maxLatitude));
        setMinimum       (-5E-6);
        setMaximum       (+5E-6);
        setColorPalette  ("red-blue");
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
        super.load(parser);
        beginningTime    = parser.getStartTime();
        rowLength        = windSpeed.getColumnCount();
        reducedRowLength = rowLength - deltaIndex;
        return (windSpeed.getRowCount()-deltaIndex) * (windSpeed.getColumnCount()-deltaIndex);
    }

    /**
     * Compute Ekman pumping at the specified index.
     */
    protected double[] compute(final int index, final double[] data) {
        final int sourceIndex1 = (index/reducedRowLength)*rowLength + (index % reducedRowLength);
        final int sourceIndex2 = sourceIndex1+deltaIndex;
        final int sourceIndex3 = sourceIndex1+rowLength;
        final int sourceIndex4 = sourceIndex3+deltaIndex;

        double x1 = longitude.get(sourceIndex1);
        double x2 = longitude.get(sourceIndex2);
        double x3 = longitude.get(sourceIndex3);
        double x4 = longitude.get(sourceIndex4);

        final double y1 =  latitude.get(sourceIndex1);
        final double y2 =  latitude.get(sourceIndex2);
        final double y3 =  latitude.get(sourceIndex3);
        final double y4 =  latitude.get(sourceIndex4);

        final double a1 = windSpeed.get(sourceIndex1);
        final double a2 = windSpeed.get(sourceIndex2);
        final double a3 = windSpeed.get(sourceIndex3);
        final double a4 = windSpeed.get(sourceIndex4);

        final double d1 = Math.toRadians(windDirection.get(sourceIndex1));
        final double d2 = Math.toRadians(windDirection.get(sourceIndex2));
        final double d3 = Math.toRadians(windDirection.get(sourceIndex3));
        final double d4 = Math.toRadians(windDirection.get(sourceIndex4));

        if (Math.abs(x1-x2)>=180 || Math.abs(x1-x3)>=180 || Math.abs(x1-x4)>=180
                                 || Math.abs(x2-x3)>=180 || Math.abs(x2-x4)>=180
                                                         || Math.abs(x3-x4)>=180)   
        {
            if (x1 >= 180) x1 -= 360;
            if (x2 >= 180) x2 -= 360;
            if (x3 >= 180) x3 -= 360;
            if (x4 >= 180) x4 -= 360;
        }

        double xo = 0.25*(x1 + x2 + x3 + x4);
        double yo = 0.25*(y1 + y2 + y3 + y4);
        double pumping;

        if (Math.abs(yo) >= maxLatitude) {
            pumping = Double.NaN;
        } else try {
            pumping = calculator.pumping(x1, y1, a1*Math.sin(d1), a1*Math.cos(d1),
                                         x2, y2, a2*Math.sin(d2), a2*Math.cos(d2),
                                         x3, y3, a3*Math.sin(d3), a3*Math.cos(d3),
                                         x4, y4, a4*Math.sin(d4), a4*Math.cos(d4),
                                         xo, yo);
        } catch (TransformException exception) {
            pumping = Double.NaN;
            final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
            record.setSourceClassName("EkmanPumpingImageFunction");
            record.setSourceMethodName("compute");
            record.setThrown(exception);
            Logger.getLogger("fr.ird.operator.image").log(record);
        }
        if (xo < 0) {
            xo += 360;
        }
        if (data != null) {
            data[0] = xo;
            data[1] = yo;
            data[2] = pumping;
            return data;
        }
        return new double[] {xo, yo, pumping};
    }

    /**
     * Compute and display an image of Ekman pumping. A list of directory and/or
     * N1B files must be specified as command-line arguments. Optional arguments
     * are:
     * <ul>
     *   <li><code>-wind</code>
     *       Compute the wind instead of Ekman pumping.</li>
     *
     *   <li><code>-width</code> = <var>[integer]</var></code>
     *       The image width. Default to 1440</code>.</li>
     *
     *   <li><code>-cells = <var>[integer]</var></code>
     *       Number of cells (around the central point) to use for computation.
     *       Default value is 1.</li>
     *
     *   <li><code>-height</code> = <var>[integer]</var></code>
     *       The image height. Default to 576</code>.</li>
     *
     *   <li><code>-output = <var>[PNG file]</var></code>
     *       The file were to write the PNG image. If ommited, then the image will
     *       be displayed to screen.</li>
     * </ul>
     */
    public static void main(String[] args) throws IOException {
        MonolineFormatter.init("fr.ird.image.pump");
        /*
         * Initial image width and height. May be modified later
         * if the user supplied -width and/or -height arguments.
         */
        int width  = 360 * 4;
        int height = maxLatitude * 8;
        /*
         * Parse command-line arguments.
         */
        final Arguments arguments = new Arguments(args);
        if (args.length == 0) {
            arguments.out.println("Crée une image représentant le pompage d'Ekman.");
            arguments.out.println("Arguments optionels:");
            arguments.out.println("  -wind                 Calcule le vent plutôt que le pompage");
            arguments.out.println("  -width  = [entier]    Largeur de l'image ("+width+" par défaut)");
            arguments.out.println("  -height = [entier]    Hauteur de l'image ( "+height+" par défaut)");
            arguments.out.println("  -cells  = [entier]    Nombre de cellules (1 par défaut)");
            arguments.out.println("  -colors = [nom]       Nom de la palette de couleurs (rainbow, grayscale, red-blue)");
            arguments.out.println("  -output = [fichier]   Fichier PNG dans lequel enregistrer");
            arguments.out.println();
            arguments.out.println("Les autres arguments sont les noms de répertoires et/ou de fichiers L2B à lire.");
            return;
        }
        final boolean    wind = arguments.getFlag           ("-wind");
        final String   output = arguments.getOptionalString ("-output");
        final Integer  widthC = arguments.getOptionalInteger("-width");
        final Integer heightC = arguments.getOptionalInteger("-height");
        final Integer   cells = arguments.getOptionalInteger("-cells");
        final String   colors = arguments.getOptionalString ("-colors");
        if (widthC  != null) width  = widthC .intValue();
        if (heightC != null) height = heightC.intValue();
        args = arguments.getRemainingArguments(Integer.MAX_VALUE);
        final File[] files = new File[args.length];
        for (int i=0; i<args.length; i++) {
            files[i] = new File(args[i]);
        }
        /*
         * All arguments have been parsed. Now, create the image.
         */
        final String            title;
        final WindImageFunction function;
        final GridCoverage      image;
        if (wind) {
            title    = "Vent";
            function = new WindImageFunction(files);
        } else {
            title    = "Pompage d'Ekman";
            function = new EkmanPumpingImageFunction(files);
            if (cells != null) {
                ((EkmanPumpingImageFunction) function).deltaIndex = cells.intValue();
            }
        }
        if (colors != null) {
            function.setColorPalette(colors);
        }
        if (output != null) {
            image = function.getGridCoverage(title, width, height);
            final int extensionIndex = output.lastIndexOf('.');
            final String type = (extensionIndex>=0) ? output.substring(extensionIndex+1) : "png";
            RenderedImage outputImage;
            if (type.equalsIgnoreCase("raw")) {
                outputImage = image.geophysics(true).getRenderedImage();
                outputImage = PlanarImage.wrapRenderedImage(outputImage).getAsBufferedImage();
                // Convert to BufferedImage in order to make sure we have only one tile.
            } else {
                outputImage = image.geophysics(false).getRenderedImage();
            }
            ImageIO.write(outputImage, type, new File(output));
        } else {
            image = function.show(title, width, height);
        }
        function.dispose();
    }
}
