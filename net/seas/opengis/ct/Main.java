/*
 * OpenGIS implementation in Java
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
package net.seas.opengis.ct;

// Coordinate systems
import net.seas.opengis.cs.Ellipsoid;
import net.seas.opengis.cs.Projection;
import net.seas.opengis.pt.CoordinatePoint;

// Input/output
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import net.seas.io.TableWriter;

// Parsing/formatting
import java.text.NumberFormat;
import java.text.ParseException;
import net.seas.text.CoordinateFormat;

// Collections
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

// Miscellaneous
import java.util.Locale;
import java.awt.geom.Point2D;
import net.seas.util.XClass;
import net.seas.util.XString;
import net.seas.util.Console;
import net.seas.util.Version;
import net.seas.resources.Resources;


/**
 * Provides some coordinates services from the command line.
 * This class may be run from the command line. It accept
 * the following options:
 *
 * <blockquote><pre>
 *  <b>-help</b> <i></i>           Display command line options
 *  <b>-list</b> <i></i>           List available transforms
 *  <b>-locale</b> <i>name</i>     Locale to be used    (example: "fr_CA")
 *  <b>-encoding</b> <i>name</i>   Output encoding name (example: "cp850")
 * </pre></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Main extends Console
{
    /**
     * Factory instance for math transform.
     */
    private final MathTransformFactory factory;

    /**
     * Locale for meta-information (e.g. column titles). It doesn't
     * need to be the same locale than table contents. May be null
     * for the default locale.
     */
    private final Locale metaLocale;

    /**
     * Construct a console.
     *
     * @param args Command line arguments.
     */
    private Main(final String[] args)
    {
        super(args);
        this.factory = MathTransformFactory.DEFAULT;
        this.metaLocale = locale;
    }

    /**
     * Print help instructions.
     */
    public void help()
    {
        out.println("Command line tool for Coordinate Transformation Services\n"+
                    "Options:\n"+
                    "  -help              Display command line options\n"+
                    "  -list              List available transforms\n"+
                    "  -locale <name>     Locale to be used    (example: \"fr_CA\")\n"+
                    "  -encoding <name>   Output encoding name (example: \"cp850\")");
    }

    /**
     * Print a table of available transforms.
     */
    public void availableTransforms()
    {
        final Resources resources = Resources.getResources(metaLocale);
        final String[] transforms = factory.getAvailableTransforms();
        final TableWriter   table = new TableWriter(out, "  \u2502  ");

        out.println();
        out.print(' ');
        out.println(resources.getLabel(Clé.AVAILABLE_TRANSFORMS));

        //////////////////////////////
        ///   Write column titles  ///
        //////////////////////////////
        table.setMultiLinesCells(true);
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_CENTER);
        table.write(resources.getString(Clé.CLASSIFICATION_NAME));
        table.nextColumn();
        table.write(resources.getString(Clé.LOCALIZED_NAME¤1, locale.getDisplayLanguage(metaLocale)));
        
        ////////////////////////////////
        ///   Write transforms list  ///
        ////////////////////////////////
        table.writeHorizontalSeparator();
        for (int i=0; i<transforms.length; i++)
        {
            table.setAlignment(TableWriter.ALIGN_LEFT);
            table.write(transforms[i]);
            table.nextColumn();
            table.write(factory.getName(transforms[i], locale));
            table.nextLine();
        }
        table.writeHorizontalSeparator();
        
        /////////////////////////////////
        ///   Flush table to console  ///
        /////////////////////////////////
        try
        {
            table.flush();
        }
        catch (IOException exception)
        {
            // Should not happen.
            if (Version.MINOR>=4)
            {
                final AssertionError error = new AssertionError(exception.getLocalizedMessage());
                error.initCause(exception);
                throw error;
            }
            else throw new Error(exception.getLocalizedMessage());
            // Error is the first parent class of AssertionError
        }
    }

    /**
     * Project a list of points from a file.
     *
     * @throws IOException if an error occurs during file access.
     * @throws ParseException if an error occurs during parsing.
     * @throws TransformException if an error occurs during map projection.
     */
    public void project(final File file) throws IOException, ParseException, TransformException
    {
        ///////////////////////////
        ///   Read all points   ///
        ///////////////////////////
        final List<Point2D>      points = new ArrayList<Point2D>();
        final BufferedReader      input = new BufferedReader(new FileReader(file));
        final CoordinateFormat inFormat = new CoordinateFormat();
        final Point2D.Double     center = new Point2D.Double();
        String line; while ((line=input.readLine())!=null)
        {
            if ((line=line.trim()).length()!=0 && line.charAt(0)!='#')
            {
                line=line.replace('\t', ' ');
                final CoordinatePoint coord = inFormat.parse(line);
                center.x += coord.getOrdinate(0);
                center.y += coord.getOrdinate(1);
                points.add(coord.toPoint2D());
            }
        }
        input.close();
        center.x /= points.size();
        center.y /= points.size();

        ////////////////////////////
        ///   Setup projection   ///
        ////////////////////////////
        final Ellipsoid     ellipsoid = Ellipsoid.WGS84;
        final Projection   projection = new Projection("Main", "Mercator_1SP", ellipsoid, center);
        final MathTransform transform = factory.createParameterizedTransform(projection);
        final NumberFormat  outFormat = NumberFormat.getNumberInstance(locale);
        final CoordinateFormat format = new CoordinateFormat("D°MM.m'", locale);
        final TableWriter       table = new TableWriter(out, "  \u2502  ");
        final Resources     resources = Resources.getResources(metaLocale);
        outFormat.setMinimumFractionDigits(3);
        outFormat.setMaximumFractionDigits(3);
        out.println();
        out.print(' ');
        out.println(resources.getString(Clé.PROJECTION¤1, transform.getName(metaLocale)));

        //////////////////////////////
        ///   Write column titles  ///
        //////////////////////////////
        table.setMultiLinesCells(true);
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_CENTER);
        table.write(resources.getString(Clé.GEOGRAPHIC_COORDINATE));
        table.nextColumn();
        table.write(resources.getString(Clé.PROJECTED_COORDINATE));
        table.nextColumn();
        table.write("ortho. (km)");
        table.nextColumn();
        table.write("carte. (km)");
        table.writeHorizontalSeparator();

        //////////////////////////////
        ///   Project all points   ///
        //////////////////////////////
        Point2D lastGeographic = null;
        Point2D lastProjected  = null;
        for (final Iterator<Point2D> it=points.iterator(); it.hasNext();)
        {
            final Point2D geographic = it.next();
            final Point2D projected  = transform.transform(geographic, null);

            // Write geographic coordinates
            table.setAlignment(TableWriter.ALIGN_LEFT);
            table.write(format.format(new CoordinatePoint(geographic)));
            table.nextColumn();

            // Write projected coordinates
            final String xLength = outFormat.format(projected.getX()/1000); // Convert m --> km
            final String yLength = outFormat.format(projected.getY()/1000); // Convert m --> km
            table.write(XString.spaces(10-xLength.length())); table.write(xLength);
            table.write(XString.spaces(12-yLength.length())); table.write(yLength);
            table.nextColumn();
            table.setAlignment(TableWriter.ALIGN_RIGHT);

            // Write orthodromic distance with previous point
            if (lastGeographic!=null)
            {
                table.write(outFormat.format(ellipsoid.orthodromicDistance(lastGeographic, geographic)/1000));
            }
            table.nextColumn();

            // Write cartesian distance with previous point
            if (lastProjected!=null)
            {
                table.write(outFormat.format(lastProjected.distance(projected)/1000));
            }
            table.nextLine();

            // Continue loop...
            lastGeographic = geographic;
            lastProjected  = projected;
        }
        table.writeHorizontalSeparator();
        table.flush();
    }

    /**
     * Run the command-line tool.
     */
    public static void main(final String[] args)
    {
        final Main console = new Main(args);
        try
        {
            final boolean       list = console.hasFlag("-list");
            final boolean       help = console.hasFlag("-help");
            final String[] toProject = console.checkRemainingArguments(1);
            final boolean   noOption = !list && toProject.length==0;
            if (list) console.availableTransforms();
            if (help || noOption) console.help();
            for (int i=0; i<toProject.length; i++)
            {
                console.project(new File(toProject[i]));
            }
        }
        catch (IllegalArgumentException exception)
        {
            console.out.println(exception.getLocalizedMessage());
        }
        catch (IOException exception)
        {
            console.out.println(exception.getLocalizedMessage());
        }
        catch (ParseException exception)
        {
            console.out.println(exception.getLocalizedMessage());
        }
        catch (TransformException exception)
        {
            console.out.println(exception.getLocalizedMessage());
        }
        console.out.flush();
    }
}
