/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.animat.server.tuna;

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
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;

// Dates
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// Animats
import fr.ird.animat.server.Clock;


/**
 * Configuration de la simulation.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class Configuration {
    /**
     * Le fuseau horaire pour l'affichage de dates.
     */
    public final TimeZone timezone;

    /**
     * Le premier pas de temps de la simulation.
     */
    public final Clock firstTimeStep;

    /**
     * Pause à prendre entre deux pas de la simulation, en nombre de millisecondes.
     */
    public final long pause;

    /**
     * Distance maximale (en miles nautiques) que peut parcourir un thon en une journée.
     */
    public final double dailyDistance;

    /**
     * Rayon de perception des thons, en miles nautiques. Les thons ne "sentiront" pas les
     * paramètres en dehors de ce rayon.
     */
    public final double perceptionRadius;

    /**
     * Résolution désirée des images en degrés d'angle de longitude et de latitude.
     */
    public final double resolution;

    /**
     * Liste des séries, opérations et évaluateurs à utiliser.
     */
    public final Set<Parameter> parameters;

    /**
     * Espèces désignées par leurs codes de la FAO.
     */
    public final Set<String> species;

    /**
     * Les paramètres {@link #parameters} sous forme de tableau.  Ce tableau peut contenir un
     * élément suplémentaire pour {@link fr.ird.animat.server.Parameter#HEADING}. Ce tableau est
     * utilisé pour construire les objets  {@link Species}  en partageant le même tableau pour
     * chaque espèce.
     */
    final fr.ird.animat.server.Parameter[] parameterArray;

    /**
     * Construit une configuration à partir du fichier spécifié.
     */
    public Configuration(final File file) throws IOException {
        this(new BufferedReader(new FileReader(file)));
    }

    /**
     * Construit une configuration à partir de l'URL spécifié.
     */
    public Configuration(final URL url) throws IOException {
        this(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /**
     * Construit une configuration à partir du flot spécifié.
     * Le flot sera fermé une fois la lecture terminée.
     */
    private Configuration(final BufferedReader reader) throws IOException {
        final Properties properties;
        properties = loadProperties(reader);
        parameters = loadParameters(reader);
        reader.close();
        try {
            ////
            ////    FUSEAU HORAIRE
            ////
            timezone = TimeZone.getTimeZone(getProperty(properties, "TIME_ZONE"));
            ////
            ////    PAS DE TEMPS
            ////
            final long timeStep = Math.round((24.0*60*60*1000)*
                    Double.parseDouble(getProperty(properties, "TIME_STEP")));
            ////
            ////    DATE DE DEPART
            ////
            final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
            dateFormat.setTimeZone(timezone);
            final Date startTime = dateFormat.parse(getProperty(properties, "START_TIME"));
            firstTimeStep = Clock.createClock(startTime, new Date(startTime.getTime() + timeStep), timezone);
            ////
            ////    PAUSE ENTRE CHAQUE PAS DE TEMPS
            ////
            pause = Math.round(1000*Double.parseDouble(getProperty(properties, "PAUSE")));
            ////
            ////    RESOLUTION SPATIALE DES PIXELS
            ////
            resolution = Double.parseDouble(getProperty(properties, "RESOLUTION"))/60;
            ////
            ////    DISTANCE MAXIMALE PARCOURUE PAR JOUR
            ////
            dailyDistance = Double.parseDouble(getProperty(properties, "DAILY_DISTANCE"));
            ////
            ////    RAYON DE PERCEPTION DES THONS
            ////
            perceptionRadius = Double.parseDouble(getProperty(properties, "PERCEPTION_RADIUS"));
            ////
            ////    ESPECES (CODES DE LA FAO)
            ////
            final Set<String> species = new HashSet<String>();
            if (true) {
                final StringTokenizer tk = new StringTokenizer(getProperty(properties, "SPECIES"), ",");
                while (tk.hasMoreTokens()) {
                    species.add(tk.nextToken().trim());
                }
            }
            this.species = Collections.unmodifiableSet(species);
            //
            //      (fin)
            //
        } catch (ParseException exception) {
            final IOException e = new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        } catch (NumberFormatException exception) {
            final IOException e = new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        final int paramCount = parameters.size();
        if (false) {
            // Inclus HEADING
            parameterArray = (fr.ird.animat.server.Parameter[])parameters.toArray(new fr.ird.animat.server.Parameter[paramCount+1]);
            parameterArray[paramCount] = Parameter.HEADING;
        } else {
            parameterArray = (fr.ird.animat.server.Parameter[])parameters.toArray(new fr.ird.animat.server.Parameter[paramCount]);
        }
    }

    /**
     * Retourne la propriété spécifiée.
     *
     * @throws NoSuchElementException si la propriété n'est pas définie.
     */
    private static String getProperty(final Properties properties, final String key)
            throws NoSuchElementException
    {
        final String property = properties.getProperty(key);
        if (property == null) {
            throw new NoSuchElementException("La propriété \""+key+"\" n'est pas définie.");
        }
        return property;
    }

    /**
     * Charge les propriétés jusqu'à la ligne qui marque la fin des propriétés dans le fichier
     * de configuration. Ca sera la première ligne qui ne contient que le caractère '-' ou des
     * espaces. Le flot <code>reader</code> sera positionnée après cette ligne de démarcation.
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
     * Lit le tableau qui suit les propriétés et construit les objets {@link Parameter}
     * correspondants.
     */
    private static Set<Parameter> loadParameters(final BufferedReader reader) throws IOException {
        final Set<Parameter> parameters = new LinkedHashSet<Parameter>();
        final String[] args = new String[4];
        String line; while ((line=reader.readLine()) != null) {
            if ((line=line.trim()).length() == 0) {
                continue;
            }
            if (line.startsWith("#")) {
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
            try {
                parameters.add(new Parameter(args[0], args[1], args[2],
                                             Float.parseFloat(args[3])));
            } catch (NumberFormatException exception) {
                final IOException e = new IOException(exception.getLocalizedMessage());
                e.initCause(exception);
                throw e;
            }
        }
        return Collections.unmodifiableSet(parameters);
    }

    /**
     * Retourne l'écart de temps entre la date des paramètres à utiliser et la date
     * de la simulation.
     */
    final long getTimeLag() {
        long timelag = Long.MAX_VALUE;
        for (final Parameter param : parameters) {
            final long dt = param.timelag;
            if (dt < timelag) {
                timelag = dt;
            }
        }
        return timelag!=Long.MAX_VALUE ? timelag : 0;
    }

    /**
     * Retourne une chaîne de caractère représentant la configuration.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer("Configuration");
        final String lineSeparator = System.getProperty("line.separator", "\n");
        buffer.append(lineSeparator);
        for (final Parameter param : parameters) {
            buffer.append("    ");
            buffer.append(param);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
