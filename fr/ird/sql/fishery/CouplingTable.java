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
package fr.ird.sql.fishery;

// J2SE dependencies
import java.util.List;
import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;

// SEAS dependencies
import fr.ird.sql.Table;


/**
 * Table faisant le lien entre les captures et les paramètres environnementaux aux positions
 * de cette capture. Cette interrogation pourrait être faites dans un logiciel de base de
 * données avec une requête SQL classique. Mais cette requête est assez longue et très
 * laborieuse à construire à la main. De plus, elle dépasse souvent les capacités de Access.
 * Cette interface découpera cette requête monstre en une série de requêtes plus petites.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CouplingTable extends Table {
    /**
     * Définit les paramètres examinées par cette table. Les paramètres doivent être des
     * noms de la table "Paramètres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Les paramètres à définir (exemple: "SST").
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public void setParameters(final String[] parameters) throws SQLException;

    /**
     * Définit les décalages de temps (en jours).
     *
     * @parm   timeLags Décalages de temps en jours.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public void setTimeLags(final int[] timeLags) throws SQLException;

    /**
     * Retourne les colonnes de titres pour cette table.
     */
    public String[] getColumnTitles() throws SQLException;

    /**
     * Retourne toutes les données. La première colonne du {@link ResultSet} retourné
     * contiendra le numéro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les paramètres environnementaux. Le nombre total de colonnes est
     * égal à la longueur de la liste retournée par {@link #getColumnTitles}.
     *
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public ResultSet getData() throws SQLException;

    /**
     * Affiche les enregistrements vers le flot spécifié.
     * Cette méthode est surtout utile à des fins de vérification.
     *
     * @param out Flot de sortie.
     * @param max Nombre maximal d'enregistrements à écrire.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public void print(final Writer out, int max) throws SQLException, IOException;
}
