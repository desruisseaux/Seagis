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
package net.seas.util;

// Input/output
import java.io.PrintWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import net.seas.io.TableWriter;

// Miscellaneous
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.prefs.Preferences;


/**
 * Abstract class for console application.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class Console
{
    /**
     * Command-line arguments. Elements are set to
     * <code>null</code> after they have been processed.
     */
    private final String[] arguments;

    /**
     * Console output stream.
     */
    protected final PrintWriter out;

    /**
     * Locale for projection names, or <code>null</code> for default locale.
     */
    protected final Locale locale;

    /**
     * Construct a console.
     *
     * @param args Command line arguments. Arguments "-encoding" and "-locale" will be
     *             handle and set to <code>null</code>. Other arguments will be ignored.
     */
    protected Console(final String[] args)
    {
        this.arguments  = (String[]) args.clone();
        this.locale     = getLocale(getParameter("-locale"));
        String encoding = getParameter("-encoding");
        boolean prefEnc = false;
        if (encoding==null)
        {
            encoding = Preferences.userNodeForPackage(this).get("Console encoding", null);
            prefEnc  = true;
        }
        if (encoding!=null) try
        {
            out = new PrintWriter(new OutputStreamWriter(System.out, encoding));
            if (!prefEnc)
            {
                Preferences.userNodeForPackage(this).put("Console encoding", encoding);
            }
        }
        catch (UnsupportedEncodingException exception)
        {
            UnsupportedCharsetException e=new UnsupportedCharsetException(encoding);
            e.initCause(exception);
            throw e;
        }
        else
        {
            out = new PrintWriter(System.out);
        }
    }

    /**
     * Returns the specified locale.
     *
     * @param  locale The programmatic locale string (e.g. "fr_CA").
     * @return The locale, or <code>null</code> if <code>locale</code> was null.
     * @throws IllegalArgumentException if the locale string is invalid.
     */
    private static Locale getLocale(final String locale) throws IllegalArgumentException
    {
        if (locale==null) return null;
        final String[] s = Pattern.compile("_").split(locale);
        switch (s.length)
        {
            case 1:  return new Locale(s[0]);
            case 2:  return new Locale(s[0], s[1]);
            case 3:  return new Locale(s[0], s[1], s[2]);
            default: throw new IllegalArgumentException("Bad local: "+locale);
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
    protected final String getParameter(final String parameter) throws IllegalArgumentException
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
                    throw new IllegalArgumentException("Missing argument for \""+arg+'"');
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
    protected final boolean getFlag(final String flag)
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
     * Run the commands. The default implementation just check if there is
     * some unprocessed command. Subclasses should override this method and
     * class <code>super.run()</code> after they have processed their arguments.
     */
    protected void run()
    {
        for (int i=0; i<arguments.length; i++)
            if (arguments[i] != null)
                throw new IllegalArgumentException("Unknow option: "+arguments[i]);
    }
}
