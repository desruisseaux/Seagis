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
package fr.ird.database.sample.sql;

// Base de données
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;

// Collections et divers
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.TimeZone;
import java.io.IOException;
import java.io.File;

// Geotools
import org.geotools.resources.Arguments;
import org.geotools.gui.headless.ProgressPrinter;

// Seagis
import fr.ird.animat.Species;
import fr.ird.database.SQLDataBase;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.sample.ParameterEntry;
import fr.ird.database.sample.OperationEntry;
import fr.ird.database.sample.RelativePositionEntry;
import fr.ird.database.gui.swing.SQLEditor;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Connection avec la base de données de échantillons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleDataBase extends SQLDataBase implements fr.ird.database.sample.SampleDataBase {
    /**
     * Retourne une des préférences du système.  Cette méthode définit les
     * paramètres par défaut qui seront utilisés lorsque l'utilisateur n'a
     * pas défini de préférence pour un paramètre.
     */
    /*private static String getPreference(final String name) {
        String def=null;
        if (name != null)
        {
                 if (name.equalsIgnoreCase(DRIVER))   def = "sun.jdbc.odbc.JdbcOdbcDriver";
            else if (name.equalsIgnoreCase(SOURCE))   def = "jdbc:odbc:SEAS-Sennes";
            else if (name.equalsIgnoreCase(TIMEZONE)) def = "Indian/Reunion";
        }
        return Table.preferences.get(name, def);
    }*/

    /**
     * Liste des propriétées par défaut. Les valeurs aux index pairs sont les index
     * des propriétées. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propriété "Pêches" donne l'instruction SQL à utiliser pour interroger la
     * table des échantillons.
     * <br><br>
     * Les clés se terminant par {@link Table#SAMPLES} sont traitées d'une manière
     * spéciale par {@link #getSampleTableType}. Cette méthode testera la requête
     * correspondantes pour détecter le type de la table des échantillons. Le numéro
     * (à partir de 1) de la première requête à réussir sera retourné.
     */
    /*private static final String[] DEFAULT_PROPERTIES = {
        Table.SPECIES,                SpeciesTable         .SQL_SELECT,
        "Punctual."+Table.SAMPLES,    PunctualSampleTable  .SQL_SELECT, // SampleTableType #1
        "Linear."+Table.SAMPLES,      LinearSampleTable    .SQL_SELECT, // SampleTableType #2
        Table.SAMPLES+":UPDATE",      SampleTable          .SQL_UPDATE,
        Table.ENVIRONMENTS,           EnvironmentTableStep .SQL_SELECT,
        Table.ENVIRONMENTS+":UPDATE", EnvironmentTable     .SQL_UPDATE,
        Table.ENVIRONMENTS+":INSERT", EnvironmentTable     .SQL_INSERT,
        Table.LINEAR_MODELS,          LinearModelTable     .SQL_SELECT,
        Table.DESCRIPTORS,            DescriptorTable      .SQL_SELECT,
        Table.PARAMETERS,             ParameterTable       .SQL_SELECT,
        Table.OPERATIONS,             OperationTable       .SQL_SELECT,
        Table.POSITIONS,              RelativePositionTable.SQL_SELECT
    };*/

    /**
     * Liste des noms descriptifs à donner aux propriétés.
     * Ces noms sont identifiés par des clés de ressources.
     * Ces clés doivent apparaîtrent dans le même ordre que
     * les éléments du tableau {@link #DEFAULT_PROPERTIES}.
     */
    /*private static final int[] PROPERTY_NAMES = {
        ResourceKeys.SQL_SPECIES,
        ResourceKeys.SQL_SAMPLES_POINT,
        ResourceKeys.SQL_SAMPLES_LINE,
        ResourceKeys.SQL_SAMPLES_UPDATE,
        ResourceKeys.SQL_ENVIRONMENTS,
        ResourceKeys.SQL_ENVIRONMENTS_UPDATE,
        ResourceKeys.SQL_ENVIRONMENTS_INSERT,
        ResourceKeys.SQL_LINEAR_MODELS,
        ResourceKeys.SQL_DESCRIPTORS,
        ResourceKeys.SQL_PARAMETERS,
        ResourceKeys.SQL_OPERATIONS,
        ResourceKeys.SQL_POSITIONS
    };*/

    /**
     * Retourne l'URL par défaut de la base de données des échantillons.
     * Cet URL sera puisé dans les préférences de l'utilisateur autant que possible.
     */
    private static String getDefaultURL() {
        LOGGER.log(loadDriver(Table.configuration.get(Configuration.KEY_DRIVER)));
        return Table.configuration.get(Configuration.KEY_SOURCE);
    }

    /**
     * Type de la table des échantillons, ou 0 si ce type n'a pas encore été
     * déterminé. Ce champ est mis à jour par {@link #getSampleTableType}
     * la première fois qu'il est demandé.
     */
    private int sampleTableType;

    /**
     * Ouvre une connection avec une base de données par défaut.
     * Ce constructeur est surtout utilisé à des fins de test.
     *
     * @throws SQLException Si on n'a pas pu se connecter à la base de données.
     */
    public SampleDataBase() throws SQLException {
        // this(getDefaultURL(), TimeZone.getTimeZone(getPreference(TIMEZONE)));
        super(getDefaultURL(), 
              TimeZone.getTimeZone(Configuration.getInstance().get(Configuration.KEY_TIME_ZONE)),
              Table.configuration.get(Configuration.KEY_LOGIN), Table.configuration.get(Configuration.KEY_PASSWORD));        
    }

    /**
     * Ouvre une connection avec la base de données des échantillons.
     *
     * @param  name Protocole et nom de la base de données des échantillons.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @throws SQLException Si on n'a pas pu se connecter à la base de données.
     */
    public SampleDataBase(final String name, final TimeZone timezone) throws SQLException {
        super(name, timezone, Table.configuration.get(Configuration.KEY_LOGIN), Table.configuration.get(Configuration.KEY_PASSWORD));
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
            while (upper<length && Character.isWhitespace(query.charAt(upper))) {
                upper++;
            }
            boolean insideQuotes = false;
            while (upper < length) {
                final char c = query.charAt(upper);
                switch (c) {
                    case '"': insideQuotes = !insideQuotes; break;
                    case '[': insideQuotes = true;          break;
                    case ']': insideQuotes = false;         break;
                }
                if (!insideQuotes && Character.isWhitespace(c)) {
                    break;
                }
                upper++;
            }
            query = query.substring(keepSelect ? 0 : lower, upper);
        }
        return query;
    }

    /**
     * Retourne le type de la table des échantillons. Cette méthode retourne le numéro
     * (à partir de 1) de la première requête de la table {@link #DEFAULT_PROPERTIES}
     * que l'on aura réussi à exécuter. Les requètes dont la clé ne se termine pas par
     * {@link Table#SAMPLES} seront traitées comme si elle n'existait pas.
     *
     * @param  Numéro (à partir de 1) de la première requête dont la clé se termine par
     *         {@link Table#SAMPLES} et dont l'exécution a réussi. Si aucune requête n'a
     *         réussi, alors cette méthode retourne 0.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    private synchronized int getSampleTableType() throws SQLException {
        /*if (sampleTableType == 0) {
            int type = sampleTableType;
            final Statement statement = connection.createStatement();
            for (int i=0; i<DEFAULT_PROPERTIES.length; i+=2) {
                final String key = DEFAULT_PROPERTIES[i];
                if (!key.endsWith('.'+Table.SAMPLES)) {
                    continue;
                }
                type++;
                final String query = cutAfterFrom(Table.preferences.get(key, DEFAULT_PROPERTIES[i+1]), true);
                try {
                    statement.executeQuery(query).close();
                    sampleTableType = type;
                    break;
                } catch (SQLException exception) {
                    continue;
                }
            }
            statement.close();
        }
        return sampleTableType;*/
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Species> getSpecies() throws SQLException {
        /*final String key, def;
        switch (getSampleTableType()) {
            case  1: key="Punctual."+Table.SAMPLES; def= PunctualSampleTable.SQL_SELECT; break;
            case  2: key=  "Linear."+Table.SAMPLES; def= LinearSampleTable.SQL_SELECT;   break;
            default: throw new SQLException("Type de table inconnu.");
        }
        final String         query = "SELECT * "+cutAfterFrom(Table.preferences.get(key, def), false);
        final SpeciesTable spTable = new SpeciesTable(connection);
        final Statement  statement = connection.createStatement();
        final ResultSet     result = statement.executeQuery(query);
        final Set<Species> species = spTable.getSpecies(result.getMetaData());
        result   .close();
        statement.close();
        spTable  .close();
        return species;*/
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ParameterEntry> getParameters(final SeriesTable series) throws SQLException {
        final ParameterTable table = new ParameterTable(connection, ParameterTable.LIST, series);
        final Set<ParameterEntry> set = table.list();
        table.close();
        return set;
    }

    /**
     * {@inheritDoc}
     */
    public Set<OperationEntry> getOperations() throws SQLException {
        final OperationTable table = new OperationTable(connection, OperationTable.LIST);
        final Set<OperationEntry> set = table.list();
        table.close();
        return set;
    }

    /**
     * {@inheritDoc}
     */
    public Set<RelativePositionEntry> getRelativePositions() throws SQLException {
        final RelativePositionTable table = new RelativePositionTable(connection, RelativePositionTable.LIST);
        final Set<RelativePositionEntry> set = table.list();
        table.close();
        return set;
    }

    /**
     * {@inheritDoc}
     */
    public fr.ird.database.sample.SampleTable getSampleTable(final Collection<Species> species)
            throws SQLException
    {
        final Set<Species> speciesSet = (species instanceof Set) ?
              (Set<Species>) species : new LinkedHashSet<Species>(species);
        switch (getSampleTableType()) {
            case  1: return new PunctualSampleTable(connection, timezone, speciesSet);
            case  2: return new   LinearSampleTable(connection, timezone, speciesSet);
            default: throw new SQLException("Type de table inconnu.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public fr.ird.database.sample.SampleTable getSampleTable(final String[] species)
            throws SQLException
    {
        final List<Species> list = new ArrayList<Species>(species.length);
        final SpeciesTable spSQL = new SpeciesTable(connection);
        for (int i=0; i<species.length; i++) {
            final Species sp = spSQL.getSpecies(species[i], i);
            if (sp!=null) {
                list.add(sp);
            }
        }
        spSQL.close();
        return getSampleTable(list);
    }

    /**
     * {@inheritDoc}
     */
    public fr.ird.database.sample.SampleTable getSampleTable(final String species)
            throws SQLException
    {
        return getSampleTable(new String[]{species});
    }

    /**
     * {@inheritDoc}
     */
    public fr.ird.database.sample.SampleTable getSampleTable()
            throws SQLException
    {
        return getSampleTable(getSpecies());
    }

    /**
     * {@inheritDoc}
     */
    public fr.ird.database.sample.EnvironmentTable getEnvironmentTable(final SeriesTable series)
            throws SQLException
    {
        return new EnvironmentTable(connection, series);
    }

    /**
     * Construit et retourne un panneau qui permet à l'utilisateur de modifier
     * les instructions SQL. Les instructions modifiées seront utilisées pour
     * interroger les tables de la base de données de échantillons.
     */
    public static SQLEditor getSQLEditor() {
        // assert 2*PROPERTY_NAMES.length == DEFAULT_PROPERTIES.length;
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor = new SQLEditor(Configuration.getInstance(),
            resources.getString(ResourceKeys.EDIT_SQL_COVERAGES_OR_SAMPLES_$1, new Integer(1)), LOGGER)
        {
            public Configuration.Key getProperty(final String name) {
                final Configuration.Key[] keys = {Table.configuration.KEY_DESCRIPTORS,
                                                  Table.configuration.KEY_PARAMETERS,
                                                  Table.configuration.KEY_LINEAR_MODELS,
                                                  Table.configuration.KEY_ENVIRONMENTS_UPDATE,
                                                  Table.configuration.KEY_ENVIRONMENTS_INSERT,
                                                  Table.configuration.KEY_OPERATIONS,
                                                  Table.configuration.KEY_LINEAR_SAMPLE,
                                                  Table.configuration.KEY_ENVIRONMENTS,
                                                  Table.configuration.KEY_POSITIONS,
                                                  Table.configuration.KEY_SAMPLES_UPDATE,
                                                  Table.configuration.KEY_SPECIES,
                                                  Table.configuration.KEY_PUNCTUAL_SAMPLE,
                                                  Table.configuration.KEY_DRIVER,
                                                  Table.configuration.KEY_TIME_ZONE,
                                                  Table.configuration.KEY_SOURCE};                
                for (int i=0 ; i<keys.length ; i++) 
                {
                    final Configuration.Key key = keys[i];
                    if (key.name.equals(name)) 
                    {
                        return key;
                    }
                }
                throw new IllegalArgumentException("Impossible de trouver la propriété '" + name + "'.");
            }
        };
        // for (int i=0; i<PROPERTY_NAMES.length; i++) {
        //    editor.addSQL(resources.getString(PROPERTY_NAMES[i]),
        //            DEFAULT_PROPERTIES[(i<<1)+1], DEFAULT_PROPERTIES[i<<1]);
        //}
        
        final Configuration.Key[] keyArray = {Table.configuration.KEY_DESCRIPTORS,
                                              Table.configuration.KEY_PARAMETERS,
                                              Table.configuration.KEY_LINEAR_MODELS,
                                              Table.configuration.KEY_ENVIRONMENTS_UPDATE,
                                              Table.configuration.KEY_ENVIRONMENTS_INSERT,
                                              Table.configuration.KEY_OPERATIONS,
                                              Table.configuration.KEY_LINEAR_SAMPLE,
                                              Table.configuration.KEY_ENVIRONMENTS,
                                              Table.configuration.KEY_POSITIONS,
                                              Table.configuration.KEY_SAMPLES_UPDATE,
                                              Table.configuration.KEY_SPECIES,
                                              Table.configuration.KEY_PUNCTUAL_SAMPLE};
                                              
        for (int i=0; i<keyArray.length; i++) {
            editor.addSQL(keyArray[i]);
        }        
        
        return editor;
    }

    /**
     * Affiche des enregistrements de la base de données ou configure les requêtes SQL.
     * Cette méthode peut être exécutée à partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.database.sample.SampleDataBase <var>options</var> [colonnes]
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
     *  <b>-t</b> <i>position</i>    Ajoute une position spatio-temporelle (exemple: -05, etc.).
     *  <b>-samples</b> <i>table</i> Table ou requête des échantillons (exemple "Présences par espèces").
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
        org.geotools.resources.MonolineFormatter.init("fr.ird");
        final Arguments  console = new Arguments(args);
        final boolean     config = console.getFlag("-config");
        final Integer maxRecords = console.getOptionalInteger("-count");
        final String      copyTo = console.getOptionalString("-copyTo");
        final String sampleTable = console.getOptionalString("-samples");
        if (config) {
            getSQLEditor().showDialog(null);
            System.exit(0);
        }
        final List<String> operations = new ArrayList<String>();
        final List<String> parameters = new ArrayList<String>();
        final List<String> positions  = new ArrayList<String>();
        if (true) {
            String candidate;
            while ((candidate=console.getOptionalString("-o")) != null) {
                operations.add(candidate);
            }
            while ((candidate=console.getOptionalString("-p")) != null) {
                parameters.add(candidate);
            }
            while ((candidate=console.getOptionalString("-t")) != null) {
                positions.add(candidate);
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
            positions .add("+00");
            positions .add("-05");
        }
        if (!operations.isEmpty() && !parameters.isEmpty() && !positions.isEmpty()) {
            final SampleDataBase database = new SampleDataBase();
            try {
                final fr.ird.database.sample.EnvironmentTable table = database.getEnvironmentTable(null);
                for (final String operation : operations) {
                    for (final String parameter : parameters) {
                        for (final String position : positions) {
                            table.addParameter(parameter, operation, position, false);
                        }
                    }
                }
                table.setSampleTable(sampleTable);
                if (copyTo != null) {
                    table.copyToTable(null, copyTo, new ProgressPrinter(console.out));
                } else {
                    table.print(console.out, (maxRecords!=null) ? maxRecords.intValue() : 20);
                }
                table.close();
                database.close();
            } catch (Exception exception) {
                database.close();
                exception.printStackTrace(console.out);
            }
        }
    }
    
    /**
     * Retourne le fichier de configuration permettant de se connecter et d'interroger 
     * la base.
     *
     */
    public static File getDefaultFileOfConfiguration() {
        final String name = Table.preferences.get(Table.DATABASE, "");
        if (name.trim().length() == 0 || (!new File(name).exists())) {
            return new File(Configuration.class.getClassLoader().
                getResource("fr/ird/database/sample/sql/resources/resources.properties").getPath());
        }
        return new File(name);
    }    

    /**
     * Définit le fichier de configuration à utiliser pour se connecter interroger 
     * la base.
     *
     * @param file  Le fichier de configuration.
     */
    public static void setDefaultFileOfConfiguration(final File file) {
        Table.preferences.put(Table.DATABASE, file.toString());
    }    
}