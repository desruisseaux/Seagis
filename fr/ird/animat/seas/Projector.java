/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains formulas from the PROJ package of USGS.
 *    USGS's work is fully acknowledged here.
 */
package fr.ird.animat.seas;

// Divers
import java.awt.geom.Point2D;
import net.seagis.cs.Ellipsoid;
import net.seagis.resources.XMath;


/**
 * Un point qui peut être projetées d'une coordonnées géographiques vers une
 * coordonnées projetées, et vis-versa. Cette classe utilise la projection
 * cylindrique de Mercator sur une terre sphérique. Les parallèles et les
 * méridients apparaissent comme des lignes droites et se croisent à angles
 * droits; cette projection produit donc des cartes rectangulaires. L'échelle
 * est vrai le long de deux parallèles équidistants de l'équateur. Cette
 * projection est utilisée pour représenter des régions près de l'équateur.
 * Elle est aussi souvent utilisée pour la navigation maritime parce que toutes
 * les lignes droites sur la carte sont des lignes <em>loxodromiques</em>,
 * c'est-à-dire qu'un navire suivant cette ligne garderait un azimuth constant
 * sur son compas.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Projector extends Point2D.Double
{
    /**
     * Ellipsoid à utiliser.
     */
    private static final Ellipsoid ellipsoid = Ellipsoid.WGS84;

    /**
     * Central meridian.
     */
    private double centralMeridian;

    /**
     * Central latitude.
     */
    private double centralLatitude;

    /**
     * False northing.
     */
    private double falseNorthing;

    /**
     * Global scale factor.
     */
    private double ak0 = ellipsoid.getSemiMajorAxis();

    /**
     * Construct a new map projection.
     */
    public Projector()
    {}

    /**
     * Set the central latitude, in degrees.
     */
    public void setCentre(final Point2D centre)
    {
        centralMeridian = Math.toRadians(centre.getX());
        centralLatitude = Math.toRadians(centre.getY());
        final double   sinY = Math.sin(centralLatitude);
        final double   cosY = Math.cos(centralLatitude);
        final double invRadius = XMath.hypot(sinY/ellipsoid.getSemiMajorAxis(),
                                             cosY/ellipsoid.getSemiMinorAxis());
        ak0 = cosY / invRadius;
        falseNorthing = -ak0 * Math.log(Math.tan((Math.PI/4) + 0.5*centralLatitude));
    }

    /**
     * Transforms this point from geographic
     * coordinates to projected coordinates.
     */
    public void toProjected()
    {
        x = ak0 * (Math.toRadians(x)-centralMeridian);
        y = ak0 * Math.log(Math.tan((Math.PI/4) + 0.5*Math.toRadians(y))) + falseNorthing;
    }

    /**
     * Transforms this point from projected
     * coordinates to geographic coordinates.
     */
    public void toGeographic()
    {
        x = Math.toDegrees(x/ak0 + centralMeridian);
        y = Math.toDegrees((Math.PI/2) - 2*Math.atan(Math.exp((falseNorthing-y)/ak0)));
    }
}
