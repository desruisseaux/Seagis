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

// Graphisme, polices et g�om�trie
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Graphics2D;

// Interface utilisateur
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JDesktopPane;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.AbstractButton;

// Ev�nements
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.EventQueue;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Entr�s/sorties
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import net.seagis.io.ExpandedTabWriter;

// Collections
import java.util.List;
import java.util.ArrayList;

// Divers
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import net.seagis.resources.Utilities;
import net.seas.resources.Resources;


/**
 * Utilitaire facilitant l'affichage de la trace d'une exception dans une composante
 * <i>Swing</i>. La classe {@link java.lang.Exception} standard contient des m�thodes
 * qui �crivent la trace de l'exception vers le p�riph�rique d'erreur.   Cette classe
 * <code>ExceptionMonitor</code> ajoute des m�thodes statiques qui font appara�tre le
 * message et �ventuellement la trace d'une exception dans une composante visuelle.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/ExceptionMonitor.png"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class ExceptionMonitor
{
    /**
     * Nombre d'espaces � laisser entre chaque taquets de tabulation.
     */
    private static final int TAB_WIDTH=4;

    /**
     * Interdit la cr�ation d'objets de
     * classe <code>ExceptionMonitor</code>.
     */
    private ExceptionMonitor()
    {}

    /**
     * Affiche un message d'erreur pour l'exception <code>exception</code> sp�cifi�e.
     * Notez que cette m�thode peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param owner Composante dans laquelle l'exception s'est produite,
     *        ou <code>null</code> si cette composante n'est pas connue.
     * @param exception Exception qui a �t� lanc�e et que l'on souhaite
     *        reporter � l'utilisateur.
     */
    public static void show(final Component owner, final Throwable exception)
    {show(owner, exception, null);}

    /**
     * Affiche un message d'erreur pour l'exception <code>exception</code> sp�cifi�e.
     * Notez que cette m�thode peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param owner Composante dans laquelle l'exception s'est produite,
     *        ou <code>null</code> si cette composante n'est pas connue.
     * @param exception Exception qui a �t� lanc�e et que l'on souhaite
     *        reporter � l'utilisateur.
     * @param message Message � afficher. Si ce param�tre est nul, alors
     *        {@link Exception#getLocalizedMessage} sera appel�e pour
     *        obtenir le message.
     */
    public static void show(final Component owner, final Throwable exception, final String message)
    {
        final Runnable monitor=new Runnable()
        {
            public void run()
            {
                Pane.show(owner, exception, message);
            }
        };
        if (!EventQueue.isDispatchThread()) try
        {
            EventQueue.invokeAndWait(monitor);
        }
        catch (InterruptedException error)
        {
            // On ne veut pas nous laisser dormir. Retourne au boulot.
        }
        catch (InvocationTargetException error)
        {
            final Throwable e = error.getTargetException();
            if (e instanceof RuntimeException) throw (RuntimeException)     e;
            if (e instanceof Error)            throw (Error)                e;
            Utilities.unexpectedException("net.seas.awt", "ExceptionMonitor", "show", e);
        }
        else monitor.run();
    }

    /**
     * Ecrit la trace de l'exception sp�cifi�e dans le graphique sp�cifi�. Cette m�thode est utile
     * lorsqu'une exception est survenue � l'int�rieur d'une m�thode {@link Component#paint} et
     * qu'on veut �crire cette exception plut�t que de laisser une fen�tre vide.
     *
     * @param exception Exception dont on veut �crire la trace.
     * @param graphics Graphique dans lequel �crire cette exception. Ce graphique doit �tre
     *        dans son �tat initial (transformation affine par d�faut, couleur par d�faut,
     *        etc...)
     * @param widgetBounds Taille de la composante qui �tait en train de se faire dessiner.
     */
    public static void paintStackTrace(final Graphics2D graphics, final Rectangle widgetBounds, final Throwable exception)
    {
        /*
         * Obtient la trace de l'exception sous forme de cha�ne
         * de caract�res. Les retours chariots dans cette cha�ne
         * peuvent �tre "\r", "\n" ou "r\n".
         */
        final String message=printStackTrace(exception);
        /*
         * Examine la cha�ne de caract�res ligne par ligne.
         * On cr�era des "glyphs" au fur et � mesure et on
         * en profitera pour calculer l'espace n�cessaire.
         */
        double width=0, height=0;
        final List<GlyphVector> glyphs=new ArrayList<GlyphVector>();
        final List<Rectangle2D> bounds=new ArrayList<Rectangle2D>();
        final int length=message.length();
        final Font font=graphics.getFont();
        final FontRenderContext context=graphics.getFontRenderContext();
        for (int i=0; i<length;)
        {
            int ir=message.indexOf('\r', i);
            int in=message.indexOf('\n', i);
            if (ir<0) ir=length;
            if (in<0) in=length;
            final int irn=Math.min(ir,in);
            final GlyphVector line=font.createGlyphVector(context, message.substring(i, irn));
            final Rectangle2D rect=line.getVisualBounds();
            final double w=rect.getWidth();
            if (w>width) width=w;
            height += rect.getHeight();
            glyphs.add(line);
            bounds.add(rect);
            i = (Math.abs(ir-in)<=1 ? Math.max(ir,in) : irn)+1;
        }
        /*
         * Proc�de au tra�age de tous les
         * glyphs pr�c�demment calcul�s.
         */
        float xpos = (float) (0.5*(widgetBounds.width -width));
        float ypos = (float) (0.5*(widgetBounds.height-height));
        final int size=glyphs.size();
        for (int i=0; i<size; i++)
        {
            final GlyphVector line = glyphs.get(i);
            final Rectangle2D rect = bounds.get(i);
            ypos += rect.getHeight();
            graphics.drawGlyphVector(line, xpos, ypos);
        }
    }

    /**
     * Retourne la trace d'une exception. Toutes les tabulations
     * auront �t� syst�matiquement remplac�es par 4 espaces blancs.
     */
    private static String printStackTrace(final Throwable exception)
    {
        final StringWriter writer=new StringWriter();
        exception.printStackTrace(new PrintWriter(new ExpandedTabWriter(writer, TAB_WIDTH)));
        return writer.toString();
    }

    /**
     * Classe ayant la charge d'afficher le message d'une exception et �ventuellement
     * sa trace. Le message appara�tra dans une bo�te de dialogue ou dans une fen�tre interne, selon le
     * parent. <strong>Note:</strong> Toutes les m�thodes de cette classe doivent �tre appel�es dans le
     * m�me thread que celui de <i>Swing</i>.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Pane extends JOptionPane implements ActionListener
    {
        /**
         * Largeur par d�faut (en nombre de colonnes) de la composante
         * qui affiche le message ou la trace de l'exception.
         */
        private static final int WIDTH=40;

        /**
         * Hauteur minimale (en pixels) de la bo�te de
         * dialogue lorsqu'elle affiche aussi la trace.
         */
        private static final int HEIGHT=300;
    
        /**
         * Bo�te de dialogue affich�e. Il s'agira d'un objet
         * {@link JDialog} ou d'un objet {@link JInternalFrame}.
         */
        private final Component dialog;

        /**
         * Exception � afficher dans la bo�te de dialogue.
         * La m�thode {@link Throwable#getLocalizedMessage}
         * sera appell�e pour obtenir le message � afficher.
         */
        private final Throwable exception;
    
        /**
         * Bo�te qui contiendra la partie "message" de la bo�te de dialogue
         * construite. Cette bo�te sera agrandie si l'utilisateur demande �
         * voir la trace de l'exception.  Elle disposera les composantes en
         * utilisant {@link BorderLayout}.
         */
        private final Container message;

        /**
         * Composante affichant la trace de l'exception. Au d�part, cette
         * composante sera nulle. Elle ne sera cr��e que si la trace fut
         * demand�e par l'utilisateur.
         */
        private Component trace;

        /**
         * Indique si la trace est pr�sentement visible. La valeur de ce
         * champ sera invers� � chaque fois que l'utilisateur appuie sur
         * le bouton "trace".
         */
        private boolean traceVisible;

        /**
         * Bouton servant � faire appara�tre
         * ou dispara�tre la trace.
         */
        private final AbstractButton traceButton;

        /**
         * Taille initiale de la bo�te de dialogue {@link #dialog}.
         * Cette information sera utilis�e pour r�tablir la bo�te �
         * sa taille initiale lorsque la trace dispara�t.
         */
        private final Dimension initialSize;

        /**
         * Resources dans la langue de l'utilisateur.
         */
        private final Resources resources;

        /**
         * Construit un panneau qui affichera le message d'erreur sp�cifi�.
         *
         * @param owner     Composante parente de la bo�te de dialogue � cr�er.
         * @param exception Exception que l'on veut reporter.
         * @param message   Message � afficher.
         * @param buttons   Boutons � placer sous le message. Ces boutons
         *                  doivent �tre dans l'ordre "Debug" et "Close".
         * @param resources Resources dans la langue de l'utilisateur.
         */
        private Pane(final Component owner, final Throwable exception, final Container message, final AbstractButton[] buttons, final Resources resources)
        {
            super(message, ERROR_MESSAGE, OK_CANCEL_OPTION, null, buttons);
            this.exception   = exception;
            this.message     = message;
            this.resources   = resources;
            this.traceButton = buttons[0];
            buttons[0].addActionListener(this);
            buttons[1].addActionListener(this);
            /*
             * Construit la bo�te de dialogue en d�tectant automatiquement si on
             * peut utiliser {@link InternalFrame}  ou si l'on doit se contenter
             * de {@link JDialog}. La trace de l'exception ne sera pas �crite
             * tout de suite.
             */
            final String title=resources.getString(Cl�.ERROR�1, Utilities.getShortClassName(exception));
            final JDesktopPane desktop=getDesktopPaneForComponent(owner);
            if (desktop!=null)
            {
                final JInternalFrame dialog=createInternalFrame(desktop, title);
                desktop.setLayer(dialog, JDesktopPane.MODAL_LAYER.intValue());
                dialog.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
                dialog.setResizable(false);
                dialog.pack();
                this.dialog=dialog;
            }
            else
            {
                final JDialog dialog=createDialog(owner, title);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setResizable(false);
                dialog.pack();
                this.dialog=dialog;
            }
            initialSize=dialog.getSize();
        }

        /**
         * Construit et fait appara�tre une bo�te de dialogue informant l'utilisateur
         * qu'une exception s'est produite. Ce constructeur doit �tre appel� dans le
         * m�me thread que <i>Swing</i>.
         */
        public static void show(final Component owner, final Throwable exception, String message)
        {
            final Resources resources = Resources.getResources((owner!=null) ? owner.getLocale() : null);
            if (message==null)
            {
                message=exception.getLocalizedMessage();
                if (message==null)
                    message=resources.getString(Cl�.NO_DETAILS�1, Utilities.getShortClassName(exception));
            }
            final JTextArea textArea=new JTextArea(message, 1, WIDTH);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBackground(null);
            textArea.setBorder(null); // Certains L&F ont une bordure.
            /**
             * Construit le reste de la bo�te de dialogue. La barre
             * de titre contiendra le nom de la classe de l'exception.
             */
            final JComponent messageBox=new JPanel(new BorderLayout());
            messageBox.add(textArea, BorderLayout.NORTH);
            final Pane pane=new Pane(owner, exception, messageBox, new AbstractButton[]
            {
                new JButton(resources.getString(Cl�.DEBUG)),
                new JButton(resources.getString(Cl�.CLOSE))
            }, resources);
            pane.dialog.setVisible(true);
        }

        /**
         * Fait appara�tre la trace de l'exception en-dessous du message.  Cette m�thode est appel�e
         * automatiquement lorsque le bouton "Debug" de la bo�te de dialogue est appuy�. Si la trace
         * de l'exception n'a pas encore �t� �crite, alors cette m�thode construira les composantes
         * n�cessaires une fois pour toute.
         */
        public void actionPerformed(final ActionEvent event)
        {
            if (event.getSource()!=traceButton)
            {
                dispose();
                return;
            }
            /*
             * Construit la trace de l'exception une fois pour
             * toute si elle n'avait pas d�j� �t� construite.
             */
            if (trace==null)
            {
                JComponent traceComponent=null;
                for (Throwable exception=this.exception; exception!=null; exception=exception.getCause())
                {
                    final JTextArea text=new JTextArea(1, WIDTH);
                    text.setTabSize(4);
                    text.setText(printStackTrace(exception));
                    text.setEditable(false);
                    final JScrollPane scroll=new JScrollPane(text);
                    if (traceComponent!=null)
                    {
                        if (!(traceComponent instanceof JTabbedPane))
                        {
                            final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
                            tabs.addTab(Utilities.getShortClassName(this.exception), traceComponent);
                            traceComponent = tabs;
                        }
                        ((JTabbedPane) traceComponent).addTab(Utilities.getShortClassName(exception), scroll);
                    }
                    else traceComponent = scroll;
                }
                if (traceComponent==null) return; // Should not happen
                traceComponent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(12,0,0,0), traceComponent.getBorder()));
                trace=traceComponent;
            }
            /*
             * Ins�re ou cache la trace de l'exception. M�me si on cache la trace, on
             * ne la d�truira pas au cas o� l'utilisateur voudrait encore la r�afficher.
             */
            traceButton.setText(Resources.format(traceVisible ? Cl�.DEBUG : Cl�.HIDE));
            traceVisible = !traceVisible;
            if (dialog instanceof Dialog)
            {
                ((Dialog) dialog).setResizable(traceVisible);
            }
            else
            {
                ((JInternalFrame) dialog).setResizable(traceVisible);
            }
            if (traceVisible)
            {
                message.add(trace, BorderLayout.CENTER);
                dialog.setSize(initialSize.width, HEIGHT);
            }
            else
            {
                message.remove(trace);
                dialog.setSize(initialSize);
            }
            dialog.validate();
        }

        /**
         * Lib�re les ressources utilis�es par cette bo�te de dialogue.
         * Cette m�thode est app�l�e lorsque l'utilisateur a ferm� la
         * bo�te de dialogue qui reportait l'exception.
         */
        private void dispose()
        {
            if (dialog instanceof Window)
            {
                ((Window) dialog).dispose();
            }
            else
            {
                ((JInternalFrame) dialog).dispose();
            }
        }

        /**
         * Lib�re les ressources utilis�es par cette bo�te de dialogue.
         * Cette m�thode est d�finie par s�curit�, afin d'�tre certain
         * que les ressources ont bel et bien �t� lib�r�s m�me si une
         * exception lanc�e a emp�ch�e la m�thode {@link #show} de se
         * terminer normalement.
         */
        protected void finalize() throws Throwable
        {
            dispose();
            super.finalize();
        }
    }
}
