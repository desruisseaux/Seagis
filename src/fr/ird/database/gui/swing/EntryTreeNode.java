package fr.ird.database.gui.swing;

// GEOTOOLS.
import org.geotools.gui.swing.tree.DefaultMutableTreeNode;

// SEAGIS.
import fr.ird.database.Entry;

// J2SE.
import java.rmi.RemoteException;

/**
 * Sur-définition de la méthode <CODE>toString()</CODE>.
 *
 * @author Remi Eve
 * @version 1.0
 *
 * @todo Should not be public.
 */
public class EntryTreeNode extends DefaultMutableTreeNode {
    
    /**
     * Creates a tree node that has no parent and no children, but which
     * allows children.
     */
    public EntryTreeNode() {
        super();
    }

    /**
     * Creates a tree node with no parent, no children, but which allows
     * children, and initializes it with the specified user object.
     *
     * @param userObject an Object provided by the user that constitutes
     *                   the node's data
     */
    public EntryTreeNode(Object userObject) {
        super(userObject);
    }

    /**
     * Creates a tree node with no parent, no children, initialized with
     * the specified user object, and that allows children only if
     * specified.
     *
     * @param userObject an Object provided by the user that constitutes
     *        the node's data
     * @param allowsChildren if true, the node is allowed to have child
     *        nodes -- otherwise, it is always a leaf node
     */
    public EntryTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }
    
    /**
     * Retourne la valeur de l'EntryTreeNode.
     */
    public String toString() {
        try {
            return ((Entry)userObject).getName();
        } catch (RemoteException e) {
            throw new IllegalArgumentException(e);
        }
    }
}