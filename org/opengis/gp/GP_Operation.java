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
package org.opengis.gp;

// Remote Method Invocation
import java.rmi.Remote;
import java.rmi.RemoteException;

// GCS dependencies
import org.opengis.gc.GC_ParameterInfo;


/**
 * This interface provides descriptive information for a grid coverage processing
 * operation. The descriptive information includes such information as the name of
 * the operation, operation description, number of source grid coverages required
 * for the operation etc.
 *
 * @version 1.00
 * @since   1.00
 */
public interface GP_Operation extends Remote
{
    /**
     * Name of the processing operation.
     *
     * @return the name of the processing operation.
     * @throws RemoteException if a remote method call failed.
     */
    String getName() throws RemoteException;

    /**
     * Description of the processing operation.
     * If no description, the value will be an null or empty string.
     *
     * @return the description of the processing operation.
     * @throws RemoteException if a remote method call failed.
     */
    String getDescription() throws RemoteException;

    /**
     * Implementation vendor name.
     *
     * @return the implementation vendor name.
     * @throws RemoteException if a remote method call failed.
     */
    String getVendor() throws RemoteException;

    /**
     * URL for documentation on the processing operation.
     * If no online documentation is available the string will be empty.
     *
     * @return the URL for documentation on the processing operation.
     * @throws RemoteException if a remote method call failed.
     */
    String getDocURL() throws RemoteException;

    /**
     * Version number for the implementation.
     *
     * @return the version number for the implementation.
     * @throws RemoteException if a remote method call failed.
     */
    String getVersion() throws RemoteException;

    /**
     * Number of source grid coverages required for the operation.
     *
     * @return the number of source grid coverages required for the operation.
     * @throws RemoteException if a remote method call failed.
     */
    int getNumSources() throws RemoteException;

    /**
     * Number of parameters for the operation.
     *
     * @return the number of parameters for the operation.
     * @throws RemoteException if a remote method call failed.
     */
    int getNumParameters() throws RemoteException;

    /**
     * Retrieve the parameter information for a given index.
     *
     * @param index Parameter information index to retrieve. Index starts at 0.
     * @return the parameter information for a given index.
     * @throws RemoteException if a remote method call failed.
     */
    GC_ParameterInfo getParameterInfo(int index) throws RemoteException;
}