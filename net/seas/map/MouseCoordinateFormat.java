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
package net.seas.map;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.cs.GeographicCoordinateSystem;
import net.seas.opengis.ct.CannotCreateTransformException;

// Miscellaneous
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;
import net.seas.awt.ExceptionMonitor;
import net.seas.text.CoordinateFormat;


/**
 * Formateurs des coordonnées pointées par le curseur de la souris. Les instances de cette classe pourront
 * écrire les coordonnées pointées ainsi qu'une éventuelle valeurs sous cette coordonnées (par exemple la
 * température sur une image satellitaire de température). Cette classe est optimisée pour être appelée
 * souvent, par exemple à l'intérieur d'une méthode {@link java.awt.event.MouseMotionListener#mouseMoved}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class MouseCoordinateFormat
{
    /**
     * Transformation à utiliser pour passer du système de coordonnées de
     * l'affichage vers le système de coordonnées à utiliser pour l'écriture.
     */
    private CoordinateTransform transform;

    /**
     * Formateur des coordonnées géographiques.
     */
    private final CoordinateFormat coordinateFormat=new CoordinateFormat("DD°MM.m'");

    /**
      * Buffer pour l'écriture des coordonnées.
      */
    private final StringBuffer buffer=new StringBuffer();

    /**
     * Point dans lequel mémoriser le résultat des projections.
     */
    private final Point2D point=new Point2D.Double();

    /**
     * Indique si la méthode {@link #format} doit écrire la valeur après la coordonnée.
     * Les valeurs sont obtenues en appelant la méthode {@link Layer#getLabel}. Par
     * Par défaut, les valeurs (si elles sont disponibles) sont écrites.
     */
    private boolean valueVisible=true;

    /**
     * Construit un objet qui écrira les coordonnées pointées par le
     * curseur de la souris. Les coordonnées seront écrites selon le
     * système de coordonnées par défaut "WGS 1984".
     */
    public MouseCoordinateFormat()
    {this(GeographicCoordinateSystem.WGS84);}

    /**
     * Construit un objet qui écrira les coordonnées pointées par le
     * curseur de la souris. Les coordonnées seront écrites selon le
     * système de coordonnées spécifié.
     */
    public MouseCoordinateFormat(final CoordinateSystem system)
    {
        try
        {
            Contour.TRANSFORMS.createFromCoordinateSystems(system, system);
        }
        catch (CannotCreateTransformException exception)
        {
            // Should not happen, since we are just asking for an identity transform/
            ExceptionMonitor.unexpectedException("net.seas.map", "MouseCoordinateFormat", "<init>", exception);
        }
    }

    /**
     * Indique si la méthode {@link #format} doit écrire la valeur après la coordonnée.
     * Les valeurs sont obtenues en appelant la méthode {@link Layer#getLabel}. Par
     * Par défaut, les valeurs (si elles sont disponibles) sont écrites.
     */
    public boolean isValueVisible()
    {return valueVisible;}

    /**
     * Spécifie si la méthode {@link #format} doit aussi écrire la valeur après la coordonnée.
     * Si la valeur doit être écrite, elle sera déterminée en appelant {@link Layer#getLabel}.
     */
    public void setValueVisible(final boolean valueVisible)
    {this.valueVisible=valueVisible;}

    /**
     * Retourne une chaîne de caractères représentant les coordonnées pointées par le curseur
     * de la souris.  Les coordonnées seront écrites selon le système de coordonnées spécifié
     * au constructeur. Si une des couches peut ajouter une valeur à la coordonnée (par exemple
     * une couche qui représente une image satellitaire de température) et que l'écriture des
     * valeurs est autorisée (voir {@link #isValueVisible}), alors la valeur sera écrite après
     * les coordonnées. Ces valeurs sont obtenues par des appels à {@link Layer#getLabel}.
     *
     * @param  event Evénements contenant les coordonnées de la souris.
     * @return Chaîne de caractères représentant les coordonnées pointées par le curseur de
     *         la souris, où <code>null</code> si ces coordonnées n'ont pas pu être déterminées.
     */
    public String format(final MouseEvent event)
    {
        final Object source=event.getSource();
        if ((source instanceof MapPanel) && (event instanceof GeoMouseEvent))
        {
            final MapPanel mapPanel = (MapPanel) source;
            if (mapPanel.hasLayers())
            {
                final GeoMouseEvent geoEvent = (GeoMouseEvent) event;
                Point2D point=geoEvent.getVisualCoordinate(this.point);
                if (point!=null) try
                {
                    transform = geoEvent.getTransformToTarget(transform);
                    point     = transform.transform(point, point);
                    buffer.setLength(0);
                    coordinateFormat.format(point.getX(), point.getY(), buffer, null);
                    if (valueVisible)
                    {
                        final int length=buffer.length();
                        buffer.append("  (");
                        if (mapPanel.getLabel(geoEvent, buffer))
                        {
                            buffer.append(')');
                        }
                        else buffer.setLength(length);
                    }
                    return buffer.toString();
                }
                catch (TransformException exception)
                {
                    mapPanel.handleException("MouseCoordinateFormat", "format", exception);
                }
            }
        }
        return null;
    }
}
