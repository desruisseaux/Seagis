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
 * à une base de données.
 *
 * @author Remi Eve
 */
public abstract class Configuration 
{
    /**
     * Liste de propriétés à utiliser lors de la connection
     * à la base de données Images.
     */
    public static final class Key 
    {
        /**
         * Nom de la propriété.
         */
        public final String name;
        
        /**
         * Valeur par defaut de la propriété, si celle ci n'est pas définie.
         */
        public final String defaultValue;
        
        /**
         * Description de la propriété.
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
     * @param file  Fichier contenant les propriétés de connection à la base.
     */
    protected Configuration(final File file) 
    {
        this.file = file;
        loadProperties();
    }
    
    /**
     * Charge les propriétés.
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
     * Retourne la valeur associée à la clef. Si aucune valeur n'est définie, la valeur par defaut
     * de la "Key" est retournée.
     *
     * @param key  La clef.
     * @return la valeur associée à la clef. Si aucune valeur n'est définie, la valeur par defaut
     * de la "Key" est retournée.
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
     * Mise à jour de la clef.
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