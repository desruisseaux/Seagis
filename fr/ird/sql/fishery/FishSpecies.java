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
package fr.ird.sql.fishery;

// J2SE
import java.awt.Color;
import java.util.Locale;
import java.io.ObjectStreamException;

// Divers
import fr.ird.animat.impl.Species;
import org.geotools.util.WeakHashSet;
import org.geotools.resources.Utilities;


/**
 * Représentation d'une espèce de thon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FishSpecies extends Species {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -7520513685175179256L;

    /**
     * Ensemble des espèces déjà créée.
     */
    private static final WeakHashSet pool = new WeakHashSet();

    /**
     * Construit une nouvelle espèce. Le nom de cette espèce peut être exprimé
     * selon plusieurs langues. A chaque nom ({@link String}) est associé une
     * langue ({@link Locale}).
     *
     * @param locales Langues des noms de cette espèces. Ce tableau doit avoir
     *        la même longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas cloné</strong>.  Evitez donc de le modifier
     *        après la construction.
     * @param names  Nom de cette espèce selon chacune des langues énumérées dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        cloné</strong>. Evitez donc de le modifier après la construction.
     * @param color Couleur par défaut à utiliser pour le traçage.
     *
     * @throws IllegalArgumentException Si un des éléments du tableau
     *         <code>locales</code> apparaît plus d'une fois.
     */
    public FishSpecies(final Locale[] locales,
                       final String[] names,
                       final Color color)
            throws IllegalArgumentException
    {
        super(locales, names, color);
    }

    /**
     * Retourne un exemplaire unique de cette espèce. Si un exemplaire
     * existe déjà dans la machine virtuelle, il sera retourné. Sinon,
     * cette méthode retourne <code>this</code>. Cette méthode est
     * habituellement appelée après la construction.
     */
    public FishSpecies intern() {
        return (FishSpecies)pool.canonicalize(this);
    }

    /**
     * Après la lecture binaire, vérifie si
     * l'espèce lue existait déjà en mémoire.
     */
    private Object readResolve() throws ObjectStreamException {
        return intern();
    }
}
