/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
 * Table faisant le lien entre les captures et les param�tres environnementaux aux positions
 * de cette capture. Cette interrogation pourrait �tre faites dans un logiciel de base de
 * donn�es avec une requ�te SQL classique. Mais cette requ�te est assez longue et tr�s
 * laborieuse � construire � la main. De plus, elle d�passe souvent les capacit�s de Access.
 * Cette interface d�coupera cette requ�te monstre en une s�rie de requ�tes plus petites.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CouplingTable extends Table {
    /**
     * D�finit les param�tres examin�es par cette table. Les param�tres doivent �tre des
     * noms de la table "Param�tres". Des exemples de valeurs sont "SST", "CHL", "SLA",
     * "U", "V" et "EKP".
     *
     * @param parameter Les param�tres � d�finir (exemple: "SST").
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public void setParameters(final String[] parameters) throws SQLException;

    /**
     * D�finit les d�calages de temps (en jours).
     *
     * @parm   timeLags D�calages de temps en jours.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public void setTimeLags(final int[] timeLags) throws SQLException;

    /**
     * Retourne les colonnes de titres pour cette table.
     */
    public String[] getColumnTitles() throws SQLException;

    /**
     * Retourne toutes les donn�es. La premi�re colonne du {@link ResultSet} retourn�
     * contiendra le num�ro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les param�tres environnementaux. Le nombre total de colonnes est
     * �gal � la longueur de la liste retourn�e par {@link #getColumnTitles}.
     *
     * @return Les donn�es environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     */
    public ResultSet getData() throws SQLException;

    /**
     * Affiche les enregistrements vers le flot sp�cifi�.
     * Cette m�thode est surtout utile � des fins de v�rification.
     *
     * @param out Flot de sortie.
     * @param max Nombre maximal d'enregistrements � �crire.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     * @throws IOException si une erreur est survenue lors de l'�criture.
     */
    public void print(final Writer out, int max) throws SQLException, IOException;
}
