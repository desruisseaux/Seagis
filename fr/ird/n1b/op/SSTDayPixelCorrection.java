package fr.ird.n1b.op;

// SEAGIS.
import fr.ird.n1b.util.StatisticGrid;
import fr.ird.n1b.io.LocalizationGridN1B;
import fr.ird.n1b.io.ImageReaderN1B;
import fr.ird.n1b.image.sst.Utilities;

// J2SE
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.util.Map;
import java.util.Locale;

// JAI
import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.ParameterList;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.cv.SampleDimension;
import org.geotools.cv.Category;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.Envelope;

/**
 * Corrige la température des pixels acquis de jour en la ramenant à la température des 
 * pixels acquis de nuit. <BR><BR>
 *
 * Ce traitement se base sur un objet de type <CODE>StatisticGrid</CODE> contenant des 
 * statistiques sur les pixels acquis de jour, de nuit à une latitude donnée. A partir de 
 * ces statistiques, pour chaque latitude est calculé un <i>delta en degré</i> contenant 
 * la différence entre la température moyenne des pixels acquis de nuit et la température 
 * moyenne des pixels acquis de jour. Ce delta est ensuite appliqué au pixel de jour pour
 * obtenir une moyenne des température équivalente entre les pixels acquis de nuit, de jour
 * à une latitude donnée.
 *
 * @author Remi Eve
 * @version $Id$
 */
public class SSTDayPixelCorrection extends PointOpImage 
{
    
    /** Statistique à utiliser pour corriger les pixels acquis de jour. */
    private final StatisticGrid stat;
    
    /** Coordonnées du pixel haut gauche de l'image dans le système géographique. */
    private final Point2D origine;
    
    /** Résolution d'un pixel de l'image en latitude. */
    private final double resolution;

    /** Intervalle des valeurs indexées de la categorie TEMPERATURE. */
    private final Range rTemperature;       
    
    /** 
     * Constructeur.
     *
     * @param source            Image source.
     * @param mask              Le masque de l'image source.
     * @param origine
     * @param resolution
     * @param stat              Les statistiques à utiliser.
     * @param layout            Type de l'image de sortie.
     * @param configuration     Configuration du comportement du JAI.
     */
    private SSTDayPixelCorrection(final RenderedImage   source, 
                                  final RenderedImage   mask, 
                                  final Point2D         origine,
                                  final double          resolution,
                                  final StatisticGrid   stat, 
                                  final ImageLayout     layout, 
                                  final Map             configuration)      
    {
        super(source, mask, layout, configuration, false);                
        permitInPlaceOperation();
        this.stat = stat;
        this.origine = origine;
        this.resolution = resolution;        

        /* Extraction des catégories et des intervalles de valeur indexés pour chacune 
           des catégories. */
        final Category catTemperature = Utilities.getCategory(Utilities.SAMPLE_SST_INDEXED, Utilities.TEMPERATURE);        
        if (catTemperature == null)
            throw new IllegalArgumentException("Category \"" + Utilities.TEMPERATURE + "\" is not define.");        
        rTemperature = catTemperature.getRange();        
    } 
    
    /**
     * Retourne une image SST pour laquelle les pixels acquis de jour ont été corrigés.
     *
     * @param source            SST Source.
     * @param mask              Masque associé à l'image source.
     * @param configuration     Configuration du traitement réalisé par JAI.
     * @return une image SST pour laquelle les pixels acquis de jour ont été corrigés.
     */
    public static GridCoverage get(final GridCoverage          source,                                   
                                   final GridCoverage          mask,           
                                   final StatisticGrid         stat,
                                   final Map            configuration)
    {
        if (source == null)
            throw new IllegalArgumentException("Source is null.");        
        if (mask == null)
            throw new IllegalArgumentException("Mask is null.");        
        if (stat == null)
            throw new IllegalArgumentException("Stat is null.");                        
        final ImageLayout layout = new ImageLayout(source.getRenderedImage());        
        final Point2D origine   = new Point2D.Double(source.getEnvelope().toRectangle2D().getMinX(), source.getEnvelope().toRectangle2D().getMaxY());
        final double resolution = (Math.abs(source.getEnvelope().toRectangle2D().getMinY() - 
                                            source.getEnvelope().toRectangle2D().getMaxY())) / 
                                   source.getRenderedImage().getHeight();
        final RenderedImage image = new SSTDayPixelCorrection(source.getRenderedImage(), 
                                                              mask.getRenderedImage(), 
                                                              origine,
                                                              resolution,
                                                              stat, 
                                                              layout, 
                                                              configuration);
        return new GridCoverage(source.getName(Locale.FRENCH),
                                image,
                                source.getCoordinateSystem(),
                                source.getEnvelope(),
                                source.getSampleDimensions(),
                                null,
                                null);                                
    }
    
    /**
     * Corrige la température des pixels acquis de jour.
     *
     * @param sources an array of PlanarImage sources.
     * @param dest a WritableRaster to be filled in.
     * @param destRect the Rectangle within the destination to be written.
     */
    public void computeRect(final PlanarImage[] sources, 
                            final WritableRaster dest, 
                            Rectangle destRect) 
    {
        final RectIter iSource         = RectIterFactory.create(sources[0],destRect);
        final RectIter iMask           = RectIterFactory.create(sources[1],destRect);
        final WritableRectIter iTarget = RectIterFactory.createWritable(dest ,destRect);
        destRect = destRect.intersection(sources[0].getBounds());        
        
        iSource.startBands();
        iMask.startBands();
        iTarget.startBands();        
        while (!iTarget.finishedBands())
        {        
            int row = (int)destRect.getY();
            iSource.startLines();
            iMask.startLines();
            iTarget.startLines();
            while (!iTarget.finishedLines())
            {
                iSource.startPixels();            
                iMask.startPixels();            
                iTarget.startPixels();  
                
                /* Calcul du delta entre la temperature moyenne des pixels acquis de nuit 
                   et la température moyenne des pixels acquis de jour. */
                final int index    = stat.getIndex(origine.getY() - (row-sources[0].getMinY())*resolution);
                final int numDay   = stat.getCountDay(index),
                          numNight = stat.getCountNight(index);
                double delta = 0;
                if (numDay > 0 && numNight > 0)
                    delta = stat.getAvgTempOfNight(index) - stat.getAvgTempOfDay(index);                
                while (!iTarget.finishedPixels())
                {                    
                    final Double sst = new Double(iSource.getSampleDouble());
                    
                    /* Si le pixel est un pixel acquis de jour, alors on lui ajoute le 
                       "delta".*/
                    if (iMask.getSampleDouble() == MatrixDayNight.DAY)                        
                    {
                        if (rTemperature.contains(sst))
                        {
                            iTarget.setSample(sst.doubleValue() + delta);
                        }
                        else
                            iTarget.setSample(sst.doubleValue());
                    }
                    else
                        iTarget.setSample(sst.doubleValue());                                            
                    iSource.nextPixel();                
                    iMask.nextPixel();                
                    iTarget.nextPixel();                    
                }        
                iSource.nextLine();
                iMask.nextLine();
                iTarget.nextLine();
                row++;
            }        
            iSource.nextBand();
            iMask.nextBand();
            iTarget.nextBand();
        }        
    }    
}