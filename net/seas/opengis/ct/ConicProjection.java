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
 * Classe de base des projections cartographiques coniques. Les projections
 * coniques consistent � projeter la surface de la Terre sur un c�ne tangeant ou s�cant � la
 * Terre. Les parall�les apparaissent habituellement comme des arcs de cercles et les m�ridiens
 * comme des lignes droites. Les projections coniques ne sont pas tr�s utilis�s du fait que
 * leurs distorsions augmentent rapidement � mesure que l'on s'�loigne des parall�les standards.
 * Elles sont plut�t utilis�es pour les r�gions aux latitudes moyennes qui s'�tendent sur une
 * large r�gion d'est en ouest, comme les Etats-Unis.
 *
 * On peut trouver plus de d�tails sur les projections coniques � l'adresse
 * <a href="http://everest.hunter.cuny.edu/mp/conic.html">http://everest.hunter.cuny.edu/mp/conic.html</a>.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="{@docRoot}/doc-files/images/map/ConicProjection.png"></p>
 * <p align="center">Repr�sentation d'une projection conique<br>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class ConicProjection extends MapProjection
{
    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected ConicProjection(final Parameter[] parameters) throws MissingParameterException
    {super(parameters);}
}