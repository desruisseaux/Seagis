/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat.seas;

// Input/Output
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.util.StringTokenizer;
import java.util.Properties;
import java.net.URL;

// Collections
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;

// Dates
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// Animats
import fr.ird.animat.TimeStep;
import fr.ird.animat.Parameter;


/**
 * Configuration de la simulation.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class Configuration {
    /**
     * Le premier pas de temps de la simulation.
     */
    public final TimeStep firstTimeStep;

    /**
     * R�solution d�sir�e des images en degr�s d'angle de longitude et de latitude.
     */
    public final double resolution;

    /**
     * Liste des s�ries, op�rations et �valuateurs � utiliser.
     */
    public final Set<Parameter> parameters;

    /**
     * Construit une configuration � partir du fichier sp�cifi�.
     */
    public Configuration(final File file) throws IOException {
        this(new BufferedReader(new FileReader(file)));
    }

    /**
     * Construit une configuration � partir de l'URL sp�cifi�.
     */
    public Configuration(final URL url) throws IOException {
        this(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /**
     * Construit une configuration � partir du flot sp�cifi�.
     * Le flot sera ferm� une fois la lecture termin�e.
     */
    private Configuration(final BufferedReader reader) throws IOException {
        final Properties properties;
        properties = loadProperties(reader);
        parameters = loadParameters(reader);
        reader.close();
        try {
            final long timeStep = Math.round((24.0*60*60*1000)*
                    Double.parseDouble(getProperty(properties, "TIME_STEP")));

            final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
            final Date startTime = dateFormat.parse(getProperty(properties, "START_TIME"));
            firstTimeStep = new TimeStep(startTime, new Date(startTime.getTime() + timeStep));

            final long pause = Math.round(1000*
                    Double.parseDouble(getProperty(properties, "PAUSE")));

            resolution = Double.parseDouble(getProperty(properties, "RESOLUTION"))/60;

            final double moveDistance = (timeStep!=0 ? timeStep*1852 : 1852)*
                    Double.parseDouble(getProperty(properties, "DAILY_DISTANCE"));
        } catch (ParseException exception) {
            final IOException e = new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Retourne la propri�t� sp�cifi�e.
     *
     * @throws NoSuchElementException si la propri�t� n'est pas d�finie.
     */
    private static String getProperty(final Properties properties, final String key)
            throws NoSuchElementException
    {
        final String property = properties.getProperty(key);
        if (property == null) {
            throw new NoSuchElementException("La propri�t� \""+key+"\" n'est pas d�finie.");
        }
        return property;
    }

    /**
     * Charge les propri�t�s jusqu'� la ligne qui marque la fin des propri�t�s dans le fichier
     * de configuration. Ca sera la premi�re ligne qui ne contient que le caract�re '-' ou des
     * espaces. Le flot <code>reader</code> sera positionn�e apr�s cette ligne de d�marcation.
     */
    private static Properties loadProperties(final BufferedReader reader) throws IOException {
        final StringBuffer  buffer = new StringBuffer();
        final String lineSeparator = System.getProperty("line.separator", "\n");
        String line; while ((line=reader.readLine()) != null) {
            boolean isSeparator = false;
            for (int i=line.length(); --i>=0;) {
                final char c = line.charAt(i);
                if (Character.isWhitespace(c)) {
                    continue;
                }
                if (c != '-') {
                    isSeparator = false;
                    break;
                }
                isSeparator = true;
            }
            if (isSeparator) {
                break;
            }
            buffer.append(line);
            buffer.append(lineSeparator);
        }
        final InputStream in = new ByteArrayInputStream(buffer.toString().getBytes());
        final Properties properties = new Properties();
        properties.load(in);
        in.close();
        return properties;
    }

    /**
     * Lit le tableau qui suit les propri�t�s et construit les objets {@link Parameter}
     * correspondants.
     */
    private static Set<Parameter> loadParameters(final BufferedReader reader) throws IOException {
        final Set<Parameter> parameters = new LinkedHashSet<Parameter>();
        final String[] args = new String[3];
        String line; while ((line=reader.readLine()) != null) {
            if ((line=line.trim()).length() == 0) {
                continue;
            }
            final StringTokenizer tokens = new StringTokenizer(line, ";");
            Arrays.fill(args, null);
            for (int i=0; i<args.length; i++) {
                if (!tokens.hasMoreTokens()) {
                    break;
                }
                final String token = tokens.nextToken().trim();
                if (token.length() != 0) {
                    args[i] = token;
                }
            }
            parameters.add(new fr.ird.animat.seas.Parameter(args[0], args[1], args[2]));
        }
        return Collections.unmodifiableSet(parameters);
    }

    /**
     * Retourne une cha�ne de caract�re repr�sentant la configuration.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer("Configuration");
        final String lineSeparator = System.getProperty("line.separator", "\n");
        buffer.append(lineSeparator);
        for (final Iterator<Parameter> it=parameters.iterator(); it.hasNext();) {
            buffer.append("    ");
            buffer.append(it.next());
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}