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
package fr.ird.seasview.layer.control;

// User interface (AWT)
import java.awt.Color;
import java.awt.Insets;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

// User interface (Swing)
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// Miscellaneous
import fr.ird.util.XArray;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;

// Geotools dependencies
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.resources.SwingUtilities;


/**
 * Paneau de configuration des images. Ce paneau fournit des options permettant de
 * sélectionner une opération, une convolution, etc.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageControlPanel extends JPanel implements ListSelectionListener, ActionListener {
    /**
     * Chaîne de caractères indiquant qu'aucune opération ne sera
     * appliquée sur les images. Cette chaîne de caractères sera
     * affichée à la place de la valeur <code>null</code>.
     */
    private final String NO_OPERATION;

    /**
     * L'icône pour le bouton "Configurer".
     */
    private static Icon configIcon;

    /**
     * Liste des opérations pouvant être appliqués sur les images.
     */
    private final JList operations;

    /**
     * Le bouton permettant de configurer une opération.
     */
    private final JButton config;

    /**
     * Construit un controleur.
     */
    public ImageControlPanel() {
        super(new GridBagLayout());
        final Resources resources = Resources.getResources(null);
        NO_OPERATION = resources.getString(ResourceKeys.NO_OPERATION);
        final DefaultListModel model = new DefaultListModel();
        model.addElement(NO_OPERATION);
        operations = new JList(model);
        operations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        operations.addListSelectionListener(this);
        if (configIcon == null) {
            configIcon = new ImageIcon(getClass().getClassLoader()
                                .getResource("toolbarButtonGraphics/general/Properties16.gif"));
        }
        config = new JButton("Configurer...", configIcon);
        config.addActionListener(this);
        /*
         * Construit l'interface utilisateur.
         */
        final GridBagConstraints c = new GridBagConstraints();
        c.fill=c.BOTH; c.weightx=1; c.weighty=1;
        c.gridx=0; c.gridy=0; add(new JScrollPane(operations), c);
        c.gridy=1; c.fill=c.HORIZONTAL; c.weighty=0;
        add(config, c);
        setSelectedOperation(null);
    }

    /**
     * Ajoute une série d'opérations par défaut.
     *
     * @param processor Processeur à utiliser pour ajouter des opérations.
     */
    protected void addDefaultOperations() {
        final Resources             resources = Resources.getResources(null);
        final GridCoverageProcessor processor = GridCoverageProcessor.getDefault();
        if (true) addOperation(new ProcessorOperation        (processor, "Recolor",           resources.getString(ResourceKeys.GRAY_SCALE    )));
        if (true) addOperation(new GradientMagnitudeOperation(processor, "GradientMagnitude", resources.getString(ResourceKeys.GRADIENT_SOBEL)));
//      if (true ) for (int i=0; i<=10; i++) addImageOperation(new Convolution(i)); // TODO (JUST A TRY)
//      if (false) addImageOperation(new ColorSmoother(ColorSmoother.KEEP_UPPER_COLOR));
//      if (false) addImageOperation(new ThemeEraser("Quadrillage", 1));
    }

    /**
     * Ajoute une opération qui peut être appliquée sur les images.
     * L'utilisateur peut choisir l'opération de son choix dans la
     * boîte "combo box".
     */
    public void addOperation(final Operation operation) {
        ((DefaultListModel) operations.getModel()).addElement(operation);
    }

    /**
     * Retire une opération qui pouvait être appliquée sur les images.
     */
    public void removeOperation(final Operation operation) {
        ((DefaultListModel) operations.getModel()).removeElement(operation);
    }

    /**
     * Retourne la liste des opérations qui ont été spécifiées.
     */
    public Operation[] getOperations() {
        final DefaultListModel list = (DefaultListModel) operations.getModel();
        final Operation[] ops=new Operation[list.getSize()];
        int c=0; for (int i=0; i<ops.length; i++) {
            final Object op=list.getElementAt(i);
            if (op instanceof Operation) {
                ops[c++] = (Operation) op;
            }
        }
        return XArray.resize(ops, c);
    }

    /**
     * Retourne l'opération présentement sélectionnée,
     * ou <code>null</code> s'il n'y en a pas.
     */
    public Operation getSelectedOperation() {
        final Object op = operations.getSelectedValue();
        return (op instanceof Operation) ? (Operation) op : null;
    }

    /**
     * Définit l'opération à appliquer sur les image. L'opération spécifiée
     * sera sélectionnée dans la boîte "combo box" des opérations. La valeur
     * <code>null</code> signifie qu'aucune opération ne doit être appliquée
     * sur les images.
     */
    public void setSelectedOperation(final Operation operation) {
        operations.setSelectedValue(operation!=null ? (Object)operation : (Object)NO_OPERATION, true);
        config.setEnabled(operation instanceof Configurable);
    }

    /**
     * Fait apparaître le controleur. Si l'utilisateur a cliqué
     * sur "Ok", alors cette méthode retourne <code>true</code>.
     */
    public boolean showDialog(final Component owner) {
        final Operation operation = getSelectedOperation();
        if (SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.IMAGES))) {
            return true;
        }
        setSelectedOperation(operation);
        return false;
    }
    
    /**
     * Appelée automatiquement lorsque l'utilisateur a choisi une nouvelle opération.
     */
    public void valueChanged(ListSelectionEvent e) {
        config.setEnabled(getSelectedOperation() instanceof Configurable);
    }
    
    /**
     * Appelée lorsque l'utilisateur a appuyé sur le bouton "Configurer".
     */
    public void actionPerformed(ActionEvent e) {
        final Operation operation = getSelectedOperation();
        if (operation instanceof Configurable) {
            ((Configurable) operation).showControler(this);
        }
    }
}
