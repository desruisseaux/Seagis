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
package fr.ird.animat.viewer;

// J2SE dependencies
import java.awt.Color;
import java.util.Date;
import java.util.TimeZone;
import java.util.Iterator;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Geotools & Seagis dependencies
import org.geotools.resources.Utilities;
import fr.ird.animat.seas.Fisheries;
import fr.ird.animat.Species;


/**
 * Layer showing fishery positions.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CatchLayer extends fr.ird.seasview.layer.CatchLayer {
    /**
     * Couleur par d�faut des positions de p�ches.
     */
    private static final Color COLOR = Color.ORANGE;

    /**
     * Connexion vers les donn�es de p�ches.
     */
    private final Fisheries fisheries;

    /**
     * Nombre de millisecondes � ajouter � l'heure UTC pour obtenir l'heure locale.
     */
    private int timezoneOffset;

    /**
     * Le jour des donn�es affich�es, en nombre de jours depuis le 1 janvier 1970 heure locale.
     */
    private int day = Integer.MIN_VALUE;

    /**
     * Construit une nouvelle couche.
     */
    public CatchLayer(final Fisheries fisheries) throws RemoteException {
        this.fisheries = fisheries;
        try {
            for (final Iterator<Species> it=fisheries.getSpecies().iterator(); it.hasNext();) {
                setColor(it.next(), COLOR);
            }
            setColor(null, COLOR);
        } catch (SQLException exception) {
            // Ne peut pas changer la couleur. Tant pis, �a n'emp�chera pas le reste de fonctionner.
            Utilities.unexpectedException("fr.ird.animat.viewer", "CatchLayer", "<init>", exception);
        }
    }

    /**
     * D�finit le fuseau horaire de la simulation. Cette information est utilis�e pour
     * d�terminer quand le jour a chang�.
     */
    public void setTimeZone(final TimeZone timezone) {
        timezoneOffset = timezone.getRawOffset();
    }

    /**
     * Remet � jour la liste des captures. Cette m�thode n'effectuera la mise � jour que si la
     * date sp�cifi�e est � une journ�e diff�rente de celle des donn�es d�j� en m�moire, afin
     * d'�viter d'interroger base de donn�es trop souvent.
     */
    public void refresh(final Date time) {
        if (time == null) {
            setCatchs(null);
            day = Integer.MIN_VALUE;
            return;
        }
        final int newDay = (int)((time.getTime() + timezoneOffset) / (24*60*60*1000));
        if (newDay != day) try {
            setCatchs(fisheries.getCatchs());
            day = newDay;
        } catch (SQLException exception) {
            Utilities.unexpectedException("fr.ird.animat.viewer", "CatchLayer", "refresh", exception);
        }
    }
}
