/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le D�veloppement
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
package fr.ird.awt;

// Interface utilisateur
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

// Interface utilisateur Swing
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.MutableComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;

// Ev�nements
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Collections
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

// Divers
import java.util.Locale;
import java.util.TimeZone;
import java.sql.SQLException;
import java.awt.geom.Ellipse2D;
import java.rmi.RemoteException;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.resources.SwingUtilities;

// Seagis
import fr.ird.animat.Species;
import fr.ird.database.ServerException;
import fr.ird.database.sample.SampleDataBase;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Bo�te de dialogue proposant � l'utilisateur de s�lectionner des esp�ces
 * animales. Pour chaque esp�ces, l'utilisateur pourra aussi contr�ler des
 * param�tres individuels comme la couleur de repr�sentation.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/SpeciesChooser.png"></p>
 * <p>&nbsp;</p>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class SpeciesChooser extends JPanel {
    /**
     * Index du paneau servant � s�lectionner les esp�ces � afficher.
     * N'oubliez pas d'ajuster cet index si des �l�ments sont ajout�s
     * ou supprim�s lors de la construction de la bo�te de dialogue!!
     */
    private static final int SPECIES_PANE = 0;

    /**
     * Index du paneau servant � choisir les couleurs des captures.
     * N'oubliez pas d'ajuster cet index si des �l�ments sont ajout�s
     * ou supprim�s lors de la construction de la bo�te de dialogue!!
     */
    private static final int COLOR_PANE = 1;

    /**
     * Paneau � onglets.
     */
    private final JTabbedPane tabs = new JTabbedPane();

    /**
     * Si ce bouton est s�lectionn�, l'utilisateur demande � afficher
     * les positions des captures plut�t que les quantit�s p�ch�es.
     */
    private final JToggleButton showPositionOnly = new JRadioButton(
            Resources.format(ResourceKeys.SHOW_POSITION_ONLY), true);

    /**
     * Si ce bouton est s�lectionn�, l'utilisateur demande � afficher
     * les captures par esp�ces plut�t que seulement les positions de
     * p�ches.
     */
    private final JToggleButton showCatchAmount = new JRadioButton(
            Resources.format(ResourceKeys.SHOW_CATCH_AMOUNT));

    /**
     * Liste des esp�ces s�lectionnables.
     */
    private final MutableComboBoxModel speciesIcons = new DefaultComboBoxModel();

    /**
     * Composante affichant la liste des esp�ces. Cette liste ne devra comprendre
     * que des objets {@link Species.Icon}.
     */
    private final JList speciesList = new JList(speciesIcons);

    /**
     * Nom des esp�ces � afficher dans la liste. Le contenu de ce dictionnaire d�pend de la
     * langue s�lectionn�e. Il sera effac� et reconstruit lorsque la langue d'affichange change.
     */
    private final Map<Species,String> speciesNames = new HashMap<Species,String>();

    /**
     * Bo�te de dialogue � utiliser pour permettre � l'utilisateur
     * de choisir les couleurs. Une seule bo�te sera utilis�e pour
     * toutes les esp�ces.
     */
    private final MarkColorChooser colorChooser = new MarkColorChooser(true);

    /**
     * Langue selon laquelle �crire le nom des esp�ces.
     */
    private Locale locale = Locale.getDefault();

    /**
     * Langues dans lesquelles peuvent �tre exprim�es les noms des esp�ces.
     */
    private final MutableComboBoxModel locales = new DefaultComboBoxModel();

    /**
     * Entr�es repr�sentant un objet {@link Locale} et le nom
     * de la langue sous forme de cha�ne de caract�res.
     */
    private static final class LocaleEntry {
        public final Locale locale;
        public final String name;

        public LocaleEntry(final Locale locale, final String name) {
            this.locale=locale;
            this.name=name;
        }

        public String  toString() {
            return name;
        }
    }

    /**
     * Construit une bo�te de dialogue par d�faut.
     * Elle ne contiendra initalement aucune esp�ce.
     */
    public SpeciesChooser() {
        super(new BorderLayout());
        final Resources resources = Resources.getResources(null);
        final Listener   listener = new Listener();
        final Renderer   renderer = new Renderer();
        ////////////////////
        ////  Esp�ces  /////
        ////////////////////
        if (true) {
            final JComboBox   localeBox = new JComboBox(locales);
            final JComponent scrollList = new JScrollPane(speciesList); scrollList.setPreferredSize(new Dimension(80,80));
            final ButtonGroup     group = new ButtonGroup();
            final JPanel          panel = new JPanel(new GridBagLayout());
            final GridBagConstraints  c = new GridBagConstraints();
            c.insets.right=9;
            c.insets.left=15;
            c.gridwidth=2;
            c.gridx=0; c.fill=c.HORIZONTAL; c.weightx=1;
            c.gridy=0; c.insets.top=9; panel.add(showPositionOnly, c);
            c.gridy=1; c.insets.top=0; panel.add(showCatchAmount,  c);
            c.gridy=2; c.insets.bottom=3; c.insets.left+=15; c.weighty=1; c.fill=c.BOTH; panel.add(scrollList, c);

            c.gridy=3; c.insets.bottom=9; c.fill=c.NONE;
            c.gridwidth=1; c.weighty=0; c.anchor=c.EAST; panel.add(new JLabel(resources.getLabel(ResourceKeys.LANGUAGE)), c);
            c.gridx=1;     c.weightx=0; c.insets.left=0; panel.add(localeBox, c);
            if (locales.getSize() != 0) {
                locale = ((LocaleEntry) locales.getElementAt(0)).locale;
            }
            /*
             * Enregistre les 'Renderers' et les 'Listeners'.
             */
            group.add(showPositionOnly);
            group.add(showCatchAmount);
            showPositionOnly.addActionListener(listener);
            showCatchAmount .addActionListener(listener);
            localeBox.addItemListener(renderer);
            speciesList.setCellRenderer  (renderer);
            speciesList.addMouseListener (listener);
            speciesList.setEnabled       (false);
            tabs.addTab(resources.getString(ResourceKeys.SPECIES), panel);
        }
        /////////////////////
        ////  Couleurs  /////
        /////////////////////
        if (true) {
            colorChooser.setShape(new Ellipse2D.Float(-24f, -24f, 48f, 48f));
            final JPanel panel=new JPanel(new GridBagLayout());
            final GridBagConstraints c=new GridBagConstraints();
            final JComboBox list = new JComboBox(speciesIcons);
            c.gridx=0; c.fill=c.HORIZONTAL;
            c.gridy=0; c.weightx=1;                panel.add(list,         c);
            c.gridy=1; c.weighty=1; c.fill=c.BOTH; panel.add(colorChooser, c);
            tabs.addTab(Resources.format(ResourceKeys.COLOR), panel);
            list.addItemListener(listener);
            list.setRenderer    (renderer);
        }
        tabs.addChangeListener(listener);
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Construit une bo�te de dialogue avec les esp�ces de la base de donn�es sp�cifi�e.
     */
    public SpeciesChooser(final SampleDataBase database) throws SQLException {
        this();
        if (database != null) try {
            final Collection<Species> sp = database.getSpecies();
            for (final Species spi : sp) {
                add(spi.getIcon());
            }
        } catch (RemoteException exception) {
            // TODO: localize
            throw new ServerException("L'obtention de l'ic�ne d'une esp�ce a �chou�e.", exception);
        }
        final Locale locale   = Locale.getDefault();
        final String language = locale.getLanguage();
        final int    size     = locales.getSize();
        for (int i=0; i<size; i++) {
            final LocaleEntry entry = (LocaleEntry) locales.getElementAt(i);
            if (entry.locale!=null && language.equals(entry.locale.getLanguage())) {
                locales.setSelectedItem(entry);
                this.locale = locale;
                break;
            }
        }
    }

    /**
     * Fait appara�tre un paneau proposant de choisir une couleur pour l'entr�e sp�cifi�e.
     */
    private void showControler(final Species.Icon entry) {
        speciesIcons.setSelectedItem(entry);
        colorChooser.setColor(entry.getColor());
        tabs.getModel().setSelectedIndex(COLOR_PANE);
        // TODO: afficher plut�t le dernier contr�leur utilis� (lorsqu'il y en aura d'autres)
    }

    /**
     * Ajoute une esp�ce au paneau. Si cette esp�ce peut �crire son nom selon
     * une nouvelle langue qui n'avait pas encore �t� prise en compte, cette
     * langue sera ajout�e dans la bo�te d�roulante "Langue".
     */
    public void add(final Species.Icon icon) {
        Locale[] loc;
        try {
            loc = icon.getSpecies().getLocales();
        } catch (RemoteException exception) {
            Utilities.unexpectedException("fr.ird.awt", "SpeciesChooser", "add", exception);
            loc = new Locale[0];
        }
        StringBuffer buffer = null;
 check: for (int i=0; i<loc.length; i++) {
            final Locale locale = loc[i];
            for (int j=locales.getSize(); --j>=0;) {
                if (Utilities.equals(((LocaleEntry) locales.getElementAt(j)).locale, locale)) {
                    continue check;
                }
            }
            final String name;
            if (locale!=null && locale!=Species.FAO) {
                if (buffer==null) {
                    buffer = new StringBuffer();
                }
                buffer.setLength(0);
                buffer.append(locale.getDisplayLanguage());
                if (buffer.length() == 0) {
                    continue check;
                }
                buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));
                name = buffer.toString();
            } else {
                name = Resources.format(ResourceKeys.FAO_CODE);
            }
            locales.addElement(new LocaleEntry(locale, name));
        }
        speciesIcons.addElement(icon);
    }

    /**
     * Set a new icon for a species. This method look for all occurences
     * of an icon for species <code>newIcon.getSpecies()</code>. Old icons
     * are removed, and the new icon is inserted at the position of the
     * first occurence.
     *
     * @param  newIcon the new icon.
     * @return <code>true</code> is an icon has been replaced, or <code>false</code>
     *         if no occurence of <code>newIcon.getSpecies()</code> has been found.
     *         In the later case, the new icon will not have been added to the list
     *         (you can use {@link #add} instead for that).
     */
    public boolean replace(final Species.Icon newIcon) {
        int insertAt = -1;
        final Species search = newIcon.getSpecies();
        for (int i=speciesIcons.getSize(); --i>=0;) {
            if (search.equals(((Species.Icon) speciesIcons.getElementAt(i)).getSpecies())) {
                speciesIcons.removeElementAt(i);
                insertAt = i;
            }
        }
        if (insertAt >= 0) {
            speciesIcons.insertElementAt(newIcon, insertAt);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns all icons displayed in this <code>SpeciesChooser</code>.
     */
    public Species.Icon[] getIcons() {
        final Species.Icon[] list = new Species.Icon[speciesIcons.getSize()];
        for (int i=list.length; --i>=0;) {
            list[i] = (Species.Icon) speciesIcons.getElementAt(i);
        }
        return list;
    }

    /**
     * Returns all icons currently selected in this <code>SpeciesChooser</code>.
     */
    public Species.Icon[] getSelectedIcons() {
        final int[]                  indices = speciesList.getSelectedIndices();
        final Species.Icon[] selectedSpecies = new Species.Icon[indices.length];
        for (int i=0; i<selectedSpecies.length; i++) {
            selectedSpecies[i] = (Species.Icon) speciesIcons.getElementAt(indices[i]);
        }
        return selectedSpecies;
    }

    /**
     * Returns <code>true</code> if the user request catch amounts,
     * or <code>false</code> if he request positions only.
     */
    public boolean isCatchAmountSelected() {
        return showCatchAmount.isSelected();
    }

    /**
     * Fait appara�tre la bo�te de dialogue demandant � l'utilisateur
     * de choisir des esp�ces. Cette m�thode retourne <code>true</code>
     * si l'utilisateur a cliqu� sur "Ok".
     */
    public boolean showDialog(final Component owner) {
        final boolean showCatch = showCatchAmount.isSelected();
        final Color[] colors = new Color[speciesIcons.getSize()];
        for (int i=colors.length; --i>=0;) {
            colors[i] = ((Species.Icon) speciesIcons.getElementAt(i)).getColor();
        }
        if (SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.SPECIES_SELECTION))) {
            saveChanges(tabs.getSelectedIndex());
            return true;
        }

        if (showCatch) showCatchAmount.setSelected(true);
        else          showPositionOnly.setSelected(true);
        speciesList.setEnabled(showCatch);

        for (int i=colors.length; --i>=0;) {
            ((Species.Icon) speciesIcons.getElementAt(i)).setColor(colors[i]);
        }
        colorChooser.setColor(((Species.Icon) speciesIcons.getSelectedItem()).getColor());
        return false;
    }

    /**
     * Sauvegarde les changements faits dans le paneau sp�cifi�.
     */
    private final void saveChanges(final int paneIndex) {
        final Species.Icon selectedSpecies = (Species.Icon) speciesIcons.getSelectedItem();
        switch (paneIndex) {
            case COLOR_PANE: selectedSpecies.setColor(colorChooser.getColor()); break;
        }
    }

    /**
     * Classe de l'objet charg� de r�pondre � certains �v�nements.
     */
    private final class Listener extends MouseAdapter implements ItemListener, ChangeListener, ActionListener {
        /**
         * Index du dernier panneau s�lectionn�.
         */
        private int lastPane;

        /**
         * M�thode appel�e automatiquement lorsque l'utilisateur a double-cliqu�
         * sur un item de la liste des esp�ces. Cette m�thode fera appara�tre la
         * palette de couleur pour l'esp�ce double-cliqu�.
         */
        public void mouseClicked(final MouseEvent event) {
            if (event.getClickCount()==2 && speciesList.isEnabled()) {
                final int index = speciesList.locationToIndex(event.getPoint());
                if (index >= 0) {
                    showControler((Species.Icon) speciesIcons.getElementAt(index));
                }
            }
        }

        /**
         * M�thode appel�e automatiquement lorsque l'utilisateur a choisit une
         * autre esp�ce dans la bo�te d�roulante au dessus de la palette de couleurs.
         */
        public void itemStateChanged(final ItemEvent event) {
            final Species.Icon icon = (Species.Icon) event.getItem();
            switch (event.getStateChange()) {
                case ItemEvent.DESELECTED: icon.setColor(colorChooser.getColor()); break;
                case ItemEvent.SELECTED:   colorChooser.setColor(icon.getColor()); break;
            }
        }
        
        /**
         * M�thode appel�e automatiquement apr�s que l'utilisateur change le paneau
         * visible. Lorsque cette m�thode est appel�e, le changement a d�j� �t� fait.
         */
        public void stateChanged(final ChangeEvent event) {
            saveChanges(lastPane);
            lastPane = tabs.getSelectedIndex();
        }
        
        /**
         * M�thode appel�e automatiquement lorsque l'utilisateur change la s�lection
         * "Afficher les positions seulement" / "Afficher les quantit�s p�ch�es".
         */
        public void actionPerformed(final ActionEvent event) {
            speciesList.setEnabled(showCatchAmount.isSelected());
        }
    }

    /**
     * Classe de l'objet charg� de dessiner les �tiquettes des items.
     */
    private final class Renderer extends DefaultListCellRenderer implements ItemListener {
        /**
         * Retourne l'objet � utiliser pour dessiner un nom d'esp�ce.
         */
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus)
        {
            final Species.Icon icon = (Species.Icon) value;
            final Component c = super.getListCellRendererComponent(
                                list, format(icon.getSpecies(), list==speciesList),
                                index, isSelected, cellHasFocus);
            setIcon(icon);
            return c;
        }

        /**
         * M�thode appel�e automatiquement lorsque l'utilisateur
         * change la langue d'affichage des noms des esp�ces.
         */
        public void itemStateChanged(final ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final LocaleEntry selected = (LocaleEntry) event.getItem();
                if (selected != null) {
                    locale = selected.locale;
                }
                speciesNames.clear();
                speciesList.repaint();
            }
        }
    }

    /**
     * Retourne la cha�ne de caract�res � afficher pour repr�senter
     * une esp�ce. Cette cha�ne d�pendra de la langue s�lectionn�e
     * par l'utilisateur.
     *
     * @task TODO: cache the name in {@link #speciesName}.
     */
    private String format(final Species species, final boolean isSpeciesList) {
        try {
            if (isSpeciesList || locale==Species.LATIN) {
                final String name = species.getName(locale);
                return (name!=null) ? name : species.getName(null);
            }
            final StringBuffer buffer=new StringBuffer("<HTML>");
            String name = species.getName(locale);
            if (name == null) {
                name = species.getName(null);
            }
            final int length = name.length();
            for (int i=0; i<length; i++) {
                final char c = name.charAt(i);
                switch (c) {
                    case '<': buffer.append("&lt;");    break;
                    case '>': buffer.append("&gt;");    break;
                    case '&': buffer.append("&amp;");   break;
                    default : buffer.append(c);         break;
                }
            }
            name = species.getName(Species.LATIN);
            if (name!=null && name.length()>=2) { // Evite "?"
                buffer.append("&nbsp;&nbsp; (<i>");
                buffer.append(name);
                buffer.append("</i>)");
            }
            buffer.append("</HTML>");
            return buffer.toString();
        } catch (RemoteException exception) {
            return "<HTML><strong>Erreur:</strong> "+exception.getLocalizedMessage()+"</HTML>";
        }
    }
}
