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
package fr.ird.layer.control;

// User interface (AWT)
import java.awt.Color;
import java.awt.Insets;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

// User interface (Swing)
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import net.seas.util.SwingUtilities;
import net.seas.awt.KernelEditor;

// Miscellaneous
import net.seas.util.XArray;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import fr.ird.operator.coverage.Operation;


/**
 * A control panel for configuring an image. This control panel
 * allows to select an operation, a convolution kernel, etc.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageControlPanel extends JPanel
{
    /**
     * Chaîne de caractères indiquant qu'aucune opération ne sera
     * appliquée sur les images. Cette chaîne de caractères sera
     * affichée à la place de la valeur <code>null</code>.
     */
    private final String NO_OPERATION;

    /**
     * Liste des opérations pouvant être appliqués sur les images.
     */
    private final JList operations;

    /**
     * Construit un controleur.
     */
    public ImageControlPanel()
    {
        super(new GridBagLayout());
        final Resources resources = Resources.getResources(null);
        NO_OPERATION = resources.getString(ResourceKeys.NO_OPERATION);
        operations = new JList(new Object[]
        {
            NO_OPERATION
        });
        operations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        /*
         * Ajoute quelques opérateurs par défaut.
         * NOTE: plusieurs de ces opérateurs ne
         *       sont que temporaires!   Ils me
         *       servent à faire un contrôle de
         *       qualité des images.
         */
//      if (true ) addImageOperation(new ColorRamp(new Color(16,16,16), new Color(240,240,240)));
//      if (true ) for (int i=0; i<=10; i++) addImageOperation(new Convolution(i)); // TODO (JUST A TRY)
//      if (false) addImageOperation(new ColorSmoother(ColorSmoother.KEEP_UPPER_COLOR));
//      if (false) addImageOperation(new ThemeEraser("Quadrillage", 1));
        /*
         * Construit l'interface utilisateur.
         */
        final GridBagConstraints c = new GridBagConstraints();
        c.fill=c.BOTH; c.weightx=1; c.weighty=1;
        c.gridx=0; c.gridy=0; add(new JScrollPane(operations), c);
        setSelectedOperation(null);
    }

    /**
     * Ajoute une opération qui peut être appliquée sur les images.
     * L'utilisateur peut choisir l'opération de son choix dans la
     * boîte "combo box".
     */
    public void addOperation(final Operation operation)
    {((DefaultListModel) operations.getModel()).addElement(operation);}

    /**
     * Retire une opération qui pouvait être appliquée sur les images.
     */
    public void removeOperation(final Operation operation)
    {((DefaultListModel) operations.getModel()).removeElement(operation);}

    /**
     * Retourne la liste des opérations qui ont été spécifiées.
     */
    public Operation[] getOperations()
    {
        final DefaultListModel list = (DefaultListModel) operations.getModel();
        final Operation[] ops=new Operation[list.getSize()];
        int c=0; for (int i=0; i<ops.length; i++)
        {
            final Object op=list.getElementAt(i);
            if (op instanceof Operation)
                ops[c++] = (Operation) op;
        }
        return XArray.resize(ops, c);
    }

    /**
     * Retourne l'opération présentement sélectionnée,
     * ou <code>null</code> s'il n'y en a pas.
     */
    public Operation getSelectedOperation()
    {
        final Object op = operations.getSelectedValue();
        return (op instanceof Operation) ? (Operation) op : null;
    }

    /**
     * Définit l'opération à appliquer sur les image. L'opération spécifiée
     * sera sélectionnée dans la boîte "combo box" des opérations. La valeur
     * <code>null</code> signifie qu'aucune opération ne doit être appliquée
     * sur les images.
     */
    public void setSelectedOperation(final Operation operation)
    {operations.setSelectedValue(operation!=null ? (Object)operation : (Object)NO_OPERATION, true);}

    /**
     * Fait apparaître le controleur. Si l'utilisateur a cliqué
     * sur "Ok", alors cette méthode retourne <code>true</code>.
     */
    public boolean showDialog(final Component owner)
    {
        final Operation operation = getSelectedOperation();
        if (SwingUtilities.showOptionDialog(owner, this, Resources.format(ResourceKeys.IMAGES)))
        {
            return true;
        }
        setSelectedOperation(operation);
        return false;
    }
}
