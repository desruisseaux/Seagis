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
package fr.ird.sql.coupling;

// J2SE
import java.awt.*;
import javax.swing.*;
import java.util.Collection;
import java.sql.SQLException;

// geotools
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.gui.swing.CoordinateChooser;
import org.geotools.gui.swing.LoggingPanel;

// Base de données SEAS
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.EnvironmentTable;

// Divers
import fr.ird.awt.DisjointLists;


/**
 * Interface graphique pour lancer l'exécution de {@link EnvironmentTableFiller}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentControlPanel extends JPanel {
    /**
     * Nom de l'opération à effectuer sur les images.
     */
    private final JComboBox operation;

    /**
     * Liste des séries à traiter.
     */
    private final DisjointLists series;

    /**
     * Coordonnées spatio-temporelles des captures à traiter.
     */
    private final CoordinateChooser coordinates;

    /**
     * L'objet à utiliser pour remplir la base de données des captures.
     */
    private final EnvironmentTableFiller filler;

    /**
     * <code>true</code> si cet objet devrait fermer la connection
     * avec les bases de données lors de la fermeture.
     */
    private boolean closeOnDispose;

    /**
     * Construit l'interface en utilisant les bases de données par défaut.
     * Les bases seront automatiquement fermées lors de la destruction de
     * cet objet.
     *
     * @throws SQLException si un problème est survenu lors d'un accès à une base de données.
     */
    public EnvironmentControlPanel() throws SQLException {
        this(new EnvironmentTableFiller());
        closeOnDispose = true;
    }

    /**
     * Construit l'interface en utilisant l'objet spécifié. Ce constructeur
     * ne fermera pas les connections à la base de données de l'objet.
     *
     * @param  filler Objet à utiliser pour remplir la table des paramètres environnementaux.
     * @throws SQLException si un problème est survenu lors d'un accès à une base de données.
     */
    public EnvironmentControlPanel(final EnvironmentTableFiller filler) throws SQLException {
        super(new BorderLayout());
        this.filler = filler;
        series      = new DisjointLists();
        operation   = new JComboBox(filler.getAvailableOperations());
        coordinates = new CoordinateChooser();
        series.addElements(filler.getSeries());
        series.setBorder(BorderFactory.createTitledBorder("Paramètres environnementaux"));
        coordinates.setSelectorVisible(CoordinateChooser.RESOLUTION, false);

        final JPanel seriesPane = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
        c.insets.top=3; c.insets.bottom=3; c.insets.right=6; c.insets.left=6;
        c.gridx=0; c.gridy=0; c.gridwidth=2; c.weighty=1; seriesPane.add(series, c);
        c.gridx=1; c.gridy=1; c.gridwidth=1; c.weighty=0; seriesPane.add(operation, c);
        c.gridx=0; c.insets.right=0;         c.weightx=0; seriesPane.add(new JLabel("Opération: "), c);

        final JPanel coordPane = new JPanel();
        coordPane.add(coordinates); // Centre la boîte de dialogue plutôt que de l'étirer.

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Séries",     seriesPane);
        tabs.addTab("Coordonnées", coordPane);
        setPreferredSize(new Dimension(520,280));
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Fait apparaître la boîte de dialogue proposant de remplir la tabke des paramètres
     * environnementaux. Si l'utilisateur clique sur "Ok", l'opération sera immédiatement
     * lancée. Cette méthode peut être appelée à partir de n'importe quel thread. La boîte
     * de dialogue sera lancée dans le thread de <cite>Swing</cite>. Si l'utilisateur clique
     * sur "ok", l'opération sera ensuite lancée dans le thread courant.
     */
    public void showDialog(final Component owner) {
        if (SwingUtilities.showOptionDialog(owner, this, "Environnement aux positions de pêches")) {
            filler.getSeries().retainAll(series.getSelectedElements());
            filler.setTimeRange(coordinates.getStartTime(), coordinates.getEndTime());
            filler.setGeographicArea(coordinates.getGeographicArea());
            filler.setOperation((Operation) operation.getSelectedItem());
            try {
                filler.run();
            } catch (SQLException exception) {
                ExceptionMonitor.show(owner, exception);
            }
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     */
    public void dispose() throws SQLException {
        if (closeOnDispose) {
            filler.close();
            closeOnDispose = false;
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Lance l'exécution de l'application.
     *
     * @throws SQLException si un problème est survenu lors d'un accès à une base de données.
     */
    public static void main(final String[] args) throws SQLException {
        final JFrame frame = new JFrame("Journal");
        frame.getContentPane().add(new LoggingPanel("fr.ird"));
        frame.pack();
        frame.show();

        final EnvironmentControlPanel panel = new EnvironmentControlPanel();
        panel.showDialog(null);
        panel.dispose();
        System.exit(0);
    }
}
