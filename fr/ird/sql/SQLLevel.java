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
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SQLLevel extends Level {
    /**
     * The level for logging SELECT instructions.
     */
    public static final Level SQL_SELECT = new SQLLevel("SQL SELECT", FINE.intValue()+50);

    /**
     * The level for logging UPDATE instructions.
     */
    public static final Level SQL_UPDATE = new SQLLevel("SQL UPDATE", INFO.intValue()-50);

    /**
     * Construct a new level.
     *
     * @param name  The name of the level, for example "SQL UPDATE".
     * @param value An integer value for the level.
     */
    private SQLLevel(final String name, final int value) {
        super(name, value);
    }
}
