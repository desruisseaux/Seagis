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
package fr.ird.seasview.layer.control;

// Bases de données et images
import java.io.IOException;
import java.sql.SQLException;
import fr.ird.sql.image.ImageEntry;

// Interface utilisateur
import fr.ird.map.Layer;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.AbstractButton;

// Evénements
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import javax.swing.event.EventListenerList;

// Divers
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;

// Resources
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Classe de base des objets capable de construire et/ou configurer
 * une couche {@link Layer} pour une image {@link ImageEntry} donnée.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class LayerControl
{
    /**
     * Case à cocher pour sélectionner cette couche.
     */
    private final JToggleButton selector;

    /**
     * Bouton à appuyer pour configurer une couche.
     */
    private final AbstractButton trigger;

    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cette couche.
     */
    private final EventListenerList listenerList=new EventListenerList();

    /**
     * Classe des actions appelées automatiquement lorsque l'utilisateur demande
     * à configurer les propriétés de cette couche. Les méthodes de cette action
     * doivent être appelées dans le thread de <i>Swing</i>. Elles ne font pas de
     * synchronisation afin de ne pas bloquer <i>Swing</i> pendant le chargement
     * d'une image par exemple.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Listeners implements ActionListener
    {
        public final void actionPerformed(final ActionEvent event)
        {
            final Object source=event.getSource();
            /*
             * Si l'utilisateur a sélectionné ou déselectionné
             * cette couche, fait apparaître ou disparaître la
             * couche correspondante. Cette action sera annulable.
             */
            if (source==selector)
            {
                final boolean selected = selector.isSelected();
                trigger.setEnabled(selected);
                fireStateChanged(new Edit()
                {
                    protected void edit(final boolean redo)
                    {
                        final boolean s = redo ? selected : !selected;
                        selector.setSelected(s);
                        trigger .setEnabled (s);
                    }
                });
            }
            /*
             * Si l'utilisateur a demandé à configurer cette
             * couche, fait apparaître le paneau de contrôle.
             */
            if (source==trigger)
            {
                selector.setEnabled(false); // Must be first
                trigger .setEnabled(false);
                try
                {
                    showControler((JComponent) source);
                }
                finally
                {
                    trigger .setEnabled(selector.isSelected());
                    selector.setEnabled(true); // Must be last
                }
            }
        }
    }

    /**
     * Construit un objet {@link LayerFactory}.
     *
     * @param selected <code>true</code> si cette couche
     *        doit être initialement sélectionnée.
     */
    protected LayerControl(final boolean selected)
    {
        // Note: we can't call 'getName()' before
        //       the construction is finished.
        selector = new JCheckBox((String)null, selected);
        trigger  = new JButton();
    }

    /**
     * Ajoute les composantes qui forment l'interface utilisateur.
     * Cette méthode ne doit être appelée qu'une seule fois.
     *
     * @param  owner Composante dans lequel placer l'interface.
     * @param  c Contraintes à utiliser pour la disposition de l'interface.
     *         Le champ <code>gridy</code> doit être initialisé. Les autres
     *         champs peuvent être écrasés.
     * @param  buttonSize   Taille par défaut des boutons "propriétés".
     * @param  propertyIcon Icône  par défaut des boutons "propriétés".
     * @param  propertyText Texte  par défaut des boutons "propriétés".
     * @throws IllegalStateException si l'interface avait déjà été construite.
     */
    private void buildUI(final Container owner, final GridBagConstraints c, final Dimension buttonSize, final Icon propertyIcon, final String propertyText)
    {
        final Listeners action = new Listeners();
        trigger .setIcon(propertyIcon);
        trigger .setPreferredSize(buttonSize);
        trigger .setToolTipText(propertyText);
        trigger .setEnabled(selector.isSelected());
        trigger .addActionListener(action);
        selector.addActionListener(action);
        selector.setText(getName());
        c.gridx=0; c.weightx=1; c.fill=c.HORIZONTAL; c.insets.left=9; c.insets.right=3; owner.add(selector, c);
        c.gridx=1; c.weightx=0; c.fill=c.NONE;       c.insets.left=0; c.insets.right=6; owner.add(trigger,  c);
    }

    /**
     * Retourne un paneau qui contiendra tous les contrôles specifiés.
     *
     * @param layers Contrôles à placer dans le paneau.
     * @param icon Icône à utiliser pour chaque contrôle.
     */
    public static JComponent getPanel(final LayerControl[] layers, final Icon icon)
    {
        final JPanel panel = new JPanel(new GridBagLayout());
        if (layers!=null)
        {
            final GridBagConstraints c = new GridBagConstraints();
            final String  propertyText = Resources.format(ResourceKeys.PROPERTIES);
            final Dimension buttonSize = new Dimension(24,24);
            for (int i=0; i<layers.length; i++)
            {
                final LayerControl layer=layers[i];
                synchronized (layer)
                {
                    c.gridy=i;
                    layer.buildUI(panel, c, buttonSize, icon, propertyText);
                }
            }
        }
        return panel;
    }

    /**
     * Retourne le nom de cette couche.
     */
    public abstract String getName();

    /**
     * Indique si cette couche est sélectionnée.
     * Cette méthode n'a pas à être synchronisée.
     */
    public final boolean isSelected()
    {return selector.isSelected();}

    /**
     * Configure des couches en fonction de cet objet <code>LayerControl</code>.
     * Si l'argument <code>layers</code> est nul, alors cette méthode retourne
     * de nouvelles couches proprement configurées. Cette méthode peut être
     * appelée de n'importe quel thread (généralement pas celui de <i>Swing</i>).
     *
     * @param  layers Couche à configurer. Si non-nul, alors ces couches doivent
     *         avoir été créées précédemment par ce même objet <code>LayerControl</code>.
     * @param  entry Image à afficher. Il s'agit d'une image sélectionnée par
     *         l'utilisateur dans la liste déroulante qui apparaît à gauche de
     *         la mosaïque d'images.
     * @param  listeners Objets à informer des progrès d'une éventuelle lecture.
     * @return Des couches proprement configurées, ou <code>null</code> si la configuration
     *         se traduirait à toute fin pratique par la disparition de la couche.
     * @throws SQLException si les accès à la base de données ont échoués.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     */
    public abstract Layer[] configLayers(final Layer[] layers, final ImageEntry entry, final EventListenerList listeners) throws SQLException, IOException;

    /**
     * Fait apparaître un paneau de configuration pour les couches. Cette méthode est
     * responsable d'appeler {@link #fireStateChanged} si l'état d'une couche a changé
     * suite aux interventions de l'utilisateur. Les "listeners" réagiront habituellement
     * en appelant {@link #configLayers} pour chacune de leurs couches.
     */
    protected abstract void showControler(final JComponent owner);

    /**
     * Ajoute un objet à la liste des objets intéressés à
     * être informés des changements apportées à cette couche.
     */
    public final void addChangeListener(final ChangeListener listener)
    {listenerList.add(ChangeListener.class, listener);}

    /**
     * Retire un objet de la liste des objets intéressés à
     * être informés des changements apportées à cette couche.
     */
    public final void removeChangeListener(final ChangeListener listener)
    {listenerList.remove(ChangeListener.class, listener);}

    /**
     * Ajoute un objet à la liste des objets intéressés à être
     * informés chaque fois qu'une édition anulable a été faite.
     */
    public void addUndoableEditListener(final UndoableEditListener listener)
    {listenerList.add(UndoableEditListener.class, listener);}

    /**
     * Retire un objet de la liste des objets intéressés à être
     * informés chaque fois qu'une édition anulable a été faite.
     */
    public void removeUndoableEditListener(final UndoableEditListener listener)
    {listenerList.remove(UndoableEditListener.class, listener);}

    /**
     * Préviens tous les objets intéressés
     * que l'état de cette couche a changé.
     *
     * @param edit Objet capable d'annuler le changement, ou
     *        <code>null</code> si le changement n'est pas annulable.
     */
    protected final void fireStateChanged(final UndoableEdit edit)
    {
        ChangeEvent changeEvent=null;
        UndoableEditEvent editEvent=null;
        final Object[] listeners=listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;)
        {
            if (listeners[i]==ChangeListener.class)
            {
                if (changeEvent==null) changeEvent=new ChangeEvent(this);
                ((ChangeListener) listeners[i+1]).stateChanged(changeEvent);
            }
            if (listeners[i]==UndoableEditListener.class)
            {
                if (edit!=null)
                {
                    if (editEvent==null) editEvent=new UndoableEditEvent(this, edit);
                    ((UndoableEditListener) listeners[i+1]).undoableEditHappened(editEvent);
                }
            }
        }
    }

    /**
     * Libère les ressources utilisées par cette couche.
     *
     * @throws SQLException si un accès à la base de données était nécessaire et a échoué.
     */
    public void dispose() throws SQLException
    {}

    /**
     * Classe de base des éditions qui peuvent être annulées.
     * Les classes n'ont que {@link #edit} à redéfinir.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    protected abstract class Edit extends AbstractUndoableEdit
    {
        /**
         * Constructeur par défaut.
         */
        protected Edit()
        {}

        /**
         * Annule cette édition.  L'implémentation par défaut appelle
         * <code>{@link #edit edit}(false)</code> après avoir vérifié
         * que l'opération peut être annulée.
         */
        public void undo() throws CannotUndoException
        {
            super.undo();
            synchronized(LayerControl.this)
            {
                edit(false);
            }
            fireStateChanged(null);
        }

        /**
         * Refait cette édition. L'implémentation par défaut appelle
         * <code>{@link #edit edit}(true)</code> après avoir vérifié
         * que l'opération peut être annulée.
         */
        public void redo() throws CannotRedoException
        {
            super.redo();
            synchronized(LayerControl.this)
            {
                edit(true);
            }
            fireStateChanged(null);
        }

        /**
         * Annule ou refait cette édition.
         *
         * @param redo <code>false</code> pour annuler ou
         *        <code>true</code> pour refaire cette édition.
         */
        protected abstract void edit(boolean redo);
    }
}
