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
package fr.ird.sql;

// Pr�f�rences
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

// Mod�les et �v�nements
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
 * Editeur d'instructions SQL. Cet objet peut �tre construit en lui sp�cifiant en param�tres
 * l'objet {@link Preferences} qui contient les instructions SQL � utiliser. On peut ensuite
 * appeler {@link #addSQL} pour ajouter une instruction SQL � la liste des instructions qui
 * pourront �tre �dit�es. Enfin, on peut appeler la m�thode {@link #showDialog} pour faire
 * appara�tre un �diteur des instructions sp�cifi�es.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class SQLEditor extends JPanel
{
    /** Index du nom de l'instruction SQL.     */ private static final int NAME    = 0;
    /** Index de l'instruction SQL par d�faut. */ private static final int DEFAULT = 1;
    /** Index de la cl� de l'instruction SQL.  */ private static final int KEY     = 2;
    /** Index de l'instruction SQL actuelle.   */ private static final int VALUE   = 3;

    /**
     * Liste des instructions dont on veut permettre l'�dition.
     * Chaque �l�ment de cette liste est un tableau de 4 cha�nes
     * de caract�res, qui contiennent dans l'ordre:
     *
     * <ul>
     *   <li>Un nom descriptif de l'instruction SQL, dans la langue de l'utilisateur.</li>
     *   <li>L'instruction SQL par d�faut.</li>
     *   <li>La cl� permetant de retrouver l'instruction SQL actuelle dans l'objet {@link Preferences}.</li>
     *   <li>L'instruction SQL actuelle.</li>
     * </ul>
     */
    private final List<String[]> toDisplay=new ArrayList<String[]>();

    /**
     * Pr�f�rences � �diter.
     */
    protected final Preferences preferences;

    /**
     * Journal dans lequel �crire une notification
     * des requ�tes qui ont �t� chang�es.
     */
    private final Logger logger;

    /**
     * Composante dans laquelle l'utilisateur pourra �diter les instructions SQL.
     * Avant de changer la requ�te � �diter, le contenu de ce champ devra �tre
     * copi� dans <code>toDisplay.get(index)[VALUE]</code>.
     */
    private final JTextArea valueArea=new JTextArea(5,40);

    /**
     * Mod�le pour l'affichage de la liste des noms descriptifs des instructions SQL.
     * Ce mod�le s'occupe des transferts entre <code>valueArea</code> et <code>toDisplay</code>.
     */
    private final Model model=new Model();

    /**
     * Liste des instructions SQL.
     */
    private final JList sqlList=new JList(model);

    /**
     * Mod�le pour l'affichage de la liste des
     * noms descriptifs des instructions SQL.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Model extends AbstractListModel implements ListSelectionListener, ActionListener
    {
        /**
         * Index de l'instruction s�lectionn�.
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
         * Retourne l'instruction � l'index sp�cifi�.
         */
        public Object getElementAt(final int index)
        {return toDisplay.get(index)[NAME];}

        /**
         * S�lectionne une nouvelle instruction. Le
         * contenu du champ de texte sera mis � jour.
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
         * Sauvegarde la requ�te SQL que l'utilisateur vient de modifier.
         * Cette modification n'est pas encore enregistr�es dans les
         * preferences. Cette �tape sera faite � la fin par la m�thode
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
         * correspond � la s�lection courrante de l'utilisateur.
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
         * V�rifie si de nouvelles instructions SQL ont �t�
         * ajout�es � la suite des instruction d�j� d�clar�es.
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
         * M�thode appel�e automatiquement lorsque l'utilisateur
         * clique sur le bouton "R�tablir".
         */
        public void actionPerformed(final ActionEvent event)
        {reset();}
    }

    /**
     * Construit un �diteur d'instructions SQL
     * @param preferences Pr�f�rences � �diter.
     * @param description Note explicative destin�e � l'utilisateur.
     * @param logger Journal dans lequel �crire une notification des
     *               requ�tes qui ont �t� chang�es.
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
     * Fait appara�tre l'�diteur des instructions SQL. Si l'utilisateur clique sur "Ok",
     * alors les instructions �dit�es seront sauvegard�es par un appel � la m�thode
     * {@link #save}.
     *
     * @param  owner Composante par-dessus laquelle faire appara�tre la bo�te de dialogue.
     * @return <code>true</code> si l'utilisateur � cliqu� sur "Ok", ou <code>false</code> sinon.
     */
    public boolean showDialog(final Component owner)
    {
        if (toDisplay.isEmpty())
        {
            // Il n'y a rien � afficher.
            return false;
        }
        model.update();
        sqlList.setSelectedIndex(0);

        // TODO: JOptionPane ne fait pas un bon travail concernant la taille des boutons
        //       que l'on ajoute sur la barre des boutons (en plus de "Ok" et "Annuler").
        //       Pour afficher le bouton "R�tablir" malgr� ces d�fauts, ne pas mettre
        //       'model' en commentaire.
        final boolean ok = SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.SQL_QUERIES)/*, model*/);
        model.commit();
        if (ok) save();
        return ok;
    }

    /**
     * Ajoute un caract�re de changement de ligne ('\n')
     * � la fin de texte sp�cifi� s'il n'y en avait pas
     * d�j� un.
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
     * Ajoute une instruction SQL � la liste des instructions qui pourront �tre �dit�es.
     *
     * @param title Nom descriptif de l'instruction SQL, dans la langue de l'utilisateur.
     * @param defaultSQL Instruction SQL par d�faut. L'instruction actuelle sera obtenu � l'aide du
     *        param�tre <code>key</code> et de l'objet {@link Preferences} sp�cifi� au constructeur.
     * @param key Cl� permetant de retrouver l'instruction SQL actuelle dans l'objet {@link Preferences}
     *        sp�cifi� au constructeur.
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
     * Enregistre les modifications apport�es aux instructions SQL. Cette
     * m�thode sera appel�e automatiquement lorsque l'utilisateur appuie
     * sur "Ok" dans la bo�te de dialogue.
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
                    final int cl�;
                    if (value.equals(record[DEFAULT].trim()))
                    {
                        preferences.remove(key);
                        cl� = ResourceKeys.REMOVE_QUERY_$1;
                    }
                    else
                    {
                        preferences.put(key, value);
                        cl� = ResourceKeys.DEFINE_QUERY_$1;
                    }
                    if (logger!=null)
                    {
                        logger.config(Resources.format(cl�, key));
                    }
                }
            }
        }
    }

    /**
     * R�tablit les requ�tes par d�faut.
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
     * Retourne une des propri�t�e par d�faut de la base de donn�es. La cl� <code>name</code>
     * est habituellement une des constantes {@link DataBase#DRIVER}, {@link DataBase#SOURCE}
     * ou {@link DataBase#TIMEZONE}. Cette m�thode retourne <code>null</code> si la propri�t�
     * demand�e n'est pas d�finie.
     */
    public String getProperty(final String name)
    {return preferences.get(name, null);}
}
