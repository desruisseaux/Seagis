/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample;

// J2SE
import java.util.Date;
import java.awt.geom.Point2D;

// Seagis
import fr.ird.database.Entry;


/**
 * Position spatio-temporelle relative � un �chantillon ({@linkp SampleEntry}}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getRelativePositions
 */
public interface RelativePositionEntry extends Entry {
    /**
     * Retourne la date � laquelle �valuer l'environnement relativement � l'�chantillon
     * sp�cifi�. On pourrait par exemple �tre int�ress�s � la hauteur de l'eau 15 jours
     * avant une p�che.
     */
    public abstract Date getTime(final SampleEntry sample);

    /**
     * Retourne la coordonn�es g�ographiques � laquelle �valuer l'environnement pour
     * l'�chantillon sp�cifi�. Il s'agit souvent (mais pas obligatoirement) de la
     * coordonn�es de l'�chantillon lui-m�me.
     */
    public abstract Point2D getCoordinate(final SampleEntry sample);

    /**
     * Indique si cette position relative devrait �tre s�lectionn�e par d�faut.
     * Cette information peut �tre utilis�e dans une interface utilisateur afin
     * de pr�-selectionner un jeu de positions courrament utilis�.
     */
    public abstract boolean isDefault();
}
