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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.ct;

// Coordinates
import net.seas.opengis.cs.Info;
import net.seas.opengis.cs.CoordinateSystem;


/**
 * Describes a coordinate transformation.
 * This class only describes a coordinate transformation, it does not
 * actually perform the transform operation on points.  To transform
 * points you must use a math transform.
 *
 * The math transform will transform positions in the source coordinate
 * system into positions in the target coordinate system.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_CoordinateSystem
 */
public abstract class CoordinateTransformation extends Info
{
    /**
     * Construct a coordinate transformation.
     */
    protected CoordinateTransformation()
    {}

    /**
     * Human readable description of domain in source coordinate system.
     */
    public abstract String getAreaOfUse();

    /**
     * Gets the semantic type of transform.
     * For example, a datum transformation or a coordinate conversion.
     */
    public abstract TransformType getTransformType();

    /**
     * Gets the source coordinate system.
     */
    public abstract CoordinateSystem getSourceCS();

    /**
     * Gets the target coordinate system.
     */
    public abstract CoordinateSystem getTargetCS();

    /**
     * Gets the math transform.
     */
    public abstract MathTransform getMathTransform();
}
