/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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
package fr.ird.layer;

// Dependencies
import java.io.IOException;
import java.sql.SQLException;
import fr.ird.sql.image.ImageEntry;
import javax.swing.event.EventListenerList;


/**
 * Interface for map layers capable of adjusting their content to an {@link ImageEntry}.
 * Such layer will usually adjust their time range according the image's time range.
 *
 * @version 1.0
 * @author  administrateur
 */
public interface ImageAdjustable
{
    /**
     * Ajust this layer's content for the specified image.
     * This method can be invoked from any thread (may not
     * be the <i>Swing</i> thread).
     *
     * @param  image The image to adjust for.
     * @param  listeners Listener for reporting progress, or <code>null</code> if none.
     * @throws SQLException If an access to the underlying database failed.
     * @throws IOException  If an access to a file failed.
     */
    public abstract void adjust(final ImageEntry image, final EventListenerList listeners) throws SQLException, IOException;
}
