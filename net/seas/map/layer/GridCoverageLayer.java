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
package net.seas.map.layer;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.gc.GridRange;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.cv.CategoryList;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;

// Others SEAGIS packages
import net.seas.map.Layer;
import net.seas.map.MapPanel;
import net.seas.map.GeoMouseEvent;
import net.seas.map.RenderingContext;
import net.seas.util.XAffineTransform;
import net.seas.util.XDimension2D;

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
     * Coordonnées géographiques de l'image. Ces coordonnées
     * sont extraites une fois pour toute afin de réduire le
     * nombre d'objets créés lors des tracés de la carte.
     */
    private final Rectangle2D geographicArea;

    /**
     * Coordonnées en points de la région
     * dans laquelle a été dessinée l'image.
     */
    private final Rectangle2D pointArea=new Rectangle2D.Float();

    /**
     * Point dans lequel mémoriser les coordonnées logiques d'un pixel
     * de l'image. Cet objet est utilisé temporairement pour obtenir la
     * valeur du paramètre géophysique d'un pixel.
     */
    private transient Point2D point;

    /**
     * Valeurs sous le curseur de la souris. Ce tableau sera créé
     * une fois pour toute la première fois où il sera nécessaire.
     */
    private transient double[] values;

    /**
     * Liste des catégories. Cette liste ne sera créée
     * que la première fois où elle sera nécessaire.
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
     * Prévient cette couche qu'elle sera bientôt dessinée sur la carte spécifiée. Cette méthode peut
     * être appelée avant que cette couche soit ajoutée à la carte.  Elle peut lancer en arrière-plan
     * quelques threads qui prépareront l'image. Note: il ne sert à rien d'appeller cette méthode
     * immédiatement avant de faire afficher cette couche. Cette méthode n'est utile que lorsqu'elle
     * est appelée un peu en avance.
     */
    public void prefetch(final MapPanel mapPanel)
    {
        final Rectangle2D area=mapPanel.getVisibleArea();
        if (area!=null && !area.isEmpty())
        {
            final CoordinateSystem sourceCS = mapPanel.getCoordinateSystem();
            final CoordinateSystem targetCS = coverage.getCoordinateSystem();
            if (sourceCS.equivalents(targetCS))
            {
                coverage.prefetch(area);
            }
            // Do not prefetch if coordinate systems don't match:
            // A new projected coverage have to be created anyway.
        }
    }

    /**
     * Dessine l'image.
     *
     * @param  graphics Graphique à utiliser pour tracer les couches.
     * @param  context  Suite des transformations nécessaires à la conversion de coordonnées
     *         géographiques (<var>longitude</var>,<var>latitude</var>) en coordonnées pixels.
     * @return Un rectangle englobeant l'image dessinée, en points (1/72 de pouce).
     * @throws TransformException si une projection cartographique était nécessaire et qu'elle a échoué.
     */
    protected Shape paint(final GraphicsJAI graphics, final RenderingContext context) throws TransformException
    {
        final CoordinateSystem sourceCS = coverage.getCoordinateSystem();
        final CoordinateSystem targetCS = context.getViewCoordinateSystem();
        if (!sourceCS.equivalents(targetCS))
        {
            // TODO: Use GridCoverageProcessor when Resample will be implemented.
            throw new UnsupportedOperationException("Image projection not yet implemented");
        }
        coverage.paint(graphics);
        return XAffineTransform.transform(context.getAffineTransform(RenderingContext.WORLD_TO_POINT), geographicArea, pointArea);
    }

    /**
     * Méthode appelée automatiquement pour construire une chaîne de caractères représentant la valeur
     * pointée par la souris. En général (mais pas obligatoirement), lorsque cette méthode est appelée,
     * le buffer <code>toAppendTo</code> contiendra déjà une chaîne de caractères représentant les
     * coordonnées pointées par la souris. Cette méthode y ajoutera la valeur du pixel sous le curseur
     * de la souris.
     *
     * @param  event Coordonnées du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette méthode a ajouté des informations dans <code>toAppendTo</code>.
     */
    protected synchronized boolean getLabel(final GeoMouseEvent event, final StringBuffer toAppendTo)
    {
        point  = event.getCoordinate(coverage.getCoordinateSystem(), point);
        values = coverage.evaluate(point, values);
        if (categories==null)
        {
            final List<SampleDimension> dimensions = coverage.getSampleDimensions();
            final CategoryList[] categories = new CategoryList[dimensions.size()];
            for (int i=0; i<categories.length; i++)
                categories[i] = dimensions.get(i).getCategoryList();
        }
        boolean modified = false;
        for (int i=0; i<values.length; i++)
        {
            final String text = categories[i].format(values[i], /*Locale*/null);
            if (text!=null)
            {
                if (modified)
                    toAppendTo.append(", ");
                toAppendTo.append(text);
                modified = true;
            }
        }
        return modified;
    }
}
