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

/** Describes the color entry in a color table.
 */
public class CV_PaletteInterpretation {
    public int value;
    
    /** Gray Scale color palette 
     */
    public static final int CV_Gray = 0;
    
    /** RGB (Red Green Blue) color palette
     */
    public static final int CV_RGB = 1;
    
    /** CYMK (Cyan Yellow Magenta blacK) color palette
     */
    public static final int CV_CMYK = 2;
    
    /** HSL (Hue Saturation Lightness) color palette
     */
    public static final int CV_HLS = 3;
}
