/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.map.io;

// Map components
import fr.ird.map.Isoline;
import org.geotools.ct.TransformException;

// Collections
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;

// References
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

// Input/output
import java.net.URI;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.net.URISyntaxException;

// Compression
import java.util.zip.CRC32;
import java.util.zip.ZipFile;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.CheckedOutputStream;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Resources
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.resources.Utilities;


/**
 * A factory class for {@link Isoline} objects. Isolines are loaded
 * using the specified  {@link IsolineReader}  and cached in memory
 * using soft references. <code>IsolineFactory</code> make sure that
 * an isoline readed twice will not result in two copies of the same
 * data in memory. Additionally, <code>IsolineFactory</code> can
 * save isolines in a serialized form for faster loading.
 *
 * @version $Id$
 * @author  Martin Desruisseaux
 */
public abstract class IsolineFactory
{
    /**
     * The logger for warning and information messages.
     */
    private static final Logger logger = Logger.getLogger("fr.ird.map");

    /**
     * Path to serialized {@link Isoline}s as a file. The first time an
     * isoline is requested,  all isolines will be read and serizalized
     * to this cache file. Subsequent calls will fetch isolines from this
     * file. This field may be <code>null</code> if the cache is not
     * accessible as a file.
     */
    private final File cacheFile;

    /**
     * Path to serialized {@link Isoline}s as an URL. This field may
     * be <code>null</code> if the cache is accessible as a file,
     * which is more efficient than an URL.
     */
    private final URL cacheURL;

    /**
     * The set of previously loaded {@link Isolines},  or <code>null</code> if no
     * isolines has been loaded yet. When this map is first constructed, keys are
     * set to all isolone values available but values may still <code>null</code>
     * as long as the isoline has not been really loaded.
     */
    private Map<Float,Reference<Isoline>> isolines;

    /**
     * Construct an <code>IsolineFactory</code>.
     *
     * @param cache Path to serialized {@link Isoline}s as a file. The first time an
     *              isoline is requested,  all isolines will be read and serizalized
     *              to this cache file. Subsequent calls will fetch isolines from this
     *              file.
     */
    public IsolineFactory(final File cache)
    {
        this.cacheFile = cache;
        this.cacheURL  = null;
    }

    /**
     * Construct an <code>IsolineFactory</code>. If the specified url
     * can be read as a file, then this constructor is equivalents to
     * the <code>IsolineFactory(File)</code> constructor.
     *
     * @param cache Path to serialized {@link Isoline}s as an URL.
     */
    public IsolineFactory(URL cache)
    {
        File file = null;
        try
        {
            file  = new File(new URI(cache.toExternalForm()));
            cache = null;
        }
        catch (URISyntaxException exception)
        {
            // Can't convert the URL into an URI.
            // It is probably not a file.
        }
        catch (IllegalArgumentException exception)
        {
            // The URI is not a file.
        }
        this.cacheFile = file;
        this.cacheURL  = cache;
    }

    /**
     * Parse an input source and read all isolines that it contains.
     * This method is invoked the first time the user calls {@link #get}.
     * Typical implementation will looks like this:
     *
     * <blockquote><pre>
     *  final GEBCOReader reader = new GEBCOReader();
     *  reader.setInput(new File("myInputFile.asc"));
     *  return reader.read();
     * </pre></blockquote>
     *
     * @return All parsed isolines.
     * @throws IOException if the reader can't be created.
     */
    protected abstract Isoline[] readAll() throws IOException;

    /**
     * Returns an entry name for the specified value.
     * Those name will be used for entry in the ZIP file.
     */
    private static String getName(final float value)
    {return Float.toString(value).replace('.',',');}

    /**
     * Gets all values currently available.
     *
     * @return All values currently availables in increasing order.
     * @throws IOException if the operation require an I/O
     *         access and this access failed.
     */
    public synchronized float[] getAvailableValues() throws IOException
    {
        if (isolines==null)
        {
            /*
             * Open and close the connection in order to construct
             * the memory cache now,  but do not read isoline data
             * since they are not needed at this stage.
             */
            close(open("getAvailableValues"));
            if (isolines==null)
            {
                return new float[0];
            }
        }
        int count = 0;
        final float[] values = new float[isolines.size()];
        for (final Iterator<Float> it=isolines.keySet().iterator(); it.hasNext();)
        {
            values[count++] = it.next().floatValue();
        }
        if (count != values.length)
        {
            // Should not happen
            throw new AssertionError(values.length - count);
        }
        Arrays.sort(values);
        return values;
    }

    /**
     * Gets the isoline for the specified value.
     *
     * @param  value The requested value.
     * @return A clone of the isoline at the specified value, or <code>null</code>
     *         if there is no isoline for the specified value.
     * @throws IOException if an I/O operation failed.
     */
    public Isoline get(final float value) throws IOException
    {return get(new float[]{value})[0];}

    /**
     * Gets isolines for the specified values. This method
     * looks for cached isolines first, and read them from
     * the source in last ressort. More specifically:
     *
     * <ul>
     *   <li>If the isoline is available in memory, then this isoline is selected.</li>
     *   <li>Otherwise, if a isolines are available in serialized form, then load
     *       those isolines. This process is usually much faster than parsing and
     *       processing isolines from their source.</li>
     *   <li>Otherwise, parse the isolines from their source and serialize them
     *       for future use.</li>
     * </ul>
     *
     * If an isoline has been found, then this method returns a clone of this
     * isoline. Note that cloning {@link Isoline} means that only meta-data
     * info (like {@link org.geotools.cs.CoordinateSystem}) are really cloned.
     * Most voluminous data (like the array of points defining ploygons) are
     * shared among all clones.
     *
     * @param  values The requested values.
     * @return An array of isoline clones at the specified values. This array has
     *         the same length then <code>values</code>. Some array's element may
     *         be <code>null</code>  if there is no isoline for the corresponding
     *         value.
     * @throws IOException if an I/O operation failed.
     */
    public synchronized Isoline[] get(final float[] values) throws IOException
    {
        final float[]   sortedValues = (float[]) values.clone();
        final Isoline[] sortedResult = new Isoline[values.length];
        Arrays.sort(sortedValues);
        Object input = null;
        if (isolines==null)
        {
            /*
             * If this method is invoked for the first time, try
             * to load isolines from the cache.  If isolines are
             * not available in serialized form, parse them from
             * their source and serialize them.
             */
            input = open("get");
            if (isolines==null)
            {
                // All elements are null.
                return sortedResult;
            }
        }
        /*
         * For each requested isoline, try to fetch
         * the isoline from the memory cache.
         */
  fill: for (int i=0; i<sortedValues.length; i++)
        {
            float floatValue = sortedValues[i];
            Float key = new Float(floatValue);
            if (floatValue==0 && !isolines.containsKey(key))
            {
                // Special case for the 0 isoline: +0 and -0 are not equals for
                // the 'Float' class. If the user asked for the 0 meter isoline,
                // we may have to look at the -0 meter isoline instead.
                floatValue = -floatValue;
                key = new Float(floatValue);
            }
            final Reference reference = isolines.get(key);
            if (reference!=null)
            {
                final Isoline iso = (Isoline) reference.get();
                if (iso!=null)
                {
                    // An isoline has been found in the cache. Put
                    // it in they array (we will clone it later).
                    sortedResult[i] = iso;
                    continue fill;
                }
            }
            else if (!isolines.containsKey(key))
            {
                // There is no isoline for the specified key.
                // Lets the corresponding element to null.
                continue fill;
            }
            /*
             * No isoline has been found in the memory cache, and
             * we know that one should be available.  Try to load
             * it from the serialized cache.
             */
            final String value = getName(floatValue);
            if (input==null)
            {
                input = open("get");
            }
            if (input instanceof ZipFile)
            {
                /*
                 * Try to read the cache as a file.
                 * This is the most efficient method.
                 */
                final ZipFile    zip = (ZipFile) input;
                final ZipEntry entry = zip.getEntry(value);
                if (entry!=null)
                {
                    sortedResult[i] = load(new BufferedInputStream(zip.getInputStream(entry)), true);
                    continue fill;
                }
            }
            else if (input instanceof ZipInputStream)
            {
                /*
                 * Try to read the cache as a stream. Slower,
                 * but more general (work through a network link).
                 */
                final ZipInputStream zip = (ZipInputStream) input;
                ZipEntry entry = zip.getNextEntry();
                while (entry!=null)
                {
                    if (entry.getName().equalsIgnoreCase(value))
                    {
                        sortedResult[i] = load(zip, false);
                        zip.closeEntry();
                        continue fill;
                    }
                    zip.closeEntry();
                    entry = zip.getNextEntry();
                }
            }
            /*
             * No data has been found in the serialized cache,
             * while we was expecting data. Log a warning.
             */
            warning("get", ResourceKeys.ERROR_MISSING_ISOLINE_$1, key);
        }
        close(input);
        /*
         * Clone the isoline. All clone keeps a reference
         * to their originating isoline in order to prevent
         * garbage collection (which would defect the purpose
         * of this cache).
         */
        final Isoline[] result = new Isoline[sortedResult.length];
        for (int i=0; i<values.length; i++)
        {
            final int index = Arrays.binarySearch(sortedValues, values[i]);
            if (index < 0)
            {
                // Should not happen
                warning("get", ResourceKeys.ERROR_MISSING_ISOLINE_$1, new Float(values[i]));
                continue;
            }
            final Isoline iso = sortedResult[index];
            if (iso != null)
            {
                result[i] = new Cloned(iso);
            }
        }
        return result;
    }

    /**
     * Close the stream.
     *
     * @param  input The stream to close, usually a {@link ZipFile}
     *         or a {@link InputStream} object. Other object type
     *         (likes <code>Isoline[]</code> or <code>null</code>)
     *         will be ignored.
     * @throws IOException if an error occured while closing the stream.
     */
    private static void close(final Object input) throws IOException
    {
        if (input instanceof ZipFile)
        {
            ((ZipFile) input).close();
            return;
        }
        if (input instanceof InputStream)
        {
            ((InputStream) input).close();
            return;
        }
    }

    /**
     * Open the cache and returns a connection to it. This method returns a
     * {@link ZipFile}  if the cache can be accessed through a {@link File}
     * object, or a {@link ZipInputStream} if the cache can be accessed
     * through an {@link URL}. If the cache can't be found, then this method
     * read all isolines from the source (with {@link #readAll}) and returns
     * an <code>Isoline[]</code> array. In any case, if the memory cache
     * {@link #isolines} hasn't be constructed yet, then this method initialize
     * the memory cache now.
     */
    private Object open(final String sourceMethodName) throws IOException
    {
        /*
         * Try to open the cache as a file.
         * This is the most efficient way.
         */
        if (cacheFile!=null && cacheFile.isFile())
        {
            final ZipFile zip = new ZipFile(cacheFile);
            if (isolines==null)
            {
                loadIndex(zip.getInputStream(zip.getEntry("index")), true);
            }
            return zip;
        }
        /*
         * Try to open the cache as an URL. This is
         * a slower but a more general way. It work
         * through network protocol like HTTP.
         */
        if (cacheURL!=null)
        {
            final ZipInputStream zip = new ZipInputStream(new BufferedInputStream(cacheURL.openStream()));
            if (isolines==null)
            {
                ZipEntry entry = zip.getNextEntry();
                while (entry!=null)
                {
                    if (entry.getName().equalsIgnoreCase("index"))
                    {
                        loadIndex(zip, false);
                        zip.closeEntry();
                        return zip;
                    }
                    zip.closeEntry();
                    entry = zip.getNextEntry();
                }
                zip.close();
                return null;
            }
            return zip;
        }
        /*
         * If no isolines was found in the cache, load all of
         * them now and save them in the cache for future use.
         * Returns the array of isolines. This is usually not
         * used, but prevent the garbage collector to collect
         * the isolines too early.
         */
        final Isoline[] all = readAll();
        isolines = new HashMap<Float,Reference<Isoline>>(all.length + all.length/2);
        final Resources resources = Resources.getResources(null);
        for (int i=0; i<all.length; i++)
        {
            final Isoline iso = all[i];
            add(iso);
            LogRecord record;
            try
            {
                final float factor = iso.compress(0.75f);
                record = resources.getLogRecord(Level.FINE, ResourceKeys.ISOLINE_DECIMATED_$2,
                                                new Float(iso.value), new Float(factor));
            }
            catch (TransformException exception)
            {
                final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(exception));
                final String message = exception.getLocalizedMessage();
                if (message!=null)
                {
                    buffer.append(": ");
                    buffer.append(message);
                }
                record = new LogRecord(Level.WARNING, buffer.toString());
                record.setThrown(exception);
            }
            record.setSourceClassName("IsolineFactory");
            record.setSourceMethodName(sourceMethodName);
            logger.log(record);
        }
        save(all);
        return all;
    }

    /**
     * Load the index from the specified input stream
     * and initialize the memory cache according. This
     * method is invoked by {@link #open} only.
     *
     * @param  input The input stream to read.
     * @param  canClose <code>true</code> to close the input stream
     *         after reading, or <code>false</code> to lets it open.
     * @throws IOException if an error occured while decoding the isoline.
     */
    private void loadIndex(final InputStream input, final boolean canClose) throws IOException
    {
        final DataInputStream in = new DataInputStream(input);
        int count = in.readInt();
        isolines = new HashMap<Float,Reference<Isoline>>(count + count/2);
        while (--count >= 0)
        {
            isolines.put(new Float(in.readFloat()), null);
        }
        if (canClose)
        {
            in.close();
        }
    }

    /**
     * Load the isoline from the specified input stream
     * and add it to the cache.  This method is invoked
     * by {@link #get} only.
     *
     * @param  input The input stream to read.
     * @param  canClose <code>true</code> to close the input stream
     *         after reading, or <code>false</code> to lets it open.
     * @return The isoline.
     * @throws IOException if an error occured while decoding the isoline.
     */
    private Isoline load(final InputStream input, final boolean canClose) throws IOException
    {
        Isoline isoline = null;
        final ObjectInputStream in = new ObjectInputStream(input);
        try
        {
            isoline = (Isoline) in.readObject();
            add(isoline);
            final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE,
                                     ResourceKeys.LOADING_ISOLINE_$1, new Float(isoline.value));
            record.setSourceClassName("IsolineFactory");
            record.setSourceMethodName("get");
            logger.log(record);
        }
        catch (ClassNotFoundException exception)
        {
            // Should not happen
            InvalidClassException e = new InvalidClassException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        if (canClose)
        {
            // Release resources used by ObjectInputStream, but it have
            // the side-effect of closing the underlying input stream.
            in.close();
        }
        return isoline;
    }

    /**
     * Add an isoline to the cache. If an other isoline was already in
     * the cache for the same value, a warning message will be logged.
     */
    private void add(final Isoline isoline)
    {
        final Float key = new Float(isoline.value);
        final Reference oldRef = isolines.put(key, new SoftReference<Isoline>(isoline));
        if (oldRef!=null && oldRef.get()!=null)
        {
            // Should not occurs.
            warning("get", ResourceKeys.ERROR_DUPLICATED_ISOLINE_$1, key);
        }
    }

    /**
     * Save the specified set of isolines to the cache file.
     * If a previous cache exist, it will be deleted first.
     *
     * @param  isolines The set of isolines to save in the cache.
     * @throws IOException if an error occurs during serialization.
     */
    private void save(final Isoline[] isolines) throws IOException
    {
        final ZipOutputStream out = new ZipOutputStream(
                (cacheFile!=null) ? new FileOutputStream(cacheFile) :
                                    cacheURL.openConnection().getOutputStream());
        out.setComment("Serialized Java objects: fr.ird.map.Isoline");
        out.setLevel(Deflater.BEST_COMPRESSION);
        /*
         * Fist write an index of all available isolines, in increasing
         * order of value. The binary format is as below:
         *
         *   - First the number of isolines is written as an 'int'.
         *   - Then all values are written as 'float'.
         */
        Arrays.sort(isolines);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        if (true)
        {
            final CheckedOutputStream checked = new CheckedOutputStream(buffer, new CRC32());
            final DataOutputStream dataStream = new DataOutputStream(checked);
            dataStream.writeInt(isolines.length);
            for (int i=0; i<isolines.length; i++)
            {
                dataStream.writeFloat(isolines[i].value);
            }
            dataStream.close();
            final int size = buffer.size();

            final ZipEntry entry = new ZipEntry("index");
            entry.setMethod(ZipEntry.STORED);
            entry.setCompressedSize(size);
            entry.setSize(size);
            entry.setCrc(checked.getChecksum().getValue());
            out.putNextEntry(entry);
            buffer.writeTo(out);
            out.closeEntry();
        }
        /*
         * Serialize each isoline to the zip file.
         */
        for (int i=0; i<isolines.length; i++)
        {
            buffer.reset();
            final Isoline isoline = isolines[i];
            final ObjectOutputStream dataStream = new ObjectOutputStream(buffer);
            dataStream.writeObject(isoline);
            dataStream.close();

            final ZipEntry entry = new ZipEntry(getName(isoline.value));
            out.putNextEntry(entry);
            buffer.writeTo(out);
            out.closeEntry();
        }
        out.close();
    }

    /**
     * Load a warning record.
     *
     * @param resourceKey The resource key for the message.
     * @param value The value to format with the message.
     */
    private static void warning(final String sourceMethodName, final int resourceKey, final Float value)
    {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.WARNING, resourceKey, value);
        record.setSourceClassName("IsolineFactory");
        record.setSourceMethodName(sourceMethodName);
        logger.log(record);
    }

    /**
     * A cloned isoline. Those clone keeps reference to their
     * originating isoline, in order to prevent garbage collection
     * (which would defect the purpose of {@link IsolineFactory}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static class Cloned extends Isoline
    {
        /**
         * The originating provider.
         */
        private final Isoline parent;

        /**
         * Construct a cloned isoline.
         */
        public Cloned(final Isoline isoline)
        {
            super(isoline);
            parent=isoline;
        }
    }
}
