/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
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
     * de d�coder les valeurs des param�tres g�ophysiques. Cette m�thode peut
     * retourner plusieurs objets {@link SampleDimension}, un par bande. Leur
     * type (g�ophysique ou non) correspond au type des images dans leur format
     * natif. Par exemple les valeurs des pixels seront des entiers
     * (<code>{@link SampleDimension#geophysics geophysics}(false)</code>)
     * si l'image est enregistr�e au format PNG, tandis que les plages de valeurs
     * peuvent �tre des nombres r�els
     * (<code>{@link SampleDimension#geophysics geophysics}(true)</code>)
     * si l'image est enregistr�e dans un format brut ou ASCII.
     */
    public abstract SampleDimension[] getSampleDimensions();
}
