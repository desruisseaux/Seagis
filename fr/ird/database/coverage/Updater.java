package fr.ird.database.coverage;

// J2SE.
import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.text.FieldPosition;
import java.util.regex.*;
import java.util.Calendar;
import java.awt.image.RenderedImage;
import java.awt.geom.Rectangle2D;
import javax.imageio.ImageIO;
import java.sql.SQLException;
import com.sun.media.imageioimpl.plugins.raw.RawImageReader;
import com.sun.media.imageio.stream.RawImageInputStream;
import javax.imageio.spi.*;
import javax.media.jai.ParameterList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.util.Vector;

// SEAGIS.
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.sql.CoverageDataBase;

// GEOTOOLS.
import org.geotools.gc.GridCoverage;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.pt.Envelope;
import org.geotools.util.NumberRange;
import org.geotools.resources.CTSUtilities;

/**
 * This class provides methods for adding an image to DataBase 'image'.
 *
 * @author  Remi Eve
 * @version $Id$
 */
public class Updater {
        
    //////////////////////////////////////////////////////////////////////////////////////
    /////////////                                                          ///////////////    
    /////////////                           PRIVATE                        ///////////////
    /////////////                                                          ///////////////    
    //////////////////////////////////////////////////////////////////////////////////////
    /** 
     * Tries to reorder Image Readers in The IIORegistry. 
     */
    private static void reOrderWritersReadersPNG() 
    { 
        final IIORegistry registry = IIORegistry.getDefaultInstance();        
        final ImageWriterSpi goodWriter = (ImageWriterSpi) registry.getServiceProviderByClass(com.sun.imageio.plugins.png.PNGImageWriterSpi.class);
        final ImageWriterSpi  badWriter = (ImageWriterSpi) registry.getServiceProviderByClass(com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriterSpi.class);                 
        
        if ((goodWriter != null) && (badWriter != null))  
            registry.setOrdering(ImageWriterSpi.class, goodWriter, badWriter);            
        
        final ImageReaderSpi goodReader = (ImageReaderSpi) registry.getServiceProviderByClass(com.sun.imageio.plugins.png.PNGImageReaderSpi.class);
        final ImageReaderSpi  badReader = (ImageReaderSpi) registry.getServiceProviderByClass(com.sun.media.imageioimpl.plugins.png.CLibPNGImageReaderSpi.class);                 
        
        if ((goodReader != null) && (badReader != null))  
            registry.setOrdering(ImageReaderSpi.class, goodReader, badReader);                    
    }        
    

    //////////////////////////////////////////////////////////////////////////////////////
    /////////////                                                          ///////////////    
    /////////////                           PUBLIC                         ///////////////
    /////////////                                                          ///////////////    
    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * Insert a new image to database.
     *
     * @param file  Image source.
     * @param name  Name of the target image without extension and path.
     * @param serie Name of target serie.
     */
    public static void insertToDataBase(final CoverageDataBase dataBase,
                                        final File   file,    final String name, 
                                        final String serie,   final Date   startTime, 
                                        final Date   endTime, final Rectangle2D area) 
         throws SQLException, IOException
    {
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.setTime(startTime);               
        calendar.clear();
        calendar.setTime(endTime);
        
        reOrderWritersReadersPNG();
        final CoverageTable     table    = dataBase.getCoverageTable(serie);
        final CoordinateSystem  cs       = table.getCoordinateSystem();
        final Color[]           colors   = fr.ird.resources.Utilities.getPaletteFactory().getColors("grayscale");
        final Category[]        category = {new Category("GRAY", colors, new NumberRange(0, 1023.0), new NumberRange(0, 1023)).geophysics(true)};
        final SampleDimension[] sample   = {new SampleDimension(category, null)};

        // 1.Extract date.
        if  (startTime.equals(endTime)) {
            throw new IllegalStateException("Start and end of acquisition can't be the same date.");
        }
        if (endTime.before(startTime)) {
            throw new IllegalStateException("End acquisition date can't be before lower than acquisition date.");
        }

        // 2.Create envelope.
        final Envelope envelope  = new Envelope(area);
        final Envelope envelopeT = buildTemporalEnvelope(CTSUtilities.getTemporalCS(cs), envelope, startTime, endTime);

        // 3. read Image.
        RenderedImage image = ImageIO.read(file);

        // 4. create GridCoverage.
        final GridCoverage grid = new GridCoverage(name, image, cs, envelopeT, sample, null, null);
        final Integer      id   = table.addGridCoverage(grid, name);
        if (id == null) {
            throw new IllegalStateException("An image with the same name '" + name + "' was already presents in the database.");
        }

        // 5. we need to copy image to target directory.
        final CoverageEntry coverageEntry = table.getEntry(id.intValue());
        final File          tgt           = coverageEntry.getFile();

        // Create directory. 
        final File directory = tgt.getParentFile();
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Can't create directory '" + directory + "'.");
            }
        }

        // Get format.
        int index = tgt.getName().indexOf('.');
        if (index == -1) {
            throw new IOException("Can't extract format from file '" + tgt + "'.");
        } else {
            int index1 = index;
            while ((index1 = tgt.getName().indexOf('.', index1+1))!=-1) {
                index = index1;
            }
        }
        final String format = tgt.getName().substring(index+1);                        
        ImageIO.write(image, format, tgt);
        image  = null;
    }   
    
    /**
     * Return a temporal envelope. Temporal envelope is build by adding temporal
     * informations.
     *
     * @param cs        Temporal coordinate system.
     * @param envelope  A non temporal envelope.
     * @param start     Date beginning of acquisition.
     * @param end       Date ending of acquisition.
     */
    private static final Envelope buildTemporalEnvelope(final TemporalCoordinateSystem cs,
                                                        final Envelope envelope,
                                                        final Date     startTime,
                                                        final Date     endTime) 
    {
        // Compute normalize date.
        final double start_ = cs.toValue(startTime),
        end_   = cs.toValue(endTime);
        
        // Build temporal envelope.
        final int count = envelope.getDimension();
        final double[] min = new double[count + 1],
        max = new double[count + 1];
        
        for (int i=0 ; i<count ; i++) {
            min[i] = envelope.getMinimum(i);
            max[i] = envelope.getMaximum(i);
        }
        min[count] = start_;
        max[count] = end_;
        
        return new Envelope(min, max);
    }    
}