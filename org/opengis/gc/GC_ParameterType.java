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
package org.opengis.gc;

// Input/output
import java.io.Serializable;


/**
 * Specifies the type of a parameter value.
 * A sequence of parameters is used to pass a variable number of arguments to
 * an operation and each of these parameters can have a different parameter type.
 * An operation requiring a string and grid coverage would use two parameters for
 * type {@link #GC_StringType} and {@link #GC_GridCoverageType}.
 *
 * @version 1.00
 * @since   1.00
 */
public class GC_ParameterType implements Serializable
{
    /**
     * Use <code>serialVersionUID</code> from first
     * draft for interoperability with GCS 1.00.
     */
    private static final long serialVersionUID = 5157019842440644024L;

    /**
     * The enum value.
     */
    public int value;

    /**
     * Integer parameter.
     */
    public static final int GC_IntegerType = 0;

    /**
     * Floating point parameter.
     */
    public static final int GC_FloatingPointType = 1;

    /**
     * String parameter.
     */
    public static final int GC_StringType = 2;

    /**
     * Sequence of numbers.
     */
    public static final int GC_VectorType = 3;

    /**
     * Grid coverage instance.
     */
    public static final int GC_GridCoverageType = 4;

    /**
     * Math transform instance.
     */
    public static final int CT_MathTransformType = 5;

    /**
     * Coordinate system instance.
     */
    public static final int CS_CoordinateSystemType = 6;

    /**
     * Grid geometry instance.
     */
    public static final int GC_GridGeometryType = 7;

    /**
     * Byte in value packing enumeration {@link GC_ByteInValuePacking}.
     */
    public static final int GC_ByteInValuePackingType = 8;

    /**
     * Value in byte packing enumeration {@link GC_ValueInBytePacking}.
     */
    public static final int GC_ValueInBytePackingType = 9;

    /**
     * Construct a new enum value.
     */
    public GC_ParameterType(final int value)
    {this.value = value;}

    /**
     * Returns the enum value.
     */
    public int hashCode()
    {return value;}

    /**
     * Compares the specified object with
     * this enum for equality.
     */
    public boolean equals(final Object object)
    {
        if (object!=null && getClass().equals(object.getClass()))
        {
            return ((GC_ParameterType) object).value == value;
        }
        else return false;
    }

    /**
     * Returns a string repr�sentation of this enum.
     * The returned string is implementation dependent.
     * It is usually provided for debugging purposes only.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer("GC_ParameterType");
        buffer.append('[');
        buffer.append(value);
        buffer.append(']');
        return buffer.toString();
    }
}