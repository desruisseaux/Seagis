/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
 */
package fr.ird.database.sql;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Logging level for SQL instructions related to {@link fr.ird.database.DataBase} operations.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see fr.ird.database.coverage.CoverageDataBase#LOGGER
 * @see fr.ird.database.sample.SampleDataBase#LOGGER
 */
public final class LoggingLevel extends Level {
    /**
     * {@linkplain Logger Logging} level for SELECT instructions.
     */
    public static final Level SELECT = new LoggingLevel("SQL SELECT", FINE.intValue()+50);

    /**
     * {@linkplain Logger Logging} level for UPDATE instructions.
     */
    public static final Level UPDATE = new LoggingLevel("SQL UPDATE", INFO.intValue()-50);

    /**
     * Construct a new logging level.
     *
     * @param name  The logging level, e.g. "SQL_UPDATE".
     * @param value The level value.
     */
    private LoggingLevel(final String name, final int value) {
        super(name, value);
    }
}
