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
//import fr.ird.resources.Utilities;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.sql.CoverageDataBase;
import fr.ird.io.text.ParseHeader;

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
    /**
     * First column  : begin of file name containing a product.
     * Second column : serie to add file name begin with first column.
     * Third column  : begin of file name inserted to databse Image.
     */
    private final static int PREFIX_SRC        = 0,
                             FORMAT_DATE_SRC   = 1,
                             SERIE             = 2,
                             OFFSET_START_TIME = 3,
                             OFFSET_END_TIME   = 4,
                             PREFIX_TGT        = 5,
                             FORMAT_DATE_TGT   = 6;
    private static final String[][] PARAMETERS = getParameters();
                                                           
    /** Chemin par défaut du fichier de configuration. */
    private final static String DEFAULT_FILE = "application-data/configuration/UpdateDatabase.txt";
                                             
                                             
    /**
     * Extract parameter for inserting image in databse 'Images'.
     */                                             
    private static final String[][] getParameters() {  
        try {
            final int size = 7; // Number of parameters by line.
            final BufferedReader input = new BufferedReader(new FileReader(new File(Updater.class.getClassLoader().getResource(DEFAULT_FILE).getPath())));

            // Listes des paramétres.
            final Vector vParameters = new Vector();

            // Parcours ligne à ligne.
            String line;
            int count = 0;
            while ((line=input.readLine())!=null) {
                count ++;
                // Ligne vide ou commentaire.
                line = line.trim();            
                if (line.length() == 0 ||line.startsWith("#")) {
                    continue;
                }

                final StringTokenizer token = new StringTokenizer(line);                        
                final int length = token.countTokens();
                if (length < size) {
                    System.err.println("An error occurs in file '" + DEFAULT_FILE + "' at line " + count + ".");
                }

                final String[] parameter = new String[size];
                for (int i=0 ; i<size && token.hasMoreTokens(); i++) {
                    parameter[i] = token.nextToken();
                }

                vParameters.add(parameter);
            }

            final String[][] parameters = new String[vParameters.size()][];
            for (int i=0 ; i<vParameters.size() ; i++) {
                parameters[i] = (String[])vParameters.get(i);
            }
            return parameters;
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
                                             
    /**
     * Insert a new image to database from filename of image and his 
     * associated header.
     *
     * @param src       Name of Image source.
     * @param header    Name of file header associated to image.
     */
    public static void insertToDataBase(final File src, final File header) throws IOException, SQLException
    {
        if (PARAMETERS == null) {
            throw new IllegalStateException("Can't determine target serie for the image '" + src + "'.");            
        }
        
        //////////////////////////
        // search target serie. //
        //////////////////////////
        final String nameSrc = src.getName();
        boolean find = false;
        int index = -1;
        
        for (int i=0 ; i<PARAMETERS.length ; i++) 
        {
            // Compare with pattern.            
            final String pattern = PARAMETERS[i][PREFIX_SRC] + "[0-9]{" + PARAMETERS[i][FORMAT_DATE_SRC].length() + "}.*";            
            if (Pattern.matches(pattern, nameSrc)) {
                if (find == false) {
                    find = true;
                    index = i;
                } else {
                    throw new IllegalStateException("Find more than one target series for image '" + src + "'.");
                }
            }
        }
        
        if (find == false) {
            throw new IllegalStateException("Can't determine target serie for the image '" + src + "'.");
        }
                
        /////////////////////////////////////////////////
        // Compute start and end time of this product. //
        /////////////////////////////////////////////////
        final Date dateSrc   = getDate(src, PARAMETERS[index][PREFIX_SRC], PARAMETERS[index][FORMAT_DATE_SRC]);
        final Date startTime = new Date(dateSrc.getTime() - Long.parseLong(PARAMETERS[index][OFFSET_START_TIME])),
                   endTime   = new Date(dateSrc.getTime() + Long.parseLong(PARAMETERS[index][OFFSET_END_TIME]));
        
        ////////////////////////
        // Build target name. //
        ////////////////////////
        final String tgt = getTargetFile(endTime, PARAMETERS[index][PREFIX_TGT], PARAMETERS[index][FORMAT_DATE_TGT]);
        
        ///////////////////////
        // Extract envelope. //
        ///////////////////////
        final ParameterList parameterIn = ParseHeader.getInputDefaultParameterList();
        parameterIn.setParameter(ParseHeader.FILE, header);
        final ParameterList parameterOut = ParseHeader.parse(parameterIn);
        final Point2D origine = (Point2D)parameterOut.getObjectParameter(ParseHeader.ORIGINE);
        final int width  = parameterOut.getIntParameter(ParseHeader.WIDTH),
                  height = parameterOut.getIntParameter(ParseHeader.HEIGHT);
        final double[] resolution = (double[])parameterOut.getObjectParameter(ParseHeader.RESOLUTION);        
        final Rectangle2D area = new Rectangle2D.Double(origine.getX(), 
                                                        origine.getY() - resolution[1] * height,
                                                        width  * resolution[0],
                                                        height * resolution[1]);
        
        ///////////////////////////
        // Inserting to database. //
        ///////////////////////////
        final CoverageDataBase database = new CoverageDataBase();
        final String serie = database.getCoverageTable(Integer.parseInt(PARAMETERS[index][SERIE])).getSeries().getName();
        insertToDataBase( database, src, tgt, serie, startTime, endTime, area);
    }
    
    /**
     * @return target file name.
     * @parma date      Date of product.
     * @param prefix    Target file name prefix.
     * @param format    Date format.
     * @return target file name.
     */
    private final static String getTargetFile(final Date date, final String prefix, final String format) 
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        final StringBuffer buffer = new StringBuffer();
        return prefix + dateFormat.format(date, buffer, new FieldPosition(0)).toString();
    }
    
    /**
     * Return date from file source.
     * @return date from file source.
     */
    private final static Date getDate(final File src, final String prefix, final String format) 
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String date  = src.getName().toString().substring(prefix.length(), prefix.length() + format.length());
        final Date dateSrc = dateFormat.parse(date, new ParsePosition(0));
        
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.setTime(dateSrc);
        return calendar.getTime();
    }
    
    
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
        final Object GoodWriter = registry.getServiceProviderByClass(com.sun.imageio.plugins.png.PNGImageWriterSpi.class),
                     BadWriter  = registry.getServiceProviderByClass(com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriterSpi.class);                 
        
        if((GoodWriter != null) && (BadWriter != null))  
            registry.setOrdering(ImageWriterSpi.class, GoodWriter, BadWriter);            
        
        final Object GoodReader = registry.getServiceProviderByClass(com.sun.imageio.plugins.png.PNGImageReaderSpi.class),
                     BadReader  = registry.getServiceProviderByClass(com.sun.media.imageioimpl.plugins.png.CLibPNGImageReaderSpi.class);                 
        
        if((GoodReader != null) && (BadReader != null))  
            registry.setOrdering(ImageReaderSpi.class, GoodReader, BadReader);                    
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
 
    
    //////////////////////////////////////////////////////////////////////////////////////
    /////////////                                                          ///////////////    
    /////////////                               MAIN                       ///////////////
    /////////////                                                          ///////////////    
    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * Main.
     */
    public static void main(final String[] args) 
        throws IOException, SQLException
    {
        final int count = args.length;
        if (count != 1)  {
            System.err.println("Insert a new image into database");
            System.err.println("Update IMAGE");
            System.err.println("IMAGE                   --> Nom du fichier représentant l'image à insérer.");
            System.exit(-1);
        }
        
        Updater.insertToDataBase(new File(args[0]), new File(args[0] + ".hdr"));
    }
}