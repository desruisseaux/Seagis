/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.awt;

// Kernel
import javax.media.jai.KernelJAI;

// Graphical user interface
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

// Graphical user interface (Swing)
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.table.AbstractTableModel;

// Events
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;

// Collections
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;

// Resources
import net.seas.util.XArray;
import net.seas.resources.Resources;
import net.seas.resources.ResourceKeys;


/**
 * A widget for selecting and/or editing a {@link KernelJAI} object.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/KernelEditor.png"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class KernelEditor extends JComponent
{
    /**
     * The matrix coefficient as a table.
     */
    private final Model model = new Model();

    /**
     * The list of available filters.
     */
    private final JComboBox filters = new JComboBox(model);

    /**
     * The matrix width.
     */
    private final JSpinner width = new JSpinner();

    /**
     * The matrix height.
     */
    private final JSpinner height = new JSpinner();

    /**
     * Construct a new kernel editor.
     */
    public KernelEditor()
    {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        width.addChangeListener(model);
        height.addChangeListener(model);
        final JTable matrixView = new JTable(model);
        matrixView.setTableHeader(null);
        matrixView.setRowSelectionAllowed(false);
        matrixView.setColumnSelectionAllowed(false);

        final Resources resources = Resources.getResources(null);
        final GridBagConstraints c = new GridBagConstraints();

        // Add labels in the two first rows
        c.fill=c.HORIZONTAL; c.gridx=0;
        c.gridy=0; add(new JLabel(resources.getLabel(ResourceKeys.KERNEL), JLabel.RIGHT ), c);
        c.gridy=1; add(new JLabel(resources.getLabel(ResourceKeys.SIZE  ), JLabel.RIGHT ), c);
        c.gridx=2; add(new JLabel(' '+resources.getString(ResourceKeys.LINES).toLowerCase()+" \u00D7 ", JLabel.CENTER), c);
        c.gridx=4; add(new JLabel(' '+resources.getString(ResourceKeys.COLUMNS).toLowerCase(), JLabel.LEFT),  c);

        // Add controlers (kernel name and size)
        c.weightx=1;
        c.gridx=1; add(height, c);
        c.gridx=3; add(width,  c);

        c.gridy=0; c.gridx=1; c.gridwidth=c.REMAINDER;
        add(filters, c);

        // Add table for kernel coefficients
        c.gridy=2; c.gridx=0;                  c.insets.top=9; add(new JLabel(resources.getString(ResourceKeys.COEFFICIENTS)), c);
        c.gridy=3; c.weighty=1; c.fill=c.BOTH; c.insets.top=0; add(new JScrollPane(matrixView), c);
        setPreferredSize(new Dimension(300,180));

        addKernel("Floyd & Steinberg (1975)",      KernelJAI.ERROR_FILTER_FLOYD_STEINBERG);
        addKernel("Jarvis, Judice & Ninke (1976)", KernelJAI.ERROR_FILTER_JARVIS);
        addKernel("Stucki (1981)",                 KernelJAI.ERROR_FILTER_STUCKI);

        addKernel("Sharp 1",   new float[] { 0.0f, -1.0f,  0.0f,
                                            -1.0f,  5.0f, -1.0f,
                                             0.0f, -1.0f,  0.0f});

        addKernel("Sharp 2",   new float[] {-1.0f, -1.0f, -1.0f,
                                            -1.0f,  9.0f, -1.0f,
                                            -1.0f, -1.0f, -1.0f});

        addKernel("Sharp 3",   new float[] { 1.0f, -2.0f,  1.0f,
                                            -2.0f,  5.0f, -2.0f,
                                             1.0f, -2.0f,  1.0f});

        addKernel("Sharp 4"  , new float[] {-1.0f,  1.0f, -1.0f,
                                             1.0f,  1.0f,  1.0f,
                                            -1.0f,  1.0f, -1.0f});

        addKernel("Laplace 1", new float[] {-1.0f, -1.0f, -1.0f,
                                            -1.0f,  8.0f, -1.0f,
                                            -1.0f, -1.0f, -1.0f});

        addKernel("Laplace 2", new float[] { 0.0f, -1.0f,  0.0f,
                                            -1.0f,  4.0f, -1.0f,
                                             0.0f, -1.0f,  0.0f});

        addKernel("Box",       new float[] { 1.0f,  1.0f,  1.0f,
                                             1.0f,  1.0f,  1.0f,
                                             1.0f,  1.0f,  1.0f});

        addKernel("Low pass",  new float[] { 1.0f,  2.0f,  1.0f,
                                             2.0f,  4.0f,  2.0f,
                                             1.0f,  2.0f,  1.0f});
    }

    /**
     * Add a 3x3 kernel to the list of available kernels.
     */
    private void addKernel(final String name, final float[] data)
    {
        double sum=0;
        for (int i=0; i<data.length; i++) sum += data[i];
        if (sum!=0)
            for (int i=0; i<data.length; i++) data[i] /= sum;
        addKernel(name, new KernelJAI(3,3,data));
    }

    /**
     * Add a kernel to the list of available kernels.
     *
     * @param name The kernel name. Kernel will be displayed in alphabetic order.
     * @param kernel The kernel. If an other kernel was registered with the same
     *        name, the previous kernel will be discarted.
     */
    public void addKernel(final String name, final KernelJAI kernel)
    {model.addKernel(name, kernel);}

    /**
     * Set the kernel. The table size will be set to the
     * specified kernel size, add all coefficients will
     * be copied in the table.
     *
     * @param kernel The new kernel.
     */
    public void setKernel(final KernelJAI kernel)
    {
        model.setKernel(kernel);
        model.findKernelName();
    }

    /**
     * Set the kernel by its name. It must be one of
     * the name registered with {@link #addKernel}.
     * If <code>name</code> is not found, then nothing
     * is done.
     */
    public void setKernel(final String name)
    {
        filters.setSelectedItem(name);
        filters.repaint();
    }

    /**
     * Returns the currently edited kernel.
     */
    public KernelJAI getKernel()
    {return model.getKernel();}

    /**
     * Returns an array of kernel names.
     */
    public String[] getKernelNames()
    {return (String[]) model.getKernelNames().clone();}

    /**
     * Set the size of the current kernel.
     *
     * @param width  The number of rows.
     * @param height The number of columns.
     */
    public void setKernelSize(final int width, final int height)
    {
        model.setKernelSize(height, width); // Inverse argument order.
    }

    /**
     * The table and list model to use. The list model contains a list of
     * predefined kernels. The table model contains coefficients for the
     * currently selected kernel. This object is also a listener for various
     * events (like changing the size of the table).
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Model extends AbstractTableModel implements ComboBoxModel, ChangeListener
    {
        /**
         * Dictionnary of kernel by their names.
         */
        private final Map<String,KernelJAI> kernels = new HashMap<String,KernelJAI>();

        /**
         * List of kernel names in alphabetical order.
         * This list is constructed only when first needed.
         */
        private String[] names;

        /**
         * Name of the current kernel, or <code>null</code>
         * if the user is editing a custom kernel.
         */
        private String name;

        /**
         * Array of elements for the current kernel.
         */
        private float[][] elements = new float[0][];

        /**
         * Returns the number of kernels in the list.
         */
        public int getSize()
        {return getKernelNames().length;}

        /**
         * Returns the number of rows in the kernel.
         */
        public int getRowCount()
        {return elements.length;}

        /**
         * Returns the number of columns in the model.
         */
        public int getColumnCount()
        {return (elements.length!=0) ? elements[0].length : 0;}

        /**
         * Returns <code>true</code> regardless of row and column index
         */
        public boolean isCellEditable(final int rowIndex, final int columnIndex)
        {return true;}

        /**
         * Returns <code>Float.class</code> regardless of column index
         */
        public Class getColumnClass(final int columnIndex)
        {return Float.class;}

        /**
         * Returns the value for the cell at <code>columnIndex</code>
         * and <code>rowIndex</code>.
         */
        public Object/*TODO: Float*/ getValueAt(final int rowIndex, final int columnIndex)
        {return new Float(elements[rowIndex][columnIndex]);}

        /**
         * Set the value for the cell at <code>columnIndex</code>
         * and <code>rowIndex</code>.
         */
        public void setValueAt(final Object value, final int rowIndex, final int columnIndex)
        {
            elements[rowIndex][columnIndex] = ((Number) value).floatValue();
            fireTableCellUpdated(rowIndex, columnIndex);
            findKernelName();
        }

        /**
         * Returns the kernel at the specified index.
         */
        public String getElementAt(final int index)
        {return getKernelNames()[index];}

        /**
         * Returns the selected kernel name.
         */
        public String getSelectedItem()
        {return (name!=null) ? name : getString(ResourceKeys.PERSONALIZED);}

        /**
         * Set the selected kernel by its name.
         */
        public void setSelectedItem(final Object item)
        {
            final String newName = item.toString();
            if (!newName.equals(name))
            {
                // 'kernel' may be null if 'item'
                // is the "Personalized" kernel name.
                final KernelJAI kernel = kernels.get(newName);
                if (kernel!=null) setKernel(kernel);
                this.name = newName;
            }
        }

        /**
         * Returns the kernel.
         */
        public KernelJAI getKernel()
        {
            final int   height = elements.length;
            final int    width = height!=0 ? elements[0].length : 0;
            final float[] data = new float[width*height];
            int c=0; for (int j=0; j<height; j++)
                for (int i=0; i<width; i++)
                    data[c++] = elements[j][i];
            return new KernelJAI(width, height, data);
        }

        /**
         * Set the kernel.
         */
        public void setKernel(final KernelJAI kernel)
        {
            final int rowCount = kernel.getHeight();
            final int colCount = kernel.getWidth();
            setKernelSize(rowCount, colCount);
            for (int j=0; j<rowCount; j++)
                for (int i=0; i<colCount; i++)
                    elements[j][i] = kernel.getElement(i,j);
            fireTableDataChanged();
        }

        /**
         * Set the kernel's size.
         */
        public void setKernelSize(final int rowCount, final int colCount)
        {
            final int oldRowCount = elements.length;
            final int oldColCount = oldRowCount!=0 ? elements[0].length : 0;
            if (rowCount!=oldRowCount || colCount!=oldColCount)
            {
                elements = XArray.resize(elements, rowCount);
                for (int i=0; i<elements.length; i++)
                {
                    if (elements[i]==null) elements[i]=new float[colCount];
                    else elements[i] = XArray.resize(elements[i], colCount);
                }
                if (colCount != oldColCount)
                    fireTableStructureChanged();
                else if (rowCount > oldRowCount)
                    fireTableRowsInserted(oldRowCount, rowCount-1);
                else if (rowCount < oldRowCount)
                    fireTableRowsDeleted(rowCount, oldRowCount-1);
                width .setValue(new Integer(colCount));
                height.setValue(new Integer(rowCount));
            }
        }

        /**
         * Add a kernel.
         */
        public void addKernel(final String name, final KernelJAI kernel)
        {
            if (kernels.put(name, kernel)==null)
            {
                names = null;
                fireListChanged(0, kernels.size()-1);
            }
        }

        /**
         * Returns the array of kernel names. <strong>This method
         * returns the array by reference; do not modify!</strong>.
         */
        public String[] getKernelNames()
        {
            if (names==null)
            {
                final int size = kernels.size();
                names = kernels.keySet().toArray(new String[size]);
                Arrays.sort(names);
                names = XArray.resize(names, size+1);
                names[size] = getString(ResourceKeys.PERSONALIZED);
            }
            return names;
        }

        /**
         * Find the name for the current kernel. If such a name is
         * found, it will be given to the combo-box. Otherwise,
         * nothing is done.
         */
        protected void findKernelName()
        {
            String newName=null; // "Personalized"
            final int rowCount = elements.length;
            final int colCount = rowCount!=0 ? elements[0].length : 0;
      iter: for (final Iterator<Map.Entry<String,KernelJAI>> it=kernels.entrySet().iterator(); it.hasNext();)
            {
                final Map.Entry<String,KernelJAI> entry = it.next();
                final KernelJAI kernel = entry.getValue();
                if (rowCount==kernel.getHeight() && colCount==kernel.getWidth())
                {
                    for (int j=0; j<rowCount; j++)
                        for (int i=0; i<colCount; i++)
                            if (elements[j][i] != kernel.getElement(i,j))
                                continue iter;
                    newName = entry.getKey();
                }
            }
            if (newName == null)
                newName = getString(ResourceKeys.PERSONALIZED);
            if (!newName.equals(name))
            {
                // Set the name now in order to avoid that
                // setSelectedItem invokes setKernel again.
                this.name = newName;
                filters.setSelectedItem(newName);
                filters.repaint(); // JComboBox doesn't seems to repaint by itself.
            }
        }
        
        /**
         * Invoked when a {@link JSpinner} has changed its state.
         * This method reset the matrix size according the new
         * spinner value.
         */
        public void stateChanged(final ChangeEvent event)
        {
            final int rowCount = ((Number) height.getValue()).intValue();
            final int colCount = ((Number) width. getValue()).intValue();
            setKernelSize(rowCount, colCount);
        }

        /**
         * Convenience method returning a string for the specified resource keys.
         */
        private String getString(final int key)
        {return Resources.getResources(getLocale()).getString(key);}

        /**
         * Adds a listener to the list that's notified
         * each time a change to the data model occurs.
         */
        public void addListDataListener(final ListDataListener listener)
        {listenerList.add(ListDataListener.class, listener);}
        
        /**
         * Removes a listener from the list that's notified
         * each time a change to the data model occurs.
         */
        public void removeListDataListener(final ListDataListener listener)
        {listenerList.remove(ListDataListener.class, listener);}

        /**
         * Invoked after one or more kernels are added to the model.
         */
        protected void fireListChanged(final int index0, final int index1)
        {
            ListDataEvent event = null;
            final Object[] listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;)
            {
                if (listeners[i]==ListDataListener.class)
                {
                    if (event==null)
                        event=new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
                    ((ListDataListener)listeners[i+1]).contentsChanged(event);
                }
            }
        }
    }

    /**
     * Test this kernel editor.
     */
    public static void main(String[] args)
    {
        final KernelEditor test = new KernelEditor();
        final JFrame frame = new JFrame("KernelEditor");
        frame.getContentPane().add(test);
        frame.pack();
        frame.show();
    }
}