/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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
package fr.ird.awt;

// Base de données
import java.sql.SQLException;
import fr.ird.animat.Species;
import fr.ird.sql.fishery.FisheryDataBase;

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

// Evénements
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Géométrie
import java.awt.geom.Ellipse2D;

// Collections
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.resources.SwingUtilities;

// Divers
import java.util.Locale;
import java.util.TimeZone;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Boîte de dialogue proposant à l'utilisateur de sélectionner des espèces
 * animales. Pour chaque espèces, l'utilisateur pourra aussi contrôler des
 * paramètres individuels comme la couleur de représentation.
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
     * Index du paneau servant à sélectionner les espèces à afficher.
     * N'oubliez pas d'ajuster cet index si des éléments sont ajoutés
     * ou supprimés lors de la construction de la boîte de dialogue!!
     */
    private static final int SPECIES_PANE = 0;

    /**
     * Index du paneau servant à choisir les couleurs des captures.
     * N'oubliez pas d'ajuster cet index si des éléments sont ajoutés
     * ou supprimés lors de la construction de la boîte de dialogue!!
     */
    private static final int COLOR_PANE = 1;

    /**
     * Paneau à onglets.
     */
    private final JTabbedPane tabs = new JTabbedPane();

    /**
     * Si ce bouton est sélectionné, l'utilisateur demande à afficher
     * les positions des captures plutôt que les quantités pêchées.
     */
    private final JToggleButton showPositionOnly = new JRadioButton(
            Resources.format(ResourceKeys.SHOW_POSITION_ONLY), true);

    /**
     * Si ce bouton est sélectionné, l'utilisateur demande à afficher
     * les captures par espèces plutôt que seulement les positions de
     * pêches.
     */
    private final JToggleButton showCatchAmount = new JRadioButton(
            Resources.format(ResourceKeys.SHOW_CATCH_AMOUNT));

    /**
     * Liste des espèces sélectionnables.
     */
    private final MutableComboBoxModel speciesIcons = new DefaultComboBoxModel();

    /**
     * Composante affichant la liste des espèces.
     * Cette liste ne devra comprendre que des
     * objets {@link Species.Icon}.
     */
    private final JList list = new JList(speciesIcons);

    /**
     * Boîte de dialogue à utiliser pour permettre à l'utilisateur
     * de choisir les couleurs. Une seule boîte sera utilisée pour
     * toutes les espèces.
     */
    private final MarkColorChooser colorChooser = new MarkColorChooser(true);

    /**
     * Langue selon laquelle écrire le nom des espèces.
     */
    private Locale locale = Locale.getDefault();

    /**
     * Langues dans lesquelles peuvent être exprimées les noms des espèces.
     */
    private final MutableComboBoxModel locales = new DefaultComboBoxModel();

    /**
     * Entrées représentant un objet {@link Locale} et le nom
     * de la langue sous forme de chaîne de caractères.
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
     * Construit une boîte de dialogue par défaut.
     * Elle ne contiendra initalement aucune espèce.
     */
    public SpeciesChooser() {
        super(new BorderLayout());
        final Resources resources = Resources.getResources(null);
        final Listener   listener = new Listener();
        final Renderer   renderer = new Renderer();
        ////////////////////
        ////  Espèces  /////
        ////////////////////
        if (true) {
            final JComboBox   localeBox = new JComboBox(locales);
            final JComponent scrollList = new JScrollPane(list); scrollList.setPreferredSize(new Dimension(80,80));
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
            list.setCellRenderer  (renderer);
            list.addMouseListener (listener);
            list.setEnabled       (false);
            tabs.addTab(resources.getString(ResourceKeys.SPECIES), panel);
        }
        /////////////////////
        ////  Couleurs  /////
        /////////////////////
        if (true) {
            colorChooser.setShape(new Ellipse2D.Float(-24f, -24f, 48f, 48f));
            final JPanel panel=new JPanel(new GridBagLayout());
            final GridBagConstraints c=new GridBagConstraints();
            final JComboBox list=new JComboBox(speciesIcons);
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
     * Construit une boîte de dialogue avec les
     * espèces de la base de données spécifiée.
     */
    public SpeciesChooser(final FisheryDataBase database) throws SQLException {
        this();
        if (database != null) {
            final Collection<Species> sp = database.getSpecies();
            for (final Iterator<Species> it=sp.iterator(); it.hasNext();) {
                add(it.next().getIcon());
            }
        }
        final Locale locale   = Locale.getDefault();
        final String language = locale.getLanguage();
        final int    size     = locales.getSize();
        for (int i=0; i<size; i++) {
            final LocaleEntry entry = (LocaleEntry) locales.getElementAt(i);
            if (entry.locale!=null && language.equals(entry.locale.getLanguage())) {
                locales.setSelectedItem(entry);
                this.locale=locale;
                break;
            }
        }
    }

    /**
     * Fait apparaître un paneau proposant de
     * choisir une couleur pour l'entrée spécifiée.
     */
    private void showControler(final Species.Icon entry) {
        speciesIcons.setSelectedItem(entry);
        colorChooser.setColor(entry.getColor());
        tabs.getModel().setSelectedIndex(COLOR_PANE);
        // TODO: afficher plutôt le dernier contrôleur utilisé (lorsqu'il y en aura d'autres)
    }

    /**
     * Ajoute une espèce au paneau. Si cette espèce peut écrire son nom selon
     * une nouvelle langue qui n'avait pas encore été prise en compte, cette
     * langue sera ajoutée dans la boîte déroulante "Langue".
     */
    public void add(final Species.Icon icon) {
        final Locale[] loc = icon.getSpecies().getLocales();
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
        final int[]                  indices = list.getSelectedIndices();
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
     * Fait apparaître la boîte de dialogue demandant à l'utilisateur
     * de choisir des espèces. Cette méthode retourne <code>true</code>
     * si l'utilisateur a cliqué sur "Ok".
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
        list.setEnabled(showCatch);

        for (int i=colors.length; --i>=0;) {
            ((Species.Icon) speciesIcons.getElementAt(i)).setColor(colors[i]);
        }
        colorChooser.setColor(((Species.Icon) speciesIcons.getSelectedItem()).getColor());
        return false;
    }

    /**
     * Sauvegarde les changements faits dans le paneau spécifié.
     */
    private final void saveChanges(final int paneIndex) {
        final Species.Icon selectedSpecies = (Species.Icon) speciesIcons.getSelectedItem();
        switch (paneIndex) {
            case COLOR_PANE: selectedSpecies.setColor(colorChooser.getColor()); break;
        }
    }

    /**
     * Classe de l'objet chargé de répondre à certains événements.
     */
    private final class Listener extends MouseAdapter implements ItemListener, ChangeListener, ActionListener {
        /**
         * Index du dernier panneau sélectionné.
         */
        private int lastPane;

        /**
         * Méthode appelée automatiquement lorsque l'utilisateur a double-cliqué
         * sur un item de la liste des espèces. Cette méthode fera apparaître la
         * palette de couleur pour l'espèce double-cliqué.
         */
        public void mouseClicked(final MouseEvent event) {
            if (event.getClickCount()==2 && list.isEnabled()) {
                final int index = list.locationToIndex(event.getPoint());
                if (index >= 0) {
                    showControler((Species.Icon) speciesIcons.getElementAt(index));
                }
            }
        }

        /**
         * Méthode appelée automatiquement lorsque l'utilisateur a choisit une
         * autre espèce dans la boîte déroulante au dessus de la palette de couleurs.
         */
        public void itemStateChanged(final ItemEvent event) {
            final Species.Icon icon = (Species.Icon) event.getItem();
            switch (event.getStateChange()) {
                case ItemEvent.DESELECTED: icon.setColor(colorChooser.getColor()); break;
                case ItemEvent.SELECTED:   colorChooser.setColor(icon.getColor()); break;
            }
        }
        
        /**
         * Méthode appelée automatiquement après que l'utilisateur change le paneau
         * visible. Lorsque cette méthode est appelée, le changement a déjà été fait.
         */
        public void stateChanged(final ChangeEvent event) {
            saveChanges(lastPane);
            lastPane = tabs.getSelectedIndex();
        }
        
        /**
         * Méthode appelée automatiquement lorsque l'utilisateur change la sélection
         * "Afficher les positions seulement" / "Afficher les quantités pêchées".
         */
        public void actionPerformed(final ActionEvent event) {
            list.setEnabled(showCatchAmount.isSelected());
        }
    }

    /**
     * Classe de l'objet chargé de dessiner les étiquettes des items.
     */
    private final class Renderer extends DefaultListCellRenderer implements ItemListener {
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus)
        {
            final Species.Icon icon = (Species.Icon) value;
            final Component c=super.getListCellRendererComponent(list, format(icon.getSpecies(), list==SpeciesChooser.this.list),
                                                                 index, isSelected, cellHasFocus);
            setIcon(icon);
            return c;
        }

        /**
         * Méthode appelée automatiquement lorsque l'utilisateur
         * change la langue d'affichage des noms des espèces.
         */
        public void itemStateChanged(final ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final LocaleEntry selected = (LocaleEntry) event.getItem();
                if (selected != null) {
                    locale = selected.locale;
                }
                list.repaint();
            }
        }
    }

    /**
     * Retourne la chaîne de caractères à afficher pour représenter
     * une espèce. Cette chaîne dépendra de la langue sélectionnée
     * par l'utilisateur.
     */
    private String format(final Species species, final boolean isSpeciesList) {
        if (isSpeciesList || locale==Species.LATIN) {
            final String name = species.getName(locale);
            return (name!=null) ? name : species.getName(null);
        }
        final StringBuffer buffer=new StringBuffer("<HTML>");
        String name = species.getName(locale);
        if (name == null) {
            name = species.getName(null);
        }
        buffer.append(name);
        name = species.getName(Species.LATIN);
        if (name!=null && name.length()>=2) { // Evite "?"
            buffer.append("&nbsp;&nbsp; (<i>");
            buffer.append(name);
            buffer.append("</i>)");
        }
        return buffer.toString();
    }
}
