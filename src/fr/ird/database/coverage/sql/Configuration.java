/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2004 Institut de Recherche pour le Développement
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
 */
package fr.ird.database.coverage.sql;

// J2SE dependencies.
import java.net.URL;
import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;

// Seagis.
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Cette classe permet d'obtenir les valeurs de configuration de la connexion  
 * à la base de données d'images.
 *
 * @author Remi Eve
 */
final class Configuration extends fr.ird.database.Configuration {       
    /**
     * Définition des clés accessibles.
     */
    public static final Key
        KEY_GRID_COVERAGES_ID_INSERT = Key.get(Table.GRID_COVERAGES+"_ID:INSERT",
                                               ResourceKeys.SQL_GRID_COVERAGES_ID_INSERT,
                                               "INSERT INTO \"" + Table.GRID_COVERAGES + "\" (ID, subseries, filename, start_time, end_time, geometry) VALUES (?, ?, ?, ?, ?, ?)"),

        KEY_GRID_COVERAGES_INSERT = Key.get(Table.GRID_COVERAGES+":INSERT", 
                                            ResourceKeys.SQL_GRID_COVERAGES_INSERT, 
                                            "INSERT INTO \"" + Table.GRID_COVERAGES + "\" ( subseries, filename, start_time, end_time, geometry) VALUES ( ?, ?, ?, ?, ?)"),                                                                                         

        KEY_GRID_GEOMETRIES_ID_INSERT = Key.get(Table.GRID_GEOMETRIES+"_ID:INSERT", 
                                                ResourceKeys.SQL_GRID_GEOMETRIES_ID_INSERT, 
                                                "INSERT INTO \"" + Table.GRID_GEOMETRIES + "\" (id, x_min, x_max, y_min, y_max, width, height, coordinate_system) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"),                                                                                         

        KEY_GRID_GEOMETRIES_INSERT = Key.get(Table.GRID_GEOMETRIES+":INSERT", 
                                             ResourceKeys.SQL_GRID_GEOMETRIES_INSERT, 
                                             "INSERT INTO \"" + Table.GRID_GEOMETRIES + "\" (x_min, x_max, y_min, y_max, width, height, coordinate_system) VALUES (?, ?, ?, ?, ?, ?, ?)"),                                                                                         

        KEY_GRID_COVERAGES3 = Key.get(Table.GRID_COVERAGES+":filename1", 
                                      ResourceKeys.SQL_GRID_COVERAGES_BY_FILENAME,
                                      "SELECT gridcoverages.id FROM \"" + Table.GRID_COVERAGES + "\" gridcoverages, \"" + Table.SUBSERIES + "\" subseries WHERE subseries.id=gridcoverages.subseries AND series=? AND filename LIKE ?"),                            

        KEY_GEOMETRY           = Key.get(Table.GRID_GEOMETRIES+"_ID",      
                                         ResourceKeys.SQL_GRID_GEOMETRY, 
                                         "SELECT id FROM \"" + Table.GRID_GEOMETRIES + "\" WHERE x_min=? AND x_max=? AND y_min=? AND y_max=? AND width=? AND height=? AND coordinate_system=?"),

    /**
     * Constructeur.
     */
    private Configuration() {
        super(CoverageDataBase.getDefaultConfigurationFile());    
    }    
    
    /**
     * Retourne une instance de configuration.
     *
     * @return une instance de configuration.
     */
    public static Configuration getInstance()
    {
        return new Configuration();
    }
    
    public String getDescription() {
    }
    
    protected java.util.logging.Logger getLogger() {
    }
    
    }