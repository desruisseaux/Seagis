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
package fr.ird.sql.image;

// Base de données
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import fr.ird.sql.SQLEditor;
import fr.ird.sql.DataBase;

// Entrés/sorties
import java.io.File;
import java.io.IOException;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.awt.geom.Rectangle2D;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Divers
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import javax.media.jai.util.Range;
import fr.ird.resources.Resources;


/**
 * Connection avec la base de données d'images.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class ImageDataBase extends DataBase
{
    /**
     * Register a set of service providers
     * the first time this class is loaded.
     */
    static
    {
        Codecs.register();
    }

    /**
     * Requête SQL utilisé pour obtenir les coordonnées
     * et la plage de temps couverte par les images.
     */
    private static final String SQL_AREA=
                    "SELECT Min("+Table.IMAGES+".start_time) AS start_time, "+
                           "Max("+Table.IMAGES+".end_time) AS end_time, "+
                           "Min("+Table.AREAS +".xmin) AS xmin, "+
                           "Min("+Table.AREAS +".ymin) AS ymin, "+
                           "Max("+Table.AREAS +".xmax) AS xmax, "+
                           "Max("+Table.AREAS +".ymax) AS ymax\n"+
                    "FROM ("+Table.IMAGES+" INNER JOIN "+Table.GROUPS+" ON "+Table.IMAGES+".groupe="+Table.GROUPS+".ID) "+
                                           "INNER JOIN "+Table.AREAS +" ON "+Table.IMAGES+".area="  +Table.AREAS +".ID\n"+
                    "WHERE ("+Table.GROUPS+".visible="+Table.TRUE+')';

    /**
     * Liste des propriétées par défaut. Les valeurs aux index pairs sont les index
     * des propriétées. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propriété "Images" donne l'instruction SQL à utiliser pour interroger la
     * table d'images.
     */
    private static final String[] DEFAULT_PROPERTIES=
    {
        "IMAGE_COUNT",  SeriesTableImpl.SQL_COUNT,
        "SERIES_TREE",  SeriesTableImpl.SQL_TREE,
        "SERIES_BY_ID", SeriesTableImpl.SQL_SELECT_BY_ID,
        Table.SERIES,   SeriesTableImpl.SQL_SELECT,
        Table.IMAGES,    ImageTableImpl.SQL_SELECT,
        Table.FORMATS,      FormatTable.SQL_SELECT,
        Table.BANDS,          BandTable.SQL_SELECT,
        Table.CATEGORIES, CategoryTable.SQL_SELECT,
        Table.AREAS,                    SQL_AREA,
    };

    /**
     * Liste des noms descriptifs à donner aux propriétés.
     * Ces noms sont identifiés par des clés de ressources.
     * Ces clés doivent apparaîtrent dans le même ordre que
     * les éléments du tableau {@link #DEFAULT_PROPERTIES}.
     */
    private static final int[] PROPERTY_NAMES=
    {
        Clé.SQL_IMAGE_COUNT,
        Clé.SQL_SERIES_TREE,
        Clé.SQL_SERIES_BY_ID,
        Clé.SQL_SERIES,
        Clé.SQL_IMAGES,
        Clé.SQL_FORMAT,
        Clé.SQL_BANDS,
        Clé.SQL_CATEGORIES,
        Clé.SQL_AREA
    };

    /**
     * Date de début des images de la base de données. Cette information
     * sera recherchée que la première fois ou elle sera demandée.
     */
    private transient long startTime;

    /**
     * Date de fin des images de la base de données. Cette information
     * sera recherchée que la première fois ou elle sera demandée.
     */
    private transient long endTime;

    /**
     * Longitude minimale des images de la base de données. Cette information
     * sera recherchée que la première fois ou elle sera demandée.
     */
    private transient float xmin;

    /**
     * Latitude minimale des images de la base de données. Cette information
     * sera recherchée que la première fois ou elle sera demandée.
     */
    private transient float ymin;

    /**
     * Longitude maximale des images de la base de données. Cette information
     * sera recherchée que la première fois ou elle sera demandée.
     */
    private transient float xmax;

    /**
     * Latitude maximale des images de la base de données. Cette information
     * sera recherchée que la première fois ou elle sera demandée.
     */
    private transient float ymax;

    /**
     * Indique si les coordonnées géographiques
     * et la plage de temps sont valides.
     */
    private transient boolean areaValid;

    /**
     * Séries d'images à proposer par défaut. On tentera de déterminer cette série
     * une fois pour toute la première fois que {@link #getImageTable()} sera appelée.
     */
    private transient SeriesEntry series;

    /**
     * Retourne l'URL par défaut de la base de données d'images.
     * Cet URL sera puisé dans les préférences de l'utilisateur
     * autant que possible.
     */
    private static String getDefaultURL()
    {
        Table.logger.log(loadDriver(Table.getPreference(DRIVER)));
        return Table.getPreference(SOURCE);
    }

    /**
     * Ouvre une connection avec une base de données par défaut. Le nom de la base de
     * données ainsi que le pilote à utiliser   seront puisés dans les préférences du
     * système.
     *
     * @throws SQLException Si on n'a pas pu se connecter
     *         à la base de données.
     */
    public ImageDataBase() throws SQLException
    {super(getDefaultURL(), TimeZone.getTimeZone(Table.getPreference(TIMEZONE)));}

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
    public ImageDataBase(final String url, final TimeZone timezone) throws SQLException
    {super(url, timezone);}

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
    public ImageDataBase(final String url, final TimeZone timezone, final String user, final String password) throws SQLException
    {super(url, timezone, user, password);}

    /**
     * Vérifie que la région est valide, en puisant les
     * données dans la base de données si nécessaire.
     */
    private void ensureXYTValid() throws SQLException
    {
        if (!areaValid)
        {
            final Statement statement=connection.createStatement();
            final ResultSet result=statement.executeQuery(Table.preferences.get(Table.AREAS, SQL_AREA));
            if (result.next())
            {
                boolean wasNull=false;
                final Calendar      calendar = new GregorianCalendar(timezone);
                final Calendar localCalendar = new GregorianCalendar();
                final Date         startTime = Table.getTimestamp(1, result, calendar, localCalendar); wasNull |= (startTime==null);
                final Date           endTime = Table.getTimestamp(2, result, calendar, localCalendar); wasNull |= (  endTime==null);
                xmin=result.getFloat(3); wasNull |= result.wasNull();
                ymin=result.getFloat(4); wasNull |= result.wasNull();
                xmax=result.getFloat(5); wasNull |= result.wasNull();
                ymax=result.getFloat(6); wasNull |= result.wasNull();
                if (!wasNull)
                {
                    this.startTime = startTime.getTime();
                    this.  endTime =   endTime.getTime();
                    areaValid=true;
                }
            }
            statement.close();
        }
    }

    /**
     * Retourne les coordonnées géographiques couvertes par les images de la base
     * de données. Les longitudes et latitudes minimales et maximales seront lues
     * dans la base de données.
     *
     * @return Coordonnées géographiques (en degrés de longitude et de latitude)
     *         couverte par les images, où <code>null</code> si la base de données
     *         ne contient pas d'images.
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     */
    public synchronized Rectangle2D getGeographicArea() throws SQLException
    {
        ensureXYTValid();
        return (areaValid) ? new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin) : null;
    }

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de données. Cette plage sera délimitée par des
     * objets {@link Date}.
     *
     * @throws SQLException si la base de données n'a pas pu être interrogée.
     */
    public synchronized Range getTimeRange() throws SQLException
    {
        ensureXYTValid();
        return (areaValid) ? new Range(Date.class, new Date(startTime), new Date(endTime)) : null;
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Series" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link SeriesTable#close}.
     *
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public SeriesTable getSeriesTable() throws SQLException
    {return new SeriesTableImpl(connection);}

    /**
     * Construit et retourne un objet qui interrogera la table "Images" de
     * la base de données d'images. Cette table fera ses recherches dans une
     * série par défaut. Il faudra appeler {@link ImageTable#setSeries} pour
     * spécifier une autre série. Lorsque cette table n'est plus nécessaire,
     * il faudra appeler {@link ImageTable#close}.
     *
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public synchronized ImageTable getImageTable() throws SQLException
    {
        if (series==null)
        {
            int maxImageCount = Integer.MIN_VALUE;
            final SeriesTable table = getSeriesTable();
            for (final Iterator<SeriesEntry> it=table.getSeries().iterator(); it.hasNext();)
            {
                final SeriesEntry entry = it.next();
                final int imageCount = table.getImageCount(entry);
                if (imageCount >= maxImageCount)
                {
                    this.series   = entry;
                    maxImageCount = imageCount;
                }
            }
            table.close();
            if (series==null)
            {
                throw new IllegalRecordException(Table.SERIES, Resources.format(Clé.NO_SERIES));
            }
        }
        return getImageTable(series);
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Images" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link ImageTable#close}.
     *
     * @param  series Référence vers la série d'images.
     * @throws SQLException si la référence n'est pas valide
     *         ou table n'a pas pu être construite.
     */
    public synchronized ImageTable getImageTable(final SeriesEntry series) throws SQLException
    {
        final Rectangle2D geographicArea;
        final Date startTime, endTime;
        ensureXYTValid();
        if (areaValid)
        {
            geographicArea = new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin);
            startTime      = new Date(this.startTime);
            endTime        = new Date(this.  endTime);
        }
        else
        {
            geographicArea = new Rectangle2D.Double(-180, -90, 180, 360);
            startTime      = new Date(0);
            endTime        = new Date();
        }
        final ImageTable table = new ImageTableImpl(connection, timezone);
        table.setGeographicArea(geographicArea);
        table.setTimeRange     (startTime, endTime);
        table.setSeries        (series);
        return table;
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Images" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link ImageTable#close}.
     *
     * @param  series Nom de la série d'images.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public ImageTable getImageTable(final String series) throws SQLException
    {
        final SeriesTable table = getSeriesTable();
        final SeriesEntry entry = table.getSeries(series);
        table.close();
        if (entry!=null) return getImageTable(entry);
        else throw new SQLException(Resources.format(Clé.SERIES_NOT_FOUND¤1, series));
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Images" de la base de données d'images.  Lorsque cette
     * table n'est plus nécessaire, il faudra appeler
     * {@link ImageTable#close}.
     *
     * @param  series Numéro de la série d'images.
     * @throws SQLException si la table n'a pas pu être construite.
     */
    public ImageTable getImageTable(final int seriesID) throws SQLException
    {
        final SeriesTable table = getSeriesTable();
        final SeriesEntry entry = table.getSeries(seriesID);
        table.close();
        if (entry!=null) return getImageTable(entry);
        else throw new SQLException(Resources.format(Clé.SERIES_NOT_FOUND¤1, new Integer(seriesID)));
    }

    /**
     * Retourne le répertoire racine à partir d'où construire le
     * chemin des images. Les chemins relatifs spécifiés dans la
     * base de données seront ajoutés à ce répertoire racine.
     *
     * @return Répertoire racine des images.
     */
    public static File getDefaultDirectory()
    {return (Table.directory!=null) ? Table.directory : new File(".");}

    /**
     * Définit le répertoire racine à partir duquel puiser les images.
     * L'appel de cette méthode affecte toutes les {@link ImageTable}
     * qui utilisent le répertoire racine par défaut, pour la session
     * de travail courante ainsi que pour toutes les prochaines sessions
     * (le répertoire sera sauvegardé dans les préférences systèmes).
     *
     * @param directory Répertoire racine des images.
     */
    public static void setDefaultDirectory(final File directory)
    {
        Table.directory=directory;
        if (directory!=null)
            Table.preferences.put(Table.DIRECTORY, directory.getPath());
        else
            Table.preferences.remove(Table.DIRECTORY);
    }

    /**
     * Retourne la liste des répertoires synchronisées. Voyez
     * {@link #setSynchronizedDirectories} pour une description
     * plus détaillée.
     */
    public static File[] getSynchronizedDirectories()
    {
        final File[] directories=new File[FormatEntry.synchronizedDirectories.length];
        System.arraycopy(FormatEntry.synchronizedDirectories, 0, directories, 0, directories.length);
        return directories;
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
    public static void setSynchronizedDirectories(final File[] directories) throws IOException
    {
        final File[] dir=new File[directories.length];
        for (int i=0; i<dir.length; i++)
            dir[i]=directories[i].getCanonicalFile();
        FormatEntry.synchronizedDirectories = dir;
    }

    /**
     * Construit et retourne un panneau qui permet à l'utilisateur de modifier
     * les instructions SQL. Les instructions modifiées seront conservées dans
     * les préférences systèmes et utilisées pour interroger les tables de la
     * base de données d'images.
     */
    public static SQLEditor getSQLEditor()
    {
        assert(2*PROPERTY_NAMES.length == DEFAULT_PROPERTIES.length);
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor=new SQLEditor(Table.preferences, resources.getString(Clé.EDIT_SQL_IMAGES_OR_FISHERIES¤1, new Integer(0)), Table.logger)
        {
            public String getProperty(final String name)
            {return Table.getPreference(name);}
        };
        for (int i=0; i<PROPERTY_NAMES.length; i++)
        {
            editor.addSQL(resources.getString(PROPERTY_NAMES[i]), DEFAULT_PROPERTIES[i*2+1], DEFAULT_PROPERTIES[i*2]);
        }
        return editor;
    }

    /**
     * Affiche des enregistrements de la base de données ou configure les requêtes SQL.
     * Cette méthode peut être exécutée à partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.sql.image.ImageDataBase <var>options</var>
     * </pre></blockquote>
     *
     * Lorsque cette classe est exécutée avec l'argument <code>-config</code>, elle
     * fait apparaître une boite de dialogue  permettant de configurer les requêtes
     * SQL utilisées par la base de données. Les requêtes modifiées seront sauvegardées
     * dans les préférences de l'utilisateur. Lorsque des arguments sont spécifiés,
     * ils sont interprétés comme suit:
     *
     * <blockquote><pre>
     *  <b>-help</b> <i></i>           Affiche cette liste des options
     *  <b>-series</b> <i></i>         Affiche l'arborescence des séries
     *  <b>-groups</b> <i></i>         Affiche l'arborescence des groupes (incluant les séries)
     *  <b>-formats</b> <i></i>        Affiche la table des formats
     *  <b>-config</b> <i></i>         Configure la base de données (interface graphique)
     *  <b>-browse</b> <i></i>         Affiche le contenu de toute la base de données (interface graphique)
     *  <b>-source</b> <i>name</i>     Source des données                (exemple: "jdbc:odbc:SEAS-Images")
     *  <b>-driver</b> <i>name</i>     Pilote de la base de données      (exemple: "sun.jdbc.odbc.JdbcOdbcDriver")
     *  <b>-locale</b> <i>name</i>     Langue et conventions d'affichage (exemple: "fr_CA")
     *  <b>-encoding</b> <i>name</i>   Page de code pour les sorties     (exemple: "cp850")
     *  <b>-output</b> <i>filename</i> Fichier de destination (le périphérique standard par défaut)
     * </pre></blockquote>
     *
     * L'argument <code>-cp</code> est surtout utile lorsque cette méthode est lancée
     * à partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la même page
     * de code que le reste du système Windows. Il est alors nécessaire de préciser la
     * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
     * caractères étendus. La page de code en cours peut être obtenu en tappant
     * <code>chcp</code> sur la ligne de commande.
     *
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public static void main(final String[] args) throws SQLException
    {
        final Main console = new Main(args);
        if (console.config)
        {
            getSQLEditor().showDialog(null);
            System.exit(0);
        }
        console.run();
    }
}
