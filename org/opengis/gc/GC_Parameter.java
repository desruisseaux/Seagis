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

/** The parameter codelist required for a grid coverage processing operation.<br>
 *  This structure contains the parameter name (as defined from the {@link GC_ParameterInfo}
 *  structure) and it s value.
 */
public class GC_Parameter {
    /** Parameter name.
     */
    public String name;
    
    /** The value for parameter.<br>
     *  The type Object can be any type including a Number, a CharacterString
     *  or an instance of an interface.<br>
     *  For example, a grid processor operation will typically require a parameter
     *  for the input grid coverage.<br>
     *  This parameter may have Source as the parameter name and the instance of
     *  the grid coverage as the value.
     */
    public Object value;
}
