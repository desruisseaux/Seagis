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
package fr.ird.sql.image;

// Base de donn�es
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import fr.ird.sql.SQLEditor;
import fr.ird.sql.DataBase;

// Entr�s/sorties
import java.io.File;
import java.io.IOException;

// Coordonn�es spatio-temporelles
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
 * Connection avec la base de donn�es d'images.
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
     * Requ�te SQL utilis� pour obtenir les coordonn�es
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
     * Liste des propri�t�es par d�faut. Les valeurs aux index pairs sont les index
     * des propri�t�es. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propri�t� "Images" donne l'instruction SQL � utiliser pour interroger la
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
     * Liste des noms descriptifs � donner aux propri�t�s.
     * Ces noms sont identifi�s par des cl�s de ressources.
     * Ces cl�s doivent appara�trent dans le m�me ordre que
     * les �l�ments du tableau {@link #DEFAULT_PROPERTIES}.
     */
    private static final int[] PROPERTY_NAMES=
    {
        Cl�.SQL_IMAGE_COUNT,
        Cl�.SQL_SERIES_TREE,
        Cl�.SQL_SERIES_BY_ID,
        Cl�.SQL_SERIES,
        Cl�.SQL_IMAGES,
        Cl�.SQL_FORMAT,
        Cl�.SQL_BANDS,
        Cl�.SQL_CATEGORIES,
        Cl�.SQL_AREA
    };

    /**
     * Date de d�but des images de la base de donn�es. Cette information
     * sera recherch�e que la premi�re fois ou elle sera demand�e.
     */
    private transient long startTime;

    /**
     * Date de fin des images de la base de donn�es. Cette information
     * sera recherch�e que la premi�re fois ou elle sera demand�e.
     */
    private transient long endTime;

    /**
     * Longitude minimale des images de la base de donn�es. Cette information
     * sera recherch�e que la premi�re fois ou elle sera demand�e.
     */
    private transient float xmin;

    /**
     * Latitude minimale des images de la base de donn�es. Cette information
     * sera recherch�e que la premi�re fois ou elle sera demand�e.
     */
    private transient float ymin;

    /**
     * Longitude maximale des images de la base de donn�es. Cette information
     * sera recherch�e que la premi�re fois ou elle sera demand�e.
     */
    private transient float xmax;

    /**
     * Latitude maximale des images de la base de donn�es. Cette information
     * sera recherch�e que la premi�re fois ou elle sera demand�e.
     */
    private transient float ymax;

    /**
     * Indique si les coordonn�es g�ographiques
     * et la plage de temps sont valides.
     */
    private transient boolean areaValid;

    /**
     * S�ries d'images � proposer par d�faut. On tentera de d�terminer cette s�rie
     * une fois pour toute la premi�re fois que {@link #getImageTable()} sera appel�e.
     */
    private transient SeriesEntry series;

    /**
     * Retourne l'URL par d�faut de la base de donn�es d'images.
     * Cet URL sera puis� dans les pr�f�rences de l'utilisateur
     * autant que possible.
     */
    private static String getDefaultURL()
    {
        Table.logger.log(loadDriver(Table.getPreference(DRIVER)));
        return Table.getPreference(SOURCE);
    }

    /**
     * Ouvre une connection avec une base de donn�es par d�faut. Le nom de la base de
     * donn�es ainsi que le pilote � utiliser   seront puis�s dans les pr�f�rences du
     * syst�me.
     *
     * @throws SQLException Si on n'a pas pu se connecter
     *         � la base de donn�es.
     */
    public ImageDataBase() throws SQLException
    {super(getDefaultURL(), TimeZone.getTimeZone(Table.getPreference(TIMEZONE)));}

    /**
     * Ouvre une connection avec la base de donn�es des images.
     *
     * @param  url Protocole et nom de la base de donn�es d'images.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @throws SQLException Si on n'a pas pu se connecter
     *         � la base de donn�es.
     */
    public ImageDataBase(final String url, final TimeZone timezone) throws SQLException
    {super(url, timezone);}

    /**
     * Ouvre une connection avec la base de donn�es des images.
     *
     * @param  url Protocole et nom de la base de donn�es d'images.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  user Nom d'utilisateur de la base de donn�es.
     * @param  password Mot de passe.
     * @throws SQLException Si on n'a pas pu se connecter
     *         � la base de donn�es.
     */
    public ImageDataBase(final String url, final TimeZone timezone, final String user, final String password) throws SQLException
    {super(url, timezone, user, password);}

    /**
     * V�rifie que la r�gion est valide, en puisant les
     * donn�es dans la base de donn�es si n�cessaire.
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
     * Retourne les coordonn�es g�ographiques couvertes par les images de la base
     * de donn�es. Les longitudes et latitudes minimales et maximales seront lues
     * dans la base de donn�es.
     *
     * @return Coordonn�es g�ographiques (en degr�s de longitude et de latitude)
     *         couverte par les images, o� <code>null</code> si la base de donn�es
     *         ne contient pas d'images.
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     */
    public synchronized Rectangle2D getGeographicArea() throws SQLException
    {
        ensureXYTValid();
        return (areaValid) ? new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin) : null;
    }

    /**
     * Retourne la plage de dates couvertes par les images de
     * la base de donn�es. Cette plage sera d�limit�e par des
     * objets {@link Date}.
     *
     * @throws SQLException si la base de donn�es n'a pas pu �tre interrog�e.
     */
    public synchronized Range getTimeRange() throws SQLException
    {
        ensureXYTValid();
        return (areaValid) ? new Range(Date.class, new Date(startTime), new Date(endTime)) : null;
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Series" de la base de donn�es d'images.  Lorsque cette
     * table n'est plus n�cessaire, il faudra appeler
     * {@link SeriesTable#close}.
     *
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public SeriesTable getSeriesTable() throws SQLException
    {return new SeriesTableImpl(connection);}

    /**
     * Construit et retourne un objet qui interrogera la table "Images" de
     * la base de donn�es d'images. Cette table fera ses recherches dans une
     * s�rie par d�faut. Il faudra appeler {@link ImageTable#setSeries} pour
     * sp�cifier une autre s�rie. Lorsque cette table n'est plus n�cessaire,
     * il faudra appeler {@link ImageTable#close}.
     *
     * @throws SQLException si la table n'a pas pu �tre construite.
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
                throw new IllegalRecordException(Table.SERIES, Resources.format(Cl�.NO_SERIES));
            }
        }
        return getImageTable(series);
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Images" de la base de donn�es d'images.  Lorsque cette
     * table n'est plus n�cessaire, il faudra appeler
     * {@link ImageTable#close}.
     *
     * @param  series R�f�rence vers la s�rie d'images.
     * @throws SQLException si la r�f�rence n'est pas valide
     *         ou table n'a pas pu �tre construite.
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
     * "Images" de la base de donn�es d'images.  Lorsque cette
     * table n'est plus n�cessaire, il faudra appeler
     * {@link ImageTable#close}.
     *
     * @param  series Nom de la s�rie d'images.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public ImageTable getImageTable(final String series) throws SQLException
    {
        final SeriesTable table = getSeriesTable();
        final SeriesEntry entry = table.getSeries(series);
        table.close();
        if (entry!=null) return getImageTable(entry);
        else throw new SQLException(Resources.format(Cl�.SERIES_NOT_FOUND�1, series));
    }

    /**
     * Construit et retourne un objet qui interrogera la table
     * "Images" de la base de donn�es d'images.  Lorsque cette
     * table n'est plus n�cessaire, il faudra appeler
     * {@link ImageTable#close}.
     *
     * @param  series Num�ro de la s�rie d'images.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public ImageTable getImageTable(final int seriesID) throws SQLException
    {
        final SeriesTable table = getSeriesTable();
        final SeriesEntry entry = table.getSeries(seriesID);
        table.close();
        if (entry!=null) return getImageTable(entry);
        else throw new SQLException(Resources.format(Cl�.SERIES_NOT_FOUND�1, new Integer(seriesID)));
    }

    /**
     * Retourne le r�pertoire racine � partir d'o� construire le
     * chemin des images. Les chemins relatifs sp�cifi�s dans la
     * base de donn�es seront ajout�s � ce r�pertoire racine.
     *
     * @return R�pertoire racine des images.
     */
    public static File getDefaultDirectory()
    {return (Table.directory!=null) ? Table.directory : new File(".");}

    /**
     * D�finit le r�pertoire racine � partir duquel puiser les images.
     * L'appel de cette m�thode affecte toutes les {@link ImageTable}
     * qui utilisent le r�pertoire racine par d�faut, pour la session
     * de travail courante ainsi que pour toutes les prochaines sessions
     * (le r�pertoire sera sauvegard� dans les pr�f�rences syst�mes).
     *
     * @param directory R�pertoire racine des images.
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
     * Retourne la liste des r�pertoires synchronis�es. Voyez
     * {@link #setSynchronizedDirectories} pour une description
     * plus d�taill�e.
     */
    public static File[] getSynchronizedDirectories()
    {
        final File[] directories=new File[FormatEntry.synchronizedDirectories.length];
        System.arraycopy(FormatEntry.synchronizedDirectories, 0, directories, 0, directories.length);
        return directories;
    }

    /**
     * Sp�cifie les r�pertoires dans lesquels il faudra synchroniser les lectures d'images. Cette liste
     * sert � �viter qu'on ne lise en m�me temps deux images qui se trouve sur le m�me disque, afin de
     * r�duire les d�placements requis de la t�te du lecteur. Cette liste s'interpr�te comme suit:
     *
     * soit deux images qui ont les chemins complets
     *
     * "<code>/common/directory1/filename1</code>" et "<code>/common/directory2/filename2</code>".
     *
     * Ces deux images ont un r�pertoire parent commun: "<code>common</code>". Si ce r�pertoire
     * commun appara�t dans la liste, ou s'il est un sous-r�pertoire d'un r�pertoire apparaissant
     * dans la liste, alors les lectures de ces images seront synchronis�es de fa�on � �viter qu'elles
     * ne soient faites en m�me temps.
     *
     * <p>Sous Windows, cette liste contient par d�faut les lettres des lecteurs ("A:", "C:", "D:",
     * etc.). Ca signifie que deux images peuvent �tre lues en m�me temps si l'une est lue sur le
     * lecteur "C:" et l'autre sur le lecteur "D:", mais pas si elles sont toutes deux lues sur le
     * lecteur "C:". Sous Unix, cette liste contient par d�faut "/", ce qui signifie qu'aucune
     * image ne peut �tre lue en m�me temps. Sp�cifier une liste de r�pertoires plus pr�cise peut
     * donc am�liorer les performances, surtout sur Unix.</p>
     *
     * <p>La liste sp�cifi�e d�pend typiquement de la configuration mat�rielle de syst�me, telle que
     * la pr�sence physique de disques durs ou de lecteur de bandes. Cette configuration est la m�me
     * pour toute les base de donn�es sur une machine, mais peut varier d'une machine � l'autre.
     * C'est pourquoi cette liste est statique (s'applique � toute les bases de donn�es de la
     * machine virtuelle courante) plut�t que li�e � une base de donn�es particuli�re.</p>
     *
     * <p>Notez enfin que cette liste n'a d'impact que sur la performance. Que la liste soit
     * pr�cise ou pas ne devrait pas affecter les r�sultats obtenus.</p>
     */
    public static void setSynchronizedDirectories(final File[] directories) throws IOException
    {
        final File[] dir=new File[directories.length];
        for (int i=0; i<dir.length; i++)
            dir[i]=directories[i].getCanonicalFile();
        FormatEntry.synchronizedDirectories = dir;
    }

    /**
     * Construit et retourne un panneau qui permet � l'utilisateur de modifier
     * les instructions SQL. Les instructions modifi�es seront conserv�es dans
     * les pr�f�rences syst�mes et utilis�es pour interroger les tables de la
     * base de donn�es d'images.
     */
    public static SQLEditor getSQLEditor()
    {
        assert(2*PROPERTY_NAMES.length == DEFAULT_PROPERTIES.length);
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor=new SQLEditor(Table.preferences, resources.getString(Cl�.EDIT_SQL_IMAGES_OR_FISHERIES�1, new Integer(0)), Table.logger)
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
     * Affiche des enregistrements de la base de donn�es ou configure les requ�tes SQL.
     * Cette m�thode peut �tre ex�cut�e � partir de la ligne de commande:
     *
     * <blockquote><pre>
     * java fr.ird.sql.image.ImageDataBase <var>options</var>
     * </pre></blockquote>
     *
     * Lorsque cette classe est ex�cut�e avec l'argument <code>-config</code>, elle
     * fait appara�tre une boite de dialogue  permettant de configurer les requ�tes
     * SQL utilis�es par la base de donn�es. Les requ�tes modifi�es seront sauvegard�es
     * dans les pr�f�rences de l'utilisateur. Lorsque des arguments sont sp�cifi�s,
     * ils sont interpr�t�s comme suit:
     *
     * <blockquote><pre>
     *  <b>-help</b> <i></i>           Affiche cette liste des options
     *  <b>-series</b> <i></i>         Affiche l'arborescence des s�ries
     *  <b>-groups</b> <i></i>         Affiche l'arborescence des groupes (incluant les s�ries)
     *  <b>-formats</b> <i></i>        Affiche la table des formats
     *  <b>-config</b> <i></i>         Configure la base de donn�es (interface graphique)
     *  <b>-browse</b> <i></i>         Affiche le contenu de toute la base de donn�es (interface graphique)
     *  <b>-source</b> <i>name</i>     Source des donn�es                (exemple: "jdbc:odbc:SEAS-Images")
     *  <b>-driver</b> <i>name</i>     Pilote de la base de donn�es      (exemple: "sun.jdbc.odbc.JdbcOdbcDriver")
     *  <b>-locale</b> <i>name</i>     Langue et conventions d'affichage (exemple: "fr_CA")
     *  <b>-encoding</b> <i>name</i>   Page de code pour les sorties     (exemple: "cp850")
     *  <b>-output</b> <i>filename</i> Fichier de destination (le p�riph�rique standard par d�faut)
     * </pre></blockquote>
     *
     * L'argument <code>-cp</code> est surtout utile lorsque cette m�thode est lanc�e
     * � partir de la ligne de commande MS-DOS: ce dernier n'utilise pas la m�me page
     * de code que le reste du syst�me Windows. Il est alors n�cessaire de pr�ciser la
     * page de code (souvent 850 ou 437) si on veut obtenir un affichage correct des
     * caract�res �tendus. La page de code en cours peut �tre obtenu en tappant
     * <code>chcp</code> sur la ligne de commande.
     *
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
