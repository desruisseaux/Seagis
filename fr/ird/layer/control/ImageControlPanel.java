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
     * Cha�ne de caract�res indiquant qu'aucune op�ration ne sera
     * appliqu�e sur les images. Cette cha�ne de caract�res sera
     * affich�e � la place de la valeur <code>null</code>.
     */
    private final String NO_OPERATION;

    /**
     * Liste des op�rations pouvant �tre appliqu�s sur les images.
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
         * Ajoute quelques op�rateurs par d�faut.
         * NOTE: plusieurs de ces op�rateurs ne
         *       sont que temporaires!   Ils me
         *       servent � faire un contr�le de
         *       qualit� des images.
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
     * Ajoute une op�ration qui peut �tre appliqu�e sur les images.
     * L'utilisateur peut choisir l'op�ration de son choix dans la
     * bo�te "combo box".
     */
    public void addOperation(final Operation operation)
    {((DefaultListModel) operations.getModel()).addElement(operation);}

    /**
     * Retire une op�ration qui pouvait �tre appliqu�e sur les images.
     */
    public void removeOperation(final Operation operation)
    {((DefaultListModel) operations.getModel()).removeElement(operation);}

    /**
     * Retourne la liste des op�rations qui ont �t� sp�cifi�es.
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
     * Retourne l'op�ration pr�sentement s�lectionn�e,
     * ou <code>null</code> s'il n'y en a pas.
     */
    public Operation getSelectedOperation()
    {
        final Object op = operations.getSelectedValue();
        return (op instanceof Operation) ? (Operation) op : null;
    }

    /**
     * D�finit l'op�ration � appliquer sur les image. L'op�ration sp�cifi�e
     * sera s�lectionn�e dans la bo�te "combo box" des op�rations. La valeur
     * <code>null</code> signifie qu'aucune op�ration ne doit �tre appliqu�e
     * sur les images.
     */
    public void setSelectedOperation(final Operation operation)
    {operations.setSelectedValue(operation!=null ? (Object)operation : (Object)NO_OPERATION, true);}

    /**
     * Fait appara�tre le controleur. Si l'utilisateur a cliqu�
     * sur "Ok", alors cette m�thode retourne <code>true</code>.
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
