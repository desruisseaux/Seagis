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


/**
 * Classe de base des projections cartographiques azimuthales (ou planaires).
 * On peut trouver plus de d�tails sur les projections azimuthales � l'adresse
 * <a href="http://everest.hunter.cuny.edu/mp/plane.html">http://everest.hunter.cuny.edu/mp/plane.html</a>.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="{@docRoot}/doc-files/images/map/PlanarProjection.png"></p>
 * <p align="center">Repr�sentation d'une projection planaire<br>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class PlanarProjection extends MapProjection
{
    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    public PlanarProjection(final Parameter[] parameters) throws MissingParameterException
    {super(parameters);}
}
