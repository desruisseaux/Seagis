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
package fr.ird.sql;

// Logging
import java.util.logging.Level;


/**
 * Level for SQL instructions. This is used
 * for logging some SQL queries and updates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class SQLLevel extends Level
{
    /**
     * The level for logging UPDATE instructions.
     */
    public static final Level SQL_UPDATE = new SQLLevel("SQL UPDATE", 0);

    /**
     * Construct a new level.
     *
     * @param name  The name of the level, for example "SQL UPDATE".
     * @param value An integer value for the level, starting from 0.
     *        0 is the highest level, 1 is the next one, etc.
     */
    private SQLLevel(final String name, final int value)
    {
        super(name, INFO.intValue()-17-value);
        if (intValue() <= CONFIG.intValue())
            throw new AssertionError(value);
    }
}
