/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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
package fr.ird.io.coverage;

// Images
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.ParameterList;

// Image database
import java.sql.SQLException;

// Input/output
import java.net.URL;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.ConsoleHandler;

// Collection and formating
import java.util.List;
import java.util.ArrayList;
import java.text.NumberFormat;
import java.util.StringTokenizer;

// Swing components
import java.awt.EventQueue;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools dependencies
import org.geotools.gp.Operation;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.SampleDimension;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.ct.MissingParameterException;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.CoordinateSystem;

// Geotools (miscellaneous)
import org.geotools.resources.Arguments;
import org.geotools.resources.Utilities;
import org.geotools.io.DefaultFileFilter;
import org.geotools.gui.swing.MapPane;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedGeometries;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.renderer.geom.GeometryCollection;

// Seagis
import fr.ird.database.coverage.FormatEntry;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.io.map.GEBCOReader;
import fr.ird.io.map.GEBCOFactory;
import fr.ird.io.map.IsolineReader;
import fr.ird.io.map.IsolineFactory;


/**
 * Importe dans la base de données les fichiers ERDAS fournis par la station
 * des Canaris. Cette classe peut lire les fichiers de données brutes (RAW),
 * appliquer une projection cartographique inverse, les convertir en PNG et
 * ajouter les entrés correspondantes dans la base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Processor extends Arguments {
    /**
     * The grid coverage processor. Will be
     * constructed only when first needed.
     */
    private transient GridCoverageProcessor processor;

    /**
     * The grid coverage exchange. This object will be constructed
     * only the first time an image is about to be read.
     */
    private transient GridCoverageExchange exchange;

    /**
     * Liste des images à charger.
     */
    private final File[] sources;

    /**
     * Répertoire de destination, ou <code>null</code>
     * pour afficher les images plutôt que de les écrire.
     */
    private final File destination;

    /**
     * The interpolation method, ou <code>null</code>
     * for default (nearest neighbor).
     */
    private final String interpolation;

    /**
     * Base de données d'images.
     */
    private final CoverageDataBase database;

    /**
     * Objet à utiliser pour mettre à jour la base de données, ou
     * <code>null</code> s'il ne faut pas faire de mises à jour.
     */
    private final CoverageTable coverageTable;

    /**
     * Listes des bandes des images à lire.
     */
    private final SampleDimension[] bands;

    /**
     * Isolignes formant la bathymétrie.
     */
    private final IsolineFactory isolineFactory;

    /**
     * Isolignes a afficher sur les images.
     */
    private final float[] isolines;

    /**
     * Panneau dans lequel placer les images affichées.
     * Cette composante ne sera construite que la première
     * fois où elle sera nécessaire.
     */
    private transient JTabbedPane tabbedPane;

    /**
     * Point d'entré du programme.
     *
     * @param args Arguments spécifiés sur la ligne de commande.
     *        Si cette méthode est appelée sans arguments, alors
     *        un résumé des commandes disponibles sera affichée.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            if (false) { // Debug code for SST
                args = new String[] {
                    "-group=365245516",
                    "-sources=D:/Pouponnière/Méditerranée/Température/sst020807.txt"
                };
            }
            if (false) { // Debug code for Chlorophylle-a
                args = new String[] {
                    "-group=2041402270",
                    "-sources=D:/Pouponnière/Méditerranée/Chlorophylle/chl_020703.txt"
                };
            }
        }
        try {
            new Processor(args).run();
        } catch (ThreadDeath stop) {
            // Construction failed. Error message
            // has already been printed. Nothing
            // else to do (except finish the program).
        }
    }

    /**
     * Construct a processor. This constructor parse all arguments
     * supplied on the command line. If the construction fails for
     * whatever raisons, an error message is printed and a
     * {@link ThreadDeath} exception is thrown, in order to kill
     * the application.
     *
     * @param args Command line arguments.
     */
    public Processor(final String[] args) {
        super(args);
        final String    seriesID;
        final String       bathy;
        final SeriesTable series;
        final SeriesEntry  série;
        final FormatEntry format;
        final String destination;
        final boolean   updateDB;
        try {
            sources          = getFiles         ("-sources");
            updateDB         = getFlag          ("-updateDB");
            seriesID         = getOptionalString("-series");
            bathy            = getOptionalString("-bathy");
            destination      = getOptionalString("-destination");
            interpolation    = getOptionalString("-interpolation");
            isolineFactory   = new GEBCOFactory ("Méditerranée");
            this.destination = (destination!=null) ? new File(destination) : null;
            getRemainingArguments(0);
            if (sources == null) {
                /////////////////////////////////////////////////////
                ////                                             ////
                ////    If no input was specified, display       ////
                ////    the help screen and stop the program.    ////
                ////                                             ////
                /////////////////////////////////////////////////////
                out.println("Usage: -sources       [fichiers] (exemple: \"*.txt\")\n"+
                            "       -destination   [Répertoire de destination]\n"+
                            "       -interpolation [NearestNeighbor (défaut) | Bilinear | Bicubic]\n"+
                            "       -bathy         [profondeurs séparées par des virgules sans espace]\n"+
                            "       -series        [#ID dans la table \"Séries\" de la base de données]\n"+
                            "       -updateDB\n"+
                            "\n"+
                            "Les arguments \"-series\" et \"-sources\" sont obligatoires. Les autres\n"+
                            "sont facultatifs. Si \"-destination\" est omis, chaque image lue sera\n"+
                            "affichée à l'écran mais ne sera pas enregistrée.  Si \"-destination\"\n"+
                            "est présent, alors les images seront au contraire enregistrées sans\n"+
                            "être affichées.\n"+
                            "\n"+
                            "Les profondeurs bathymétriques disponibles sont (en mètres):");
                String str; int length=8,spaces=8;
                isolines = isolineFactory.getAvailableValues();
                for (int i=isolines.length; --i>=0;) {
                    out.print(Utilities.spaces(spaces));
                    out.print(str = String.valueOf(-Math.round(isolines[i])));
                    if (i != 0) {
                        out.print(','); spaces=1;
                        if ((length += str.length()) <= 40) continue;
                    }
                    out.println();
                    spaces = length = 8;
                }
                throw new ThreadDeath(); // Stop the program.
            }
            /////////////////////////////////////////////
            ////                                     ////
            ////    Open the database connection.    ////
            ////                                     ////
            /////////////////////////////////////////////
            if (seriesID == null) {
                throw new MissingParameterException("L'argument -series est obligatoire", "series");
            }
            database = new fr.ird.database.coverage.sql.CoverageDataBase();
            series   = database.getSeriesTable();
            série    = series.getEntry(Integer.parseInt(seriesID));
            format   = series.getFormat(série);
            series.close();
            if (format == null) {
                database.close();
                throw new IllegalArgumentException("Code de séries inconnu: "+seriesID);
            }
            bands = format.getSampleDimensions();
            for (int i=0; i<bands.length; i++) {
                // In images to be read, bands contain already geophysics values.
                bands[i] = bands[i].geophysics(true);
            }
            if (updateDB) {
                coverageTable = database.getCoverageTable();
                coverageTable.setSeries(série);
            } else {
                coverageTable = null;
            }
            /////////////////////////////////////////////
            ////                                     ////
            ////    Get the requested bathymetry.    ////
            ////                                     ////
            /////////////////////////////////////////////
            if (bathy != null) {
                final StringTokenizer tk = new StringTokenizer(bathy, ",");
                isolines = new float[tk.countTokens()];
                for (int i=0; tk.hasMoreTokens(); i++) {
                    isolines[i] = -Float.parseFloat(tk.nextToken());
                }
            } else {
                isolines = null;
            }
        } catch (Exception exception) {
            handleException(exception, "<init>");
            throw new ThreadDeath(); // Stop the program.
        }
    }

    /**
     * Run the processor and close the database. This method
     * must be invoked after <code>Processor</code> has been
     * built, in order to execute the instructions parsed on
     * the command line.
     */
    public void run() {
        String methodName = "run";
        try {
            if (destination == null) {
                methodName="verify";
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY+2);
                for (int i=0; i<sources.length; i++) {
                    verify(sources[i]);
                }
            } else {
                methodName = "process";
                for (int i=0; i<sources.length; i++) {
                    if (!process(sources[i])) {
                        break;
                    }
                }
            }
            methodName = "close";
            if (coverageTable != null) {
                coverageTable.close();
            }
            database.close();
        } catch (Exception exception) {
            handleException(exception, methodName);
        }
    }

    /**
     * Returns a list of files for the specified parameter.
     * This method is for internal use by the constructor.
     */
    private File[] getFiles(final String parameter) {
        final String path = getOptionalString(parameter);
        if (path != null) {
            File file = new File(path);
            String name = file.getName();
            if (name.indexOf('*')>=0 || name.indexOf('?')>=0) {
                file = file.getParentFile();
                if (file == null) {
                    file=new File(".");
                }
                return file.listFiles((FileFilter) new DefaultFileFilter(name));
            }
            return new File[] {file};
        }
        return null;
    }

    /**
     * Load a grid coverage from a file. If <code>reproject</code> is
     * <code>true</code>,  then the grid coverage will be reprojected
     * to {@link GeographicCoordinateSystem#WGS84}.
     */
    private GridCoverage load(final File file, final boolean reproject) throws IOException {
        if (exchange == null) {
            exchange = new GridCoverageExchange(bands);
            exchange.setLocale(locale);
        }
        GridCoverage coverage = exchange.createFromName(file.getPath());
        if (reproject) {
            CoordinateSystem sourceCS = coverage.getCoordinateSystem();
            CoordinateSystem targetCS = GeographicCoordinateSystem.WGS84;
            if (sourceCS instanceof CompoundCoordinateSystem) {
                targetCS = new CompoundCoordinateSystem(targetCS.getName(null), targetCS,
                                      ((CompoundCoordinateSystem) sourceCS).getTailCS());
            }
            if (processor == null) {
                processor = GridCoverageProcessor.getDefault();
            }
            final Operation operation = processor.getOperation("Resample");
            final ParameterList param = operation.getParameterList();
            param.setParameter("Source",           coverage);
            param.setParameter("CoordinateSystem", targetCS);
            if (interpolation != null) {
                param.setParameter("InterpolationType", interpolation);
            }
            coverage = processor.doOperation(operation, param);
        }
        return coverage.geophysics(false);
    }

    /**
     * Load and display an image. Image's properties will be dumped
     * on the standard output stream.
     *
     * @param  file The file to process.
     * @throws IOException if an error occured while reading the file.
     */
    private void verify(final File file) throws IOException {
        final GridCoverage coverage = load(file, false);
        out.println();
        out.println(exchange.getLastProperties());
        out.println(coverage.getCoordinateSystem());

        if (tabbedPane == null) {
            tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
            final JFrame frame = new JFrame("Images");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(tabbedPane);
            frame.setSize(400, 400);
            frame.show();
        }
        final MapPane map = new MapPane(coverage.getCoordinateSystem());
        final RenderedGridCoverage image = new RenderedGridCoverage(coverage);
        image.setZOrder(Float.NEGATIVE_INFINITY);
        map.getRenderer().addLayer(image);
        if (isolines != null) {
            for (int i=0; i<isolines.length; i++) {
                final GeometryCollection isoline = isolineFactory.get(isolines[i]);
                if (isoline != null) {
                    final RenderedGeometries layer = new RenderedGeometries(isoline);
                    layer.setContour(Color.white);
                    map.getRenderer().addLayer(layer);
                }
            }
        }
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {tabbedPane.addTab(coverage.getName(locale), map.createScrollPane());}
        });
    }

    /**
     * Process an image. The image is loaded, projected
     * and saved into the destination directory.
     *
     * @param  file The file to process.
     * @return <code>true</code> if succed, <code>false</code> if failed.
     * @throws IOException if an error occured while reading or writing the image.
     * @throws SQLException if an error occured while updating the database.
     */
    private boolean process(final File file) throws IOException, SQLException {
        String filename = file.getName();
        out.print(filename);
        out.print(": Chargement...\r");
        out.flush();
        final GridCoverage coverage = load(file, true);

        filename = exchange.getOutputFilename();
        final int index = filename.lastIndexOf('.');
        if (index >= 0) {
            filename = filename.substring(0, index);
        }
        final File output = new File(destination, filename+".png");
        if (output.exists()) {
            out.print  ("Le fichier \"");
            out.print  (output.getPath());
            out.println("\" existe déjà.");
            out.println("Abandon de l'opération");
            return false;
        }

        out.print(filename);
        out.print(": Projection...\r");
        out.flush();
        final BufferedImage image = ((PlanarImage) coverage.getRenderedImage()).getAsBufferedImage();

        if (isolines != null) try {
            out.print(filename);
            out.print(": Bathymétrie...\r");
            out.flush();
            final AffineTransform tr = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
            final Point2D.Double  pt = new Point2D.Double(1,1); // Select a slightly more than 1-pixel width line.
            tr.deltaTransform(pt,pt);
            final Graphics2D gr = image.createGraphics();
            gr.transform(tr.createInverse());
            gr.setStroke(new BasicStroke((float)Math.sqrt(pt.x*pt.x + pt.y*pt.y)));
            gr.setColor(Color.white);
            for (int i=0; i<isolines.length; i++) {
                final GeometryCollection isoline = isolineFactory.get(isolines[i]);
                if (isoline!=null) try {
                    isoline.setCoordinateSystem(coverage.getCoordinateSystem());
                    gr.draw(isoline);
                } catch (TransformException exception) {
                    handleException(exception, "process");
                }
            }
            gr.dispose();
        } catch (NoninvertibleTransformException exception) {
            handleException(exception, "process");
        }

        out.print('\r');
        out.print(filename);
        out.print(": Enregistrement...\r");
        out.flush();
        ImageIO.write(image, "png", output);
        out.println();

        final LogRecord record = new LogRecord(Level.INFO, "Enregistrement de \""+filename+"\" terminé.");
        record.setSourceClassName ("Processor");
        record.setSourceMethodName("process");
        silentLog(record);

        if (coverageTable != null) {
            coverageTable.addGridCoverage(coverage, filename);
        }
        return true;
    }

    /**
     * Invoked when an operation failed. The exception message
     * is printed and the exception trace is logged.
     */
    private void handleException(final Exception exception, final String methodName) {
        exception.printStackTrace(out);

        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(exception));
        final String message = exception.getLocalizedMessage();
        if (message != null) {
            buffer.append(": ");
            buffer.append(message);
        }
        final LogRecord record = new LogRecord(Level.WARNING, buffer.toString());
        record.setSourceClassName ("Processor");
        record.setSourceMethodName(methodName);
        record.setThrown          (exception);
        silentLog(record);
        if (exchange != null) {
            out.println();
            out.println("Dernières informations:");
            out.println(exchange.getLastProperties());
        }
        out.flush();
    }

    /**
     * Log a message to the stream, but not to the console.
     */
    private static void silentLog(final LogRecord record) {
        Logger logger = Logger.getLogger("fr.ird.io.coverage");
        while (logger != null) {
            final Handler[] handlers = logger.getHandlers();
            for (int i=0; i<handlers.length; i++) {
                final Handler handler = handlers[i];
                if (!(handler instanceof ConsoleHandler)) {
                    handler.publish(record);
                }
            }
            if (!logger.getUseParentHandlers()) {
                break;
            }
            logger = logger.getParent();
        }
    }
}
