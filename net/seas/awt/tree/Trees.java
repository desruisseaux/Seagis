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
package net.seas.awt.tree;

// Arborescence
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;

// Collections
import java.util.List;
import java.util.ArrayList;
import net.seas.util.XArray;


/**
 * Convenience static methods for trees operations.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Trees
{
    /**
     * Interdit la création d'objets de cette classe.
     */
    private Trees()
    {}

    /**
     * Retourne les chemins vers l'objet spécifié. Cette méthode suppose que l'arborescence est
     * constituée de noeuds {@link net.seas.awt.tree.TreeNode} et comparera <code>value</code>
     * avec les objets retournés par la méthode {@link net.seas.awt.tree.TreeNode#getUserObject}.
     * Les noeuds qui ne sont pas des objets {@link net.seas.awt.tree.TreeNode} ne seront pas
     * comparés à <code>value</code>.
     *
     * @param  model Modèle dans lequel rechercher le chemin.
     * @param  value Objet à rechercher dans {@link net.seas.awt.tree.TreeNode#getUserObject}.
     * @return Chemins vers l'objet spécifié. Ce tableau peut avoir une
     *         longueur de 0, mais ne sera jamais <code>null</code>.
     */
    public static TreePath[] getPathsToUserObject(final TreeModel model, final Object value)
    {
        final List<TreePath> paths=new ArrayList<TreePath>(8);
        final Object[] path=new Object[8];
        path[0]=model.getRoot();
        getPathsToUserObject(model, value, path, 1, paths);
        return paths.toArray(new TreePath[paths.size()]);
    }

    /**
     * Implémentation de la recherche des chemins. Cette
     * méthode s'appele elle-même d'une façon récursive.
     *
     * @param  model  Modèle dans lequel rechercher le chemin.
     * @param  value  Objet à rechercher dans {@link net.seas.awt.tree.TreeNode#getUserObject}.
     * @param  path   Chemin parcouru jusqu'à maintenant.
     * @param  length Longueur valide de <code>path</code>.
     * @param  list   Liste dans laquelle ajouter les {@link TreePath} trouvés.
     * @return <code>path</code>, ou un nouveau tableau s'il a fallu l'agrandir.
     */
    private static Object[] getPathsToUserObject(final TreeModel model, final Object value, Object[] path, final int length, final List<TreePath> list)
    {
        final Object parent=path[length-1];
        if (parent instanceof net.seas.awt.tree.TreeNode)
        {
            final Object nodeValue = ((net.seas.awt.tree.TreeNode) parent).getUserObject();
            if (nodeValue==value || (value!=null && value.equals(nodeValue)))
            {
                list.add(new TreePath(XArray.resize(path, length)));
            }
        }
        final int count=model.getChildCount(parent);
        for (int i=0; i<count; i++)
        {
            if (length>=path.length)
            {
                path = XArray.resize(path, length << 1);
            }
            path[length] = model.getChild(parent, i);
            path = getPathsToUserObject(model, value, path, length+1, list);
        }
        return path;
    }

    /**
     * Construit une chaîne de caractères qui contiendra le
     * noeud spécifié ainsi que tous les noeuds enfants.
     *
     * @param model  Arborescence à écrire.
     * @param node   Noeud de l'arborescence à écrire.
     * @param buffer Buffer dans lequel écrire le noeud.
     * @param level  Niveau d'indentation (à partir de 0).
     * @param last   Indique si les niveaux précédents sont
     *               en train d'écrire leurs derniers items.
     * @return       Le tableau <code>last</code>, qui peut
     *               éventuellement avoir été agrandit.
     */
    private static boolean[] toString(final TreeModel model, final Object node, final StringBuffer buffer, final int level, boolean[] last)
    {
        for (int i=0; i<level; i++)
        {
            if (i != level-1)
            {
                buffer.append(last[i] ? '\u00A0' : '\u2502');
                buffer.append("\u00A0\u00A0\u00A0");
            }
            else
            {
                buffer.append(last[i] ? '\u2514': '\u251C');
                buffer.append("\u2500\u2500\u2500");
            }
        }
        buffer.append(node);
        buffer.append('\n');
        if (level >= last.length)
        {
            last = XArray.resize(last, level*2);
        }
        final int count=model.getChildCount(node);
        for (int i=0; i<count; i++)
        {
            last[level] = (i == count-1);
            last=toString(model, model.getChild(node,i), buffer, level+1, last);
        }
        return last;
    }

    /**
     * Retourne une chaîne de caractères qui contiendra une
     * représentation graphique de l'arborescence spécifiée.
     * Cette arborescence apparaître correctement si elle
     * est écrite avec une police mono-espacée.
     *
     * @param  tree Arborescence à écrire.
     * @param  root Noeud à partir d'où commencer à tracer l'arborescence.
     * @return Chaîne de caractères représentant l'arborescence, ou
     *         <code>null</code> si <code>root</code> était nul.
     */
    private static String toString(final TreeModel tree, final Object root)
    {
        if (root==null) return null;
        final StringBuffer buffer=new StringBuffer();
        toString(tree, root, buffer, 0, new boolean[64]);
        return buffer.toString();
    }

    /**
     * Retourne une chaîne de caractères qui contiendra une
     * représentation graphique de l'arborescence spécifiée.
     * Cette arborescence apparaître correctement si elle
     * est écrite avec une police mono-espacée.
     *
     * @param  tree Arborescence à écrire.
     * @return Chaîne de caractères représentant l'arborescence, ou
     *         <code>null</code> si l'arborescence ne contenait aucun noeud.
     */
    public static String toString(final TreeModel tree)
    {return toString(tree, tree.getRoot());}

    /**
     * Retourne une chaîne de caractères qui contiendra une
     * représentation graphique de l'arborescence spécifiée.
     * Cette arborescence apparaître correctement si elle
     * est écrite avec une police mono-espacée.
     *
     * @param  node Noeud à partir d'où écrire l'arborescence.
     * @return Chaîne de caractères représentant l'arborescence.
     */
    public static String toString(final TreeNode node)
    {return toString(new DefaultTreeModel(node, true));}
}
