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
 */
package fr.ird.animat.server.tuna;

// J2SE
import java.rmi.RemoteException;
import java.awt.geom.RectangularShape;

// Seagis
import fr.ird.resources.XEllipse2D;


/**
 * Une esp�ce de thon.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Species extends fr.ird.animat.server.Species {
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
     * @param  parent L'esp�ce dont on veut copier les propri�t�s (noms, couleur).
     * @param  La configuration de la simulation.
     * @throws RemoteException si des m�thodes devaient �tre appel�e sur une machine distance
     *         et que ces appels ont �chou�s.
     */
    protected Species(final fr.ird.animat.Species parent,
                      final Configuration configuration)
            throws RemoteException
    {
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
        if (object instanceof Species) {
            if (super.equals(object)) {
                final Species that = (Species) object;
                return Double.doubleToLongBits(this.perceptionRadius) ==
                       Double.doubleToLongBits(that.perceptionRadius);
            }
            return false;
        }
        return super.equals(object); // Compare RMI stubs
    }
}
