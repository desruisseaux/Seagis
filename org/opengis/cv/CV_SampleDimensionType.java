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
package org.opengis.cv;

/** Specifies the various dimension types for coverage values.
 *  For grid coverages, these correspond to band types.
 */
public class CV_SampleDimensionType {
    public int value;
    
    public static final int CV_1BIT = 0;
    public static final int CV_2BIT = 1;
    public static final int CV_4BIT = 2;
    public static final int CV_8BIT_U = 3;
    public static final int CV_8BIT_S = 4;
    public static final int CV_16BIT_U = 5;
    public static final int CV_16BIT_S = 6;
    public static final int CV_32BIT_U = 7;
    public static final int CV_32BIT_S = 8;
    public static final int CV_32BIT_REAL = 9;
    public static final int CV_64BIT_REAL = 10;
}
