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

// OpenGIS dependencies (SEAGIS)
import net.seagis.gp.Operation;
import net.seagis.gc.GridCoverage;
import net.seagis.cv.CategoryList;
import net.seagis.ct.TransformException;
import net.seagis.gp.GridCoverageProcessor;
import net.seagis.ct.MissingParameterException;
import net.seagis.cs.GeographicCoordinateSystem;
import net.seagis.cs.CompoundCoordinateSystem;
import net.seagis.cs.CoordinateSystem;

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
import fr.ird.sql.image.FormatEntry;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.TableFiller;
import fr.ird.sql.image.ImageDataBase;

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

// Map display
import net.seas.map.Layer;
import net.seas.map.Isoline;
import net.seas.map.MapPanel;
import net.seas.map.io.GEBCOReader;
import net.seas.map.io.IsolineReader;
import net.seas.map.layer.IsolineLayer;
import net.seas.map.layer.GridCoverageLayer;

// Swing components
import java.awt.EventQueue;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

// Miscellaneous
import net.seas.util.Console;
import net.seagis.resources.Utilities;
import net.seagis.io.DefaultFileFilter;


/**
 * Importe dans la base de données les fichiers ERDAS fournis par la station
 * des Canaris. Cette classe peut lire les fichiers de données brutes (RAW),
 * appliquer une projection cartographique inverse, les convertir en PNG et
 * ajouter les entrés correspondantes dans la base de données.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Processor extends Console
{
    /**
     * The bathymetry to put over images.
     */
    private static final String SOURCE_BATHY = "compilerData/map/Méditerranée.asc";

    /**
     * Cache to use for serialized bathymetry.
     */
    private static final String CACHED_BATHY = "applicationData/cache/Méditerranée.serialized";

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
    private final ImageDataBase database;

    /**
     * Objet à utiliser pour mettre à jour la base de données, ou
     * <code>null</code> s'il ne faut pas faire de mises à jour.
     */
    private final TableFiller tableFiller;

    /**
     * Listes des catégories pour chaque bandes des images à lire.
     * Chaque objet {@link CategoryList} correspond à une bande de
     * l'image.
     */
    private final CategoryList[] categories;

    /**
     * Isolignes formant la bathymétrie.
     */
    private final Isoline[] isolines;

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
    public static void main(String[] args)
    {
        if (args.length==0)
        {
            if (false) // Debug code for SST
            {
                args = new String[]
                {
                    "-group=365245516",
                    "-sources=E:/Pouponnière/Mediterranee/SST_new/md020117.txt"
                };
            }
            if (false) // Debug code for Chlorophylle-a
            {
                args = new String[]
                {
                    "-group=2041402270",
                    "-sources=E:/Pouponnière/Mediterranee/CHLORO_traitées/md_chl011007.txt"
                };
            }
        }
        try
        {
            new Processor(args).run();
        }
        catch (ThreadDeath stop)
        {
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
    public Processor(final String[] args)
    {
        super(args);
        final String       group;
        final String       bathy;
        final SeriesTable series;
        final FormatEntry format;
        final String destination;
        final boolean   updateDB;
        final int        groupID;
        try
        {
            sources       = getFiles  ("-sources");
            updateDB      = hasFlag  ("-updateDB");
            group         = getParameter("-group");
            bathy         = getParameter("-bathy");
            destination   = getParameter("-destination");
            interpolation = getParameter("-interpolation");
            checkRemainingArguments(0);
            this.destination = (destination!=null) ? new File(destination) : null;
            if (sources==null)
            {
                out.println("Usage: -sources       [fichiers] (exemple: \"*.txt\")\n"+
                            "       -destination   [Répertoire de destination]\n"+
                            "       -interpolation [NearestNeighbor (défaut) | Bilinear | Bicubic]\n"+
                            "       -bathy         [profondeurs séparées par des virgules sans espace]\n"+
                            "       -group         [#ID dans la table \"Group\" de la base de données]\n"+
                            "       -updateDB\n"+
                            "\n"+
                            "Les arguments \"-groups\" et \"-sources\" sont obligatoires. Les autres\n"+
                            "sont facultatifs. Si \"-destination\" est omis, chaque image lue sera\n"+
                            "affichée à l'écran mais ne sera pas enregistrée.  Si \"-destination\"\n"+
                            "est présent, alors les images seront au contraire enregistrées sans\n"+
                            "être affichées.\n"+
                            "\n"+
                            "Les profondeurs bathymétriques disponibles sont (en mètres):");
                isolines = getIsolines();
                String str; int length=8,spaces=8;
                for (int i=isolines.length; --i>=0;)
                {
                    out.print(Utilities.spaces(spaces));
                    out.print(str = String.valueOf(-Math.round(isolines[i].value)));
                    if (i!=0)
                    {
                        out.print(','); spaces=1;
                        if ((length += str.length()) <= 40) continue;
                    }
                    out.println();
                    spaces = length = 8;
                }
                throw new ThreadDeath(); // Stop the program.
            }
            if (group==null)
            {
                throw new MissingParameterException("L'argument -group est obligatoire", "group");
            }
            groupID  = Integer.parseInt(group);
            database = new ImageDataBase();
            series   = database.getSeriesTable();
            format   = series.getFormat(groupID);
            series.close();
            if (format==null)
            {
                database.close();
                throw new IllegalArgumentException("Code de groupe inconnu: "+groupID);
            }
            isolines    = getIsolines(bathy);
            categories  = format.getCategoryLists();
            if (updateDB)
            {
                tableFiller = database.getTableFiller();
                tableFiller.setGroup(groupID);
            }
            else tableFiller = null;
        }
        catch (Exception exception)
        {
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
    public void run()
    {
        String methodName = "run";
        try
        {
            if (destination==null)
            {
                methodName="verify";
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY+2);
                for (int i=0; i<sources.length; i++)
                    verify(sources[i]);
            }
            else
            {
                methodName="process";
                for (int i=0; i<sources.length; i++)
                    if (!process(sources[i])) break;
            }
            methodName="close";
            tableFiller.close();
            database.close();
        }
        catch (Exception exception)
        {
            handleException(exception, methodName);
        }
    }

    /**
     * Read isolines and retains only the
     * ones specified in the argument list.
     */
    private Isoline[] getIsolines(final String args) throws IOException, TransformException
    {
        if (args==null) return null;
        final Isoline[] isolines = getIsolines();
        final List<Isoline> keep = new ArrayList<Isoline>(isolines.length);
        final StringTokenizer tk = new StringTokenizer(args, ",");
        while (tk.hasMoreTokens())
        {
            final float value = -Float.parseFloat(tk.nextToken());
            for (int i=0; i<isolines.length; i++)
                if (isolines[i].value == value)
                    keep.add(isolines[i]);
        }
        return keep.toArray(new Isoline[keep.size()]);
    }

    /**
     * Read all isolines. This method first try to load isolines
     * from "applicationData/cache/Méditerranée.serialized".  If
     * this loading fails, then try to load
     * "compilerData/map/Méditerranée.asc".
     */
    private Isoline[] getIsolines() throws IOException, TransformException
    {
        final ClassLoader loader = Processor.class.getClassLoader();
        final URL cacheURL = loader.getResource(CACHED_BATHY);
        if (cacheURL!=null)
        {
            final ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cacheURL.openStream()));
            try
            {
                final Isoline[] isolines = (Isoline[]) in.readObject();
                in.close();
                return isolines;
            }
            catch (IOException exception)
            {
                Utilities.unexpectedException("fr.ird", "Processor", "getIsolines", exception);
                in.close();
            }
            catch (ClassNotFoundException exception)
            {
                Utilities.unexpectedException("fr.ird", "Processor", "getIsolines", exception);
                in.close();
            }
        }
        //
        // Failed to load the bathymetry from the cache.
        // Try to load it from the GEBCO ASCII file.
        //
        final GEBCOReader reader = new GEBCOReader();
        final URL sourceURL = loader.getResource(SOURCE_BATHY);
        if (sourceURL==null)
        {
            throw new FileNotFoundException(SOURCE_BATHY);
        }
        reader.setInput(sourceURL);
        final Isoline[] isolines = reader.read();
        final NumberFormat pf=NumberFormat.getPercentInstance(locale);
        for (int i=0; i<isolines.length; i++)
        {
            final Isoline iso=isolines[i];
            out.print("Isoligne ");
            out.print(iso.value);
            out.print(" décimée de ");
            out.println(pf.format(iso.compress(0.75f)));
        }
        //
        // Save the bathymetry in the cache for future use.
        // The working directory must be set to the fr.ird
        // root.
        //
        final ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(CACHED_BATHY)));
        out.writeObject(isolines);
        out.close();
        return isolines;
    }

    /**
     * Returns a list of files for the specified parameter.
     * This method is for internal use by the constructor.
     */
    private File[] getFiles(final String parameter)
    {
        final String path = getParameter(parameter);
        if (path!=null)
        {
            File file = new File(path);
            String name = file.getName();
            if (name.indexOf('*')>=0 || name.indexOf('?')>=0)
            {
                file = file.getParentFile();
                if (file==null) file=new File(".");
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
    private GridCoverage load(final File file, final boolean reproject) throws IOException
    {
        if (exchange==null)
        {
            exchange = new GridCoverageExchange(categories);
            exchange.setLocale(locale);
        }
        GridCoverage coverage = exchange.createFromName(file.getPath());
        if (reproject)
        {
            CoordinateSystem sourceCS = coverage.getCoordinateSystem();
            CoordinateSystem targetCS = GeographicCoordinateSystem.WGS84;
            if (sourceCS instanceof CompoundCoordinateSystem)
            {
                targetCS = new CompoundCoordinateSystem(targetCS.getName(null), targetCS,
                                      ((CompoundCoordinateSystem) sourceCS).getTailCS());
            }
            if (processor==null)
            {
                processor = GridCoverageProcessor.getDefault();
            }
            final Operation operation = processor.getOperation("Resample");
            final ParameterList param = operation.getParameterList();
            param.setParameter("Source",           coverage);
            param.setParameter("CoordinateSystem", targetCS);
            if (interpolation!=null)
            {
                param.setParameter("InterpolationType", interpolation);
            }
            coverage = processor.doOperation(operation, param);
        }
        return coverage;
    }

    /**
     * Load and display an image. Image's properties will be dumped
     * on the standard output stream.
     *
     * @param  file The file to process.
     * @throws IOException if an error occured while reading the file.
     */
    private void verify(final File file) throws IOException
    {
        final GridCoverage coverage = load(file, false);
        out.println();
        out.println(exchange.getLastProperties());
        out.println(coverage.getCoordinateSystem());

        if (tabbedPane==null)
        {
            tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
            final JFrame frame = new JFrame("Images");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(tabbedPane);
            frame.setSize(400, 400);
            frame.show();
        }
        final MapPanel map = new MapPanel(coverage.getCoordinateSystem());
        final GridCoverageLayer image = new GridCoverageLayer(coverage);
        image.setZOrder(Float.NEGATIVE_INFINITY);
        map.addLayer(image);
        if (isolines!=null)
        {
            for (int i=0; i<isolines.length; i++)
            {
                final IsolineLayer layer = new IsolineLayer(isolines[i]);
                layer.setContour(Color.white);
                map.addLayer(layer);
            }
        }
        EventQueue.invokeLater(new Runnable()
        {
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
    private boolean process(final File file) throws IOException, SQLException
    {
        String filename = file.getName();
        out.print(filename);
        out.print(": Chargement...\r");
        out.flush();
        final GridCoverage coverage = load(file, true);

        filename = exchange.getOutputFilename();
        final int index = filename.lastIndexOf('.');
        if (index>=0)
        {
            filename = filename.substring(0, index);
        }
        final File output = new File(destination, filename+".png");
        if (output.exists())
        {
            out.print  ("Le fichier \"");
            out.print  (output.getPath());
            out.println("\" existe déjà.");
            out.println("Abandon de l'opération");
            return false;
        }

        out.print(filename);
        out.print(": Projection...\r");
        out.flush();
        final BufferedImage image = ((PlanarImage) coverage.getRenderedImage(false)).getAsBufferedImage();

        if (isolines!=null) try
        {
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
            for (int i=0; i<isolines.length; i++)
            {
                final Isoline isoline = isolines[i];
                try
                {
                    isoline.setCoordinateSystem(coverage.getCoordinateSystem());
                    gr.draw(isoline);
                }
                catch (TransformException exception)
                {
                    handleException(exception, "process");
                }
            }
            gr.dispose();
        }
        catch (NoninvertibleTransformException exception)
        {
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

        if (tableFiller!=null)
        {
            tableFiller.addImage(coverage, filename);
        }
        return true;
    }

    /**
     * Invoked when an operation failed. The exception message
     * is printed and the exception trace is logged.
     */
    private void handleException(final Exception exception, final String methodName)
    {
        out.print(Utilities.getShortClassName(exception));
        out.print(": ");
        out.println(exception.getLocalizedMessage());
        //
        // Log the error using FINE level (instead of WARNING)
        // since we don't want it to be dumped on the console
        // stream.
        //
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(exception));
        final String message = exception.getLocalizedMessage();
        if (message!=null)
        {
            buffer.append(": ");
            buffer.append(message);
        }
        final LogRecord record = new LogRecord(Level.WARNING, buffer.toString());
        record.setSourceClassName ("Processor");
        record.setSourceMethodName(methodName);
        record.setThrown          (exception);
        silentLog(record);
    }

    /**
     * Log a message to the stream, but not to the console.
     */
    private static void silentLog(final LogRecord record)
    {
        Logger logger = Logger.getLogger("fr.ird.io.coverage");
        while (logger!=null)
        {
            final Handler[] handlers = logger.getHandlers();
            for (int i=0; i<handlers.length; i++)
            {
                final Handler handler = handlers[i];
                if (!(handler instanceof ConsoleHandler))
                {
                    handler.publish(record);
                }
            }
            if (!logger.getUseParentHandlers()) break;
            logger = logger.getParent();
        }
    }
}
