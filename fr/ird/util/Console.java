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
package fr.ird.util;

// Input/output
import java.io.Writer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;

// Miscellaneous
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.prefs.Preferences;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.resources.Utilities;


/**
 * Abstract class for console application.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Console
{
    /**
     * The preference name for default encoding.
     */
    private static final String ENCODING = "Console encoding";

    /**
     * Command-line arguments. Elements are set to
     * <code>null</code> after they have been processed.
     */
    private final String[] arguments;

    /**
     * Console output stream.
     */
    public final PrintWriter out;

    /**
     * The locale.
     */
    public final Locale locale;

    /**
     * Construct a console.
     *
     * @param args Command line arguments. Arguments "-encoding", "-locale" and
     *             "-output" will be handle. Other arguments will be ignored.
     */
    public Console(final String[] args)
    {
        this.arguments     = (String[]) args.clone();
        this.locale        = getLocale(getParameter("-locale"));
        String encoding    = getParameter("-encoding");
        String destination = getParameter("-output");
        try
        {
            /*
             * If a destination file was specified,  open the file using the platform
             * default encoding or the specified encoding. Do not use encoding stored
             * in preference since they were usually for console encoding.
             */
            if (destination!=null)
            {
                final Writer fileWriter;
                if (encoding!=null)
                {
                    fileWriter = new OutputStreamWriter(new FileOutputStream(destination), encoding);
                }
                else
                {
                    fileWriter = new FileWriter(destination);
                }
                out = new PrintWriter(fileWriter);
                return;
            }
            /*
             * If output to screen, fetch the encoding from user's preferences.
             */
            boolean prefEnc = false;
            if (encoding==null)
            {
                encoding = Preferences.userNodeForPackage(Console.class).get(ENCODING, null);
                prefEnc  = true;
            }
            if (encoding!=null)
            {
                out = new PrintWriter(new OutputStreamWriter(System.out, encoding), true);
                if (!prefEnc)
                {
                    Preferences.userNodeForPackage(Console.class).put(ENCODING, encoding);
                }
            }
            else
            {
                out = new PrintWriter(System.out, true);
            }
        }
        catch (UnsupportedEncodingException exception)
        {
            UnsupportedCharsetException e=new UnsupportedCharsetException(encoding);
            e.initCause(exception);
            throw e;
        }
        catch (IOException exception)
        {
            IllegalArgumentException e=new IllegalArgumentException(destination);
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Returns the specified locale.
     *
     * @param  locale The programmatic locale string (e.g. "fr_CA").
     * @return The locale, or the default one if <code>locale</code> was null.
     * @throws IllegalArgumentException if the locale string is invalid.
     */
    private static Locale getLocale(final String locale) throws IllegalArgumentException
    {
        if (locale==null) return Locale.getDefault();
        final String[] s = Pattern.compile("_").split(locale);
        switch (s.length)
        {
            case 1:  return new Locale(s[0]);
            case 2:  return new Locale(s[0], s[1]);
            case 3:  return new Locale(s[0], s[1], s[2]);
            default: throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_LOCALE_$1, locale));
        }
    }

    /**
     * Returns a parameter value from the command line. This method should be called
     * exactly once for each parameter. Second invocation for the same parameter will
     * returns <code>null</code> (unless the same parameter appears many times on the
     * command line).
     * <br><br>
     * Paramater may be instructions like "-encoding cp850" or "-encoding=cp850".
     * Both forms (with or without "=") are accepted.
     *
     * @param  parameter The parameter name.
     * @return The parameter value, of <code>null</code> if there is no parameter
     *         defined for the specified name.
     * @throws IllegalArgumentException if the parameter <code>name</code> exists
     *         but doesn't define any value.
     */
    public final String getParameter(final String parameter) throws IllegalArgumentException
    {
        for (int i=0; i<arguments.length; i++)
        {
            String arg=arguments[i];
            if (arg!=null)
            {
                arg = arg.trim();
                String value = "";
                int split = arg.indexOf('=');
                if (split>=0)
                {
                    value = arg.substring(split+1).trim();
                    arg = arg.substring(0, split).trim();
                }
                if (arg.equalsIgnoreCase(parameter))
                {
                    arguments[i] = null;
                    if (value.length()!=0)
                        return value;
                    while (++i<arguments.length)
                    {
                        value=arguments[i];
                        arguments[i]=null;
                        if (value==null) break;
                        value = value.trim();
                        if (split>=0) return value;
                        if (!value.equals("="))
                        {
                            return value.startsWith("=") ? value.substring(1).trim() : value;
                        }
                        split = 0;
                    }
                    throw new IllegalArgumentException(Resources.getResources(locale).
                              getString(ResourceKeys.ERROR_MISSING_ARGUMENT_VALUE_$1, arg));
                }
            }
        }
        return null;
    }

    /**
     * Returns <code>true</code> if the specified flag is set on the command line.
     * This method should be called exactly once for each flag. Second invocation
     * for the same flag will returns <code>false</code> (unless the same flag
     * appears many times on the command line).
     */
    public final boolean hasFlag(final String flag)
    {
        for (int i=0; i<arguments.length; i++)
        {
            String arg=arguments[i];
            if (arg!=null)
            {
                arg = arg.trim();
                if (arg.equalsIgnoreCase(flag))
                {
                    arguments[i] = null;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if there is some unprocessed command.
     *
     * @param  max Maximum remaining arguments autorized.
     * @return An array of remaining arguments. Will never be longer than <code>max</code>.
     * @throws IllegalArgumentException if there is more argument left than <code>max</code>.
     */
    public String[] checkRemainingArguments(final int max) throws IllegalArgumentException
    {
        int count=0;
        final String[] left = new String[max];
        for (int i=0; i<arguments.length; i++)
        {
            final String arg = arguments[i];
            if (arg != null)
            {
                if (count>=max)
                {
                    throw new IllegalArgumentException(Resources.getResources(locale).
                              format(ResourceKeys.ERROR_UNKNOW_OPTION_$1, arguments[i]));
                }
                left[count++] = arg;
            }
        }
        return XArray.resize(left, count);
    }

    /**
     * Gets a writer for the specified output stream.
     */
    public static Writer getWriter(final OutputStream out)
    {
        try
        {
            final String encoding = Preferences.userNodeForPackage(Console.class).get(ENCODING, null);
            if (encoding!=null) return new OutputStreamWriter(out, encoding);
        }
        catch (UnsupportedEncodingException exception)
        {
            Utilities.unexpectedException("fr.ird.util", "Console", "getWriter", exception);
        }
        return new OutputStreamWriter(out);
    }
}
