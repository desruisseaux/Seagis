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
import javax.sql.RowSet;

// SEAS dependencies
import fr.ird.sql.Table;
import fr.ird.awt.progress.Progress;


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
     * Oublie tous les param�tres qui ont �t� d�clar�s avec {@link #addParameter}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public void clear() throws SQLException;

    /**
     * Ajoute un param�tre � la s�lection. Ce param�tre sera pris en compte
     * lors du prochain appel de la m�thode {@link #getRowSet}.
     *
     * @param  operation Op�ration (exemple "valeur" ou "sobel"). Ces op�rations
     *         correspondent � des noms des colonnes de la table "Environnement".
     *         La liste des op�rations disponibles peut �tre obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  parameter Param�tre (exemple "SST" ou "EKP"). La liste des param�tres
     *         disponibles peut �tre obtenu avec {@link #getAvailableParameters()}.
     * @param  position Position position relative sur la ligne de p�che o� l'on veut
     *         les valeurs. Les principales valeurs permises sont {@link #START_POINT},
     *         {@link #CENTER} et {@link #END_POINT}.
     * @param  timeLag D�calage temporel entre la capture et le param�tre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public void addParameter(final String operation,
                             final String parameter,
                             final int    position,
                             final int    timeLag) throws SQLException;

    /**
     * Retire un param�tre � la s�lection.
     *
     * @param  operation Op�ration (exemple "valeur" ou "sobel").
     * @param  parameter Param�tre (exemple "SST" ou "EKP").
     * @param  position Position position relative sur la ligne de p�che o� l'on veut
     *         les valeurs.
     * @param  timeLag D�calage temporel entre la capture et le param�tre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public void removeParameter(final String operation,
                                final String parameter,
                                final int    position,
                                final int    timeLag) throws SQLException;

    /**
     * Retourne les nom des colonnes pour cette table. Ces noms de colonnes sont identiques
     * � ceux que retourne <code>getRowSet(null).getMetaData().getColumnLabel(...)</code>.
     * Cette m�thode permet toutefoit d'obtenir ces noms sans passer par la co�teuse cr�ation
     * d'un objet {@link RowSet}.
     *
     * @return Les noms de colonnes.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public String[] getColumnLabels() throws SQLException;

    /**
     * Retourne toutes les donn�es. La premi�re colonne du {@link RowSet} retourn�
     * contiendra le num�ro identifiant les captures. Toutes les colonnes suivantes
     * contiendront les param�tres environnementaux. Le nombre total de colonnes est
     * �gal � la longueur de la liste retourn�e par {@link #getColumnTitles}.
     *
     * @param  progress Objet � utiliser pour informer des progr�s de l'initialisation,
     *         ou <code>null</code> si aucun.
     * @return Les donn�es environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     */
    public RowSet getRowSet(final Progress progress) throws SQLException;

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
