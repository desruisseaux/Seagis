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
package fr.ird.database.coverage;

// Geotools
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.Entry;


/**
 * Information sur un format d'image.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface FormatEntry extends Entry {
    /**
     * Retourne les listes des bandes {@link SampleDimension} qui permettent
     * de décoder les valeurs des paramètres géophysiques. Cette méthode peut
     * retourner plusieurs objets {@link SampleDimension}, un par bande.
     */
    public abstract SampleDimension[] getSampleDimensions();
}
