/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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

// Base de donn�es SEAS
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.EnvironmentTable;

// Divers
import fr.ird.awt.DisjointLists;


/**
 * Interface graphique pour lancer l'ex�cution de {@link EnvironmentTableFiller}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentControlPanel extends JPanel {
    /**
     * Nom de l'op�ration � effectuer sur les images.
     */
    private final JComboBox operation;

    /**
     * Liste des s�ries � traiter.
     */
    private final DisjointLists series;

    /**
     * Coordonn�es spatio-temporelles des captures � traiter.
     */
    private final CoordinateChooser coordinates;

    /**
     * L'objet � utiliser pour remplir la base de donn�es des captures.
     */
    private final EnvironmentTableFiller filler;

    /**
     * <code>true</code> si cet objet devrait fermer la connection
     * avec les bases de donn�es lors de la fermeture.
     */
    private boolean closeOnDispose;

    /**
     * Construit l'interface en utilisant les bases de donn�es par d�faut.
     * Les bases seront automatiquement ferm�es lors de la destruction de
     * cet objet.
     *
     * @throws SQLException si un probl�me est survenu lors d'un acc�s � une base de donn�es.
     */
    public EnvironmentControlPanel() throws SQLException {
        this(new EnvironmentTableFiller());
        closeOnDispose = true;
    }

    /**
     * Construit l'interface en utilisant l'objet sp�cifi�. Ce constructeur
     * ne fermera pas les connections � la base de donn�es de l'objet.
     *
     * @param  filler Objet � utiliser pour remplir la table des param�tres environnementaux.
     * @throws SQLException si un probl�me est survenu lors d'un acc�s � une base de donn�es.
     */
    public EnvironmentControlPanel(final EnvironmentTableFiller filler) throws SQLException {
        super(new BorderLayout());
        this.filler = filler;
        series      = new DisjointLists();
        operation   = new JComboBox(filler.getAvailableOperations());
        coordinates = new CoordinateChooser();
        series.addElements(filler.getSeries());
        series.setBorder(BorderFactory.createTitledBorder("Param�tres environnementaux"));
        coordinates.setSelectorVisible(CoordinateChooser.RESOLUTION, false);

        final JPanel seriesPane = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
        c.insets.top=3; c.insets.bottom=3; c.insets.right=6; c.insets.left=6;
        c.gridx=0; c.gridy=0; c.gridwidth=2; c.weighty=1; seriesPane.add(series, c);
        c.gridx=1; c.gridy=1; c.gridwidth=1; c.weighty=0; seriesPane.add(operation, c);
        c.gridx=0; c.insets.right=0;         c.weightx=0; seriesPane.add(new JLabel("Op�ration: "), c);

        final JPanel coordPane = new JPanel();
        coordPane.add(coordinates); // Centre la bo�te de dialogue plut�t que de l'�tirer.

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("S�ries",     seriesPane);
        tabs.addTab("Coordonn�es", coordPane);
        setPreferredSize(new Dimension(520,280));
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Fait appara�tre la bo�te de dialogue proposant de remplir la tabke des param�tres
     * environnementaux. Si l'utilisateur clique sur "Ok", l'op�ration sera imm�diatement
     * lanc�e. Cette m�thode peut �tre appel�e � partir de n'importe quel thread. La bo�te
     * de dialogue sera lanc�e dans le thread de <cite>Swing</cite>. Si l'utilisateur clique
     * sur "ok", l'op�ration sera ensuite lanc�e dans le thread courant.
     */
    public void showDialog(final Component owner) {
        if (SwingUtilities.showOptionDialog(owner, this, "Environnement aux positions de p�ches")) {
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
     * Lib�re les ressources utilis�es par cet objet.
     */
    public void dispose() throws SQLException {
        if (closeOnDispose) {
            filler.close();
            closeOnDispose = false;
        }
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Lance l'ex�cution de l'application.
     *
     * @throws SQLException si un probl�me est survenu lors d'un acc�s � une base de donn�es.
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
