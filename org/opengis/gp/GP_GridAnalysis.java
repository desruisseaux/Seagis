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
import org.opengis.pt.PT_Matrix;

/** Performs various analysis operations on a grid coverage.
 */
public interface GP_GridAnalysis extends java.rmi.Remote {
    /** Determine the histogram of the grid values for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension to be histogrammed.
     *  @param miniumEntryValue Minimum value stored in the first histogram entry.
     *  @param maximumEntryValue Maximum value stored in the last histogram entry.
     *  @param numberEntries Number of entries in the histogram.
     *  @return the histogram of the grid values for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    int [] histogram(int sampleDimension, double minimumEntryValue, double maximumEntryValue, int numberEntries) throws java.rmi.RemoteException;

    /** Determine the minimum grid value for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension.
     *  @return the minimum grid value for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double minValue(int sampleDimension) throws java.rmi.RemoteException;
    
    /** Determine the maximum grid value for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension.
     *  @return the maximum grid value for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double maxValue(int sampleDimension) throws java.rmi.RemoteException;
    
    /** Determine the mean grid value for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension.
     *  @return the mean grid value for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double meanValue(int sampleDimension) throws java.rmi.RemoteException;
    
    /** Determine the median grid value for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension.
     *  @return the median grid value for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double medianValue(int sampleDimension) throws java.rmi.RemoteException;
    
    /** Determine the mode grid value for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension.
     *  @return the mode grid value for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double modeValue(int sampleDimension) throws java.rmi.RemoteException;
    
    /** Determine the standard deviation from the mean of the grid values for a sample dimension.
     *
     *  @param sampleDimension Index of sample dimension.
     *  @return he standard deviation from the mean of the grid values for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double stdDev(int sampleDimension) throws java.rmi.RemoteException;
    
    /** Determine the correlation between sample dimensions in the grid.
     *
     *  @return the correlation between sample dimensions in the grid.
     *  @throws java.rmi.RemoteException
     */
    PT_Matrix correlation() throws java.rmi.RemoteException;
}
