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
package fr.ird.database.coverage;

// G�om�trie
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

// Requ�tes SQL et entr�s/sorties
import java.sql.SQLException;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.imageio.ImageReader;

// Ev�nements
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import org.geotools.io.image.IIOReadProgressAdapter;

// Divers
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.media.jai.util.Range;

// Geotools (CTS)
import org.geotools.pt.Envelope;
import org.geotools.pt.CoordinatePoint;
import org.geotools.pt.MismatchedDimensionException;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.resources.CTSUtilities;

// Geotools (GCS)
import org.geotools.gc.GridRange;
import org.geotools.gc.GridGeometry;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.Coverage;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.ColorInterpretation;
import org.geotools.cv.CannotEvaluateException;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.gp.GridCoverageProcessor;

// Seagis
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Enveloppe une table d'images comme s'il s'agissait d'un espace � trois dimensions, la
 * troisi�me dimension �tant le temps.  Cette classe offre une fa�on pratique d'extraire
 * des valeurs � des positions et des dates arbitraires. Les valeurs sont interpoll�es �
 * la fois dans l'espace et dans le temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Coverage3D extends Coverage {
    /**
     * <code>true</code> pour ex�cuter {@link System#gc} avant tout chargement d'images. Il
     * s'agit d'une tentative de r�duction des erreurs de type {@link OutOfMemoryError}.  A
     * l'heure actuelle, il est pratiquement impossible d'utiliser cette classe sans se casser
     * la gueule sur un {@link OutOfMemoryError}, m�me avec une quantit� monstrueuse de m�moire
     * donn�e � la machine virtuelle avec l'option -Xmx. Le probl�me est peut-�tre li� au
     * bug #4640743 (SoftReferences are not being released in time and cause OutOfMemoryError).
     */
    private static final boolean RUN_GC = true;

    /**
     * Liste des images � prendre en compte.
     */
    private final CoverageEntry[] entries;

    /**
     * Listes des bandes des images.
     */
    private final SampleDimension[] bands;

    /**
     * The temporal coordinate system.
     */
    private final TemporalCoordinateSystem temporalCS;

    /**
     * The dimension of the temporal coordinate system.
     */
    private final int temporalDimension;

    /**
     * L'envelope (coordonn�es g�ographiques et plage de temps) englobant celles
     * de toutes les images trouv�es dans {@link #entries}.
     *
     * @see #getEnvelope
     * @see #getTimeRange
     * @see #getGeographicArea
     */
    private final Envelope envelope;

    /**
     * La partie horizontale de l'enveloppe en degr�s de longitude et de latitude.
     */
    private final Rectangle2D geographicArea;

    /**
     * Indique si les interpolations spatio-temporelles sont permises.
     */
    private boolean interpolationAllowed = true;

    /**
     * Intervalle de temps maximal tol�r� entre la fin d'une image et
     * le d�but de l'image suivante, en nombre de millisecondes. Si un
     * intervalle de temps sup�rieur s�pare deux images, on consid�rera
     * qu'on a un trou dans les donn�es.
     */
    private final long maxTimeLag = 0;

    /**
     * Liste des objets int�ress�s � �tre inform�s
     * des progr�s de la lecture des images.
     */
    private final EventListenerList listeners = new EventListenerList();

    /**
     * Internal listener for logging image loading.
     */
    private transient Listeners readListener;

    /**
     * Donn�es dont la date de d�but est inf�rieure ou �gale � la date demand�e.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit inf�rieure ou �gale � la date demand�e (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage lower;

    /**
     * Donn�es dont la date de fin  est sup�rieure ou �gale � la date demand�e.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit sup�rieure ou �gale � la date demand�e (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage upper;

    /**
     * Date et heure du milieu des donn�es {@link #lower} et {@link #upper},
     * en nombre de millisecondes �coul�es depuis le 1er janvier 1970 UTC.
     */
    private transient long timeLower=Long.MAX_VALUE, timeUpper=Long.MIN_VALUE;

    /**
     * L'objet � utiliser pour effectuer des op�rations sur les images
     * (notamment modifier les interpolations). Ne sera construit que
     * la premi�re fois o� il sera n�cessaire.
     */
    private transient GridCoverageProcessor processor;

    /**
     * Initialize fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        timeLower = Long.MAX_VALUE;
        timeUpper = Long.MIN_VALUE;
    }

    /**
     * Construit une couverture � partir des donn�es de la table sp�cifi�e.
     * Les entr�es {@link CoverageEntry} seront m�moris�es immediatement.
     * Toute modification faite � la table apr�s la construction de cet objet
     * <code>Coverage3D</code> (incluant la fermeture de la table) n'auront
     * aucun effet sur cet objet.
     *
     * @param  table Table d'o� proviennent les donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     * @throws TransformException si une transformation de coordonn�es �tait n�cessaire et a �chou�.
     */
    public Coverage3D(final CoverageTable table) throws SQLException, TransformException {
        this(table, table.getCoordinateSystem());
    }

    /**
     * Construit une couverture � partir des donn�es de la table sp�cifi�e et utilisant
     * le syst�me de coordonn�es sp�cifi�.
     *
     * @param  table Table d'o� proviennent les donn�es.
     * @param  cs Le syst�me de coordonn�es � utiliser pour cet obet {@link Coverage}.
     *         Ce syst�me de coordonn�es doit obligatoirement comprendre un axe temporel.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     * @throws TransformException si une transformation de coordonn�es �tait n�cessaire et a �chou�.
     */
    public Coverage3D(final CoverageTable table, final CoordinateSystem cs)
            throws SQLException, TransformException
    {
        super(table.getSeries().getName(), cs, null, null);
        temporalCS = CTSUtilities.getTemporalCS(cs);
        if (temporalCS == null) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_COORDINATE_SYSTEM));
        }
        temporalDimension = CTSUtilities.getDimensionOf(cs, temporalCS.getClass());
        assert temporalDimension >= 0 : temporalDimension;
        /*
         * Obtient la liste des images en ordre chronologiques, et
         * v�rifie au passage qu'elles ont toutes les m�mes bandes.
         */
        final List<CoverageEntry> entryList = table.getEntries();
        this.entries = (CoverageEntry[])entryList.toArray(new CoverageEntry[entryList.size()]);
        this.bands   = (entries.length!=0) ? entries[0].getSampleDimensions() : new SampleDimension[0];
        for (int i=1; i<entries.length; i++) {
            if (!Arrays.equals(bands, entries[i].getSampleDimensions())) {
                throw new SQLException(Resources.format(ResourceKeys.ERROR_CATEGORIES_MITMATCH));
            }
        }
        Arrays.sort(entries, COMPARATOR);
        /*
         * Calcule l'enveloppe englobant celles de toutes les images.
         * Les coordonn�es seront transform�es si n�cessaires.
         */
        Envelope                       envelope = null;
        CoordinateTransformation      transform = null;
        CoordinateTransformationFactory factory = null;
        for (int i=0; i<entries.length; i++) {
            CoverageEntry       entry = entries[i];
            Envelope        candidate = entry.getEnvelope();
            CoordinateSystem sourceCS = entry.getCoordinateSystem();
            if (!cs.equals(sourceCS, false)) {
                if (transform==null || !transform.getSourceCS().equals(sourceCS, false)) {
                    if (factory == null) {
                        factory = CoordinateTransformationFactory.getDefault();
                    }
                    transform = factory.createFromCoordinateSystems(sourceCS, cs);
                }
                candidate = CTSUtilities.transform(transform.getMathTransform(), candidate);
            }
            if (envelope == null) {
                envelope = candidate;
            } else {
                envelope.add(candidate);
            }
        }
        this.envelope = envelope;
        /*
         * Obtient la partie horizontale de l'envelope, et transforme (si n�cessaire)
         * cette partie en degr�s de longitude et de latitude selon WGS84.
         */
        Rectangle2D geographicArea = envelope.getReducedEnvelope(temporalDimension, temporalDimension+1).toRectangle2D();
        final CoordinateSystem sourceCS = CTSUtilities.getHorizontalCS(cs);
        final CoordinateSystem targetCS = GeographicCoordinateSystem.WGS84;
        if (!targetCS.equals(sourceCS, false)) {
            if (factory == null) {
                factory = CoordinateTransformationFactory.getDefault();
            }
            transform = factory.createFromCoordinateSystems(sourceCS, targetCS);
            geographicArea = CTSUtilities.transform((MathTransform2D)transform.getMathTransform(),
                                                    geographicArea, geographicArea);
        }
        this.geographicArea = geographicArea;
    }

    /**
     * Comparateur � utiliser pour classer les images et effectuer
     * des recherches rapides. Ce comparateur utilise la date du
     * milieu comme crit�re.
     */
    private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
        public int compare(final Object entry1, final Object entry2) {
            final long time1 = getTime(entry1);
            final long time2 = getTime(entry2);
            if (time1 < time2) return -1;
            if (time1 > time2) return +1;
            return 0;
        }
    };

    /**
     * Retourne la date de l'objet sp�cifi�e.  L'argument peut �tre un objet
     * {@link Date} ou {@link CoverageEntry}. Dans ce dernier cas, la date
     * sera extraite avec {@link #getTime}.
     */
    private static long getTime(final Object object) {
        if (object instanceof Date) {
            return ((Date) object).getTime();
        }
        if (object instanceof CoverageEntry) {
            return getTime((CoverageEntry) object);
        }
        return Long.MIN_VALUE;
    }

    /**
     * Retourne la date du milieu de l'image sp�cifi�e.  Si l'image ne couvre aucune
     * plage de temps (par exemple s'il s'agit de donn�es qui ne varient pas avec le
     * temps, comme la bathym�trie), alors cette m�thode retourne {@link Long#MIN_VALUE}.
     */
    private static long getTime(final CoverageEntry entry) {
        final Range timeRange = entry.getTimeRange();
        if (timeRange!=null) {
            final Date startTime = (Date) timeRange.getMinValue();
            final Date   endTime = (Date) timeRange.getMaxValue();
            if (startTime!=null) {
                if (endTime!=null) {
                    return (endTime.getTime()+startTime.getTime())/2;
                } else {
                    return startTime.getTime();
                }
            } else if (endTime!=null) {
                return endTime.getTime();
            }
        }
        return Long.MIN_VALUE;
    }

    /**
     * Retourne un rectangle englobant les coordonn�es g�ographiques de toutes les donn�es
     * disponibles. Les coordonn�es seront toujours exprim�es en degr�s de longitude et de
     * latitudes, selon le syst�me de coordonn�es {@linkplain GeographicCoordinateSystem#WGS84
     * WGS84}.
     */
    public Rectangle2D getGeographicArea() {
        return (Rectangle2D) geographicArea.clone();
    }

    /**
     * Retourne la plage de temps englobant toutes les donn�es disponibles.
     * La plage contiendra des objets {@link Date}.
     */
    public Range getTimeRange() {
        return new Range(Date.class, temporalCS.toDate(envelope.getMinimum(temporalDimension)),
                                     temporalCS.toDate(envelope.getMinimum(temporalDimension)));
    }

    /**
     * Returns the bounding box for the coverage domain in coordinate system coordinates.
     */
    public Envelope getEnvelope() {
        return (Envelope) envelope.clone();
    }

    /**
     * Returns the number of {@link SampleDimension} in this coverage.
     */
    public int getNumSampleDimensions() {
        return bands.length;
    }

    /**
     * Retrieve sample dimension information for the coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension.
     */
    public SampleDimension[] getSampleDimensions() {
        return (SampleDimension[]) bands.clone();
    }

    /**
     * Snap the specified coordinate point and date to the closest point available in
     * this coverage. First, this method locate the image at or near the specified date
     * (if no image was available at the specified date, the closest one is selected).
     * The <code>date</code> argument is then set to this date. Then, this method locate
     * the pixel under the <code>point</code> coordinate on this image. The <code>point</code>
     * argument is then set to this pixel center. Consequently, calling any <code>evaluate</code>
     * method with snapped coordinates will returns non-interpolated values.
     *
     * @param point The point to snap (may be null).
     * @param date  The date to snap (can not be null, since we need to
     *              know the image's date before to snap the point).
     */
    public void snap(final Point2D point, final Date date) { // No synchronization needed.
        int index = Arrays.binarySearch(entries, date, COMPARATOR);
        if (index<0) {
            /*
             * There is no exact match for the date.
             * Snap the date to the closest image.
             */
            index = ~index;
            long time;
            if (index==entries.length) {
                if (index==0) return; // No entries in this coverage!
                time = getTime(entries[--index]);
            } else if (index>=1) {
                time = date.getTime();
                final long lowerTime = getTime(entries[index-1]);
                final long upperTime = getTime(entries[index])-1; // Long.MIN_VALUE-1 == Long.MAX_VALUE
                assert (time>lowerTime && time<upperTime);
                if (time-lowerTime < upperTime-time) {
                    index--;
                    time = lowerTime;
                } else {
                    time = upperTime+1;
                }
            } else {
                time = getTime(entries[index]);
            }
            if (time!=Long.MIN_VALUE && time!=Long.MAX_VALUE) {
                date.setTime(time);
            }
        }
        /*
         * Now that we know the image entry,
         * snap the spatial coordinate point.
         */
        if (point != null) try {
            final CoverageEntry  entry = entries[index];
            CoordinatePoint coordinate = new CoordinatePoint(coordinateSystem.getDimension());
            coordinate.ord[temporalDimension!=0 ? 0 : 1] = point.getX();
            coordinate.ord[temporalDimension>=2 ? 1 : 2] = point.getY();
            coordinate.ord[temporalDimension] = temporalCS.toValue(date);
            if (!coordinateSystem.equals(entry.getCoordinateSystem(), false)) {
                // TODO: impl�menter la transformation de coordonn�es.
                throw new CannotEvaluateException("Syst�me de coordonn�es incompatibles.");
            }
            final GridGeometry   geometry = entry.getGridGeometry();
            final GridRange         range = geometry.getGridRange();
            final MathTransform transform = geometry.getGridToCoordinateSystem();
            coordinate = transform.inverse().transform(coordinate, coordinate);
            for (int i=coordinate.getDimension(); --i>=0;) {
                coordinate.ord[i] = Math.max(range.getLower(i),
                                    Math.min(range.getUpper(i)-1,
                                    (int)Math.rint(coordinate.ord[i])));
            }
            coordinate = transform.transform(coordinate, coordinate);
            point.setLocation(coordinate.ord[0], coordinate.ord[1]);
        } catch (TransformException exception) {
            throw new CannotEvaluateException(point, exception);
        }
    }

    /**
     * Load a single image for the specified image entry.
     *
     * @param  entry The image to load.
     * @return The loaded image.
     * @throws IOException if an error occured while loading image.
     */
    private GridCoverage load(final CoverageEntry entry) throws IOException {
        GridCoverage coverage = entry.getGridCoverage(listeners);
        if (!interpolationAllowed) {
            if (processor == null) {
                processor = GridCoverageProcessor.getDefault();
            }
            coverage = processor.doOperation("Interpolate", coverage, "Type", "NearestNeighbor");
        }
        return coverage;
    }

    /**
     * Loads a single image at the given index.
     *
     * @param  index Index in {@link #entries} for the image to load.
     * @throws IOException if an error occured while loading image.
     */
    private void load(final int index) throws IOException {
        if (RUN_GC) {
            System.gc();
            System.runFinalization();
        }
        final CoverageEntry entry = entries[index];
        log(ResourceKeys.LOADING_IMAGE_$1, new Object[]{entry});
        lower = upper = load(entry);
        timeLower = timeUpper = getTime(entry);
    }

    /**
     * Loads images for the given entries.
     *
     * @throws IOException if an error occured while loading images.
     */
    private void load(final CoverageEntry entryLower,
                      final CoverageEntry entryUpper)
            throws IOException
    {
        if (RUN_GC) {
            System.gc();
            System.runFinalization();
        }
        final long timeLower = getTime(entryLower);
        final long timeUpper = getTime(entryUpper);
        log(ResourceKeys.LOADING_IMAGES_$2, new Object[]{entryLower, entryUpper});
        final GridCoverage lower = load(entryLower);
        final GridCoverage upper = load(entryUpper);
        this.lower     = lower; // Set only when BOTH images are OK.
        this.upper     = upper;
        this.timeLower = timeLower;
        this.timeUpper = timeUpper;
    }

    /**
     * Proc�de � la lecture des images n�cessaires � l'interpolation des donn�es � la date
     * sp�cifi�e. Les images lues seront point�es par {@link #lower} et {@link #upper}. Il
     * est possible que la m�me image soit affect�e � ces deux champs, si cette m�thode
     * d�termine qu'il n'y a pas d'interpolation � faire.
     *
     * @param  date La date demand�e.
     * @return <code>true</code> si les donn�es sont pr�sentes.
     * @throws PointOutsideCoverageException si la date sp�cifi�e est
     *         en dehors de la plage de temps des donn�es disponibles.
     * @throws CannotEvaluateException Si l'op�ration a �chou�e pour
     *         une autre raison.
     */
    private boolean seek(final Date date) throws CannotEvaluateException {
        /*
         * Check if images currently loaded
         * are valid for the requested date.
         */
        final long time = date.getTime();
        if (time>=timeLower && time<=timeUpper) {
            return true;
        }
        /*
         * Currently loaded images are not valid for the
         * requested date. Search for the image to use
         * as upper bounds ({@link #upper}).
         */
        int index = Arrays.binarySearch(entries, date, COMPARATOR);
        try {
            if (index>=0) {
                /*
                 * An exact match has been found.
                 * Load only this image and exit.
                 */
                load(index);
                return true;
            }
            index = ~index; // Insertion point (note: ~ is NOT the minus sign).
            if (index == entries.length) {
                if (--index>=0) { // Does this coverage has at least 1 image?
                    /*
                     * The requested date is after the last image's central time.
                     * Maybe it is not after the last image's *end* time. Check...
                     */
                    if (entries[index].getTimeRange().contains(date)) {
                        load(index);
                        return true;
                    }
                }
                // fall through the exception at this method's end.
            } else if (index == 0) {
                /*
                 * The requested date is before the first image's central time.
                 * Maybe it is not before the first image's *start* time. Check...
                 */
                if (entries[index].getTimeRange().contains(date)) {
                    load(index);
                    return true;
                }
                // fall through the exception at this method's end.
            } else {
                /*
                 * An interpolation between two image seems possible.
                 * Checks if there is not a time lag between both.
                 */
                final CoverageEntry lowerEntry = entries[index-1];
                final CoverageEntry upperEntry = entries[index  ];
                final Range lowerRange = lowerEntry.getTimeRange();
                final Range upperRange = upperEntry.getTimeRange();
                final long  lowerEnd   = getTime(lowerRange.getMaxValue());
                final long  upperStart = getTime(upperRange.getMinValue())-1; // MIN_VALUE-1 == MAX_VALUE
                if (lowerEnd+maxTimeLag >= upperStart) {
                    if (interpolationAllowed) {
                        load(lowerEntry, upperEntry);
                    } else {
                        int nearest = index;
                        if (Math.abs(getTime(upperRange)-time) < Math.abs(time-getTime(lowerRange))) {
                            nearest++;
                        }
                        load(nearest);
                    }
                    return true;
                }
                if (lowerRange.contains(date)) {
                    load(index-1);
                    return true;
                }
                if (upperRange.contains(date)) {
                    load(index);
                    return true;
                }
                return false; // Missing data.
            }
        } catch (IOException exception) {
            throw new CannotEvaluateException(exception.getLocalizedMessage(), exception);
        }
        throw new PointOutsideCoverageException(Resources.format(ResourceKeys.ERROR_DATE_OUTSIDE_COVERAGE_$1, date));
    }

    /**
     * Returns a 2 dimensional grid coverage for the given date.
     *
     * NOTE: current implementation doesn't returns an interpolated image.
     *       We will fix that in a future version.
     *
     * @param  time The date where to evaluate.
     * @return The grid coverage at the specified time, or <code>null</code>
     *         if the requested date fall in a hole in the data.
     * @throws PointOutsideCoverageException if <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public synchronized GridCoverage getGridCoverage2D(final Date time) throws CannotEvaluateException {
        if (!seek(time)) {
            // Missing data
            return null;
        }
        if (lower == upper) {
            // No interpolation needed.
            return lower;
        }
        assert coordinateSystem.equals(lower.getCoordinateSystem(), false) : lower;
        assert coordinateSystem.equals(upper.getCoordinateSystem(), false) : upper;

        final long timeMillis = time.getTime();
        assert (timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);

        // TODO: Interpolate here: lower + ratio*(upper-lower)
        //       Cache the result; it may be reused often.

        // GridCoverage result = lower + ratio*(upper-lower)

        return (ratio < 0.5) ? lower : upper;
    }

    /**
     * Proj�te un point du syst�me de coordonn�es de cette couverture vers le syst�me
     * de l'image sp�cifi�e. Cette m�thode doit �tre utilis�e avant d'appeller une
     * m�thode <code>evaluate(...)</code> sur la couverture sp�cifi�e.
     *
     * @param  point Le point � transformer. Ce point ne sera jamais modifi�.
     * @return Le point transform�.
     * @throws CannotEvaluateException si la transformation n'a pas pu �tre faites.
     */
    private Point2D project(final Point2D point, final GridCoverage coverage) throws CannotEvaluateException {
        final CoordinateSystem targetCS = coverage.getCoordinateSystem();
        if (coordinateSystem.equals(targetCS, false)) {
            return point;
        }
        // TODO: Impl�menter la transformation de coordonn�es.
        throw new CannotEvaluateException("Syst�me de coordonn�es incompatibles.");
    }

    /**
     * Returns a sequence of integer values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link CoverageTable}:  usually bicubic for spatial axis, and
     * linear for temporal axis.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public synchronized int[] evaluate(final Point2D point, final Date time, int[] dest)
            throws CannotEvaluateException
    {
        if (!seek(time)) {
            // Missing data
            if (dest == null) {
                dest = new int[bands.length];
            }
            Arrays.fill(dest, 0, bands.length, 0);
            return dest;
        }
        assert coordinateSystem.equals(lower.getCoordinateSystem(), false) : lower;
        assert coordinateSystem.equals(upper.getCoordinateSystem(), false) : upper;
        if (lower == upper) {
            return lower.evaluate(point, dest);
        }
        int[] last=null;
        last = upper.evaluate(project(point, upper), last);
        dest = lower.evaluate(project(point, lower), dest);
        final long timeMillis = time.getTime();
        assert (timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);
        for (int i=0; i<last.length; i++) {
            dest[i] = (int)Math.round(dest[i] + ratio*(last[i]-dest[i]));
        }
        return dest;
    }

    /**
     * Returns a sequence of float values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link CoverageTable}:  usually bicubic for spatial axis, and
     * linear for temporal axis.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public synchronized float[] evaluate(final Point2D point, final Date time, float[] dest)
            throws CannotEvaluateException
    {
        if (!seek(time)) {
            // Missing data
            if (dest == null) {
                dest = new float[bands.length];
            }
            Arrays.fill(dest, 0, bands.length, Float.NaN);
            return dest;
        }
        assert coordinateSystem.equals(lower.getCoordinateSystem(), false) : lower;
        assert coordinateSystem.equals(upper.getCoordinateSystem(), false) : upper;
        if (lower == upper) {
            return lower.evaluate(point, dest);
        }
        float[] last=null;
        last = upper.evaluate(project(point, upper), last);
        dest = lower.evaluate(project(point, lower), dest);
        final long timeMillis = time.getTime();
        assert (timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);
        for (int i=0; i<last.length; i++) {
            dest[i] = (float)(dest[i] + ratio*(last[i]-dest[i]));
        }
        return dest;
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link CoverageTable}:  usually bicubic for spatial axis, and
     * linear for temporal axis.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public synchronized double[] evaluate(final Point2D point, final Date time, double[] dest)
            throws CannotEvaluateException
    {
        if (!seek(time)) {
            // Missing data
            if (dest == null) {
                dest = new double[bands.length];
            }
            Arrays.fill(dest, 0, bands.length, Double.NaN);
            return dest;
        }
        assert coordinateSystem.equals(lower.getCoordinateSystem(), false) : lower;
        assert coordinateSystem.equals(upper.getCoordinateSystem(), false) : upper;
        if (lower == upper) {
            return lower.evaluate(point, dest);
        }
        double[] last=null;
        last = upper.evaluate(project(point, upper), last);
        dest = lower.evaluate(project(point, lower), dest);
        final long timeMillis = time.getTime();
        assert (timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);
        for (int i=0; i<last.length; i++) {
            dest[i] += ratio*(last[i]-dest[i]);
        }
        return dest;
    }

    /**
     * Returns a sequence of integer values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link CoverageTable}:  usually bicubic for spatial axis, and
     * linear for temporal axis.  The coordinate system of the point is the same as the
     * grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public int[] evaluate(final CoordinatePoint coord, int[] dest) throws CannotEvaluateException {
        return evaluate(checkDimension(coord), temporalCS.toDate(coord.ord[temporalDimension]), dest);
    }

    /**
     * Returns a sequence of float values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link CoverageTable}:  usually bicubic for spatial axis, and
     * linear for temporal axis.  The coordinate system of the point is the same as the
     * grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public float[] evaluate(final CoordinatePoint coord, float[] dest) throws CannotEvaluateException {
        return evaluate(checkDimension(coord), temporalCS.toDate(coord.ord[temporalDimension]), dest);
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link CoverageTable}:  usually bicubic for spatial axis, and
     * linear for temporal axis.  The coordinate system of the point is the same as the
     * grid coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     * @throws CannotEvaluateException if the computation failed for some other reason.
     */
    public double[] evaluate(final CoordinatePoint coord, final double[] dest) throws CannotEvaluateException {
        return evaluate(checkDimension(coord), temporalCS.toDate(coord.ord[temporalDimension]), dest);
    }

    /**
     * V�rifie que le point sp�cifi� a bien la dimension attendue.
     *
     * @param  coord Coordonn�e du point dont on veut v�rifier la dimension.
     * @throws MismatchedDimensionException si le point n'a pas la dimension attendue.
     */
    private final Point2D checkDimension(final CoordinatePoint coord) throws MismatchedDimensionException {
        if (coord.getDimension() != coordinateSystem.getDimension()) {
            throw new MismatchedDimensionException(coord, coordinateSystem);
        }
        return new Point2D.Double(coord.ord[temporalDimension!=0 ? 0 : 1],
                                  coord.ord[temporalDimension>=2 ? 1 : 2]);
    }

    /**
     * Indique si cet objet est autoris� � interpoller dans l'espace et dans le temps.
     * La valeur par d�faut est <code>true</code>.
     */
    public boolean isInterpolationAllowed() {
        return interpolationAllowed;
    }

    /**
     * Sp�cifie si cet objet est autoris� � interpoller dans l'espace et dans le temps.
     * La valeur par d�faut est <code>true</code>.
     */
    public synchronized void setInterpolationAllowed(final boolean flag) {
        lower     = null;
        upper     = null;
        timeLower = Long.MAX_VALUE;
        timeUpper = Long.MIN_VALUE;
        interpolationAllowed = flag;
    }

    /**
     * Adds an {@link IIOReadWarningListener} to
     * the list of registered warning listeners.
     */
    public void addIIOReadWarningListener(final IIOReadWarningListener listener) {
        listeners.add(IIOReadWarningListener.class, listener);
    }

    /**
     * Removes an {@link IIOReadWarningListener} from
     * the list of registered warning listeners.
     */
    public void removeIIOReadWarningListener(final IIOReadWarningListener listener) {
        listeners.remove(IIOReadWarningListener.class, listener);
    }

    /**
     * Adds an {@link IIOReadProgressListener} to
     * the list of registered progress listeners.
     */
    public void addIIOReadProgressListener(final IIOReadProgressListener listener) {
        listeners.add(IIOReadProgressListener.class, listener);
    }

    /**
     * Removes an {@link IIOReadProgressListener} from
     * the list of registered progress listeners.
     */
    public void removeIIOReadProgressListener(final IIOReadProgressListener listener) {
        listeners.remove(IIOReadProgressListener.class, listener);
    }

    /**
     * Enregistre un message vers le journal des �v�nements.
     */
    protected void log(final LogRecord record) {
        CoverageDataBase.LOGGER.log(record);
    }

    /**
     * Pr�pare un enregistrement pour le journal.
     */
    private void log(final int cl�, final Object[] parameters) {
        final Locale locale = null;
        final LogRecord record = Resources.getResources(locale).getLogRecord(Level.INFO, cl�);
        record.setSourceClassName("Coverage3D");
        record.setSourceMethodName("evaluate");
        record.setParameters(parameters);
        if (readListener == null) {
            readListener = new Listeners();
            addIIOReadProgressListener(readListener);
        }
        readListener.record = record;
    }

    /**
     * Objet ayant la charge de suivre le chargement d'une image. Cet objet sert
     * surtout � enregistrer dans le journal un enregistrement indiquant que la
     * lecture d'une image a commenc�.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Listeners extends IIOReadProgressAdapter {
        /**
         * The record to log.
         */
        public LogRecord record;

        /**
         * Reports that an image read operation is beginning.
         */
        public void imageStarted(ImageReader source, int imageIndex) {
            if (record != null) {
                log(record);
                source.removeIIOReadProgressListener(this);
                record = null;
            }
        }
    }
}
