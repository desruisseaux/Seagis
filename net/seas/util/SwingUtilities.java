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
package net.seas.util;

// Interface utilisateur
import java.awt.Color;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.EventQueue;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;


/**
 * A collection of utility methods for Swing.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class SwingUtilities
{
    /**
     * Do not allow any instance
     * of this class to be created.
     */
    private SwingUtilities()
    {}

    /**
     * Fait appara�tre le contenu d'une composante dans une bo�te de dialogue.
     * Cette m�thode ne retourne qu'apr�s que l'utilisateur ait cliqu� sur "ok"
     * ou sur "annuler". Cette m�thode peut �tre appel� de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param  owner  Composante par-dessus laquelle faire appara�tre la bo�te de dialogue.
     * @param  dialog Le message � faire appara�tre.
     * @param  title  Titre de la bo�te de dialogue.
     * @return <code>true</code> si l'utilisateur a appuy� sur "ok", et <code>false</code> sinon.
     */
    public static boolean showOptionDialog(final Component owner, final Object dialog, final String title)
    {
        if (!EventQueue.isDispatchThread())
        {
            final boolean result[]=new boolean[1];
            invokeAndWait(new Runnable()
            {
                public void run()
                {result[0]=showOptionDialog(owner, dialog, title);}
            });
            return result[0];
        }
        final int choice;
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null)
        {
            choice=JOptionPane.showInternalOptionDialog(
                    owner,                         // Composante parente
                    dialog,                        // Message
                    title,                         // Titre de la bo�te de dialogue
                    JOptionPane.OK_CANCEL_OPTION,  // Boutons � placer
                    JOptionPane.PLAIN_MESSAGE,     // Type du message
                    null,                          // Icone
                    null,                          // Liste des boutons
                    null);                         // Bouton par d�faut
        }
        else
        {
            choice=JOptionPane.showOptionDialog(
                    owner,                         // Composante parente
                    dialog,                        // Message
                    title,                         // Titre de la bo�te de dialogue
                    JOptionPane.OK_CANCEL_OPTION,  // Boutons � placer
                    JOptionPane.PLAIN_MESSAGE,     // Type du message
                    null,                          // Icone
                    null,                          // Liste des boutons
                    null);                         // Bouton par d�faut
        }
        return JOptionPane.OK_OPTION==choice;
    }

    /**
     * Fait appara�tre un message dans une bo�te de dialogue. Cette m�thode
     * ne retourne qu'apr�s que l'utilisateur ait cliqu� sur "ok"
     *
     * @param owner   Composante par-dessus laquelle faire appara�tre la bo�te de dialogue.
     * @param message Message � afficher.
     * @param title   Titre de la bo�te de dialogue.
     * @param type    Type du message
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} ou
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     */
    public static void showMessageDialog(final Component owner, final Object message, final String title, final int type)
    {
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null)
        {
            JOptionPane.showInternalMessageDialog(
                    owner,     // Composante parente
                    message,   // Message
                    title,     // Titre de la bo�te de dialogue
                    type);     // Type du message
        }
        else
        {
            JOptionPane.showMessageDialog(
                    owner,     // Composante parente
                    message,   // Message
                    title,     // Titre de la bo�te de dialogue
                    type);     // Type du message
        }
    }

    /**
     * Fait appara�tre une question dans une bo�te de dialogue. Cette m�thode
     * ne retourne qu'apr�s que l'utilisateur ait cliqu� sur "oui" ou "non".
     *
     * @param owner   Composante par-dessus laquelle faire appara�tre la bo�te de dialogue.
     * @param message Message � afficher.
     * @param title   Titre de la bo�te de dialogue.
     * @param type    Type du message
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} ou
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     * @return <code>true</code> si l'utilisateur a cliqu� sur "Oui",
     *         <code>false</code> sinon.
     */
    public static boolean showConfirmDialog(final Component owner, final Object message, final String title, final int type)
    {
        final int choice;
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null)
        {
            choice=JOptionPane.showInternalConfirmDialog(
                    owner,                     // Composante parente
                    message,                   // Message
                    title,                     // Titre de la bo�te de dialogue
                    JOptionPane.YES_NO_OPTION, // Boutons � faire appara�tre
                    type);                     // Type du message
        }
        else
        {
            choice=JOptionPane.showConfirmDialog(
                    owner,                     // Composante parente
                    message,                   // Message
                    title,                     // Titre de la bo�te de dialogue
                    JOptionPane.YES_NO_OPTION, // Boutons � faire appara�tre
                    type);                     // Type du message
        }
        return choice==JOptionPane.YES_OPTION;
    }

    /**
     * Retourne une �tiquette pour la composante sp�cifi�e.
     * Le texte de l'�tiquette pourra �ventuellement �tre
     * distribu� sur plusieurs lignes.
     *
     * @param owner Composante pour laquelle on construit une �tiquette.
     *              L'�tiquette aura la m�me largeur que <code>owner</code>.
     * @param text  Texte � placer dans l'�tiquette.
     */
    public static JComponent getMultilineLabelFor(final JComponent owner, final String text)
    {
        final JTextArea label=new JTextArea(text);
        final Dimension size=owner.getPreferredSize();
        size.height=label.getMaximumSize().height;
        label.setMaximumSize  (size);
        label.setWrapStyleWord(true);
        label.setLineWrap     (true);
        label.setEditable    (false);
        label.setFocusable   (false);
        label.setOpaque      (false);
        label.setBorder       (null); // Certains L&F placent une bordure.
        LookAndFeel.installColorsAndFont(label, "Label.background", "Label.foreground", "Label.font");
        return label;
    }

    /**
     * Ex�cute <code>runnable</code> dans le thread de <i>Swing</i>,
     * et attend que son ex�cution se soit termin�e avant de retourner.
     */
    public static void invokeAndWait(final Runnable runnable)
    {
        try
        {
            EventQueue.invokeAndWait(runnable);
        }
        catch (InterruptedException exception)
        {
            // Someone don't want to let us sleep. Go back to work.
        }
        catch (InvocationTargetException target)
        {
            final Throwable exception=target.getTargetException();
            if (exception instanceof RuntimeException)
                throw (RuntimeException) exception;
            if (exception instanceof Error)
                throw (Error) exception;
            // Should not happen, since {@link Runnable#run} do not allow checked exception.
            throw new UndeclaredThrowableException(exception, exception.getLocalizedMessage());
        }
    }
}
