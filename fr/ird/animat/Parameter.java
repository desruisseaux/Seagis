/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat;

// J2SE dependencies
import java.awt.geom.Point2D;


/**
 * Un param�tre observ� par les {@linkplain Animal animaux}. La classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plut�t des indications sur ces
 * {@linkplain Observation observations}, un peu comme des noms de colonnes dans un tableau.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see Environment#getParameters
 * @see Animal#getObservations
 */
public interface Parameter {
    /**
     * Retourne le nom de ce param�tre. En g�n�ral, la m�thode {@link #toString}
     * retournera aussi ce m�me nom afin de faciliter l'insertion des param�tres
     * dans une interface graphique <cite>Swing</cite> (par exemple une liste
     * d�roulante).
     */
    String getName();
}
