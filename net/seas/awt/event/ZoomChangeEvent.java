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
package net.seas.awt.event;

// Dependencies
import java.util.EventObject;
import java.awt.geom.AffineTransform;


/**
 * An event which indicates that a zoom occurred in a component.
 * This event is usually fire by {@link net.seas.awt.ZoomPane}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class ZoomChangeEvent extends EventObject
{
    /**
     * Transformation affine qui représente le changement dans le zoom.
     * Soit <code>oldZoom</code> et <code>newZoom</code> les transformations
     * affines de l'ancien et du nouveau zoom respectivement. Alors
     * <code>newZoom=oldZoom.concatenate(change)</code>.
     */
    private final AffineTransform change;

    /**
     * Construct a new event.
     *
     * @param source The event source (usually a {@link net.seas.awt.ZoomPane}.
     * @param change An affine transform indicating the zoom change. If
     *               <code>oldZoom</code> and <code>newZoom</code> are the affine
     *               transform before and after the change respectively, then the
     *               following relation must be respected (in the limit of rounding error):
     *               <code>newZoom=oldZoom.{@link AffineTransform#concatenate concatenate}(change)</code>
     */
    public ZoomChangeEvent(final Object source, final AffineTransform change)
    {
        super(source);
        this.change=change;
    }

    /**
     * Returns the affine transform indicating the zoom change.
     * Note: for performance raisons, this method do not clone
     * the returned transform. Do not change!
     */
    public AffineTransform getChange()
    {return change;}

    /**
     * Combine cet évènement avec un nouvel événement <code>ZoomChangeEvent</code>.
     * Cette méthode est placée ici comme un rappel en attendant qu'un mécanisme
     * standard (dans le JDK 1.4?) devienne public.
     */
//  private static ZoomChangeEvent coalesceEvents(final ZoomChangeEvent oldEvent, final ZoomChangeEvent newEvent)
//  {
//      oldEvent.change.concatenate(newEvent.change);
//      return oldEvent;
//  }
}
