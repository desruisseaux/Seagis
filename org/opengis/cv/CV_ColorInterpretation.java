/*
 * OpenGIS� Grid Coverage Services Implementation Specification
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

/** Specifies the mapping of a band to a color model component.
 */
public class CV_ColorInterpretation {
    public int value;
    
    /** Band is not associated with a color model component.
     */
    public static final int CV_Undefined = 0 ;
    
    /** Band is an index into a lookup table.
     */
    public static final int CV_GrayIndex = 1 ;
    
    /** Band is a color index into a color table.
     */
    public static final int CV_PaletteIndex = 2 ;
    
    /** Red Band for the RGB color model components.
     */
    public static final int CV_RedBand = 3 ;
    
    /** Greend Band for the RGB color model components.
     */
    public static final int CV_GreenBand = 4 ;
    
    /** Blue Band for the RGB color model components.
     */
    public static final int CV_BlueBand = 5 ;
    
    /** Alpha Band for the RGB color model components.
     *  AlphaBand may or may not be present.
     */
    public static final int CV_AlphaBand = 6 ;
    
    /** Hue Band for the HSL color model.
     */
    public static final int CV_HueBand = 7 ;
    
    /** Saturation Band for the HSL color model.
     */
    public static final int CV_SaturationBand = 8 ;
    
    /** Lightness Band for the HSL color model.
     */
    public static final int CV_LightnessBand = 9 ;
    
    /** Cyan Band for the CMYK color model.
     */
    public static final int CV_CyanBand = 10 ;
    
    /** Magenta Band for the CMYK color model.
     */
    public static final int CV_MagentaBand = 11 ;
    
    /** Yellow Band for the CMYK color model.
     */
    public static final int CV_YellowBand = 12 ;
    
    /** Black Band for the CMYK color model.
     */
    public static final int CV_BlackBand = 13 ;
    
}
