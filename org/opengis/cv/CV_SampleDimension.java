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
package org.opengis.cv;
import org.opengis.cs.CS_Unit;

/** Contains information for an individual sample dimension of coverage.<br>
 *  This interface is applicable to any coverage type.<br>
 *  For grid coverages, the sample dimension refers to an individual band.
 */
public interface CV_SampleDimension extends java.rmi.Remote {
    /** Sample dimension title or description.<br>
     *  This String may be empty if no description is present.
     *
     *  @return the sample dimension title or description
     *  @throws java.rmi.RemoteException
     */
    String getDescription() throws java.rmi.RemoteException;
    
    /** A code value indicating grid value data type.<br>
     *  This will also indicate the number of bits for the data type.
     *
     *  @return a code value indicating grid value data type.
     *  @throws java.rmi.RemoteException
     */
    CV_SampleDimensionType getSampleDimensionType() throws java.rmi.RemoteException;
    
    /** Sequence of category names for the values contained in a sample dimension.<br>
     *  This allows for names to be assigned to numerical values.<br>
     *  The first entry in the sequence relates to a cell value of zero.<br>
     *  For grid coverages, category names are only valid for a classified grid data.<br><br>
     *
     *  For example:<br>
     *  <UL><li>0 Background
     *      <li>1 Water
     *      <li>2 Forest
     *      <li>3 Urban
     *  </UL>
     *  Note: If no category names exist, an empty sequence is returned.
     *  @return the sequence of category names for the values contained in a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    String [] getCategoryNames() throws java.rmi.RemoteException;
    
    /** Color interpretation of the sample dimension.<br>
     *  A sample dimension can be an index into a color palette or be a color model component.<br>
     *  If the sample dimension is not assigned a color interpretation the value is CV_Undefined.
     *
     *  @return the color interpretation of the sample dimension.
     *  @throws java.rmi.RemoteException
     */
    CV_ColorInterpretation getColorInterpretation() throws java.rmi.RemoteException;
    
    /** Indicates the type of color palette entry for sample dimensions which have a palette.<br>
     *  If a sample dimension has a palette, the color interpretation must be CV_GrayIndex
     *  or CV_PaletteIndex.<br>
     *  A palette entry type can be Gray, RGB, CMYK or HLS.
     *
     *  @return the type of color palette entry for sample dimensions which have a palette.
     *  @throws java.rmi.RemoteException
     */
    CV_PaletteInterpretation getPaletteInterpretation() throws java.rmi.RemoteException;
    
    /** Color palette associated with the sample dimension.<br>
     *  A color palette can have any number of colors.<br>
     *  See palette interpretation for meaning of the palette entries.<br><br>
     *
     *  Note: If the grid coverage has no color palette, an empty sequence will be returned.
     *
     *  @return the color palette associated with the sample dimension.
     *  @throws java.rmi.RemoteException
     */
    int [][] getPalette() throws java.rmi.RemoteException;
    
    /** Values to indicate no data values for the sample dimension.<br>
     *  For low precision sample dimensions, this will often be no data values .
     *
     *  @return the values to indicate no data values for the sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double [] getNoDataValue() throws java.rmi.RemoteException;
    
    /** The minimum value occurring in the sample dimension.<br>
     *  If this value is not available, this value can be determined from the
     *  GP_GridAnalysis getMinValue operation.<br><br>
     *
     *  Note: This value can be empty if this value is not provided by the implementation.
     *
     *  @return the minimum value occurring in the sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double getMinimumValue() throws java.rmi.RemoteException;
    
    /** The maximum value occurring in the sample dimension.<br>
     *  If this value is not available, this value can be determined from the
     *  GP_GridAnalysis getMaxValue operation.<br><br>
     *
     *  Note: This value can be empty if this value is not provided by the implementation.
     *
     *  @return the maximum value occurring in the sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double getMaximumValue() throws java.rmi.RemoteException;
    
    /** The unit information for this sample dimension.<br>
     *  This interface typically is provided with grid coverages which represent
     *  digital elevation data.<br><br>
     *
     *  Note: This value will be <code>null</code> if no unit information is available.
     *
     *  @return the unit information for this sample dimension.
     *  @throws java.rmi.RemoteException
     */
    CS_Unit getUnits() throws java.rmi.RemoteException;
    
    /** Offset is the value to add to grid values for this sample dimension.<br>
     *  This attribute is typically used when the sample dimension represents
     *  elevation data.<br>
     *  The default for this value is 0.
     *
     *  @return the offset is the value to add to grid values for this sample dimension.
     *  @throws java.rmi.RemoteException
     */
    double getOffset() throws java.rmi.RemoteException;
    
    /** Scale is the value which is multiplied to grid values for this sample dimension.<br>
     *  This attribute is typically used when the sample dimension represents
     *  elevation data.<br>
     *  The default for this value is 1.
     *
     *  @return the scale.
     *  @throws java.rmi.RemoteException
     */
    double getScale() throws java.rmi.RemoteException;
    
    /** The list of metadata keywords for a sample dimension.<br>
     *  If no metadata is available, the sequence will be empty.
     *
     *  @return the list of metadata keywords for a sample dimension.
     *  @throws java.rmi.RemoteException
     */
    String [] getMetaDataNames() throws java.rmi.RemoteException;
    
    /** Retrieve the metadata value for a given metadata name.
     *
     *  @param name Metadata keyword for which to retrieve metadata.
     *  @return the metadata value for a given metadata name.
     *  @throws java.rmi.RemoteException
     */
    String getMetadataValue(String name) throws java.rmi.RemoteException;
}
