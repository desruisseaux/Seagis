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

// Swing et AWT
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

// Divers
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;

// Geotools dependencies
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Classe des actions correspondante à différents menu.  Ces actions peuvent
 * avoir un équivalent sur la barre des boutons. De façon facultative, elles
 * peuvent être activées ou désactivées en fonction de la fenêtre interne
 * {@link InternalFrame} qui a le focus. Cette dernière fonctionalité est
 * gérée par {@link Desktop}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Action extends AbstractAction
{
    /**
     * The property key for icon's name.
     */
    private static final String ICON_NAME = "IconName";

    /**
     * Bureau qui exécutera cette action.  L'action sera exécutée en
     * appelant {@link Desktop#process}. Cette information est aussi
     * utilisée par {@link Task} afin de savoir où placer les fenêtres
     * qui auront éventuellement été créées par cette action.
     */
    private final Desktop desktop;

    /**
     * Code identifiant cette action. Ce code est l'une des constantes de
     * l'interface {@link ResourceKeys}, utilisé pour la construction des menus.
     * Par exemple le code {@link ResourceKeys#EXPORT} désigne le menu "exporter".
     */
    private final int clé;

    /**
     * Indique si cette action est occupée. Une action occupée est ausi
     * désactivée (<code>enabled==false</code>). Toutefois, une action
     * désactivée n'est pas nécessairement occupée. Cette distinction
     * est utile pour {@link Desktop}.
     */
    private transient boolean busy;

    /**
     * Construit une action avec le code spécifié.
     *
     * @param desktop  Bureau qui exécutera (avec {@link Desktop#process}) cette action.
     * @param clé      Code identifiant l'action pour la méthode {@link #dispatch}.
     * @param trailing <code>true</code> s'il faut placer "..." après l'étiquette du menu.
     */
    public Action(final Desktop desktop, final int clé, final boolean trailing)
    {this(desktop, clé, format(desktop, clé, trailing));}

    /**
     * Construit une action avec le code spécifié.
     *
     * @param desktop Bureau qui exécutera (avec {@link Desktop#process}) cette action.
     * @param clé     Code identifiant l'action pour la méthode {@link #dispatch}.
     * @param name    Nom de l'action. Ce nom apparaîtra dans le menu.
     */
    public Action(final Desktop desktop, final int clé, final String name)
    {
        super(name);
        this.desktop = desktop;
        this.clé     = clé;
    }

    /**
     * Returns the resources string for the specified key.
     */
    private static String format(final Desktop desktop, final int clé, final boolean trailing)
    {
        final Resources resources = Resources.getResources(desktop.getLocale());
        return trailing ? resources.getMenuLabel(clé) : resources.getString(clé);
    }

    /**
     * Returns an icon.
     */
    private final Icon getIcon(final String icon, final String suffix)
    {return new ImageIcon(getClass().getClassLoader().getResource("toolbarButtonGraphics/"+icon+suffix));}

    /**
     * Définit l'icone de cette action.
     *
     * @param icon Nom de l'icone à utiliser (par exemple "general/SaveAs").
     *             Ce nom ne comprend pas le chemin "toolbarButtonGraphics/"
     *             ni l'extension "16.gif" ou "24.gif".
     */
    protected final void setIcon(final String icon)
    {
        putValue(SMALL_ICON, getIcon(icon, "24.gif"));
        putValue(ICON_NAME, icon);
    }

    /**
     * Définit le texte à afficher lorsque la souris
     * traine sur le bouton de cette action.
     */
    protected final void setToolTipText(final int clé)
    {putValue(SHORT_DESCRIPTION, Resources.getResources(desktop.getLocale()).getString(clé));}

    /**
     * Définit la touche activant cette action dans l'ensemble de l'application.
     */
    protected final void setAccelerator(final int keyCode, final int modifiers)
    {putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, modifiers));}

    /**
     * Définit le caractère à souligner dans le menu.
     */
    protected final void setMnemonicKey(final int mnemonicKey)
    {putValue(MNEMONIC_KEY, new Integer(mnemonicKey));}

    /**
     * Retourne une des constantes de l'interface {@link ResourceKeys} utilisé pour la construction
     * des actions. Par exemple le code {@link ResourceKeys#EXPORT} désigne le menu "exporter". Ce
     * code sera transmis aux différentes fenêtres {@link InternalFrame} pour vérifier si
     * une fenêtre peut gérer cette action. Voyez le code source de {@link MainFrame} pour
     * voir la liste des codes utilisés.
     */
    public final int getCommandKey()
    {return clé;}

    /**
     * Retourne le bureau qui exécutera cette action. L'action sera exécutée en
     * appelant {@link Desktop#process}. Cette information est aussi utilisée par
     * {@link Task} afin de savoir où placer les fenêtres qui auront éventuellement
     * été créées par cette action.
     */
    public final Desktop getDesktop()
    {return desktop;}

    /**
     * Ajoute cette action dans le menu spécifié.
     */
    final void addTo(final JMenu menu)
    {
        final JMenuItem item=new JMenuItem(this);
        // On pourrait placer le petit icône (16x16) plutôt que le gros (24x24).
        // Ca parait bien, mais le texte ne reste malheureusement pas aligné.
        final String icon = (String) getValue(ICON_NAME);
        if (icon!=null)
        {
            item.setIcon(getIcon(icon, "16.gif"));
        }
        menu.add(item);
    }

    /**
     * Ajoute cette action dans le menu ainsi
     * que dans la barre d'outils spécifiés.
     */
    final void addTo(final JMenu menu, final JToolBar toolBar)
    {
        if (menu!=null) addTo(menu);
        final JButton button=new JButton(this);
        button.setText(null);
        toolBar.add(button);
    }

    /**
     * Active ou désactive cette action. Une action activée sera automatiquement
     * considéré comme n'étant plus occupée (<code>busy==false</code>). Mais une
     * action désactivée ne sera pas nécessairement considérée comme occupée.
     */
    public synchronized void setEnabled(final boolean enabled)
    {
        if (enabled) busy=false;
        super.setEnabled(enabled);
    }

    /**
     * Indique si cette action est déjà occupée. Une action occupée est
     * généralement désactivée (<code>getEnabled()==false</code>).   En
     * revanche, une action désactivée n'est pas nécessairement occupée.
     * Une action est considérée "occupée" pendant au moins la durée de
     * l'exécution de {@link #actionPerformed}.
     */
    final boolean isBusy()
    {return busy;}

    /**
     * Méthode appelée automatiquement lorsque l'action est exécutée.
     * Cette méthode appelle {@link Desktop#process} avec en argument
     * le code {@link #getCommandKey}.  Durant l'exécution, les menus
     * et boutons de cette action seront désactivés. Si une exception
     * est lancée, elle sera attrapée et affichée dans une boîte de
     * dialogue sur le bureau.
     */
    public synchronized void actionPerformed(final ActionEvent event)
    {
        if (isEnabled()) try
        {
            busy=true;
            setEnabled(false);
            final Task task=desktop.process(clé);
            if (task==null)
            {
                setEnabled(true);
                desktop.stateChanged();
            }
            else task.run(this); // Appelera 'setEnabled(true)' plus tard.
        }
        catch (Throwable exception)
        {
            setEnabled(true);
            desktop.stateChanged();
            ExceptionMonitor.show(desktop, exception);
        }
    }
}
