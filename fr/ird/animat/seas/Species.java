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
package fr.ird.animat.seas;

// J2SE
import java.awt.geom.RectangularShape;

// Seagis
import fr.ird.util.XEllipse2D;


/**
 * Une esp�ce de thon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Species extends fr.ird.animat.impl.Species {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -7347862222695899211L;

    /**
     * Valeur par d�faut du rayon de perception de l'animal, en miles nautiques.
     *
     * @see #getPerceptionArea
     */
    private final double perceptionRadius;

    /**
     * Distance maximale (en miles nautiques) que ce thon peut parcourir en une journ�e.
     */
    final double dailyDistance;

    /**
     * Construit une esp�ce avec le m�me nom que l'esp�ce sp�cifi�e mais qui s'int�ressera
     * � des param�tres diff�rents.
     *
     * @param parent L'esp�ce dont on veut copier les propri�t�s (noms, couleur).
     * @param La configuration de la simulation.
     */
    protected Species(final fr.ird.animat.Species parent, final Configuration configuration) {
        super(wrap(parent), configuration.parameterArray);
        perceptionRadius = configuration.perceptionRadius;
        dailyDistance    = configuration.dailyDistance;
    }

    /**
     * Retourne la r�gion de perception par d�faut des animaux de cette esp�ce. Il s'agit de la
     * r�gion dans laquelle  l'animal peut percevoir les param�tres de son environnement autour
     * de lui. Les coordonn�es de cette forme doivent �tre en <strong>m�tres</strong> et la forme
     * doit �tre centr�e sur la position de l'animal, sans rotation.
     */
    protected RectangularShape getPerceptionArea() {
        return new XEllipse2D(-perceptionRadius,
                              -perceptionRadius,
                             2*perceptionRadius,
                             2*perceptionRadius);
    }

    /**
     * V�rifie si cette esp�ce est �gale � l'objet sp�cifi�.
     */
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final Species that = (Species) object;
            return Double.doubleToLongBits(this.perceptionRadius) ==
                   Double.doubleToLongBits(that.perceptionRadius);
        }
        return false;
    }
}
