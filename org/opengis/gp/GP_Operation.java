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
package org.opengis.gp;
import org.opengis.gc.GC_ParameterInfo;

/** This interface provides descriptive information for a grid coverage processing operation.<br>
 *  The descriptive information includes such information as the name of the operation,
 *  operation description, number of source grid coverages required for the operation etc.
 */
public interface GP_Operation extends java.rmi.Remote {
    /** Name of the processing operation.
     *
     *  @return the name of the processing operation.
     *  @throws java.rmi.RemoteException
     */
    String getName() throws java.rmi.RemoteException;
    
    /** Description of the processing operation.<br>
     *  If no description, the value will be an empty string.
     *
     *  @return the description of the processing operation.
     *  @throws java.rmi.RemoteException
     */
    String getDescription() throws java.rmi.RemoteException;

    /** Implementation vendor name.
     *
     *  @return the implementation vendor name.
     *  @throws java.rmi.RemoteException
     */
    String getVendor() throws java.rmi.RemoteException;
    
    /** URL for documentation on the processing operation.<br>
     *  If no online documentation is available the string will be empty.
     *
     *  @return the URL for documentation on the processing operation.
     *  @throws java.rmi.RemoteException
     */
    String getDocURL() throws java.rmi.RemoteException;
    
    /** Version number for the implementation.
     *
     *  @return the version number for the implementation.
     *  @throws java.rmi.RemoteException
     */
    String getVersion() throws java.rmi.RemoteException;
    
    /** Number of source grid coverages required for the operation.
     *
     *  @return the number of source grid coverages required for the operation.
     *  @throws java.rmi.RemoteException
     */
    int getNumSources() throws java.rmi.RemoteException;
    
    /** Number of parameters for the operation.
     *
     *  @return the number of parameters for the operation.
     *  @throws java.rmi.RemoteException
     */
    int getNumParameters() throws java.rmi.RemoteException;
    
    /** Retrieve the parameter information for a given index.
     *
     *  @param index Parameter information index to retrieve. Index starts at 0.
     *  @return the parameter information for a given index.
     *  @throws java.rmi.RemoteException
     */
    GC_ParameterInfo getParameterInfo(int index) throws java.rmi.RemoteException;
}
