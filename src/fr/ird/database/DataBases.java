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
package fr.ird.database;

// J2SE dependencies
import java.io.IOException;
import java.sql.SQLException;

// Seagis dependencies
import fr.ird.database.coverage.CoverageDataBase;


/**
 * Factory for {@linkplain CoverageDataBase coverage databases}.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
public final class DataBases {
    /**
     * Do not allow instantiation of this class.
     */
    private DataBases() {
    }

    /**
     * Returns a coverage database from the specified host. The special value
     * <code>"localhost"</code> search for a direct connection to a local
     * database.
     *
     * @param  host The host, or <code>"localhost"</code>.
     * @return The coverage database from the specified host.
     */
    public static CoverageDataBase getCoverageDataBase(final String host) throws IOException {
        if (!host.equalsIgnoreCase("localhost")) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
        try {
            return new fr.ird.database.coverage.sql.CoverageDataBase();
        } catch (SQLException exception) {
            throw new CatalogException(exception);
        }
    }
}
