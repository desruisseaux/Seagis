/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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

// Mod�les et �v�nements
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
 * Editeur d'instructions SQL. Cet objet peut �tre construit en lui sp�cifiant en param�tres
 * l'objet {@link DataBase} qui contient les instructions SQL � utiliser. On peut
 * ensuite appeler {@link #addSQL} pour ajouter une instruction SQL � la liste des instructions
 * qui pourront �tre �dit�es. Enfin, on peut appeler la m�thode {@link #showDialog} pour faire
 * appara�tre un �diteur des instructions sp�cifi�es.
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
     * Liste des cl�s repr�sentant les instructions SQL �ditables.
     */
    private final List<ConfigurationKey> keySQL = new ArrayList<ConfigurationKey>();

    /**
     * Liste des instructions SQL �dit�es par l'utilisateur.
     */
    private final List<String> userSQL = new ArrayList<String>();
    
    /**
     * Base de donn�es � �diter.
     */
    protected final AbstractDataBase configuration;

    /**
     * Journal dans lequel �crire une notification
     * des requ�tes qui ont �t� chang�es.
     */
    private final Logger logger;

    /**
     * Composante dans laquelle l'utilisateur pourra �diter les instructions SQL.
     * Avant de changer la requ�te � �diter, le contenu de ce champ devra �tre
     * copi� dans <code>userSQL.get(index)</code>.
     */
    private final JTextArea valueArea = new JTextArea(5,40);

    /**
     * Mod�le pour l'affichage de la liste des noms descriptifs des instructions SQL.
     * Ce mod�le s'occupe des transferts entre <code>valueArea</code> et <code>userSQL</code>.
     */
    private final Model model = new Model();

    /**
     * Liste des instructions SQL.
     */
    private final JList sqlList = new JList(model);

    /**
     * Mod�le pour l'affichage de la liste des noms descriptifs des instructions SQL.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Model extends AbstractListModel implements ListSelectionListener, ActionListener {
        /**
         * Index de l'instruction s�lectionn�.
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
         * Retourne l'instruction � l'index sp�cifi�.
         */
        public Object getElementAt(final int index) {
            return keySQL.get(index).description.toString(getLocale());
        }

        /**
         * S�lectionne une nouvelle instruction. Le
         * contenu du champ de texte sera mis � jour.
         */
        public void valueChanged(final ListSelectionEvent event) {
            if (index>=0 && index<userSQL.size()) {
                commit();
            }
            valueTextChanged();
        }

        /**
         * Sauvegarde la requ�te SQL que l'utilisateur vient de modifier.
         * Cette modification n'est pas encore enregistr�es dans les
         * configuration. Cette �tape sera faite � la fin par la m�thode
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
         * correspond � la s�lection courrante de l'utilisateur.
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
         * V�rifie si de nouvelles instructions SQL ont �t�
         * ajout�es � la suite des instruction d�j� d�clar�es.
         */
        protected void update() {
            final int size = userSQL.size();
            if (size > lastSize) {
                fireIntervalAdded(this, lastSize, size-1);
                lastSize = size;
            }
        }
        
        /**
         * M�thode appel�e automatiquement lorsque l'utilisateur
         * clique sur le bouton "R�tablir".
         */
        public void actionPerformed(final ActionEvent event) {
            reset();
        }
    }

    /**
     * Construit un �diteur d'instructions SQL.
     *
     * @param configuration Base de donn�es dont on veut �diter la configuration.
     * @param description Note explicative destin�e � l'utilisateur.
     * @param logger Journal dans lequel �crire une notification des
     *               requ�tes qui ont �t� chang�es.
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
     * Fait appara�tre l'�diteur des instructions SQL. Si l'utilisateur clique sur "Ok",
     * alors les instructions �dit�es seront sauvegard�es par un appel � la m�thode
     * {@link #save}.
     *
     * @param  owner Composante par-dessus laquelle faire appara�tre la bo�te de dialogue.
     * @return <code>true</code> si l'utilisateur � cliqu� sur "Ok", ou <code>false</code> sinon.
     */
    public boolean showDialog(final Component owner) {
        if (userSQL.isEmpty()) {
            // Il n'y a rien � afficher.
            return false;
        }
        model.update();
        sqlList.setSelectedIndex(0);

        // TODO: JOptionPane ne fait pas un bon travail concernant la taille des boutons
        //       que l'on ajoute sur la barre des boutons (en plus de "Ok" et "Annuler").
        //       Pour afficher le bouton "R�tablir" malgr� ces d�fauts, ne pas mettre
        //       'model' en commentaire.
        final boolean ok = SwingUtilities.showOptionDialog(owner, this,
                           Resources.format(ResourceKeys.SQL_QUERIES)/*, model*/);
        model.commit();
        if (ok) save();
        return ok;
    }

    /**
     * Ajoute un caract�re de changement de ligne ('\n')
     * � la fin de texte sp�cifi� s'il n'y en avait pas
     * d�j� un.
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
     * Ajoute une instruction SQL � la liste des instructions qui pourront �tre �dit�es.
     *
     * @param key Cl� permetant de retrouver l'instruction SQL actuelle dans l'objet
     *        {@link DataBase}.
     */
     public synchronized void addSQL(final ConfigurationKey key) {
         userSQL.add(line(configuration.getProperty(key)));
         keySQL.add(key);
     }

    /**
     * Enregistre les modifications apport�es aux instructions SQL. Cette
     * m�thode sera appel�e automatiquement lorsque l'utilisateur appuie
     * sur "Ok" dans la bo�te de dialogue.
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
                final int cl�;
                if (Utilities.equals(value, key.defaultValue)) {
                    cl� = ResourceKeys.REMOVE_QUERY_$1;
                    value = null;
                } else {
                    cl� = ResourceKeys.DEFINE_QUERY_$1;
                }
                configuration.setProperty(key, value);
                if (logger != null) {
                    logger.config(Resources.format(cl�, key.name));
                }
            }
        }
    }

    /**
     * R�tablit les requ�tes par d�faut.
     */
    private void reset() {
        for (int i=userSQL.size(); --i>=0;) {
            userSQL.set(i, line(keySQL.get(i).defaultValue));
        }
        model.valueTextChanged();
    }
}
