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
 */
package fr.ird.database.gui.swing;

// Interface utilisateur
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;

// Modèles et événements
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// Divers
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.resources.SwingUtilities;

// Seagis
import fr.ird.database.DataBase;
import fr.ird.database.ConfigurationKey;
import fr.ird.database.sql.AbstractDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Editeur d'instructions SQL. Cet objet peut être construit en lui spécifiant en paramètres
 * l'objet {@link DataBase} qui contient les instructions SQL à utiliser. On peut
 * ensuite appeler {@link #addSQL} pour ajouter une instruction SQL à la liste des instructions
 * qui pourront être éditées. Enfin, on peut appeler la méthode {@link #showDialog} pour faire
 * apparaître un éditeur des instructions spécifiées.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/SQLEditor.png"></p>
 * <p>&nbsp;</p>
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
public class SQLEditor extends JPanel {
    /**
     * Liste des clés représentant les instructions SQL éditables.
     */
    private final List<ConfigurationKey> keySQL = new ArrayList<ConfigurationKey>();

    /**
     * Liste des instructions SQL éditées par l'utilisateur.
     */
    private final List<String> userSQL = new ArrayList<String>();
    
    /**
     * Base de données à éditer.
     */
    protected final AbstractDataBase configuration;

    /**
     * Journal dans lequel écrire une notification
     * des requêtes qui ont été changées.
     */
    private final Logger logger;

    /**
     * Composante dans laquelle l'utilisateur pourra éditer les instructions SQL.
     * Avant de changer la requête à éditer, le contenu de ce champ devra être
     * copié dans <code>userSQL.get(index)</code>.
     */
    private final JTextArea valueArea = new JTextArea(5,40);

    /**
     * Modèle pour l'affichage de la liste des noms descriptifs des instructions SQL.
     * Ce modèle s'occupe des transferts entre <code>valueArea</code> et <code>userSQL</code>.
     */
    private final Model model = new Model();

    /**
     * Liste des instructions SQL.
     */
    private final JList sqlList = new JList(model);

    /**
     * Modèle pour l'affichage de la liste des noms descriptifs des instructions SQL.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Model extends AbstractListModel implements ListSelectionListener, ActionListener {
        /**
         * Index de l'instruction sélectionné.
         */
        int index = -1;

        /**
         * Taille qu'avait {@link #userSQL} lors du dernier appel de {@link #update}.
         */
        private int lastSize;

        /**
         * Retourne le nombre d'instructions.
         */
        public int getSize() {
            return keySQL.size();
        }

        /**
         * Retourne l'instruction à l'index spécifié.
         */
        public Object getElementAt(final int index) {
            return keySQL.get(index).description.toString(getLocale());
        }

        /**
         * Sélectionne une nouvelle instruction. Le
         * contenu du champ de texte sera mis à jour.
         */
        public void valueChanged(final ListSelectionEvent event) {
            if (index>=0 && index<userSQL.size()) {
                commit();
            }
            valueTextChanged();
        }

        /**
         * Sauvegarde la requête SQL que l'utilisateur vient de modifier.
         * Cette modification n'est pas encore enregistrées dans les
         * configuration. Cette étape sera faite à la fin par la méthode
         * {@link #save()} si l'utilisateur clique sur "Ok"
         */
        final void commit() {
            String editedText = valueArea.getText();
            if (editedText.trim().length() == 0) {
                editedText = keySQL.get(index).defaultValue;
            }
            userSQL.set(index, editedText);
        }

        /**
         * Affiche dans <code>valueArea</code> l'instruction SQL qui
         * correspond à la sélection courrante de l'utilisateur.
         */
        void valueTextChanged() {
            index = sqlList.getSelectedIndex();
            if (index>=0 && index<userSQL.size()) {
                valueArea.setText(userSQL.get(index));
                valueArea.setEnabled(true);
            } else {
                valueArea.setText(null);
                valueArea.setEnabled(false);
            }
        }

        /**
         * Vérifie si de nouvelles instructions SQL ont été
         * ajoutées à la suite des instruction déjà déclarées.
         */
        protected void update() {
            final int size = userSQL.size();
            if (size > lastSize) {
                fireIntervalAdded(this, lastSize, size-1);
                lastSize = size;
            }
        }
        
        /**
         * Méthode appelée automatiquement lorsque l'utilisateur
         * clique sur le bouton "Rétablir".
         */
        public void actionPerformed(final ActionEvent event) {
            reset();
        }
    }

    /**
     * Construit un éditeur d'instructions SQL.
     *
     * @param configuration Base de données dont on veut éditer la configuration.
     * @param description Note explicative destinée à l'utilisateur.
     * @param logger Journal dans lequel écrire une notification des
     *               requêtes qui ont été changées.
     */
    public SQLEditor(final AbstractDataBase configuration,
                     final String           description,
                     final Logger           logger)
    {
        super(new BorderLayout());
        this.logger = logger;
        this.configuration = configuration;
        if (configuration == null) {
            throw new NullPointerException();
        }
        sqlList.addListSelectionListener(model);
        sqlList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        valueArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        setPreferredSize(new Dimension(720, 320));

        final JScrollPane scrollList  = new JScrollPane(sqlList);
        final JScrollPane scrollValue = new JScrollPane(valueArea);
        final JSplitPane  splitPane   = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scrollList, scrollValue);
        final JComponent  comments    = SwingUtilities.getMultilineLabelFor(scrollList, description);
        comments.setBorder(BorderFactory.createEmptyBorder(/*top*/0, /*left*/0, /*bottom*/18, /*right*/0));
        add(comments,   BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Fait apparaître l'éditeur des instructions SQL. Si l'utilisateur clique sur "Ok",
     * alors les instructions éditées seront sauvegardées par un appel à la méthode
     * {@link #save}.
     *
     * @param  owner Composante par-dessus laquelle faire apparaître la boîte de dialogue.
     * @return <code>true</code> si l'utilisateur à cliqué sur "Ok", ou <code>false</code> sinon.
     */
    public boolean showDialog(final Component owner) {
        if (userSQL.isEmpty()) {
            // Il n'y a rien à afficher.
            return false;
        }
        model.update();
        sqlList.setSelectedIndex(0);

        // TODO: JOptionPane ne fait pas un bon travail concernant la taille des boutons
        //       que l'on ajoute sur la barre des boutons (en plus de "Ok" et "Annuler").
        //       Pour afficher le bouton "Rétablir" malgré ces défauts, ne pas mettre
        //       'model' en commentaire.
        final boolean ok = SwingUtilities.showOptionDialog(owner, this,
                           Resources.format(ResourceKeys.SQL_QUERIES)/*, model*/);
        model.commit();
        if (ok) save();
        return ok;
    }

    /**
     * Ajoute un caractère de changement de ligne ('\n')
     * à la fin de texte spécifié s'il n'y en avait pas
     * déjà un.
     */
    private static String line(String value) {
        if (value == null) {
            return "";
        }
        final int length = value.length();
        if (length != 0) {
            final char c = value.charAt(length-1);
            if (c!='\r' && c!='\n') {
                value += '\n';
            }
        }
        return value;
    }

    /**
     * Ajoute une instruction SQL à la liste des instructions qui pourront être éditées.
     *
     * @param key Clé permetant de retrouver l'instruction SQL actuelle dans l'objet
     *        {@link DataBase}.
     */
     public synchronized void addSQL(final ConfigurationKey key) {
         userSQL.add(line(configuration.getProperty(key)));
         keySQL.add(key);
     }

    /**
     * Enregistre les modifications apportées aux instructions SQL. Cette
     * méthode sera appelée automatiquement lorsque l'utilisateur appuie
     * sur "Ok" dans la boîte de dialogue.
     */
    protected void save() {
        for (int i=userSQL.size(); --i>=0;) {
            final ConfigurationKey key = keySQL.get(i);
            String value = userSQL.get(i);
            if (value != null) {
                value = value.trim();
                if (value.length() == 0) {
                    value = null;
                }
            }
            if (!Utilities.equals(value, configuration.getProperty(key))) {
                final int clé;
                if (Utilities.equals(value, key.defaultValue)) {
                    clé = ResourceKeys.REMOVE_QUERY_$1;
                    value = null;
                } else {
                    clé = ResourceKeys.DEFINE_QUERY_$1;
                }
                configuration.setProperty(key, value);
                if (logger != null) {
                    logger.config(Resources.format(clé, key.name));
                }
            }
        }
    }

    /**
     * Rétablit les requêtes par défaut.
     */
    private void reset() {
        for (int i=userSQL.size(); --i>=0;) {
            userSQL.set(i, line(keySQL.get(i).defaultValue));
        }
        model.valueTextChanged();
    }
}
