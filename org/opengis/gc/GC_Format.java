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

/** This interface is a discovery mechanism to determine the formats supported by a
 *  {@link GC_GridCoverageExchange} implementation.
 *  A GC_GridCoverageExchange implementation can support a number of file format or resources.
 */
public interface GC_Format extends java.rmi.Remote {
    /** Name of the file format.<br>
     *  This name is used as the name of the file in the exportTo operation.
     *
     *  @return the name of the file format.
     *  @throws java.rmi.RemoteException
     */
    String getName() throws java.rmi.RemoteException;
    
    /** Description of the file format.<br>
     *  If no description, the value will be an empty string.
     *
     *  @return the description of the file format.
     *  @throws java.rmi.RemoteException
     */
    String getDescription() throws java.rmi.RemoteException;
    
    /** Vendor or agency for the format.
     *
     *  @return the vendor or agency for the format.
     *  @throws java.rmi.RemoteException
     */
    String getVendor() throws java.rmi.RemoteException;
    
    /** Documentation URL for the format.
     *
     *  @return the documentation URL for the format.
     *  @throws java.rmi.RemoteException
     */
    String getDocURL() throws java.rmi.RemoteException;
    
    /** Version number of the format.
     *
     *  @return the version number of the format.
     *  @throws java.rmi.RemoteException
     */
    String getVersion() throws java.rmi.RemoteException;
    
    /** Number of optional parameters for the exportTo operation.
     *
     *  @return the number of optional parameters for the exportTo operation.
     *  @throws java.rmi.RemoteException
     */
    int getNumParameters() throws java.rmi.RemoteException;
    
    /** Retrieve the parameter information for a given index.
     *
     *  @param index Index to the parameter.
     *  @throws java.rmi.RemoteException
     *  @return the parameter information for the given index.
     */
    GC_ParameterInfo getParameterInfo(int index) throws java.rmi.RemoteException;
}
