/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.awt;

// Temps et utilitaires
import java.util.TimeZone;
import java.util.Arrays;

// Interface utilisateur
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ListSelectionModel;

// Evénements
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.beans.PropertyChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// Divers
import net.seas.util.SwingUtilities;
import net.seas.resources.Resources;


/**
 * A dialog box for choosing a time zone. If the parent container is a
 * {@link javax.swing.JDesktopPane}, then the dialog box will appear as
 * an internal frame. Otherwise, it will appear as a regular dialog box.
 * <br><br>
 * Every click on a time zone fires a "TimeZone" property change event.
 * Classes can listen to this event by registering themselves:
 * <blockquote><pre>
 * timezoneChooser.{@link JComponent#addPropertyChangeListener(String,PropertyChangeListener) addPropertyChangeListener}("TimeZone", listener);
 * </pre></blockquote>
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="{@docRoot}/doc-files/images/widget/TimeZoneChooser.png"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TimeZoneChooser extends JPanel
{
    /**
     * Liste des fuseaux horaires.
     */
    private final JList list;

    /**
     * Selected timezone, or <code>null</code>.
     */
    private TimeZone selected;

    /**
     * Construit un objet <code>TimeZoneChooser</code> qui
     * proposera un choix parmi tous les fuseaux horaires
     * disponibles.
     *
     * @param message Message à placer en haut de la liste.
     */
    public TimeZoneChooser(final String message)
    {this(message, TimeZone.getAvailableIDs());}

    /**
     * Construit un objet <code>TimeZoneChooser</code> qui
     * proposera un choix parmi tous les fuseaux horaires
     * qui représentent le décalage spécifié.
     *
     * @param message Message à placer en haut de la liste.
     * @param rawOffset Décalage (en milliseconde) par rapport à
     *        l'heure UTC, sans correction pour l'heure solaire.
     *        Il s'agit du décalage à ajouter à l'heure UTC pour
     *        obtenir l'heure locale.
     */
    public TimeZoneChooser(final String message, final int rawOffset)
    {this(message, TimeZone.getAvailableIDs(rawOffset));}

    /**
     * Construit un objet <code>TimeZoneChooser</code> qui
     * proposera un choix parmi les fuseaux horaire spécifiés.
     */
    private TimeZoneChooser(final String message, final String[] IDs)
    {
        super(new BorderLayout());
        Arrays.sort(IDs);
        list=new JList(IDs);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(list), BorderLayout.CENTER);
        setTimeZone(TimeZone.getDefault());
        addComponentListener(new ComponentAdapter()
        {
            /*
             * "Workaround" pour forcer l'apparition du fuseau horaire
             * sélectionné dans la fenêtre. 'ensureIndexIsVisible(int)'
             * ne fonctionne pas lorsque la fenêtre n'est pas visible.
             */
            public void componentResized(final ComponentEvent event)
            {
                final int index=list.getSelectedIndex();
                if (index>=0) list.ensureIndexIsVisible(index);
            }
        });
        list.addListSelectionListener(new ListSelectionListener()
        {
            /*
             * Prévient cette classe chaque fois que l'utilisateur
             * a sélectionné un nouveau fuseau horaire.
             */
            public void valueChanged(final ListSelectionEvent event)
            {
                if (!event.getValueIsAdjusting())
                {
                    final TimeZone old=selected;
                    final Object value=list.getSelectedValue();
                    selected = value!=null ? TimeZone.getTimeZone(value.toString()) : null;
                    firePropertyChange("TimeZone", old, selected);
                }
            }
        });
        final JComponent label=SwingUtilities.getMultilineLabelFor(list, message);
        label.setBorder(BorderFactory.createEmptyBorder(/*top*/0, /*left*/0, /*bottom*/6, /*right*/0));
        add(label, BorderLayout.NORTH);
        setPreferredSize(new Dimension(200,200));
    }

    /**
     * Sélectionne le fuseau horaire spécifié.
     */
    public void setTimeZone(final TimeZone timezone)
    {list.setSelectedValue((timezone!=null) ? timezone.getID() : null, true);}

    /**
     * Retourne le fuseau horaire présentement sélectionné,
     * ou <code>null</code> si aucun fuseau horaire n'a été
     * sélectionné.
     */
    public TimeZone getTimeZone()
    {
        final Object value=list.getSelectedValue();
        return (value!=null) ? TimeZone.getTimeZone(value.toString()) : null;
    }

    /**
     * Fait apparaître ce panneau dans une boîte de dialogue avec les boutons
     * "ok" et "annuler" et attend la sélection de l'utilisateur.
     *
     * @param  owner Composante parente.
     * @return Le fuseau horaire sélectionné si l'utilisateur a cliqué sur "ok",
     *         ou <code>null</code> s'il a cliqué sur "annuler".
     */
    public TimeZone showDialog(final Component owner)
    {
        if (SwingUtilities.showOptionDialog(owner, this, Resources.format(Clé.TIME_ZONE)))
        {
            return getTimeZone();
        }
        else return null;
    }
}
