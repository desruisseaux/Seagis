/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.seasview;

// J2SE dependencies
import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

// Geotools
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Un menu permettant de sélectionner le "Look and Feel" de l'application.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
final class LookAndFeelMenu extends JMenu implements ActionListener {
    /**
     * Le nom sous lequel mémoriser le L&F choisit dans les préférences.
     */
    static final String PREF = "LookAndFeel";

    /**
     * Les préférences dans lequel mémoriser le L&F choisit.
     */
    private static final Preferences preferences = Preferences.userNodeForPackage(LookAndFeelMenu.class);

    /**
     * Le bureau de l'application.
     */
    private final Component desktop;

    /**
     * Les choix proposés à l'utilisateur.
     */
    private final JMenuItem[] items;

    /**
     * Les noms des classes correspondant aux choix {@link #items}.
     */
    private final UIManager.LookAndFeelInfo[] infos;

    /**
     * Construit un menu.
     *
     * @param desktop Le bureau de l'application.
     * @param resources Les resources à utiliser pour construire les chaînes de caractères.
     */
    public LookAndFeelMenu(final Component desktop, final Resources resources) {
        super(resources.getString(ResourceKeys.LOOK_AND_FEEL));
        this.desktop = desktop;
        this.infos   = UIManager.getInstalledLookAndFeels();
        this.items   = new JMenuItem[infos.length];
        final ButtonGroup group = new ButtonGroup();
        for (int i=0; i<infos.length; i++) {
            final JMenuItem item = new JRadioButtonMenuItem(infos[i].getName());
            items[i] = item;
            group.add(item);
            add(item);
        }
        final LookAndFeel clf = UIManager.getLookAndFeel();
        if (clf != null) {
            final String classname = clf.getClass().getName();
            for (int i=0; i<items.length; i++) {
                if (classname.equals(infos[i].getClassName())) {
                    items[i].setSelected(true);
                }
            }
        }
        for (int i=0; i<items.length; i++) {
            items[i].addActionListener(this);
        }
    }

    /**
     * Appellée automatiquement lorsque l'utilisateur sélectionne une nouvelle apparence.
     */
    public void actionPerformed(final ActionEvent event) {
        final Object source = event.getSource();
        for (int i=0; i<items.length; i++) {
            if (items[i] == source) try {
                final String classname = infos[i].getClassName();
                UIManager.setLookAndFeel(classname);
                SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(desktop));
                preferences.put(PREF, classname);
                return;
            } catch (Exception exception) {
                ExceptionMonitor.show(desktop, exception);
            }
        }
    }

    /**
     * Initialise le "L&F" selon les préférences de l'utilisateur. Cette méthode devrait
     * être appelée au tout début du programme, avant de construire une interface graphique.
     */
    public static void initLookAndFeel() {
        final String classname = preferences.get(PREF, null);
        if (classname != null) try {
            UIManager.setLookAndFeel(classname);
        } catch (Exception exception) {
            preferences.remove(PREF);
            Utilities.unexpectedException("fr.ird.seasview", "Main", "main", exception);
        }
    }
}
