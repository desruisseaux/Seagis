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
package fr.ird.sql.fishery;

// Base de donn�es
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
 * Connection avec la base de donn�es de p�ches.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class FisheryDataBase extends DataBase {
    /**
     * Retourne une des pr�f�rences du syst�me.  Cette m�thode d�finit les
     * param�tres par d�faut qui seront utilis�s lorsque l'utilisateur n'a
     * pas d�fini de pr�f�rence pour un param�tre.
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
     * Liste des propri�t�es par d�faut. Les valeurs aux index pairs sont les index
     * des propri�t�es. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propri�t� "P�ches" donne l'instruction SQL � utiliser pour interroger la
     * table des p�ches.
     * <br><br>
     * Les cl�s se terminant par {@link Table#CATCHS} sont trait�es d'une mani�re
     * sp�ciale par {@link #getCatchTableType}.  Cette m�thode testera la requ�te
     * correspondantes pour d�tecter le type de la table des captures.  Le num�ro
     * (� partir de 1) de la premi�re requ�te � r�ussir sera retourn�.
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
     * Liste des noms descriptifs � donner aux propri�t�s.
     * Ces noms sont identifi�s par des cl�s de ressources.
     * Ces cl�s doivent appara�trent dans le m�me ordre que
     * les �l�ments du tableau {@link #DEFAULT_PROPERTIES}.
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
     * Retourne l'URL par d�faut de la base de donn�es de p�ches.
     * Cet URL sera puis� dans les pr�f�rences de l'utilisateur
     * autant que possible.
     */
    private static String getDefaultURL() {
        Table.logger.log(loadDriver(getPreference(DRIVER)));
        return getPreference(SOURCE);
    }

    /**
     * Type de la table des captures, ou 0 si ce type n'a pas encore �t�
     * d�termin�. Ce champ est mis � jour par {@link #getCatchTableType}
     * la premi�re fois qu'il est demand�.
     */
    private int catchTableType;

    /**
     * Ouvre une connection avec une base de donn�es par d�faut.
     * Ce constructeur est surtout utilis� � des fins de test.
     *
     * @throws SQLException Si on n'a pas pu se connecter
     *         � la base de donn�es.
     */
    public FisheryDataBase() throws SQLException {
        super(getDefaultURL(), TimeZone.getTimeZone(getPreference(TIMEZONE)));
    }

    /**
     * Ouvre une connection avec la base de donn�es de p�ches.
     *
     * @param  name Protocole et nom de la base de donn�es des p�ches.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    public FisheryDataBase(final String name, final TimeZone timezone) throws SQLException {
        super(name, timezone);
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
     * Retourne le type de la table des captures. Cette m�thode retourne le num�ro (� partir de 1)
     * de la premi�re requ�te de la table {@link #DEFAULT_PROPERTIES} que l'on aura r�ussi �
     * ex�cuter. Les requ�tes dont la cl� ne se termine pas par {@link Table#CATCHS} seront
     * trait�es comme si elle n'existait pas.
     *
     * @param  Num�ro (� partir de 1) de la premi�re requ�te dont la cl� se termine par
     *         {@link Table#CATCHS} et dont l'ex�cution a r�ussi. Si aucune requ�te n'a
     *         r�ussi, alors cette m�thode retourne 0.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
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
     * Retourne les esp�ces �num�r�s dans la base de donn�es.
     *
     * @return Ensemble des esp�ces r�pertori�es dans la base de donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
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
     * Retourne la liste des param�tres environnementaux disponibles. Les param�tres
     * environnementaux sont repr�sent�s par des noms courts tels que "CHL" ou "SST".
     * Ces param�tres peuvent �tre sp�cifi�s en argument � la m�thode
     * {@link #getEnvironmentTable}.
     *
     * @return L'ensemble des param�tres environnementaux disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public Set<String> getParameters() throws SQLException {
        return ParameterTable.list(connection, Table.PARAMETERS);
    }

    /**
     * Retourne la liste des op�rations disponibles. Les op�rations sont appliqu�es sur
     * des param�tres environnementaux. Par exemple les op�rations "valeur" et "sobel3"
     * correspondent � la valeur d'un param�tre environnemental et son gradient calcul�
     * par l'op�rateur de Sobel, respectivement. Ces op�rations peuvent �tre sp�cifi�s
     * en argument � la m�thode {@link #getEnvironmentTable}.
     *
     * @return L'ensemble des op�rations disponibles dans la base de donn�es.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public Set<String> getOperations() throws SQLException {
        return ParameterTable.list(connection, Table.OPERATIONS);
    }

    /**
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Esp�ces d'int�r�t dans la table.
     * @return La table des captures pour les esp�ces demand�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
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
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Code des esp�ces d'int�r�t dans la table (par exemple "SWO").
     * @return La table des captures pour les esp�ces demand�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
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
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Esp�ces d'int�r�t dans la table (par exemple "SWO").
     * @return La table des captures pour l'esp�ce demand�e.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public CatchTable getCatchTable(final String species) throws SQLException {
        return getCatchTable(new String[]{species});
    }

    /**
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @return La table des captures pour toute les esp�ces r�pertori�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public CatchTable getCatchTable() throws SQLException {
        return getCatchTable(getSpecies());
    }

    /**
     * Construit et retourne un objet qui interrogera la table des param�tres environnementaux.
     * Ces param�tres peuvent �tre mesur�s aux coordonn�es spatio-temporelles des captures, ou
     * dans son voisinage (par exemple quelques jours avant ou apr�s la p�che). Chaque param�tre
     * appara�tra dans une colonne. Ces colonnes doivent �tre ajout�es en appelant la m�thode
     * {@link EnvironmentTable#addParameter} autant de fois que n�cessaire.
     *
     * @return La table des param�tres environnementaux pour toute les captures.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public EnvironmentTable getEnvironmentTable() throws SQLException {
        return new EnvironmentTableImpl(connection);
    }

    /**
     * Construit et retourne un panneau qui permet � l'utilisateur de modifier
     * les instructions SQL. Les instructions modifi�es seront utilis�es pour
     * interroger les tables de la base de donn�es de p�ches.
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
     * Affiche des enregistrements de la base de donn�es ou configure les requ�tes SQL.
     * Cette m�thode peut �tre ex�cut�e � partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.sql.fishery.FisheryDataBase <var>options</var> [colonnes]
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
     *  <b>-t</b> <i>timeLag</i>     Ajoute un �cart de temps en jours (exemple: 0, -5, etc.).
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
