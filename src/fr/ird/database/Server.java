package fr.ird.database;

// Seagis.
import fr.ird.database.coverage.sql.CoverageDataBase;
import fr.ird.database.sample.sql.SampleDataBase;

// J2SE.
import java.util.Properties;
import java.rmi.Naming;
import java.net.InetAddress;
import java.rmi.registry.Registry;
import java.rmi.RMISecurityManager;
import java.rmi.server.RemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import javax.rmi.PortableRemoteObject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Lance le serveur RMI permettant d'accéder au catalogue Image et Observation.
 *
 * @author Remi Eve
 */
public class Server {
    
    /**
     * Main.
     */
    public static void main(final String[] args) {
        if (false)  {
            try {                
                ///////////////////
                // RemoteObject. //
                ///////////////////            

                // Objets distants.
                final CoverageDataBase cdb = new fr.ird.database.coverage.sql.CoverageDataBase();            
                System.out.println("-------------------------------------");
                final SampleDataBase sdb   = new fr.ird.database.sample.sql.SampleDataBase();            

                /*PortableRemoteObject.exportObject(cdb);
                PortableRemoteObject.exportObject(sdb);*/

                // Creation et initialisation du contexte.
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
		properties.put(Context.PROVIDER_URL, "rmi://beatrice:3233");

                Context ctx = new InitialContext((java.util.Hashtable)properties);
                
                ctx.rebind("CoverageDatabase",cdb);        
                ctx.rebind("SampleDatabase",sdb);        
                
                // ctx.unbind("CoverageDatabase");
                // ctx.unbind("SampleDatabase");
                // PortableRemoteObject.unexportObject(cdb);
                // PortableRemoteObject.unexportObject(sdb);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NamingException e) {
                e.printStackTrace();
            }
        } else {
            //////////////////////////
            // UnicastRemoteObject. //
            //////////////////////////            
            
            try {
                // Mise en place d'une politique de sécurité.
                /*if  (System.getSecurityManager() == null) {
                    System.setSecurityManager(new RMISecurityManager());
                }            */

                // Connection au port 3233 du serveur rmiRegistry.
                //final Registry registry = LocateRegistry.createRegistry(3233);

                System.out.println("Enregistrement des objets...");        

                final CoverageDataBase cdb = new fr.ird.database.coverage.sql.CoverageDataBase();            
                Naming.rebind("//beatrice:1099/CoverageDatabase", cdb);

                final SampleDataBase sdb = new fr.ird.database.sample.sql.SampleDataBase();            
                Naming.rebind("//beatrice:1099/SampleDatabase", sdb);

                System.out.println("Fin de la phase d'initialisation.");            
            } catch (final AccessException e) {
                e.printStackTrace();
            } catch (final RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}