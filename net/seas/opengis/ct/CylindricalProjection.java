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
 *
 *    This package contains formulas from the PROJ package of USGS.
 *    USGS's work is fully acknowledged here.
 */
package net.seas.opengis.ct;

// Coordinates
import net.seas.opengis.cs.Ellipsoid;
import java.awt.geom.Point2D;


/** 
 * Classe de base des projections cartographiques cylindriques. Les projections
 * cylindriques consistent � projeter la surface de la Terre sur un cylindre tangeant ou s�cant
 * � la Terre. Les parall�les et mes m�ridiens apparaissent habituellement comme des lignes droites.
 *
 * On peut trouver plus de d�tails sur les projections cylindriques � l'adresse
 * <a href="http://everest.hunter.cuny.edu/mp/cylind.html">http://everest.hunter.cuny.edu/mp/cylind.html</a>.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="{@docRoot}/doc-files/images/map/CylindricalProjection.png"></p>
 * <p align="center">Repr�sentation d'une projection cylindrique<br>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class CylindricalProjection extends MapProjection
{
    /**
     * Construit une projection cartographique
     * qui utilisera l'ellipso�de sp�cifi�.
     */
    public CylindricalProjection(final Ellipsoid ellipsoid, final Point2D centroid)
    {super(ellipsoid, centroid);}
}
