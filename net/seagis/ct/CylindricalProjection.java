/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
 *              1999, Fisheries and Oceans Canada
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains formulas from the PROJ package of USGS.
 *    USGS's work is fully acknowledged here.
 */
package net.seagis.ct;

// OpenGIS (SEAS) dependencies
import net.seagis.cs.Projection;


/**
 * Classe de base des projections cartographiques cylindriques. Les projections
 * cylindriques consistent � projeter la surface de la Terre sur un cylindre tangeant ou s�cant
 * � la Terre. Les parall�les et mes m�ridiens apparaissent habituellement comme des lignes droites.
 *
 * On peut trouver plus de d�tails sur les projections cylindriques � l'adresse
 * <a href="http://everest.hunter.cuny.edu/mp/cylind.html">http://everest.hunter.cuny.edu/mp/cylind.html</a>.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/CylindricalProjection.png"></p>
 * <p align="center">Repr�sentation d'une projection cylindrique<br>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class CylindricalProjection extends MapProjection
{
    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected CylindricalProjection(final Projection parameters) throws MissingParameterException
    {super(parameters);}
}