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

// J2SE dependencies
import java.util.TimeZone;
import java.util.Date;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.rmi.RemoteException;

// Geotools dependencies
import org.geotools.resources.Utilities;

// Seagis dependencies
import fr.ird.animat.Report;
import fr.ird.animat.Simulation;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;
import fr.ird.animat.server.SampleSource;


/**
 * Composante affichant une carte dans laquelle les animaux se déplacent selon des
 * règles pré-établies.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class RuleSimulationPane extends SimulationPane {
    /**
     * La couche des captures, or <code>null</code> s'il n'y en a pas.
     */
    private final SampleLayer samples;

    /**
     * Liste des couvertures utilisées pour le pas de temps courant.
     */
    private final JList coverageNames;

    /**
     * La date du pas de temps courant.
     */
    private final JLabel stepTime = new JLabel(" ", JLabel.CENTER);

    /**
     * Pourcentage des données manquantes.
     */
    private final JLabel missingData = new JLabel(" ", JLabel.CENTER);

    /**
     * Pourcentage des données manquantes (cumulatif).
     */
    private final JLabel missingDataCumul = new JLabel(" ");

    /**
     * Pourcentage des points à l'extérieur (cumulatif).
     */
    private final JLabel outsideCumul = new JLabel(" ");

    /**
     * Affiche la quantité de mémoire allouée.
     */
    private final JLabel allocatedMemory = new JLabel();

    /**
     * Affiche la quantité de mémoire utilisée.
     */
    private final JLabel usedMemory = new JLabel();

    /**
     * Le format à utiliser pour écrire la date courante.
     */
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

    /**
     * Le format à utiliser pour lire et écrire des nombres.
     */
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    /**
     * Le format à utiliser pour lire et écrire des pourcentages.
     */
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    /**
     * Construit un afficheur.
     *
     * @param simulation La simulation à afficher.
     */
    public RuleSimulationPane(final Simulation simulation) throws RemoteException {
        super(simulation);
        final Environment environment = environmentLayer.environment;
        final TimeZone timezone = environment.getClock().getTimeZone();
        if (getBoolean("FISHERIES_VISIBLE", true) && environment instanceof SampleSource) {
            samples = new SampleLayer((SampleSource)environment);
            samples.setTimeZone(timezone);
            mapPane.getRenderer().addLayer(samples);
        } else {
            samples = null;
        }
        coverageNames = new JList(environmentLayer);
        final JPanel control = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets.right=6; c.insets.left=6; c.insets.top=6; c.insets.bottom=6;
        c.gridx=0; c.gridwidth=1; c.gridheight=1; c.weightx=1; c.fill=c.BOTH;
        c.gridy=0;
        control.add(stepTime, c);
        c.gridy++; c.insets.bottom=0;
        control.add(new JLabel("Données courantes"), c);
        c.gridy++; c.weighty=1; c.insets.bottom=6;
        control.add(new JScrollPane(coverageNames), c);
        if (true) {
            c.gridy++; c.weighty=0;
            c.insets.top=0;
            control.add(missingData, c);
            c.insets.top=6;
        }
        if (true) {
            c.gridy++; c.weighty=0;
            final JPanel panel = new JPanel(new GridLayout(2,1));
            panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder("Cumulatif"),
                            BorderFactory.createEmptyBorder(0, 9, 3, 0)));
            panel.add(missingDataCumul);
            panel.add(outsideCumul);
            control.add(panel, c);
        }
        if (true) {
            c.gridy++;
            final JPanel panel = new JPanel(new GridLayout(2,2));
            panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder("Mémoire"),
                            BorderFactory.createEmptyBorder(0, 9, 3, 0)));
            panel.add(new JLabel("Allouée:"));  panel.add(allocatedMemory);
            panel.add(new JLabel("Utilisée:")); panel.add(usedMemory);
            control.add(panel, c);
        }
        if (true) {
            c.gridy++;
            final JButton addPop = new JButton("Ajouter une population");
            addPop.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    addPopulation(event);
                }
            });
            control.add(addPop, c);
        }
        stepTime.setFont(stepTime.getFont().deriveFont(Font.BOLD));
        dateFormat.setTimeZone(timezone);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        updateLabels();
        setControlPanel(control);
        mapPane.reset();
    }

    /**
     * Ajoute une population initialisé avec les positions de pêches actuels.
     */
    private void addPopulation(final ActionEvent event) {
        try {
            environmentLayer.environment.newPopulation();
            // La méthode 'propertyChange' se chargera de détecter qu'une nouvelle
            // population a été ajoutée et de construire la couche correspondante.
        } catch (RemoteException exception) {
            EnvironmentLayer.failed("Environment", "newPopulation", exception);
            JButton source = (JButton) event.getSource();
            source.setText("Erreur");
            source.setEnabled(false);
        }
    }

    /**
     * Met à jour les étiquettes (date courante, mémoire utilisée, etc.).
     */
    protected void updateLabels() {
        final Runtime runtime = Runtime.getRuntime();
        final long allocated = runtime.totalMemory();
        final long used = allocated-runtime.freeMemory();
        allocatedMemory.setText(numberFormat.format(allocated/(1024*1024.0))+" Mo");
        usedMemory.setText(percentFormat.format((double)used/(double)allocated));
        Date time;
        try {
            time = environmentLayer.environment.getClock().getTime();
            stepTime.setText(dateFormat.format(time));
        } catch (RemoteException exception) {
            time = null;
            stepTime.setText(Utilities.getShortClassName(exception));
            EnvironmentLayer.failed("Environment", "getClock", exception);
        }
        try {
            Report report = environmentLayer.environment.getReport(false);
            final FieldPosition dummy = new FieldPosition(0);
            final StringBuffer buffer = new StringBuffer();
            percentFormat.format(report.percentMissingData(), buffer, dummy);
            buffer.append(" de données manquantes");
            missingData.setText(buffer.toString());

            report = environmentLayer.environment.getReport(true);
            buffer.setLength(0);
            percentFormat.format(report.percentMissingData(), buffer, dummy);
            buffer.append(" de données manquantes");
            missingDataCumul.setText(buffer.toString());

            buffer.setLength(0);
            percentFormat.format(report.percentOutsideSpatialBounds(), buffer, dummy);
            buffer.append(" à l'extérieur");
            outsideCumul.setText(buffer.toString());
        } catch (RemoteException exception) {
            missingData.setText(Utilities.getShortClassName(exception));
            EnvironmentLayer.failed("Environment", "getReport", exception);
        }
        if (samples != null) {
            samples.refresh(time);
        }
    }
}
