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

// Bases de données
import java.sql.SQLException;

// Images
import javax.media.jai.JAI;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

// Interface utilisateur
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JMenu;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JCheckBoxMenuItem;
import fr.ird.sql.ControlPanel;

// Evénements
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;

// Temps
import java.util.TimeZone;

// Collections
import java.util.List;
import java.util.ArrayList;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Divers
import fr.ird.util.Console;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Point d'entré de l'application SEAS. Cette classe ne contient
 * qu'une seule méthode, {@link #setup}, qui construit la fenêtre
 * de l'application.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Main
{
    /**
     * Do not allows instantiation of this class.
     */
    private Main()
    {}

    /**
     * Construit une fenêtre de l'application SEAS.
     *
     * @param database Connection avec les bases de données.
     */
    public static JFrame setup(final DataBase database)
    {
        final Resources   resources = Resources.getResources(null);
        final JFrame          frame = new JFrame(resources.getString(ResourceKeys.APPLICATION_TITLE));
        final Desktop       desktop = new Desktop(database);
        final JToolBar      toolBar = new JToolBar("SEAS");
        final JMenuBar      menuBar = new JMenuBar();
        final Container contentPane = frame.getContentPane();
        frame.addWindowListener(new WindowAdapter()
        {
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
        final List<Action> enableWhenFrameFocused=new ArrayList<Action>();
        frame.setJMenuBar(menuBar);
        /////////////////
        ///  Fichier  ///
        /////////////////
        if (true)
        {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.FILE));
            menu.setMnemonic(KeyEvent.VK_F);
            menuBar.add(menu);
            //////////////////////////
            ///  Fichier - Nouveau ///
            //////////////////////////
            if (true)
            {
                final JMenu submenu = new JMenu(resources.getString(ResourceKeys.NEW));
                submenu.setMnemonic(KeyEvent.VK_N);
                menu.add(submenu);
                ////////////////////////////////////////////
                ///  Fichier - Nouveau - Séries d'images ///
                ////////////////////////////////////////////
                if (true)
                {
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
                if (false)
                {
                    final Action action = new Action(desktop, ResourceKeys.COUPLING, true);
                    action.setMnemonicKey(KeyEvent.VK_C);
                    action.addTo(submenu);
                    enableWhenFrameFocused.add(action);
                }
            }
            /////////////////////////
            ///  Fichier - Ouvrir ///
            /////////////////////////
            if (true)
            {
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
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.SAVE, false);
                action.setToolTipText(ResourceKeys.SAVE_DESKTOP);
                action.setIcon("general/Save");
                action.addTo(menu, toolBar);
            }
            //////////////////////////////////////
            ///  Fichier - Enregistrer sous... ///
            //////////////////////////////////////
            if (true)
            {
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
            if (false)
            {
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
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.EXIT, false);
                action.addTo(menu);
            }
        }
        /////////////////
        ///  Edition  ///
        /////////////////
        if (true)
        {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.EDIT));
            menu.setMnemonic(KeyEvent.VK_E);
            menuBar.add(menu);
            toolBar.addSeparator();
            //////////////////////////
            ///  Edition - Annuler ///
            //////////////////////////
            if (true)
            {
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
            if (true)
            {
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
            if (true)
            {
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
            if (true)
            {
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
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.SELECT_ALL, false);
                action.setAccelerator(KeyEvent.VK_A, KeyEvent.CTRL_MASK);
                action.setToolTipText(ResourceKeys.SELECT_ALL);
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            ////////////////////////////////////////
            ///  Edition - Inverser la sélection ///
            ////////////////////////////////////////
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.INVERT_SELECTION, false);
                action.setToolTipText(ResourceKeys.INVERT_SELECTION);
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
        }
        ///////////////////
        ///  Affichage  ///
        ///////////////////
        if (true)
        {
            //////////////////////////////////////
            ///  Affichage - Rétablir le zoom  ///
            //////////////////////////////////////
            if (true)
            {
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
        if (true)
        {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.SERIES));
            menu.setMnemonic(KeyEvent.VK_S);
            menuBar.add(menu);
            ///////////////////////////////////
            ///  Séries - Ajouter une série ///
            ///////////////////////////////////
            if (true)
            {
                final Action action=new Action(desktop, ResourceKeys.ADD_SERIES, true);
                action.setMnemonicKey(KeyEvent.VK_A);
                action.setIcon("general/Add");
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            /////////////////////////////////
            ///  Séries - Fermer la série ///
            /////////////////////////////////
            if (true)
            {
                final Action action=new Action(desktop, ResourceKeys.CLOSE_SERIES, true);
                action.setMnemonicKey(KeyEvent.VK_F);
                action.setIcon("general/Remove");
                action.addTo(menu);
                enableWhenFrameFocused.add(action);
            }
            /////////////////////////////////////////
            ///  Séries - Changer les coordonnées ///
            /////////////////////////////////////////
            if (true)
            {
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
            if (true)
            {
                menu.addSeparator();
                final Action action=new Action(desktop, ResourceKeys.IMAGES_CATALOG, true);
                action.setMnemonicKey(KeyEvent.VK_S);
                action.setIcon("general/Information");
                action.addTo(menu);
            }
        }
        /////////////////////
        ///  Préférences  ///
        /////////////////////
        if (true)
        {
            final JMenu menu=new JMenu(resources.getString(ResourceKeys.PREFERENCES));
            menu.setMnemonic(KeyEvent.VK_P);
            menuBar.add(menu);
            ///////////////////////////////////////////
            ///  Préférences - Images synchronisées ///
            ///////////////////////////////////////////
            if (true)
            {
                final JMenuItem item=new JCheckBoxMenuItem(resources.getString(ResourceKeys.SYNCHRONIZED_IMAGES), true);
                item.addActionListener(new ActionListener()
                {
                    public void actionPerformed(final ActionEvent event)
                    {desktop.setImagesSynchronized(item.isSelected());}
                });
                menu.add(item);
            }
            /////////////////////////////////////////
            ///  Préférences - Défilement continu ///
            /////////////////////////////////////////
            if (true)
            {
                final JMenuItem item=new JCheckBoxMenuItem(resources.getString(ResourceKeys.LIVE_SCROLLING));
                item.addActionListener(new ActionListener()
                {
                    public void actionPerformed(final ActionEvent event)
                    {desktop.setPaintingWhileAdjusting(item.isSelected());}
                });
                menu.add(item);
            }
            menu.addSeparator();
            /////////////////////////////////////
            ///  Préférences - Fuseau horaire ///
            /////////////////////////////////////
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.TIMEZONE, true);
                action.setMnemonicKey(KeyEvent.VK_H);
                action.addTo(menu);
            }
            ///////////////////////////////////////
            ///  Préférences - Bases de données ///
            ///////////////////////////////////////
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.DATABASES, true);
                action.setMnemonicKey(KeyEvent.VK_D);
                action.addTo(menu);
            }
        }
        ///////////
        ///  ?  ///
        ///////////
        if (true)
        {
            final JMenu menu=new JMenu("?");
            menuBar.add(menu);
            ///////////////////////////
            ///  ? - A propos de... ///
            ///////////////////////////
            if (true)
            {
                final Action action = new Action(desktop, ResourceKeys.ABOUT, true);
                action.setMnemonicKey(KeyEvent.VK_A);
                action.setIcon("general/About");
                action.addTo(menu);
            }
        }
        ///////////////
        ///  Debug  ///
        ///////////////
        if (true)
        {
            final String DEBUG="Debug";
            final Action action=new Action(desktop, ResourceKeys.DEBUG, DEBUG);
            desktop.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_MASK), DEBUG);
            desktop.getActionMap().put(DEBUG, action);
            enableWhenFrameFocused.add(action);
        }
        desktop.setActions(enableWhenFrameFocused);
        return frame;
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
    public static void main(final String[] args)
    {
        /*
         * Interprète les arguments de la ligne de commange.
         * Les arguments non-valides provoqueront un message
         * d'erreur et l'arrêt du programme.
         */
        try
        {
            final Console console  = new Console(args);
            final String tilecache = console.getParameter("-tilecache");
            final boolean nativeLF = console.hasFlag("-native");
            console.checkRemainingArguments(0);
            if (tilecache!=null)
            {
                final long value = Long.parseLong(tilecache)*(1024*1024);
                JAI.getDefaultInstance().getTileCache().setMemoryCapacity(value);
                // La capacité par défaut était de 64 Megs.
            }
            if (nativeLF)
            {
                try
                {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Exception exception)
                {
                    Utilities.unexpectedException("fr.ird", "Main", "main", exception);
                }
            }
        }
        catch (RuntimeException exception)
        {
            System.out.print(Utilities.getShortClassName(exception));
            final String message = exception.getLocalizedMessage();
            if (message!=null)
            {
                System.out.print(": ");
                System.out.print(message);
            }
            System.out.println();
            return;
        }
        /*
         * Procède au chargement de la classe du pilote. Cette classe
         * s'enregistrement automatiquement auprès du gestionnaire de
         * pilotes, de sorte qu'on n'a aucune autre opération à faire.
         * Le pilote JDBC:ODBC est fournit en standard avec le JDK 1.3.
         * Il existe aussi avec le JDK 1.2, mais ce dernier n'est pas
         * compatible avec le JDBC 2.0.
         */
        if (false) try
        {
            Class.forName("org.gjt.mm.mysql.Driver");
            System.out.println("Utilise le pilote MySQL");
        }
        catch (ClassNotFoundException exception1)
        {
            try
            {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            }
            catch (ClassNotFoundException exception2)
            {
                // Ignore. On espère que l'utilisateur spécifiera
                // un nom de base de données avec un pilote existant.
                System.err.println("Pilote JDBC-ODBC introuvable");
            }
        }
        /*
         * Lance le programme. Tans que la création de {@link DataBase}
         * échoue à cause d'un problème de connection avec la base de
         * données, fait apparaître une boîte de dialogue et tente de
         * nouveau la connection.
         */
        ControlPanel control = null;
        do try
        {
            setup(new DataBase()).show();
            return;
        }
        catch (SQLException exception)
        {
            ExceptionMonitor.show(null, exception);
            if (control==null)
                control = new ControlPanel();
        }
        while (control.showDialog(null));
        System.exit(0);
    }
}
