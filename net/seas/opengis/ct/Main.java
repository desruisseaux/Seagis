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
import java.awt.geom.Point2D;
import net.seas.util.XClass;
import net.seas.util.XString;
import net.seas.util.Console;
import net.seas.resources.Resources;


/**
 * Main class for Coordinate Transform package. This class is not part of OpenGIS specification,
 * so it is not public. It is provided as a convenient way to query Coordinate Transform services
 * from the command line.
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
     * Construct a console.
     *
     * @param args Command line arguments.
     */
    public Main(final String[] args)
    {
        super(args);
        this.factory = MathTransformFactory.DEFAULT;
    }

    /**
     * Parse command line and run the application.
     * Command-line arguments may be:
     *
     * <blockquote><pre>
     *  <b>-list</b> <i></i>           List available transforms
     *  <b>-locale</b> <i>name</i>     Locale to be used    (example: "fr_CA")
     *  <b>-encoding</b> <i>name</i>   Output encoding name (example: "cp850")
     * </pre></blockquote>
     */
    public static void main(final String[] args)
    {
        if (args.length==0)
        {
            System.out.println("Options:\n"+
                               "  -list              List available transforms\n"+
                               "  -locale <name>     Locale to be used    (example: \"fr_CA\")\n"+
                               "  -encoding <name>   Output encoding name (example: \"cp850\")");
        }
        else new Main(args).run();
    }

    /**
     * Run the commands.
     */
    protected void run()
    {
        try
        {
            final boolean list = getFlag("-list");
            final String[] toProject = checkRemainingArguments(1);
            if (list)
            {
                availableTransforms();
            }
            for (int i=0; i<toProject.length; i++)
            {
                project(new File(toProject[i]));
            }
        }
        catch (IllegalArgumentException exception)
        {
            out.println(exception.getLocalizedMessage());
        }
        catch (IOException exception)
        {
            out.println(exception.getLocalizedMessage());
        }
        catch (ParseException exception)
        {
            out.println(exception.getLocalizedMessage());
        }
        catch (TransformException exception)
        {
            out.println(exception.getLocalizedMessage());
        }
        out.flush();
    }

    /**
     * Print a table of available transforms.
     */
    public void availableTransforms()
    {
        final Resources resources = Resources.getResources(null);
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
        table.write(resources.getString(Clé.LOCALIZED_NAME¤1, locale.getDisplayLanguage()));
        
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
            final AssertionError error = new AssertionError(exception.getLocalizedMessage());
            error.initCause(exception);
            throw error;
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
        final Projection   projection = new Projection("Main", "Mercator_1SP", center);
        final MathTransform transform = factory.createParameterizedTransform(projection);
        final NumberFormat  outFormat = NumberFormat.getNumberInstance(locale);
        final CoordinateFormat format = new CoordinateFormat("D°MM.m'", locale);
        final TableWriter       table = new TableWriter(out, "  \u2502  ");
        final Resources     resources = Resources.getResources(null);
        outFormat.setMinimumFractionDigits(3);
        outFormat.setMaximumFractionDigits(3);
        out.println();
        out.print(' ');
        out.println(resources.format(Clé.PROJECTION¤1, transform.getName(null)));

        //////////////////////////////
        ///   Write column titles  ///
        //////////////////////////////
        table.setMultiLinesCells(true);
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_CENTER);
        table.write(resources.getString(Clé.GEOGRAPHIC_COORDINATE));
        table.nextColumn();
        table.write(resources.getString(Clé.PROJECTED_COORDINATE));
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_LEFT);

        //////////////////////////////
        ///   Project all points   ///
        //////////////////////////////
        for (final Iterator<Point2D> it=points.iterator(); it.hasNext();)
        {
            final Point2D geographic = it.next();
            final Point2D projected  = transform.transform(geographic, null);
            table.write(format.format(new CoordinatePoint(geographic)));
            table.nextColumn();
            final String xLength = outFormat.format(projected.getX()/1000); // Convert m --> km
            final String yLength = outFormat.format(projected.getY()/1000); // Convert m --> km
            table.write(XString.spaces(10-xLength.length())); table.write(xLength);
            table.write(XString.spaces(12-yLength.length())); table.write(yLength);
            table.nextLine();
        }
        table.writeHorizontalSeparator();
        table.flush();
    }
}
