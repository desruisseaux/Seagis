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

/** Provides information for the parameters required for grid coverage processing
 *  operations and grid exchange.<br>
 *  This information includes such information as the name of the parameter,
 *  parameter description, parameter type etc.
 */
public class GC_ParameterInfo {
    /** Parameter name.
     */
    public String name;
    
    /** Parameter description.<br>
     *  If no description, the value will be an empty string.
     */
    public String description;
    
    /** Parameter type.<br>
     *  The enumeration contains standard parameter types for integer, string, 
     *  floating-point numbers, objects, etc.
     */
    public GC_ParameterType type;
    
    /** Default value for parameter.<br>
     *  The type Object can be any type including a Number or a CharacterString.
     *  For example, a filtering operation could have a default kernel size of 3.<br><br>
     *
     *  Note: If there is no default value, defaultValue will be empty.
     */
    public Object defaultValue;
    
    /** Minimum parameter value.<br>
     *  For example, a filtering operation could have a minimum kernel size of 3.
     */
    public double minimumValue;
    
    /** Maximum parameter value.<br>
     *  For example, a filtering operation could have a maximum kernel size of 9.
     */
    public double maximumValue;
}
