package fr.ird.database.coverage;

// J2SE.
import java.util.List;
import java.io.Serializable;

// JAI.
import javax.media.jai.util.Range;

// Seagis.
import fr.ird.database.coverage.CoverageEntry;

// Geotools.
import org.geotools.util.RangeSet;

/**
 * Contient les plages de temps et de coordonnées des images ainsi que la liste des 
 * entrées correspondantes.
 *
 * @version $Id$
 * @author Remi Eve
 */
public class GridCoverageRange implements Serializable {
    /**
     * Objet dans lequel ajouter les plages de longitudes.
     */
    public RangeSet x;
    
    /**
     * Objet dans lequel ajouter les plages de latitudes.
     */
    public RangeSet y;
    
    /**
     * Objet dans lequel ajouter les plages de temps.
     */    
    public RangeSet t;
    
    /**
     * Liste dans laquelle ajouter les images qui auront été lues.
     */
    public List<CoverageEntry> entryList;

    /** 
     * Constructeur.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     * @param entryList Liste dans laquelle ajouter les images qui auront été
     *        lues, ou <code>null</code> pour ne pas construire cette liste.
     */
    public GridCoverageRange(final RangeSet x, final RangeSet y, final RangeSet t, final List<CoverageEntry> entryList) {
        this.x = x;
        this.y = y;
        this.t = t;
        this.entryList = entryList;
    }
}