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

// SQL database
import javax.sql.RowSet;
import java.sql.Connection;
import java.sql.SQLException;

// J2SE dependencies
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.io.Writer;
import java.io.IOException;

// SEAS dependencies
import fr.ird.sql.Table;
import org.geotools.util.ProgressListener;


/**
 * Table faisant le lien entre les captures et les paramètres environnementaux aux
 * positions de cette capture. Les paramètres environnementaux sont enregistrées dans
 * la table <code>Environnement</code>. Cette classe interroge cette table en la réarangeant
 * d'une façon plus appropriée pour l'analyse avec des logiciels statistiques classiques.
 * Les paramètres environnementaux correspondant à une même capture (SST 5 jours avant, 10
 * jours avant, etc.) sont juxtaposés sur une même ligne.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table {
    /**
     * Constante désignant la position de départ sur la ligne de pêche.
     */
    public static final int START_POINT = 0;

    /**
     * Constante désignant la position centrale sur la ligne de pêche.
     */
    public static final int CENTER = 50;

    /**
     * Constante désignant la position finale sur la ligne de pêche.
     */
    public static final int END_POINT = 100;

    /**
     * Spécifie le nom d'une table des captures à joindre avec les paramètres environnementaux
     * retournés par {@link #getRowSet}. Il ne s'agit pas nécessairement de la table
     * <code>&quot;Captures&quot;</code>. Il pourrait s'agir d'une requête, comme par exemple
     * <code>&quot;Présences par espèces&quot;<code>. Cette requête doit obligatoirement avoir une
     * colonne &quot;ID&quot; contenant le numéro identifiant la capture, suivit de préférence par
     * les colonnes &quot;date&quot;, &quot;x&quot; et &quot;y&quot; contenant les coordonnées
     * spatio-temporelles de la capture. Les colonnes suivantes contiennent les captures par
     * espèces.
     *
     * @param table Le nom de la table des captures, ou <code>null</code> si aucune.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract void setCatchTable(final String table) throws SQLException;

    /**
     * Retourne le nom d'une table des captures à joindre avec les paramètres environnementaux
     * retournés par {@link #getRowSet}, ou <code>null</code> si aucune.
     *
     * @return Le nom de la table des captures, ou <code>null</code> si aucune.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract String getCatchTable() throws SQLException;

    /**
     * Retourne la liste des paramètres environnementaux disponibles. Les paramètres
     * environnementaux sont représentés par des noms courts tels que "CHL" ou "SST".
     *
     * @return L'ensemble des paramètres environnementaux disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<String> getAvailableParameters() throws SQLException;

    /**
     * Retourne la liste des opérations disponibles. Les opérations sont appliquées sur
     * des paramètres environnementaux. Par exemple les opérations "valeur" et "sobel3"
     * correspondent à la valeur d'un paramètre environnemental et son gradient calculé
     * par l'opérateur de Sobel, respectivement.
     *
     * @return L'ensemble des opérations disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<String> getAvailableOperations() throws SQLException;

    /**
     * Ajoute un paramètre à la sélection. Le paramètre sera mesuré aux coordonnées
     * spatio-temporelles exacte de la capture.  Cette méthode est équivalente à un
     * appel de <code>{@linkplain #addParameter(String,String,int,int) addParameter}(operation,
     * parameter, {@linkplain #CENTER}, 0)</code>.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel"). Ces opérations
     *         correspondent à des noms des colonnes de la table "Environnement".
     *         La liste des opérations disponibles peut être obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  parameter Paramètre (exemple "SST" ou "EKP"). La liste des paramètres
     *         disponibles peut être obtenu avec {@link #getAvailableParameters()}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void addParameter(final String operation,
                                      final String parameter) throws SQLException;

    /**
     * Ajoute un ensemble de paramètres à la sélection. Chaque objet <code>EnvironmentTable</code>
     * nouvellement créé ne contient initialement qu'une seule colonne: le numéro ID des captures.
     * Chaque appel à <code>addParameter</code> ajoute une colonne. Chaque colonne correspondra à
     * un paramètre environnemental (<code>parameter</code>) à une certaine coordonnée
     * spatio-temporelle relative à la capture (<code>position</code>, <code>timeLag</code>), et
     * sur lequel on applique une certaine opération (<code>operation</code>). Cette colonne sera
     * prise en compte lors du prochain appel de la méthode {@link #getRowSet}.
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
    public abstract void addParameter(final String operation,
                                      final String parameter,
                                      final int    position,
                                      final int    timeLag) throws SQLException;

    /**
     * Retire un ensemble de paramètres de la sélection. Cette méthode permet de retirer une
     * colonne qui aurait été ajoutée précédement par un appel à
     *
     * <code>{@linkplain #addParameter(String,String,int,int) addParameter}(operation,
     * parameter, position, timeLag)</code>. Cette méthode ne fait rien
     * si aucune colonne ne correspond au bloc de paramètres spécifiés.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel").
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @param  position Position position relative sur la ligne de pêche où l'on veut les valeurs.
     * @param  timeLag Décalage temporel entre la capture et le paramètre environnemental,
     *         en nombre de jours.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void removeParameter(final String operation,
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
    public abstract String[] getColumnLabels() throws SQLException;

    /**
     * Retourne un itérateur qui baleyera l'ensemble des données sélectionnées. La première
     * colonne du tableau {@link RowSet} contiendra le numéro identifiant les captures (ID).
     * Toutes les colonnes suivantes contiendront les paramètres environnementaux qui auront
     * été demandé par des appels de {@link #addParameter(String,String,int,int)}. Le nombre
     * total de colonnes est égal à la longueur du tableau retournée par {@link #getColumnLabels}.
     * <br><br>
     * Note: <strong>Chaque objet <code>EnvironmentTable</code> ne mantient qu'un seul objet
     *       <code>RowSet</code> à la fois.</strong>  Si cette méthode est appelée plusieurs
     *       fois, alors chaque nouvel appel fermera le {@link RowSet} de l'appel précédent.
     *
     * @param  progress Objet à utiliser pour informer des progrès de l'initialisation, ou
     *         <code>null</code> si aucun. Cette méthode appelle {@link ProgressListener#started},
     *         mais n'appelle <strong>pas</strong> {@link ProgressListener#complete} étant donné
     *         qu'on voudra probablement continuer à l'utiliser pour informer des progrès
     *         de la lecture du {@link RowSet}.
     * @return Les données environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public abstract RowSet getRowSet(final ProgressListener progress) throws SQLException;

    /**
     * Affiche les enregistrements vers le flot spécifié.
     * Cette méthode est surtout utile à des fins de vérification.
     *
     * @param  out Flot de sortie.
     * @param  max Nombre maximal d'enregistrements à écrire.
     * @return Nombre d'enregistrement écrits.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public abstract int print(final Writer out, int max) throws SQLException, IOException;

    /**
     * Copie toutes les données de {@link #getRowSet} vers une table du nom
     * spécifiée. Aucune table ne doit exister sous ce nom avant l'appel de
     * cette méthode. Cette méthode construira elle-même la table nécessaire.
     *
     * @param  connection La connection vers la base de données dans laquelle créer la table,
     *         or <code>null</code> pour créer une table dans la base de données courante.
     * @param  tableName Nom de la table à créer.
     * @param  progress Objet à utiliser pour informer des progrès, ou <code>null</code> si aucun.
     * @return Le nombre d'enregistrement copiés dans la nouvelle table.
     * @throws Si un problème est survenu lors des accès aux bases de données.
     */
    public abstract int copyToTable(final Connection     connection,
                                    final String          tableName,
                                    final ProgressListener progress) throws SQLException;

    /**
     * Définit la valeur des paramètres environnementaux pour une capture. Cette méthode
     * affecte la valeur de chacune des colonnes qui ont été ajoutées avec {@link #addPatameter}.
     *
     * @param  capture La capture pour laquelle on veut définir les valeurs des paramètres
     *         environnementaux.
     * @param  value Les valeurs des paramètres environnementaux. Ce tableau doit avoir
     *         la même longueur que le nombre de paramètres ajoutés avec la méthode
     *         {@link #addParameter(String,String,int,int)} (c'est-à-dire la longueur de
     *         {@link #getColumnLabels} moins 1). Les valeurs <code>NaN</code> seront ignorées
     *         (c'est-à-dire que les valeurs déjà présentes dans la base de données ne seront
     *         pas écrasées).
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public abstract void set(final CatchEntry capture, final float[] values) throws SQLException;

    /**
     * Définit la valeur des paramètres environnementaux pour une capture. Cette méthode fonctionne
     * comme {@link #set(CatchEntry, float[]),  excepté qu'elle permet de spécifier des coordonnées
     * spatio-temporelles différentes de celles qui avaient été spécifiées avec {@link #addPatameter}.
     *
     * @param  capture La capture pour laquelle on veut définir les valeurs des paramètres
     *         environnementaux.
     * @param  position Position relative de la valeur <code>value</code>. Si la capture est
     *         représentée par un seul point (c'est-à-dire si {@link CatchEntry#getShape}
     *         retourne <code>null</code>), alors cette méthode met à jour la l'enregistrement
     *         correspondant à la position {@link #CENTER}, quelle que soit la valeur de cet
     *         argument <code>position</code>.
     * @param  time La date à laquelle a été évaluée la valeur <code>value</code>.
     *         Si cet argument est non-nul, alors l'écart de temps entre cette date
     *         et la date de la capture sera calculée et utilisée.
     * @param  value Les valeurs des paramètres environnementaux. Ce tableau doit avoir
     *         la même longueur que le nombre de paramètres ajoutés avec la méthode {@link
     *         #addParameter(String,String)} (c'est-à-dire la longueur de {@link #getColumnLabels}
     *         moins 1). Les valeurs <code>NaN</code> seront ignorées (c'est-à-dire que les
     *         valeurs déjà présentes dans la base de données ne seront pas écrasées).
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public abstract void set(final CatchEntry capture,
                             final int        relativePosition,
                             final Date       valueTime,
                             final float[]    values) throws SQLException;

    /**
     * Oublie tous les paramètres qui ont été déclarés avec
     * {@link #addParameter(String,String,int,int)}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void clear() throws SQLException;
}
