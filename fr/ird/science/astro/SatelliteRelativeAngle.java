/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
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
 * Calcul l'angle z�nithal et l'angle de vis� (ou angle d'acquisition) du satellite relatif 
 * � une position g�ographique. <BR><BR>
 *
 * Le sch�ma ci-dessous pr�sente l'angle de vis� (angle <i>B</i>) et l'angle z�nithal 
 * (angle <i>A</i> + angle <i>B</i>) du satellite.<BR><BR>
 *
 * <IMG SRC="doc-files/compute_angle.png"><BR><BR>
 *
 * Note : L'ensemble des calculs sont bas�s sur le syst�me de coordonn�es g�ographique 
 * WGS_84.
 *
 * @verison $Id$
 * @author Remi Eve
 */ 
public final class SatelliteRelativeAngle
{
    /** Angle zenithal du satellite (A + B). */
    private double zenithalAngle;
    
    /** Angle de vis� du satellite (angle B). */
    private double sensorAngle;
    
    /** Transformation du systeme g�ographique vers le systeme g�ocentric. */
    private final CoordinateTransformation transformation;    
        
    /** Syst�me de coordonn�es g�ocentrique utilis�. */
    final CoordinateSystem targetCS = GeocentricCoordinateSystem.DEFAULT;

    /** Syst�me de coordonn�es g�ographique utilis�. */
    final CoordinateSystem sourceCS = GeographicCoordinateSystem.WGS84;

    
    /**
     * Construit un objet de type SatteliteRelativeAngle.
     */
    public SatelliteRelativeAngle() 
    {
        /* Construit la transformation du syst�me de coordonn�es g�ographiques vers le 
           syst�me g�ocentriques. */
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
     * Calcul l'angle z�nithal du satellite et l'angle de vis� du satellite par rapport 
     * � un point de la surface du globe.
     *
     * @param satGeog    Position du satellite dans le syst�me g�ographique WGS_84 (x,y,z).
     * @param altitude   Altitude du satellite en m�tre.
     * @param ptGeog     Position du point  (ou de l'observateur) dans le syst�me 
     *                   g�ographique WGS_84 (x,y,z).
     */
    public void compute(final CoordinatePoint satGeog,
                        final double          altitude,
                        final CoordinatePoint ptGeog) throws Exception 
    {
        // Transformation des points dans le syst�me g�ocentric.
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
        
        /* Calcul de l'angle "a". "a" �tant l'angle forme entre les vecteurs ON et OA.
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
     * Retourne l'angle de vis� du satellite.
     * @return l'angle de vis� du satellite.
     */
    public double getSensorAngle()
    {
        return sensorAngle;
    }    
}