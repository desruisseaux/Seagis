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

// Miscellaneous
import java.awt.geom.AffineTransform;


/**
 * Ekman pumping computation using 4 points.
 *
 * @version $Id$
 * @author Josep Coca
 * @author Martin Desruisseaux
 */
public class EkmanPumpingCalculator4P extends EkmanPumpingCalculator
{
    /**
     * Construct an Ekman pumping calculator.
     */
    public EkmanPumpingCalculator4P()
    {}

    /**
     * Compute Ekman pumping given the wind at 4 surrounding point. Each point has
     * its coordinates projected in a local cartesien coordinate system, and its
     * wind vector as (<var>u</var>,<var>v</var>) components. The wind vector point
     * <u>toward</u> the direction where the wind is going. The point where to compute
     * Ekman pumping is located at (0,0). If an argument is unknow, it should be
     * {@link Double#NaN}.
     * <br><br>
     * Current algorithm requires that the four points form a square. If they do not,
     * a slight error is introduced. The error is small if the shape is close to a square.
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
    protected double pumpingUsingCartesianCoord(final double x1, final double y1, final double u1, final double v1,
                                                final double x2, final double y2, final double u2, final double v2,
                                                final double x3, final double y3, final double u3, final double v3,
                                                final double x4, final double y4, final double u4, final double v4)
    {
        /*
         * Compute a unit vector from P2 to P3 and from P1 to P4.
         * All distances are in metres; unit vector are unitless.
         *
         * Note: All unit vector have prefix "û". We use "û" instead of "u" in
         *       order to avoid confusion with the "u" component of wind speed.
         */
        double dx = x3-x2;
        double dy = y3-y2;              
        //Diagonal of the square betwen point 3 ---> 2
        final double distance_2to3 = Math.sqrt(dx*dx + dy*dy);
        // Unit vector for the axis 3 ---> 2
        final double ûx_2to3 = dx / distance_2to3;
        final double ûy_2to3 = dy / distance_2to3;
        
        //Diagonal of the square betwen point 4 ---> 1  
        dx = x4-x1;
        dy = y4-y1;
        final double distance_1to4 = Math.sqrt(dx*dx + dy*dy);
        // Unit vector for the axis 4 ---> 1
        final double ûx_1to4 = dx / distance_1to4;
        final double ûy_1to4 = dy / distance_1to4;
        /*
         * Compute the velocity components relative to the normal unit vector.
         * (i.e. Project the velocity components to the new axis). All computed
         * velocity are in m/s.
         */
        final double determinant =  (ûx_1to4 * ûy_2to3)- (ûx_2to3 * ûy_1to4);
        final double nu1   =   (ûy_1to4 * u1  -  ûy_2to3 * v1) / determinant;
        final double nv1   =   (ûx_2to3 * v1  -  ûx_1to4 * u1) / determinant;
        final double nu2   =   (ûy_1to4 * u2  -  ûy_2to3 * v2) / determinant;
        final double nv2   =   (ûx_2to3 * v2  -  ûx_1to4 * u2) / determinant;
        final double nu3   =   (ûy_1to4 * u3  -  ûy_2to3 * v3) / determinant;
        final double nv3   =   (ûx_2to3 * v3  -  ûx_1to4 * u3) / determinant;
        final double nu4   =   (ûy_1to4 * u4  -  ûy_2to3 * v4) / determinant;
        final double nv4   =   (ûx_2to3 * v4  -  ûx_1to4 * u4) / determinant;
        /*
         * Compute the Ekman pumping in m/s: 'pumping_x' and 'pumping_y'
         * The substraction order depends on the sense of the axis diagonals
         * to the square (must be the same).
         */
        final double pumping_x = (dragCoefficientTimeSpeed(u4,v4)*nu4 - dragCoefficientTimeSpeed(u1,v1)*nu1) / distance_1to4;
        final double pumping_y = (dragCoefficientTimeSpeed(u3,v3)*nv3 - dragCoefficientTimeSpeed(u2,v2)*nv2) / distance_2to3;

        return airDensity * (pumping_y - pumping_x)  /  (coriolisParameter * waterDensity);
    }

    /**
     * Test this class.
     */
    public static void main(final String[]args)
    {
        final double[] positions=new double[]
        {2,2,6,2,2,6,6,6};
        
        final double[] velocityComponents=new double[]
        {20,-12,-5,14,2,-10,7,-2};

        EkmanPumpingCalculator4P calculator=new EkmanPumpingCalculator4P();
        System.out.print("Reference : ");
        System.out.println(calculator.pumpingUsingCartesianCoord(positions, velocityComponents));

        AffineTransform tr = AffineTransform.getRotateInstance(Math.toRadians(355));
        tr.transform     (positions,          0, positions,          0, positions.length         /2);
        tr.deltaTransform(velocityComponents, 0, velocityComponents, 0, velocityComponents.length/2);
        System.out.print("Rotated   : ");
        System.out.println(calculator.pumpingUsingCartesianCoord(positions ,velocityComponents));

        tr.setToScale(2, 2);
        tr.transform     (positions,          0, positions,          0, positions.length         /2);
        tr.deltaTransform(velocityComponents, 0, velocityComponents, 0, velocityComponents.length/2);
        System.out.print("Scaled    : ");
        System.out.println(calculator.pumpingUsingCartesianCoord(positions ,velocityComponents));

        tr.setToTranslation(457, -562);
        tr.transform     (positions,          0, positions,          0, positions.length         /2);
        tr.deltaTransform(velocityComponents, 0, velocityComponents, 0, velocityComponents.length/2);
        System.out.print("Translated: ");
        System.out.println(calculator.pumpingUsingCartesianCoord(positions ,velocityComponents));
    }
}
