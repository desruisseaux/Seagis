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

// Input/output
import java.io.Serializable;


/**
 * Specifies the various dimension types for coverage values.
 * For grid coverages, these correspond to band types.
 *
 * @version 1.00
 * @since   1.00
 */
public class CV_SampleDimensionType implements Serializable
{
    /**
     * Use <code>serialVersionUID</code> from first
     * draft for interoperability with GCS 1.00.
     */
    private static final long serialVersionUID = 9136873943539839966L;

    /**
     * The enum value.
     */
    public int value;

    /** 1 bits integers.                         */ public static final int CV_1BIT       =  0;
    /** 2 bits integers.                         */ public static final int CV_2BIT       =  1;
    /** 4 bits integers.                         */ public static final int CV_4BIT       =  2;
    /** Unsigned 8 bits integers.                */ public static final int CV_8BIT_U     =  3;
    /** Signed 8 bits integers.                  */ public static final int CV_8BIT_S     =  4;
    /** Unsigned 16 bits integers.               */ public static final int CV_16BIT_U    =  5;
    /** Signed 16 bits integers.                 */ public static final int CV_16BIT_S    =  6;
    /** Unsigned 32 bits integers.               */ public static final int CV_32BIT_U    =  7;
    /** Signed 32 bits integers.                 */ public static final int CV_32BIT_S    =  8;
    /** Simple precision floating point numbers. */ public static final int CV_32BIT_REAL =  9;
    /** Double precision floating point numbers. */ public static final int CV_64BIT_REAL =  10;

    /**
     * Construct a new enum value.
     */
    public CV_SampleDimensionType(final int value)
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
            return ((CV_SampleDimensionType) object).value == value;
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
        final StringBuffer buffer=new StringBuffer("CV_SampleDimensionType");
        buffer.append('[');
        buffer.append(value);
        buffer.append(']');
        return buffer.toString();
    }
}