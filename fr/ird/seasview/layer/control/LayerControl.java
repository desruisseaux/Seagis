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
package fr.ird.seasview.layer.control;

// Bases de donn�es et images
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

// Ev�nements
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
 * une couche {@link Layer} pour une image {@link ImageEntry} donn�e.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class LayerControl
{
    /**
     * Case � cocher pour s�lectionner cette couche.
     */
    private final JToggleButton selector;

    /**
     * Bouton � appuyer pour configurer une couche.
     */
    private final AbstractButton trigger;

    /**
     * Liste des objets int�ress�s � �tre inform�s
     * des changements apport�s � cette couche.
     */
    private final EventListenerList listenerList=new EventListenerList();

    /**
     * Classe des actions appel�es automatiquement lorsque l'utilisateur demande
     * � configurer les propri�t�s de cette couche. Les m�thodes de cette action
     * doivent �tre appel�es dans le thread de <i>Swing</i>. Elles ne font pas de
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
             * Si l'utilisateur a s�lectionn� ou d�selectionn�
             * cette couche, fait appara�tre ou dispara�tre la
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
             * Si l'utilisateur a demand� � configurer cette
             * couche, fait appara�tre le paneau de contr�le.
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
     *        doit �tre initialement s�lectionn�e.
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
     * Cette m�thode ne doit �tre appel�e qu'une seule fois.
     *
     * @param  owner Composante dans lequel placer l'interface.
     * @param  c Contraintes � utiliser pour la disposition de l'interface.
     *         Le champ <code>gridy</code> doit �tre initialis�. Les autres
     *         champs peuvent �tre �cras�s.
     * @param  buttonSize   Taille par d�faut des boutons "propri�t�s".
     * @param  propertyIcon Ic�ne  par d�faut des boutons "propri�t�s".
     * @param  propertyText Texte  par d�faut des boutons "propri�t�s".
     * @throws IllegalStateException si l'interface avait d�j� �t� construite.
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
     * Retourne un paneau qui contiendra tous les contr�les specifi�s.
     *
     * @param layers Contr�les � placer dans le paneau.
     * @param icon Ic�ne � utiliser pour chaque contr�le.
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
     * Indique si cette couche est s�lectionn�e.
     * Cette m�thode n'a pas � �tre synchronis�e.
     */
    public final boolean isSelected()
    {return selector.isSelected();}

    /**
     * Configure des couches en fonction de cet objet <code>LayerControl</code>.
     * Si l'argument <code>layers</code> est nul, alors cette m�thode retourne
     * de nouvelles couches proprement configur�es. Cette m�thode peut �tre
     * appel�e de n'importe quel thread (g�n�ralement pas celui de <i>Swing</i>).
     *
     * @param  layers Couche � configurer. Si non-nul, alors ces couches doivent
     *         avoir �t� cr��es pr�c�demment par ce m�me objet <code>LayerControl</code>.
     * @param  entry Image � afficher. Il s'agit d'une image s�lectionn�e par
     *         l'utilisateur dans la liste d�roulante qui appara�t � gauche de
     *         la mosa�que d'images.
     * @param  listeners Objets � informer des progr�s d'une �ventuelle lecture.
     * @return Des couches proprement configur�es, ou <code>null</code> si la configuration
     *         se traduirait � toute fin pratique par la disparition de la couche.
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     * @throws IOException si une erreur d'entr�/sortie est survenue.
     */
    public abstract Layer[] configLayers(final Layer[] layers, final ImageEntry entry, final EventListenerList listeners) throws SQLException, IOException;

    /**
     * Fait appara�tre un paneau de configuration pour les couches. Cette m�thode est
     * responsable d'appeler {@link #fireStateChanged} si l'�tat d'une couche a chang�
     * suite aux interventions de l'utilisateur. Les "listeners" r�agiront habituellement
     * en appelant {@link #configLayers} pour chacune de leurs couches.
     */
    protected abstract void showControler(final JComponent owner);

    /**
     * Ajoute un objet � la liste des objets int�ress�s �
     * �tre inform�s des changements apport�es � cette couche.
     */
    public final void addChangeListener(final ChangeListener listener)
    {listenerList.add(ChangeListener.class, listener);}

    /**
     * Retire un objet de la liste des objets int�ress�s �
     * �tre inform�s des changements apport�es � cette couche.
     */
    public final void removeChangeListener(final ChangeListener listener)
    {listenerList.remove(ChangeListener.class, listener);}

    /**
     * Ajoute un objet � la liste des objets int�ress�s � �tre
     * inform�s chaque fois qu'une �dition anulable a �t� faite.
     */
    public void addUndoableEditListener(final UndoableEditListener listener)
    {listenerList.add(UndoableEditListener.class, listener);}

    /**
     * Retire un objet de la liste des objets int�ress�s � �tre
     * inform�s chaque fois qu'une �dition anulable a �t� faite.
     */
    public void removeUndoableEditListener(final UndoableEditListener listener)
    {listenerList.remove(UndoableEditListener.class, listener);}

    /**
     * Pr�viens tous les objets int�ress�s
     * que l'�tat de cette couche a chang�.
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
     * Lib�re les ressources utilis�es par cette couche.
     *
     * @throws SQLException si un acc�s � la base de donn�es �tait n�cessaire et a �chou�.
     */
    public void dispose() throws SQLException
    {}

    /**
     * Classe de base des �ditions qui peuvent �tre annul�es.
     * Les classes n'ont que {@link #edit} � red�finir.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    protected abstract class Edit extends AbstractUndoableEdit
    {
        /**
         * Constructeur par d�faut.
         */
        protected Edit()
        {}

        /**
         * Annule cette �dition.  L'impl�mentation par d�faut appelle
         * <code>{@link #edit edit}(false)</code> apr�s avoir v�rifi�
         * que l'op�ration peut �tre annul�e.
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
         * Refait cette �dition. L'impl�mentation par d�faut appelle
         * <code>{@link #edit edit}(true)</code> apr�s avoir v�rifi�
         * que l'op�ration peut �tre annul�e.
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
         * Annule ou refait cette �dition.
         *
         * @param redo <code>false</code> pour annuler ou
         *        <code>true</code> pour refaire cette �dition.
         */
        protected abstract void edit(boolean redo);
    }
}
