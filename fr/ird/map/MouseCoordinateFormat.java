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
package fr.ird.map;

// Geotools dependencies
import org.geotools.pt.Latitude;
import org.geotools.pt.Longitude;
import org.geotools.pt.AngleFormat;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.CannotCreateTransformException;

// Miscellaneous
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;


/**
 * Formateurs des coordonn�es point�es par le curseur de la souris. Les instances de cette classe pourront
 * �crire les coordonn�es point�es ainsi qu'une �ventuelle valeurs sous cette coordonn�es (par exemple la
 * temp�rature sur une image satellitaire de temp�rature). Cette classe est optimis�e pour �tre appel�e
 * souvent, par exemple � l'int�rieur d'une m�thode {@link java.awt.event.MouseMotionListener#mouseMoved}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class MouseCoordinateFormat
{
    /**
     * Transformation � utiliser pour passer du syst�me de coordonn�es de
     * l'affichage vers le syst�me de coordonn�es � utiliser pour l'�criture.
     */
    private CoordinateTransformation transformation;

    /**
     * Formateur des coordonn�es g�ographiques.
     */
    private final AngleFormat angleFormat=new AngleFormat("DD�MM.m'");

    /**
      * Buffer pour l'�criture des coordonn�es.
      */
    private final StringBuffer buffer=new StringBuffer();

    /**
     * Point dans lequel m�moriser le r�sultat des projections.
     */
    private final Point2D point=new Point2D.Double();

    /**
     * Indique si la m�thode {@link #format} doit �crire la valeur apr�s la coordonn�e.
     * Les valeurs sont obtenues en appelant la m�thode {@link Layer#getLabel}. Par
     * Par d�faut, les valeurs (si elles sont disponibles) sont �crites.
     */
    private boolean valueVisible=true;

    /**
     * Construit un objet qui �crira les coordonn�es point�es par le
     * curseur de la souris. Les coordonn�es seront �crites selon le
     * syst�me de coordonn�es par d�faut "WGS 1984".
     */
    public MouseCoordinateFormat()
    {this(GeographicCoordinateSystem.WGS84);}

    /**
     * Construit un objet qui �crira les coordonn�es point�es par le
     * curseur de la souris. Les coordonn�es seront �crites selon le
     * syst�me de coordonn�es sp�cifi�.
     */
    public MouseCoordinateFormat(final CoordinateSystem system)
    {
        transformation = Contour.getIdentityTransform(system);
    }

    /**
     * Indique si la m�thode {@link #format} doit �crire la valeur apr�s la coordonn�e.
     * Les valeurs sont obtenues en appelant la m�thode {@link Layer#getLabel}. Par
     * Par d�faut, les valeurs (si elles sont disponibles) sont �crites.
     */
    public boolean isValueVisible()
    {return valueVisible;}

    /**
     * Sp�cifie si la m�thode {@link #format} doit aussi �crire la valeur apr�s la coordonn�e.
     * Si la valeur doit �tre �crite, elle sera d�termin�e en appelant {@link Layer#getLabel}.
     */
    public void setValueVisible(final boolean valueVisible)
    {this.valueVisible=valueVisible;}

    /**
     * Retourne une cha�ne de caract�res repr�sentant les coordonn�es point�es par le curseur
     * de la souris.  Les coordonn�es seront �crites selon le syst�me de coordonn�es sp�cifi�
     * au constructeur. Si une des couches peut ajouter une valeur � la coordonn�e (par exemple
     * une couche qui repr�sente une image satellitaire de temp�rature) et que l'�criture des
     * valeurs est autoris�e (voir {@link #isValueVisible}), alors la valeur sera �crite apr�s
     * les coordonn�es. Ces valeurs sont obtenues par des appels � {@link Layer#getLabel}.
     *
     * @param  event Ev�nements contenant les coordonn�es de la souris.
     * @return Cha�ne de caract�res repr�sentant les coordonn�es point�es par le curseur de
     *         la souris, o� <code>null</code> si ces coordonn�es n'ont pas pu �tre d�termin�es.
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
                    transformation = geoEvent.getTransformToTarget(transformation);
                    point = ((MathTransform2D) transformation.getMathTransform()).transform(point, point);
                    buffer.setLength(0);
                    angleFormat.format(new  Latitude(point.getY()), buffer, null);
                    buffer.append(' ');
                    angleFormat.format(new Longitude(point.getX()), buffer, null);
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
