/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
 * Copyright (C) 1999 P�ches et Oc�ans Canada
 *               2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.awt.series;

// Geometry
import java.awt.Shape;


/**
 * An interface for series.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface Series
{
    /**
     * Returns the name of this series.
     */
    public String getName();

    /**
     * Returns the series data as a path.
     * This path use logical coordinates.
     */
    public Shape getPath();
}
