/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.science.astro;

// Geotools.
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.TransformException;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeocentricCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.pt.CoordinatePoint;

/**
 * Calcul l'angle zénithal et l'angle de visé (ou angle d'acquisition) du satellite relatif 
 * à une position géographique. <BR><BR>
 *
 * Le schéma ci-dessous présente l'angle de visé (angle <i>B</i>) et l'angle zénithal 
 * (angle <i>A</i> + angle <i>B</i>) du satellite.<BR><BR>
 *
 * <IMG SRC="doc-files/compute_angle.png"><BR><BR>
 *
 * Note : L'ensemble des calculs sont basés sur le système de coordonnées géographique 
 * WGS_84.
 *
 * @verison $Id$
 * @author Remi Eve
 */ 
public final class SatelliteRelativeAngle
{
    /** Angle zenithal du satellite (A + B). */
    private double zenithalAngle;
    
    /** Angle de visé du satellite (angle B). */
    private double sensorAngle;
    
    /** Transformation du systeme géographique vers le systeme géocentric. */
    private final CoordinateTransformation transformation;    
        
    /** Système de coordonnées géocentrique utilisé. */
    final CoordinateSystem targetCS = GeocentricCoordinateSystem.DEFAULT;

    /** Système de coordonnées géographique utilisé. */
    final CoordinateSystem sourceCS = GeographicCoordinateSystem.WGS84;

    
    /**
     * Construit un objet de type SatteliteRelativeAngle.
     */
    public SatelliteRelativeAngle() 
    {
        /* Construit la transformation du système de coordonnées géographiques vers le 
           système géocentriques. */
        try 
        {
            CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();
            transformation = factory.createFromCoordinateSystems(sourceCS, targetCS);
        }
        catch (CannotCreateTransformException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }                                 
    }    
    
    
    /**
     * Calcul l'angle zénithal du satellite et l'angle de visé du satellite par rapport 
     * à un point de la surface du globe.
     *
     * @param satGeog    Position du satellite dans le système géographique WGS_84 (x,y,z).
     * @param altitude   Altitude du satellite en mètre.
     * @param ptGeog     Position du point  (ou de l'observateur) dans le système 
     *                   géographique WGS_84 (x,y,z).
     */
    public void compute(final CoordinatePoint satGeog,
                        final double          altitude,
                        final CoordinatePoint ptGeog) throws Exception 
    {
        // Transformation des points dans le système géocentric.
        final CoordinatePoint ptGeoc  = new CoordinatePoint(3),        
                              satGeoc = new CoordinatePoint(3);        
        transformation.getMathTransform().transform(satGeog, satGeoc);
        transformation.getMathTransform().transform(ptGeog,  ptGeoc);

        /* Calcul des rayons RN et RA correspondant aux normes des vecteurs respectifs 
           ON et OA. "O" representant le centre du globe. */
        double RN = 0,
               RA = 0;
        for (int d=0; d<targetCS.getDimension(); d++)
        {
            RA += ptGeoc.getOrdinate(d)*ptGeoc.getOrdinate(d);
            RN += satGeoc.getOrdinate(d)*satGeoc.getOrdinate(d);
        }
        RA = Math.sqrt(RA);
        RN = Math.sqrt(RN);
        
        /* Calcul de l'angle "a". "a" étant l'angle forme entre les vecteurs ON et OA.
           ON.OA = R*R*cos a. */
        double pScal = 0;
        for (int d=0 ; d<targetCS.getDimension(); d++)
            pScal += satGeoc.getOrdinate(d) * ptGeoc.getOrdinate(d);
        final double a = Math.acos(Math.min(Math.max(pScal / (RA*RN), 0),1));
        
        /* Calcul de d1. */
        final double d1 = RA*Math.cos(a);

        /* Calcul de d2. */
        final double d2 = RN - d1;

        /* Calcul de d. */
        final double d = d2 + altitude;        
                
        /* Calcul de d3. */
        final double d3 = RA*Math.sin(a);
        
        /* Calcul de l'angle "b". */
        sensorAngle = Math.toDegrees(Math.atan((d3 / d)));
        
        /* Calcul de l'angle zenital du satellite egal a "a" + "b". */
        zenithalAngle = Math.toDegrees(a) + sensorAngle;
    }    
        
    /**
     * Retourne l'angle zenital du satellite.
     * @return l'angle zenital du satellite.
     */
    public double getZenithalAngle()
    {
        return zenithalAngle;
    }
    
    /**
     * Retourne l'angle de visé du satellite.
     * @return l'angle de visé du satellite.
     */
    public double getSensorAngle()
    {
        return sensorAngle;
    }    
}