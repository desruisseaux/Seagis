/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.gui.swing;

// J2SE
import javax.swing.JList;
import javax.swing.ListModel;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.database.sample.OperationEntry;
import fr.ird.database.sample.RelativePositionEntry;


/**
 * Utilitaires swing pour la gestion des bases de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Utilities {
    /**
     * Interdit la cr�ation d'instances de cette classes.
     */
    private Utilities() {
    }

    /**
     * S�lectionne les positions spatio-temporelle relatives par d�faut.
     */
    public static void selectDefaultPositions(final JList list) {
        final ListModel model = list.getModel();
        final int size = model.getSize();
        final int[] index = new int[size];
        int count = 0;
        for (int i=0; i<size; i++) {
            if (((RelativePositionEntry) model.getElementAt(i)).isDefault()) {
                index[count++] = i;
            }
        }
        list.setSelectedIndices(XArray.resize(index, count));
    }

    /**
     * Change le mode d'affichage de la liste de fa�on � ce qu'elle affiche les
     * noms de colonnes plut�t que les noms cours des op�rations.
     */
    public static void displayOperationColumnName(final JList list) {
        final ListModel model = list.getModel();
        final OperationEntry[] operations = new OperationEntry[model.getSize()];
        for (int i=0; i<operations.length; i++) {
            operations[i] = new OperationEntry.Proxy((OperationEntry)model.getElementAt(i)) {
                public String toString() {
                    return getColumn();
                }
            };
        }
        list.setListData(operations);
    }
}
