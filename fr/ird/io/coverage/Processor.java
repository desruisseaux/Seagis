/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le D�veloppement
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
import fr.ird.io.IsolineFactory;

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
 * Importe dans la base de donn�es les fichiers ERDAS fournis par la station
 * des Canaris. Cette classe peut lire les fichiers de donn�es brutes (RAW),
 * appliquer une projection cartographique inverse, les convertir en PNG et
 * ajouter les entr�s correspondantes dans la base de donn�es.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Processor extends Console
{
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
     * Liste des images � charger.
     */
    private final File[] sources;

    /**
     * R�pertoire de destination, ou <code>null</code>
     * pour afficher les images plut�t que de les �crire.
     */
    private final File destination;

    /**
     * The interpolation method, ou <code>null</code>
     * for default (nearest neighbor).
     */
    private final String interpolation;

    /**
     * Base de donn�es d'images.
     */
    private final ImageDataBase database;

    /**
     * Objet � utiliser pour mettre � jour la base de donn�es, ou
     * <code>null</code> s'il ne faut pas faire de mises � jour.
     */
    private final TableFiller tableFiller;

    /**
     * Listes des cat�gories pour chaque bandes des images � lire.
     * Chaque objet {@link CategoryList} correspond � une bande de
     * l'image.
     */
    private final CategoryList[] categories;

    /**
     * Isolignes formant la bathym�trie.
     */
    private final IsolineFactory isolineFactory;

    /**
     * Isolignes a afficher sur les images.
     */
    private final float[] isolines;

    /**
     * Panneau dans lequel placer les images affich�es.
     * Cette composante ne sera construite que la premi�re
     * fois o� elle sera n�cessaire.
     */
    private transient JTabbedPane tabbedPane;

    /**
     * Indique s'il faut afficher toute la trace
     * de l'exception en cas d'erreur.
     */
    private boolean stackTrace=false;

    /**
     * Point d'entr� du programme.
     *
     * @param args Arguments sp�cifi�s sur la ligne de commande.
     *        Si cette m�thode est appel�e sans arguments, alors
     *        un r�sum� des commandes disponibles sera affich�e.
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
                    "-sources=E:/Pouponni�re/Mediterranee/SST_new/md020117.txt"
                };
            }
            if (false) // Debug code for Chlorophylle-a
            {
                args = new String[]
                {
                    "-group=2041402270",
                    "-sources=E:/Pouponni�re/Mediterranee/CHLORO_trait�es/md_chl011007.txt"
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
            stackTrace       = hasFlag("-stackTrace");
            sources          = getFiles  ("-sources");
            updateDB         = hasFlag  ("-updateDB");
            group            = getParameter("-group");
            bathy            = getParameter("-bathy");
            destination      = getParameter("-destination");
            interpolation    = getParameter("-interpolation");
            isolineFactory   = new IsolineFactory("M�diterran�e");
            this.destination = (destination!=null) ? new File(destination) : null;
            checkRemainingArguments(0);
            if (sources==null)
            {
                /////////////////////////////////////////////////////
                ////                                             ////
                ////    If no input was specified, display       ////
                ////    the help screen and stop the program.    ////
                ////                                             ////
                /////////////////////////////////////////////////////
                out.println("Usage: -sources       [fichiers] (exemple: \"*.txt\")\n"+
                            "       -destination   [R�pertoire de destination]\n"+
                            "       -interpolation [NearestNeighbor (d�faut) | Bilinear | Bicubic]\n"+
                            "       -bathy         [profondeurs s�par�es par des virgules sans espace]\n"+
                            "       -group         [#ID dans la table \"Group\" de la base de donn�es]\n"+
                            "       -updateDB\n"+
                            "\n"+
                            "Les arguments \"-groups\" et \"-sources\" sont obligatoires. Les autres\n"+
                            "sont facultatifs. Si \"-destination\" est omis, chaque image lue sera\n"+
                            "affich�e � l'�cran mais ne sera pas enregistr�e.  Si \"-destination\"\n"+
                            "est pr�sent, alors les images seront au contraire enregistr�es sans\n"+
                            "�tre affich�es.\n"+
                            "\n"+
                            "Les profondeurs bathym�triques disponibles sont (en m�tres):");
                String str; int length=8,spaces=8;
                isolines = isolineFactory.getAvailableValues();
                for (int i=isolines.length; --i>=0;)
                {
                    out.print(Utilities.spaces(spaces));
                    out.print(str = String.valueOf(-Math.round(isolines[i])));
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
            /////////////////////////////////////////////
            ////                                     ////
            ////    Open the database connection.    ////
            ////                                     ////
            /////////////////////////////////////////////
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
            categories  = format.getCategoryLists();
            if (updateDB)
            {
                tableFiller = database.getTableFiller();
                tableFiller.setGroup(groupID);
            }
            else tableFiller = null;
            /////////////////////////////////////////////
            ////                                     ////
            ////    Get the requested bathymetry.    ////
            ////                                     ////
            /////////////////////////////////////////////
            if (bathy!=null)
            {
                final StringTokenizer tk = new StringTokenizer(bathy, ",");
                isolines = new float[tk.countTokens()];
                for (int i=0; tk.hasMoreTokens(); i++)
                {
                    isolines[i] = -Float.parseFloat(tk.nextToken());
                }
            }
            else isolines = null;
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
            if (tableFiller!=null)
            {
                tableFiller.close();
            }
            database.close();
        }
        catch (Exception exception)
        {
            handleException(exception, methodName);
        }
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
                final Isoline isoline = isolineFactory.get(isolines[i]);
                if (isoline!=null)
                {
                    final IsolineLayer layer = new IsolineLayer(isoline);
                    layer.setContour(Color.white);
                    map.addLayer(layer);
                }
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
            out.println("\" existe d�j�.");
            out.println("Abandon de l'op�ration");
            return false;
        }

        out.print(filename);
        out.print(": Projection...\r");
        out.flush();
        final BufferedImage image = ((PlanarImage) coverage.getRenderedImage(false)).getAsBufferedImage();

        if (isolines!=null) try
        {
            out.print(filename);
            out.print(": Bathym�trie...\r");
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
                final Isoline isoline = isolineFactory.get(isolines[i]);
                if (isoline!=null) try
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

        final LogRecord record = new LogRecord(Level.INFO, "Enregistrement de \""+filename+"\" termin�.");
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
        if (stackTrace)
        {
            exception.printStackTrace(out);
        }
        else
        {
            out.print(Utilities.getShortClassName(exception));
            out.print(": ");
            out.println(exception.getLocalizedMessage());
        }
        out.flush();
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