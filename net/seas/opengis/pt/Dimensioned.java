/*
 * OpenGIS implementation in Java
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
package net.seas.opengis.pt;


/**
 * Interface for dimensioned object.
 * Objects implementing this interface may have an arbitrary number of dimensions.
 * It is generally not possible to determine their dimension only by their type.
 * For example a {@link CoordinatePoint} may very well be one-dimensional (e.g.
 * a height), two-dimensional (e.g. a geographic coordinate), three-dimensional,
 * etc. The actual number of dimension can be queried by {@link #getDimension}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Dimensioned
{
    /**
     * Returns the number of dimensions.
     */
    public abstract int getDimension();
}