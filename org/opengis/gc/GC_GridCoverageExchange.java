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
import org.opengis.cs.CS_CoordinateSystem;
import org.opengis.ct.CT_MathTransform;

/** Support for creation of grid coverages from persistent formats as well as exporting
 *  a grid coverage to a persistent formats.<br>
 *  For example, it allows for creation of grid coverages from the GeoTIFF Well-known
 *  Binary format and exporting to the GeoTIFF file format.<br>
 *  Basic implementations only require creation of grid coverages from a file format or resource.
 */
public interface GC_GridCoverageExchange extends java.rmi.Remote {
    /** The number of formats supported by the GC_GridCoverageExchange.
     *
     *  @return the number of formats supported by the GC_GridCoverageExchange.
     *  @throws java.rmi.RemoteException
     */
    int getNumFormats() throws java.rmi.RemoteException;
    
    /** List of metadata keywords for the interface.<br>
     *  If no metadata is available, the sequnce will be empty.
     *
     *  @return the list of metadata keywords for the interface.
     *  @throws java.rmi.RemoteException
     */
    String [] getMetadataNames() throws java.rmi.RemoteException;
    
    /** Retrieve information on file formats or resources available with the
     *  GC_GridCoverageExchange implementation.<br>
     *  Indices start at zero.
     *
     *  @param index Index for which to retrieve the format information.
     *  @return information on file formats or resources available with the 
     *  GC_GridCoverageExchange implementation.
     *  @throws java.rmi.RemoteException
     */
    GC_Format getFormat(int index) throws java.rmi.RemoteException;
    
    /** Retrieve the metadata value for a given metadata name.
     *
     *  @param name Metadata keyword for which to retrieve metadata.
     *  @return the metadata value for the given metadata name.
     *  @throws java.rmi.RemoteException
     */
    String getMetadataValue(String name) throws java.rmi.RemoteException;
    
    /** Create a new GC_GridCoverage from a grid coverage file.<br>
     *  This method is meant to allow implementations to create a GC_GridCoverage
     *  from any file format.<br>
     *  An implementation can support nay number of formats which is determined
     *  from the GC_Format interface.<br><br>
     *
     *  @param name File name (including path) from which to create a grid coverage interface.<br>
     *  This file name can be any valid file name within the underlying operating system
     *  of the server or a valid string, such as a URL which specifics a grid coverage.<br>
     *  Each implementation must determine if file name is valid for it s own use.
     *  @return a new GC_GridCoverage.
     *  @throws java.rmi.RemoteException
     */
    GC_GridCoverage createFromName(String name) throws java.rmi.RemoteException;
    
    /** Retrieve the list of grid coverages contained within the given file or resource.<br>
     *  Each grid can have a different coordinate system, number of dimensions and grid geometry.<br>
     *  For example, a HDF-EOS file (GRID.HDF) contains 6 grid coverages each having a different
     *  projection.<br><br>
     *
     *  GRID.HDF:____ UTM <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|___ Geo <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|___ Polar <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|___ IGoode <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|___ SOM <br>
     *  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|___ Lamaz <br><br>
     *
     *  Note: An empty sequence will be returned if no sub names exist.
     *
     *  @param name File name (including path) from which to retrieve the grid coverage names.
     *  This file name can be any valid file name within the underlying operating system
     *  of the server or a valid string, such as a URL which specifics a grid coverage.
     *  Each implementation must determine if file name is valid for it s own use.
     *  Implementations can support many different of file formats.
     *  @return he list of grid coverages contained within the given file or resource.
     *  @throws java.rmi.RemoteException
     */
    String [] listSubNames(String name) throws java.rmi.RemoteException;
    
    /** Create a new GC_GridCoverage from a file where the file contains many grid coverages.<br>
     *  This method is meant to allow implementations to create a GC_GridCoverage from
     *  any file format which contains many grid coverages.<br>
     *  An example of such a format is HDF-EOS format.
     *
     *  @param name File name (including path) from which to create a grid coverage interface.
     *  This file name can be any valid file name within the underlying operating system of
     *  the server or a valid string, such as a URL which specifics a grid coverage.
     *  Each implementation must determine if name is valid for it s own use.
     *  @param subName Name of grid coverage contained in file name or resource.
     *  @return a new GC_GridCoverage from a file where the file contains many grid coverages.
     *  @throws java.rmi.RemoteException
     */
    GC_GridCoverage createFromSubName(String name, String subName) throws java.rmi.RemoteException;
    
    /** Export a grid coverage to a persistent file format.
     *  
     *  @param gridCoverage Source grid coverage.
     *  @param fileFormat String which indicates exported file format.
     *  The file format types are implementation specific.
     *  The file format name is determined from the GC_Format interface.
     *      Sample file formats include:  
     *         "GeoTIFF"   - GeoTIFF
     *         "PIX"       - PCI Geomatics PIX
     *         "HDF-EOS"   - NASA HDF-EOS
     *         "NITF"      - National Image Transfer Format
     *         "STDS-DEM"  - Standard Transfer Data Standard
     *  Other file format names are implementation dependent.
     *  @param fileName File name to store grid coverage.
     *  This file name can be any valid file name within the underlying operating system of
     *  the server.
     *  @param createOptions Options to use for creating the file.
     *  These options are implementation specific are the valid options is determined
     *  from the GC_Format interface.
     *  @throws java.rmi.RemoteException
     */
    void exportTo(GC_GridCoverage gridCoverage, String fileFormat, String fileName, GC_Parameter [] creationOptions) throws java.rmi.RemoteException;
    
    /** Create a new coverage with a different coordinate reference system.
     *
     *  @param gridCoverage Source grid coverage.
     *  @param coordsys Coordinate system of the new grid coverage.
     *  @param gridToCoordinateSystem Math transform to assign to grid coverage.
     *  @return a new coverage with a different coordinate reference system.
     *  @throws java.rmi.RemoteException
     */
    GC_GridCoverage move (GC_GridCoverage gridCoverage, CS_CoordinateSystem coordsys, CT_MathTransform gridToCoordinateSystem) throws java.rmi.RemoteException;
}
