/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.sql.image;

// Miscellaneous
import java.io.File;
import java.sql.SQLException;
import net.seagis.gc.GridCoverage;


/**
 * Interface for objects capable to insert records into some of the database's tables.
 * For example, this interface can be used for adding new entries in the Images" table,
 * which imply adding automatically new entries into the "Areas" table when necessary.
 * Typically, this interface add new rows to the database but doesn't modify existing
 * one. It doesn't change table's structure neither.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface TableFiller
{
    /**
     * Set the group into which inserting new image. Images
     * will be inserted using the {@link #addImage} method.
     *
     * @param  ID The ID for the image's group. This is the
     *         ID that will be copied in the "groupe" column.
     * @throws SQLException if the operation failed.
     */
    public void setGroup(final int ID) throws SQLException;

    /**
     * Add an entry to the <code>Images</code> table. The {@link #setGroup}
     * method must have been invoked at least once before this method.
     *
     * @param  coverage The coverage to add. This coverage should have a three-dimensional
     *         envelope, the third dimension being along the time axis.
     * @param  filename The filename for the grid coverage, without extension.
     *         This is the filename that will be copied in the "filename" column.
     * @return <code>true</code> if the image has been added to the database, or
     *         <code>false</code> if an image with the same filename was already
     *         presents in the database. In the later case, the database is not
     *         updated and a information message is logged.
     * @throws SQLException if the operation failed.
     */
    public boolean addImage(final GridCoverage coverage, final String filename) throws SQLException;

    /**
     * Dispose resources used by the updater.
     *
     * @throws SQLException if the operation failed.
     */
    public void close() throws SQLException;
}
