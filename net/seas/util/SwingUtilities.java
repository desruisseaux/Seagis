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
     * Fait apparaître le contenu d'une composante dans une boîte de dialogue.
     * Cette méthode ne retourne qu'après que l'utilisateur ait cliqué sur "ok"
     * ou sur "annuler". Cette méthode peut être appelé de n'importe quel thread
     * (pas nécessairement celui de <i>Swing</i>).
     *
     * @param  owner  Composante par-dessus laquelle faire apparaître la boîte de dialogue.
     * @param  dialog Le message à faire apparaître.
     * @param  title  Titre de la boîte de dialogue.
     * @return <code>true</code> si l'utilisateur a appuyé sur "ok", et <code>false</code> sinon.
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
                    title,                         // Titre de la boîte de dialogue
                    JOptionPane.OK_CANCEL_OPTION,  // Boutons à placer
                    JOptionPane.PLAIN_MESSAGE,     // Type du message
                    null,                          // Icone
                    null,                          // Liste des boutons
                    null);                         // Bouton par défaut
        }
        else
        {
            choice=JOptionPane.showOptionDialog(
                    owner,                         // Composante parente
                    dialog,                        // Message
                    title,                         // Titre de la boîte de dialogue
                    JOptionPane.OK_CANCEL_OPTION,  // Boutons à placer
                    JOptionPane.PLAIN_MESSAGE,     // Type du message
                    null,                          // Icone
                    null,                          // Liste des boutons
                    null);                         // Bouton par défaut
        }
        return JOptionPane.OK_OPTION==choice;
    }

    /**
     * Fait apparaître un message dans une boîte de dialogue. Cette méthode
     * ne retourne qu'après que l'utilisateur ait cliqué sur "ok"
     *
     * @param owner   Composante par-dessus laquelle faire apparaître la boîte de dialogue.
     * @param message Message à afficher.
     * @param title   Titre de la boîte de dialogue.
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
                    title,     // Titre de la boîte de dialogue
                    type);     // Type du message
        }
        else
        {
            JOptionPane.showMessageDialog(
                    owner,     // Composante parente
                    message,   // Message
                    title,     // Titre de la boîte de dialogue
                    type);     // Type du message
        }
    }

    /**
     * Fait apparaître une question dans une boîte de dialogue. Cette méthode
     * ne retourne qu'après que l'utilisateur ait cliqué sur "oui" ou "non".
     *
     * @param owner   Composante par-dessus laquelle faire apparaître la boîte de dialogue.
     * @param message Message à afficher.
     * @param title   Titre de la boîte de dialogue.
     * @param type    Type du message
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} ou
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     * @return <code>true</code> si l'utilisateur a cliqué sur "Oui",
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
                    title,                     // Titre de la boîte de dialogue
                    JOptionPane.YES_NO_OPTION, // Boutons à faire apparaître
                    type);                     // Type du message
        }
        else
        {
            choice=JOptionPane.showConfirmDialog(
                    owner,                     // Composante parente
                    message,                   // Message
                    title,                     // Titre de la boîte de dialogue
                    JOptionPane.YES_NO_OPTION, // Boutons à faire apparaître
                    type);                     // Type du message
        }
        return choice==JOptionPane.YES_OPTION;
    }

    /**
     * Retourne une étiquette pour la composante spécifiée.
     * Le texte de l'étiquette pourra éventuellement être
     * distribué sur plusieurs lignes.
     *
     * @param owner Composante pour laquelle on construit une étiquette.
     *              L'étiquette aura la même largeur que <code>owner</code>.
     * @param text  Texte à placer dans l'étiquette.
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
     * Exécute <code>runnable</code> dans le thread de <i>Swing</i>,
     * et attend que son exécution se soit terminée avant de retourner.
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
