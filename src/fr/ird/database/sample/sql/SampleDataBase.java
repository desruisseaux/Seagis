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
package fr.ird.database.sample.sql;

// Base de donn�es
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
 * Connection avec la base de donn�es de �chantillons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleDataBase extends SQLDataBase implements fr.ird.database.sample.SampleDataBase {
    /**
     * Retourne une des pr�f�rences du syst�me.  Cette m�thode d�finit les
     * param�tres par d�faut qui seront utilis�s lorsque l'utilisateur n'a
     * pas d�fini de pr�f�rence pour un param�tre.
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
     * Liste des propri�t�es par d�faut. Les valeurs aux index pairs sont les index
     * des propri�t�es. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propri�t� "P�ches" donne l'instruction SQL � utiliser pour interroger la
     * table des �chantillons.
     * <br><br>
     * Les cl�s se terminant par {@link Table#SAMPLES} sont trait�es d'une mani�re
     * sp�ciale par {@link #getSampleTableType}. Cette m�thode testera la requ�te
     * correspondantes pour d�tecter le type de la table des �chantillons. Le num�ro
     * (� partir de 1) de la premi�re requ�te � r�ussir sera retourn�.
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
     * Liste des noms descriptifs � donner aux propri�t�s.
     * Ces noms sont identifi�s par des cl�s de ressources.
     * Ces cl�s doivent appara�trent dans le m�me ordre que
     * les �l�ments du tableau {@link #DEFAULT_PROPERTIES}.
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
     * Retourne l'URL par d�faut de la base de donn�es des �chantillons.
     * Cet URL sera puis� dans les pr�f�rences de l'utilisateur autant que possible.
     */
    private static String getDefaultURL() {
        LOGGER.log(loadDriver(Table.configuration.get(Configuration.KEY_DRIVER)));
        return Table.configuration.get(Configuration.KEY_SOURCE);
    }

    /**
     * Type de la table des �chantillons, ou 0 si ce type n'a pas encore �t�
     * d�termin�. Ce champ est mis � jour par {@link #getSampleTableType}
     * la premi�re fois qu'il est demand�.
     */
    private int sampleTableType;

    /**
     * Ouvre une connection avec une base de donn�es par d�faut.
     * Ce constructeur est surtout utilis� � des fins de test.
     *
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    public SampleDataBase() throws SQLException {
        // this(getDefaultURL(), TimeZone.getTimeZone(getPreference(TIMEZONE)));
        super(getDefaultURL(), 
              TimeZone.getTimeZone(Configuration.getInstance().get(Configuration.KEY_TIME_ZONE)),
              Table.configuration.get(Configuration.KEY_LOGIN), Table.configuration.get(Configuration.KEY_PASSWORD));        
    }

    /**
     * Ouvre une connection avec la base de donn�es des �chantillons.
     *
     * @param  name Protocole et nom de la base de donn�es des �chantillons.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    public SampleDataBase(final String name, final TimeZone timezone) throws SQLException {
        super(name, timezone, Table.configuration.get(Configuration.KEY_LOGIN), Table.configuration.get(Configuration.KEY_PASSWORD));
    }

    /**
     * Coupe la requ�te sp�cifi�e juste apr�s la clause "FROM".
     *
     * @param  query Requ�te � couper apr�s la clause "FROM".
     * @param  keepSelect <code>true</code> s'il faut conserver le d�but de la requ�te
     *         (habituellement une clause "SELECT"), ou <code>false</code> s'il faut
     *         retourner seulement la clause "FROM".
     * @return La requ�te coup�e.
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
     * Retourne le type de la table des �chantillons. Cette m�thode retourne le num�ro
     * (� partir de 1) de la premi�re requ�te de la table {@link #DEFAULT_PROPERTIES}
     * que l'on aura r�ussi � ex�cuter. Les requ�tes dont la cl� ne se termine pas par
     * {@link Table#SAMPLES} seront trait�es comme si elle n'existait pas.
     *
     * @param  Num�ro (� partir de 1) de la premi�re requ�te dont la cl� se termine par
     *         {@link Table#SAMPLES} et dont l'ex�cution a r�ussi. Si aucune requ�te n'a
     *         r�ussi, alors cette m�thode retourne 0.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
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
     * Construit et retourne un panneau qui permet � l'utilisateur de modifier
     * les instructions SQL. Les instructions modifi�es seront utilis�es pour
     * interroger les tables de la base de donn�es de �chantillons.
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
                throw new IllegalArgumentException("Impossible de trouver la propri�t� '" + name + "'.");
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
     * Affiche des enregistrements de la base de donn�es ou configure les requ�tes SQL.
     * Cette m�thode peut �tre ex�cut�e � partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.database.sample.SampleDataBase <var>options</var> [colonnes]
     * </pre></blockquote>
     *
     * Lorsque cette classe est ex�cut�e avec l'argument <code>-config</code>, elle
     * fait appara�tre une boite de dialogue  permettant de configurer les requ�tes
     * SQL utilis�es par la base de donn�es. Les requ�tes modifi�es seront sauvegard�es
     * dans les pr�f�rences du syst�me. Lorsque ce sont d'autres arguments qui sont sp�cifi�s,
     * ils servent � afficher ou copier dans une autre table les donn�es environnementales.
     * Les arguments permis sont:
     *
     * <blockquote><pre>
     *  <b>-config</b> <i></i>       Configure la base de donn�es (interface graphique)
     *  <b>-copyTo</b> <i>table</i>  Copie les donn�es dans une table au lieu de les afficher.
     *  <b>-count</b> <i>n</i>       Nombre maximal d'enregistrement � afficher (20 par d�faut).
     *  <b>-o</b> <i>operation</i>   Ajoute une op�ration (exemple: "valeur", "sobel3", etc.).
     *  <b>-p</b> <i>parameter</i>   Ajoute un param�tre (exemple: "SST", "CHL", etc.).
     *  <b>-t</b> <i>position</i>    Ajoute une position spatio-temporelle (exemple: -05, etc.).
     *  <b>-samples</b> <i>table</i> Table ou requ�te des �chantillons (exemple "Pr�sences par esp�ces").
     *  <b>-locale</b> <i>name</i>   Langue et conventions d'affichage (exemple: "fr_CA")
     *  <b>-encoding</b> <i>name</i> Page de code pour les sorties     (exemple: "cp850")
     *  <b>-Xout</b> <i>filename</i> Fichier de destination (le p�riph�rique standard par d�faut)
     * </pre></blockquote>
     *
     * Les arguments <code>-o</code>, <code>-p</code> et <code>-t</code> peuvent appara�tre
     * autant de fois que n�cessaire afin d'ajouter plusieurs op�rations, param�tres et �carts
     * de temps. Toutes les combinaisons possibles de ces arguments seront pris en compte. Chacun
     * de ces arguments doivent appara�tre au moins une fois si on veut avoir des donn�es � afficher.
     * <br><br>
     * L'argument <code>-encoding</code> est surtout utile lorsque cette m�thode est lanc�e
     * � partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la m�me page
     * de code que le reste du syst�me Windows. Il est alors n�cessaire de pr�ciser la
     * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
     * caract�res �tendus. La page de code en cours peut �tre obtenu en tappant
     * <code>chcp</code> sur la ligne de commande.
     *
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
     * D�finit le fichier de configuration � utiliser pour se connecter interroger 
     * la base.
     *
     * @param file  Le fichier de configuration.
     */
    public static void setDefaultFileOfConfiguration(final File file) {
        Table.preferences.put(Table.DATABASE, file.toString());
    }    
}