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

// Logging
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.logging.SimpleFormatter;


/**
 * Simple formatter adding a new-line after each record.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class InterlineFormatter extends SimpleFormatter
{
    /**
     * The line separator.
     */
    private final String lineSeparator = System.getProperty("line.separator", "\n");

    /**
     * The line separator repeated twice.
     */
    private final String doubleLineSeparator = lineSeparator + lineSeparator;

    /**
     * Buffer for formatting messages.
     */
    private final StringBuffer buffer = new StringBuffer();

    /**
     * Only {@link #init} can construct instance of this class.
     */
    private InterlineFormatter()
    {}

    /**
     * Format the given log record and return the formatted string.
     */
    public String format(final LogRecord record)
    {
        final String text = super.format(record);
        if (text.endsWith(doubleLineSeparator))
        {
            return text;
        }
        buffer.setLength(0);
        buffer.append(text);
        buffer.append(lineSeparator);
        return buffer.toString();
    }

    /**
     * Replace all {@link SimpleFormatter} with {@link InterlineFormatter}
     * for the specified logger and all its parents loggers.
     */
    public static void init(Logger logger)
    {
        while (logger!=null)
        {
            final Handler[] handlers = logger.getHandlers();
            for (int i=0; i<handlers.length; i++)
            {
                final Handler     handler = handlers[i];
                final Formatter formatter = handler.getFormatter();
                if (formatter.getClass().equals(SimpleFormatter.class))
                {
                    handler.setFormatter(new InterlineFormatter());
                }
            }
            if (!logger.getUseParentHandlers()) break;
            logger = logger.getParent();
        }
    }
}
