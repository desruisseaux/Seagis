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
package fr.ird.awt;

// Interface utilisateur
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;

// Images et événements
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;

// Divers
import java.util.Arrays;
import fr.ird.util.XArray;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;

// Geotools dependencies
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Barre d'état apparaissant dans le bas des fenêtre internes. Cette barre
 * d'état pourra contenir un message d'intérêt général ainsi qu'une barre
 * des progrès.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class StatusBar extends JComponent
{
    /**
     * Chaîne de caractères représentant un texte nul. Ce sera en général un
     * espace afin que l'étiquette conserve quand même une certaine hauteur.
     */
    private static final String NULL=" ";

    /**
     * Texte à afficher dans la barre d'état lorsqu'aucune opération n'est en cours.
     * S'il n'y a pas de texte à afficher, alors cette chaîne devrait être la constante
     * <code>StatusBar.NULL</code> plutôt que <code>null</code>.
     */
    private String text=NULL;

    /**
     * Composante dans lequel écrire des messages.
     */
    private final JLabel message=new JLabel(NULL);

    /**
     * Composante dans lequel écrire les coordonnées
     * pointées par le curseur de la souris.
     */
    private final JLabel coordinate=new JLabel(NULL);

    /**
     * Progression d'une opération quelconque. Ce sera
     * souvent la progression de la lecture d'une image.
     */
    private final BoundedRangeModel progress;

    /**
     * Liste de numéros (<strong>en ordre croissant</code>) identifiant les objets
     * qui veulent écrire leur progression dans la barre des progrès. Chaque objet
     * {@link ProgressListener} a un numéro unique.  Le premier numéro de la liste
     * est celui de l'objet {@link ProgressListener} qui possède la barre des progrès.
     * On ne retient pas des références directes afin de ne pas nuire au travail du
     * ramasse-miettes.
     */
    private transient int[] progressQueue=new int[0]; // must be transient

    /**
     * Construit une nouvelle barre d'état.
     */
    public StatusBar()
    {
        setLayout(new GridBagLayout());
        final JProgressBar progress=new JProgressBar();
        final GridBagConstraints c=new GridBagConstraints();

        c.gridy=0; c.fill=c.BOTH;
        c.gridx=0; c.weightx=1; add(message,    c);
        c.gridx=1; c.weightx=0; add(coordinate, c);
        c.gridx=2;              add(progress,   c);

        config(message);
        config(coordinate);
        final Dimension size=coordinate.getPreferredSize();
        size.width=200; coordinate.setPreferredSize(size);
        progress.setBorder(BorderFactory.createLoweredBevelBorder());
        this.progress=progress.getModel();
    }

    /**
     * Configure la zone de texte spécifiée.
     */
    private static void config(final JLabel label)
    {
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLoweredBevelBorder(),
                        BorderFactory.createEmptyBorder(0/*top*/, 6/*left*/, 0/*bottom*/,0/*right*/)));
    }

    /**
     * Retourne le texte à afficher dans la barre d'état. La valeur
     * <code>null</code> signifie qu'aucun texte ne sera affiché.
     */
    public String getText()
    {return (NULL.equals(text)) ? null : text;}

    /**
     * Définit le texte à afficher dans la barre d'état. La valeur
     * <code>null</code> signifie qu'aucun texte ne doit être affiché.
     */
    public void setText(String text)
    {
        if (text==null || text.length()==0) text=NULL;
        message.setText(this.text=text);
    }

    /**
     * Définit le texte à afficher dans la case réservée aux coordonnées. La
     * valeur <code>null</code> signifie qu'aucun texte ne doit être affiché.
     */
    public void setCoordinate(String text)
    {
        if (text==null || text.length()==0) text=NULL;
        coordinate.setText(text);
    }

    /**
     * Retourne un objet à informer chaque fois que la lecture d'une image a progressée.
     * Cette méthode ainsi que l'objet retourné pevent être utilisés dans n'importe quel
     * thread (pas nécessairement celui de <i>Swing</i>).
     *
     * @param name Nom de l'image en cours de lecture. Ce sera le plus souvent le nom du fichier.
     *        Ce nom servira à écrire automatiquement un texte lorsque la lecture commencera.
     */
    public synchronized IIOReadProgressListener getIIOReadProgressListener(final String name)
    {return new ProgressListener(Resources.getResources(getLocale()).getString(ResourceKeys.LOADING_$1, name));}

    /**
     * Classe chargée de réagir
     * au progrès de la lecture.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class ProgressListener implements IIOReadProgressListener, Runnable
    {
        /** Pas d'opération.     */ private static final byte   NOP      = 0;
        /** Début de la lecture. */ private static final byte   START    = 1;
        /** Lecture en cours.    */ private static final byte   PROGRESS = 2;
        /** Fin de la lecture.   */ private static final byte   END      = 3;
        /** Numéro identificateur*/ private               int   ID;
        /** Nom de l'image.      */ private        final String name;
        /** Nom de l'image.      */ private              String toWrite;
        /** Pourcentage accompli.*/ private               int   percent;
        /** Code de l'opération. */ private              byte   operation=NOP;
        /** Ignoré.              */ public void sequenceStarted  (ImageReader source, int minIndex  ) {}
        /** Initialise la barre. */ public void imageStarted     (ImageReader source, int imageIndex) {invokeLater(START,                0);}
        /** Informe des progrès. */ public void imageProgress    (ImageReader source, float percent ) {invokeLater(PROGRESS, (int) percent);}
        /** Efface la barre.     */ public void imageComplete    (ImageReader source                ) {invokeLater(END,                100);}
        /** Efface la barre.     */ public void readAborted      (ImageReader source                ) {invokeLater(END,                  0);}
        /** Ignoré.              */ public void thumbnailStarted (ImageReader source, int im, int th) {}
        /** Ignoré.              */ public void thumbnailProgress(ImageReader source, float percent ) {}
        /** Ignoré.              */ public void thumbnailComplete(ImageReader source                ) {}
        /** Ignoré.              */ public void sequenceComplete (ImageReader source                ) {}

        /**
         * Construit un objet chargé d'informer
         * des progrès de la lecture d'une image.
         */
        protected ProgressListener(final String name)
        {
            this.name = name;
            toWrite   = name;
        }
        
        /**
         * Prépare une opération à exécuter dans le thread de <i>Swing</i>.
         * Cette opération sera décrite par le champ {@link #operation} et
         * consistera typiquement à initialiser la barre des progrès ou
         * afficher son pourcentage ({@link #percent}).
         *
         * @param nextOp  Code de l'opération ({@link #START}, {@link #PROGRESS} ou {@link #END}).
         * @param percent Pourcentage des progrès accomplis.
         */
        private void invokeLater(final byte nextOp, final int percent)
        {
            synchronized (StatusBar.this)
            {
                final byte currentOp=this.operation;
                if (this.percent!=percent || currentOp!=nextOp)
                {
                    this.percent=percent;
                    switch (currentOp)
                    {
                        case START:
                        {
                            if (nextOp==END) this.operation=NOP;
                            // Sinon, on continue avec 'START'.
                            break;
                        }
                        case NOP:
                        {
                            EventQueue.invokeLater(this);
                            // fall through
                        }
                        case PROGRESS:
                        {
                            this.operation=nextOp;
                            break;
                        }
                    }
                }
            }
        }

        /**
         * Exécute une opération préparée par {@link #invokeLater}. Cette opération peut
         * constiter à initialiser la barre des progrès ({@link #START}), informer des
         * progrès accomplis ({@link #PROGRESS}) ou informer que la tâche est terminée
         * ({@link #END}). Cette méthode doit obligatoirement être appelée dans le thread
         * de <i>Swing</i>.
         */
        public void run()
        {
            synchronized (StatusBar.this)
            {
                try
                {
                    switch (operation)
                    {
                        /*
                         * Si on démarre la lecture d'une nouvelle image, tente de
                         * prendre possession de la barre d'état.  Si on n'est pas
                         * le premier à demander la possession de la barre d'état,
                         * cet objet 'ProgressListener' sera placé dans une liste
                         * d'attente.
                         */
                        case START:
                        {
                            toWrite=name;
                            if (lock())
                            {
                                flush();
                                progress.setRangeProperties(percent, 1, 0, 100, false);
                            }
                            break;
                        }
                        /*
                         * Si la lecture de l'image a avancé, on écrira les progrès dans la barre d'état
                         * à la condition que cette barre d'état nous appartient. On écrira le nom de
                         * l'opération si ce n'était pas déjà fait (c'est le cas si on n'avait pas pu
                         * prendre possession de la barre d'état au moment ou START avait été exécuté).
                         */
                        case PROGRESS:
                        {
                            if (hasLock())
                            {
                                flush();
                                progress.setValue(percent);
                            }
                            break;
                        }
                        /*
                         * A la fin de la lecture, relâche la barre d'état. Elle
                         * pourra être récupérée par d'autres 'ProgressListener'
                         * qui étaient dans la liste d'attente.
                         */
                        case END:
                        {
                            if (hasLock())
                            {
                                progress.setRangeProperties(0, 1, 0, 100, false);
                                message.setText(text);
                            }
                            unlock();
                            break;
                        }
                    }
                }
                catch (RuntimeException exception)
                {
                    ExceptionMonitor.show(StatusBar.this, exception);
                }
                finally
                {
                    operation = NOP;
                }
            }
        }

        /**
         * Ecrit dans la barre d'état la description de cet objet <code>ProgressListener</code>, si
         * ce n'était pas déjà fait.  Cette méthode ne doit être appelée que lorsque les conditions
         * suivantes ont été remplises:
         *
         * <ul>
         *   <li>Cette méthode est appelée dans le thread de Swing.</li>
         *   <li>Cette méthode est appelée dans un bloc synchronisé sur <code>StatusBar.this</code>.</li>
         *   <li>La méthode {@link #lock} ou {@link #hasLock} a retourné <code>true</code>.</li>
         * </ul>
         */
        private void flush()
        {
            if (toWrite!=null)
            {
                message.setText(toWrite);
                toWrite=null;
            }
        }

        /**
         * Vérifie si cet objet <code>ProgressBar</code> possède la barre d'état. Cette
         * méthode ne doit être appelée que lorsque les conditions suivantes ont été remplises:
         *
         * <ul>
         *   <li>Cette méthode est appelée dans un bloc synchronisé sur <code>StatusBar.this</code>.</li>
         * </ul>
         */
        private boolean hasLock()
        {
            final int[] progressQueue = StatusBar.this.progressQueue;
            return (progressQueue.length>=1 && progressQueue[0]==ID);
        }

        /**
         * tente de prendre possession de la barre d'état. Cette méthode retourne <code>true</code>
         * si elle a effectivement réussie à en prendre possession, ou <code>false</code> si elle
         * s'est placée dans une liste d'attente. Cette méthode ne doit être appelée que lorsque
         * les conditions suivantes ont été remplises:
         *
         * <ul>
         *   <li>Cette méthode est appelée dans un bloc synchronisé sur <code>StatusBar.this</code>.</li>
         * </ul>
         */
        private boolean lock()
        {
            final int index=Arrays.binarySearch(progressQueue, ID);
            if (index>=0) return index==0;
            final int length=progressQueue.length;
            if (length!=0)
            {
                ID=progressQueue[length-1]+1;
                if (ID<=0) return false; // Too many ProgressListener
                progressQueue=XArray.resize(progressQueue, length+1);
                progressQueue[length]=ID;
                return false;
            }
            else
            {
                progressQueue=new int[] {ID=1};
                return true;
            }
        }

        /**
         * Déclare que cet objet <code>ProgressBar</code> n'est plus intéressé
         * a posséder la barre d'état. Cette méthode ne doit être appelée que
         * lorsque les conditions suivantes ont été remplises:
         *
         * <ul>
         *   <li>Cette méthode est appelée dans un bloc synchronisé sur <code>StatusBar.this</code>.</li>
         * </ul>
         */
        private void unlock()
        {
            final int index=Arrays.binarySearch(progressQueue, ID);
            if (index>=0) progressQueue=XArray.remove(progressQueue, index, 1);
            ID=0;
        }

        /**
         * Déclare que cet objet <code>ProgressBar</code>
         * n'est plus intéressé a posséder la barre d'état.
         */
        protected void finalize() throws Throwable
        {
            synchronized (StatusBar.this)
            {
                if (hasLock())
                {
                    progress.setRangeProperties(0, 1, 0, 100, false);
                    message.setText(text);
                }
                unlock();
                super.finalize();
            }
        }
    }
}
