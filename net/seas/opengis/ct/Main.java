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

// Miscellaneous
import java.io.IOException;
import net.seas.util.XClass;
import net.seas.util.Console;
import net.seas.io.TableWriter;


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
        super.run();
        if (list) availableTransforms();
    }

    /**
     * Print a table of available transforms.
     */
    public void availableTransforms()
    {
        out.println();
        out.println(" Available transforms:");
        final String[] transforms = factory.getAvailableTransforms();
        final TableWriter table = new TableWriter(out, "  \u2502  ");
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_CENTER);
        table.write("Classification name\tLocalized name");
        table.writeHorizontalSeparator();
        table.setAlignment(TableWriter.ALIGN_LEFT);
        table.setMultiLinesCells(true);
        for (int i=0; i<transforms.length; i++)
        {
            table.write(transforms[i]);
            table.nextColumn();
            table.write(factory.getName(transforms[i], locale));
            table.nextLine();
        }
        table.writeHorizontalSeparator();
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
            System.out.println(
                    "Options:\n"+
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
