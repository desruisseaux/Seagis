package fr.ird.database.sample.sql;

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
 * à la base de données Pêche.
 *
 * @author Remi Eve
 */
public class Configuration extends fr.ird.database.Configuration
{    
    /**
     * Définition des "Key" accessibles.
     */
    public static final Key KEY_DESCRIPTORS = Key.get(Table.DESCRIPTORS, 
                                                      ResourceKeys.SQL_DESCRIPTORS, 
                                                      "SELECT nom, position, paramètre, opération, distribution, scale, offset, log FROM " + Table.DESCRIPTORS + " INNER JOIN " + Table.DISTRIBUTIONS + " ON " + Table.DESCRIPTORS + ".distribution = " + Table.DISTRIBUTIONS + ".ID WHERE nom LIKE ?"),

                        KEY_PARAMETERS = Key.get(Table.PARAMETERS, 
                                                 ResourceKeys.SQL_PARAMETERS, 
                                                 "SELECT id, nom, séries0, séries1, bande FROM " + Table.PARAMETERS + " WHERE id=? ORDER BY nom"),
    
                        KEY_LINEAR_MODELS  = Key.get("ModèlesLinéaires"/*Table.LINEAR_MODELS*/, 
                                                     ResourceKeys.SQL_LINEAR_MODELS, 
                                                     "SELECT source1, source2, coefficient FROM [" + Table.LINEAR_MODELS + "] WHERE cible=?"),
            
                        KEY_ENVIRONMENTS_UPDATE = Key.get(Table.ENVIRONMENTS+":UPDATE", 
                                                          ResourceKeys.SQL_ENVIRONMENTS_UPDATE, 
                                                          "UPDATE " + Table.ENVIRONMENTS + " SET [?]=? WHERE capture=? AND position=? AND paramètre=?"),

                        KEY_ENVIRONMENTS_INSERT = Key.get(Table.ENVIRONMENTS+":INSERT", 
                                                          ResourceKeys.SQL_ENVIRONMENTS_INSERT, 
                                                          "INSERT INTO " + Table.ENVIRONMENTS + " (capture,position,paramètre,[?]) VALUES(?,?,?,?)"),
    
                        KEY_OPERATIONS = Key.get(Table.OPERATIONS, 
                                                 ResourceKeys.SQL_OPERATIONS, 
                                                 "SELECT ID, colonne, préfix, opération, nom, remarques FROM " + Table.OPERATIONS + " WHERE ID=? ORDER BY ID"),
    
                        KEY_LINEAR_SAMPLE = Key.get("Linear."+Table.SAMPLES, 
                                                 ResourceKeys.SQL_SAMPLES_LINE, 
                                                 "SELECT id, date, x1, y1, x2, y2, nb_hameçons FROM " + Table.SAMPLES + " WHERE valid=TRUE AND (date>=? AND date<=?) AND (total>=?) ORDER BY date"),

                        KEY_ENVIRONMENTS = Key.get(Table.ENVIRONMENTS,
                                                   ResourceKeys.SQL_ENVIRONMENTS,
                                                   "SELECT capture FROM " + Table.ENVIRONMENTS + " WHERE position=? AND paramètre=? ORDER BY capture"),
           
                        KEY_POSITIONS = Key.get(Table.POSITIONS,
                                                ResourceKeys.SQL_POSITIONS,
                                                "SELECT id, nom, temps, défaut FROM " + Table.POSITIONS + " WHERE ID=? ORDER BY temps DESC"),

                        KEY_SAMPLES_UPDATE = Key.get(Table.SAMPLES+":UPDATE",
                                                     ResourceKeys.SQL_SAMPLES_UPDATE,
                                                     "UPDATE " + Table.SAMPLES + " SET [?]=? WHERE ID=?"),
    
                        KEY_SPECIES = Key.get(Table.SPECIES,
                                              ResourceKeys.SQL_SPECIES,
                                              "SELECT id, anglais, français, latin FROM " + Table.SPECIES + " WHERE ID=?"),

                        KEY_PUNCTUAL_SAMPLE = Key.get("Punctual."+Table.SAMPLES,
                                                      ResourceKeys.SQL_SAMPLES_POINT,
                                                      "SELECT ID, marée, nSennes, date, x, y FROM " + Table.SAMPLES + " WHERE (date>=? AND date<=?) AND (x>=? AND x<=?) AND (y>=? AND y<=?) AND (total>=?) ORDER BY date"),
                                                            
                        KEY_DRIVER             = Key.get(SampleDataBase.DRIVER,    
                                                         ResourceKeys.SQL_DRIVER, 
                                                         "sun.jdbc.odbc.JdbcOdbcDriver"),

                        KEY_SOURCE             = Key.get(SampleDataBase.SOURCE,    
                                                         ResourceKeys.SQL_SOURCE, 
                                                         "jdbc:odbc:SEAS-Sennes"),

                        KEY_TIME_ZONE          = Key.get(SampleDataBase.TIMEZONE,  
                                                         ResourceKeys.SQL_TIME_ZONE,  
                                                         "UTC"),

                        KEY_LOGIN              = Key.get("LOGIN",                    
                                                         ResourceKeys.SQL_LOGIN,
                                                         "postgres"),

                        KEY_PASSWORD           = Key.get("PASSWORD",                 
                                                         ResourceKeys.SQL_PASSWORD, 
                                                         "postrges"); 

    /**
     * Constructeur.
     */
    private Configuration() 
    {        
        super(SampleDataBase.getDefaultConfigurationFile());    
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