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

// Evénements
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
 * "arrêter" pour l'arrêter à tout moment. Lorsque l'utilisateur demande
 * à arêter un calcul, cette classe change l'affichage pour signaler que
 * l'arrêt est en cours, et appelle la méthode {@link Worker#stop}.   Le
 * texte "arrêt en cours" restera affiché indéfiniment;  c'est au thread
 * de {@link Worker} d'appeler {@link System#exit} lorsqu'il a terminé.
 * <br><br>
 * La seule méthode utile de cette classe est {@link #show}. Les autres
 * sont réservées à un usage interne.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ProgressPanel extends Progress implements ActionListener, WindowListener
{
    /**
     * Objet effectuant le calcul.  La méthode {@link Worker#stop}
     * de cet objet sera appelée lorsque l'utilisateur demandera à
     * interrompre le calcul,  ou lorsque l'utilisateur fermera la
     * fenêtre.
     */
    private final Worker worker;

    /**
     * Paneau indiquant qu'un calcul est en cours.
     */
    private final JPanel panel;

    /**
     * Barre des progrès. Cet objet devra
     * utiliser une plage allant de 0 à 100.
     */
    private final BoundedRangeModel progress;

    /**
     * Description à placer à gauche de la barre des progrès.
     * Cette description contient notamment le nom de l'image
     * en cours de traitement.
     */
    private final JLabel progressLabel = new JLabel(Resources.format(ResourceKeys.PROGRESSION), JLabel.RIGHT);

    /**
     * Indique si le calcul a été interrompu.  Ce champ prendra la valeur
     * <code>true</code> après que l'utilisateur ait cliqué sur le bouton
     * "arrêté" où qu'il ait tenté de fermer la fenêtre.
     */
    private boolean stopped;

    /**
     * Construit un paneau indiquant qu'un calcul est en cours avec
     * l'objet {@link Worker} spécifié. Ce paneau affichera aussi les
     * progrès de l'opération.
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
        final JButton       stopButton = new JButton("Arrêter le calcul", new ImageIcon(loader.getResource("toolbarButtonGraphics/general/Stop24.gif")));
        final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);

        pane.setPreferredSize(new Dimension(920,400));
        stopButton.setMargin(new Insets(/*top*/3, /*left*/15, /*bottom*/3, /*right*/9));
        stopButton.setFont(stopButton.getFont().deriveFont(16f));
        stopButton.addActionListener(this);
        stopButton.setToolTipText("Sauvegarde le calcul et ferme la fenêtre");
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
     * Fait apparaître une fenêtre qui indiquera qu'un calcul est en cours.
     * L'utilisateur pourra interrompre le calcul en cliquant sur le bouton
     * "Arrêter" ou en fermant la fenêtre.
     *
     * @param  title Titre de la fenêtre.
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
     * Interrompt le calcul en cours. Cette méthode appelle {@link Worker#stop},
     * puis remplace l'affichage par un paneau indiquant que l'arrêt est en cours.
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
            final JLabel label=new JLabel("Arrêt en cours...");
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
    
    /** Indique que l'opération a commencée. */
    public void started()
    {if (stopped && worker!=null) worker.stop();}
    
    /** Indique que l'opération est terminée. */
    public void complete()
    {}
    
    /** Retourne le message d'écrivant l'opération en cours. */
    public String getDescription()
    {return progressLabel.getText();}
    
    /** Spécifie un message qui décrit l'opération en cours. */
    public void setDescription(final String description)
    {progressLabel.setText(description!=null ? description : Resources.format(ResourceKeys.PROGRESSION));}
    
    /** Envoie un message d'avertissement. */
    public void warningOccurred(final String source, final String margin, final String warning)
    {}
    
    /** Indique qu'une exception est survenue pendant le traitement de l'opération. */
    public void exceptionOccurred(final Throwable exception)
    {ExceptionMonitor.show(panel, exception);}
    
    /**
     * Indique l'état d'avancement de l'opération. Le progrès est représenté par un
     * pourcentage variant de 0 à 100 inclusivement. Si la valeur spécifiée est en
     * dehors de ces limites, elle sera automatiquement ramenée entre 0 et 100.
     */
    public void progress(float percent)
    {
        if (!(percent>=0)) percent=0; // Replace NaN by 0.
        if (percent>100) percent=100;
        progress.setValue((int)percent);
    }

    /**
     * Méthode appelée automatiquement lorsque l'utilisateur a demandé à interrompre le
     * calcul. Cette méthode appelle {@link Worker#stop}, puis remplace l'affichage par
     * un paneau indiquant que l'arrêt est en cours. Cette méthode ne fermera pas
     * l'application; c'est {@link Worker} qui devra appeller {@link System#exit}
     * lorsqu'il aura terminé.
     */
    public void actionPerformed(final ActionEvent event)
    {stop();}
    
    /**
     * Méthode appelée automatiquement la première fois
     * une fenêtre est rendue visible. L'implémentation
     * par défaut ne fait rien.
     */
    public void windowOpened(final WindowEvent event)
    {}
    
    /**
     * Méthode appelée automatiquement lorsque l'utilisateur demande à fermer
     * la fenêtre. Lors du premier clic, cette méthode remplacera l'affichage
     * par un paneau indiquant que l'arrêt est en cours. Si l'utilisateur clique
     * une seconde fois, la fenêtre disparaîtra mais le système continuera quand
     * même de tourner jusqu'à ce que {@link Worker} appelle {@link System#exit}.
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
     * Méthode appelée automatiquement lorsqu'une fenêtre a
     * été fermée. L'implémentation par défaut ne fait rien.
     */
    public void windowClosed(final WindowEvent event)
    {}
    
    /**
     * Méthode appelée automatiquement lorsqu'une fenêtre a été
     * réduite en icône. L'implémentation par défaut ne fait rien.
     */
    public void windowIconified(final WindowEvent event)
    {}

    /**
     * Méthode appelée automatiquement lorsqu'une fenêtre a été
     * réaffichée. L'implémentation par défaut ne fait rien.
     */
    public void windowDeiconified(final WindowEvent event)
    {}
    
    /**
     * Méthode appelée automatiquement lorsqu'une fenêtre n'est
     * plus active. L'implémentation par défaut ne fait rien.
     */
    public void windowDeactivated(final WindowEvent event)
    {}
    
    /**
     * Méthode appelée automatiquement lorsqu'une fenêtre redevient
     * active. L'implémentation par défaut ne fait rien.
     */
    public void windowActivated(final WindowEvent event)
    {}

    /**
     * Fait apparaître une fenêtre indiquant qu'un calcul est en cours.
     * Cette méthode peut être appelée à partir de la ligne de commande
     * pour vérifier l'apparence de la fenêtre.
     *
     * @throws IOException si une erreur de lecture est survenue.
     */
    public static void main(final String[] args) throws IOException
    {new ProgressPanel(null).show(null);}
}
