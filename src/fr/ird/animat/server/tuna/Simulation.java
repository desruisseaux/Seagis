/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.animat.server.tuna;

// Entrés/sorties
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

// Remote Method Invocation (RMI)
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Swing et JAI
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.media.jai.JAI;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools
import org.geotools.resources.Arguments;
import org.geotools.resources.MonolineFormatter;

// Seagis
import fr.ird.animat.gui.swing.Viewer;


/**
 * Simulation pour le projet Seas.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Simulation extends fr.ird.animat.server.Simulation {
    /**
     * Le processus qui aura la charge de fermer les connexion aux bases de données.
     */
    private final Thread shutdown;

    /**
     * Construit une simulation à partir du fichier de configuration spécifié.
     *
     * @param  url L'adresse URL du fichier de configuration.
     * @throws IOException si le fichier de configuration n'a pas pu être lu.
     * @throws SQLException si la connexion à une base de données a échouée.
     * @throws TransformException si une transformation de coordonnées était nécessaire et a échouée.
     */
    public Simulation(final URL url) throws IOException, SQLException, TransformException {
        this(url.getPath(), new Configuration(url));
    }
    
    /**
     * Construit une simulation à partir du fichier de configuration spécifié.
     *
     * @param  file Le fichier de configuration.
     * @throws IOException si le fichier de configuration n'a pas pu être lu.
     * @throws SQLException si la connexion à une base de données a échouée.
     * @throws TransformException si une transformation de coordonnées était nécessaire et a échouée.
     */
    public Simulation(final File file) throws IOException, SQLException, TransformException {
        this(file.getPath(), new Configuration(file));
    }
    
    /**
     * Construit une simulation à partir de la configuration spécifiée. Ce constructeur devrait
     * faire partie intégrante d'un constructeur précédent  si Sun voulait bien donner suite au
     * RFE #4093999.
     */
    private Simulation(final String name, final Configuration configuration)
            throws SQLException, RemoteException, TransformException
    {
        super(name, new Environment(configuration));
        this.delay = (int)configuration.pause;
        setProperty("GRAYSCALE_IMAGES",  Boolean.toString(configuration.grayscaleImages ));
        setProperty("FISHERIES_VISIBLE", Boolean.toString(configuration.fisheriesVisible));
        shutdown = new Thread(THREAD_GROUP, (Environment)getEnvironment(), "Simulation shutdown");
        Runtime.getRuntime().addShutdownHook(shutdown);
        /*
         * Ajoute d'office une première population.
         */
        final int n = getEnvironment().newPopulation().getAnimals().size();
        Logger.getLogger("fr.ird.animat.server").info("Population initiale de "+n+" animaux.");
    }

    /**
     * Ferme les connexions à la base de données.
     */
    protected void finalize() throws Throwable {
        Runtime.getRuntime().removeShutdownHook(shutdown);
        getEnvironment().dispose();
        super.finalize();
    }

    /**
     * Démarre une simulation et/ou affiche son résultat.
     * Les arguments acceptés sont:
     * <ul>
     *   <li><code>-config <var>&lt;fichier de configuration&gt;</var></code>
     *       Utilise le fichier de configuration spécifié.</li>
     *   <li><code>-server</code> Démarre la simulation comme serveur.</li>
     *   <li><code>-connect <var>&lt;nom du serveur&gt;</var></code>
     *       Affiche la simulation en cours sur un autre serveur.</li>
     * </ul>
     *
     * @param  Les arguments transmis sur la ligne de commande.
     * @throws IOException si le fichier de configuration n'a pas pu être lu.
     * @throws RemoteException Si une méthode devait être exécutée sur
     *         une machine distante et que cette exécution a échouée.
     * @throws SQLException si la connexion à une base de données a échouée.
     * @throws TransformException si une transformation de coordonnées était nécessaire et a échouée.
     */
    public static void main(String[] args) throws IOException, SQLException, TransformException {
        if (true) {
            MonolineFormatter.init("org.geotools");
            MonolineFormatter.init("fr.ird");
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(128L * 1024 * 1024);
        }
        if (true) try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            Logger.getLogger("fr.ird.animat.server").log(Level.INFO, "Utilise le L&F par défaut.", exception);
        }
        final Arguments  arguments = new Arguments(args);
        final boolean       server = arguments.getFlag("-server");
        final String       connect = arguments.getOptionalString("-connect");
        final String configuration = arguments.getOptionalString("-config");
        args = arguments.getRemainingArguments(0);
        final fr.ird.animat.Simulation simulation;
        /*
         * Construit l'afficheur en premier. Ca nous permettra de commencer immédiatement
         * à afficher les messages du journal. Un onglet pour la simulation sera ajouté
         * plus tard.
         */
        Object view = null;
        if (connect!=null || !server) {
            final Viewer viewer = new Viewer();
            final JFrame  frame = new JFrame("Animats");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(viewer);
            frame.pack();
            frame.show();
            view=viewer;
        }
        /*
         * Construit l'objet 'Simulation'. L'environnement sera construit à partir des informations
         * fournies dans le fichier "Configuration.txt",  ou tout autre fichier fournis en argument
         * sur la ligne de commande.   Si l'option "-server" a été spécifiée,  alors un serveur est
         * démarée. Si l'option "-connect" a été spécifiée, alors on va au contraire se connecter à
         * un serveur existant.
         */
        if (server || connect==null) {
            if (configuration != null) {
                simulation = new Simulation(new File(configuration));
            } else {
                final URL url = Simulation.class.getClassLoader().getResource("simulation.txt");
                if (url != null) {
                    simulation = new Simulation(url);
                } else {
                    arguments.out.println("Un fichier de configuration doit être spécifiée.");
                    arguments.out.println("Utilisez l'option \"-config=<nom du fichier>\".");
                    return;
                }
            }
            if (server) {
                if (true) {
                    // Equivalent au lancement de l'outils 'rmiregistry'
                    LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
                }
                ((Simulation)simulation).export(0);
                arguments.out.println("Serveur démarré. Appuyez sur [Ctrl-C] pour l'arrêter.");
                arguments.out.println("Le résultat de la simulation peut être affichée sur une autre machine en");
                arguments.out.println("spécifiant l'option \"-connect=<nom du serveur>\".");
            }
            simulation.start();
        } else {
            simulation = lookup(connect);
            arguments.out.println("Connecté au serveur.");
        }
        if (view != null) {
            ((Viewer) view).addSimulation(simulation);
        }
    }
}
