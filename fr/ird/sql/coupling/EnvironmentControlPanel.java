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
import java.awt.event.*;
import javax.swing.*;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

// JAI
import javax.media.jai.KernelJAI;

// geotools
import org.geotools.resources.XArray;
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.gui.swing.CoordinateChooser;
import org.geotools.gui.swing.LoggingPanel;

// Base de données SEAS
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.EnvironmentTable;

// Divers
import fr.ird.awt.KernelEditor;
import fr.ird.awt.DisjointLists;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


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
     * La colonne dans laquelle écrire les données.
     */
    private final JTextField column;

    /**
     * Liste des séries à traiter.
     */
    private final DisjointLists series;

    /**
     * Liste des pas de temps à utiliser.
     */
    private final JList timeLags;

    /**
     * Coordonnées spatio-temporelles des captures à traiter.
     */
    private final CoordinateChooser coordinates;

    /**
     * Matrices à appliquer sur les données pour calculer les
     * magnitudes des grandients.
     */
    private final KernelEditor kernelH, kernelV;

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
        column      = new JTextField();
        coordinates = new CoordinateChooser();
        timeLags    = new JList();
        kernelV     = new KernelEditor();
        kernelH     = new KernelEditor();

        final Resources resources = Resources.getResources(null);
        series     .setToolTipText("Paramètres environnementaux à utiliser");
        operation  .setToolTipText("Opération à appliquer sur les images");
        column     .setToolTipText("Colonne de destination dans la base de données");
        coordinates.setToolTipText("Coordonnées spatio-temporelles des captures à prendre en compte");
        timeLags   .setToolTipText("Décalage de temps (en jours) par rapport à la date de chaque capture");

        final JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(520,280));
        ////////////////////////////////////////////
        ////////    Onglet Séries sources   ////////
        ////////////////////////////////////////////
        if (true) {
            series.addElements(filler.getSeries());
            series.setBorder(BorderFactory.createTitledBorder("Paramètres environnementaux"));
            final JPanel panel = new JPanel(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
            c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
            c.insets.top=3; c.insets.bottom=3; c.insets.right=6; c.insets.left=6;
            c.gridx=0; c.gridy=0; c.gridwidth=4; c.weighty=1; panel.add(series, c);
            c.gridy=1; c.weighty=0; c.gridwidth=1;
            c.gridx=0; c.weightx=0; c.insets.right=0; panel.add(new JLabel("Opération: "), c);
            c.gridx=2;                                panel.add(new JLabel("Colonne: "), c);
            c.gridx=1; c.weightx=1; c.insets.right=6; panel.add(operation, c);
            c.gridx=3; c.weightx=0.5;                 panel.add(column, c);
            tabs.addTab("Séries", panel);
        }
        /////////////////////////////////////////////////////////////
        ////////    Onglet Coordonnées spatio-temporelles    ////////
        /////////////////////////////////////////////////////////////
        if (true) {
            final JPanel panel = new JPanel(new BorderLayout());
            if (true) {
                // Configure la liste des décalages de temps.
                int selectedCount=0;
                final int[]         days = filler.getDaysToEvaluate();
                final List<Integer> lags = new ArrayList<Integer>();
                int[]    selectedIndices = new int[days.length];
                for (int t=5; t>=-30; t--) {
                    if (Arrays.binarySearch(days, t)>=0) {
                        selectedIndices[selectedCount++] = lags.size();
                    }
                    lags.add(new Integer(t));
                }
                selectedIndices = XArray.resize(selectedIndices, selectedCount);
                timeLags.setListData(lags.toArray());
                timeLags.setSelectedIndices(selectedIndices);
                final JPanel timePanel = new JPanel(new BorderLayout());
                timePanel.add(new JLabel("Décalages en jours"), BorderLayout.NORTH);
                timePanel.add(new JScrollPane(timeLags),        BorderLayout.CENTER);
                timePanel.setBorder(BorderFactory.createEmptyBorder(6,18,6,6));
                panel.add(timePanel, BorderLayout.CENTER);
            }
            coordinates.setSelectorVisible(CoordinateChooser.RESOLUTION, false);
            panel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            panel.add(coordinates, BorderLayout.WEST);
            tabs.addTab("Coordonnées", panel);
        }
        //////////////////////////////////////////////////
        ////////    Onglet Operateurs de Sobel    ////////
        //////////////////////////////////////////////////
        if (true) {
            final String GRADIENT_MASKS = resources.getString(ResourceKeys.GRADIENT_MASKS);
            final float cos45 = (float)Math.cos(Math.toRadians(45));
            kernelH.removeKernel(KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL);
            kernelV.removeKernel(KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL);
            kernelH.addKernel(GRADIENT_MASKS, "Sobel 7\u00D77", getSobel(7, true ));
            kernelV.addKernel(GRADIENT_MASKS, "Sobel 7\u00D77", getSobel(7, false));
            kernelH.addKernel(GRADIENT_MASKS, "Sobel 5\u00D75", getSobel(5, true ));
            kernelV.addKernel(GRADIENT_MASKS, "Sobel 5\u00D75", getSobel(5, false));
            kernelH.addKernel(GRADIENT_MASKS, "Sobel 3\u00D73", KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL);
            kernelV.addKernel(GRADIENT_MASKS, "Sobel 3\u00D73", KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL);

            kernelH.removeKernel(KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL);
            kernelV.removeKernel(KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL);
            kernelH.setKernel   (KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL);
            kernelV.setKernel   (KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL);
            final JPanel panel = new JPanel(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
            c.insets.top = c.insets.bottom = c.insets.left = c.insets.right = 6;
            c.gridy=0; c.weightx=1; c.gridwidth=1; c.gridheight=1; c.fill=c.BOTH;
            c.gridx=0; panel.add(new JLabel("Composante verticale",   JLabel.CENTER), c);
            c.gridx=1; panel.add(new JLabel("Composante horizontale", JLabel.CENTER), c);
            c.gridy=1; c.weighty=1;
            c.gridx=0; panel.add(kernelH, c); // Note: les étiquettes sont inversées: 
            c.gridx=1; panel.add(kernelV, c); // SOBEL_HORIZONTAL semble calculer la
            tabs.addTab("Gradient", panel);  // composante verticale du gradient, et vis-versa.
            final int tabIndex = tabs.getTabCount()-1;
            operation.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    operationSelected(tabs, tabIndex);
                }
            });
            operationSelected(tabs, tabIndex);
        }
    }

    /**
     * Retourne une extension de l'opérateur de Sobel. Pour chaque élément dont la position
     * par rapport à l'élément central est (x,y),  on calcul la composante horizontale avec
     * le cosinus de l'angle divisé par la distance. On peut l'écrire comme suit:
     *
     * <blockquote><pre>
     *     cos(atan(y/x)) / sqrt(x²+y²)
     * </pre></blockquote>
     *
     * En utilisant l'identité 1/cos² = (1+tan²), on peut réécrire l'équation comme suit:
     *
     * <blockquote><pre>
     *     1 / sqrt( (x²+y²) * (1 + y²/x²) )
     * </pre></blockquote>
     *
     * @param size Taille de la matrice. Doit être un nombre positif et impair.
     * @param horizontal <code>true</code> pour l'opérateur horizontal,
     *        or <code>false</code> pour l'opérateur vertical.
     */
    private static KernelJAI getSobel(final int size, final boolean horizontal) {
        final int key = size/2;
        final float[] data = new float[size*size];
        for (int y=key; y>=0; y--) {
            int row1 = (key-y)*size + key;
            int row2 = (key+y)*size + key;
            final int y2 = y*y;
            for (int x=key; x!=0; x--) {
                final int x2 = x*x;
                final float v = (float) (2/Math.sqrt((x2+y2)*(1+y2/(double)x2)));
                if (!horizontal) {
                    data[row1-x] = data[row2-x] = -v;
                    data[row1+x] = data[row2+x] = +v;
                } else {
                    // Swap x and y.
                    row1 = (key-x)*size + key;
                    row2 = (key+x)*size + key;
                    data[row1-y] = data[row1+y] = -v;
                    data[row2-y] = data[row2+y] = +v;
                }
            }
        }
        return new KernelJAI(size, size, key, key, data);
    }

    /**
     * Retourne les jours où extraire des données, avant, pendant et après le jour de la pêche.
     * Il décalages sélectionnés par l'utilisateur.
     */
    private int[] getDaysToEvaluate() {
        final Object[] items = timeLags.getSelectedValues();
        final int[] days = new int[items.length];
        for (int i=0; i<items.length; i++) {
            days[i] = ((Number) items[i]).intValue();
        }
        return days;
    }

    /**
     * Appelée chaque fois que l'utilisateur choisit une nouvelle opération.
     */
    private void operationSelected(final JTabbedPane tabs, final int tabIndex) {
        final Operation op = (Operation) operation.getSelectedItem();
        final String  name = op.name;
        tabs.setEnabledAt(tabIndex, (name!=null) && name.equalsIgnoreCase("GradientMagnitude"));
        column.setText(op.column);
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
            final LoggingPanel logging = new LoggingPanel("");
            logging.getHandler().setLevel(Level.FINE);
            logging.show(owner);

            final Map<String,Object> arguments = new HashMap<String,Object>();
            arguments.put("mask1", kernelH.getKernel());
            arguments.put("mask2", kernelV.getKernel());

            filler.getSeries().retainAll(series.getSelectedElements());
            filler.setTimeRange(coordinates.getStartTime(), coordinates.getEndTime());
            filler.setGeographicArea(coordinates.getGeographicArea());
            filler.setOperation((Operation) operation.getSelectedItem(), column.getText(), arguments);
            filler.setDaysToEvaluate(getDaysToEvaluate());
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
        final EnvironmentControlPanel panel = new EnvironmentControlPanel();
        panel.showDialog(null);
        panel.dispose();
        System.exit(0);
    }
}
