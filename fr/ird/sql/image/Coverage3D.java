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

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.pt.MismatchedDimensionException;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.ct.MathTransform;
import net.seas.opengis.ct.TransformException;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.gc.GridRange;
import net.seas.opengis.gc.GridGeometry;
import net.seas.opengis.gc.GridCoverage;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cv.Coverage;
import net.seas.opengis.cv.CategoryList;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.cv.ColorInterpretation;
import net.seas.opengis.cv.PointOutsideCoverageException;

// Requêtes SQL et entrés/sorties
import java.sql.SQLException;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.imageio.ImageReader;

// Evénements
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import net.seas.image.io.IIOReadProgressAdapter;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Divers
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.awt.geom.Point2D;
import javax.media.jai.util.Range;
import fr.ird.resources.Resources;


/**
 * Enveloppe une table d'images comme s'il s'agissait d'un espace à trois dimensions, la
 * troisième dimension étant le temps.  Cette classe offre une façon pratique d'extraire
 * des valeurs à des positions et des dates arbitraires. Les valeurs sont interpollées à
 * la fois dans l'espace et dans le temps.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Coverage3D extends Coverage
{
    /**
     * Liste des images à prendre en compte.
     */
    private final ImageEntry[] entries;

    /**
     * Listes de catégories des images.
     */
    private final CategoryList[] categories;

    /**
     * Les {@link SampleDimension} pour cet objet. Cette
     * liste ne sera construite que la première fois où
     * elle sera demandée.
     */
    private transient List<SampleDimension> dimensions;

    /**
     * Enveloppe des données englobées par cet objet.
     */
    private final Envelope envelope;

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
     * Initialize fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
    {
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
    public Coverage3D(final ImageTable table) throws SQLException
    {
        super(table.getSeries().getName(), table.getCoordinateSystem(), null, null);
        final List<ImageEntry> entryList = table.getEntries();
        this.entries    = entryList.toArray(new ImageEntry[entryList.size()]);
        this.envelope   = table.getEnvelope();
        this.categories = (entries.length!=0) ? entries[0].getCategoryLists() : new CategoryList[0];
        for (int i=1; i<entries.length; i++)
            if (!Arrays.equals(categories, entries[i].getCategoryLists()))
                throw new SQLException(Resources.format(Clé.CATEGORIES_MITMATCH));
        Arrays.sort(entries, COMPARATOR);
    }

    /**
     * Comparateur à utiliser pour classer les images et effectuer
     * des recherches rapides. Ce comparateur utilise la date du
     * milieu comme critère.
     */
    private static final Comparator<Object> COMPARATOR = new Comparator<Object>()
    {
        public int compare(final Object entry1, final Object entry2)
        {
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
    private static long getTime(final Object object)
    {
        if (object instanceof Date)
        {
            return ((Date) object).getTime();
        }
        if (object instanceof ImageEntry)
        {
            return getTime((ImageEntry) object);
        }
        return Long.MIN_VALUE;
    }

    /**
     * Retourne la date du milieu de l'image spécifiée.  Si l'image ne couvre aucune
     * plage de temps (par exemple s'il s'agit de données qui ne varient pas avec le
     * temps, comme la bathymétrie), alors cette méthode retourne {@link Long#MIN_VALUE}.
     */
    private static long getTime(final ImageEntry entry)
    {
        final Range timeRange = entry.getTimeRange();
        if (timeRange!=null)
        {
            final Date startTime = (Date) timeRange.getMinValue();
            final Date   endTime = (Date) timeRange.getMaxValue();
            if (startTime!=null)
            {
                if (endTime!=null)
                {
                    return (endTime.getTime()+startTime.getTime())/2;
                }
                else return startTime.getTime();
            }
            else if (endTime!=null)
            {
                return endTime.getTime();
            }
        }
        return Long.MIN_VALUE;
    }

    /**
     * Returns The bounding box for the coverage
     * domain in coordinate system coordinates.
     */
    public Envelope getEnvelope()
    {return envelope.clone();}

    /**
     * Retrieve sample dimension information for the coverage.
     * For a grid coverage, a sample dimension is a band. The sample dimension information
     * include such things as description, data type of the value (bit, byte, integer...),
     * the no data values, minimum and maximum values and a color table if one is associated
     * with the dimension.
     */
    public synchronized List<SampleDimension> getSampleDimensions()
    {
        if (dimensions==null)
        {
            final SampleDimension[] array = new SampleDimension[categories.length];
            for (int i=0; i<array.length; i++)
            {
                array[i] = new Dimension(categories[i]);
            }
            dimensions = Collections.unmodifiableList(Arrays.asList(array));
        }
        return dimensions;
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
    public void snap(final Point2D point, final Date date) // No synchronization needed.
    {
        int index = Arrays.binarySearch(entries, date, COMPARATOR);
        if (index<0)
        {
            /*
             * There is no exact match for the date.
             * Snap the date to the closest image.
             */
            index = ~index;
            if (index==entries.length)
            {
                if (index==0) return; // No entries in this coverage!
                index--;
            }
            long time;
            if (index>=1)
            {
                time = date.getTime();
                final long lowerTime = getTime(entries[index-1]);
                final long upperTime = getTime(entries[index])-1; // Long.MIN_VALUE-1 == Long.MAX_VALUE
                assert(time>lowerTime && time<upperTime);
                if (time-lowerTime < upperTime-time)
                {
                    index--;
                    time = lowerTime;
                }
                else time = upperTime+1;
            }
            else
            {
                time = getTime(entries[index]);
            }
            if (time!=Long.MIN_VALUE && time!=Long.MAX_VALUE)
            {
                date.setTime(time);
            }
        }
        /*
         * Now that we know the image entry,
         * snap the spatial coordinate point.
         */
        if (point!=null) try
        {
            // TODO: Next line assume we are using the default table implementation.
            assert(coordinateSystem.equivalents(entries[index].getCoordinateSystem()));
            CoordinatePoint    coordinate = new CoordinatePoint(point.getX(), point.getY(), ImageTableImpl.toJulian(date.getTime()));
            final GridGeometry   geometry = entries[index].getGridGeometry();
            final GridRange         range = geometry.getGridRange();
            final MathTransform transform = geometry.getGridToCoordinateSystem();
            coordinate = transform.inverse().transform(coordinate, coordinate);
            for (int i=coordinate.getDimension(); --i>=0;)
            {
                coordinate.ord[i] = Math.max(range.getLower(i),
                                    Math.min(range.getUpper(i)-1,
                                    (int)Math.rint(coordinate.ord[i])));
            }
            coordinate = transform.transform(coordinate, coordinate);
            point.setLocation(coordinate.ord[0], coordinate.ord[1]);
        }
        catch (TransformException exception)
        {
            PointOutsideCoverageException e=new PointOutsideCoverageException(point);
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Prépare un enregistrement pour le journal.
     */
    private void log(final int clé, final Object[] parameters)
    {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE, clé);
        record.setSourceClassName("Coverage3D");
        record.setSourceMethodName("evaluate");
        record.setParameters(parameters);
        if (readListener==null)
        {
            readListener = new Listeners();
            addIIOReadProgressListener(readListener);
        }
        readListener.record = record;
    }

    /**
     * Loads a single image at the given index.
     *
     * @param  index Index in {@link #entries} for the image to load.
     * @throws IOException if an error occured while loading image.
     */
    private void load(final int index) throws IOException
    {
        final ImageEntry entry = entries[index];
        log(Clé.LOAD_IMAGE¤1, new Object[]{entry});
        lower = upper = entry.getImage(listeners);
        timeLower = timeUpper = getTime(entry);
    }

    /**
     * Loads images for the given entries.
     *
     * @throws IOException if an error occured while loading images.
     */
    private void load(final ImageEntry entryLower, final ImageEntry entryUpper) throws IOException
    {
        final long timeLower = getTime(entryLower);
        final long timeUpper = getTime(entryUpper);
        log(Clé.LOAD_IMAGES¤2, new Object[]{entryLower, entryUpper});
        final GridCoverage lower = entryLower.getImage(listeners);
        final GridCoverage upper = entryUpper.getImage(listeners);
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
    private boolean seek(final Date date) throws PointOutsideCoverageException
    {
        /*
         * Check if images currently loaded
         * are valid for the requested date.
         */
        final long time = date.getTime();
        if (time>=timeLower && time<=timeUpper)
        {
            return true;
        }
        /*
         * Currently loaded images are not valid for the
         * requested date. Search for the image to use
         * as upper bounds ({@link #upper}).
         */
        int index = Arrays.binarySearch(entries, date, COMPARATOR);
        try
        {
            if (index>=0)
            {
                /*
                 * An exact match has been found.
                 * Load only this image and exit.
                 */
                load(index);
                return true;
            }
            index = ~index; // Insertion point (note: ~ is NOT the minus sign).
            if (index==entries.length)
            {
                if (--index>=0) // Does this coverage has at least 1 image?
                {
                    /*
                     * The requested date is after the last image's central time.
                     * Maybe it is not after the last image's *end* time. Check...
                     */
                    if (entries[index].getTimeRange().contains(date))
                    {
                        load(index);
                        return true;
                    }
                }
                // fall through the exception at this method's end.
            }
            else if (index==0)
            {
                /*
                 * The requested date is before the first image's central time.
                 * Maybe it is not before the first image's *start* time. Check...
                 */
                if (entries[index].getTimeRange().contains(date))
                {
                    load(index);
                    return true;
                }
                // fall through the exception at this method's end.
            }
            else
            {
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
                if (lowerEnd+maxTimeLag >= upperStart)
                {
                    load(lowerEntry, upperEntry);
                    return true;
                }
                if (lowerRange.contains(date))
                {
                    load(index-1);
                    return true;
                }
                if (upperRange.contains(date))
                {
                    load(index);
                    return true;
                }
                return false; // Missing data.
            }
        }
        catch (IOException exception)
        {
            PointOutsideCoverageException e=new PointOutsideCoverageException(Resources.format(Clé.DATE_OUTSIDE_COVERAGE¤1, date));
            e.initCause(exception);
            throw e;
        }
        throw new PointOutsideCoverageException(Resources.format(Clé.DATE_OUTSIDE_COVERAGE¤1, date));
    }

    /**
     * Return an sequence of integer values for a given point in the coverage.
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
        if (!seek(time))
        {
            // Missing data
            if (dest==null) dest=new int[categories.length];
            Arrays.fill(dest, 0, categories.length, 0);
            return dest;
        }
        assert(coordinateSystem.equivalents(lower.getCoordinateSystem())) : lower;
        assert(coordinateSystem.equivalents(upper.getCoordinateSystem())) : upper;
        if (lower==upper)
        {
            return lower.evaluate(point, dest);
        }

        int[] last=null;
        last = upper.evaluate(point, last);
        dest = lower.evaluate(point, dest);
        final long timeMillis = time.getTime();
        assert(timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);
        for (int i=0; i<last.length; i++)
        {
            dest[i] = (int)Math.round(dest[i] + ratio*(last[i]-dest[i]));
        }
        return dest;
    }

    /**
     * Return an sequence of float values for a given point in the coverage.
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
        if (!seek(time))
        {
            // Missing data
            if (dest==null) dest=new float[categories.length];
            Arrays.fill(dest, 0, categories.length, Float.NaN);
            return dest;
        }
        assert(coordinateSystem.equivalents(lower.getCoordinateSystem())) : lower;
        assert(coordinateSystem.equivalents(upper.getCoordinateSystem())) : upper;
        if (lower==upper)
        {
            return lower.evaluate(point, dest);
        }

        float[] last=null;
        last = upper.evaluate(point, last);
        dest = lower.evaluate(point, dest);
        final long timeMillis = time.getTime();
        assert(timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);
        for (int i=0; i<last.length; i++)
        {
            dest[i] = (float)(dest[i] + ratio*(last[i]-dest[i]));
        }
        return dest;
    }

    /**
     * Return an sequence of double values for a given point in the coverage.
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
        if (!seek(time))
        {
            // Missing data
            if (dest==null) dest=new double[categories.length];
            Arrays.fill(dest, 0, categories.length, Double.NaN);
            return dest;
        }
        assert(coordinateSystem.equivalents(lower.getCoordinateSystem())) : lower;
        assert(coordinateSystem.equivalents(upper.getCoordinateSystem())) : upper;
        if (lower==upper)
        {
            return lower.evaluate(point, dest);
        }

        double[] last=null;
        last = upper.evaluate(point, last);
        dest = lower.evaluate(point, dest);
        final long timeMillis = time.getTime();
        assert(timeMillis>=timeLower && timeMillis<=timeUpper) : time;
        final double ratio = (double)(timeMillis-timeLower) / (double)(timeUpper-timeLower);
        for (int i=0; i<last.length; i++)
        {
            dest[i] += ratio*(last[i]-dest[i]);
        }
        return dest;
    }

    /**
     * Return an sequence of integer values for a given point in the coverage.
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
     * Return an sequence of float values for a given point in the coverage.
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
     * Return an sequence of double values for a given point in the coverage.
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
        if (coord.getDimension()!=coordinateSystem.getDimension())
            throw new MismatchedDimensionException(coord, coordinateSystem);
    }

    /**
     * Adds an {@link IIOReadWarningListener} to
     * the list of registered warning listeners.
     */
    public void addIIOReadWarningListener(final IIOReadWarningListener listener)
    {listeners.add(IIOReadWarningListener.class, listener);}

    /**
     * Removes an {@link IIOReadWarningListener} from
     * the list of registered warning listeners.
     */
    public void removeIIOReadWarningListener(final IIOReadWarningListener listener)
    {listeners.remove(IIOReadWarningListener.class, listener);}

    /**
     * Adds an {@link IIOReadProgressListener} to
     * the list of registered progress listeners.
     */
    public void addIIOReadProgressListener(final IIOReadProgressListener listener)
    {listeners.add(IIOReadProgressListener.class, listener);}

    /**
     * Removes an {@link IIOReadProgressListener} from
     * the list of registered progress listeners.
     */
    public void removeIIOReadProgressListener(final IIOReadProgressListener listener)
    {listeners.remove(IIOReadProgressListener.class, listener);}

    /**
     * Objet ayant la charge de suivre le chargement d'une image. Cet objet sert
     * surtout à enregistrer dans le journal un enregistrement indiquant que la
     * lecture d'une image a commencé.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Listeners extends IIOReadProgressAdapter
    {
        /**
         * The record to log.
         */
        public LogRecord record;

        /**
         * Reports that an image read operation is beginning.
         */
        public void imageStarted(ImageReader source, int imageIndex)
        {
            if (record!=null)
            {
                Table.logger.log(record);
                source.removeIIOReadProgressListener(this);
                record=null;
            }
        }
    }




    /**
     * Sample dimension for {@link Coverage3D}.
     *
     * @version 1.00
     * @author Martin Desruisseaux
     */
    private static final class Dimension extends SampleDimension
    {
        /**
         * Construct a sample dimension with a set of categories.
         *
         * @param categories The category list for this sample dimension, or
         *        <code>null</code> if this sample dimension has no category.
         */
        public Dimension(final CategoryList categories)
        {super(categories);}

        /**
         * Returns the color interpretation of the sample dimension.
         * Since {@link CategoryList} are designed for indexed color
         * models, current implementation returns {@link ColorInterpretation#PALETTE_INDEX}.
         * We need to find a more general way in some future version.
         */
        public ColorInterpretation getColorInterpretation()
        {return ColorInterpretation.PALETTE_INDEX;}

        /**
         * Returns the minimum value occurring in this sample dimension.
         */
        public double getMinimumValue()
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Returns the maximum value occurring in this sample dimension.
         */
        public double getMaximumValue()
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Determine the mode grid value in this sample dimension.
         */
        public double getModeValue()
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Determine the median grid value in this sample dimension.
         */
        public double getMedianValue()
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Determine the mean grid value in this sample dimension.
         */
        public double getMeanValue()
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Determine the standard deviation from the mean
         * of the grid values in this sample dimension.
         */
        public double getStandardDeviation()
        {throw new UnsupportedOperationException("Not implemented");}
    }
}
