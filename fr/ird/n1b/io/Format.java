/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D?veloppement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la t?l?d?tection
 *          Institut de Recherche pour le d?veloppement
 *          500 rue Jean-Fran?ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.io;

// J2SE
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

/**
 * Description des champs compris dans diff?rentes parties d'un fichier N1B, pour des donnees issues
 * du capteur AVHRR.
 *
 * @version $Id$
 * @author Remi Eve
 * @author Martin Desruisseaux
 */
public final class Format
{
    /** Identifiant des formats de donnees disponibles. */
    public static final int FORMAT_AJ  = 0,
                            FORMAT_KLM = 1;
    
    /** Identifiant des champs les plus utilises dans les differents formats. */
     public final static String TBM_SPACECRAFT_ID           = "Spacecraft Unique ID",
                                TBM_CHANNEL_SELECTED        = "Channels selected",                                
                                HEADER_NUMBER_OF_SCANS      = "Number of scans",
                                HEADER_START_TIME           = "Start-Time",
                                HEADER_STOP_TIME            = "Stop-Time",                                     
                                HEADER_AJ_CARTESIAN_ELEMENTS= "Cartesian elements",
                                DATA_SCAN_LINE_NUMBER       = "Scan line number",                                
                                DATA_TIME                   = "Time",
                                DATA_QUALITY_INDICATOR      ="Quality indicators",
                                DATA_AJ_CALIBRATION_COEF_CHANNEL_1 = "Calibration coefficients channel 1",
                                DATA_AJ_CALIBRATION_COEF_CHANNEL_2 = "Calibration coefficients channel 2",
                                DATA_AJ_CALIBRATION_COEF_CHANNEL_3 = "Calibration coefficients channel 3",
                                DATA_AJ_CALIBRATION_COEF_CHANNEL_4 = "Calibration coefficients channel 4",
                                DATA_AJ_CALIBRATION_COEF_CHANNEL_5 = "Calibration coefficients channel 5",
                                DATA_EARTH_LOCATION         = "Earth location",
                                DATA_PACKED_VIDEO_DATA      = "Packed video data",                                
                                HEADER_KLM_WAVE_CHANNEL_3B      = "Channel 3b central wave number",
                                HEADER_KLM_CONSTANT1_CHANNEL_3B = "Channel 3b constant 1",
                                HEADER_KLM_CONSTANT2_CHANNEL_3B = "Channel 3b constant 2",
                                HEADER_KLM_WAVE_CHANNEL_4       = "Channel 4 central wave number",
                                HEADER_KLM_CONSTANT1_CHANNEL_4  = "Channel 4 constant 1",
                                HEADER_KLM_CONSTANT2_CHANNEL_4  = "Channel 4 constant 2",
                                HEADER_KLM_WAVE_CHANNEL_5       = "Channel 5 central wave number",
                                HEADER_KLM_CONSTANT1_CHANNEL_5  = "Channel 5 constant 1",
                                HEADER_KLM_CONSTANT2_CHANNEL_5  = "Channel 5 constant 2",
                                DATA_KLM_SLOPE_1_CHANNEL_1  = "Visible prelaunch cal channel 1 - slope 1",                                
                                DATA_KLM_SLOPE_2_CHANNEL_1  = "Visible prelaunch cal channel 1 - slope 2",                                                                
                                DATA_KLM_SLOPE_1_CHANNEL_2  = "Visible prelaunch cal channel 2 - slope 1",                                
                                DATA_KLM_SLOPE_2_CHANNEL_2  = "Visible prelaunch cal channel 2 - slope 2",                                                                
                                DATA_KLM_SLOPE_1_CHANNEL_3A = "Visible prelaunch cal channel 3A - slope 1",                                
                                DATA_KLM_SLOPE_2_CHANNEL_3A = "Visible prelaunch cal channel 3A - slope 2",                                                                
                                DATA_KLM_INTERSECTION_CHANNEL_1 = "Visible prelaunch cal channel 1 - intersection",                                
                                DATA_KLM_INTERCEPT_1_CHANNEL_1  = "Visible prelaunch cal channel 1 - intercept 1",                                
                                DATA_KLM_INTERCEPT_2_CHANNEL_1  = "Visible prelaunch cal channel 1 - intercept 2",                                                                
                                DATA_KLM_INTERSECTION_CHANNEL_2 = "Visible prelaunch cal channel 2 - intersection",                                
                                DATA_KLM_INTERCEPT_1_CHANNEL_2  = "Visible prelaunch cal channel 2 - intercept 1",                                
                                DATA_KLM_INTERCEPT_2_CHANNEL_2  = "Visible prelaunch cal channel 2 - intercept 2",                                                                
                                DATA_KLM_INTERSECTION_CHANNEL_3A = "Visible prelaunch cal channel 3A - intersection",                                
                                DATA_KLM_INTERCEPT_1_CHANNEL_3A = "Visible prelaunch cal channel 3A - intercept 1",                                
                                DATA_KLM_INTERCEPT_2_CHANNEL_3A = "Visible prelaunch cal channel 3A - intercept 2",                                                                
                                DATA_KLM_COEFFICIENT1_CHANNEL_3B = "IR Operational cal Channel 3B coefficient1",
                                DATA_KLM_COEFFICIENT2_CHANNEL_3B = "IR Operational cal Channel 3B coefficient2",
                                DATA_KLM_COEFFICIENT3_CHANNEL_3B = "IR Operational cal Channel 3B coefficient3",
                                DATA_KLM_COEFFICIENT1_CHANNEL_4  = "IR Operational cal Channel 4 coefficient1",
                                DATA_KLM_COEFFICIENT2_CHANNEL_4  = "IR Operational cal Channel 4 coefficient2",
                                DATA_KLM_COEFFICIENT3_CHANNEL_4  = "IR Operational cal Channel 4 coefficient3",
                                DATA_KLM_COEFFICIENT1_CHANNEL_5  = "IR Operational cal Channel 5 coefficient1",
                                DATA_KLM_COEFFICIENT2_CHANNEL_5  = "IR Operational cal Channel 5 coefficient2",
                                DATA_KLM_COEFFICIENT3_CHANNEL_5  = "IR Operational cal Channel 5 coefficient3",
                                DATA_KLM_AVHRR_DIGITAL_B_DATA    = "States of channel and selected channel 3",
                                DATA_KLM_SPACECRAFT_ALTITUDE     = "Spacecraft altitude above reference ellipsoid in km";
    /** Variable interne. */    
    private final Map<String,Field> fields = new HashMap<String,Field>();

    /** Variable interne. */    
    private Field last;

    /**
     * Construit un nouveau format avec en parametre le premier champs.
     * 
     * @param name nom du premier champs du format.
     * @param width taille du champs.
     */    
    private Format(final String name, final int width)
    {
        add(name, new Field(width));
    }

    /**
     * Decris un nouveau champs.
     *
     * @param name nom.
     * @param width taille du champs.
     */
    private void add(final String name, final int width)
    {
        add(name, new Field(last, width));
    }

    /**
     * Decris un nouveau champs.
     *
     * @param name premier champs du format.
     * @param skip offset par rapport au dernier champ du format.
     * @param width taille du champs.
     */
    private void add(final String name, final int skip, final int width)
    {
        add(name, new Field(last, skip, width));
    }

    /**
     * Decris un nouveau champs.
     */
    private void add(final String name, final Field field)
    {
        if (fields.put(name, last=field) != null)
        {
            throw new IllegalArgumentException(name);
        }
    }

    /**
     * Retourne le champs associe au nom "name".
     *
     * @param name Le nom.
     */
    public Field get(final String name) {
        return fields.get(name);
    }
    
    /**
     * Retourne le format de la partie "TBM" du fichier N1B .
     *
     * @param id identifiant du format (KLM, AJ)
     * @return le format de la partie "TBM" du fichier N1B .
     */
    public static synchronized Format getTBM(final int id)
    {
        Format TBM = null;
        
        switch (id) 
        {
            case Format.FORMAT_AJ :
                TBM = new Format("Blank",             30);
                TBM.add("Processing center",           3);// start data set name
                TBM.add("Data type",                1, 4);
                TBM.add(TBM_SPACECRAFT_ID,          1, 2);
                TBM.add("Year day",                 1, 6);// each data set has a unique 
                TBM.add("Start-time",               1, 5);// data set name.
                TBM.add("Stop-time",                1, 5);
                TBM.add("Processing Block ID",      1, 8);
                TBM.add("Source",                   1, 2);// end data set name
                TBM.add("Total / Selective copy",   7, 1);
                TBM.add("Beginning Latitude",          3);
                TBM.add("Ending Latitude",             3);
                TBM.add("Beginning Longitude",         4);
                TBM.add("Ending Longitude",            4);
                TBM.add("Start Hour",                  2);
                TBM.add("Start Minute",                2);
                TBM.add("Number of minutes",           3);
                TBM.add("Appended data selection",     1);
                TBM.add(TBM_CHANNEL_SELECTED,         20);
                break;
                
            // Dans le format KLM, le TBM est appele ARS. L'ARS inclut le TBM legerement modifie + 
            // diverses autres informations.
            case Format.FORMAT_KLM :
                TBM = new Format("Request criteria",  30);
                TBM.add("Processing center",           3);// start data set name
                TBM.add("Data type",                1, 4);
                TBM.add(TBM_SPACECRAFT_ID,          1, 2);
                TBM.add("Year day",                 1, 6);// each data set has a unique 
                TBM.add("Start-time",               1, 5);// data set name.
                TBM.add("Stop-time",                1, 5);
                TBM.add("Processing Block ID",      1, 8);
                TBM.add("Source",                   1, 2);// end data set name                
                TBM.add("Total / Selective copy",   1, 1);
                TBM.add("Beginning Latitude",          3);
                TBM.add("Ending Latitude",             3);
                TBM.add("Beginning Longitude",         4);
                TBM.add("Ending Longitude",            4);
                TBM.add("Start Hour",                  2);
                TBM.add("Start Minute",                2);
                TBM.add("Number of minutes",           3);
                TBM.add("Appended data selection",     1);
                TBM.add(TBM_CHANNEL_SELECTED,         20);
                TBM.add("Divers informations ARS",   395);
                break;                
        }
        
        return TBM;
    }

    /**
     * Retourne le format de la partie "header" du fichier N1B pour les satellites NOAA de a-j.
     *
     * @param id identifiant du format (KLM, AJ)
     * @return le format de la partie "header" du fichier N1B pour les satellites NOAA de a-j.
     */
    public static synchronized Format getHeader(final int id)
    {
        Format HEADER = null;
        switch (id) 
        {
            case FORMAT_AJ :
                HEADER = new Format("Spacecraft Ident.",       1);
                HEADER.add("Data type",                        1);
                HEADER.add(HEADER_START_TIME,                  6);
                HEADER.add(HEADER_NUMBER_OF_SCANS,             2);
                HEADER.add(HEADER_STOP_TIME,                   6);
                HEADER.add("Processing block ident.",          7);
                HEADER.add("Ramp/Auto calibration",            1);
                HEADER.add("Number of data gaps",              2);
                HEADER.add("DACS Quality",                     6);
                HEADER.add("Calibration parameter ident.",     2);
                HEADER.add("DACS status",                      1);
                HEADER.add("Attitude correction",              1);
                HEADER.add("Earth location tolerance",         1);
                HEADER.add("Data set name",                3, 44);
                HEADER.add(HEADER_AJ_CARTESIAN_ELEMENTS,  32, 24);
                HEADER.add("Other",                    48, 14612);
                break;
                
            case FORMAT_KLM :
                HEADER = new Format("Divers",              85);                                
                HEADER.add(HEADER_START_TIME,               8);
                HEADER.add(HEADER_STOP_TIME,             4, 8);                
                HEADER.add(HEADER_NUMBER_OF_SCANS,      24, 2);
                HEADER.add(HEADER_KLM_WAVE_CHANNEL_3B, 150, 4);                
                HEADER.add(HEADER_KLM_CONSTANT1_CHANNEL_3B, 4);
                HEADER.add(HEADER_KLM_CONSTANT2_CHANNEL_3B, 4);
                HEADER.add(HEADER_KLM_WAVE_CHANNEL_4,       4);                
                HEADER.add(HEADER_KLM_CONSTANT1_CHANNEL_4,  4);
                HEADER.add(HEADER_KLM_CONSTANT2_CHANNEL_4,  4);
                HEADER.add(HEADER_KLM_WAVE_CHANNEL_5,       4);                
                HEADER.add(HEADER_KLM_CONSTANT1_CHANNEL_5,  4);
                HEADER.add(HEADER_KLM_CONSTANT2_CHANNEL_5,  4);
                HEADER.add("Other",                     15555);
                break;                
        }
        return HEADER;
    }

    /**
     * Retourne le format de la partie "Data" du fichier N1B pour les satellites NOAA de a-j.
     *
     * @param id identifiant du format (KLM, AJ)
     * @return le format de la partie "Data" du fichier N1B pour les satellites NOAA de a-j.
     */
    public static synchronized Format getData(final int id)
    {
        Format DATA = null;
        switch (id)
        {
            case FORMAT_AJ :
                DATA = new Format(DATA_SCAN_LINE_NUMBER,        2);
                DATA.add(DATA_TIME,                             6);                
                DATA.add(DATA_QUALITY_INDICATOR,                4);
                DATA.add(DATA_AJ_CALIBRATION_COEF_CHANNEL_1,    8);
                DATA.add(DATA_AJ_CALIBRATION_COEF_CHANNEL_2,    8);
                DATA.add(DATA_AJ_CALIBRATION_COEF_CHANNEL_3,    8);
                DATA.add(DATA_AJ_CALIBRATION_COEF_CHANNEL_4,    8);
                DATA.add(DATA_AJ_CALIBRATION_COEF_CHANNEL_5,    8);
                DATA.add("Solar zenithal angle",            1, 51);
                DATA.add(DATA_EARTH_LOCATION,                 204);
                DATA.add("Telemetry",                         140);
                DATA.add(DATA_PACKED_VIDEO_DATA,            13656);
                DATA.add("Other",                             696);
                break;
                
            case FORMAT_KLM :                
                DATA = new Format(DATA_SCAN_LINE_NUMBER,          2);
                DATA.add(DATA_TIME,                            1,10);                
                DATA.add(DATA_KLM_SLOPE_1_CHANNEL_1,          76, 4);
                DATA.add(DATA_KLM_INTERCEPT_1_CHANNEL_1,          4);
                DATA.add(DATA_KLM_SLOPE_2_CHANNEL_1,              4);                
                DATA.add(DATA_KLM_INTERCEPT_2_CHANNEL_1,          4);
                DATA.add(DATA_KLM_INTERSECTION_CHANNEL_1,         4);       
                DATA.add(DATA_KLM_SLOPE_1_CHANNEL_2,          40, 4);
                DATA.add(DATA_KLM_INTERCEPT_1_CHANNEL_2,          4);
                DATA.add(DATA_KLM_SLOPE_2_CHANNEL_2,              4);
                DATA.add(DATA_KLM_INTERCEPT_2_CHANNEL_2,          4);
                DATA.add(DATA_KLM_INTERSECTION_CHANNEL_2,         4);       
                DATA.add(DATA_KLM_SLOPE_1_CHANNEL_3A,         40, 4);
                DATA.add(DATA_KLM_INTERCEPT_1_CHANNEL_3A,         4);
                DATA.add(DATA_KLM_SLOPE_2_CHANNEL_3A,             4);
                DATA.add(DATA_KLM_INTERCEPT_2_CHANNEL_3A,         4);                
                DATA.add(DATA_KLM_INTERSECTION_CHANNEL_3A,        4);                       
                DATA.add(DATA_KLM_COEFFICIENT1_CHANNEL_3B,        4);
                DATA.add(DATA_KLM_COEFFICIENT2_CHANNEL_3B,        4);                
                DATA.add(DATA_KLM_COEFFICIENT3_CHANNEL_3B,        4);                                
                DATA.add(DATA_KLM_COEFFICIENT1_CHANNEL_4,     12, 4);
                DATA.add(DATA_KLM_COEFFICIENT2_CHANNEL_4,         4);                
                DATA.add(DATA_KLM_COEFFICIENT3_CHANNEL_4,         4);                                
                DATA.add(DATA_KLM_COEFFICIENT1_CHANNEL_5,     12, 4);
                DATA.add(DATA_KLM_COEFFICIENT2_CHANNEL_5,         4);                
                DATA.add(DATA_KLM_COEFFICIENT3_CHANNEL_5,         4);                                                                
                DATA.add(DATA_KLM_SPACECRAFT_ALTITUDE,        38, 2);                                                
                DATA.add(DATA_EARTH_LOCATION,              312, 408);                
                DATA.add(DATA_PACKED_VIDEO_DATA,         216, 13656);                
                DATA.add(DATA_KLM_AVHRR_DIGITAL_B_DATA,       10, 2);                                
                DATA.add("Other",                               939);                
                break;                
        }
        return DATA;
    }
    
    /**
     * Retourne la taille occupee par le format.
     *
     * @param format Le Format.
     * @return la taille occupee par le format.
     */
    public int getSize() 
    {        
        final Iterator iterator = fields.values().iterator();
        int size = 0;
        while (iterator.hasNext()) {
            final Field field = (Field)iterator.next();
            size = Math.max(size, field.size + field.offset);
        }        
        return size;
    }     
}