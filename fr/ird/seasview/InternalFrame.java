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
package fr.ird.seasview;

// Interface utilisateur
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Divers
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.sql.SQLException;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Classe de base des fenêtres internes de l'application. Ces
 * fenêtres seront placées dans un objet {@link Desktop}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class InternalFrame extends JInternalFrame
{
    /**
     * Ensemble d'icônes qui ont déjà été retournés par cette {@link #getIcon}.
     * Ces icônes sont conservés en mémoire afin de les réutiliser si possible.
     */
    private static Map<String,Icon> icons;

    /**
     * Construit une fenêtre interne. La fenêtre pourra
     * être redimensionnée, fermée, maximisée ou iconifiée.
     *
     * @param title titre de la fenêtre.
     */
    public InternalFrame(final String title)
    {super(title, /*resizable*/true, /*closable*/true, /*maximizable*/true, /*iconifiable*/true);}

    /**
     * Indique si cette fenêtre peut traiter l'opération désignée par le code spécifié.
     * Le code <code>clé</code> désigne une des constantes de l'interface {@link ResourceKeys},
     * utilisé pour la construction des menus. Par exemple le code {@link ResourceKeys#EXPORT}
     * désigne le menu "exporter". Voyez le code source de {@link MainFrame} pour voir
     * la liste des codes utilisés.
     */
    protected boolean canProcess(final int clé)
    {
        switch (clé)
        {
            default:                 return false;
            case ResourceKeys.CLOSE: return true;
            // TODO: VERIFIER SI ON EST AUTORISE A FERMER LA FENETRE!!!!
            //       Il faudrait en particulier vérifier s'il n'y a pas
            //       un thread en cours d'exécution en arrière-plan.
        }
    }

    /**
     * Exécute une action. Le code <code>clé</code> de l'action est le même
     * que celui qui avait été préalablement transmis à {@link #canProcess}.
     * Si le code de l'action n'est pas reconnue, une exception sera lancée.
     * Les classes dérivées devraient redéfinir cette méthode en traitant
     * d'abord les clés qu'elles reconnaissent, puis en appelant <code>super.process(clé)</code>
     * pour les clés qu'elles ne reconnaissent pas. L'implémentation par
     * défaut reconnait les clés suivantes:
     *
     * <ul>
     *   <li>{@link ResourceKeys#CLOSE}</li>
     * </ul>
     *
     * @return Tache à exécuter en arrière plan pour continuer l'action, ou
     *         <code>null</code> si l'action est terminée.
     * @throws SQLException si une interrogation de la base de données était
     *         nécessaire et a échouée.
     */
    protected Task process(final int clé) throws SQLException
    {
        switch (clé)
        {
            default:
            {
                throw new IllegalArgumentException(String.valueOf(clé));
            }
            case ResourceKeys.CLOSE:
            {
                final JDesktopPane desktop=getDesktopPane();
                if (desktop!=null) desktop.remove(this);
                dispose();
                break;
            }
        }
        return null;
    }

    /**
     * Retourne le fuseau horaire du bureau. Il ne s'agit pas nécessairement
     * du fuseau horaire local du client, ni du fuseau horaire de la base de
     * données.
     */
    protected final TimeZone getTimeZone()
    {
        final JDesktopPane desktop=getDesktopPane();
        if (desktop instanceof Desktop)
            return ((Desktop) desktop).getTimeZone();
        else return TimeZone.getDefault();
    }

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre. L'implémentation par
     * défaut ne fait rien.
     */
    protected void setTimeZone(final TimeZone timezone)
    {}

    /**
     * Indique si les cartes doivent être redessinées
     * durant les glissements des ascenceurs.
     */
    protected final boolean isPaintingWhileAdjusting()
    {
        final JDesktopPane desktop=getDesktopPane();
        return (desktop!=null) && (desktop.getDragMode()==JDesktopPane.LIVE_DRAG_MODE);
    }

    /**
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected void setPaintingWhileAdjusting(final boolean s)
    {}

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqué sur une image d'une
     * mosaïque doit être répliqué sur les autres. L'implémentation par
     * défaut ne fait rien.
     */
    protected void setImagesSynchronized(final boolean s)
    {}

    /**
     * Prévient le bureau que l'état de cette fenêtre a changé.
     * Ca aura pour effet de changer par exemple l'état des boutons.
     */
    protected final void stateChanged()
    {
        final JDesktopPane desktop=getDesktopPane();
        if (desktop instanceof Desktop)
            ((Desktop) desktop).stateChanged();
    }

    /**
     * Affiche en avertissement. Cette méthode peut être appelée
     * à partir de n'importe quel thread (pas nécessairement celui
     * de <i>Swing</i>).
     *
     * @param source  Source de l'avertissement. Il peut s'agir
     *                par exemple du nom du fichier d'une image.
     * @param message Message à afficher.
     */
    public final void warning(final String source, final String message)
    {
        final JDesktopPane desktop=getDesktopPane();
        if (desktop instanceof Desktop)
        {
            ((Desktop) desktop).warning(source, message);
        }
        else
        {
            final LogRecord record = new LogRecord(Level.WARNING, message);
            DataBase.logger.log(record);
        }
    }

    /**
     * Retourne un icône. Si cet icône avait déjà été retourné
     * pour une autre fenêtre, l'ancien icône sera réutilisé.
     *
     * @param name Nom et chemin de l'icône.
     */
    public static synchronized Icon getIcon(final String name)
    {
        if (icons==null)
        {
            icons=new HashMap<String,Icon>();
        }
        Icon icon = icons.get(name);
        if (icon==null)
        {
            icon = new ImageIcon(InternalFrame.class.getClassLoader().getResource(name));
            icons.put(name, icon);
        }
        return icon;
    }

    /**
     * Retourne la base de données du bureau. Si cette fenêtre n'a pas
     * encore été insérée dans un bureau, alors cette méthode retourne
     * <code>null</code>.
     */
    protected final DataBase getDataBase()
    {
        final JDesktopPane desktop=getDesktopPane();
        if (desktop instanceof Desktop)
            return ((Desktop) desktop).getDataBase();
        else return null;
    }

    /**
     * Retourne un modèle représentant les données de cette fenêtre.
     * Ce modèle doit pouvoir être enregistré et lu en binaire, et
     * permettre la création de nouvelles fenêtres par un appel de
     * la méthode {@link Task#run()}. L'implémentation par défaut
     * retourne toujours <code>null</code>.
     *
     * @return Object contenant les données à enregistrer, ou
     *         <code>null</code> si cette fenêtre ne contient
     *         pas de données à enregistrer.
     */
    protected Task getSerializable()
    {return null;}
}
