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

// Events
import java.util.EventListener;


/**
 * Defines an object which listens for zoom change events. Zoom change
 * are indicated by an {@link java.awt.geom.AffineTransform}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface ZoomChangeListener extends EventListener
{
    /**
     * Invoked when a zoom changed.
     */
    public abstract void zoomChanged(final ZoomChangeEvent event);
}
