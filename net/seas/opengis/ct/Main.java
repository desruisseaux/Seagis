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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.ct;

// Coordinate systems
import net.seas.opengis.cs.Ellipsoid;

// Miscellaneous
import java.io.IOException;
import java.awt.geom.Point2D;
import java.text.NumberFormat;

import net.seas.util.XClass;
import net.seas.util.Console;
import net.seas.io.TableWriter;
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
        this.factory = new MathTransformFactory();
    }

    /**
     * Run the commands.
     */
    protected void run()
    {
        final boolean list = getFlag("-list");
        final boolean test = getFlag("-test");
        super.run();
        if      (test) availableTransforms(true);
        else if (list) availableTransforms(false);
    }

    /**
     * Print a table of available transforms.
     *
     * @param test <code>true</code> to run the test, or
     *        <code>false</code> to do nothing more than
     *        list transforms.
     */
    public void availableTransforms(final boolean test)
    {
        final Resources resources = Resources.getResources(null);
        final String[] transforms = factory.getAvailableTransforms();
        final TableWriter   table = new TableWriter(out, "  \u2502  ");
        String[]  transformErrors = null;

        out.println();
        out.print(' ');
        out.println(resources.getLabel(Cl�.AVAILABLE_TRANSFORMS));

        //////////////////////////////
        ///   Write column titles  ///
        //////////////////////////////
        table.setMultiLinesCells(true);
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_CENTER);
        table.write(resources.getString(Cl�.CLASSIFICATION_NAME));
        table.nextColumn();
        table.write(resources.getString(Cl�.LOCALIZED_NAME�1, locale.getDisplayLanguage()));
        if (test)
        {
            table.nextColumn();
            table.write("Erreur (m)");

            ///////////////////////
            ///   Perform test  ///
            ///////////////////////
            final double            x0 = 360*Math.random()-180;
            final double            y0 = 180*Math.random()- 90;
            final Point2D.Double point = new Point2D.Double(x0,y0);
            final int[]          count = new int[transforms.length];
            final double[]         sum = new double[transforms.length];
            final MapProjection[]   tr = new MapProjection[transforms.length];
            final Ellipsoid  ellipsoid = Ellipsoid.WGS84;
            for (int i=0; i<tr.length; i++)
            {
                final MathTransform mtr = factory.createParameterizedTransform(transforms[i], ellipsoid, point);
                if (mtr instanceof MapProjection) tr[i] = (MapProjection) mtr;
            }

            for (int j=0; j<10000; j++)
            {
                final double x = x0 + Math.max(-180, Math.min(+180, (20*Math.random()-10)));
                final double y = y0 + Math.max( -90, Math.min( +90, (20*Math.random()-10)));
                for (int i=0; i<tr.length; i++)
                {
                    final MapProjection projection=tr[i];
                    if (projection==null) continue;
                    point.x = x;
                    point.y = y;
                    try
                    {
                        projection.inverseTransform(projection.transform(point, point), point);
                        sum  [i] += ellipsoid.orthodromicDistance(x, y, point.x, point.y);
                        count[i]++;
                    }
                    catch (TransformException exception)
                    {
                        transformErrors[i] = exception.getLocalizedMessage();
                        tr[i] = null;
                    }
                }
            }
            final NumberFormat nf = NumberFormat.getInstance(locale);
            nf.setMinimumFractionDigits(6);
            nf.setMaximumFractionDigits(6);
            transformErrors = new String[transforms.length];
            for (int i=0; i<transforms.length; i++)
            {
                if (transformErrors[i]==null && count[i]!=0)
                    transformErrors[i] = nf.format(sum[i]/count[i]);
            }
        }
        
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
            if (transformErrors!=null && transformErrors[i]!=null)
            {
                table.nextColumn();
                table.setAlignment(TableWriter.ALIGN_RIGHT);
                table.write(transformErrors[i]);
            }
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
     * Parse command line and run the application.
     */
    public static void main(final String[] args)
    {
        if (args.length==0)
        {
            System.out.println("Options:\n"+
                               "-list                 List available transforms\n"+
                               "-locale <name>        Locale to be used    (example: \"fr_CA\")\n"+
                               "-encoding <name>      Output encoding name (example: \"cp850\")");
            return;
        }
        try
        {
            new Main(args).run();
        }
        catch (RuntimeException exception)
        {
            System.out.print(XClass.getShortClassName(exception));
            System.out.print(": ");
            System.out.println(exception.getLocalizedMessage());
        }
    }
}
