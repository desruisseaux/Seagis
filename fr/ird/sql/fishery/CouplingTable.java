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
import javax.sql.RowSet;

// SEAS dependencies
import fr.ird.sql.Table;
import fr.ird.awt.progress.Progress;


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
     * Oublie tous les paramètres qui ont été déclarés avec {@link #addParameter}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public void clear() throws SQLException;

    /**
     * Ajoute un paramètre à la sélection. Ce paramètre sera pris en compte
     * lors du prochain appel de la méthode {@link #getRowSet}.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel"). Ces opérations
     *         correspondent à des noms des colonnes de la table "Environnement".
     *         La liste des opérations disponibles peut être obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  parameter Paramètre (exemple "SST" ou "EKP"). La liste des paramètres
     *         disponibles peut être obtenu avec {@link #getAvailableParameters()}.
     * @param  position Position position relative sur la ligne de pêche où l'on veut
     *         les valeurs. Les principales valeurs permises sont {@link #START_POINT},
     *         {@link #CENTER} et {@link #END_POINT}.
     * @param  timeLag Décalage temporel entre la capture et le paramètre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public void addParameter(final String operation,
                             final String parameter,
                             final int    position,
                             final int    timeLag) throws SQLException;

    /**
     * Retire un paramètre à la sélection.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel").
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @param  position Position position relative sur la ligne de pêche où l'on veut
     *         les valeurs.
     * @param  timeLag Décalage temporel entre la capture et le paramètre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public void removeParameter(final String operation,
                                final String parameter,
                                final int    position,
                                final int    timeLag) throws SQLException;

    /**
     * Retourne les nom des colonnes pour cette table. Ces noms de colonnes sont identiques
     * à ceux que retourne <code>getRowSet(null).getMetaData().getColumnLabel(...)</code>.
     * Cette méthode permet toutefoit d'obtenir ces noms sans passer par la coûteuse création
     * d'un objet {@link RowSet}.
     *
     * @return Les noms de colonnes.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public String[] getColumnLabels() throws SQLException;

    /**
     * Retourne toutes les données. La première colonne du {@link RowSet} retourné
     * contiendra le numéro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les paramètres environnementaux. Le nombre total de colonnes est
     * égal à la longueur de la liste retournée par {@link #getColumnTitles}.
     *
     * @param  progress Objet à utiliser pour informer des progrès de l'initialisation,
     *         ou <code>null</code> si aucun.
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public RowSet getRowSet(final Progress progress) throws SQLException;

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
