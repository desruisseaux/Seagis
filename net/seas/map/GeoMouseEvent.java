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
package net.seas.map;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.ct.CoordinateTransformFactory;
import net.seas.opengis.ct.CannotCreateTransformException;

// Events
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

// Geometry
import java.awt.geom.Point2D;


/**
 * Ev�nements repr�sentant les coordonn�es g�ographiques du curseur de la souris.
 * Les objets {@link MouseListener} enregistr�s aupr�s de {@link MapPanel} recevront
 * des �v�nements de ce type. Ainsi, la m�thode {@link MouseListener#mouseClicked}
 * par exemple peut �tre impl�ment�e comme suit:
 *
 * <blockquote><pre>
 * &nbsp;public void mouseClicked(MouseEvent e)
 * &nbsp;{
 * &nbsp;    GeoMouseEvent event = (GeoMouseEvent) e;
 * &nbsp;    // Process event here...
 * &nbsp;}
 * </pre></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class GeoMouseEvent extends MouseEvent
{
    /**
     * Carte qui a construit cet �v�nement.
     */
    private final MapPanel mapPanel;

    /**
     * Transformation de coordonn�es convertissant les coordonn�es cartographiques d'un
     * syst�me quelconque vers le syst�me de coordonn�es logiques de l'affichage. Le syst�me
     * <code>projection.getTargetCS()</code> <u>doit</u> correspondre au syst�me de coordonn�es
     * affich� � l'�cran. Le syst�me <code>projection.getSourceCS()</code> peut �tre arbitraire,
     * mais il est conseill� (pour des raisons de performances) qu'il soit le syst�me ayant le
     * plus de chance d'�tre demand� par {@link #getCoordinate}.
     */
    private CoordinateTransform projection;

    /**
     * Coordonn�es <code>x</code> projet�e dans le syst�me
     * <code>projection.getSourceCS()</code>. Ce champ n'est
     * valide que si {@link #projected} est <code>true</code>.
     */
    private transient double px;

    /**
     * Coordonn�es <code>y</code> projet�e dans le syst�me
     * <code>projection.getSourceCS()</code>. Ce champ n'est
     * valide que si {@link #projected} est <code>true</code>.
     */
    private transient double py;

    /**
     * Indique si les coordonn�es {@link #px} et {@link #py}
     * sont valides. Ces coordonn�es ne seront calcul�es que
     * la premi�re fois o� elle seront demand�es.
     */
    private transient boolean projected;

    /**
     * Construit un �v�nements qui utilisera les m�mes param�tres que <code>event</code>.
     * Les coordonn�es en pixels pourront �tre converties en coordonn�es g�ographiques en
     * utilisant les param�tres de l'objet {@link MapPanel} sp�cifi�.
     *
     * @param event Ev�nement original.
     * @param mapPanel Carte ayant produit cet �v�nement.
     */
    public GeoMouseEvent(final MouseEvent event, final MapPanel mapPanel)
    {
        super(event.getComponent(),    // the Component that originated the event
              event.getID(),           // the integer that identifies the event
              event.getWhen(),         // a long int that gives the time the event occurred
              event.getModifiers(),    // the modifier keys down during event (shift, ctrl, alt, meta)
              event.getX(),            // the horizontal x coordinate for the mouse location
              event.getY(),            // the vertical y coordinate for the mouse location
              event.getClickCount(),   // the number of mouse clicks associated with event
              event.isPopupTrigger()); // a boolean, true if this event is a trigger for a popup-menu
        this.mapPanel = mapPanel;
    }

    /**
     * Retourne les coordonn�es pixel de cet �v�nement. Cette m�thode est � peu
     * pr�s �quivalente � {@link #getPoint}, mais tiendra compte de la pr�sence
     * de la loupe si elle est affich�e.
     *
     * @param  dest Un point pr�-alou� dans lequel m�moriser le r�sultat,
     *         ou <code>null</code> s'il n'y en a pas.
     * @return La coordonn�e pixel. Si <code>dest</code> �tait non-nul,
     *         alors il sera utilis� et retourn�.
     */
    public Point2D getPoint2D(Point2D dest)
    {
        if (dest!=null) dest.setLocation(getX(), getY());
        else dest=new Point2D.Double(getX(), getY());
        mapPanel.correctPointForMagnifier(dest);
        return dest;
    }

    /**
     * Retourne les coordonn�es logiques de l'endroit ou s'est produit l'�v�nement.
     * Les coordonn�es seront exprim�es selon le syst�me de coordonn�es de l'afficheur
     * {@link MapPanel}. Si la coordonn�e n'a pas pu �tre d�termin�e, alors
     * cette m�thode retourne <code>null</code>.
     *
     * @param  dest Un point pr�-alou� dans lequel m�moriser le r�sultat, ou <code>null</code>
     *         s'il n'y en a pas.
     * @return La coordonn�e logique, ou <code>null</code> si elle n'a pas pu �tre calcul�e.
     *         Si la coordonn�e a pu �tre obtenue et que <code>dest</code> est non-nul, alors
     *         une nouveau point sera automatiquement cr�� et retourn�.
     */
    public Point2D getVisualCoordinate(final Point2D dest)
    {return mapPanel.inverseTransform(getPoint2D(dest));}

    /**
     * Retourne les coordonn�es logiques de l'endroit o� s'est produit l'�v�nement.
     * Les coordonn�es seront exprim�es selon le syst�me de coordonn�es sp�cifi�.
     * Si la coordonn�e n'a pas pu �tre d�termin�e, alors cette m�thode retourne
     * <code>null</code>.
     *
     * @param  system Le syst�me de coordonn�es selon lequel on veut exprimer la coordonn�e.
     * @param  dest Un point pr�-alou� dans lequel m�moriser le r�sultat, ou <code>null</code>
     *         s'il n'y en a pas.
     * @return La coordonn�e logique, ou <code>null</code> si elle n'a pas pu �tre calcul�e.
     *         Si la coordonn�e a pu �tre obtenue et que <code>dest</code> est non-nul, alors
     *         une nouveau point sera automatiquement cr�� et retourn�.
     */
    public Point2D getCoordinate(final CoordinateSystem system, Point2D dest)
    {
        try
        {
            if (projection==null)
            {
                projection=mapPanel.getCommonestCoordinateTransform();
            }
            /*
             * Si le syst�me de coordonn�es sp�cifi� n'est pas
             * celui que l'on attendait, calcule � la vol� les
             * coordonn�es.
             */
            if (!system.equivalents(projection.getSourceCS()))
            {
                dest=getVisualCoordinate(dest);
                if (dest!=null)
                {
                    return Contour.TRANSFORMS.createFromCoordinateSystems(projection.getTargetCS(), system).transform(dest, dest);
                }
            }
            /*
             * Si le syst�me de coordonn�es est bien celui que l'on attendait
             * et qu'on avait d�j� calcul� les coordonn�es pr�c�demment, utilise
             * le r�sultat qui avait �t� conserv� dans la cache.
             */
            if (projected)
            {
                if (dest!=null)
                {
                    dest.setLocation(px,py);
                    return dest;
                }
                return new Point2D.Double(px,py);
            }
            /*
             * Calcule les coordonn�es et conserve le r�sultat dans
             * une cache interne pour les interogations subs�quentes.
             */
            dest=getVisualCoordinate(dest);
            if (dest!=null)
            {
                dest=projection.inverse().transform(dest, dest);
                if (dest instanceof Point2D.Double)
                {
                    px=dest.getX();
                    py=dest.getY();
                    projected=true;
                }
            }
            return dest;
        }
        /*
         * Si une projection cartographique a �chou�e, reporte
         * l'erreur � l'objet {@link MapPanel} qui avait lanc�
         * cet �v�nement.
         */
        catch (TransformException exception)
        {
            mapPanel.handleException("GeoMouseEvent", "getCoordinate", exception);
        }
        return null;
    }

    /**
     * Retourne une transformation qui convertira les coordonn�es du syst�me de l'affichage
     * vers le syst�me <code>cached.{@link #getTargetCS() getTargetCS()}</code>. Si la
     * transformation <code>cached</code> convient d�j�, elle sera retourn�e plut�t que
     * de cr�er une nouvelle transformation.
     */
    final CoordinateTransform getTransformToTarget(final CoordinateTransform cached) throws CannotCreateTransformException
    {
        if (projection==null)
        {
            projection=mapPanel.getCommonestCoordinateTransform();
        }
        final CoordinateSystem source=projection.getSourceCS();
        if (cached.getSourceCS().equivalents(source))
        {
            return cached;
        }
        final CoordinateSystem destination=cached.getTargetCS();
        if (projection.getTargetCS().equivalents(destination))
        {
            return projection;
        }
        return Contour.TRANSFORMS.createFromCoordinateSystems(source, destination);
    }
}
