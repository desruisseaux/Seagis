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
package fr.ird.database;

// Logging
import java.util.logging.Level;


/**
 * Niveau pour enregistrer des instructions SQL dans le journal des événements.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SQLLevel extends Level {
    /**
     * Le niveau pour enregistrer les instruction SELECT dans le {@linkplain Logger journal}.
     */
    public static final Level SQL_SELECT = new SQLLevel("SQL SELECT", FINE.intValue()+50);

    /**
     * Le niveau pour enregistrer les instruction UPDATE dans le {@linkplain Logger journal}.
     */
    public static final Level SQL_UPDATE = new SQLLevel("SQL UPDATE", INFO.intValue()-50);

    /**
     * Construit un nouveau niveau.
     *
     * @param name  Le nom du niveau, par exemple "SQL_UPDATE".
     * @param value Valeur entière pour le niveau.
     */
    private SQLLevel(final String name, final int value) {
        super(name, value);
    }
}
