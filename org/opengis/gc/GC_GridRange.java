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

/** Specifies the range of valid coordinates for each dimension of the coverage.
 */
public class GC_GridRange {
    /** The valid minimum inclusive grid coordinate.<br>
     *  The sequence contains a minimum value for each dimension of the grid coverage.<br>
     *  The lowest valid grid coordinate is zero.
     */
    public int [] lo;
    
    /** The valid maximum exclusive grid coordinate.<br>
     *  The sequence contains a maximum value for each dimension of the grid coverage.
     */
    public int [] hi;
}
