/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 * Table faisant le lien entre les �chantillons et les param�tres environnementaux aux
 * positions de cet �chantillons. Les param�tres environnementaux sont enregistr�es dans
 * la table <code>&quot;Environments&quot;</code>. Cette classe interroge cette table en
 * la r�arangeant d'une fa�on plus appropri�e pour l'analyse avec des logiciels statistiques
 * classiques. Les param�tres environnementaux correspondant � un m�me �chantillons (SST 5
 * jours avant, 10 jours avant, etc.) sont juxtapos�s sur une m�me ligne.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface EnvironmentTable extends Table {
    /**
     * Sp�cifie le nom d'une table des �chantillons � joindre avec les param�tres environnementaux
     * retourn�s par {@link #getRowSet}. Il ne s'agit pas n�cessairement de la table
     * <code>&quot;Samples&quot;</code>. Il pourrait s'agir d'une requ�te, comme par exemple
     * <code>&quot;Pr�sences par esp�ces&quot;<code>. Cette requ�te doit obligatoirement avoir une
     * colonne &quot;ID&quot; contenant le num�ro identifiant l'�chantillons, suivit de pr�f�rence
     * par les colonnes &quot;date&quot;, &quot;x&quot; et &quot;y&quot; contenant les coordonn�es
     * spatio-temporelles de l'�chantillons. Les colonnes suivantes contiennent les �chantillons par
     * esp�ces.
     *
     * @param table Le nom de la table des �chantillons, ou <code>null</code> si aucune.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract void setSampleTable(final String table) throws SQLException;

    /**
     * Retourne le nom d'une table des �chantillons � joindre avec les param�tres environnementaux
     * retourn�s par {@link #getRowSet}, ou <code>null</code> si aucune.
     *
     * @return Le nom de la table des �chantillons, ou <code>null</code> si aucune.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract String getSampleTable() throws SQLException;

    /**
     * Retourne la liste des param�tres environnementaux disponibles. Les param�tres
     * environnementaux sont repr�sent�s par des noms courts tels que "CHL" ou "SST".
     *
     * @return L'ensemble des param�tres environnementaux disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract Set<+ParameterEntry> getAvailableParameters() throws SQLException;

    /**
     * Retourne la liste des op�rations disponibles. Les op�rations sont appliqu�es sur
     * des param�tres environnementaux. Par exemple les op�rations "valeur" et "sobel3"
     * correspondent � la valeur d'un param�tre environnemental et son gradient calcul�
     * par l'op�rateur de Sobel, respectivement.
     *
     * @return L'ensemble des op�rations disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract Set<OperationEntry> getAvailableOperations() throws SQLException;

    /**
     * Retourne la liste des positions relatives disponibles. Ces positions comprennent
     * un d�calage spatio-temporel � appliquer sur les coordonn�es des �chantillons avant
     * d'obtenir les valeurs environnementales.
     *
     * @return L'ensemble des positions relatives disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public abstract Set<RelativePositionEntry> getAvailablePositions() throws SQLException;

    /**
     * Ajoute un param�tre � la s�lection. Chaque objet <code>EnvironmentTable</code> nouvellement
     * cr�� ne contient initialement qu'une seule colonne: le num�ro ID des �chantillons. Chaque appel
     * � <code>addParameter</code> ajoute une colonne. Chaque colonne correspondra � un param�tre
     * environnemental (<code>parameter</code>) � une certaine coordonn�e spatio-temporelle relative
     * � l'�chantillons (<code>position</code>), et sur lequel on applique une certaine op�ration
     * (<code>operation</code>). Cette colonne sera prise en compte lors du prochain appel de
     * la m�thode {@link #getRowSet}.
     *
     * @param  parameter Param�tre (exemple "SST" ou "EKP"). La liste des param�tres
     *         disponibles peut �tre obtenu avec {@link #getAvailableParameters()}.
     * @param  operation Op�ration (exemple "valeur" ou "sobel"). Ces op�rations
     *         correspondent � des noms des colonnes de la table "Environnement".
     *         La liste des op�rations disponibles peut �tre obtenu avec {@link
     *         #getAvailableOperations()}.
     * @param  position Position spatio-temporelle relative o� l'on veut les valeurs.
     * @param  nullIncluded Indique si {@link #getRowSet} est autoris�e � retourner des valeurs
     *         nulles. La valeur par d�faut est <code>false</code>, ce qui indique que tous les
     *         enregistrements pour lesquels au moins un param�tre environnemental est manquant
     *         seront omis.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public abstract void addParameter(final ParameterEntry        parameter,
                                      final OperationEntry        operation,
                                      final RelativePositionEntry position,
                                      final boolean nullIncluded) throws SQLException;

    /**
     * Ajoute un param�tre � la s�lection en ne le d�signant que par son nom.
     *
     * @param  parameter Param�tre (exemple "SST" ou "EKP").
     * @param  operation Op�ration (exemple "valeur" ou "sobel").
     * @param  position Position spatio-temporelle relative o� l'on veut les valeurs.
     * @param  nullIncluded Indique si {@link #getRowSet} est autoris�e � retourner des valeurs
     *         nulles.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public abstract void addParameter(final String  parameter,
                                      final String  operation,
                                      final String  position,
                                      final boolean nullIncluded) throws SQLException;

    /**
     * Retire un param�tres de la s�lection. Cette m�thode permet de retirer une
     * colonne qui aurait �t� ajout�e pr�c�dement par un appel �
     * <code>{@linkplain #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry)
     * addParameter}(parameter, operation, position, ...)</code>.
     * Cette m�thode ne fait rien si aucune colonne ne correspond au bloc de param�tres sp�cifi�.
     *
     * @param  operation Op�ration (exemple "valeur" ou "sobel").
     * @param  parameter Param�tre (exemple "SST" ou "EKP").
     * @param  position Position spatio-temporelle relative o� l'on voulait les valeurs.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public abstract void removeParameter(final ParameterEntry       parameter,
                                         final OperationEntry       operation,
                                         final RelativePositionEntry position) throws SQLException;

    /**
     * Retire un param�tre � la s�lection en ne le d�signant que par son nom.
     *
     * @param  parameter Param�tre (exemple "SST" ou "EKP").
     * @param  operation Op�ration (exemple "valeur" ou "sobel").
     * @param  position Position spatio-temporelle relative o� l'on voulait les valeurs.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public abstract void removeParameter(final String parameter,
                                         final String operation,
                                         final String position) throws SQLException;

    /**
     * Retourne le nombre de param�tres dans cette table correspondant aux crit�res sp�cifi�s.
     * Si un ou plusieurs des arguments <code>parameter</code>, <code>operation</code> ou
     * <code>position</code> est non-nul, alors cette m�thode filtre les param�tres en ne
     * comptant que ceux qui correspondent aux arguments non-nuls. Par exemple
     * <code>getParameterCount(null,null,position)</code> comptera tous les param�tres dont la
     * position spatio-temporelle relative est �gale � <code>position</code>.
     *
     * @param  parameter Param�tre � compter, ou <code>null</code> pour les compter tous.
     * @param  operation Si non-nul, alors seul les param�tres sur lesquels on applique cette
     *         op�ration seront pris en compte.
     * @param  position Si non-nul, alors seul les param�tres � cette position relative seront
     *         pris en compte.
     */
    public abstract int getParameterCount(final ParameterEntry       parameter,
                                          final OperationEntry       operation,
                                          final RelativePositionEntry position);

    /**
     * Retourne les nom des colonnes pour cette table. Ces noms de colonnes sont identiques
     * � ceux que retourne <code>getRowSet(null).getMetaData().getColumnLabel(...)</code>.
     * Cette m�thode permet toutefoit d'obtenir ces noms sans passer par la co�teuse cr�ation
     * d'un objet {@link RowSet}.
     *
     * @return Les noms de colonnes.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public abstract String[] getColumnLabels() throws SQLException;

    /**
     * Retourne un it�rateur qui baleyera l'ensemble des donn�es s�lectionn�es. La premi�re
     * colonne du tableau {@link RowSet} contiendra le num�ro identifiant les captures (ID).
     * Toutes les colonnes suivantes contiendront les param�tres environnementaux qui auront
     * �t� demand� par des appels de
     * {@link #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry) addParameter(...)}.
     * Le nombre total de colonnes est �gal � la longueur du tableau retourn�e par
     * {@link #getColumnLabels}.
     * <br><br>
     * Note: <strong>Chaque objet <code>EnvironmentTable</code> ne maintient qu'un seul objet
     *       <code>RowSet</code> � la fois.</strong>  Si cette m�thode est appel�e plusieurs
     *       fois, alors chaque nouvel appel fermera le {@link RowSet} de l'appel pr�c�dent.
     *
     * @param  progress Objet � utiliser pour informer des progr�s de l'initialisation, ou
     *         <code>null</code> si aucun. Cette m�thode appelle {@link ProgressListener#started},
     *         mais n'appelle <strong>pas</strong> {@link ProgressListener#complete} �tant donn�
     *         qu'on voudra probablement continuer � l'utiliser pour informer des progr�s
     *         de la lecture du {@link RowSet}.
     * @return Les donn�es environnementales pour les captures.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     */
    public abstract RowSet getRowSet(final ProgressListener progress) throws SQLException;

    /**
     * Affiche les enregistrements vers le flot sp�cifi�.
     * Cette m�thode est surtout utile � des fins de v�rification.
     *
     * @param  out Flot de sortie.
     * @param  max Nombre maximal d'enregistrements � �crire.
     * @return Nombre d'enregistrement �crits.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     * @throws IOException si une erreur est survenue lors de l'�criture.
     */
    public abstract int print(final Writer out, int max) throws SQLException, IOException;

    /**
     * Copie toutes les donn�es de {@link #getRowSet} vers une table du nom
     * sp�cifi�e. Aucune table ne doit exister sous ce nom avant l'appel de
     * cette m�thode. Cette m�thode construira elle-m�me la table n�cessaire.
     *
     * @param  connection La connection vers la base de donn�es dans laquelle cr�er la table,
     *         or <code>null</code> pour cr�er une table dans la base de donn�es courante.
     * @param  tableName Nom de la table � cr�er.
     * @param  progress Objet � utiliser pour informer des progr�s, ou <code>null</code> si aucun.
     * @return Le nombre d'enregistrement copi�s dans la nouvelle table.
     * @throws Si un probl�me est survenu lors des acc�s aux bases de donn�es.
     */
    public abstract int copyToTable(final Connection     connection,
                                    final String          tableName,
                                    final ProgressListener progress) throws SQLException;

    /**
     * D�finit la valeur des param�tres environnementaux pour un �chantillons. Le nombre
     * de valeurs (<code>values.length</code>) doit correspondre au nombre de param�tres
     * <code>{@link #getParameterCount getParameterCount}(null, null, <var>position</var>)</code>.
     *
     * Si l'argument <code>position</code> est nul, alors on consid�rera que
     * les valeurs ont �t� �valu�es � toutes les positions d�clar�es avec
     * {@link #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry) addParameter(...)}.
     * Si <code>position</code> est non-nul, alors on consid�rera que les valeurs n'ont �t� �valu�e
     * qu'� cette position. Cette position doit tout de m$eme correspondre � l'une de celle qui ont
     * �t� d�clar�es pr�c�demment.
     *
     * @param  sample L'�chantillons pour laquelle on veut d�finir les valeurs des param�tres
     *         environnementaux.
     * @param  position Position relative des valeurs <code>values</code>, ou <code>null</code>
     *         si les valeurs ont �t� �valu�es � toutes les positions relatives.
     * @param  value Les valeurs des param�tres environnementaux.
     *         Les valeurs <code>NaN</code> seront ignor�es (c'est-�-dire que les
     *         valeurs d�j� pr�sentes dans la base de donn�es ne seront pas �cras�es).
     * @throws SQLException si un probl�me est survenu lors de la mise � jour.
     */
    public abstract void set(final SampleEntry           sample,
                             final RelativePositionEntry position,
                             final double[]              values) throws SQLException;

    /**
     * Oublie tous les param�tres qui ont �t� d�clar�s avec
     * {@link #addParameter(ParameterEntry,OperationEntry,RelativePositionEntry) addParameter(...)}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    public abstract void clear() throws SQLException;
}
