package fr.ird.database;

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
 * � une base de donn�es.
 *
 * @author Remi Eve
 */
public abstract class Configuration 
{
    /**
     * Liste de propri�t�s � utiliser lors de la connection
     * � la base de donn�es Images.
     */
    public static final class Key 
    {
        /**
         * Nom de la propri�t�.
         */
        public final String name;
        
        /**
         * Valeur par defaut de la propri�t�, si celle ci n'est pas d�finie.
         */
        public final String defaultValue;
        
        /**
         * Description de la propri�t�.
         */
        public final int description;
    
        /**
         * Construit un nouvel object "Key".
         *
         * @param name          
         * @param description   
         * @param defaultValue
         */
        private Key(final String name, final int description, final String defaultValue) 
        {
            this.name = name;
            this.defaultValue = defaultValue;
            this.description  = description;
        }
        
        /**
         * Retourne un objet de type "Key".
         *
         * @param name          
         * @param description   
         * @param defaultValue
         * @return un objet de type "Key".
         */
        public static final Key get(final String name, final int description, final String defaultValue) 
        {
            return new Key(name, description, defaultValue);
        }
    }    
    
    /**
     * "Properties" utiliser pour extraire les valeurs du fichier de configuration.
     */
    private final Properties properties = new Properties();           
    private final File file;
    
    /**
     * Constructeur.
     *
     * @param file  Fichier contenant les propri�t�s de connection � la base.
     */
    protected Configuration(final File file) 
    {
        this.file = file;
        loadProperties();
    }
    
    /**
     * Charge les propri�t�s.
     */
    private void loadProperties()
    {
        try 
        {
            properties.load(new FileInputStream(file));
        } 
        catch (Exception e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    /**
     * Retourne une instance de configuration.
     *
     * @return une instance de configuration.
     */
    public static Configuration getInstance() 
    {
        return null;
    }
    
    /**
     * Retourne la valeur associ�e � la clef. Si aucune valeur n'est d�finie, la valeur par defaut
     * de la "Key" est retourn�e.
     *
     * @param key  La clef.
     * @return la valeur associ�e � la clef. Si aucune valeur n'est d�finie, la valeur par defaut
     * de la "Key" est retourn�e.
     */
    public String get(final Key key) 
    {
        final String property = properties.getProperty(key.name);
        if (property == null) 
        {
            return key.defaultValue;
        }
        return property;
    }
            
    /**
     * Mise � jour de la clef.
     *
     * @param key   La clef.
     * @param value Nouvelle valeur de la clef.
     */
    public void set(final Key key, final String value) 
    {
        try 
        {
            properties.setProperty(key.name, value);
            properties.store(new FileOutputStream(file), null);
        } 
        catch (Exception e)         
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }    
}