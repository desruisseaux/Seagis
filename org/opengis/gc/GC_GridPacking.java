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

/** Describes the packing of data values within grid coverages.<br>
 *  It includes the packing scheme of data values with less then 8 bits per value
 *  within a byte, byte packing (Little Endian / Big Endian) for values with more
 *  than 8 bits and the packing of the values within the dimensions.
 */
public class GC_GridPacking {
    /** Order of bytes packed in values for sample dimensions with greater than 8 bits.
     */
    public GC_ByteInValuePacking byteInValuePacking;
    
    /** Order of values packed in a byte for CV_1BIT, CV_2BIT and CV_4BIT data types.
     */
    public GC_ValueInBytePacking valueInBytePacking;
    
    /** Gives the ordinate index for the band.<br>
     *  This index indicates how to form a band-specific coordinate from a grid coordinate
     *  and a sample dimension number.<br>
     *  This indicates the order in which the grid values are stored in streamed data.<br>
     *  This packing order is used when grid values are retrieved using the
     *  PackedDataBlock or set using SetPackedDataBlock operations on GC_GridCoverage.<br><br>
     *
     *  bandPacking of 
     *    <UL>
     *      <li>0 : the full band-specific coordinate is (b, n1, n2...)
     *      <li>1 : the full band-specific coordinate is (n1, b, n2...)
     *      <li>2 : the full band-specific coordinate is (n1, n2, b...)
     *    </UL>
     *  Where
     *  <UL>
     *     <li>b is band
     *     <li>n1 is dimension 1
     *     <li>n2 is dimension 2
     *  </UL>
     *  For 2 dimensional grids, band packing of 0 is referred to as band sequential,
     *  1 line interleaved and 2 pixel interleaved.
     */
    public int bandPacking;  
}
