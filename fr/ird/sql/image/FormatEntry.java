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
package fr.ird.sql.image;

// Divers
import fr.ird.sql.Entry;
import net.seagis.cv.CategoryList;
import net.seagis.resources.Utilities;


/**
 * Information sur un format. Un objet <code>FormatEntry</code> correspond à un
 * enregistrement de la base de données de formats d'images.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface FormatEntry extends Entry
{
    /**
     * Retourne les listes de catégories {@link CategoryList} qui permettent
     * de décoder les valeurs des paramètres géophysiques. Cette méthode peut
     * retourner plusieurs objets {@link CategoryList}, un par bande.
     */
    public abstract CategoryList[] getCategoryLists();
}
