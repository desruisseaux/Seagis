/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.gui.swing;

// J2SE standard
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;

// AWT et Swing
import java.awt.EventQueue;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

// Geotools
import org.geotools.gui.swing.LoggingPanel;

// Animats
import fr.ird.animat.Simulation;


/**
 * Composante affichant une carte représentant la position des animaux dans leur environnement,
 * ainsi que quelques contrôles.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Viewer extends JTabbedPane {
    /**
     * Construit un afficheur par défaut. Cet afficheur ne contiendra initialement aucune
     * simulation. Pour afficher une simulation, appelez {@link #addSimulation}.
     */
    public Viewer() {
        super(TOP);
        final LoggingPanel logging = new LoggingPanel("fr.ird");
        logging.setColumnVisible(LoggingPanel.LOGGER, false);
        final Handler handler = logging.getHandler();
        Logger.getLogger("org.geotools").addHandler(handler);
        if (false) handler.setLevel(Level.FINE);
        addTab("Journal", logging);
    }

    /**
     * Ajoute un onglet affichant la simulation spécifiée. Cette méthode peut être appelée
     * de n'importe quel thread (pas nécessairement celui de <cite>Swing</cite>).
     *
     * @param  simulation La simulation à afficher.
     * @throws RemoteException si la construction de la simulation a échouée.
     */
    public void addSimulation(final Simulation simulation) throws RemoteException {
        final String     name = simulation.getName();
        final JComponent pane = new RuleSimulationPane(simulation);
        final JComponent plot = new PopulationMonitor(simulation.getEnvironment());
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final int toShow = getTabCount();
                addTab(name, pane);
                addTab("Graphiques", plot);
                setSelectedIndex(toShow);
            }
        });
    }
}
