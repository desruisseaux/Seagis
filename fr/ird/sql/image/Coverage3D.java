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

// Geotools dependencies (CTS)
import org.geotools.pt.Envelope;
import org.geotools.pt.CoordinatePoint;
import org.geotools.pt.MismatchedDimensionException;
import org.geotools.ct.MathTransform;
import org.geotools.ct.TransformException;

// Geotools dependencies (GCS)
import org.geotools.gc.GridRange;
import org.geotools.gc.GridGeometry;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.Coverage;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.ColorInterpretation;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.gp.GridCoverageProcessor;

// Géométrie
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

// Requêtes SQL et entrés/sorties
import java.sql.SQLException;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.imageio.ImageReader;

// Evénements
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import org.geotools.io.image.IIOReadProgressAdapter;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Divers
import java.util.Date;
import javax.media.jai.util.Range;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Enveloppe une table d'images comme s'il s'agissait d'un espace à trois dimensions, la
 * troisième dimension étant le temps.  Cette classe offre une façon pratique d'extraire
 * des valeurs à des positions et des dates arbitraires. Les valeurs sont interpollées à
 * la fois dans l'espace et dans le temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Coverage3D extends Coverage
{
    /**
     * Liste des images à prendre en compte.
     */
    private final ImageEntry[] entries;

    /**
     * Listes des bandes des images.
     */
    private final SampleDimension[] bands;

    /**
     * Enveloppe des données englobées par cet objet.
     */
    private final Envelope envelope;

    /**
     * Indique si les interpolations sont permises.
     */
    private boolean interpolationAllowed = true;

    /**
     * Intervalle de temps maximal toléré entre la fin d'une image et
     * le début de l'image suivante, en nombre de millisecondes. Si un
     * intervalle de temps supérieur sépare deux images, on considèrera
     * qu'on a un trou dans les données.
     */
    private final long maxTimeLag = 0;

    /**
     * Liste des objets intéressés à être informés
     * des progrès de la lecture des images.
     */
    private final EventListenerList listeners = new EventListenerList();

    /**
     * Internal listener for logging image loading.
     */
    private transient Listeners readListener;

    /**
     * Données dont la date de début est inférieure ou égale à la date demandée.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit inférieure ou égale à la date demandée (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage lower;

    /**
     * Données dont la date de fin  est supérieure ou égale à la date demandée.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit supérieure ou égale à la date demandée (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage upper;

    /**
     * Date et heure du milieu des données {@link #lower} et {@link #upper},
     * en nombre de millisecondes écoulées depuis le 1er janvier 1970 UTC.
     */
    private transient long timeLower=Long.MAX_VALUE, timeUpper=Long.MIN_VALUE;

    /**
     * L'objet à utiliser pour effectuer des opérations sur les images
     * (notamment modifier les interpolations). Ne sera construit que
     * la première fois où il sera nécessaire.
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
     * Construit une couverture à partir des données de la table spécifiée.
     * La entrées {@link ImageEntry} seront mémorisées immediatement. Toute
     * modification faite à la table après la construction de cet objet
     * <code>Coverage3D</code> (incluant la fermeture de la table) n'auront
     * aucun effet sur cet objet.
     *
     * @param  table Table d'où proviennent les données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public Coverage3D(final ImageTable table) throws SQLException {
        super(table.getSeries().getName(), table.getCoordinateSystem(), null, null);
        final List<ImageEntry> entryList = table.getEntries();
        this.entries  = entryList.toArray(new ImageEntry[entryList.size()]);
        this.envelope = table.getEnvelope();
        this.bands    = (entries.length!=0) ? entries[0].getSampleDimensions() : new SampleDimension[0];
        for (int i=1; i<entries.length; i++) {
            if (!Arrays.equals(bands, entries[i].getSampleDimensions())) {
                throw new SQLException(Resources.format(ResourceKeys.ERROR_CATEGORIES_MITMATCH));
            }
        }
        Arrays.sort(entries, COMPARATOR);
    }

    /**
     * Comparateur à utiliser pour classer les images et effectuer
     * des recherches rapides. Ce comparateur utilise la date du
     * milieu comme critère.
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
     * Retourne la date de l'objet spécifiée.  L'argument peut être un objet
     * {@link Date} ou {@link ImageEntry}. Dans ce dernier cas, la date sera
     * extraite avec {@link #getTime}.
     */
    private static long getTime(final Object object) {
        if (object instanceof Date) {
            return ((Date) object).getTime();
        }
        if (object instanceof ImageEntry) {
            return getTime((ImageEntry) object);
        }
        return Long.MIN_VALUE;
    }

    /**
     * Retourne la date du milieu de l'image spécifiée.  Si l'image ne couvre aucune
     * plage de temps (par exemple s'il s'agit de données qui ne varient pas avec le
     * temps, comme la bathymétrie), alors cette méthode retourne {@link Long#MIN_VALUE}.
     */
    private static long getTime(final ImageEntry entry) {
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
     * Returns The bounding box for the coverage
     * domain in coordinate system coordinates.
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
     * argument is then set to this pixel center.
     * <br><br>
     * Calling any <code>evaluate</code> method with snapped coordinates will
     * returns non-interpolated value.
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
            // TODO: Next line assume we are using the default table implementation.
            assert coordinateSystem.equivalents(entries[index].getCoordinateSystem());
            CoordinatePoint    coordinate = new CoordinatePoint(point.getX(), point.getY(), ImageTableImpl.toJulian(date.getTime()));
            final GridGeometry   geometry = entries[index].getGridGeometry();
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
            PointOutsideCoverageException e=new PointOutsideCoverageException(point);
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Prépare un enregistrement pour le journal.
     */
    private void log(final int clé, final Object[] parameters) {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE, clé);
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
     * Load a single image for the specified image entry.
     *
     * @param  entry The image to load.
     * @return The loaded image.
     * @throws IOException if an error occured while loading image.
     */
    private GridCoverage load(final ImageEntry entry) throws IOException {
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
        final ImageEntry entry = entries[index];
        log(ResourceKeys.LOADING_IMAGE_$1, new Object[]{entry});
        lower = upper = load(entry);
        timeLower = timeUpper = getTime(entry);
    }

    /**
     * Loads images for the given entries.
     *
     * @throws IOException if an error occured while loading images.
     */
    private void load(final ImageEntry entryLower, final ImageEntry entryUpper) throws IOException {
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
     * Procède à la lecture des images nécessaires à l'interpolation des données à la date
     * spécifiée. Les images lues seront pointées par {@link #lower} et {@link #upper}. Il
     * est possible que la même image soit affectée à ces deux champs, si cette méthode
     * détermine qu'il n'y a pas d'interpolation à faire.
     *
     * @param  date La date demandée.
     * @return <code>true</code> si les données sont présentes.
     * @throws PointOutsideCoverageException si la date spécifiée est
     *         en dehors de la plage de temps des données disponibles.
     */
    private boolean seek(final Date date) throws PointOutsideCoverageException {
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
                final ImageEntry lowerEntry = entries[index-1];
                final ImageEntry upperEntry = entries[index  ];
                final Range      lowerRange = lowerEntry.getTimeRange();
                final Range      upperRange = upperEntry.getTimeRange();
                final long lowerEnd   = getTime(lowerRange.getMaxValue());
                final long upperStart = getTime(upperRange.getMinValue())-1; // MIN_VALUE-1 == MAX_VALUE
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
            PointOutsideCoverageException e=new PointOutsideCoverageException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
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
     */
    public synchronized GridCoverage getGridCoverage2D(final Date time) throws PointOutsideCoverageException {
        if (!seek(time)) {
            // Missing data
            return null;
        }
        if (lower == upper) {
            // No interpolation needed.
            return lower;
        }
        assert coordinateSystem.equivalents(lower.getCoordinateSystem()) : lower;
        assert coordinateSystem.equivalents(upper.getCoordinateSystem()) : upper;

        final long timeMillis = time.getTime();
        assert (timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);

        // TODO: Interpolate here: lower + ratio*(upper-lower)
        //       Cache the result; it may be reused often.

        // GridCoverage result = lower + ratio*(upper-lower)

        return (ratio < 0.5) ? lower : upper;
    }

    /**
     * Returns a sequence of integer values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link ImageTable}: usually bicubic for spatial axis, and linear
     * for temporal axis.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     */
    public synchronized int[] evaluate(final Point2D point, final Date time, int[] dest) throws PointOutsideCoverageException
    {
        if (!seek(time)) {
            // Missing data
            if (dest == null) {
                dest = new int[bands.length];
            }
            Arrays.fill(dest, 0, bands.length, 0);
            return dest;
        }
        assert coordinateSystem.equivalents(lower.getCoordinateSystem()) : lower;
        assert coordinateSystem.equivalents(upper.getCoordinateSystem()) : upper;
        if (lower == upper) {
            return lower.evaluate(point, dest);
        }
        int[] last=null;
        last = upper.evaluate(point, last);
        dest = lower.evaluate(point, dest);
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
     * inherited from {@link ImageTable}: usually bicubic for spatial axis, and linear
     * for temporal axis.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     */
    public synchronized float[] evaluate(final Point2D point, final Date time, float[] dest) throws PointOutsideCoverageException
    {
        if (!seek(time)) {
            // Missing data
            if (dest == null) {
                dest = new float[bands.length];
            }
            Arrays.fill(dest, 0, bands.length, Float.NaN);
            return dest;
        }
        assert coordinateSystem.equivalents(lower.getCoordinateSystem()) : lower;
        assert coordinateSystem.equivalents(upper.getCoordinateSystem()) : upper;
        if (lower == upper) {
            return lower.evaluate(point, dest);
        }
        float[] last=null;
        last = upper.evaluate(point, last);
        dest = lower.evaluate(point, dest);
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
     * inherited from {@link ImageTable}: usually bicubic for spatial axis, and linear
     * for temporal axis.
     *
     * @param  point The coordinate point where to evaluate.
     * @param  time  The date where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>point</code> or <code>time</code> is outside coverage.
     */
    public synchronized double[] evaluate(final Point2D point, final Date time, double[] dest) throws PointOutsideCoverageException
    {
        if (!seek(time)) {
            // Missing data
            if (dest == null) {
                dest = new double[bands.length];
            }
            Arrays.fill(dest, 0, bands.length, Double.NaN);
            return dest;
        }
        assert coordinateSystem.equivalents(lower.getCoordinateSystem()) : lower;
        assert coordinateSystem.equivalents(upper.getCoordinateSystem()) : upper;
        if (lower == upper) {
            return lower.evaluate(point, dest);
        }
        double[] last=null;
        last = upper.evaluate(point, last);
        dest = lower.evaluate(point, dest);
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
     * inherited from {@link ImageTable}: usually bicubic for spatial axis, and linear
     * for temporal axis. The coordinate system of the point is the same as the grid
     * coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public int[] evaluate(final CoordinatePoint coord, int[] dest) throws PointOutsideCoverageException
    {
        checkDimension(coord);
        // TODO: Current implementation doesn't check the coordinate system.
        return evaluate(new Point2D.Double(coord.ord[0], coord.ord[1]), ImageTableImpl.toDate(coord.ord[2]), dest);
    }

    /**
     * Returns a sequence of float values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link ImageTable}: usually bicubic for spatial axis, and linear
     * for temporal axis. The coordinate system of the point is the same as the grid
     * coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public float[] evaluate(final CoordinatePoint coord, float[] dest) throws PointOutsideCoverageException
    {
        checkDimension(coord);
        // TODO: Current implementation doesn't check the coordinate system.
        return evaluate(new Point2D.Double(coord.ord[0], coord.ord[1]), ImageTableImpl.toDate(coord.ord[2]), dest);
    }

    /**
     * Returns a sequence of double values for a given point in the coverage.
     * A value for each sample dimension is included in the sequence. The interpolation
     * type used when accessing grid values for points which fall between grid cells is
     * inherited from {@link ImageTable}: usually bicubic for spatial axis, and linear
     * for temporal axis. The coordinate system of the point is the same as the grid
     * coverage coordinate system.
     *
     * @param  coord The coordinate point where to evaluate.
     * @param  dest  An array in which to store values, or <code>null</code> to
     *               create a new array. If non-null, this array must be at least
     *               <code>{@link #getSampleDimensions()}.size()</code> long.
     * @return The <code>dest</code> array, or a newly created array if <code>dest</code> was null.
     * @throws PointOutsideCoverageException if <code>coord</code> is outside coverage.
     */
    public double[] evaluate(final CoordinatePoint coord, final double[] dest) throws PointOutsideCoverageException
    {
        checkDimension(coord);
        // TODO: Current implementation doesn't check the coordinate system.
        return evaluate(new Point2D.Double(coord.ord[0], coord.ord[1]), ImageTableImpl.toDate(coord.ord[2]), dest);
    }

    /**
     * Vérifie que le point spécifié a bien la dimension attendue.
     *
     * @param  coord Coordonnée du point dont on veut vérifier la dimension.
     * @throws MismatchedDimensionException si le point n'a pas la dimension attendue.
     */
    private final void checkDimension(final CoordinatePoint coord) throws MismatchedDimensionException
    {
        if (coord.getDimension() != coordinateSystem.getDimension()) {
            throw new MismatchedDimensionException(coord, coordinateSystem);
        }
    }

    /**
     * Indique si cet objet est autorisé à interpoller dans l'espace et dans le temps.
     * La valeur par défaut est <code>true</code>.
     */
    public boolean isInterpolationAllowed() {
        return interpolationAllowed;
    }

    /**
     * Spécifie si cet objet est autorisé à interpoller dans l'espace et dans le temps.
     * La valeur par défaut est <code>true</code>.
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
     * Objet ayant la charge de suivre le chargement d'une image. Cet objet sert
     * surtout à enregistrer dans le journal un enregistrement indiquant que la
     * lecture d'une image a commencé.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Listeners extends IIOReadProgressAdapter {
        /**
         * The record to log.
         */
        public LogRecord record;

        /**
         * Reports that an image read operation is beginning.
         */
        public void imageStarted(ImageReader source, int imageIndex) {
            if (record!=null) {
                Table.logger.log(record);
                source.removeIIOReadProgressListener(this);
                record=null;
            }
        }
    }
}
