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
 */
package fr.ird.database.coverage;

// J2SE dependencies
import java.rmi.RemoteException;

// Geotools
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.Entry;


/**
 * Information sur un format d'image.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface FormatEntry extends Entry {
    /**
     * Retourne les listes des bandes {@link SampleDimension} qui permettent
     * de décoder les valeurs des paramètres géophysiques. Cette méthode peut
     * retourner plusieurs objets {@link SampleDimension}, un par bande. Leur
     * type (géophysique ou non) correspond au type des images dans leur format
     * natif. Par exemple les valeurs des pixels seront des entiers
     * (<code>{@link SampleDimension#geophysics geophysics}(false)</code>)
     * si l'image est enregistrée au format PNG, tandis que les plages de valeurs
     * peuvent être des nombres réels
     * (<code>{@link SampleDimension#geophysics geophysics}(true)</code>)
     * si l'image est enregistrée dans un format brut ou ASCII.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract SampleDimension[] getSampleDimensions() throws RemoteException;
}
