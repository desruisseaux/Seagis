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
 * Repr�sentation d'une esp�ce de thon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FishSpecies extends Species {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -7520513685175179256L;

    /**
     * Ensemble des esp�ces d�j� cr��e.
     */
    private static final WeakHashSet pool = new WeakHashSet();

    /**
     * Construit une nouvelle esp�ce. Le nom de cette esp�ce peut �tre exprim�
     * selon plusieurs langues. A chaque nom ({@link String}) est associ� une
     * langue ({@link Locale}).
     *
     * @param locales Langues des noms de cette esp�ces. Ce tableau doit avoir
     *        la m�me longueur que l'argument <code>names</code>. <strong>NOTE:
     *        Ce tableau n'est pas clon�</strong>.  Evitez donc de le modifier
     *        apr�s la construction.
     * @param names  Nom de cette esp�ce selon chacune des langues �num�r�es dans
     *        l'argument <code>locales</code>. <strong>NOTE: Ce tableau n'est pas
     *        clon�</strong>. Evitez donc de le modifier apr�s la construction.
     * @param color Couleur par d�faut � utiliser pour le tra�age.
     *
     * @throws IllegalArgumentException Si un des �l�ments du tableau
     *         <code>locales</code> appara�t plus d'une fois.
     */
    public FishSpecies(final Locale[] locales,
                       final String[] names,
                       final Color color)
            throws IllegalArgumentException
    {
        super(locales, names, color);
    }

    /**
     * Retourne un exemplaire unique de cette esp�ce. Si un exemplaire
     * existe d�j� dans la machine virtuelle, il sera retourn�. Sinon,
     * cette m�thode retourne <code>this</code>. Cette m�thode est
     * habituellement appel�e apr�s la construction.
     */
    public FishSpecies intern() {
        return (FishSpecies)pool.canonicalize(this);
    }

    /**
     * Apr�s la lecture binaire, v�rifie si
     * l'esp�ce lue existait d�j� en m�moire.
     */
    private Object readResolve() throws ObjectStreamException {
        return intern();
    }
}
