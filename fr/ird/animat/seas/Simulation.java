/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.animat.seas;

// Entr�s/sorties et divers
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.media.jai.JAI;
import javax.swing.JFrame;

// Remote Method Invocation (RMI)
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools
import org.geotools.resources.Arguments;
import org.geotools.resources.MonolineFormatter;

// Seagis
import fr.ird.animat.viewer.Viewer;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.fishery.FisheryDataBase;


/**
 * Simulation pour le projet Seas.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Simulation extends fr.ird.animat.impl.Simulation {
    /**
     * La base de donn�es des images.
     */
    private final ImageDataBase images;

    /**
     * La base de donn�es des p�ches.
     */
    private final FisheryDataBase fisheries;

    /**
     * Le processus qui aura la charge de fermer les connexion aux bases de donn�es.
     */
    private final Thread shutdown;

    /**
     * Construit une simulation � partir du fichier de configuration sp�cifi�.
     *
     * @param  url L'adresse URL du fichier de configuration.
     * @throws IOException si le fichier de configuration n'a pas pu �tre lu.
     * @throws SQLException si la connexion � une base de donn�es a �chou�e.
     */
    public Simulation(final URL url) throws IOException, SQLException {
        this(url.getPath(), new Configuration(url));
    }
    
    /**
     * Construit une simulation � partir du fichier de configuration sp�cifi�.
     *
     * @param  file Le fichier de configuration.
     * @throws IOException si le fichier de configuration n'a pas pu �tre lu.
     * @throws SQLException si la connexion � une base de donn�es a �chou�e.
     */
    public Simulation(final File file) throws IOException, SQLException {
        this(file.getPath(), new Configuration(file));
    }
    
    /**
     * Construit une simulation � partir de la configuration sp�cifi�e.
     * Ce constructeur devrait faire partie int�grante d'un constructeur pr�c�dent
     * si Sun voulait bien donner suite au RFE #4093999
     */
    private Simulation(final String name, final Configuration configuration)
            throws SQLException, RemoteException
    {
        this(name, new ImageDataBase(), new FisheryDataBase(), configuration);
    }

    /**
     * Construit une nouvelle simulation. Ce constructeur devrait faire partie int�grante
     * d'un constructeur pr�c�dent si Sun voulait bien donner suite au RFE #4093999
     */
    private Simulation(final String          name,
                       final ImageDataBase   images,
                       final FisheryDataBase fisheries,
                       final Configuration   configuration)
            throws SQLException, RemoteException
    {
        super(name, new Environment(images, fisheries, configuration));
        this.images    = images;
        this.fisheries = fisheries;
        this.delay     = (int)configuration.pause;
        shutdown = new Thread() {
            public void run() {
                try {
                    images.close();
                    fisheries.close();
                    Logger.getLogger("fr.ird.animat.seas").fine("D�connexion des bases de donn�es");
                } catch (SQLException exception) {
                    final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
                    record.setSourceClassName("Simulation");
                    record.setSourceMethodName("shutdown");
                    record.setThrown(exception);
                    Logger.getLogger("fr.ird.animat.seas").log(record);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdown);
        /*
         * Ajoute d'office une premi�re population.
         */
        final int n = getEnvironment().newPopulation().getAnimals().size();
        Logger.getLogger("fr.ird.animat.seas").info("Population initiale de "+n+" animaux.");
    }

    /**
     * Ferme les connexions � la base de donn�es.
     */
    protected void finalize() throws Throwable {
        Runtime.getRuntime().removeShutdownHook(shutdown);
        fisheries.close();
        images.close();
        super.finalize();
    }

    /**
     * D�marre une simulation et/ou affiche son r�sultat.
     * Les arguments accept�s sont:
     * <ul>
     *   <li><code>-config <var>&lt;fichier de configuration&gt;</var></code>
     *       Utilise le fichier de configuration sp�cifi�.</li>
     *   <li><code>-server</code> D�marre la simulation comme serveur.</li>
     *   <li><code>-connect <var>&lt;nom du serveur&gt;</var></code>
     *       Affiche la simulation en cours sur un autre serveur.</li>
     * </ul>
     *
     * @param  Les arguments transmis sur la ligne de commande.
     * @throws IOException si le fichier de configuration n'a pas pu �tre lu.
     * @throws SQLException si la connexion � une base de donn�es a �chou�e.
     * @throws RemoteException Si une m�thode devait �tre ex�cut�e sur
     *         une machine distante et que cette ex�cution a �chou�e.
     */
    public static void main(String[] args) throws IOException, SQLException {
        if (true) {
            MonolineFormatter.init("org.geotools");
            MonolineFormatter.init("fr.ird");
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(128L * 1024 * 1024);
        }
        final Arguments  arguments = new Arguments(args);
        final boolean       server = arguments.getFlag("-server");
        final String       connect = arguments.getOptionalString("-connect");
        final String configuration = arguments.getOptionalString("-config");
        args = arguments.getRemainingArguments(0);
        final fr.ird.animat.Simulation simulation;
        /*
         * Construit l'objet 'Simulation'. L'environnement sera construit � partir des informations
         * fournies dans le fichier "Configuration.txt",  ou tout autre fichier fournis en argument
         * sur la ligne de commande.   Si l'option "-server" a �t� sp�cifi�e,  alors un serveur est
         * d�mar�e. Si l'option "-connect" a �t� sp�cifi�e, alors on va au contraire se connecter �
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
                    arguments.out.println("Un fichier de configuration doit �tre sp�cifi�e.");
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
                arguments.out.println("Serveur d�marr�. Appuyez sur [Ctrl-C] pour l'arr�ter.");
                arguments.out.println("Le r�sultat de la simulation peut �tre affich�e sur une autre machine en");
                arguments.out.println("sp�cifiant l'option \"-connect=<nom du serveur>\".");
            }
            simulation.start();
        } else {
            simulation = lookup(connect);
            arguments.out.println("Connect� au serveur.");
        }
        /*
         * Construit l'afficheur.
         */
        if (connect!=null || !server) {
            final Viewer viewer = new Viewer(simulation);
            final JFrame  frame = new JFrame(simulation.getName());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(viewer);
            frame.pack();
            frame.show();
        }
    }
}
