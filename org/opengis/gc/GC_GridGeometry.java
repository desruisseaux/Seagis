/*
 * OpenGIS® Grid Coverage Services Implementation Specification
 * Copyright (2001) OpenGIS consortium
 *
 * THIS COPYRIGHT NOTICE IS A TEMPORARY PATCH.   Version 1.00 of official
 * OpenGIS's interface files doesn't contain a copyright notice yet. This
 * file is a slightly modified version of official OpenGIS's interface.
 * Changes have been done in order to fix RMI problems and are documented
 * on the SEAGIS web site (seagis.sourceforge.net). THIS FILE WILL LIKELY
 * BE REPLACED BY NEXT VERSION OF OPENGIS SPECIFICATIONS.
 */
package org.opengis.gc;
import org.opengis.ct.CT_MathTransform;

/** Describes the geometry and georeferencing information of the grid coverage.<br>
 *  The grid range attribute determines the valid grid coordinates and allows for calculation
 *  of grid size. A grid coverage may or may not have georeferencing.
 */
public class GC_GridGeometry {
    /** The valid coordinate range of a grid coverage.<br>
     *  The lowest valid grid coordinate is zero.<br>
     *  A grid with 512 cells can have a minimum coordinate of 0 and maximum of 512,
     *  with 511 as the highest valid index.
     */
    public GC_GridRange gridRange;
    
    /** The CT_MathTransform allows for the transformations from grid coordinates to real
     *  world earth coordinates.<br>
     *  The transform is often an affine transformation.<br>
     *  The coordinate system of the real world coordinates is given by the
     *  coordinateSystem : {@link org.opengis.cs.CS_CoordinateSystem} attribute on the 
     *  {@link org.opengis.cv.CV_Coverage}.<br><br>
     *
     *  Note: If no math transform is given, gridToCoordinateSystem will be <code>null</code>.
     */
    public CT_MathTransform gridToCoordinateSystem;
}
