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
/** Represent the basic implementation which provides access to grid coverage data.<br>
 *  A GC_GridCoverage implementation may provide the ability to update grid values.<br>
 *  A basic read-only implementation would be fairly easy to implement.
 */
public interface GC_GridCoverage extends java.rmi.Remote {
    /** True if grid data can be edited.
     *
     *  @return <code>true</code> if grid data can be edited.
     *  @throws java.rmi.RemoteException
     */
    boolean isDataEditable() throws java.rmi.RemoteException;
    
    /** Information for the packing of grid coverage values.
     *
     *  @return the information for the packing of grid coverage values.
     *  @throws java.rmi.RemoteException
     */
    GC_GridPacking getGridPacking() throws java.rmi.RemoteException;
    
    /** Information for the grid coverage geometry.<br>
     *  Grid geometry includes the valid range of grid coordinates and the georeferencing.
     *
     *  @return the information for the grid coverage geometry.
     *  @throws java.rmi.RemoteException
     */
    GC_GridGeometry getGridGeometry() throws java.rmi.RemoteException;
    
    /** Number of predetermined overviews for the grid.
     *
     *  @return the number of predetermined overviews for the grid.
     *  @throws java.rmi.RemoteException
     */
    int getNumOverviews() throws java.rmi.RemoteException;
    
    /** Optimal size to use for each dimension when accessing grid values.<br>
     *  These values together give the optimal block size to use when retrieving
     *  grid coverage values.<br>
     *  For example, a client application can achieve better performance for a 2-D grid
     *  coverage by reading blocks of 128 by 128 if the grid is  tiled  into blocks of
     *  this size.<br>
     *  The sequence is ordered by dimension.<br><br>
     *
     *  Note: If the implementation does not have optimal sizes the sequence will be empty.
     *
     *  @return the optimal size to use for each dimension when accessing grid values.
     *  @throws java.rmi.RemoteException
     */
    int [] getOptimalDataBlockSizes() throws java.rmi.RemoteException;
    
    /** Return the grid geometry for an overview.
     *
     *  @param overviewIndex Overview index for which to retrieve grid geometry. Indices start at 0.
     *  @return the grid geometry for an overview.
     *  @throws java.rmi.RemoteException
     */
    GC_GridGeometry getOverviewGridGeometry(int overviewIndex) throws java.rmi.RemoteException;
    
    /** Returns a pre-calculated overview for a grid coverage.<br>
     *  The overview indices are numbered from 0 to numberOverviews -1.<br>
     *  The overviews are ordered from highest (index 0)
     *  to lowest (numberOverviews -1) resolution.<br>
     *  Overview grid coverages will have overviews which are the overviews for
     *  the grid coverage with lower resolution than the overview.<br><br>
     *
     *  For example: <br>
     *  A 1 meter grid coverage with 3, 9, and 27 meter overviews will be ordered as follows:<br>
     *       <table border=0 align="center"> 
     *                        <tr> <td align="center">Index</td> <td align="center">resolution</td> </tr>
     *                        <tr> <td align="center">  0  </td> <td align="center">   3  </td> </tr>
     *                        <tr> <td align="center">  1  </td> <td align="center">   9  </td> </tr>
     *                        <tr> <td align="center">  2  </td> <td align="center">   27 </td> </tr>
     *       </table><br><br>
     *
     *  The 3 meter overview will have 2 overviews as follows:
     *      <table border=0 align="center"> 
     *                        <tr> <td align="center">Index</td> <td align="center">resolution</td> </tr>
     *                        <tr> <td align="center">  0  </td> <td align="center">   9  </td> </tr>
     *                        <tr> <td align="center">  1  </td> <td align="center">   27  </td> </tr>
     *      </table>
     *
     *  @param overviewIndex Index of grid coverage overview to retrieve. Indexes start at 0.
     *  @return a pre-calculated overview for a grid coverage.
     *  @throws java.rmi.RemoteException
     */
    GC_GridCoverage getOverview(int overviewIndex) throws java.rmi.RemoteException;
    
    
    /** Return a sequence of boolean values for a block.<br>
     *  A value for each sample dimension will be returned.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @throws java.rmi.RemoteException
     */
    boolean [] getDataBlockAsBoolean(GC_GridRange gridRange) throws java.rmi.RemoteException;
    
    /** Return a sequence of byte values for a block.
     *  A value for each sample dimension will be returned.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @throws java.rmi.RemoteException
     */
    byte [] getDataBlockAsByte(GC_GridRange gridRange) throws java.rmi.RemoteException;
    
    /** Return a sequence of int values for a block.<br>
     *  A value for each sample dimension will be returned.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @throws java.rmi.RemoteException
     */
    int [] getDataBlockAsInteger(GC_GridRange gridRange) throws java.rmi.RemoteException;
    
    /** Return a sequence of double values for a block.<br>
     *  A value for each sample dimension will be returned.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @throws java.rmi.RemoteException
     */
    double [] getValueBlockAsDouble(GC_GridRange gridRange) throws java.rmi.RemoteException;

    /** Return a block of grid coverage data for all sample dimensions.<br>
     *  A value for each sample dimension will be returned.<br>
     *  This operation provides efficient access of the grid values.<br>
     *  The sequencing order of the values in the sequence will follow the rules
     *  given by valueInBytePacking and bandPacking defined in GC_GridPacking.<br><br>
     *
     *  The requested grid range must satisfy the following rules for each dimension
     *  of the grid coverage:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp; Min grid coordinate <= grid range minimum <= grid range maximum <= maximum grid coordinate<br><br>
     *
     *  The sequence of bytes returned will match the data type of the dimension.<br>
     *  For example, a grid with one 16 bit unsigned (CV_16BIT_U) sample dimension will
     *  return 2 bytes for every cell in the block.<br><br>
     *
     *  Byte padding Rules for grid values of less than 8 bits.<br>
     *  For 2 D grid coverages, padding is to the nearest byte for the following cases<br>
     *  <table border=0>
     *  <tr> <td>For PixelInterleaved</td>
     *       <td>For grids with multiple sample dimensions, padding occurs between<br>
     *           pixels for each change in dimension type.</td>
     *  </tr>
     *  <tr> <td>For LineInterleaved</td>
     *       <td>Padding occurs at the end of each row or column (depending on the
     *           valueSequence of the grid).</td>
     *  </tr>
     *  <tr> <td>For BandSequencial</td>
     *       <td>Padding occurs at the end of every sample dimension.</td>
     *  </tr>
     *  </table>
     *  For grid values smaller than 8 bits, their order within each byte is given by the
     *  valueInBytePacking defined in {@link GC_GridPacking}.
     *  For grid values bigger than 8 bits, the order of their bytes is given by the
     *  byteInValuePacking defined in GC_GridPacking.
     *  
     *  @param gridRange Grid range for block of data to be accessed.
     *  @return a block of grid coverage data for all sample dimensions.
     *  @throws java.rmi.RemoteException
     */
    byte [] getPackedDataBlock(GC_GridRange gridRange) throws java.rmi.RemoteException;
    
    /** Set a block of boolean values for all sample dimensions.<br>
     *  The requested grid range must satisfy the following rules for each
     *  dimension of the grid coverage:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp; Min grid coordinate <= grid range minimum <= grid range
     *  maximum <= maximum grid coordinate<br><br>
     *
     *  The number of values must equal:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp; (Max1   Min1 + 1) * (Max2   Min2 + 1)... * (Maxn   Minn + 1) * numberSampleDimensions<br><br>
     *
     *  Where<UL><li> Min is the mimium ordinate in the grid range
     *           <li> Max is the maximum ordinate in the grid range
     *           <li> N is the number of dimensions in the grid coverage
     *       </UL>
     *  @param gridRange Grid range for block of data to be accessed.
     *  @param values Sequence of grid values for the given region.
     *  @throws java.rmi.RemoteException
     */
    void setDataBlockAsBoolean(GC_GridRange gridRange, boolean [] values) throws java.rmi.RemoteException;
    
    /** Set a block of byte values for all sample dimensions.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @param values Sequence of grid values for the given region.
     *  @throws java.rmi.RemoteException
     */
    void setDataBlockAsByte(GC_GridRange gridRange, byte [] values) throws java.rmi.RemoteException;
    
    /** Set a block of bint values for all sample dimensions.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @param values Sequence of grid values for the given region.
     *  @throws java.rmi.RemoteException
     */
    void setDataBlockAsInteger(GC_GridRange gridRange, int [] values) throws java.rmi.RemoteException;
    
    /** Set a block of double values for all sample dimensions.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @param values Sequence of grid values for the given region.
     *  @throws java.rmi.RemoteException
     */
    void setDataBlockAsDouble(GC_GridRange gridRange, double [] values) throws java.rmi.RemoteException;
    
    /** Set a block of grid coverage data for all sample dimensions.<br>
     *  See GetDataBlock for details on how to pack the values.<br><br>
     *
     *  The requested grid range must satisfy the following rules for each dimension
     *  of the grid coverage:<br>
     *  &nbsp;&nbsp;&nbsp;&nbsp; Min grid coordinate <= grid range minimum <= grid
     *  range maximum <= maximum grid coordinate<br><br>
     *
     *  For byte padding rules see getDataBlock.
     *
     *  @param gridRange Grid range for block of data to be accessed.
     *  @param values Sequence of grid values for the given region.
     *  @throws java.rmi.RemoteException
     */
    void setPackedDataBlock(GC_GridRange gridRange, byte [] values) throws java.rmi.RemoteException;
}
