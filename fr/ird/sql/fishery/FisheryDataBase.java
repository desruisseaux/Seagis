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

// Base de données
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import fr.ird.sql.SQLEditor;
import fr.ird.sql.DataBase;

// Divers
import java.util.TimeZone;
import java.io.IOException;

// Collections
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

// Geotools
import org.geotools.resources.Arguments;

// Divers
import fr.ird.animat.Species;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import fr.ird.awt.progress.PrintProgress;


/**
 * Connection avec la base de données de pêches.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class FisheryDataBase extends DataBase {
    /**
     * Retourne une des préférences du système.  Cette méthode définit les
     * paramètres par défaut qui seront utilisés lorsque l'utilisateur n'a
     * pas défini de préférence pour un paramètre.
     */
    private static String getPreference(final String name) {
        String def=null;
        if (name != null)
        {
                 if (name.equalsIgnoreCase(DRIVER))   def = "sun.jdbc.odbc.JdbcOdbcDriver";
            else if (name.equalsIgnoreCase(SOURCE))   def = "jdbc:odbc:SEAS-Sennes";
            else if (name.equalsIgnoreCase(TIMEZONE)) def = "Indian/Reunion";
        }
        return Table.preferences.get(name, def);
    }

    /**
     * Liste des propriétées par défaut. Les valeurs aux index pairs sont les index
     * des propriétées. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propriété "Pêches" donne l'instruction SQL à utiliser pour interroger la
     * table des pêches.
     * <br><br>
     * Les clés se terminant par {@link Table#CATCHS} sont traitées d'une manière
     * spéciale par {@link #getCatchTableType}.  Cette méthode testera la requête
     * correspondantes pour détecter le type de la table des captures.  Le numéro
     * (à partir de 1) de la première requête à réussir sera retourné.
     */
    private static final String[] DEFAULT_PROPERTIES = {
        Table.SPECIES,                SpeciesTable        .SQL_SELECT,
        "Palangres."+Table.CATCHS,    LonglineCatchTable  .SQL_SELECT, // CatchTableType #1
        "Seines."   +Table.CATCHS,    SeineCatchTable     .SQL_SELECT, // CatchTableType #2
        Table.CATCHS+":UPDATE",       AbstractCatchTable  .SQL_UPDATE,
        Table.ENVIRONMENTS,           EnvironmentTableStep.SQL_SELECT,
        Table.ENVIRONMENTS+":UPDATE", EnvironmentTableImpl.SQL_UPDATE,
        Table.ENVIRONMENTS+":INSERT", EnvironmentTableImpl.SQL_INSERT,
        Table.PARAMETERS,             ParameterTable      .SQL_SELECT,
        Table.PARAMETERS+":LIST",     ParameterTable      .SQL_LIST,
        Table.OPERATIONS,             ParameterTable      .SQL_SELECT_OPERATION
    };

    /**
     * Liste des noms descriptifs à donner aux propriétés.
     * Ces noms sont identifiés par des clés de ressources.
     * Ces clés doivent apparaîtrent dans le même ordre que
     * les éléments du tableau {@link #DEFAULT_PROPERTIES}.
     */
    private static final int[] PROPERTY_NAMES = {
        ResourceKeys.SQL_SPECIES,
        ResourceKeys.SQL_LONGLINES,
        ResourceKeys.SQL_SEINES,
        ResourceKeys.SQL_CATCHS_UPDATE,
        ResourceKeys.SQL_ENVIRONMENTS,
        ResourceKeys.SQL_ENVIRONMENTS_UPDATE,
        ResourceKeys.SQL_ENVIRONMENTS_INSERT,
        ResourceKeys.SQL_PARAMETER,
        ResourceKeys.SQL_PARAMETER_LIST,
        ResourceKeys.SQL_OPERATION
    };

    /**
     * Retourne l'URL par défaut de la base de données de pêches.
     * Cet URL sera puisé dans les préférences de l'utilisateur
     * autant que possible.
     */
    private static String getDefaultURL() {
        Table.logger.log(loadDriver(getPreference(DRIVER)));
        return getPreference(SOURCE);
    }

    /**
     * Type de la table des captures, ou 0 si ce type n'a pas encore été
     * déterminé. Ce champ est mis à jour par {@link #getCatchTableType}
     * la première fois qu'il est demandé.
     */
    private int catchTableType;

    /**
     * Ouvre une connection avec une base de données par défaut.
     * Ce constructeur est surtout utilisé à des fins de test.
     *
     * @throws SQLException Si on n'a pas pu se connecter
     *         à la base de données.
     */
    public FisheryDataBase() throws SQLException {
        super(getDefaultURL(), TimeZone.getTimeZone(getPreference(TIMEZONE)));
    }

    /**
     * Ouvre une connection avec la base de données de pêches.
     *
     * @param  name Protocole et nom de la base de données des pêches.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @throws SQLException Si on n'a pas pu se connecter à la base de données.
     */
    public FisheryDataBase(final String name, final TimeZone timezone) throws SQLException {
        super(name, timezone);
    }

    /**
     * Coupe la requête spécifiée juste après la clause "FROM".
     *
     * @param  query Requête à couper après la clause "FROM".
     * @param  keepSelect <code>true</code> s'il faut conserver le début de la requête
     *         (habituellement une clause "SELECT"), ou <code>false</code> s'il faut
     *         retourner seulement la clause "FROM".
     * @return La requête coupée.
     */
    private static String cutAfterFrom(String query, final boolean keepSelect) {
        final String FROM = "FROM";
        final int lower = Table.indexOfWord(query, FROM);
        if (lower >= 0) {
            int upper = lower + FROM.length();
            final int length = query.length();
            boolean skipWS = true;
            do {
                while (upper<length && Character.isWhitespace(query.charAt(upper))==skipWS) {
                    upper++;
                }
            } while ((skipWS = !skipWS) == false);
            query = query.substring(keepSelect ? 0 : lower, upper);
        }
        return query;
    }

    /**
     * Retourne le type de la table des captures. Cette méthode retourne le numéro (à partir de 1)
     * de la première requête de la table {@link #DEFAULT_PROPERTIES} que l'on aura réussi à
     * exécuter. Les requètes dont la clé ne se termine pas par {@link Table#CATCHS} seront
     * traitées comme si elle n'existait pas.
     *
     * @param  Numéro (à partir de 1) de la première requête dont la clé se termine par
     *         {@link Table#CATCHS} et dont l'exécution a réussi. Si aucune requête n'a
     *         réussi, alors cette méthode retourne 0.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    private synchronized int getCatchTableType() throws SQLException {
        if (catchTableType == 0) {
            int type = catchTableType;
            final Statement statement = connection.createStatement();
            for (int i=0; i<DEFAULT_PROPERTIES.length; i+=2) {
                final String key = DEFAULT_PROPERTIES[i];
                if (!key.endsWith('.'+Table.CATCHS)) {
                    continue;
                }
                type++;
                final String query = cutAfterFrom(Table.preferences.get(key, DEFAULT_PROPERTIES[i+1]), true);
                try {
                    statement.executeQuery(query).close();
                    catchTableType = type;
                    break;
                } catch (SQLException exception) {
                    continue;
                }
            }
            statement.close();
        }
        return catchTableType;
    }

    /**
     * Retourne les espèces énumérés dans la base de données.
     *
     * @return Ensemble des espèces répertoriées dans la base de données.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public Set<Species> getSpecies() throws SQLException {
        final String key, def;
        switch (getCatchTableType()) {
            case  1: key= "Palangres."+Table.CATCHS; def= LonglineCatchTable.SQL_SELECT; break;
            default: key=    "Seines."+Table.CATCHS; def=    SeineCatchTable.SQL_SELECT; break;
        }
        final String         query = "SELECT * "+cutAfterFrom(Table.preferences.get(key, def), false);
        final SpeciesTable spTable = new SpeciesTable(connection);
        final Statement  statement = connection.createStatement();
        final ResultSet     result = statement.executeQuery(query);
        final Set<Species> species = spTable.getSpecies(result.getMetaData());
        result   .close();
        statement.close();
        spTable  .close();
        return species;
    }

    /**
     * Retourne la liste des paramètres environnementaux disponibles. Les paramètres
     * environnementaux sont représentés par des noms courts tels que "CHL" ou "SST".
     * Ces paramètres peuvent être spécifiés en argument à la méthode
     * {@link #getEnvironmentTable}.
     *
     * @return L'ensemble des paramètres environnementaux disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public Set<String> getParameters() throws SQLException {
        return ParameterTable.list(connection, Table.PARAMETERS);
    }

    /**
     * Retourne la liste des opérations disponibles. Les opérations sont appliquées sur
     * des paramètres environnementaux. Par exemple les opérations "valeur" et "sobel3"
     * correspondent à la valeur d'un paramètre environnemental et son gradient calculé
     * par l'opérateur de Sobel, respectivement. Ces opérations peuvent être spécifiés
     * en argument à la méthode {@link #getEnvironmentTable}.
     *
     * @return L'ensemble des opérations disponibles dans la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public Set<String> getOperations() throws SQLException {
        return ParameterTable.list(connection, Table.OPERATIONS);
    }

    /**
     * Construit et retourne un objet qui interrogera la table des pêches de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Espèces d'intérêt dans la table.
     * @return La table des captures pour les espèces demandées.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public CatchTable getCatchTable(final Collection<Species> species) throws SQLException {
        final Set<Species> speciesSet = (species instanceof Set<Species>) ?
              (Set<Species>) species : new LinkedHashSet<Species>(species);
        switch (getCatchTableType()) {
            case  1: return new LonglineCatchTable(connection, timezone, speciesSet);
            default: return new SeineCatchTable   (connection, timezone, speciesSet);
        }
    }

    /**
     * Construit et retourne un objet qui interrogera la table des pêches de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Code des espèces d'intérêt dans la table (par exemple "SWO").
     * @return La table des captures pour les espèces demandées.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public CatchTable getCatchTable(final String[] species) throws SQLException {
        final List<Species> list = new ArrayList<Species>(species.length);
        final SpeciesTable spSQL = new SpeciesTable(connection);
        for (int i=0; i<species.length; i++) {
            final Species sp = spSQL.getSpecies(species[i], i);
            if (sp!=null) {
                list.add(sp);
            }
        }
        spSQL.close();
        return getCatchTable(list);
    }

    /**
     * Construit et retourne un objet qui interrogera la table des pêches de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Espèces d'intérêt dans la table (par exemple "SWO").
     * @return La table des captures pour l'espèce demandée.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public CatchTable getCatchTable(final String species) throws SQLException {
        return getCatchTable(new String[]{species});
    }

    /**
     * Construit et retourne un objet qui interrogera la table des pêches de la base de données.
     * Lorsque cette table ne sera plus nécessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @return La table des captures pour toute les espèces répertoriées.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public CatchTable getCatchTable() throws SQLException {
        return getCatchTable(getSpecies());
    }

    /**
     * Construit et retourne un objet qui interrogera la table des paramètres environnementaux.
     * Ces paramètres peuvent être mesurés aux coordonnées spatio-temporelles des captures, ou
     * dans son voisinage (par exemple quelques jours avant ou après la pêche). Chaque paramètre
     * apparaîtra dans une colonne. Ces colonnes doivent être ajoutées en appelant la méthode
     * {@link EnvironmentTable#addParameter} autant de fois que nécessaire.
     *
     * @return La table des paramètres environnementaux pour toute les captures.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public EnvironmentTable getEnvironmentTable() throws SQLException {
        return new EnvironmentTableImpl(connection);
    }

    /**
     * Construit et retourne un panneau qui permet à l'utilisateur de modifier
     * les instructions SQL. Les instructions modifiées seront utilisées pour
     * interroger les tables de la base de données de pêches.
     */
    public static SQLEditor getSQLEditor() {
        assert 2*PROPERTY_NAMES.length == DEFAULT_PROPERTIES.length;
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor=new SQLEditor(Table.preferences,
                resources.getString(ResourceKeys.EDIT_SQL_IMAGES_OR_FISHERIES_$1, new Integer(1)), Table.logger)
        {
            public String getProperty(final String name) {
                return getPreference(name);
            }
        };
        for (int i=0; i<PROPERTY_NAMES.length; i++) {
            editor.addSQL(resources.getString(PROPERTY_NAMES[i]),
                    DEFAULT_PROPERTIES[(i<<1)+1], DEFAULT_PROPERTIES[i<<1]);
        }
        return editor;
    }

    /**
     * Affiche des enregistrements de la base de données ou configure les requêtes SQL.
     * Cette méthode peut être exécutée à partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.sql.fishery.FisheryDataBase <var>options</var> [colonnes]
     * </pre></blockquote>
     *
     * Lorsque cette classe est exécutée avec l'argument <code>-config</code>, elle
     * fait apparaître une boite de dialogue  permettant de configurer les requêtes
     * SQL utilisées par la base de données. Les requêtes modifiées seront sauvegardées
     * dans les préférences du système. Lorsque ce sont d'autres arguments qui sont spécifiés,
     * ils servent à afficher ou copier dans une autre table les données environnementales.
     * Les arguments permis sont:
     *
     * <blockquote><pre>
     *  <b>-config</b> <i></i>       Configure la base de données (interface graphique)
     *  <b>-copyTo</b> <i>table</i>  Copie les données dans une table au lieu de les afficher.
     *  <b>-count</b> <i>n</i>       Nombre maximal d'enregistrement à afficher (20 par défaut).
     *  <b>-o</b> <i>operation</i>   Ajoute une opération (exemple: "valeur", "sobel3", etc.).
     *  <b>-p</b> <i>parameter</i>   Ajoute un paramètre (exemple: "SST", "CHL", etc.).
     *  <b>-t</b> <i>timeLag</i>     Ajoute un écart de temps en jours (exemple: 0, -5, etc.).
     *  <b>-locale</b> <i>name</i>   Langue et conventions d'affichage (exemple: "fr_CA")
     *  <b>-encoding</b> <i>name</i> Page de code pour les sorties     (exemple: "cp850")
     *  <b>-Xout</b> <i>filename</i> Fichier de destination (le périphérique standard par défaut)
     * </pre></blockquote>
     *
     * Les arguments <code>-o</code>, <code>-p</code> et <code>-t</code> peuvent apparaître
     * autant de fois que nécessaire afin d'ajouter plusieurs opérations, paramètres et écarts
     * de temps. Toutes les combinaisons possibles de ces arguments seront pris en compte. Chacun
     * de ces arguments doivent apparaître au moins une fois si on veut avoir des données à afficher.
     * <br><br>
     * L'argument <code>-encoding</code> est surtout utile lorsque cette méthode est lancée
     * à partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la même page
     * de code que le reste du système Windows. Il est alors nécessaire de préciser la
     * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
     * caractères étendus. La page de code en cours peut être obtenu en tappant
     * <code>chcp</code> sur la ligne de commande.
     *
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public static void main(final String[] args) throws SQLException {
        final Arguments  console = new Arguments(args);
        final boolean     config = console.getFlag("-config");
        final Integer maxRecords = console.getOptionalInteger("-count");
        final String      copyTo = console.getOptionalString("-copyTo");
        if (config) {
            getSQLEditor().showDialog(null);
            System.exit(0);
        }
        final List<String> operations = new ArrayList<String>();
        final List<String> parameters = new ArrayList<String>();
        final List<Number> timeLags   = new ArrayList<Number>();
        if (true) {
            String candidate;
            while ((candidate=console.getOptionalString("-o")) != null) {
                operations.add(candidate);
            }
            while ((candidate=console.getOptionalString("-p")) != null) {
                parameters.add(candidate);
            }
            Number t;
            while ((t=console.getOptionalInteger("-t")) != null) {
                timeLags.add(t);
            }
            console.getRemainingArguments(0);
        } else {
            //
            // Debugging code
            //
            operations.add("valeur");
            operations.add("sobel3");
            parameters.add("CHL");
            parameters.add("SST");
            parameters.add("SLA");
            timeLags.add(new Integer( 0));
            timeLags.add(new Integer(-5));
        }
        if (!operations.isEmpty() || !parameters.isEmpty() || !timeLags.isEmpty()) {
            final FisheryDataBase database = new FisheryDataBase();
            try {
                final EnvironmentTable table = database.getEnvironmentTable();
                for (final Iterator<String> o=operations.iterator(); o.hasNext();) {
                    final String operation = o.next();
                    for (final Iterator<String> p=parameters.iterator(); p.hasNext();) {
                        final String parameter = p.next();
                        for (final Iterator<Number> t=timeLags.iterator(); t.hasNext();) {
                            final int timeLag = t.next().intValue();
                            table.addParameter(operation, parameter, EnvironmentTable.CENTER,  timeLag);
                        }
                    }
                }
                if (copyTo != null) {
                    table.copyToTable(copyTo, new PrintProgress(console.out));
                } else {
                    table.print(console.out, (maxRecords!=null) ? maxRecords.intValue() : 20);
                }
                table.close();
            } catch (Exception exception) {
                database.close();
                exception.printStackTrace(console.out);
            }
        }
    }
}
