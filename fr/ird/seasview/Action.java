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
 * Classe des actions correspondante � diff�rents menu.  Ces actions peuvent
 * avoir un �quivalent sur la barre des boutons. De fa�on facultative, elles
 * peuvent �tre activ�es ou d�sactiv�es en fonction de la fen�tre interne
 * {@link InternalFrame} qui a le focus. Cette derni�re fonctionalit� est
 * g�r�e par {@link Desktop}.
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
     * Bureau qui ex�cutera cette action.  L'action sera ex�cut�e en
     * appelant {@link Desktop#process}. Cette information est aussi
     * utilis�e par {@link Task} afin de savoir o� placer les fen�tres
     * qui auront �ventuellement �t� cr��es par cette action.
     */
    private final Desktop desktop;

    /**
     * Code identifiant cette action. Ce code est l'une des constantes de
     * l'interface {@link ResourceKeys}, utilis� pour la construction des menus.
     * Par exemple le code {@link ResourceKeys#EXPORT} d�signe le menu "exporter".
     */
    private final int cl�;

    /**
     * Indique si cette action est occup�e. Une action occup�e est ausi
     * d�sactiv�e (<code>enabled==false</code>). Toutefois, une action
     * d�sactiv�e n'est pas n�cessairement occup�e. Cette distinction
     * est utile pour {@link Desktop}.
     */
    private transient boolean busy;

    /**
     * Construit une action avec le code sp�cifi�.
     *
     * @param desktop  Bureau qui ex�cutera (avec {@link Desktop#process}) cette action.
     * @param cl�      Code identifiant l'action pour la m�thode {@link #dispatch}.
     * @param trailing <code>true</code> s'il faut placer "..." apr�s l'�tiquette du menu.
     */
    public Action(final Desktop desktop, final int cl�, final boolean trailing)
    {this(desktop, cl�, format(desktop, cl�, trailing));}

    /**
     * Construit une action avec le code sp�cifi�.
     *
     * @param desktop Bureau qui ex�cutera (avec {@link Desktop#process}) cette action.
     * @param cl�     Code identifiant l'action pour la m�thode {@link #dispatch}.
     * @param name    Nom de l'action. Ce nom appara�tra dans le menu.
     */
    public Action(final Desktop desktop, final int cl�, final String name)
    {
        super(name);
        this.desktop = desktop;
        this.cl�     = cl�;
    }

    /**
     * Returns the resources string for the specified key.
     */
    private static String format(final Desktop desktop, final int cl�, final boolean trailing)
    {
        final Resources resources = Resources.getResources(desktop.getLocale());
        return trailing ? resources.getMenuLabel(cl�) : resources.getString(cl�);
    }

    /**
     * Returns an icon.
     */
    private final Icon getIcon(final String icon, final String suffix)
    {return new ImageIcon(getClass().getClassLoader().getResource("toolbarButtonGraphics/"+icon+suffix));}

    /**
     * D�finit l'icone de cette action.
     *
     * @param icon Nom de l'icone � utiliser (par exemple "general/SaveAs").
     *             Ce nom ne comprend pas le chemin "toolbarButtonGraphics/"
     *             ni l'extension "16.gif" ou "24.gif".
     */
    protected final void setIcon(final String icon)
    {
        putValue(SMALL_ICON, getIcon(icon, "24.gif"));
        putValue(ICON_NAME, icon);
    }

    /**
     * D�finit le texte � afficher lorsque la souris
     * traine sur le bouton de cette action.
     */
    protected final void setToolTipText(final int cl�)
    {putValue(SHORT_DESCRIPTION, Resources.getResources(desktop.getLocale()).getString(cl�));}

    /**
     * D�finit la touche activant cette action dans l'ensemble de l'application.
     */
    protected final void setAccelerator(final int keyCode, final int modifiers)
    {putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, modifiers));}

    /**
     * D�finit le caract�re � souligner dans le menu.
     */
    protected final void setMnemonicKey(final int mnemonicKey)
    {putValue(MNEMONIC_KEY, new Integer(mnemonicKey));}

    /**
     * Retourne une des constantes de l'interface {@link ResourceKeys} utilis� pour la construction
     * des actions. Par exemple le code {@link ResourceKeys#EXPORT} d�signe le menu "exporter". Ce
     * code sera transmis aux diff�rentes fen�tres {@link InternalFrame} pour v�rifier si
     * une fen�tre peut g�rer cette action. Voyez le code source de {@link MainFrame} pour
     * voir la liste des codes utilis�s.
     */
    public final int getCommandKey()
    {return cl�;}

    /**
     * Retourne le bureau qui ex�cutera cette action. L'action sera ex�cut�e en
     * appelant {@link Desktop#process}. Cette information est aussi utilis�e par
     * {@link Task} afin de savoir o� placer les fen�tres qui auront �ventuellement
     * �t� cr��es par cette action.
     */
    public final Desktop getDesktop()
    {return desktop;}

    /**
     * Ajoute cette action dans le menu sp�cifi�.
     */
    final void addTo(final JMenu menu)
    {
        final JMenuItem item=new JMenuItem(this);
        // On pourrait placer le petit ic�ne (16x16) plut�t que le gros (24x24).
        // Ca parait bien, mais le texte ne reste malheureusement pas align�.
        final String icon = (String) getValue(ICON_NAME);
        if (icon!=null)
        {
            item.setIcon(getIcon(icon, "16.gif"));
        }
        menu.add(item);
    }

    /**
     * Ajoute cette action dans le menu ainsi
     * que dans la barre d'outils sp�cifi�s.
     */
    final void addTo(final JMenu menu, final JToolBar toolBar)
    {
        if (menu!=null) addTo(menu);
        final JButton button=new JButton(this);
        button.setText(null);
        toolBar.add(button);
    }

    /**
     * Active ou d�sactive cette action. Une action activ�e sera automatiquement
     * consid�r� comme n'�tant plus occup�e (<code>busy==false</code>). Mais une
     * action d�sactiv�e ne sera pas n�cessairement consid�r�e comme occup�e.
     */
    public synchronized void setEnabled(final boolean enabled)
    {
        if (enabled) busy=false;
        super.setEnabled(enabled);
    }

    /**
     * Indique si cette action est d�j� occup�e. Une action occup�e est
     * g�n�ralement d�sactiv�e (<code>getEnabled()==false</code>).   En
     * revanche, une action d�sactiv�e n'est pas n�cessairement occup�e.
     * Une action est consid�r�e "occup�e" pendant au moins la dur�e de
     * l'ex�cution de {@link #actionPerformed}.
     */
    final boolean isBusy()
    {return busy;}

    /**
     * M�thode appel�e automatiquement lorsque l'action est ex�cut�e.
     * Cette m�thode appelle {@link Desktop#process} avec en argument
     * le code {@link #getCommandKey}.  Durant l'ex�cution, les menus
     * et boutons de cette action seront d�sactiv�s. Si une exception
     * est lanc�e, elle sera attrap�e et affich�e dans une bo�te de
     * dialogue sur le bureau.
     */
    public synchronized void actionPerformed(final ActionEvent event)
    {
        if (isEnabled()) try
        {
            busy=true;
            setEnabled(false);
            final Task task=desktop.process(cl�);
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
