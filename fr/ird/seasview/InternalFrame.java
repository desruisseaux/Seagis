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
 * Classe de base des fen�tres internes de l'application. Ces
 * fen�tres seront plac�es dans un objet {@link Desktop}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class InternalFrame extends JInternalFrame
{
    /**
     * Ensemble d'ic�nes qui ont d�j� �t� retourn�s par cette {@link #getIcon}.
     * Ces ic�nes sont conserv�s en m�moire afin de les r�utiliser si possible.
     */
    private static Map<String,Icon> icons;

    /**
     * Construit une fen�tre interne. La fen�tre pourra
     * �tre redimensionn�e, ferm�e, maximis�e ou iconifi�e.
     *
     * @param title titre de la fen�tre.
     */
    public InternalFrame(final String title)
    {super(title, /*resizable*/true, /*closable*/true, /*maximizable*/true, /*iconifiable*/true);}

    /**
     * Indique si cette fen�tre peut traiter l'op�ration d�sign�e par le code sp�cifi�.
     * Le code <code>cl�</code> d�signe une des constantes de l'interface {@link ResourceKeys},
     * utilis� pour la construction des menus. Par exemple le code {@link ResourceKeys#EXPORT}
     * d�signe le menu "exporter". Voyez le code source de {@link MainFrame} pour voir
     * la liste des codes utilis�s.
     */
    protected boolean canProcess(final int cl�)
    {
        switch (cl�)
        {
            default:                 return false;
            case ResourceKeys.CLOSE: return true;
            // TODO: VERIFIER SI ON EST AUTORISE A FERMER LA FENETRE!!!!
            //       Il faudrait en particulier v�rifier s'il n'y a pas
            //       un thread en cours d'ex�cution en arri�re-plan.
        }
    }

    /**
     * Ex�cute une action. Le code <code>cl�</code> de l'action est le m�me
     * que celui qui avait �t� pr�alablement transmis � {@link #canProcess}.
     * Si le code de l'action n'est pas reconnue, une exception sera lanc�e.
     * Les classes d�riv�es devraient red�finir cette m�thode en traitant
     * d'abord les cl�s qu'elles reconnaissent, puis en appelant <code>super.process(cl�)</code>
     * pour les cl�s qu'elles ne reconnaissent pas. L'impl�mentation par
     * d�faut reconnait les cl�s suivantes:
     *
     * <ul>
     *   <li>{@link ResourceKeys#CLOSE}</li>
     * </ul>
     *
     * @return Tache � ex�cuter en arri�re plan pour continuer l'action, ou
     *         <code>null</code> si l'action est termin�e.
     * @throws SQLException si une interrogation de la base de donn�es �tait
     *         n�cessaire et a �chou�e.
     */
    protected Task process(final int cl�) throws SQLException
    {
        switch (cl�)
        {
            default:
            {
                throw new IllegalArgumentException(String.valueOf(cl�));
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
     * Retourne le fuseau horaire du bureau. Il ne s'agit pas n�cessairement
     * du fuseau horaire local du client, ni du fuseau horaire de la base de
     * donn�es.
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
     * Cette modification n'affecte pas le fuseau horaire des �ventuelles
     * bases de donn�es acc�d�es par cette fen�tre. L'impl�mentation par
     * d�faut ne fait rien.
     */
    protected void setTimeZone(final TimeZone timezone)
    {}

    /**
     * Indique si les cartes doivent �tre redessin�es
     * durant les glissements des ascenceurs.
     */
    protected final boolean isPaintingWhileAdjusting()
    {
        final JDesktopPane desktop=getDesktopPane();
        return (desktop!=null) && (desktop.getDragMode()==JDesktopPane.LIVE_DRAG_MODE);
    }

    /**
     * Sp�cifie si les cartes doivent �tre redessin�es
     * durant les glissements des ascenceurs. Sp�cifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected void setPaintingWhileAdjusting(final boolean s)
    {}

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqu� sur une image d'une
     * mosa�que doit �tre r�pliqu� sur les autres. L'impl�mentation par
     * d�faut ne fait rien.
     */
    protected void setImagesSynchronized(final boolean s)
    {}

    /**
     * Pr�vient le bureau que l'�tat de cette fen�tre a chang�.
     * Ca aura pour effet de changer par exemple l'�tat des boutons.
     */
    protected final void stateChanged()
    {
        final JDesktopPane desktop=getDesktopPane();
        if (desktop instanceof Desktop)
            ((Desktop) desktop).stateChanged();
    }

    /**
     * Affiche en avertissement. Cette m�thode peut �tre appel�e
     * � partir de n'importe quel thread (pas n�cessairement celui
     * de <i>Swing</i>).
     *
     * @param source  Source de l'avertissement. Il peut s'agir
     *                par exemple du nom du fichier d'une image.
     * @param message Message � afficher.
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
     * Retourne un ic�ne. Si cet ic�ne avait d�j� �t� retourn�
     * pour une autre fen�tre, l'ancien ic�ne sera r�utilis�.
     *
     * @param name Nom et chemin de l'ic�ne.
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
     * Retourne la base de donn�es du bureau. Si cette fen�tre n'a pas
     * encore �t� ins�r�e dans un bureau, alors cette m�thode retourne
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
     * Retourne un mod�le repr�sentant les donn�es de cette fen�tre.
     * Ce mod�le doit pouvoir �tre enregistr� et lu en binaire, et
     * permettre la cr�ation de nouvelles fen�tres par un appel de
     * la m�thode {@link Task#run()}. L'impl�mentation par d�faut
     * retourne toujours <code>null</code>.
     *
     * @return Object contenant les donn�es � enregistrer, ou
     *         <code>null</code> si cette fen�tre ne contient
     *         pas de donn�es � enregistrer.
     */
    protected Task getSerializable()
    {return null;}
}
