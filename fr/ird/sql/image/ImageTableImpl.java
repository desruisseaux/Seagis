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
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Geotools dependencies (CTS)
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.HorizontalCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.TransformException;

// Geotools dependencies (GCS)
import org.geotools.gp.Operation;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.gp.OperationNotFoundException;

// Geotools dependencies (resources)
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.Utilities;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XRectangle2D;

// Géométrie
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;

// Temps
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;

// Collections
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Divers
import java.io.File;
import net.seas.plot.RangeSet;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import javax.media.jai.ParameterList;
import javax.media.jai.util.Range;


/**
 * Connection vers une table d'images. Cette table contient des références vers des images sous
 * forme d'objets {@link ImageEntry}.  Une table <code>ImageTable</code> est capable de fournir
 * la liste des entrés {@link ImageEntry} qui interceptent une certaines région géographique et
 * une certaine plage de dates.
 *
 * @see ImageDataBase#getImageTable
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageTableImpl extends Table implements ImageTable
{
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des images.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes [@link #SERIES}, [@link #FILENAME} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID         */ IMAGES+".ID, "          +
                                /*[02] SERIES     */ GROUPS+".series, "      +
                                /*[03] PATHNAME   */ GROUPS+".pathname, "    +
                                /*[04] FILENAME   */ IMAGES+".filename, "    +
                                /*[05] START_TIME */ IMAGES+".start_time, "  +
                                /*[06] END_TIME   */ IMAGES+".end_time, "    +
                                /*[07] ELLIPSOID  */  AREAS+".ellipsoid, "   +
                                /*[08] XMIN       */  AREAS+".xmin, "        +
                                /*[09] XMAX       */  AREAS+".xmax, "        +
                                /*[10] YMIN       */  AREAS+".ymin, "        +
                                /*[11] YMAX       */  AREAS+".ymax, "        +
                                /*[12] WIDTH      */  AREAS+".width, "       +
                                /*[13] HEIGHT     */  AREAS+".height, "      +
                                /*[14] FORMAT     */ GROUPS+".format, "      +
                                /*[15] PERIOD     */ GROUPS+".period\n"      +

                    "FROM ("+IMAGES+" INNER JOIN "+AREAS+ " ON "+IMAGES+".area="  +AREAS+ ".ID)"+
                                    " INNER JOIN "+GROUPS+" ON "+IMAGES+".groupe="+GROUPS+".ID\n"+

                    "WHERE ("+GROUPS+".visible="+TRUE+") "+
                      "AND (xmax>? AND xmin<? AND ymax>? AND ymin<?) "+
                      "AND (((end_time Is Null) OR end_time>=?) AND ((start_time Is Null) OR start_time<=?)) "+
                      "AND (series=?)\n"+

                      "ORDER BY end_time, groupe"; // DOIT être en ordre chronologique. Voir {@link ImageEntryImpl#compare}.

    /** Numéro de colonne. */ static final int ID         =  1;
    /** Numéro de colonne. */ static final int SERIES     =  2;
    /** Numéro de colonne. */ static final int PATHNAME   =  3;
    /** Numéro de colonne. */ static final int FILENAME   =  4;
    /** Numéro de colonne. */ static final int START_TIME =  5;
    /** Numéro de colonne. */ static final int END_TIME   =  6;
    /** Numéro de colonne. */ static final int ELLIPSOID  =  7;
    /** Numéro de colonne. */ static final int XMIN       =  8;
    /** Numéro de colonne. */ static final int XMAX       =  9;
    /** Numéro de colonne. */ static final int YMIN       = 10;
    /** Numéro de colonne. */ static final int YMAX       = 11;
    /** Numéro de colonne. */ static final int WIDTH      = 12;
    /** Numéro de colonne. */ static final int HEIGHT     = 13;
    /** Numéro de colonne. */ static final int FORMAT     = 14;
    /** Numéro de colonne. */ static final int PERIOD     = 15;

    /** Numéro d'argument. */ private static final int ARG_XMIN       = 1;
    /** Numéro d'argument. */ private static final int ARG_XMAX       = 2;
    /** Numéro d'argument. */ private static final int ARG_YMIN       = 3;
    /** Numéro d'argument. */ private static final int ARG_YMAX       = 4;
    /** Numéro d'argument. */ private static final int ARG_START_TIME = 5;
    /** Numéro d'argument. */ private static final int ARG_END_TIME   = 6;
    /** Numéro d'argument. */ private static final int ARG_SERIES     = 7;

    /**
     * Nombre de millisecondes entre le 01/01/1970 00:00 UTC et le 01/01/1950 00:00 UTC.
     * Le 1er janvier 1970 est l'epoch du Java, tandis que le 1er janvier 1950 est celui
     * de la Nasa (son jour julier "0"). La constante <code>EPOCH</code> sert à faire les
     * conversions d'un système à l'autre.
     */
    private static final long EPOCH = -631152000000L; // Pour 1958, utiliser -378691200000L;

    /**
     * Nombre de millisecondes dans une journée.
     */
    private static final double DAY = 24*60*60*1000;

    /**
     * Convertit un jour julien en date.
     */
    static Date toDate(final double t)
    {return new Date(Math.round(t*DAY)+EPOCH);}

    /**
     * Convertit une date en nombre de jours écoulés depuis le 1er janvier 1950.
     * Les valeurs <code>[MIN/MAX]_VALUE</code> sont converties en infinies.
     */
    static double toJulian(final long time)
    {
        if (time==Long.MIN_VALUE) return Double.NEGATIVE_INFINITY;
        if (time==Long.MAX_VALUE) return Double.POSITIVE_INFINITY;
        return (time-EPOCH)/DAY;
    }

    /**
     * Système de coordonnées par défaut.  Les coordonnées sont dans l'ordre
     * la longitude et la latitude (selon l'ellipsoïde WGS 1984)  et la date
     * en jours julien depuis le 1er janvier 1950 à 00:00 UTC.
     */
    private static final CompoundCoordinateSystem coordinateSystem =
                         new CompoundCoordinateSystem("SEAS",
                             GeographicCoordinateSystem.WGS84,
                             new TemporalCoordinateSystem("Aviso", new Date(EPOCH)));

    /**
     * Réference vers la série d'images. Cette référence
     * est construite à partir du champ ID dans la table
     * "Series" de la base de données.
     */
    private SeriesEntry series;

    /**
     * L'opération à appliquer sur les images lue,
     * ou <code>null</code> s'il n'y en a aucune.
     */
    private Operation operation;

    /**
     * Paramètres de l'opération, ou <code>null</code>
     * s'il n'y a pas d'opération.
     */
    private ParameterList opParam;

    /**
     * Dimension logique (en degrés de longitude et de latitude) désirée des pixels
     * de l'images. Cette information n'est qu'approximative. Il n'est pas garantie
     * que les lectures produiront effectivement des images de cette résolution.
     * Une valeur nulle signifie que les lectures doivent se faire avec la meilleure
     * résolution possible.
     */
    private Dimension2D resolution;

    /**
     * Coordonnées géographiques de la région d'intéret, en degrés de longitude et de latitude.
     * Ces coordonnées peuvent être spécifiées par un appel à {@link #setGeographicArea}.  Les
     * objets {@link Rectangle2D} affectés à <code>geographicArea</code> peuvent changer, mais
     * les coordonnées d'un objet {@link Rectangle2D} donné ne doivent PAS être changées.   La
     * classe <code>ImageEntryImpl</code> se réferera directement à ce champ afin d'éviter la
     * création d'une multitude de clones.
     */
    private Rectangle2D geographicArea;

    /**
     * Date du début de la plage de temps des images
     * recherchées par cet objet <code>ImageTable</code>.
     */
    private long startTime;

    /**
     * Date du fin de la plage de temps des images
     * recherchées par cet objet <code>ImageTable</code>.
     */
    private long endTime;

    /**
     * Calendrier utilisé pour préparer les dates. Ce calendrier
     * utilisera le fuseau horaire spécifié lors de la construction.
     */
    private final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Fuseau horaire de la base de données.
     */
    public final TimeZone timezone;

    /**
     * Formatteur à utiliser pour écrire des dates pour l'utilisateur. Les caractères et
     * les conventions linguistiques dépendront de la langue de l'utilisateur. Toutefois,
     * le fuseau horaire devrait être celui de la région d'étude plutôt que celui du pays
     * de l'utilisateur.
     */
    private final DateFormat dateFormat;

    /**
     * Table des formats. Cette table ne sera construite que la première fois
     * où elle sera nécessaire.  Elle sera ensuite fermée chaque fois qu'elle
     * n'est plus utilisée pour économiser des ressources.
     */
    private transient FormatTable formatTable;

    /**
     * Ensemble des formats déjà lue. Autant que possible,
     * on réutilisera les formats qui ont déjà été créés.
     */
    private final Map<Integer,FormatEntryImpl> formats=new HashMap<Integer,FormatEntryImpl>();

    /**
     * Requète SQL faisant le lien avec la base de données.
     */
    private final PreparedStatement statement;

    /**
     * Table d'images pouvant être récupérée par leur numéro ID.
     * Cette table ne sera construite que si elle est nécessaire.
     */
    private transient PreparedStatement imageByID;

    /**
     * Table d'images pouvant être récupérée par leur nom.
     * Cette table ne sera construite que si elle est nécessaire.
     */
    private transient PreparedStatement imageByName;

    /**
     * Derniers paramètres à avoir été construit. Ces paramètres sont
     * retenus afin d'éviter d'avoir à les reconstruires trop souvent
     * si c'est évitable.
     */
    private transient Parameters parameters;

    /**
     * Construit une table des images en utilisant la connection spécifiée.
     * L'appellant <strong>doit</strong> appeler {@link #setSeries},
     * {@link #setGeographicArea} et {@link #setTimeRange} avant d'utiliser
     * cette table.
     *
     * @param  connection Connection vers une base de données d'images.
     * @param  timezone   Fuseau horaire des dates inscrites dans la base
     *                    de données. Cette information est utilisée pour
     *                    convertir en heure UTC les dates écrites dans la
     *                    base de données.
     * @throws SQLException si <code>ImageTable</code> n'a pas pu construire sa requête SQL.
     */
    ImageTableImpl(final Connection connection, final TimeZone timezone) throws SQLException
    {
        statement = connection.prepareStatement(preferences.get(IMAGES, SQL_SELECT));
        this.timezone   = timezone;
        this.calendar   = new GregorianCalendar(timezone);
        this.dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        this.dateFormat.setCalendar(calendar);
    }

    /**
     * Retourne la référence vers la séries d'images.
     */
    public SeriesEntry getSeries()
    {return series;}

    /**
     * Définit la série dont on veut les images.
     *
     * @param  series       Réference vers la série d'images. Cette référence
     *                      est construite à partir du champ ID dans la table
     *                      "Series" de la base de données.
     * @throws SQLException si une erreur est survenu lors de l'accès à la
     *                      base de données, ou si <code>series</code> ne
     *                      se réfère pas à un enregistrement de la table
     *                      des séries.
     */
    public synchronized void setSeries(final SeriesEntry series) throws SQLException
    {
        if (!series.equals(this.series))
        {
            parameters = null;
            statement.setInt(ARG_SERIES, series.getID());
            this.series=series;

            log("setSeries", Level.CONFIG, ResourceKeys.SET_SERIES_$1, series.getName());
        }
    }

    /**
     * Retourne le système de coordonnées utilisé pour les coordonnées spatio-temporelles de
     * <code>[get/set]Envelope(...)</code>. En général, ce système de coordonnées aura trois
     * dimensions (la dernière dimension étant le temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Les latitudes,  en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     */
    public final CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Retourne les coordonnées spatio-temporelles de la région d'intérêt. Le système
     * de coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}.
     */
    public synchronized Envelope getEnvelope()
    {
        final Envelope envelope = new Envelope(3);
        envelope.setRange(0, geographicArea.getMinX(), geographicArea.getMaxX());
        envelope.setRange(1, geographicArea.getMinY(), geographicArea.getMaxY());
        envelope.setRange(2, toJulian(startTime),      toJulian(endTime));
        return envelope;
    }

    /**
     * Définit les coordonnées spatio-temporelles de la région d'intérêt. Le système de
     * coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}. Appeler
     * cette méthode équivaut à effectuer les transformations nécessaires des coordonnées
     * et à appeler {@link #setTimeRange} et {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized void setEnvelope(final Envelope envelope) throws SQLException
    {
        // No coordinate transformation needed for this implementation.
        setGeographicArea(new Rectangle2D.Double(envelope.getMinimum(0), envelope.getMinimum(1),
                                                 envelope.getLength (0), envelope.getLength (1)));
        setTimeRange(toDate(envelope.getMinimum(2)), toDate(envelope.getMaximum(2)));
    }

    /**
     * Retourne la période de temps d'intérêt.  Cette plage sera délimitée par des objets
     * {@link Date}. Appeler cette méthode équivant à n'extraire que la partie temporelle
     * de {@link #getEnvelope} et à transformer les coordonnées si nécessaire.
     */
    public synchronized Range getTimeRange()
    {return new Range(Date.class, new Date(startTime), new Date(endTime));}

    /**
     * Définit la période de temps d'intérêt (dans laquelle rechercher des images).
     * Cette méthode ne change que la partie temporelle de l'enveloppe recherchée
     * (voir {@link #getEnvelope}).
     *
     * @param  range Période d'intérêt dans laquelle rechercher des images.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public void setTimeRange(final Range range) throws SQLException
    {
        Date startTime = (Date) range.getMinValue();
        Date   endTime = (Date) range.getMaxValue();
        if (!range.isMinIncluded())
            startTime = new Date(startTime.getTime()+1);
        if (!range.isMaxIncluded())
            endTime = new Date(endTime.getTime()-1);
        setTimeRange(startTime, endTime);
    }

    /**
     * Définit la période de temps d'intérêt (dans laquelle rechercher des images).
     * Cette méthode ne change que la partie temporelle de l'enveloppe recherchée
     * (voir {@link #getEnvelope}).
     *
     * @param  startTime Date du  début de la plage de temps, inclusive.
     * @param  endTime   Date de la fin de la plage de temps, inclusive.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized void setTimeRange(final Date startTime, final Date endTime) throws SQLException
    {
        final long newStartTime = startTime.getTime();
        final long newEndTime   =   endTime.getTime();
        if (newStartTime!=this.startTime || newEndTime!=this.endTime)
        {
            parameters = null;
            final Timestamp time=new Timestamp(newStartTime);
            statement.setTimestamp(ARG_START_TIME, time, calendar);
            time.setTime(newEndTime);
            statement.setTimestamp(ARG_END_TIME, time, calendar);
            this.startTime = newStartTime;
            this.endTime   = newEndTime;

            final String startText = dateFormat.format(startTime);
            final String   endText = dateFormat.format(  endTime);
            log("setTimeRange", Level.CONFIG, ResourceKeys.SET_TIME_RANGE_$2, new String[]{startText, endText});
        }
    }

    /**
     * Retourne les coordonnées géographiques de la région d'intérêt.
     * Les coordonnées seront exprimées en degrés de longitudes et de latitudes
     * selon l'ellipsoïde WGS 1984. Appeler cette méthode équivaut à n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et à transformer les
     * coordonnées si nécessaire.
     */
    public synchronized Rectangle2D getGeographicArea()
    {return (Rectangle2D) geographicArea.clone();}

    /**
     * Définit les coordonnées géographiques de la région d'intérêt   (dans laquelle rechercher des
     * images). Ces coordonnées sont toujours exprimées en degrés de longitude et de latitude selon
     * l'ellipsoïde WGS 1984. Cette méthode ne change que la partie horizontale de l'enveloppe (voir
     * {@link #setEnvelope}).
     *
     * @param  rect Coordonnées géographiques de la région, selon l'ellipsoïde WGS 1984.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized void setGeographicArea(final Rectangle2D rect) throws SQLException
    {
        if (!rect.equals(geographicArea))
        {
            parameters = null;
            statement.setDouble(ARG_XMIN, rect.getMinX());
            statement.setDouble(ARG_XMAX, rect.getMaxX());
            statement.setDouble(ARG_YMIN, rect.getMinY());
            statement.setDouble(ARG_YMAX, rect.getMaxY());
            geographicArea = new XRectangle2D(rect);

            log("setGeographicArea", Level.CONFIG, ResourceKeys.SET_GEOGRAPHIC_AREA_$1, getStringArea());
        }
    }

    /**
     * Retourne la dimension désirée des pixels de l'images.
     *
     * @return Résolution préférée, ou <code>null</code> si la lecture
     *         doit se faire avec la meilleure résolution disponible.
     */
    public synchronized Dimension2D getPreferredResolution()
    {return (resolution!=null) ? (Dimension2D)resolution.clone() : null;}

    /**
     * Définit la dimension désirée des pixels de l'images.  Cette information n'est
     * qu'approximative. Il n'est pas garantie que la lecture produira effectivement
     * des images de cette résolution. Une valeur nulle signifie que la lecture doit
     * se faire avec la meilleure résolution disponible.
     *
     * @param  pixelSize Taille préférée des pixels. Les unités sont les mêmes
     *         que celles de {@link #setGeographicArea}.
     */
    public synchronized void setPreferredResolution(final Dimension2D pixelSize)
    {
        if (!Utilities.equals(resolution, pixelSize))
        {
            parameters = null;
            final int clé;
            final Object param;
            if (pixelSize!=null)
            {
                resolution = (Dimension2D)pixelSize.clone();
                clé = ResourceKeys.SET_RESOLUTION_$2;
                param = new Double[]{new Double(resolution.getWidth()),new Double(resolution.getHeight())};
            }
            else
            {
                resolution = null;
                clé = ResourceKeys.UNSET_RESOLUTION;
                param = null;
            }
            log("setPreferredResolution", Level.CONFIG, clé, param);
        }
    }

    /**
     * Retourne l'opération appliquée sur les images lues. L'opération retournée
     * peut représenter par exemple un gradient. Si aucune opération n'est appliquée
     * (c'est-à-dire si les images retournées représentent les données originales),
     * alors cette méthode retourne <code>null</code>.
     */
    public Operation getOperation()
    {return operation;}

    /**
     * Définit l'opération à appliquer sur les images lues. Si des paramètres doivent
     * être spécifiés  en plus de l'opération,   ils peuvent l'être en appliquant des
     * méthodes <code>setParameter</code> sur la référence retournée. Par exemple, la
     * ligne suivante transforme tous les pixels des images à lire en appliquant
     * l'équation linéaire <code>value*constant+offset</code>:
     *
     * <blockquote><pre>
     * setOperation("Rescale").setParameter("constants", new double[]{10})
     *                        .setParameter("offsets"  , new double[]{50]);
     * </pre></blockquote>
     *
     * @param  operation L'opération à appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune opération.
     * @return Liste de paramètres par défaut, ou <code>null</code> si <code>operation</code>
     *         était nul. Les modifications apportées sur cette liste de paramètres influenceront
     *         les images obtenues lors du prochain appel d'une méthode <code>getEntry</code>.
     */
    public synchronized ParameterList setOperation(final Operation operation)
    {
        this.parameters = null;
        this.operation=operation;
        final int clé;
        final String name;
        if (operation!=null)
        {
            opParam = operation.getParameterList();
            name    = operation.getName();
            clé     = ResourceKeys.SET_OPERATION_$1;
        }
        else
        {
            opParam = null;
            name    = null;
            clé     = ResourceKeys.UNSET_OPERATION;
        }
        log("setOperation", Level.CONFIG, clé, name);
        return opParam;
    }

    /**
     * Définit l'opération à appliquer sur les images lues. Cette méthode est équivalente à
     * <code>setOperation({@link GridImageProcessor#getOperation GridImageProcessor.getOperation}(name))</code>.
     *
     * @param  operation L'opération à appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune opération.
     * @return Liste de paramètres par défaut, ou <code>null</code> si <code>operation</code>
     *         était nul. Les modifications apportées sur cette liste de paramètres influenceront
     *         les images obtenues lors du prochain appel d'une méthode <code>getEntry</code>.
     * @throws OperationNotFoundException si l'opération <code>operation</code> n'a pas été trouvée.
     */
    public ParameterList setOperation(final String operation) throws OperationNotFoundException
    {return setOperation(operation!=null ? Parameters.PROCESSOR.getOperation(operation) : null);}

    /**
     * Retourne la liste des images disponibles dans la plage de coordonnées
     * spatio-temporelles préalablement sélectionnées. Ces plages auront été
     * spécifiées à l'aide des différentes méthodes <code>set...</code> de
     * cette classe.
     *
     * @return Liste d'images qui interceptent la plage de temps et la région géographique d'intérêt.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public List<ImageEntry> getEntries() throws SQLException
    {
        /*
         * On construit un tableau d'ImageEntry ET NON d'ImageEntryImpl
         * parce que certains utilisateurs (par exemple ImageTableModel)
         * voudront remplacer certains éléments de ce tableau sans que
         * ça ne lance un {@link java.lang.ArrayStoreException}.
         */
        final List<ImageEntry> entries = new ArrayList<ImageEntry>();
        getRanges(null, null, null, entries);
        return entries;
    }

    /**
     * Retourne une des images disponibles dans la plage de coordonnées spatio-temporelles
     * préalablement sélectionnées. Si plusieurs images interceptent la région et la plage
     * de temps   (c'est-à-dire si {@link #getEntries} retourne un tableau d'au moins deux
     * entrées), alors le choix de l'image se fera en utilisant un objet {@link ImageComparator}
     * par défaut. Ce choix peut être arbitraire.
     *
     * @return Une image choisie arbitrairement dans la région et la plage de date
     *         sélectionnées, ou <code>null</code> s'il n'y a pas d'image dans ces plages.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public ImageEntry getEntry() throws SQLException
    {
        ImageEntry best = null;
        final ImageComparator comparator=new ImageComparator(this);
        for (final Iterator<ImageEntry> it=getEntries().iterator(); it.hasNext();)
        {
            final ImageEntry entry = it.next();
            if (best==null || comparator.compare(entry, best)<=-1)
            {
                best = entry;
            }
        }
        return best;
    }

    /**
     * Retourne l'image correspondant au numéro ID spécifié. L'argument <code>ID</code>
     * correspond au numéro  {@link ImageEntry#getID}  d'une des images retournées par
     * {@link #getEntries()} ou {@link #getEntry()}.  L'image demandée doit appartenir
     * à la série accédée par cette table (voir {@link #getSeries}). L'image retournée
     * sera découpée de façon à n'inclure que les coordonnées spécifiées lors du dernier
     * appel de {@link #setGeographicArea}.
     *
     * @param  ID Numéro identifiant l'image désirée.
     * @return L'image demandée, ou <code>null</code> si elle n'a pas été trouvée.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized ImageEntry getEntry(final int ID) throws SQLException
    {
        if (imageByID==null)
        {
            final String query = select(preferences.get(IMAGES, SQL_SELECT))+" WHERE "+IMAGES+".ID=?";
            imageByID = statement.getConnection().prepareStatement(query);
        }
        imageByID.setInt(1, ID);
        return getEntry(imageByID);
    }

    /**
     * Retourne l'image nommée. L'argument <code>name</code> correspond au nom {@link ImageEntry#getName}
     * d'une des images retournées par {@link #getEntries} ou {@link #getEntry()}.  L'image demandée doit
     * appartenir à la série accédée par cette table ({@link #getSeries}). L'image retournée sera découpée
     * de façon à n'inclure que les coordonnées spécifiées lors du dernier appel de {@link #setGeographicArea}.
     *
     * @param  name Nom de l'image désirée.
     * @return L'image demandée, ou <code>null</code> si elle n'a pas été trouvée.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized ImageEntry getEntry(final String name) throws SQLException
    {
        if (imageByName==null)
        {
            final String query = select(preferences.get(IMAGES, SQL_SELECT))+" WHERE ("+GROUPS+".visible="+TRUE+") AND (series=?) AND (filename LIKE ?)";
            imageByName = statement.getConnection().prepareStatement(query);
        }
        imageByName.setInt(1, series.getID());
        imageByName.setString(2, name);
        return getEntry(imageByName);
    }

    /**
     * Retourne l'image correspondant à la requête spécifiée. Il ne
     * doit y avoir qu'une image correspondant à cette requête.
     */
    private ImageEntry getEntry(final PreparedStatement query) throws SQLException
    {
        assert Thread.holdsLock(this);
        ImageEntry entry=null;
        final ResultSet result=query.executeQuery();
        if (result.next())
        {
            entry=new ImageEntryImpl(this, result).intern();
            while (result.next())
            {
                final ImageEntry check = new ImageEntryImpl(this, result);
                if (!entry.equals(check))
                    throw new IllegalRecordException(IMAGES, Resources.format(ResourceKeys.ERROR_DUPLICATED_IMAGE_$2, entry.getName(), check.getName()));
            }
        }
        result.close();
        return entry;
    }

    /**
     * Obtient les plages de temps et de coordonnées
     * couvertes par les images de cette table.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public void getRanges(final RangeSet x, final RangeSet y, final RangeSet t) throws SQLException
    {
        getRanges(x, y, t, null);
    }

    /**
     * Obtient les plages de temps et de coordonnées des images, ainsi que la
     * liste des entrées correspondantes. Cette méthode peut être vue comme une
     * combinaison des méthodes {@link #getRanges(RangeSet,RangeSet,RangeSet)}
     * et {@link #getEntries()}.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     * @param entryList Liste dans laquelle ajouter les images qui auront été
     *        lues, ou <code>null</code> pour ne pas construire cette liste.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public synchronized void getRanges(final RangeSet x, final RangeSet y, final RangeSet t, final List<ImageEntry> entryList) throws SQLException
    {
        ImageEntryImpl newEntry = null;
        long        lastEndTime = Long.MIN_VALUE;
        final int    startIndex = (entryList!=null) ? entryList.size() : 0;
        final ResultSet  result = statement.executeQuery();
  loop: while (result.next())
        {
            /*
             * Add the new entry to the list.  If many entries have the same
             * spatio-temporal coordinates but different resolution, then an
             * entry with a resolution close to the requested resolution will
             * be selected.
             */
            if (entryList!=null)
            {
                newEntry = new ImageEntryImpl(this, result);
                for (int i=entryList.size(); --i>=0;)
                {
                    final ImageEntryImpl olderEntry = (ImageEntryImpl) entryList.get(i);
                    if (!olderEntry.compare(newEntry))
                    {
                        // Entry not equals according the "ORDER BY" clause.
                        break;
                    }
                    final ImageEntryImpl lowestResolution = olderEntry.getLowestResolution(newEntry);
                    if (lowestResolution!=null)
                    {
                        // Two entries has the same spatio-temporal coordinates.
                        if (lowestResolution.hasEnoughResolution(resolution, coordinateSystem))
                        {
                            // The entry with the lowest resolution is enough.
                            entryList.set(i, lowestResolution);
                        }
                        else if (lowestResolution == olderEntry)
                        {
                            // No entry has enough resolution;
                            // keep the one with the finest resolution.
                            entryList.set(i, newEntry);
                        }
                        continue loop;
                    }
                }
                entryList.add(newEntry);
            }
            /*
             * Compute ranges if it has been requested.  If we have previously
             * constructed an ImageEntry, fetch the data from this entry since
             * some JDBC driver doesn't allow to get data from the same column
             * twice. Furthermore, this is faster...       The "continue loop"
             * statement above may have hidden some rows, but since those rows
             * have the same spatio-temporal coordinates than one visible row,
             * it should not have any effect except improving performance.
             */
            if (t!=null)
            {
                final Date startTime;
                final Date   endTime;
                if (newEntry!=null)
                {
                    startTime = newEntry.getStartTime();
                      endTime = newEntry.getEndTime();
                }
                else
                {
                    startTime = getTimestamp(START_TIME, result);
                      endTime = getTimestamp(  END_TIME, result);
                }
                if (startTime!=null && endTime!=null)
                {
                    final long    period = Math.round(result.getDouble(PERIOD)*DAY); // 0 si le champ est blanc.
                    final long lgEndTime = endTime.getTime();
                    final long checkTime = lgEndTime-period;
                    if (checkTime <= lastEndTime  &&  checkTime < startTime.getTime())
                    {
                        // Il arrive parfois que des images soient prises à toutes les 24 heures,
                        // mais pendant 12 heures seulement. On veut éviter que de telles images
                        // apparaissent tout le temps entrecoupées d'images manquantes.
                        startTime.setTime(checkTime);
                    }
                    lastEndTime = lgEndTime;
                    t.add(startTime, endTime);
                }
            }
            if (x!=null)
            {
                final float xmin;
                final float xmax;
                if (newEntry!=null)
                {
                    xmin = newEntry.xmin;
                    xmax = newEntry.xmax;
                }
                else
                {
                    xmin = result.getFloat(XMIN);
                    xmax = result.getFloat(XMAX);
                }
                x.add(xmin, xmax);
            }
            if (y!=null)
            {
                final float ymin;
                final float ymax;
                if (newEntry!=null)
                {
                    ymin = newEntry.ymin;
                    ymax = newEntry.ymax;
                }
                else
                {
                    ymin = result.getFloat(YMIN);
                    ymax = result.getFloat(YMAX);
                }
                y.add(ymin, ymax);
            }
        }
        result.close();
        if (entryList!=null)
        {
            final List<ImageEntry> newEntries = entryList.subList(startIndex, entryList.size());
            final int size = newEntries.size();
            ImageEntryImpl.intern(newEntries.toArray(new ImageEntry[size]));
            log("getEntries", Level.FINE, ResourceKeys.FOUND_IMAGES_$1, new Integer(size));
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (imageByID!=null)
        {
            imageByID.close();
            imageByID = null;
        }
        if (formatTable!=null)
        {
            formatTable.close();
            formatTable = null;
        }
        statement.close();
    }

    /**
     * Retourne une chaîne de caractères décrivant cette table.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer("ImageTable[\"");
        buffer.append(series.getName());
        buffer.append("\": ");
        buffer.append(getStringArea());
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Retourne les coordonnées demandées sous
     * forme de chaîne de caractères.
     */
    private String getStringArea()
    {return CTSUtilities.toWGS84String(coordinateSystem.getHeadCS(), geographicArea);}

    /**
     * Enregistre un évènement dans le journal.
     */
    private static void log(final String method, final Level level, final int clé, final Object param)
    {
        final Resources resources = Resources.getResources(null);
        final LogRecord record = resources.getLogRecord(level, clé, param);
        record.setSourceClassName("ImageTable");
        record.setSourceMethodName(method);
        logger.log(record);
    }

    /**
     * Procède à l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException
    {
        assert Thread.holdsLock(this);
        if (false)
        {
            // Cette ligne aurait suffit si ce n'était du bug #4380653...
            return result.getTimestamp(field, calendar);
        }
        else
        {
            if (localCalendar==null)
                localCalendar=new GregorianCalendar();
            return getTimestamp(field, result, calendar, localCalendar);
        }
    }

    /**
     * Retourne le format qui correspond au numéro spécifié. Si ce format avait
     * déjà été demandé auparavant, le même format sera réutilisé. Cette méthode
     * ne retourne jamais nul.
     *
     * @param  formatID Numéro identifiant le format voulu.
     * @throws SQLException si le format spécifié n'a pas été trouvé.
     */
    private FormatEntryImpl getFormat(final int formatID) throws SQLException
    {
        assert Thread.holdsLock(this);
        final Integer ID   = new Integer(formatID);
        FormatEntryImpl format = formats.get(ID);
        if (format==null)
        {
            if (formatTable==null)
            {
                formatTable = new FormatTable(statement.getConnection());
            }
            format = formatTable.getEntry(ID);
            formats.put(ID, format);
        }
        else if (formatTable!=null)
        {
            /*
             * Si on a demandé un format qui avait déjà été lu auparavant,
             * il y a de bonnes chances pour qu'on n'ai plus besoin de la
             * table des formats. On va donc la fermer pour économiser des
             * ressources. On la réouvrira plus tard si c'est nécessaire.
             */
            formatTable.close();
            formatTable = null;
        }
        return format;
    }

    /**
     * Retourne les paramètres de cette table. Pour des raisons d'économie
     * de mémoire (de très nombreux objets <code>Parameters</code> pouvant
     * être créés), cette méthode retourne un exemplaire unique autant que
     * possible. L'objet retourné ne doit donc pas être modifié!
     *
     * @param  seriesID Numéro ID de la série, pour fin de vérification. Ce
     *                  numéro doit correspondre à celui de la série examinée
     *                  par cette table.
     * @param  formatID Numéro ID du format des images.
     * @param  pathname Chemin relatif des images.
     * @param  cs       Système de coordonnées horizontale dans lequel exprimer
     *                  les coordonnées retournées, ou <code>null</code> pour
     *                  utiliser le système par défaut de la table.
     *
     * @return Un objet incluant les paramètres demandées ainsi que ceux de la table.
     * @throws SQLException si les paramètres n'ont pas pu être obtenus.
     */
    final synchronized Parameters getParameters(final int seriesID, final int formatID, final String pathname, HorizontalCoordinateSystem cs) throws SQLException
    {
        if (seriesID != series.getID())
        {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_WRONG_SERIES_$1, series.getName()));
        }
        if (cs==null)
        {
            cs = (HorizontalCoordinateSystem) coordinateSystem.getHeadCS();
        }
        /*
         * Si les paramètres spécifiés sont identiques à ceux qui avaient été
         * spécifiés la dernière fois, retourne le dernier bloc de paramètres.
         */
        final boolean invalidate = (parameters==null) ||
                                   (parameters.format.getID()!=formatID) ||
                                   !Utilities.equals(parameters.pathname, pathname);
        if (!invalidate && cs.equivalents(parameters.coordinateSystem.getHeadCS()))
        {
            if (formatTable!=null)
            {
                /*
                 * Si on a demandé un format qui avait déjà été lu auparavant,
                 * il y a de bonnes chances pour qu'on n'ai plus besoin de la
                 * table des formats. On va donc la fermer pour économiser des
                 * ressources. On la réouvrira plus tard si c'est nécessaire.
                 */
                formatTable.close();
                formatTable = null;
            }
            return parameters;
        }
        /*
         * Construit un nouveau bloc de paramètres et projète les
         * coordonnées vers le système de coordonnées spécifié.
         */
        if (invalidate || !coordinateSystem.equivalents(parameters.coordinateSystem))
        {
            parameters = (Parameters)pool.intern(new Parameters(series, getFormat(formatID), pathname, operation, opParam,
                                                                coordinateSystem, geographicArea, resolution, dateFormat));
        }
        try
        {
            parameters = parameters.createTransformed(cs);
        }
        catch (TransformException exception)
        {
            final SQLException e = new SQLException(Resources.format(ResourceKeys.ERROR_INCOMPATIBLE_COORDINATES_$1, getStringArea()));
            e.initCause(exception);
            throw e;
        }
        return parameters;
    }
}
