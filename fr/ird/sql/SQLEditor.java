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
package fr.ird.sql;

// Préférences
import java.util.prefs.Preferences;

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
import net.seas.util.SwingUtilities;

// Modèles et événements
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// Journal
import java.util.logging.Logger;

// Divers
import java.util.List;
import java.util.ArrayList;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Editeur d'instructions SQL. Cet objet peut être construit en lui spécifiant en paramètres
 * l'objet {@link Preferences} qui contient les instructions SQL à utiliser. On peut ensuite
 * appeler {@link #addSQL} pour ajouter une instruction SQL à la liste des instructions qui
 * pourront être éditées. Enfin, on peut appeler la méthode {@link #showDialog} pour faire
 * apparaître un éditeur des instructions spécifiées.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class SQLEditor extends JPanel
{
    /** Index du nom de l'instruction SQL.     */ private static final int NAME    = 0;
    /** Index de l'instruction SQL par défaut. */ private static final int DEFAULT = 1;
    /** Index de la clé de l'instruction SQL.  */ private static final int KEY     = 2;
    /** Index de l'instruction SQL actuelle.   */ private static final int VALUE   = 3;

    /**
     * Liste des instructions dont on veut permettre l'édition.
     * Chaque élément de cette liste est un tableau de 4 chaînes
     * de caractères, qui contiennent dans l'ordre:
     *
     * <ul>
     *   <li>Un nom descriptif de l'instruction SQL, dans la langue de l'utilisateur.</li>
     *   <li>L'instruction SQL par défaut.</li>
     *   <li>La clé permetant de retrouver l'instruction SQL actuelle dans l'objet {@link Preferences}.</li>
     *   <li>L'instruction SQL actuelle.</li>
     * </ul>
     */
    private final List<String[]> toDisplay=new ArrayList<String[]>();

    /**
     * Préférences à éditer.
     */
    protected final Preferences preferences;

    /**
     * Journal dans lequel écrire une notification
     * des requêtes qui ont été changées.
     */
    private final Logger logger;

    /**
     * Composante dans laquelle l'utilisateur pourra éditer les instructions SQL.
     * Avant de changer la requête à éditer, le contenu de ce champ devra être
     * copié dans <code>toDisplay.get(index)[VALUE]</code>.
     */
    private final JTextArea valueArea=new JTextArea(5,40);

    /**
     * Modèle pour l'affichage de la liste des noms descriptifs des instructions SQL.
     * Ce modèle s'occupe des transferts entre <code>valueArea</code> et <code>toDisplay</code>.
     */
    private final Model model=new Model();

    /**
     * Liste des instructions SQL.
     */
    private final JList sqlList=new JList(model);

    /**
     * Modèle pour l'affichage de la liste des
     * noms descriptifs des instructions SQL.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Model extends AbstractListModel implements ListSelectionListener, ActionListener
    {
        /**
         * Index de l'instruction sélectionné.
         */
        int index=-1;

        /**
         * Taille qu'avait {@link #toDisplay} lors
         * de dernier appel de {@link #update}.
         */
        private int lastSize;

        /**
         * Retourne le nombre d'instructions.
         */
        public int getSize()
        {return toDisplay.size();}

        /**
         * Retourne l'instruction à l'index spécifié.
         */
        public Object getElementAt(final int index)
        {return toDisplay.get(index)[NAME];}

        /**
         * Sélectionne une nouvelle instruction. Le
         * contenu du champ de texte sera mis à jour.
         */
        public void valueChanged(final ListSelectionEvent event)
        {
            if (index>=0 && index<toDisplay.size())
            {
                commit();
            }
            valueTextChanged();
        }

        /**
         * Sauvegarde la requête SQL que l'utilisateur vient de modifier.
         * Cette modification n'est pas encore enregistrées dans les
         * preferences. Cette étape sera faite à la fin par la méthode
         * {@link #save()} si l'utilisateur clique sur "Ok"
         */
        final void commit()
        {
            final String[] record=toDisplay.get(index);
            String editedText = valueArea.getText();
            if (editedText.trim().length()==0)
                editedText = record[DEFAULT];
            record[VALUE] = editedText;
        }

        /**
         * Affiche dans <code>valueArea</code> l'instruction SQL qui
         * correspond à la sélection courrante de l'utilisateur.
         */
        void valueTextChanged()
        {
            index=sqlList.getSelectedIndex();
            if (index>=0 && index<toDisplay.size())
            {
                final String[] record=toDisplay.get(index);
                valueArea.setText(record[VALUE]);
                valueArea.setEnabled(true);
            }
            else
            {
                valueArea.setText(null);
                valueArea.setEnabled(false);
            }
        }

        /**
         * Vérifie si de nouvelles instructions SQL ont été
         * ajoutées à la suite des instruction déjà déclarées.
         */
        protected void update()
        {
            final int size=toDisplay.size();
            if (size>lastSize)
            {
                fireIntervalAdded(this, lastSize, size-1);
                lastSize=size;
            }
        }
        
        /**
         * Méthode appelée automatiquement lorsque l'utilisateur
         * clique sur le bouton "Rétablir".
         */
        public void actionPerformed(final ActionEvent event)
        {reset();}
    }

    /**
     * Construit un éditeur d'instructions SQL
     * @param preferences Préférences à éditer.
     * @param description Note explicative destinée à l'utilisateur.
     * @param logger Journal dans lequel écrire une notification des
     *               requêtes qui ont été changées.
     */
    public SQLEditor(final Preferences preferences, final String description, final Logger logger)
    {
        super(new BorderLayout());
        this.logger=logger;
        this.preferences=preferences;
        if (preferences==null) throw new NullPointerException();

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
    public boolean showDialog(final Component owner)
    {
        if (toDisplay.isEmpty())
        {
            // Il n'y a rien à afficher.
            return false;
        }
        model.update();
        sqlList.setSelectedIndex(0);

        // TODO: JOptionPane ne fait pas un bon travail concernant la taille des boutons
        //       que l'on ajoute sur la barre des boutons (en plus de "Ok" et "Annuler").
        //       Pour afficher le bouton "Rétablir" malgré ces défauts, ne pas mettre
        //       'model' en commentaire.
        final boolean ok = SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.SQL_QUERIES)/*, model*/);
        model.commit();
        if (ok) save();
        return ok;
    }

    /**
     * Ajoute un caractère de changement de ligne ('\n')
     * à la fin de texte spécifié s'il n'y en avait pas
     * déjà un.
     */
    private static String line(String value)
    {
        final int length=value.length();
        if (length!=0)
        {
            final char c = value.charAt(length-1);
            if (c!='\r' && c!='\n') value+='\n';
        }
        return value;
    }

    /**
     * Ajoute une instruction SQL à la liste des instructions qui pourront être éditées.
     *
     * @param title Nom descriptif de l'instruction SQL, dans la langue de l'utilisateur.
     * @param defaultSQL Instruction SQL par défaut. L'instruction actuelle sera obtenu à l'aide du
     *        paramètre <code>key</code> et de l'objet {@link Preferences} spécifié au constructeur.
     * @param key Clé permetant de retrouver l'instruction SQL actuelle dans l'objet {@link Preferences}
     *        spécifié au constructeur.
     */
    public void addSQL(final String title, final String defaultSQL, final String key)
    {
        synchronized (toDisplay)
        {
            final String[] record = new String[4];
            record[NAME   ] = title;
            record[DEFAULT] = defaultSQL;
            record[KEY    ] = key;
            record[VALUE  ] = line(preferences.get(key, defaultSQL));
            toDisplay.add(record);
        }
    }

    /**
     * Enregistre les modifications apportées aux instructions SQL. Cette
     * méthode sera appelée automatiquement lorsque l'utilisateur appuie
     * sur "Ok" dans la boîte de dialogue.
     */
    protected void save()
    {
        synchronized (toDisplay)
        {
            for (int i=toDisplay.size(); --i>=0;)
            {
                final String[] record = toDisplay.get(i);
                final String      key = record[KEY];
                final String    value = record[VALUE].trim();
                if (!value.equals(preferences.get(key, record[DEFAULT]).trim()))
                {
                    final int clé;
                    if (value.equals(record[DEFAULT].trim()))
                    {
                        preferences.remove(key);
                        clé = ResourceKeys.REMOVE_QUERY_$1;
                    }
                    else
                    {
                        preferences.put(key, value);
                        clé = ResourceKeys.DEFINE_QUERY_$1;
                    }
                    if (logger!=null)
                    {
                        logger.config(Resources.format(clé, key));
                    }
                }
            }
        }
    }

    /**
     * Rétablit les requêtes par défaut.
     */
    private void reset()
    {
        synchronized (toDisplay)
        {
            for (int i=toDisplay.size(); --i>=0;)
            {
                final String[] record=toDisplay.get(i);
                record[VALUE] = line(record[DEFAULT]);
            }
            model.valueTextChanged();
        }
    }

    /**
     * Retourne une des propriétée par défaut de la base de données. La clé <code>name</code>
     * est habituellement une des constantes {@link DataBase#DRIVER}, {@link DataBase#SOURCE}
     * ou {@link DataBase#TIMEZONE}. Cette méthode retourne <code>null</code> si la propriété
     * demandée n'est pas définie.
     */
    public String getProperty(final String name)
    {return preferences.get(name, null);}
}
