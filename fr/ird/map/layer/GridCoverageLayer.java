/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.map.layer;

// Miscellaneous
import java.util.Locale;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import javax.media.jai.GraphicsJAI;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.cv.SampleDimension;
import org.geotools.gc.GridRange;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XAffineTransform;

// Others packages
import fr.ird.map.Layer;
import fr.ird.map.MapPanel;
import fr.ird.map.GeoMouseEvent;
import fr.ird.map.RenderingContext;


/**
 * A layer for displaying a grid coverage in a {@link MapPanel}.
 * Many layers may share the same grid coverage (for example in
 * order to display the coverage in many {@link MapPanel}).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class GridCoverageLayer extends Layer
{
    /**
     * The underlying grid coverage.
     */
    private GridCoverage coverage;

    /**
     * The projected grid coverage. This coverage
     * is computed only when first needed.
     */
    private transient GridCoverage projectedCoverage;

    /**
     * Coordonn�es g�ographiques de l'image. Ces coordonn�es
     * sont extraites une fois pour toute afin de r�duire le
     * nombre d'objets cr��s lors des trac�s de la carte.
     */
    private Rectangle2D geographicArea;

    /**
     * Coordonn�es en points de la r�gion
     * dans laquelle a �t� dessin�e l'image.
     */
    private final Rectangle2D pointArea=new Rectangle2D.Float();

    /**
     * Point dans lequel m�moriser les coordonn�es logiques d'un pixel
     * de l'image. Cet objet est utilis� temporairement pour obtenir la
     * valeur du param�tre g�ophysique d'un pixel.
     */
    private transient Point2D point;

    /**
     * Valeurs sous le curseur de la souris. Ce tableau sera cr��
     * une fois pour toute la premi�re fois o� il sera n�cessaire.
     */
    private transient double[] values;

    /**
     * Liste des bandes. Cette liste ne sera cr��e
     * que la premi�re fois o� elle sera n�cessaire.
     */
    private transient SampleDimension[] bands;

    /**
     * Construct a grid coverage layer which will display
     * image from the specified coordinate systems.
     */
    public GridCoverageLayer(final CoordinateSystem coordinateSystem)
    {
        super(coordinateSystem);
    }

    /**
     * Construct a new layer for the specified grid coverage.
     * It is legal to construct many layers for the same grid
     * coverage (in order to be inserted in many {@link MapPanel}
     * for example).
     */
    public GridCoverageLayer(final GridCoverage coverage)
    {
        super(coverage.getCoordinateSystem());
        setCoverage(coverage);
    }

    /**
     * Set the grid coverage. A <code>null</code> value
     * will remove the current grid coverage.
     */
    public void setCoverage(final GridCoverage coverage)
    {
        if (coverage == null)
        {
            clearCache();
            this.coverage = null;
            return;
        }
        if (!getCoordinateSystem().equals(CTSUtilities.getCoordinateSystem2D(coverage.getCoordinateSystem())))
        {
            // TODO: implement changing coordinate system.
            throw new UnsupportedOperationException("Can't change coordinate system");
        }
        clearCache();
        this.coverage = coverage;
        final Envelope envelope = coverage.getEnvelope();
        final GridRange   range = coverage.getGridGeometry().getGridRange();
        this.geographicArea = new Rectangle2D.Double(envelope.getMinimum(0),
                                                     envelope.getMinimum(1),
                                                     envelope.getLength (0),
                                                     envelope.getLength (1));
        setPreferredArea(geographicArea);
        setZOrder(envelope.getDimension()>=3 ? (float)envelope.getCenter(2) : Float.NEGATIVE_INFINITY);
        setPreferredPixelSize(new XDimension2D.Double(envelope.getLength(0)/range.getLength(0),
                                                      envelope.getLength(1)/range.getLength(1)));
    }

    /**
     * Returns the underlying grid coverage, or <code>null</code>
     * if no grid coverage has been set.
     */
    public GridCoverage getCoverage()
    {return coverage;}

    /**
     * Returns the grid coverage projected
     * to the specified coordinate system.
     */
    private GridCoverage getCoverage(CoordinateSystem targetCS)
    {
        if (coverage==null)
        {
            return null;
        }
        CoordinateSystem sourceCS;
        if (projectedCoverage==null)
        {
            projectedCoverage = coverage;
        }
        sourceCS = projectedCoverage.getCoordinateSystem();
        sourceCS = CTSUtilities.getCoordinateSystem2D(sourceCS);
        targetCS = CTSUtilities.getCoordinateSystem2D(targetCS);
        if (!sourceCS.equivalents(targetCS))
        {
            final GridCoverageProcessor processor = GridCoverageProcessor.getDefault();
            projectedCoverage = processor.doOperation("Resample", coverage, "CoordinateSystem", targetCS);
        }
        return projectedCoverage;
    }

    /**
     * Pr�vient cette couche qu'elle sera bient�t dessin�e sur la carte sp�cifi�e. Cette m�thode peut
     * �tre appel�e avant que cette couche soit ajout�e � la carte.  Elle peut lancer en arri�re-plan
     * quelques threads qui pr�pareront l'image. Note: il ne sert � rien d'appeller cette m�thode
     * imm�diatement avant de faire afficher cette couche. Cette m�thode n'est utile que lorsqu'elle
     * est appel�e un peu en avance.
     */
    public void prefetch(final MapPanel mapPanel)
    {
        final Rectangle2D area=mapPanel.getVisibleArea();
        if (area!=null && !area.isEmpty())
        {
            final GridCoverage coverage = getCoverage(mapPanel.getCoordinateSystem());
            if (coverage!=null)
            {
                coverage.prefetch(area);
            }
        }
    }

    /**
     * Dessine l'image.
     *
     * @param  graphics Graphique � utiliser pour tracer les couches.
     * @param  context  Suite des transformations n�cessaires � la conversion de coordonn�es
     *         g�ographiques (<var>longitude</var>,<var>latitude</var>) en coordonn�es pixels.
     * @return Un rectangle englobeant l'image dessin�e, en points (1/72 de pouce).
     * @throws TransformException si une projection cartographique �tait n�cessaire et qu'elle a �chou�.
     */
    protected Shape paint(final GraphicsJAI graphics, final RenderingContext context) throws TransformException
    {
        final GridCoverage coverage = getCoverage(context.getViewCoordinateSystem());
        if (coverage!=null)
        {
            final MathTransform2D mathTransform = coverage.getGridGeometry().getGridToCoordinateSystem2D();
            if (!(mathTransform instanceof AffineTransform)) {
                throw new UnsupportedOperationException("Non-affine transformations not yet implemented"); // TODO
            }
            final AffineTransform gridToCoordinate = (AffineTransform) mathTransform;
            final AffineTransform transform = new AffineTransform(gridToCoordinate);
            transform.translate(-0.5, -0.5); // Map to upper-left corner.
            graphics.drawRenderedImage(coverage.getRenderedImage(), transform);
            return XAffineTransform.transform(context.getAffineTransform(RenderingContext.WORLD_TO_POINT), geographicArea, pointArea);
        }
        return null;
    }

    /**
     * M�thode appel�e automatiquement pour construire une cha�ne de caract�res repr�sentant la valeur
     * point�e par la souris. En g�n�ral (mais pas obligatoirement), lorsque cette m�thode est appel�e,
     * le buffer <code>toAppendTo</code> contiendra d�j� une cha�ne de caract�res repr�sentant les
     * coordonn�es point�es par la souris. Cette m�thode y ajoutera la valeur du pixel sous le curseur
     * de la souris.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette m�thode a ajout� des informations dans <code>toAppendTo</code>.
     */
    protected synchronized boolean getLabel(final GeoMouseEvent event, final StringBuffer toAppendTo)
    {
        if (coverage==null)
        {
            return false;
        }
        point  = event.getCoordinate(getCoordinateSystem(), point);
        values = coverage.evaluate(point, values);
        if (bands == null)
        {
            bands = coverage.getSampleDimensions();
        }
        boolean modified = false;
        for (int i=0; i<values.length; i++)
        {
            final String text = bands[i].getLabel(values[i], (Locale)null);
            if (text!=null)
            {
                if (modified)
                {
                    toAppendTo.append(", ");
                }
                toAppendTo.append(text);
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Efface les informations qui avaient �t�
     * sauvegard�es dans la cache interne.
     */
    protected void clearCache()
    {
        point             = null;
        values            = null;
        bands             = null;
        projectedCoverage = null;
        super.clearCache();
    }
}
