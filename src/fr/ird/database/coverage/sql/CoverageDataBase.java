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
package fr.ird.database.coverage.sql;

// Base de données et E/S
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.rmi.RemoteException;
import java.io.IOException;
import java.io.File;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.awt.geom.Rectangle2D;

// Divers
import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.resources.geometry.XRectangle2D;

// Seagis
import fr.ird.database.SQLDataBase;
import fr.ird.database.CatalogException;
import fr.ird.database.gui.swing.SQLEditor;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.CoverageTable;


/**
 * Connexion avec la base de données d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CoverageDataBase extends SQLDataBase implements fr.ird.database.coverage.CoverageDataBase {
    /**
     * Liste des propriétées par défaut. Les valeurs aux index pairs sont les index
     * des propriétées. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propriété "GridCoverages" donne l'instruction SQL à utiliser pour interroger
     * la table d'images.
     */
    /*private static final String[] DEFAULT_PROPERTIES = {
        Table.SERIES+":TREE",             SeriesTable.SQL_TREE,
        Table.SERIES+":ID",               SeriesTable.SQL_SELECT_BY_ID,
        Table.SERIES,                     SeriesTable.SQL_SELECT,
        Table.GRID_COVERAGES,       GridCoverageTable.SQL_SELECT,
        Table.FORMATS,                    FormatTable.SQL_SELECT,
        Table.SAMPLE_DIMENSIONS, SampleDimensionTable.SQL_SELECT,
        Table.CATEGORIES,               CategoryTable.SQL_SELECT,
        Table.GRID_GEOMETRIES,      GridGeometryTable.SQL_SELECT
    };*/

    /**
     * Liste des noms descriptifs à donner aux propriétés.
     * Ces noms sont identifiés par des clés de ressources.
     * Ces clés doivent apparaîtrent dans le même ordre que
     * les éléments du tableau {@link #DEFAULT_PROPERTIES}.
     */
    /*private static final int[] PROPERTY_NAMES = {
        ResourceKeys.SQL_SERIES_TREE,
        ResourceKeys.SQL_SERIES_BY_ID,
        ResourceKeys.SQL_SERIES,
        ResourceKeys.SQL_GRID_COVERAGES,
        ResourceKeys.SQL_FORMATS,
        ResourceKeys.SQL_SAMPLE_DIMENSIONS,
        ResourceKeys.SQL_CATEGORIES,
        ResourceKeys.SQL_GRID_GEOMETRIES
    };*/

    /**
     * La géométrie de l'ensemble des images de la base de données,
     * ou <code>null</code> si elle n'a pas encore été calculée.
     */
    private transient GridGeometryTable geometry;

    /**
     * Séries d'images à proposer par défaut. On tentera de déterminer cette série
     * une fois pour toute la première fois que {@link #getCoverageTable()} sera appelée.
     */
    private transient fr.ird.database.coverage.SeriesEntry series;

    /**
     * Retourne l'URL par défaut de la base de données d'images.
     * Cet URL sera puisé dans les préférences de l'utilisateur
     * autant que possible.
     */
    private static String getDefaultURL() {                
        final String driver = Table.configuration.get(Configuration.KEY_DRIVER);
        LOGGER.log(loadDriver(driver));
        return Table.configuration.get(Configuration.KEY_SOURCE);
    }

    /**
     * Ouvre une connection avec une base de données par défaut. Le nom de la base de
     * données ainsi que le pilote à utiliser   seront puisés dans les préférences du
     * système.
     *
     * @throws SQLException Si on n'a pas pu se connecter
     *         à la base de données.
     */
    public CoverageDataBase() throws RemoteException {
        super(getDefaultURL(), TimeZone.getTimeZone(Table.configuration.get(Configuration.KEY_TIME_ZONE)),
             Table.configuration.get(Configuration.KEY_LOGIN), Table.configuration.get(Configuration.KEY_PASSWORD));
    }

    /**
     * Ouvre une connection avec la base de données des images.
     *
     * @param  url Protocole et nom de la base de données d'images.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @throws SQLException Si on n'a pas pu se connecter
     *         à la base de données.
     */
    public CoverageDataBase(final String url, final TimeZone timezone) throws RemoteException {
        super(url, timezone, Table.configuration.get(Configuration.KEY_LOGIN), Table.configuration.get(Configuration.KEY_PASSWORD));
    }

    /**
     * Ouvre une connection avec la base de données des images.
     *
     * @param  url Protocole et nom de la base de données d'images.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @param  user Nom d'utilisateur de la base de données.
     * @param  password Mot de passe.
     * @throws SQLException Si on n'a pas pu se connecter
     *         à la base de données.
     */
    public CoverageDataBase(final String url, final TimeZone timezone, final String user, final String password) throws RemoteException
    {
        super(url, timezone, user, password);
    }

    /**
     * Vérifie que la région est valide, en puisant les
     * données dans la base de données si nécessaire.
     */
    private void ensureGeometryValid() throws RemoteException {
        if (geometry == null) {
            geometry = new GridGeometryTable(connection, timezone);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws RemoteException si le catalogue n'a pas pu être interrogé.
     */
    public synchronized Rectangle2D getGeographicArea() throws RemoteException {
        try {
            ensureGeometryValid();
            return new XRectangle2D(geometry.getGeographicArea());
        } catch (SQLException e) {
            throw new CatalogException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RemoteException si le catalogue n'a pas pu être interrogée.
     */
    public synchronized Range getTimeRange() throws RemoteException {
        try {
            ensureGeometryValid();
            return geometry.getTimeRange();
        } catch (SQLException e) {
            throw new CatalogException(e);
        }            
    }

    /**
     * {@inheritDoc}
     *
     * @throws RemoteException si la table n'a pas pu être construite.
     */
    public fr.ird.database.coverage.SeriesTable getSeriesTable() throws RemoteException {
        return new SeriesTable(connection);
    }

    /**
     * {@inheritDoc}
     *
     * @throws RemoteException si la table n'a pas pu être construite.
     */
    public synchronized CoverageTable getCoverageTable()
            throws RemoteException
    {
        if (series == null) {
            double minPeriod = Double.POSITIVE_INFINITY;
            final fr.ird.database.coverage.SeriesTable table = getSeriesTable();
            for (final fr.ird.database.coverage.SeriesEntry entry : table.getEntries()) {
                final double period = entry.getPeriod();
                if (period < minPeriod) {
                    this.series = entry;
                    minPeriod   = period;
                }
            }
            table.close();
            if (series == null) {
                throw new IllegalRecordException(Table.SERIES, Resources.format(ResourceKeys.ERROR_NO_SERIES));
            }
        }
        return getCoverageTable(series);
    }

    /**
     * {@inheritDoc}
     *
     * @param  series {@inheritDoc}
     * @throws RemoteException si la référence n'est pas valide ou table n'a pas pu être construite.
     */
    public synchronized CoverageTable getCoverageTable(final fr.ird.database.coverage.SeriesEntry series)
            throws RemoteException
    {
        Rectangle2D geographicArea = getGeographicArea();
        Range            timeRange = getTimeRange();
        Date startTime, endTime;
        if (geographicArea == null) {
            geographicArea = new Rectangle2D.Double(-180, -90, 180, 360);
        }
        if (timeRange != null) {
            startTime = (Date)timeRange.getMinValue();
            endTime   = (Date)timeRange.getMaxValue();
        } else {
            startTime = new Date(0);
            endTime   = new Date( );
        }
        final CoverageTable table = new WritableGridCoverageTable(connection, timezone);
        // Initial setup of the table. We set the series last in order to
        // avoid logging of "setGeographicArea" and "setTimeRange". Those
        // two methods do not log anything as long as the series in null.
        table.setGeographicArea(geographicArea);
        table.setTimeRange     (startTime, endTime);
        table.setSeries        (series); // Should bet last
        return table;
    }

    /**
     * {@inheritDoc}
     *
     * @param  series {@inheritDoc}
     * @throws RemoteException si la table n'a pas pu être construite.
     */
    public CoverageTable getCoverageTable(final String series) throws RemoteException {
        final fr.ird.database.coverage.SeriesTable table = getSeriesTable();       
        final fr.ird.database.coverage.SeriesEntry entry = table.getEntry(series);
        
        table.close();
        if (entry != null) {
            return getCoverageTable(entry);
        } else {
            throw new CatalogException(Resources.format(ResourceKeys.ERROR_SERIES_NOT_FOUND_$1, series));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param  series {@inheritDoc}
     * @throws RemoteException si la table n'a pas pu être construite.
     */
    public CoverageTable getCoverageTable(final int seriesID) throws RemoteException {
        final fr.ird.database.coverage.SeriesTable table = getSeriesTable();
        final fr.ird.database.coverage.SeriesEntry entry = table.getEntry(seriesID);
        table.close();
        if (entry != null) {
            return getCoverageTable(entry);
        } else {
            throw new CatalogException(Resources.format(ResourceKeys.ERROR_SERIES_NOT_FOUND_$1,
                                                    new Integer(seriesID)));
        }
    }

    /**
     * Retourne le processeur par défaut à utiliser pour appliquer des opérations sur les images
     * lues. Les operations sont spécifiées par {@link CoverageTable#setOperation(String)} et
     * appliquée lors de l'appel de {@link CoverageEntry#getGridCoverage}. Le processeur par
     * défaut accepte la combinaison d'un certain nombre d'opérations séparées par des point
     * virgules, par exemple <code>"NodataFilter;GradientMagnitude"</code>.
     *
     * @return Le processeur par défaut.
     * @see CoverageTable#setOperation(String)
     * @see CoverageEntry#getGridCoverage
     */
    public static GridCoverageProcessor getDefaultGridCoverageProcessor() {
        return GridCoverageEntry.PROCESSOR;
    }

    /**
     * Définit le processeur par défaut à utiliser pour appliquer des opérations sur les images
     * lues. Les operations sont spécifiées par {@link CoverageTable#setOperation(String)} et
     * appliquée lors de l'appel de {@link CoverageEntry#getGridCoverage}.
     *
     * @param processor Le processeur par défaut.
     * @see CoverageTable#setOperation(String)
     * @see CoverageEntry#getGridCoverage
     */
    public static void setDefaultGridCoverageProcessor(final GridCoverageProcessor processor) {
        GridCoverageEntry.PROCESSOR = processor;
    }

    /**
     * Retourne le répertoire racine à partir d'où construire le
     * chemin des images. Les chemins relatifs spécifiés dans la
     * base de données seront ajoutés à ce répertoire racine.
     *
     * @return Répertoire racine des images.
     */
    public static File getDefaultDirectory() {
        // return (Table.directory!=null) ? Table.directory : new File(".");
        return new File(Table.configuration.get(Configuration.KEY_DIRECTORY)!=null ? Table.configuration.get(Configuration.KEY_DIRECTORY) : ".");
    }

    /**
     * Définit le répertoire racine à partir duquel puiser les images. L'appel de cette méthode
     * affecte toutes les {@link CoverageTable} qui utilisent le répertoire racine par défaut,
     * pour la session de travail courante ainsi que pour toutes les prochaines sessions (le
     * répertoire sera sauvegardé dans les préférences systèmes).
     *
     * @param directory Répertoire racine des images.
     */
    public static void setDefaultDirectory(final File directory) {
        Table.directory=directory;
        // if (directory!=null) {
            // Table.PREFERENCES.put(Table.DIRECTORY, directory.getPath());
        // } else {
            // Table.PREFERENCES.remove(Table.DIRECTORY);
            // Configuration.get(Configuration.KEY_DIRECTORY_ROOT);
        // }
        Table.configuration.set(Table.configuration.KEY_DIRECTORY, directory.toString());
    }

    /**
     * Retourne le fichier de configuration permettant de se connecter et d'interroger 
     * la base.
     */
    public static File getDefaultFileOfConfiguration() {
        final String name = Table.preferences.get(Table.DATABASE, "");
        if (name.trim().length() == 0 || !(new File(name).exists())) {
            return new File(Configuration.class.getClassLoader().
                getResource("fr/ird/database/coverage/sql/resources/resources.properties").getPath());
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

    /**
     * Retourne la liste des répertoires synchronisées. Voyez
     * {@link #setSynchronizedDirectories} pour une description
     * plus détaillée.
     */
    public static File[] getSynchronizedDirectories() {
        return (File[]) FormatEntry.synchronizedDirectories.clone();
    }

    /**
     * Spécifie les répertoires dans lesquels il faudra synchroniser les lectures d'images. Cette liste
     * sert à éviter qu'on ne lise en même temps deux images qui se trouve sur le même disque, afin de
     * réduire les déplacements requis de la tête du lecteur. Cette liste s'interprète comme suit:
     *
     * soit deux images qui ont les chemins complets
     *
     * "<code>/common/directory1/filename1</code>" et "<code>/common/directory2/filename2</code>".
     *
     * Ces deux images ont un répertoire parent commun: "<code>common</code>". Si ce répertoire
     * commun apparaît dans la liste, ou s'il est un sous-répertoire d'un répertoire apparaissant
     * dans la liste, alors les lectures de ces images seront synchronisées de façon à éviter qu'elles
     * ne soient faites en même temps.
     *
     * <p>Sous Windows, cette liste contient par défaut les lettres des lecteurs ("A:", "C:", "D:",
     * etc.). Ca signifie que deux images peuvent être lues en même temps si l'une est lue sur le
     * lecteur "C:" et l'autre sur le lecteur "D:", mais pas si elles sont toutes deux lues sur le
     * lecteur "C:". Sous Unix, cette liste contient par défaut "/", ce qui signifie qu'aucune
     * image ne peut être lue en même temps. Spécifier une liste de répertoires plus précise peut
     * donc améliorer les performances, surtout sur Unix.</p>
     *
     * <p>La liste spécifiée dépend typiquement de la configuration matérielle de système, telle que
     * la présence physique de disques durs ou de lecteur de bandes. Cette configuration est la même
     * pour toute les base de données sur une machine, mais peut varier d'une machine à l'autre.
     * C'est pourquoi cette liste est statique (s'applique à toute les bases de données de la
     * machine virtuelle courante) plutôt que liée à une base de données particulière.</p>
     *
     * <p>Notez enfin que cette liste n'a d'impact que sur la performance. Que la liste soit
     * précise ou pas ne devrait pas affecter les résultats obtenus.</p>
     */
    public static void setSynchronizedDirectories(final File[] directories) throws IOException {
        final File[] dir = new File[directories.length];
        for (int i=0; i<dir.length; i++) {
            dir[i]=directories[i].getCanonicalFile();
        }
        FormatEntry.synchronizedDirectories = dir;
    }

    /**
     * Construit et retourne un panneau qui permet à l'utilisateur de modifier
     * les instructions SQL. Les instructions modifiées seront conservées dans
     * les préférences systèmes et utilisées pour interroger les tables de la
     * base de données d'images.
     */
    public static SQLEditor getSQLEditor() {
        //assert(2*PROPERTY_NAMES.length == DEFAULT_PROPERTIES.length);
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor = new SQLEditor(Table.configuration,
            resources.getString(ResourceKeys.EDIT_SQL_COVERAGES_OR_SAMPLES_$1, new Integer(0)), LOGGER)
        {
            public Configuration.Key getProperty(final String name) {
                final Configuration.Key[] keys = {Configuration.KEY_SERIES_TREE,
                                                  Configuration.KEY_SERIES_ID,
                                                  Configuration.KEY_SERIES_SUBSERIES,
                                                  Configuration.KEY_SERIES_NAME,
                                                  Configuration.KEY_GRID_COVERAGES_ID_INSERT,
                                                  Configuration.KEY_GRID_COVERAGES_INSERT,
                                                  Configuration.KEY_GRID_GEOMETRIES_ID_INSERT,
                                                  Configuration.KEY_GRID_GEOMETRIES_INSERT,
                                                  Configuration.KEY_GRID_COVERAGES,
                                                  Configuration.KEY_GRID_COVERAGES3,
                                                  Configuration.KEY_GRID_COVERAGES1,
                                                  Configuration.KEY_GRID_COVERAGES2,
                                                  Configuration.KEY_FORMATS,
                                                  Configuration.KEY_SAMPLE_DIMENSIONS,
                                                  Configuration.KEY_CATEGORIES,
                                                  Configuration.KEY_GRID_GEOMETRIES,
                                                  Configuration.KEY_GEOMETRY,
                                                  Configuration.KEY_DRIVER,
                                                  Configuration.KEY_SOURCE,
                                                  Configuration.KEY_TIME_ZONE,
                                                  Configuration.KEY_DIRECTORY,
                                                  Configuration.KEY_LOGIN,
                                                  Configuration.KEY_PASSWORD};

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
        //     editor.addSQL(resources.getString(PROPERTY_NAMES[i]),
        //                   DEFAULT_PROPERTIES[i*2+1], DEFAULT_PROPERTIES[i*2]);
        // }
                
        final Configuration.Key[] keys = {Configuration.KEY_SERIES_TREE,
                                          Configuration.KEY_SERIES_ID,
                                          Configuration.KEY_SERIES_SUBSERIES,
                                          Configuration.KEY_SERIES_NAME,
                                          Configuration.KEY_GRID_COVERAGES_ID_INSERT,
                                          Configuration.KEY_GRID_COVERAGES_INSERT,
                                          Configuration.KEY_GRID_GEOMETRIES_ID_INSERT,
                                          Configuration.KEY_GRID_GEOMETRIES_INSERT,
                                          Configuration.KEY_GRID_COVERAGES,
                                          Configuration.KEY_GRID_COVERAGES3,
                                          Configuration.KEY_GRID_COVERAGES1,
                                          Configuration.KEY_GRID_COVERAGES2,
                                          Configuration.KEY_FORMATS,
                                          Configuration.KEY_SAMPLE_DIMENSIONS,
                                          Configuration.KEY_CATEGORIES,
                                          Configuration.KEY_GRID_GEOMETRIES,
                                          Configuration.KEY_GEOMETRY};
                                          
        for (int i=0; i<keys.length; i++) {
            editor.addSQL(keys[i]);
        }
        return editor;
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws RemoteException si un problème est survenu lors de la disposition des ressources.
     */
    public void close() throws RemoteException {
        if (geometry != null) {
            geometry.close();
        }
        super.close();
    }

    /**
     * Affiche des enregistrements de la base de données ou configure les requêtes SQL.
     * Cette méthode peut être exécutée à partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.database.coverage.sql.CoverageDataBase <var>options</var>
     * </pre></blockquote>
     *
     * Lorsque cette classe est exécutée avec l'argument <code>-config</code>, elle
     * fait apparaître une boite de dialogue  permettant de configurer les requêtes
     * SQL utilisées par la base de données. Les requêtes modifiées seront sauvegardées
     * dans les préférences du système. Lorsque des arguments sont spécifiés,
     * ils sont interprétés comme suit:
     *
     * <blockquote><pre>
     *  <b>-help</b> <i></i>         Affiche cette liste des options
     *  <b>-series</b> <i></i>       Affiche l'arborescence des séries
     *  <b>-formats</b> <i></i>      Affiche la table des formats
     *  <b>-config</b> <i></i>       Configure la base de données (interface graphique)
     *  <b>-browse</b> <i></i>       Affiche le contenu de toute la base de données (interface graphique)
     *  <b>-source</b> <i>name</i>   Source des données                (exemple: "jdbc:odbc:SEAS-Images")
     *  <b>-driver</b> <i>name</i>   Pilote de la base de données      (exemple: "sun.jdbc.odbc.JdbcOdbcDriver")
     *  <b>-locale</b> <i>name</i>   Langue et conventions d'affichage (exemple: "fr_CA")
     *  <b>-encoding</b> <i>name</i> Page de code pour les sorties     (exemple: "cp850")
     *  <b>-Xout</b> <i>filename</i> Fichier de destination (le périphérique standard par défaut)
     * </pre></blockquote>
     *
     * L'argument <code>-encoding</code> est surtout utile lorsque cette méthode est lancée
     * à partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la même page
     * de code que le reste du système Windows. Il est alors nécessaire de préciser la
     * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
     * caractères étendus. La page de code en cours peut être obtenu en tappant
     * <code>chcp</code> sur la ligne de commande.
     *
     * @throws RemoteException si l'interrogation du catalogue a échoué.
     */
    public static void main(final String[] args) throws RemoteException {
        org.geotools.resources.MonolineFormatter.init("fr.ird");
        final Main console = new Main(args);
        if (console.config) {
            getSQLEditor().showDialog(null);
            System.exit(0);
        }
        console.run();
    }
}
