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

// OpenGIS dependencies
import net.seas.opengis.pt.ConvexHull;
import net.seas.opengis.pt.CoordinatePoint;


/**
 * Transforms multi-dimensional coordinate points.
 * If a client application wishes to query the source and target
 * coordinate systems of a transformation, then it should keep hold
 * of the {@link CoordinateTransformation} interface, and use the
 * contained math transform object whenever it wishes to perform a
 * transform.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_MathTransform
 */
public abstract class MathTransform
{
    /**
     * Construct a math transform.
     */
    protected MathTransform()
    {}

    /**
     * Gets flags classifying domain points within a convex hull.
     * Conceptually, each of the (usually infinite) points inside
     * the convex hull is tested against the source domain.  The
     * flags of all these tests are then combined.  In practice,
     * implementations of different transforms will use different
     * short-cuts to avoid doing an infinite number of tests.
     *
     * @param  hull The convex hull.
     * @return flags classifying domain points within the convex hull.
     */
    public abstract DomainFlags getDomainFlags(ConvexHull hull);

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result
     * in <code>ptDst</code>. If <code>ptDst</code> is <code>null</code>
     * or do not have the expected number of dimensions, a new
     * {@link CoordinatePoint} object is allocated and then the result
     * of the transformation is stored in this object. In either case,
     * <code>ptDst</code>, which contains the transformed point, is
     * returned for convenience. If <code>ptSrc</code> and
     * <code>ptDst</code> are the same object, the input point is
     * correctly overwritten with the transformed point.
     *
     * @param ptSrc the specified coordinate point to be transformed
     * @param ptDst the specified coordinate point that stores the
     *              result of transforming <code>ptSrc</code>, or
     *              <code>null</code>.
     * @return the coordinate point after transforming <code>ptSrc</code>
     *         and stroring the result in <code>ptDst</code>.
     * @throws TransformException if the point can't be transformed.
     */
    public abstract CoordinatePoint transform(CoordinatePoint ptSrc, CoordinatePoint ptDst) throws TransformException;

    /**
     * Transforms a list of coordinate point ordinal values. 
     * This method is provided for efficiently transforming many points.
     * The supplied array of ordinal values will contain packed ordinal
     * values.  For example, if the source dimension is 3, then the ordinals
     * will be packed in this order:
     *
     * (<var>x<sub>0</sub></var>,<var>y<sub>0</sub></var>,<var>z<sub>0</sub></var>,
     *  <var>x<sub>1</sub></var>,<var>y<sub>1</sub></var>,<var>z<sub>1</sub></var> ...).
     *
     * The size of the passed array must be an integer multiple of
     * {@link #getDimSource DimSource}.
     *
     * @param srcPts the array containing the source point coordinates.
     * @param srcOff the offset to the first point to be transformed
     *               in the source array.
     * @param dstPts the array into which the transformed point
     *               coordinates are returned.
     * @param dstOff the offset to the location of the first
     *               transformed point that is stored in the
     *               destination array.
     * @param numPts the number of point objects to be transformed.
     * @throws TransformException if a point can't be transformed.
     */
    public abstract void transform(double[] srcPtr, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException;

    /**
     * Transforms a list of coordinate point ordinal values. 
     * This method is provided for efficiently transforming many points.
     * The supplied array of ordinal values will contain packed ordinal
     * values.  For example, if the source dimension is 3, then the ordinals
     * will be packed in this order:
     *
     * (<var>x<sub>0</sub></var>,<var>y<sub>0</sub></var>,<var>z<sub>0</sub></var>,
     *  <var>x<sub>1</sub></var>,<var>y<sub>1</sub></var>,<var>z<sub>1</sub></var> ...).
     *
     * The size of the passed array must be an integer multiple of
     * {@link #getDimSource DimSource}.
     *
     * @param srcPts the array containing the source point coordinates.
     * @param srcOff the offset to the first point to be transformed
     *               in the source array.
     * @param dstPts the array into which the transformed point
     *               coordinates are returned.
     * @param dstOff the offset to the location of the first
     *               transformed point that is stored in the
     *               destination array.
     * @param numPts the number of point objects to be transformed.
     * @throws TransformException if a point can't be transformed.
     */
    public abstract void transform(float[] srcPtr, int srcOff, float[] dstPts, int dstOff, int numPts) throws TransformException;

    /**
     * Creates the inverse transform of this object.
     * This method may fail if the transform is not one to one.
     * However, all cartographic projections should succeed.
     *
     * @return The inverse transform.
     * @throws TransformException if the transform can't be inversed.
     */
    public abstract MathTransform inverse() throws TransformException;

    /**
     * Gets the dimension of input points.
     */
    public abstract int getDimSource();

    /**
     * Gets the dimension of output points.
     */
    public abstract int getDimTarget();

    /**
     * Tests whether this transform does not move any points.
     *
     * @return <code>true</code> if this <code>MathTransform</code> is
     *         an identity transform; <code>false</code> otherwise.
     */
    public abstract boolean isIdentity();
}
