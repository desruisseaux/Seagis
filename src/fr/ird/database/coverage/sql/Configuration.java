package fr.ird.database.coverage.sql;

// Seagis.
import fr.ird.resources.seagis.ResourceKeys;

// J2SE.
import java.net.URL;
import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Cette classe permet d'obtenir les valeurs de configuration de la connexion  
 * à la base de données d'images.
 *
 * @author Remi Eve
 */
public class Configuration extends fr.ird.database.Configuration {       
    /**
     * Définition des clés accessibles.
     */
    public static final Key
        KEY_SERIES_TREE = Key.get(Table.SERIES+":TREE", ResourceKeys.SQL_SERIES_TREE,
            "SELECT subseries.id, subseries.name, subseries.description, series.id, series.name, "+
            "series.description, operations.id, operations.name, operations.description, parameters.ID, "+
            "parameters.name, parameters.description, subseries.format, series.period FROM \"" +
            Table.PARAMETERS + "\" parameters, \"" + Table.OPERATIONS + "\" operations, \"" +
            Table.SERIES+ "\" series, \"" + Table.SUBSERIES + "\" subseries WHERE "+
            "parameters.id=parameter AND operations.id=operation AND series.id=series "+
            "AND visible=TRUE ORDER BY parameters.name, operations.name, series.name"),

        KEY_SERIES_ID = Key.get(Table.SERIES+":ID", ResourceKeys.SQL_SERIES_BY_ID,
            "SELECT id, name, description, period FROM \"" + Table.SERIES + "\" WHERE ID=?"),

        KEY_SERIES_SUBSERIES   = Key.get(Table.SERIES+":SubSeries",
                                         ResourceKeys.SQL_SERIES_BY_SUBSERIES,
                                         "SELECT id, name, description, format FROM \"" + Table.SUBSERIES + "\" WHERE series=?"),

        KEY_SERIES_NAME        = Key.get(Table.SERIES,
                                 ResourceKeys.SQL_SERIES_BY_NAME,
                                 "SELECT id, name, description, period FROM \"" + Table.SERIES    + "\" WHERE name LIKE ?"),

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

        KEY_GRID_COVERAGES     = Key.get(Table.GRID_COVERAGES, 
                                         ResourceKeys.SQL_GRID_COVERAGES, 
                                         "SELECT gridcoverages.id, series, pathname, filename, start_time, end_time, x_min, x_max, y_min, y_max, width, height, coordinate_system, format FROM \"" + Table.GRID_GEOMETRIES + "\" gridgeometries, \"" + Table.GRID_COVERAGES + "\" gridcoverages, \"" + Table.SUBSERIES + "\" subseries WHERE gridcoverages.subseries = subseries.id AND gridgeometries.id = gridcoverages.geometry AND (x_max>? AND x_min<? AND y_max>? AND y_min<?) AND (((end_time Is Null) OR end_time>=?) AND ((start_time Is Null) OR start_time<=?)) AND series=? ORDER BY end_time, subseries"),                            

        KEY_GRID_COVERAGES3 = Key.get(Table.GRID_COVERAGES+":filename1", 
                                      ResourceKeys.SQL_GRID_COVERAGES_BY_FILENAME,
                                      "SELECT gridcoverages.id FROM \"" + Table.GRID_COVERAGES + "\" gridcoverages, \"" + Table.SUBSERIES + "\" subseries WHERE subseries.id=gridcoverages.subseries AND series=? AND filename LIKE ?"),                            

        KEY_GRID_COVERAGES1 = Key.get(Table.GRID_COVERAGES+":ID",
                                      ResourceKeys.SQL_GRID_COVERAGES_BY_ID, 
                                      "SELECT gridcoverages.id, series, pathname, filename, start_time, end_time, x_min AS xmin, x_max as xmax, y_min as ymin, y_max as ymax, width, height, coordinate_system, format FROM \"" + Table.GRID_GEOMETRIES + "\" gridgeometries, + \"" + Table.GRID_COVERAGES + "\" gridcoverages, \"" + Table.SUBSERIES + "\" subseries WHERE gridcoverages.subseries = subseries.id AND gridgeometries.id = gridcoverages.geometry and gridcoverages.id=?"),

        KEY_GRID_COVERAGES2 = Key.get(Table.GRID_COVERAGES+":filename", 
                                      ResourceKeys.SQL_GRID_COVERAGES_BY_FILENAME, 
                                      "SELECT gridcoverages.id, series, pathname, filename, start_time, end_time, x_min AS xmin, x_max as xmax, y_min as ymin, y_max as ymax, width, height, coordinate_system, format FROM \"" + Table.GRID_GEOMETRIES + "\" gridgeometries, + \"" + Table.GRID_COVERAGES + "\" gridcoverages, \"" + Table.SUBSERIES + "\" subseries WHERE gridcoverages.subseries = subseries.id AND gridgeometries.id = gridcoverages.geometry and (visible=TRUE) AND (series=?) AND (filename LIKE ?)"),                            

        KEY_FORMATS            = Key.get(Table.FORMATS, 
                                         ResourceKeys.SQL_FORMATS, 
                                         "SELECT id, name, mime, extension, geophysics FROM \"" + Table.FORMATS + "\" WHERE ID=?"),

        KEY_SAMPLE_DIMENSIONS  = Key.get(Table.SAMPLE_DIMENSIONS, 
                                         ResourceKeys.SQL_SAMPLE_DIMENSIONS, 
                                         "SELECT id, band, units FROM \"" + Table.SAMPLE_DIMENSIONS + "\" WHERE format=? ORDER BY band"),

        KEY_CATEGORIES         = Key.get(Table.CATEGORIES, 
                                         ResourceKeys.SQL_CATEGORIES, 
                                         "SELECT name, lower, upper, c0, c1, log, colors FROM \"" + Table.CATEGORIES + "\" WHERE band=? ORDER BY lower"),

        KEY_GRID_GEOMETRIES    = Key.get(Table.GRID_GEOMETRIES, 
                                         ResourceKeys.SQL_GRID_GEOMETRIES, 
                                         "SELECT Min(gridcoverages.start_time) AS start_time, Max(gridcoverages.end_time) AS end_time, Min(gridgeometries.x_min) AS x_min, Min(gridgeometries.y_min) AS y_min, Max(gridgeometries.x_max) AS x_max, Max(gridgeometries.y_max) AS y_max FROM \"" + Table.GRID_COVERAGES + "\" gridcoverages, \"" + Table.SUBSERIES + "\" subseries, \"" + Table.GRID_GEOMETRIES + "\" gridgeometries WHERE gridcoverages.subseries = subseries.id AND gridgeometries.id = gridcoverages.geometry AND visible=true"),

        KEY_GEOMETRY           = Key.get(Table.GRID_GEOMETRIES+"_ID",      
                                         ResourceKeys.SQL_GRID_GEOMETRY, 
                                         "SELECT id FROM \"" + Table.GRID_GEOMETRIES + "\" WHERE x_min=? AND x_max=? AND y_min=? AND y_max=? AND width=? AND height=? AND coordinate_system=?"),

        KEY_DRIVER             = Key.get(CoverageDataBase.DRIVER,    
                                         ResourceKeys.SQL_DRIVER, 
                                         "org.postgresql.Driver"),

        KEY_SOURCE             = Key.get(CoverageDataBase.SOURCE,    
                                         ResourceKeys.SQL_SOURCE, 
                                         "jdbc:postgresql://golden.teledetection.fr/Images"),

        KEY_TIME_ZONE          = Key.get(CoverageDataBase.TIMEZONE,  
                                         ResourceKeys.SQL_TIME_ZONE,  
                                         "UTC"),

        KEY_DIRECTORY          = Key.get(Table.DIRECTORY,            
                                         ResourceKeys.SQL_DIRECTORY,  
                                         null),

        KEY_LOGIN              = Key.get("LOGIN",                    
                                         ResourceKeys.SQL_LOGIN,
                                         "postgres"),

        KEY_PASSWORD           = Key.get("PASSWORD",
                                         ResourceKeys.SQL_PASSWORD, 
                                         "postgres"); 

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
    }