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
package fr.ird.image.work;

// Interface utilisateur
import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JProgressBar;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;

// Ev�nements
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

// Geotools dependencies
import org.geotools.gui.swing.ExceptionMonitor;

// Divers
import java.io.IOException;
import fr.ird.awt.progress.Progress;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Paneau signalant qu'un calcul est en cours.  Cette composante affiche
 * un texte signalant qu'un calcul est en cours,    et propose un bouton
 * "arr�ter" pour l'arr�ter � tout moment. Lorsque l'utilisateur demande
 * � ar�ter un calcul, cette classe change l'affichage pour signaler que
 * l'arr�t est en cours, et appelle la m�thode {@link Worker#stop}.   Le
 * texte "arr�t en cours" restera affich� ind�finiment;  c'est au thread
 * de {@link Worker} d'appeler {@link System#exit} lorsqu'il a termin�.
 * <br><br>
 * La seule m�thode utile de cette classe est {@link #show}. Les autres
 * sont r�serv�es � un usage interne.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ProgressPanel extends Progress implements ActionListener, WindowListener
{
    /**
     * Objet effectuant le calcul.  La m�thode {@link Worker#stop}
     * de cet objet sera appel�e lorsque l'utilisateur demandera �
     * interrompre le calcul,  ou lorsque l'utilisateur fermera la
     * fen�tre.
     */
    private final Worker worker;

    /**
     * Paneau indiquant qu'un calcul est en cours.
     */
    private final JPanel panel;

    /**
     * Barre des progr�s. Cet objet devra
     * utiliser une plage allant de 0 � 100.
     */
    private final BoundedRangeModel progress;

    /**
     * Description � placer � gauche de la barre des progr�s.
     * Cette description contient notamment le nom de l'image
     * en cours de traitement.
     */
    private final JLabel progressLabel = new JLabel(Resources.format(ResourceKeys.PROGRESSION), JLabel.RIGHT);

    /**
     * Indique si le calcul a �t� interrompu.  Ce champ prendra la valeur
     * <code>true</code> apr�s que l'utilisateur ait cliqu� sur le bouton
     * "arr�t�" o� qu'il ait tent� de fermer la fen�tre.
     */
    private boolean stopped;

    /**
     * Construit un paneau indiquant qu'un calcul est en cours avec
     * l'objet {@link Worker} sp�cifi�. Ce paneau affichera aussi les
     * progr�s de l'op�ration.
     *
     * @param  worker Travail en cours.
     * @throws IOException si une erreur de lecture est survenue.
     */
    public ProgressPanel(final Worker worker) throws IOException
    {
        this.worker = worker;
        if (worker!=null)
        {
            worker.addProgressListener(this);
        }
        final ClassLoader       loader = getClass().getClassLoader();
        final JEditorPane         pane = new JEditorPane(loader.getResource("application-data/HTML/Computing.html"));
        final JButton       stopButton = new JButton("Arr�ter le calcul", new ImageIcon(loader.getResource("toolbarButtonGraphics/general/Stop24.gif")));
        final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);

        pane.setPreferredSize(new Dimension(920,400));
        stopButton.setMargin(new Insets(/*top*/3, /*left*/15, /*bottom*/3, /*right*/9));
        stopButton.setFont(stopButton.getFont().deriveFont(16f));
        stopButton.addActionListener(this);
        stopButton.setToolTipText("Sauvegarde le calcul et ferme la fen�tre");
        progressBar.setStringPainted(true);
        progressBar.setToolTipText(progressLabel.getText());
        progress=progressBar.getModel();

        panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.white);
        final GridBagConstraints c=new GridBagConstraints();
        c.fill=c.BOTH; c.weightx=1; c.weighty=1;
        c.gridx=0; c.gridy=0; c.gridwidth=2; panel.add(pane, c);

        c.fill=c.HORIZONTAL; c.weighty=0;
        c.insets.top  = c.insets.bottom =  9;
        c.insets.left = c.insets.right  = 30;
        c.gridy=2; panel.add(stopButton, c);

        c.gridx=1; c.gridy=1;   c.gridwidth=1;                   c.insets.left=3;  panel.add(progressBar, c);
        c.gridx=0; c.weightx=0; c.fill=c.NONE; c.insets.right=3; c.insets.left=30; panel.add(progressLabel, c);
    }

    /**
     * Fait appara�tre une fen�tre qui indiquera qu'un calcul est en cours.
     * L'utilisateur pourra interrompre le calcul en cliquant sur le bouton
     * "Arr�ter" ou en fermant la fen�tre.
     *
     * @param  title Titre de la fen�tre.
     * @throws IOException si une erreur de lecture est survenue.
     */
    public void show(final String title)
    {
        final JFrame frame=new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.addWindowListener(this);
        frame.pack();

        final Dimension size=frame.getToolkit().getScreenSize();
        frame.setLocation((size.width-frame.getWidth())/2, (size.height-frame.getHeight())/2);
        frame.show();
    }

    /**
     * Interrompt le calcul en cours. Cette m�thode appelle {@link Worker#stop},
     * puis remplace l'affichage par un paneau indiquant que l'arr�t est en cours.
     */
    private final void stop()
    {
        if (worker!=null)
        {
            worker.stop();
            worker.removeProgressListener(this);
        }
        if (!stopped)
        {
            final JLabel label=new JLabel("Arr�t en cours...");
            label.setFont(new Font("Helvetica", Font.BOLD, 48));
            label.setForeground(Color.yellow);

            panel.removeAll();
            panel.add(label);
            panel.setBackground(Color.black);
            panel.revalidate();
            panel.repaint();
            stopped=true;
        }
    }
    
    /** Indique que l'op�ration a commenc�e. */
    public void started()
    {if (stopped && worker!=null) worker.stop();}
    
    /** Indique que l'op�ration est termin�e. */
    public void complete()
    {}
    
    /** Retourne le message d'�crivant l'op�ration en cours. */
    public String getDescription()
    {return progressLabel.getText();}
    
    /** Sp�cifie un message qui d�crit l'op�ration en cours. */
    public void setDescription(final String description)
    {progressLabel.setText(description!=null ? description : Resources.format(ResourceKeys.PROGRESSION));}
    
    /** Envoie un message d'avertissement. */
    public void warningOccurred(final String source, final String margin, final String warning)
    {}
    
    /** Indique qu'une exception est survenue pendant le traitement de l'op�ration. */
    public void exceptionOccurred(final Throwable exception)
    {ExceptionMonitor.show(panel, exception);}
    
    /**
     * Indique l'�tat d'avancement de l'op�ration. Le progr�s est repr�sent� par un
     * pourcentage variant de 0 � 100 inclusivement. Si la valeur sp�cifi�e est en
     * dehors de ces limites, elle sera automatiquement ramen�e entre 0 et 100.
     */
    public void progress(float percent)
    {
        if (!(percent>=0)) percent=0; // Replace NaN by 0.
        if (percent>100) percent=100;
        progress.setValue((int)percent);
    }

    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur a demand� � interrompre le
     * calcul. Cette m�thode appelle {@link Worker#stop}, puis remplace l'affichage par
     * un paneau indiquant que l'arr�t est en cours. Cette m�thode ne fermera pas
     * l'application; c'est {@link Worker} qui devra appeller {@link System#exit}
     * lorsqu'il aura termin�.
     */
    public void actionPerformed(final ActionEvent event)
    {stop();}
    
    /**
     * M�thode appel�e automatiquement la premi�re fois
     * une fen�tre est rendue visible. L'impl�mentation
     * par d�faut ne fait rien.
     */
    public void windowOpened(final WindowEvent event)
    {}
    
    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur demande � fermer
     * la fen�tre. Lors du premier clic, cette m�thode remplacera l'affichage
     * par un paneau indiquant que l'arr�t est en cours. Si l'utilisateur clique
     * une seconde fois, la fen�tre dispara�tra mais le syst�me continuera quand
     * m�me de tourner jusqu'� ce que {@link Worker} appelle {@link System#exit}.
     */
    public void windowClosing(final WindowEvent event)
    {
        if (stopped)
        {
            if (worker!=null) worker.stop(); // Par prudence
            event.getWindow().dispose();
        }
        else stop();
    }
    
    /**
     * M�thode appel�e automatiquement lorsqu'une fen�tre a
     * �t� ferm�e. L'impl�mentation par d�faut ne fait rien.
     */
    public void windowClosed(final WindowEvent event)
    {}
    
    /**
     * M�thode appel�e automatiquement lorsqu'une fen�tre a �t�
     * r�duite en ic�ne. L'impl�mentation par d�faut ne fait rien.
     */
    public void windowIconified(final WindowEvent event)
    {}

    /**
     * M�thode appel�e automatiquement lorsqu'une fen�tre a �t�
     * r�affich�e. L'impl�mentation par d�faut ne fait rien.
     */
    public void windowDeiconified(final WindowEvent event)
    {}
    
    /**
     * M�thode appel�e automatiquement lorsqu'une fen�tre n'est
     * plus active. L'impl�mentation par d�faut ne fait rien.
     */
    public void windowDeactivated(final WindowEvent event)
    {}
    
    /**
     * M�thode appel�e automatiquement lorsqu'une fen�tre redevient
     * active. L'impl�mentation par d�faut ne fait rien.
     */
    public void windowActivated(final WindowEvent event)
    {}

    /**
     * Fait appara�tre une fen�tre indiquant qu'un calcul est en cours.
     * Cette m�thode peut �tre appel�e � partir de la ligne de commande
     * pour v�rifier l'apparence de la fen�tre.
     *
     * @throws IOException si une erreur de lecture est survenue.
     */
    public static void main(final String[] args) throws IOException
    {new ProgressPanel(null).show(null);}
}
