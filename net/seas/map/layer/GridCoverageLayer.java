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
package net.seas.map.layer;

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.gc.GridRange;
import net.seagis.gc.GridCoverage;
import net.seagis.cv.CategoryList;
import net.seagis.cv.SampleDimension;
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.TransformException;
import net.seagis.gp.GridCoverageProcessor;
import net.seagis.resources.OpenGIS;

// Others SEAGIS packages
import net.seas.map.Layer;
import net.seas.map.MapPanel;
import net.seas.map.GeoMouseEvent;
import net.seas.map.RenderingContext;
import net.seagis.resources.XDimension2D;
import net.seagis.resources.XAffineTransform;

// Miscellaneous
import java.util.List;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.media.jai.GraphicsJAI;


/**
 * A layer for displaying a grid coverage in a {@link MapPanel}.
 * Many layers may share the same grid coverage (for example in
 * order to display the coverage in many {@link MapPanel}).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class GridCoverageLayer extends Layer
{
    /**
     * The underlying grid coverage.
     */
    private final GridCoverage coverage;

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
    private final Rectangle2D geographicArea;

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
     * Liste des cat�gories. Cette liste ne sera cr��e
     * que la premi�re fois o� elle sera n�cessaire.
     */
    private transient CategoryList[] categories;

    /**
     * Construct a new layer for the specified grid coverage.
     * It is legal to construct many layers for the same grid
     * coverage (in order to be inserted in many {@link MapPanel}
     * for example).
     */
    public GridCoverageLayer(final GridCoverage coverage)
    {
        super(coverage.getCoordinateSystem());
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
     * Returns the underlying grid coverage.
     */
    public GridCoverage getCoverage()
    {return coverage;}

    /**
     * Returns the grid coverage projected
     * to the specified coordinate system.
     */
    private GridCoverage getCoverage(CoordinateSystem targetCS)
    {
        CoordinateSystem sourceCS;
        if (projectedCoverage==null)
        {
            projectedCoverage = coverage;
        }
        sourceCS = projectedCoverage.getCoordinateSystem();
        sourceCS = OpenGIS.getCoordinateSystem2D(sourceCS);
        targetCS = OpenGIS.getCoordinateSystem2D(targetCS);
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
            getCoverage(mapPanel.getCoordinateSystem()).prefetch(area);
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
        getCoverage(context.getViewCoordinateSystem()).paint(graphics);
        return XAffineTransform.transform(context.getAffineTransform(RenderingContext.WORLD_TO_POINT), geographicArea, pointArea);
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
        point  = event.getCoordinate(coverage.getCoordinateSystem(), point);
        values = coverage.evaluate(point, values);
        if (categories==null)
        {
            final SampleDimension[] dimensions = coverage.getSampleDimensions();
            final CategoryList[] categories = new CategoryList[dimensions.length];
            for (int i=0; i<categories.length; i++)
                categories[i] = dimensions[i].getCategoryList();
        }
        boolean modified = false;
        for (int i=0; i<values.length; i++)
        {
            final String text = categories[i].format(values[i], /*Locale*/null);
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
}
