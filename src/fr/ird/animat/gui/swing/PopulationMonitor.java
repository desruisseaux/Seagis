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
package fr.ird.animat.gui.swing;

// J2SE
import java.util.Set;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.rmi.RemoteException;
import javax.swing.JTree;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

// Geotools
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.tree.MutableTreeNode;
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// Seagis
import fr.ird.animat.Clock;
import fr.ird.animat.Animal;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;
import fr.ird.animat.event.PopulationChangeEvent;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.PopulationChangeListener;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Listes et graphiques des animaux dans des populations.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class PopulationMonitor extends JSplitPane
        implements EnvironmentChangeListener, PopulationChangeListener, TreeSelectionListener
{
    /**
     * L'environnement des populations.
     */
    private final Environment environment;

    /**
     * Arborescence des populations et des animaux qu'elles contiennent.
     */
    private final DefaultTreeModel tree;

    /**
     * Objet à utiliser pour écrire les dates.
     */
    private final DateFormat dateFormat = DateFormat.getDateInstance();

    /**
     * Le graphique à afficher.
     */
    private final AnimalMonitor plot = new AnimalMonitor();

    /**
     * Un noeud pour un environnement ou une population.
     */
    private static final class TreeNode extends DefaultMutableTreeNode {
        /** L'étiquette à afficher. */
        private final String label;

        /** Construit un noeud. */
        public TreeNode(final PopulationMonitor owner, final Object object, final Clock clock) {
            super(object);
            final StringBuffer buffer = new StringBuffer();
            if (object instanceof Environment) {
                buffer.append("Environnement");
            } else if (object instanceof Population) {
                buffer.append("Population");
            } else if (object instanceof Animal) {
                buffer.append("Animal");
            } else {
                buffer.append(Utilities.getShortClassName(object));
            }
            buffer.append(" du ");
            owner.dateFormat.format(clock.getTime(0), buffer, new FieldPosition(0));
            label = buffer.toString();
        }

        /** Retourne le texte à utiliser pour afficher ce noeud. */
        public String toString() {
            return label;
        }
    }

    /**
     * Construit un afficheur.
     *
     * @param environment L'environnement des populations.
     */
    public PopulationMonitor(final Environment environment) throws RemoteException {
        super(HORIZONTAL_SPLIT);
        this.environment = environment;
        environment.addEnvironmentChangeListener(this);
        tree = new DefaultTreeModel(new TreeNode(this, environment, environment.getClock()));
        addPopulations(environment.getPopulations());
        final JTree treeView = new JTree(this.tree);
        final TreeSelectionModel selection = treeView.getSelectionModel();
        selection.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        selection.addTreeSelectionListener(this);
        setLeftComponent(new JScrollPane(treeView));
        setRightComponent(plot);
    }

    /**
     * Ajoute les populations spécifiées.
     */
    private void addPopulations(final Set<+Population> populations) throws RemoteException {
        if (populations == null) {
            return;
        }
        final MutableTreeNode root = (MutableTreeNode)tree.getRoot();
        for (final Population population : populations) {
            final TreeNode branch = new TreeNode(this, population, population.getEnvironment().getClock());
            for (final Animal animal : population.getAnimals()) {
                branch.add(new TreeNode(this, animal, animal.getClock()));
            }
            tree.insertNodeInto(branch, root, root.getChildCount());
            population.addPopulationChangeListener(this);
        }
    }

    /**
     * Appelée quand un environnement a changé.
     *
     * @param  event L'événement décrivant le changement dans l'environnement.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     *
     */
    public void environmentChanged(final EnvironmentChangeEvent event) throws RemoteException {
        addPopulations(event.getPopulationAdded());
        final Set<Population> removed = event.getPopulationRemoved();
        if (removed != null) {
            final MutableTreeNode root = (MutableTreeNode)tree.getRoot();
            for (final Population population : removed) {
                population.removePopulationChangeListener(this);
                for (int i=root.getChildCount(); --i>=0;) {
                    final MutableTreeNode branch = (MutableTreeNode)root.getChildAt(i);
                    if (branch.getUserObject().equals(population)) {
                        tree.removeNodeFromParent(branch);
                    }
                }
            }
        }
    }
    
    /**
     * Appelée quand une population a changée.
     *
     * @param  event L'événement décrivant le changement dans une population.
     * @throws RemoteException Si cette méthode devait être exécutée sur une machine distante
     *         et que cette exécution a échouée.
     *
     */
    public void populationChanged(final PopulationChangeEvent event) throws RemoteException {
        // TODO
    }
    
    /**
     * Appelée lorsque l'animal sélectionné a changé.
     */
    public void valueChanged(final TreeSelectionEvent event) {
        final MutableTreeNode node = (MutableTreeNode)event.getPath().getLastPathComponent();
        final Object candidate = node.getUserObject();
        try {
            plot.setAnimal((candidate instanceof Animal) ? (Animal)candidate : null);
        } catch (RemoteException exception) {
            Utilities.unexpectedException("fr.ird.animat.viewer", "AnimalMonitor", "setAnimal", exception);
        }
    }
    
    /**
     * Libère les ressources utilisées par cet objet.
     */
    public void dispose() throws RemoteException {
        environment.removeEnvironmentChangeListener(this);
        for (final Population population : environment.getPopulations()) {
            population.removePopulationChangeListener(this);
        }
        plot.dispose();
    }
}
