/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
     * Couleur par défaut des positions de pêches.
     */
    private static final Color COLOR = Color.ORANGE;

    /**
     * Connexion vers les données de pêches.
     */
    private final Fisheries fisheries;

    /**
     * Nombre de millisecondes à ajouter à l'heure UTC pour obtenir l'heure locale.
     */
    private int timezoneOffset;

    /**
     * Le jour des données affichées, en nombre de jours depuis le 1 janvier 1970 heure locale.
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
            // Ne peut pas changer la couleur. Tant pis, ça n'empêchera pas le reste de fonctionner.
            Utilities.unexpectedException("fr.ird.animat.viewer", "CatchLayer", "<init>", exception);
        }
    }

    /**
     * Définit le fuseau horaire de la simulation. Cette information est utilisée pour
     * déterminer quand le jour a changé.
     */
    public void setTimeZone(final TimeZone timezone) {
        timezoneOffset = timezone.getRawOffset();
    }

    /**
     * Remet à jour la liste des captures. Cette méthode n'effectuera la mise à jour que si la
     * date spécifiée est à une journée différente de celle des données déjà en mémoire, afin
     * d'éviter d'interroger base de données trop souvent.
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
