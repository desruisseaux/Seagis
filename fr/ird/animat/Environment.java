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
package fr.ird.animat;

// Divers
import java.util.Date;
import org.geotools.gc.GridCoverage;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Environment
{
    /**
     * Définit la date courante.
     */
    public abstract void setTime(final Date newTime);

    /**
     * Retourne le nombre de paramètres
     * compris dans cet environnement.
     */
    public abstract int getParameterCount();

    /**
     * Retourne l'image courante.
     *
     * @param  parameter Index du paramètre dont on veut l'image.
     * @return L'image courange, ou <code>null</code> s'il n'y en a pas.
     */
    public abstract GridCoverage getGridCoverage(final int parameter);

    /**
     * Déclare un objet à informer des changements survenant dans cet
     * environnement. Ces changements suviennent souvent suite à un
     * appel de {@link #setTime}.
     */
    public abstract void addEnvironmentChangeListener(final EnvironmentChangeListener listener);

    /**
     * Retire un objet à informer des changements survenant dans cet
     * environnement.
     */
    public abstract void removeEnvironmentChangeListener(final EnvironmentChangeListener listener);
}
