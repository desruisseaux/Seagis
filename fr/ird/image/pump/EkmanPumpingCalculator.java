/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Universidad de Las Palmas de Gran Canaria
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
 * Contact: Antonio Ramos
 *          Departamento de Biologia ULPGC
 *          Campus Universitario de Tafira
 *          Edificio de Ciencias Basicas
 *          35017
 *          Las Palmas de Gran Canaria
 *          Spain
 *
 *          mailto:antonio.ramos@biologia.ulpgc.es
 */
package fr.ird.image.pump;

// Coordinates
import java.awt.geom.Point2D;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools dependencies
import org.geotools.ct.MathTransform2D;


/**
 * Base class for Ekman pumping computation.
 *
 * @version $Id$
 * @author Josep Coca
 * @author Martin Desruisseaux
 */
abstract class EkmanPumpingCalculator {
    /** Limits of equator band (°N/S). */ private   static final double equatorBandLimit     =      0;
    /** Air density           (kg/m³). */ protected static final double airDensity           =    1.3;
    /** Water density         (kg/m³). */ protected static final double waterDensity         = 1024.8;
    /** Earth radius              (m). */ private   static final double earthRadius          = 6378137.0;
    /** Earth angular velocity  (1/s). */ private   static final double earthAngularVelocity = (2*Math.PI)/(3600*(24-1/365.2425));
    /** Coriolis parameter      (1/s). */ protected              double coriolisParameter    = (2*earthAngularVelocity); // To be computed
    /** Drag coefficient <var>a</var>. */ private   static final double drag_a               = 0.0003;
    /** Drag coefficient <var>b</var>. */ private   static final double drag_b               = 0.00003;

    /**
     * Map projection for conversions between geographic coordinates
     * (<var>latitude</var>,<var>longitude</var>) and cartesien
     * coordinates (<var>x</var>,<var>y</var>).
     */
    private final MathTransform2D projection = null;

    /**
     * Temporary point used for internal computation.
     */
    private final Point2D.Double point=new Point2D.Double();

    /**
     * Construct an Ekman pumping calculator.
     */
    public EkmanPumpingCalculator() {
    }

    /**
     * Compute Emkan pumping given the wind at 4 surrounding point. Each point has
     * its coordinates expressed in radians of longitude and latitude, and its wind
     * vector as (<var>u</var>,<var>v</var>) components. The wind vector point
     * <u>toward</u> the direction where the wind is goind. If an argument is unknow,
     * it should be {@link Double#NaN}.
     *
     * @param  x1 Longitude (in degrees) of the first point.
     * @param  y1 Latitude  (in degrees) of the first point.
     * @param  u1 wind vectors's <var>u</var> component (m/s) at the first point.
     * @param  v1 wind vectors's <var>v</var> component (m/s) at the first point.
     *
     * @param  x2 Longitude (in degrees) of the second point.
     * @param  y2 Latitude  (in degrees) of the second point.
     * @param  u2 wind vectors's <var>u</var> component (m/s) at the second point.
     * @param  v2 wind vectors's <var>v</var> component (m/s) at the second point.
     *
     * @param  x3 Longitude (in degrees) of the third point.
     * @param  y3 Latitude  (in degrees) of the third point.
     * @param  u3 wind vectors's <var>u</var> component (m/s) at the third point.
     * @param  v3 wind vectors's <var>v</var> component (m/s) at the third point.
     *
     * @param  x4 Longitude (in degrees) of the fourth point.
     * @param  y4 Latitude  (in degrees) of the fourth point.
     * @param  u4 wind vectors's <var>u</var> component (m/s) at the fourth point.
     * @param  v4 wind vectors's <var>v</var> component (m/s) at the fourth point.
     *
     * @param  xo Longitude (in degrees) where to compute Ekman pumping.
     * @param  yo Latitude  (in degrees) where to compute Ekman pumping.
     *
     * @return Ekman pumping (in m/s), or {@link Double#NaN} if the
     *         pumping can not be computed with the given arguments.
     * @throws TransformException if a map projection failed.
     */
    public final double pumping(double x1, double y1, final double u1, final double v1,
                                double x2, double y2, final double u2, final double v2,
                                double x3, double y3, final double u3, final double v3,
                                double x4, double y4, final double u4, final double v4,
                                double xo, double yo) throws TransformException
    {
        if (Math.abs(yo) < equatorBandLimit) {
            return Double.NaN;
        }
        /*
         * Compute latitude-dependants parameters.
         */
        final double radians_yo = Math.toRadians(yo);
        coriolisParameter = (2*earthAngularVelocity) * Math.sin(radians_yo);

        if (projection != null) {
            /*
             * Method 1: Project coordinate using a map projection (SLOW!)
             */
            if (xo > 180) xo -= 360;
            if (x1 > 180) x1 -= 360;
            if (x2 > 180) x2 -= 360;
            if (x3 > 180) x3 -= 360;
            if (x4 > 180) x4 -= 360;
            /*
             * Set the map projection and get the projected coordinate
             * of the point where to compute the Ekman pumping.
             */
            point.x = xo;
            point.y = yo;
            projection.transform(point, point);
            xo = point.x;
            yo = point.y;
            /*
             * Project the four points and compute the Ekman pumping in cartesian coordinates.
             */
            point.x=x1; point.y=y1; projection.transform(point, point); x1=point.x-xo; y1=point.y-yo;
            point.x=x2; point.y=y2; projection.transform(point, point); x2=point.x-xo; y2=point.y-yo;
            point.x=x3; point.y=y3; projection.transform(point, point); x3=point.x-xo; y3=point.y-yo;
            point.x=x4; point.y=y4; projection.transform(point, point); x4=point.x-xo; y4=point.y-yo;
        }
        else
        {
            /*
             * Method 2: Compute only distance relative point xo,yo (faster than
             *           map projection, but valid only for small distances).
             */
            final double k0 = earthRadius * Math.cos(radians_yo);
            x1 = k0 * Math.toRadians(x1-xo);    y1 = earthRadius * Math.toRadians(y1-yo);
            x2 = k0 * Math.toRadians(x2-xo);    y2 = earthRadius * Math.toRadians(y2-yo);
            x3 = k0 * Math.toRadians(x3-xo);    y3 = earthRadius * Math.toRadians(y3-yo);
            x4 = k0 * Math.toRadians(x4-xo);    y4 = earthRadius * Math.toRadians(y4-yo);
        }
        return pumpingUsingCartesianCoord(x1,y1,u1,v1 , x2,y2,u2,v2 , x3,y3,u3,v3 , x4,y4,u4,v4);
    }

    /**
     * Compute Ekman pumping given the wind at 4 surrounding point. Each point has its
     * coordinates projected in a local cartesien coordinate system, and its wind vector
     * as (<var>u</var>,<var>v</var>) components. The wind vector point <u>toward</u> the
     * direction where the wind is going. The point where to compute Ekman pumping is
     * located at (0,0). If an argument is unknow, it should be {@link Double#NaN}.
     *
     * @param  x1 <var>x</var> coordinate (in metres) of the first point.
     * @param  y1 <var>y</var> coordinate (in metres) of the first point.
     * @param  u1 wind vectors's <var>u</var> component (m/s) at the first point.
     * @param  v1 wind vectors's <var>v</var> component (m/s) at the first point.
     *
     * @param  x2 <var>x</var> coordinate (in metres)  of the second point.
     * @param  y2 <var>y</var> coordinate (in metres) of the second point.
     * @param  u2 wind vectors's <var>u</var> component (m/s) at the second point.
     * @param  v2 wind vectors's <var>v</var> component (m/s) at the second point.
     *
     * @param  x3 <var>x</var> coordinate (in metres) of the third point.
     * @param  y3 <var>y</var> coordinate (in metres) of the third point.
     * @param  u3 wind vectors's <var>u</var> component (m/s) at the third point.
     * @param  v3 wind vectors's <var>v</var> component (m/s) at the third point.
     *
     * @param  x4 <var>x</var> coordinate (in metres) of the fourth point.
     * @param  y4 <var>y</var> coordinate (in metres) of the fourth point.
     * @param  u4 wind vectors's <var>u</var> component (m/s) at the fourth point.
     * @param  v4 wind vectors's <var>v</var> component (m/s) at the fourth point.
     *
     * @return Ekman pumping (in m/s) at the cartesien coordinate (0,0), or
     *         {@link Double#NaN} if the pumping can not be computed with
     *         the given arguments.
     */
    protected abstract double pumpingUsingCartesianCoord(final double x1, final double y1, final double u1, final double v1,
                                                         final double x2, final double y2, final double u2, final double v2,
                                                         final double x3, final double y3, final double u3, final double v3,
                                                         final double x4, final double y4, final double u4, final double v4);

    /**
     * Convenience method computing Ekman pumping from arrays.
     *
     * @param positions An array of length 8 containing the
     *        <code>(x1,y1),(x2,y2),(x3,y3),(x4,y4)</code> coordinates.
     * @param velocityComponents An array of length 8 containing the
     *        <code>(u1,v1),(u2,v2),(u3,v3),(u4,v4)</code> components.
     */
    final double pumpingUsingCartesianCoord(final double[] positions, final double[] velocityComponents)
    {
        return pumpingUsingCartesianCoord(positions[0], positions[1], velocityComponents[0], velocityComponents[1],
                                          positions[2], positions[3], velocityComponents[2], velocityComponents[3],
                                          positions[4], positions[5], velocityComponents[4], velocityComponents[5],
                                          positions[6], positions[7], velocityComponents[6], velocityComponents[7]);
    }

    /**
     * Convenience method computing Ekman pumping from arrays.
     *
     * @param positions An array of length 8 containing the
     *        <code>(x1,y1),(x2,y2),(x3,y3),(x4,y4)</code> coordinates.
     * @param velocityComponents An array of length 8 containing the
     *        <code>(u1,v1),(u2,v2),(u3,v3),(u4,v4)</code> components.
     * @param  xo Longitude (in degrees) where to compute Ekman pumping.
     * @param  yo Latitude  (in degrees) where to compute Ekman pumping.
     *
     * @return Ekman pumping (in m/s), or {@link Double#NaN} if the
     *         pumping can not be computed with the given arguments.
     * @throws TransformException if a map projection failed.
     */
    final double pumping(final double[] positions, final double[] velocityComponents,
                         final double xo, final double yo) throws TransformException
    {
        return pumping(positions[0], positions[1], velocityComponents[0], velocityComponents[1],
                       positions[2], positions[3], velocityComponents[2], velocityComponents[3],
                       positions[4], positions[5], velocityComponents[4], velocityComponents[5],
                       positions[6], positions[7], velocityComponents[6], velocityComponents[7],
                       xo, yo);
    }

    /**
     * Compute wind stress from wind velocity.
     *
     * @param  u wind vectors's <var>u</var> component (m/s).
     * @param  v wind vectors's <var>v</var> component (m/s).
     * @return Wind stress (N/m²).
     */
    static double dragCoefficientTimeSpeed(final double u, final double v) {
        final double speed = Math.sqrt(u*u + v*v);
        return  (drag_a + drag_b*speed)*speed;
    }
}
