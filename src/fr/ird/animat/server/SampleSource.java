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
package fr.ird.animat.server;

// Dépendences
import java.util.Set;
import java.util.Collection;
import java.sql.SQLException;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;
import fr.ird.database.sample.SampleEntry;


/**
 * Interface des {@linkplain Environment environnement} capable de fournir des valeurs
 * d'{@linkplain SampleEntry échantillons}.  Ces échantillons peuvent être par exemple
 * des pêches. Une source d'échantillon sera typiquement connectée à une base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleSource {
    /**
     * Retourne toutes les espèces se trouvant dans la table.
     *
     * @return Les espèces se trouvant dans la table.
     * @throws SQLException si une erreur est survenue lors de l'interrogation de la base de données.
     */
    public Set<Species> getSpecies() throws SQLException;

    /**
     * Retourne l'ensemble des captures pour le pas de temps courant.
     *
     * @return Les captures pour le pas de temps courant.
     * @throws SQLException si une erreur est survenue lors de l'interrogation de la base de données.
     */
    public Collection<SampleEntry> getSamples() throws SQLException;
}
