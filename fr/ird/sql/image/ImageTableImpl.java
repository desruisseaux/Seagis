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

// G�om�trie
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
 * Connection vers une table d'images. Cette table contient des r�f�rences vers des images sous
 * forme d'objets {@link ImageEntry}.  Une table <code>ImageTable</code> est capable de fournir
 * la liste des entr�s {@link ImageEntry} qui interceptent une certaines r�gion g�ographique et
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
     * Requ�te SQL utilis�e par cette classe pour obtenir la table des images.
     * L'ordre des colonnes est essentiel. Ces colonnes sont r�f�renc�es par
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

                      "ORDER BY end_time, groupe"; // DOIT �tre en ordre chronologique. Voir {@link ImageEntryImpl#compare}.

    /** Num�ro de colonne. */ static final int ID         =  1;
    /** Num�ro de colonne. */ static final int SERIES     =  2;
    /** Num�ro de colonne. */ static final int PATHNAME   =  3;
    /** Num�ro de colonne. */ static final int FILENAME   =  4;
    /** Num�ro de colonne. */ static final int START_TIME =  5;
    /** Num�ro de colonne. */ static final int END_TIME   =  6;
    /** Num�ro de colonne. */ static final int ELLIPSOID  =  7;
    /** Num�ro de colonne. */ static final int XMIN       =  8;
    /** Num�ro de colonne. */ static final int XMAX       =  9;
    /** Num�ro de colonne. */ static final int YMIN       = 10;
    /** Num�ro de colonne. */ static final int YMAX       = 11;
    /** Num�ro de colonne. */ static final int WIDTH      = 12;
    /** Num�ro de colonne. */ static final int HEIGHT     = 13;
    /** Num�ro de colonne. */ static final int FORMAT     = 14;
    /** Num�ro de colonne. */ static final int PERIOD     = 15;

    /** Num�ro d'argument. */ private static final int ARG_XMIN       = 1;
    /** Num�ro d'argument. */ private static final int ARG_XMAX       = 2;
    /** Num�ro d'argument. */ private static final int ARG_YMIN       = 3;
    /** Num�ro d'argument. */ private static final int ARG_YMAX       = 4;
    /** Num�ro d'argument. */ private static final int ARG_START_TIME = 5;
    /** Num�ro d'argument. */ private static final int ARG_END_TIME   = 6;
    /** Num�ro d'argument. */ private static final int ARG_SERIES     = 7;

    /**
     * Nombre de millisecondes entre le 01/01/1970 00:00 UTC et le 01/01/1950 00:00 UTC.
     * Le 1er janvier 1970 est l'epoch du Java, tandis que le 1er janvier 1950 est celui
     * de la Nasa (son jour julier "0"). La constante <code>EPOCH</code> sert � faire les
     * conversions d'un syst�me � l'autre.
     */
    private static final long EPOCH = -631152000000L; // Pour 1958, utiliser -378691200000L;

    /**
     * Nombre de millisecondes dans une journ�e.
     */
    private static final double DAY = 24*60*60*1000;

    /**
     * Convertit un jour julien en date.
     */
    static Date toDate(final double t)
    {return new Date(Math.round(t*DAY)+EPOCH);}

    /**
     * Convertit une date en nombre de jours �coul�s depuis le 1er janvier 1950.
     * Les valeurs <code>[MIN/MAX]_VALUE</code> sont converties en infinies.
     */
    static double toJulian(final long time)
    {
        if (time==Long.MIN_VALUE) return Double.NEGATIVE_INFINITY;
        if (time==Long.MAX_VALUE) return Double.POSITIVE_INFINITY;
        return (time-EPOCH)/DAY;
    }

    /**
     * Syst�me de coordonn�es par d�faut.  Les coordonn�es sont dans l'ordre
     * la longitude et la latitude (selon l'ellipso�de WGS 1984)  et la date
     * en jours julien depuis le 1er janvier 1950 � 00:00 UTC.
     */
    private static final CompoundCoordinateSystem coordinateSystem =
                         new CompoundCoordinateSystem("SEAS",
                             GeographicCoordinateSystem.WGS84,
                             new TemporalCoordinateSystem("Aviso", new Date(EPOCH)));

    /**
     * R�ference vers la s�rie d'images. Cette r�f�rence
     * est construite � partir du champ ID dans la table
     * "Series" de la base de donn�es.
     */
    private SeriesEntry series;

    /**
     * L'op�ration � appliquer sur les images lue,
     * ou <code>null</code> s'il n'y en a aucune.
     */
    private Operation operation;

    /**
     * Param�tres de l'op�ration, ou <code>null</code>
     * s'il n'y a pas d'op�ration.
     */
    private ParameterList opParam;

    /**
     * Dimension logique (en degr�s de longitude et de latitude) d�sir�e des pixels
     * de l'images. Cette information n'est qu'approximative. Il n'est pas garantie
     * que les lectures produiront effectivement des images de cette r�solution.
     * Une valeur nulle signifie que les lectures doivent se faire avec la meilleure
     * r�solution possible.
     */
    private Dimension2D resolution;

    /**
     * Coordonn�es g�ographiques de la r�gion d'int�ret, en degr�s de longitude et de latitude.
     * Ces coordonn�es peuvent �tre sp�cifi�es par un appel � {@link #setGeographicArea}.  Les
     * objets {@link Rectangle2D} affect�s � <code>geographicArea</code> peuvent changer, mais
     * les coordonn�es d'un objet {@link Rectangle2D} donn� ne doivent PAS �tre chang�es.   La
     * classe <code>ImageEntryImpl</code> se r�ferera directement � ce champ afin d'�viter la
     * cr�ation d'une multitude de clones.
     */
    private Rectangle2D geographicArea;

    /**
     * Date du d�but de la plage de temps des images
     * recherch�es par cet objet <code>ImageTable</code>.
     */
    private long startTime;

    /**
     * Date du fin de la plage de temps des images
     * recherch�es par cet objet <code>ImageTable</code>.
     */
    private long endTime;

    /**
     * Calendrier utilis� pour pr�parer les dates. Ce calendrier
     * utilisera le fuseau horaire sp�cifi� lors de la construction.
     */
    private final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Fuseau horaire de la base de donn�es.
     */
    public final TimeZone timezone;

    /**
     * Formatteur � utiliser pour �crire des dates pour l'utilisateur. Les caract�res et
     * les conventions linguistiques d�pendront de la langue de l'utilisateur. Toutefois,
     * le fuseau horaire devrait �tre celui de la r�gion d'�tude plut�t que celui du pays
     * de l'utilisateur.
     */
    private final DateFormat dateFormat;

    /**
     * Table des formats. Cette table ne sera construite que la premi�re fois
     * o� elle sera n�cessaire.  Elle sera ensuite ferm�e chaque fois qu'elle
     * n'est plus utilis�e pour �conomiser des ressources.
     */
    private transient FormatTable formatTable;

    /**
     * Ensemble des formats d�j� lue. Autant que possible,
     * on r�utilisera les formats qui ont d�j� �t� cr��s.
     */
    private final Map<Integer,FormatEntryImpl> formats=new HashMap<Integer,FormatEntryImpl>();

    /**
     * Requ�te SQL faisant le lien avec la base de donn�es.
     */
    private final PreparedStatement statement;

    /**
     * Table d'images pouvant �tre r�cup�r�e par leur num�ro ID.
     * Cette table ne sera construite que si elle est n�cessaire.
     */
    private transient PreparedStatement imageByID;

    /**
     * Table d'images pouvant �tre r�cup�r�e par leur nom.
     * Cette table ne sera construite que si elle est n�cessaire.
     */
    private transient PreparedStatement imageByName;

    /**
     * Derniers param�tres � avoir �t� construit. Ces param�tres sont
     * retenus afin d'�viter d'avoir � les reconstruires trop souvent
     * si c'est �vitable.
     */
    private transient Parameters parameters;

    /**
     * Construit une table des images en utilisant la connection sp�cifi�e.
     * L'appellant <strong>doit</strong> appeler {@link #setSeries},
     * {@link #setGeographicArea} et {@link #setTimeRange} avant d'utiliser
     * cette table.
     *
     * @param  connection Connection vers une base de donn�es d'images.
     * @param  timezone   Fuseau horaire des dates inscrites dans la base
     *                    de donn�es. Cette information est utilis�e pour
     *                    convertir en heure UTC les dates �crites dans la
     *                    base de donn�es.
     * @throws SQLException si <code>ImageTable</code> n'a pas pu construire sa requ�te SQL.
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
     * Retourne la r�f�rence vers la s�ries d'images.
     */
    public SeriesEntry getSeries()
    {return series;}

    /**
     * D�finit la s�rie dont on veut les images.
     *
     * @param  series       R�ference vers la s�rie d'images. Cette r�f�rence
     *                      est construite � partir du champ ID dans la table
     *                      "Series" de la base de donn�es.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la
     *                      base de donn�es, ou si <code>series</code> ne
     *                      se r�f�re pas � un enregistrement de la table
     *                      des s�ries.
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
     * Retourne le syst�me de coordonn�es utilis� pour les coordonn�es spatio-temporelles de
     * <code>[get/set]Envelope(...)</code>. En g�n�ral, ce syst�me de coordonn�es aura trois
     * dimensions (la derni�re dimension �tant le temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Les latitudes,  en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     */
    public final CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Retourne les coordonn�es spatio-temporelles de la r�gion d'int�r�t. Le syst�me
     * de coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
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
     * D�finit les coordonn�es spatio-temporelles de la r�gion d'int�r�t. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}. Appeler
     * cette m�thode �quivaut � effectuer les transformations n�cessaires des coordonn�es
     * et � appeler {@link #setTimeRange} et {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public synchronized void setEnvelope(final Envelope envelope) throws SQLException
    {
        // No coordinate transformation needed for this implementation.
        setGeographicArea(new Rectangle2D.Double(envelope.getMinimum(0), envelope.getMinimum(1),
                                                 envelope.getLength (0), envelope.getLength (1)));
        setTimeRange(toDate(envelope.getMinimum(2)), toDate(envelope.getMaximum(2)));
    }

    /**
     * Retourne la p�riode de temps d'int�r�t.  Cette plage sera d�limit�e par des objets
     * {@link Date}. Appeler cette m�thode �quivant � n'extraire que la partie temporelle
     * de {@link #getEnvelope} et � transformer les coordonn�es si n�cessaire.
     */
    public synchronized Range getTimeRange()
    {return new Range(Date.class, new Date(startTime), new Date(endTime));}

    /**
     * D�finit la p�riode de temps d'int�r�t (dans laquelle rechercher des images).
     * Cette m�thode ne change que la partie temporelle de l'enveloppe recherch�e
     * (voir {@link #getEnvelope}).
     *
     * @param  range P�riode d'int�r�t dans laquelle rechercher des images.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * D�finit la p�riode de temps d'int�r�t (dans laquelle rechercher des images).
     * Cette m�thode ne change que la partie temporelle de l'enveloppe recherch�e
     * (voir {@link #getEnvelope}).
     *
     * @param  startTime Date du  d�but de la plage de temps, inclusive.
     * @param  endTime   Date de la fin de la plage de temps, inclusive.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * Retourne les coordonn�es g�ographiques de la r�gion d'int�r�t.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     */
    public synchronized Rectangle2D getGeographicArea()
    {return (Rectangle2D) geographicArea.clone();}

    /**
     * D�finit les coordonn�es g�ographiques de la r�gion d'int�r�t   (dans laquelle rechercher des
     * images). Ces coordonn�es sont toujours exprim�es en degr�s de longitude et de latitude selon
     * l'ellipso�de WGS 1984. Cette m�thode ne change que la partie horizontale de l'enveloppe (voir
     * {@link #setEnvelope}).
     *
     * @param  rect Coordonn�es g�ographiques de la r�gion, selon l'ellipso�de WGS 1984.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * Retourne la dimension d�sir�e des pixels de l'images.
     *
     * @return R�solution pr�f�r�e, ou <code>null</code> si la lecture
     *         doit se faire avec la meilleure r�solution disponible.
     */
    public synchronized Dimension2D getPreferredResolution()
    {return (resolution!=null) ? (Dimension2D)resolution.clone() : null;}

    /**
     * D�finit la dimension d�sir�e des pixels de l'images.  Cette information n'est
     * qu'approximative. Il n'est pas garantie que la lecture produira effectivement
     * des images de cette r�solution. Une valeur nulle signifie que la lecture doit
     * se faire avec la meilleure r�solution disponible.
     *
     * @param  pixelSize Taille pr�f�r�e des pixels. Les unit�s sont les m�mes
     *         que celles de {@link #setGeographicArea}.
     */
    public synchronized void setPreferredResolution(final Dimension2D pixelSize)
    {
        if (!Utilities.equals(resolution, pixelSize))
        {
            parameters = null;
            final int cl�;
            final Object param;
            if (pixelSize!=null)
            {
                resolution = (Dimension2D)pixelSize.clone();
                cl� = ResourceKeys.SET_RESOLUTION_$2;
                param = new Double[]{new Double(resolution.getWidth()),new Double(resolution.getHeight())};
            }
            else
            {
                resolution = null;
                cl� = ResourceKeys.UNSET_RESOLUTION;
                param = null;
            }
            log("setPreferredResolution", Level.CONFIG, cl�, param);
        }
    }

    /**
     * Retourne l'op�ration appliqu�e sur les images lues. L'op�ration retourn�e
     * peut repr�senter par exemple un gradient. Si aucune op�ration n'est appliqu�e
     * (c'est-�-dire si les images retourn�es repr�sentent les donn�es originales),
     * alors cette m�thode retourne <code>null</code>.
     */
    public Operation getOperation()
    {return operation;}

    /**
     * D�finit l'op�ration � appliquer sur les images lues. Si des param�tres doivent
     * �tre sp�cifi�s  en plus de l'op�ration,   ils peuvent l'�tre en appliquant des
     * m�thodes <code>setParameter</code> sur la r�f�rence retourn�e. Par exemple, la
     * ligne suivante transforme tous les pixels des images � lire en appliquant
     * l'�quation lin�aire <code>value*constant+offset</code>:
     *
     * <blockquote><pre>
     * setOperation("Rescale").setParameter("constants", new double[]{10})
     *                        .setParameter("offsets"  , new double[]{50]);
     * </pre></blockquote>
     *
     * @param  operation L'op�ration � appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune op�ration.
     * @return Liste de param�tres par d�faut, ou <code>null</code> si <code>operation</code>
     *         �tait nul. Les modifications apport�es sur cette liste de param�tres influenceront
     *         les images obtenues lors du prochain appel d'une m�thode <code>getEntry</code>.
     */
    public synchronized ParameterList setOperation(final Operation operation)
    {
        this.parameters = null;
        this.operation=operation;
        final int cl�;
        final String name;
        if (operation!=null)
        {
            opParam = operation.getParameterList();
            name    = operation.getName();
            cl�     = ResourceKeys.SET_OPERATION_$1;
        }
        else
        {
            opParam = null;
            name    = null;
            cl�     = ResourceKeys.UNSET_OPERATION;
        }
        log("setOperation", Level.CONFIG, cl�, name);
        return opParam;
    }

    /**
     * D�finit l'op�ration � appliquer sur les images lues. Cette m�thode est �quivalente �
     * <code>setOperation({@link GridImageProcessor#getOperation GridImageProcessor.getOperation}(name))</code>.
     *
     * @param  operation L'op�ration � appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune op�ration.
     * @return Liste de param�tres par d�faut, ou <code>null</code> si <code>operation</code>
     *         �tait nul. Les modifications apport�es sur cette liste de param�tres influenceront
     *         les images obtenues lors du prochain appel d'une m�thode <code>getEntry</code>.
     * @throws OperationNotFoundException si l'op�ration <code>operation</code> n'a pas �t� trouv�e.
     */
    public ParameterList setOperation(final String operation) throws OperationNotFoundException
    {return setOperation(operation!=null ? Parameters.PROCESSOR.getOperation(operation) : null);}

    /**
     * Retourne la liste des images disponibles dans la plage de coordonn�es
     * spatio-temporelles pr�alablement s�lectionn�es. Ces plages auront �t�
     * sp�cifi�es � l'aide des diff�rentes m�thodes <code>set...</code> de
     * cette classe.
     *
     * @return Liste d'images qui interceptent la plage de temps et la r�gion g�ographique d'int�r�t.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public List<ImageEntry> getEntries() throws SQLException
    {
        /*
         * On construit un tableau d'ImageEntry ET NON d'ImageEntryImpl
         * parce que certains utilisateurs (par exemple ImageTableModel)
         * voudront remplacer certains �l�ments de ce tableau sans que
         * �a ne lance un {@link java.lang.ArrayStoreException}.
         */
        final List<ImageEntry> entries = new ArrayList<ImageEntry>();
        getRanges(null, null, null, entries);
        return entries;
    }

    /**
     * Retourne une des images disponibles dans la plage de coordonn�es spatio-temporelles
     * pr�alablement s�lectionn�es. Si plusieurs images interceptent la r�gion et la plage
     * de temps   (c'est-�-dire si {@link #getEntries} retourne un tableau d'au moins deux
     * entr�es), alors le choix de l'image se fera en utilisant un objet {@link ImageComparator}
     * par d�faut. Ce choix peut �tre arbitraire.
     *
     * @return Une image choisie arbitrairement dans la r�gion et la plage de date
     *         s�lectionn�es, ou <code>null</code> s'il n'y a pas d'image dans ces plages.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * Retourne l'image correspondant au num�ro ID sp�cifi�. L'argument <code>ID</code>
     * correspond au num�ro  {@link ImageEntry#getID}  d'une des images retourn�es par
     * {@link #getEntries()} ou {@link #getEntry()}.  L'image demand�e doit appartenir
     * � la s�rie acc�d�e par cette table (voir {@link #getSeries}). L'image retourn�e
     * sera d�coup�e de fa�on � n'inclure que les coordonn�es sp�cifi�es lors du dernier
     * appel de {@link #setGeographicArea}.
     *
     * @param  ID Num�ro identifiant l'image d�sir�e.
     * @return L'image demand�e, ou <code>null</code> si elle n'a pas �t� trouv�e.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * Retourne l'image nomm�e. L'argument <code>name</code> correspond au nom {@link ImageEntry#getName}
     * d'une des images retourn�es par {@link #getEntries} ou {@link #getEntry()}.  L'image demand�e doit
     * appartenir � la s�rie acc�d�e par cette table ({@link #getSeries}). L'image retourn�e sera d�coup�e
     * de fa�on � n'inclure que les coordonn�es sp�cifi�es lors du dernier appel de {@link #setGeographicArea}.
     *
     * @param  name Nom de l'image d�sir�e.
     * @return L'image demand�e, ou <code>null</code> si elle n'a pas �t� trouv�e.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
     * Retourne l'image correspondant � la requ�te sp�cifi�e. Il ne
     * doit y avoir qu'une image correspondant � cette requ�te.
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
     * Obtient les plages de temps et de coordonn�es
     * couvertes par les images de cette table.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public void getRanges(final RangeSet x, final RangeSet y, final RangeSet t) throws SQLException
    {
        getRanges(x, y, t, null);
    }

    /**
     * Obtient les plages de temps et de coordonn�es des images, ainsi que la
     * liste des entr�es correspondantes. Cette m�thode peut �tre vue comme une
     * combinaison des m�thodes {@link #getRanges(RangeSet,RangeSet,RangeSet)}
     * et {@link #getEntries()}.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     * @param entryList Liste dans laquelle ajouter les images qui auront �t�
     *        lues, ou <code>null</code> pour ne pas construire cette liste.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
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
                        // Il arrive parfois que des images soient prises � toutes les 24 heures,
                        // mais pendant 12 heures seulement. On veut �viter que de telles images
                        // apparaissent tout le temps entrecoup�es d'images manquantes.
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
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
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
     * Retourne une cha�ne de caract�res d�crivant cette table.
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
     * Retourne les coordonn�es demand�es sous
     * forme de cha�ne de caract�res.
     */
    private String getStringArea()
    {return CTSUtilities.toWGS84String(coordinateSystem.getHeadCS(), geographicArea);}

    /**
     * Enregistre un �v�nement dans le journal.
     */
    private static void log(final String method, final Level level, final int cl�, final Object param)
    {
        final Resources resources = Resources.getResources(null);
        final LogRecord record = resources.getLogRecord(level, cl�, param);
        record.setSourceClassName("ImageTable");
        record.setSourceMethodName(method);
        logger.log(record);
    }

    /**
     * Proc�de � l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException
    {
        assert Thread.holdsLock(this);
        if (false)
        {
            // Cette ligne aurait suffit si ce n'�tait du bug #4380653...
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
     * Retourne le format qui correspond au num�ro sp�cifi�. Si ce format avait
     * d�j� �t� demand� auparavant, le m�me format sera r�utilis�. Cette m�thode
     * ne retourne jamais nul.
     *
     * @param  formatID Num�ro identifiant le format voulu.
     * @throws SQLException si le format sp�cifi� n'a pas �t� trouv�.
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
             * Si on a demand� un format qui avait d�j� �t� lu auparavant,
             * il y a de bonnes chances pour qu'on n'ai plus besoin de la
             * table des formats. On va donc la fermer pour �conomiser des
             * ressources. On la r�ouvrira plus tard si c'est n�cessaire.
             */
            formatTable.close();
            formatTable = null;
        }
        return format;
    }

    /**
     * Retourne les param�tres de cette table. Pour des raisons d'�conomie
     * de m�moire (de tr�s nombreux objets <code>Parameters</code> pouvant
     * �tre cr��s), cette m�thode retourne un exemplaire unique autant que
     * possible. L'objet retourn� ne doit donc pas �tre modifi�!
     *
     * @param  seriesID Num�ro ID de la s�rie, pour fin de v�rification. Ce
     *                  num�ro doit correspondre � celui de la s�rie examin�e
     *                  par cette table.
     * @param  formatID Num�ro ID du format des images.
     * @param  pathname Chemin relatif des images.
     * @param  cs       Syst�me de coordonn�es horizontale dans lequel exprimer
     *                  les coordonn�es retourn�es, ou <code>null</code> pour
     *                  utiliser le syst�me par d�faut de la table.
     *
     * @return Un objet incluant les param�tres demand�es ainsi que ceux de la table.
     * @throws SQLException si les param�tres n'ont pas pu �tre obtenus.
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
         * Si les param�tres sp�cifi�s sont identiques � ceux qui avaient �t�
         * sp�cifi�s la derni�re fois, retourne le dernier bloc de param�tres.
         */
        final boolean invalidate = (parameters==null) ||
                                   (parameters.format.getID()!=formatID) ||
                                   !Utilities.equals(parameters.pathname, pathname);
        if (!invalidate && cs.equivalents(parameters.coordinateSystem.getHeadCS()))
        {
            if (formatTable!=null)
            {
                /*
                 * Si on a demand� un format qui avait d�j� �t� lu auparavant,
                 * il y a de bonnes chances pour qu'on n'ai plus besoin de la
                 * table des formats. On va donc la fermer pour �conomiser des
                 * ressources. On la r�ouvrira plus tard si c'est n�cessaire.
                 */
                formatTable.close();
                formatTable = null;
            }
            return parameters;
        }
        /*
         * Construit un nouveau bloc de param�tres et proj�te les
         * coordonn�es vers le syst�me de coordonn�es sp�cifi�.
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
