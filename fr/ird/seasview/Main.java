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

// Bases de données et images
import java.sql.SQLException;
import java.rmi.RemoteException;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

// Interface utilisateur
import javax.swing.*;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;

// Evénements
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;

// Divers
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.prefs.Preferences;

// JAI
import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;

// Geotools
import org.geotools.resources.Arguments;
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis
import fr.ird.database.gui.swing.ControlPanel;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Point d'entré de l'application SEAS. Cette classe ne contient
 * qu'une seule méthode, {@link #setup}, qui construit la fenêtre
 * de l'application.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Main {
    /**
     * Do not allows instantiation of this class.
     */
    private Main() {
    }

    /**
     * Construit et affiche une fenêtre de l'application SEAS.
     *
     * @param database Connection avec les bases de données.
     */
    public static void setup(final DataBase database) {
        final Resources   resources = Resources.getResources(null);
        final JFrame          frame = new JFrame(resources.getString(ResourceKeys.APPLICATION_TITLE));
        final Desktop       desktop = new Desktop(database);
        final JToolBar      toolBar = new JToolBar("SEAS");
        final JMenuBar      menuBar = new JMenuBar();
        final Container contentPane = frame.getContentPane();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent event)
            {desktop.exit();}
        });
        frame.setSize(800,680);
        contentPane.setLayout(new BorderLayout());
        contentPane.add(desktop, BorderLayout.CENTER);
        contentPane.add(toolBar, BorderLayout.NORTH );
        /*
         * Construction de la barre des menus. On tient aussi une
         * liste des menus qui doivent être activés ou désactivés
         * en fonction de la fenêtre active.
         */
        final List<Action> enableWhenFrameFocused = new ArrayList<Action>();
        frame.setJMenuBar(menuBar);
        /////////////////
        ///  Fichier  ///
        /////////////////
        if (true) {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.FILE));
            menu.setMnemonic(KeyEvent.VK_F);
            menuBar.add(menu);
            //////////////////////////
            ///  Fichier - Nouveau ///
            //////////////////////////
            if (true) {
                final JMenu submenu = new JMenu(resources.getString(ResourceKeys.NEW));
                submenu.setMnemonic(KeyEvent.VK_N);
                menu.add(submenu);
                ////////////////////////////////////////////
                ///  Fichier - Nouveau - Séries d'images ///
                ////////////////////////////////////////////
                if (true) {
                    final Action action = new Action(desktop, ResourceKeys.IMAGES_SERIES, true);
                    action.setAccelerator(KeyEvent.VK_N, KeyEvent.CTRL_MASK);
                    action.setToolTipText(ResourceKeys.NEW_IMAGES_SERIES);
                    action.setMnemonicKey(KeyEvent.VK_S);
                    action.setIcon("general/New");
                    action.addTo(submenu, toolBar);
                }
                //////////////////////////////////////////////////////////
                ///  Fichier - Nouveau - Couplage pêche/environnement  ///
                //////////////////////////////////////////////////////////
                if (false) {
                    final Action action = new Action(desktop, ResourceKeys.COUPLING, true);
                    action.setMnemonicKey(KeyEvent.VK_C);
                    action.addTo(submenu);
                    enableWhenFrameFocused.add(action);
                }
            }
            /////////////////////////
            ///  Fichier - Ouvrir ///
            /////////////////////////
            if (false) {
                final Action action = new Action(desktop, ResourceKeys.OPEN, false);
                action.setAccelerator(KeyEvent.VK_O, KeyEvent.CTRL_MASK);
                action.setMnemonicKey(KeyEvent.VK_O);
                action.setToolTipText(ResourceKeys.OPEN_DESKTOP);
                action.setIcon("general/Open");
                action.addTo(menu, toolBar);
            }
            //////////////////////////////
            ///  Fichier - Enregistrer ///
            //////////////////////////////
            if (false) {
                final Action action = new Action(desktop, ResourceKeys.SAVE, false);
                action.setToolTipText(ResourceKeys.SAVE_DESKTOP);
                action.setIcon("general/Save");
                action.addTo(menu, toolBar);
            }
            //////////////////////////////////////
            ///  Fichier - Enregistrer sous... ///
            //////////////////////////////////////
            if (false) {
                final Action action = new Action(desktop, ResourceKeys.SAVE_AS, true);
                action.setAccelerator(KeyEvent.VK_S, KeyEvent.CTRL_MASK);
                action.setMnemonicKey(KeyEvent.VK_S); // TODO: devrait être le **deuxième** S.
                action.setToolTipText(ResourceKeys.SAVE_AS);
                action.setIcon("general/SaveAs");
                action.addTo(menu/*, toolBar*/);
            }
            ///////////////////////////
            ///  Fichier - Exporter ///
            ///////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.EXPORT, true);
                action.setMnemonicKey(KeyEvent.VK_E);
                action.setToolTipText(ResourceKeys.EXPORT);
                action.setIcon("general/Export");
                action.addTo(menu, toolBar);
                enableWhenFrameFocused.add(action);
            }
            //////////////////////////
            ///  Fichier - Quitter ///
            //////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.EXIT, false);
                action.addTo(menu);
            }
        }
        /////////////////
        ///  Edition  ///
        /////////////////
        if (true) {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.EDIT));
            menu.setMnemonic(KeyEvent.VK_E);
            menuBar.add(menu);
            toolBar.addSeparator();
            //////////////////////////
            ///  Edition - Annuler ///
            //////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.UNDO, false);
                action.setAccelerator(KeyEvent.VK_Z, KeyEvent.CTRL_MASK);
                action.setToolTipText(ResourceKeys.UNDO);
                action.setIcon("general/Undo");
                action.addTo(menu, toolBar);
                enableWhenFrameFocused.add(action);
            }
            //////////////////////////
            ///  Edition - Refaire ///
            //////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.REDO, false);
                action.setAccelerator(KeyEvent.VK_Y, KeyEvent.CTRL_MASK);
                action.setToolTipText(ResourceKeys.REDO);
                action.setIcon("general/Redo");
                action.addTo(menu, toolBar);
                enableWhenFrameFocused.add(action);
            }
            menu   .addSeparator();
            toolBar.addSeparator();
            /////////////////////////
            ///  Edition - Copier ///
            /////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.COPY, false);
                action.setAccelerator(KeyEvent.VK_C, KeyEvent.CTRL_MASK);
                action.setMnemonicKey(KeyEvent.VK_C);
                action.setToolTipText(ResourceKeys.COPY);
                action.setIcon("general/Copy");
                action.addTo(menu, toolBar);
                enableWhenFrameFocused.add(action);
            }
            ////////////////////////////
            ///  Edition - Supprimer ///
            ////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.DELETE, false);
                action.setAccelerator(KeyEvent.VK_DELETE, 0);
                action.setToolTipText(ResourceKeys.DELETE);
                action.setIcon("general/Delete");
                action.addTo(menu, toolBar);
                enableWhenFrameFocused.add(action);
            }
            menu.addSeparator();
            ////////////////////////////////////
            ///  Edition - Sélectionner tout ///
            ////////////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.SELECT_ALL, false);
                action.setAccelerator(KeyEvent.VK_A, KeyEvent.CTRL_MASK);
                action.setToolTipText(ResourceKeys.SELECT_ALL);
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            ////////////////////////////////////////
            ///  Edition - Inverser la sélection ///
            ////////////////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.INVERT_SELECTION, false);
                action.setToolTipText(ResourceKeys.INVERT_SELECTION);
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
        }
        ///////////////////
        ///  Affichage  ///
        ///////////////////
        if (true) {
            //////////////////////////////////////
            ///  Affichage - Rétablir le zoom  ///
            //////////////////////////////////////
            if (true) {
                toolBar.addSeparator();
                final Action action=new Action(desktop, ResourceKeys.RESET_VIEW, false);
                action.setToolTipText(ResourceKeys.RESET_VIEW);
                action.setIcon("navigation/Home");
                action.addTo(null, toolBar);
                enableWhenFrameFocused.add(action);
            }
        }
        ////////////////
        ///  Séries  ///
        ////////////////
        if (true) {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.SERIES));
            menu.setMnemonic(KeyEvent.VK_S);
            menuBar.add(menu);
            ///////////////////////////////////
            ///  Séries - Ajouter une série ///
            ///////////////////////////////////
            if (true) {
                final Action action=new Action(desktop, ResourceKeys.ADD_SERIES, true);
                action.setMnemonicKey(KeyEvent.VK_A);
                action.setIcon("general/Add");
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            /////////////////////////////////
            ///  Séries - Fermer la série ///
            /////////////////////////////////
            if (true) {
                final Action action=new Action(desktop, ResourceKeys.CLOSE_SERIES, true);
                action.setMnemonicKey(KeyEvent.VK_F);
                action.setIcon("general/Remove");
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            /////////////////////////////////////////
            ///  Séries - Changer les coordonnées ///
            /////////////////////////////////////////
            if (true) {
                menu.addSeparator();
                final Action action=new Action(desktop, ResourceKeys.CHANGE_COORDINATES, true);
                action.setMnemonicKey(KeyEvent.VK_C);
                action.setIcon("general/Find");
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            //////////////////////////////////////////////
            ///  Séries - Sommaire des plages de temps ///
            //////////////////////////////////////////////
            if (true) {
                menu.addSeparator();
                final Action action=new Action(desktop, ResourceKeys.IMAGES_CATALOG, true);
                action.setMnemonicKey(KeyEvent.VK_S);
                action.setIcon("general/Information");
                action.addTo(menu);
            }
        }
        /////////////////
        ///  Analyse  ///
        /////////////////
        if (true) {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.ANALYSES));
            menu.setMnemonic(KeyEvent.VK_A);
            menuBar.add(menu);
            /////////////////////////////////////////////
            ///  Analyses - Environnements des pêches ///
            /////////////////////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.FISHERIES_ENVIRONMENT, true);
                action.setMnemonicKey(KeyEvent.VK_P);
                action.addTo(menu);
            }
            ////////////////////////////////////////////////////////////
            ///  Analyses - Extractions de la table d'environnements ///
            ////////////////////////////////////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.EXTRACT_ENVIRONMENT, true);
                action.setMnemonicKey(KeyEvent.VK_E);
                action.addTo(menu);
            }
        }
        /////////////////////
        ///  Préférences  ///
        /////////////////////
        if (true) {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.PREFERENCES));
            menu.setMnemonic(KeyEvent.VK_P);
            menuBar.add(menu);
            ///////////////////////////////////////////
            ///  Préférences - Images synchronisées ///
            ///////////////////////////////////////////
            if (true) {
                final JMenuItem item=new JCheckBoxMenuItem(resources.getString(ResourceKeys.SYNCHRONIZED_IMAGES));
                item.setSelected(DataBase.preferences.getBoolean("images.synchronized", true));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent event) {
                        final boolean selected = item.isSelected();
                        desktop.setImagesSynchronized(selected);
                        DataBase.preferences.putBoolean("images.synchronized", selected);
                    }
                });
                menu.add(item);
                desktop.setImagesSynchronized(item.isSelected());
            }
            /////////////////////////////////////////
            ///  Préférences - Défilement continu ///
            /////////////////////////////////////////
            if (true) {
                final JMenuItem item=new JCheckBoxMenuItem(resources.getString(ResourceKeys.LIVE_SCROLLING));
                item.setSelected(DataBase.preferences.getBoolean("scroll.continuous", false));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent event) {
                        final boolean selected = item.isSelected();
                        desktop.setPaintingWhileAdjusting(selected);
                        DataBase.preferences.putBoolean("scroll.continuous", selected);
                    }
                });
                menu.add(item);
                desktop.setPaintingWhileAdjusting(item.isSelected());
            }
            menu.addSeparator();
            /////////////////////////////////////
            ///  Préférences - Fuseau horaire ///
            /////////////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.TIME_ZONE, true);
                action.setMnemonicKey(KeyEvent.VK_H);
                action.addTo(menu);
            }
            ///////////////////////////////////////
            ///  Préférences - Bases de données ///
            ///////////////////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.DATABASES, true);
                action.setMnemonicKey(KeyEvent.VK_D);
                action.addTo(menu);
            }
            ////////////////////////////////
            ///  Préférences - Apparence ///
            ////////////////////////////////
            if (true) {
                menu.addSeparator();
                menu.add(new LookAndFeelMenu(desktop, resources));
            }
        }
        ///////////
        ///  ?  ///
        ///////////
        if (true) {
            final JMenu menu=new JMenu("?");
            menuBar.add(menu);
            ///////////////////////////
            ///  ? - A propos de... ///
            ///////////////////////////
            if (true) {
                final Action action = new Action(desktop, ResourceKeys.ABOUT, true);
                action.setMnemonicKey(KeyEvent.VK_A);
                action.setIcon("general/About");
                action.addTo(menu);
            }
        }
        ///////////////
        ///  Debug  ///
        ///////////////
        if (true) {
            final String DEBUG="Debug";
            final Action action=new Action(desktop, ResourceKeys.DEBUG, DEBUG);
            desktop.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_MASK), DEBUG);
            desktop.getActionMap().put(DEBUG, action);
            enableWhenFrameFocused.add(action);
        }
        desktop.setActions(enableWhenFrameFocused);
        frame.show();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Démarre l'application du projet PELOPS. Si les connections avec les
     * bases de données échouent, un message d'erreur sera affichée et une
     * boîte de dialogue apparaîtra pour permettre à l'utilisateur d'éditer
     * les paramètres. La connection sera réessayé tant que l'utilisateur
     * ne cliquera pas sur "Annuler".
     *
     * <p>L'application peut être lancé avec les arguments suivants:</p>
     *
     * <ul>
     *   <li><code><strong>-tilecache=</strong><i>m</i></code> définit la quantité maximale de
     *       mémoire (en méga-octets) à alouer à la cache interne de la bibliothèque JAI (Java
     *       Advanced Imaging). La valeur par défaut est 64 méga-octets. Les systèmes qui n'ont
     *       pas 256 mega-octets de mémoire vive devrait définir une valeur plus basse.</li>
     *   <li><code><strong>-native</strong></code> Démarre l'application en utilisant le "Look
     *       and Feel" de la plateforme. Si cet argument est omis, l'application démarrera en
     *       utilisant le "Look and Feel" par défaut de <i>Swing</i>.</li>
     * </ul>
     */
    public static void main(final String[] args) {
        org.geotools.resources.Geotools.init();
        org.geotools.resources.MonolineFormatter.init("fr.ird");
        org.geotools.resources.image.ImageUtilities.allowNativeCodec("png", false, false);
        org.geotools.resources.image.ImageUtilities.allowNativeCodec("png", true , false);
        final Arguments arguments = new Arguments(args);
        if (false) {
            LookAndFeelMenu.initLookAndFeel();
        } else {
            // Effectue le changement de L&F ici pour éviter de charger Swing trop tôt.
            String classname = arguments.getOptionalString("-plaf");
            if (classname == null) {
                final Preferences preferences = Preferences.userNodeForPackage(Main.class);
                classname = preferences.get(LookAndFeelMenu.PREF, null);
            }
            if (classname != null) try {
                UIManager.setLookAndFeel(classname);
            } catch (Exception exception) {
                Utilities.unexpectedException("fr.ird.seasview", "Main", "main", exception);
            }
        }
        /*
         * Interprète les arguments de la ligne de commange.
         * Les arguments non-valides provoqueront un message
         * d'erreur et l'arrêt du programme.
         */
        final String    tilecache = arguments.getOptionalString("-tilecache");
        DataBase.MEDITERRANEAN_VERSION = arguments.getFlag("-Méditerranée");
        arguments.getRemainingArguments(0);
        if (tilecache != null) {
            final long value = Long.parseLong(tilecache)*(1024*1024);
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(value);
            // La capacité par défaut était de 64 Megs.
        }
        /*
         * Modifie la priorité du 'TileScheduler' afin d'éviter de bloquer le thread de Swing.
         * La priorité par défaut était NORM_PRIORITY (c'est-à-dire 5).
         */
        JAI.getDefaultInstance().getTileScheduler().setPriority(Thread.NORM_PRIORITY - 1);
        /*
         * Procède au chargement de la classe du pilote. Cette classe
         * s'enregistrement automatiquement auprès du gestionnaire de
         * pilotes, de sorte qu'on n'a aucune autre opération à faire.
         * Le pilote JDBC:ODBC est fournit en standard avec le JDK 1.3.
         * Il existe aussi avec le JDK 1.2, mais ce dernier n'est pas
         * compatible avec le JDBC 2.0.
         */
        if (false) try {
            Class.forName("org.gjt.mm.mysql.Driver");
            arguments.out.println("Utilise le pilote MySQL");
        } catch (ClassNotFoundException exception1) {
            try {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            } catch (ClassNotFoundException exception2) {
                // Ignore. On espère que l'utilisateur spécifiera
                // un nom de base de données avec un pilote existant.
                arguments.out.println("Pilote JDBC-ODBC introuvable");
            }
        }
        arguments.out.flush();
        /*
         * Lance le programme. Tant que la création de {@link DataBase}
         * échoue à cause d'un problème de connection avec la base de
         * données, fait apparaître une boîte de dialogue et tente de
         * nouveau la connection.
         */
        DataBase.out = arguments.out;
        ControlPanel control = null;
        do try {                     
            setup(new DataBase());
            return;
        } catch (RemoteException exception) {
            ExceptionMonitor.show(null, exception);
            if (control==null) {
                control = new ControlPanel();
            }
        }
        while (control.showDialog(null));
        System.exit(0);
    }
}
