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
package fr.ird.database.sample;

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
import fr.ird.database.Table;
import org.geotools.util.ProgressListener;


/**
 * Table faisant le lien entre les échantillons et les paramètres environnementaux aux
 * positions de cet échantillons. Les paramètres environnementaux sont enregistrées dans
 * la table <code>&quot;Environments&quot;</code>. Cette classe interroge cette table en
 * la réarangeant d'une façon plus appropriée pour l'analyse avec des logiciels statistiques
 * classiques. Les paramètres environnementaux correspondant à un même échantillons (SST 5
 * jours avant, 10 jours avant, etc.) sont juxtaposés sur une même ligne.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table {
    /**
     * Spécifie le nom d'une table des échantillons à joindre avec les paramètres environnementaux
     * retournés par {@link #getRowSet}. Il ne s'agit pas nécessairement de la table
     * <code>&quot;Samples&quot;</code>. Il pourrait s'agir d'une requête, comme par exemple
     * <code>&quot;Présences par espèces&quot;<code>. Cette requête doit obligatoirement avoir une
     * colonne &quot;ID&quot; contenant le numéro identifiant l'échantillons, suivit de préférence
     * par les colonnes &quot;date&quot;, &quot;x&quot; et &quot;y&quot; contenant les coordonnées
     * spatio-temporelles de l'échantillons. Les colonnes suivantes contiennent les échantillons par
     * espèces.
     *
     * @param table Le nom de la table des échantillons, ou <code>null</code> si aucune.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract void setSampleTable(final String table) throws SQLException;

    /**
     * Retourne le nom d'une table des échantillons à joindre avec les paramètres environnementaux
     * retournés par {@link #getRowSet}, ou <code>null</code> si aucune.
     *
     * @return Le nom de la table des échantillons, ou <code>null</code> si aucune.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract String getSampleTable() throws SQLException;

    /**
     * Retourne la liste des paramètres environnementaux disponibles. Les paramètres
     * environnementaux sont représentés par des noms courts tels que "CHL" ou "SST".
     *
     * @return L'ensemble des paramètres environnementaux disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<+ParameterEntry> getAvailableParameters() throws SQLException;

    /**
     * Retourne la liste des opérations disponibles. Les opérations sont appliquées sur
     * des paramètres environnementaux. Par exemple les opérations "valeur" et "sobel3"
     * correspondent à la valeur d'un paramètre environnemental et son gradient calculé
     * par l'opérateur de Sobel, respectivement.
     *
     * @return L'ensemble des opérations disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<OperationEntry> getAvailableOperations() throws SQLException;

    /**
     * Retourne la liste des positions relatives disponibles. Ces positions comprennent
     * un décalage spatio-temporel à appliquer sur les coordonnées des échantillons avant
     * d'obtenir les valeurs environnementales.
     *
     * @return L'ensemble des positions relatives disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public abstract Set<RelativePositionEntry> getAvailablePositions() throws SQLException;

    /**
     * Ajoute un paramètre à la sélection. Chaque objet <code>EnvironmentTable</code> nouvellement
     * créé ne contient initialement qu'une seule colonne: le numéro ID des échantillons. Chaque appel
     * à <code>addParameter</code> ajoute une colonne. Chaque colonne correspondra à un paramètre
     * environnemental (<code>parameter</code>) à une certaine coordonnée spatio-temporelle relative
     * à l'échantillons (<code>position</code>), et sur lequel on applique une certaine opération
     * (<code>operation</code>). Cette colonne sera prise en compte lors du prochain appel de
     * la méthode {@link #getRowSet}.
     *
     * @param  parameter Paramètre (exemple "SST" ou "EKP"). La liste des paramètres
     *         disponibles peut être obtenu avec {@link #getAvailableParameters()}.
     * @param  operation Opération (exemple "valeur" ou "sobel"). Ces opérations
     *         correspondent à des noms des colonnes de la table "Environnement".
     *         La liste des opérations disponibles peut être obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  position Position spatio-temporelle relative où l'on veut les valeurs.
     * @param  nullIncluded Indique si {@link #getRowSet} est autorisée à retourner des valeurs
     *         nulles. La valeur par défaut est <code>false</code>, ce qui indique que tous les
     *         enregistrements pour lesquels au moins un paramètre environnemental est manquant
     *         seront omis.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void addParameter(final ParameterEntry        parameter,
                                      final OperationEntry        operation,
                                      final RelativePositionEntry position,
                                      final boolean nullIncluded) throws SQLException;

    /**
     * Ajoute un paramètre à la sélection en ne le désignant que par son nom.
     *
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @param  operation Opération (exemple "valeur" ou "sobel").
     * @param  position Position spatio-temporelle relative où l'on veut les valeurs.
     * @param  nullIncluded Indique si {@link #getRowSet} est autorisée à retourner des valeurs
     *         nulles.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void addParameter(final String  parameter,
                                      final String  operation,
                                      final String  position,
                                      final boolean nullIncluded) throws SQLException;

    /**
     * Retire un paramètres de la sélection. Cette méthode permet de retirer une
     * colonne qui aurait été ajoutée précédement par un appel à
     * <code>{@linkplain #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry)
     * addParameter}(parameter, operation, position, ...)</code>.
     * Cette méthode ne fait rien si aucune colonne ne correspond au bloc de paramètres spécifié.
     *
     * @param  operation Opération (exemple "valeur" ou "sobel").
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @param  position Position spatio-temporelle relative où l'on voulait les valeurs.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void removeParameter(final ParameterEntry       parameter,
                                         final OperationEntry       operation,
                                         final RelativePositionEntry position) throws SQLException;

    /**
     * Retire un paramètre à la sélection en ne le désignant que par son nom.
     *
     * @param  parameter Paramètre (exemple "SST" ou "EKP").
     * @param  operation Opération (exemple "valeur" ou "sobel").
     * @param  position Position spatio-temporelle relative où l'on voulait les valeurs.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void removeParameter(final String parameter,
                                         final String operation,
                                         final String position) throws SQLException;

    /**
     * Retourne le nombre de paramètres dans cette table correspondant aux critères spécifiés.
     * Si un ou plusieurs des arguments <code>parameter</code>, <code>operation</code> ou
     * <code>position</code> est non-nul, alors cette méthode filtre les paramètres en ne
     * comptant que ceux qui correspondent aux arguments non-nuls. Par exemple
     * <code>getParameterCount(null,null,position)</code> comptera tous les paramètres dont la
     * position spatio-temporelle relative est égale à <code>position</code>.
     *
     * @param  parameter Paramètre à compter, ou <code>null</code> pour les compter tous.
     * @param  operation Si non-nul, alors seul les paramètres sur lesquels on applique cette
     *         opération seront pris en compte.
     * @param  position Si non-nul, alors seul les paramètres à cette position relative seront
     *         pris en compte.
     */
    public abstract int getParameterCount(final ParameterEntry       parameter,
                                          final OperationEntry       operation,
                                          final RelativePositionEntry position);

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
     * été demandé par des appels de
     * {@link #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry) addParameter(...)}.
     * Le nombre total de colonnes est égal à la longueur du tableau retournée par
     * {@link #getColumnLabels}.
     * <br><br>
     * Note: <strong>Chaque objet <code>EnvironmentTable</code> ne maintient qu'un seul objet
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
     * Définit la valeur des paramètres environnementaux pour un échantillons. Le nombre
     * de valeurs (<code>values.length</code>) doit correspondre au nombre de paramètres
     * <code>{@link #getParameterCount getParameterCount}(null, null, <var>position</var>)</code>.
     *
     * Si l'argument <code>position</code> est nul, alors on considèrera que
     * les valeurs ont été évaluées à toutes les positions déclarées avec
     * {@link #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry) addParameter(...)}.
     * Si <code>position</code> est non-nul, alors on considèrera que les valeurs n'ont été évaluée
     * qu'à cette position. Cette position doit tout de m$eme correspondre à l'une de celle qui ont
     * été déclarées précédemment.
     *
     * @param  sample L'échantillons pour laquelle on veut définir les valeurs des paramètres
     *         environnementaux.
     * @param  position Position relative des valeurs <code>values</code>, ou <code>null</code>
     *         si les valeurs ont été évaluées à toutes les positions relatives.
     * @param  value Les valeurs des paramètres environnementaux.
     *         Les valeurs <code>NaN</code> seront ignorées (c'est-à-dire que les
     *         valeurs déjà présentes dans la base de données ne seront pas écrasées).
     * @throws SQLException si un problème est survenu lors de la mise à jour.
     */
    public abstract void set(final SampleEntry           sample,
                             final RelativePositionEntry position,
                             final double[]              values) throws SQLException;

    /**
     * Oublie tous les paramètres qui ont été déclarés avec
     * {@link #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry) addParameter(...)}.
     *
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    public abstract void clear() throws SQLException;
}
