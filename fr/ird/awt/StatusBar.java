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

// Images et �v�nements
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
 * Barre d'�tat apparaissant dans le bas des fen�tre internes. Cette barre
 * d'�tat pourra contenir un message d'int�r�t g�n�ral ainsi qu'une barre
 * des progr�s.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class StatusBar extends JComponent
{
    /**
     * Cha�ne de caract�res repr�sentant un texte nul. Ce sera en g�n�ral un
     * espace afin que l'�tiquette conserve quand m�me une certaine hauteur.
     */
    private static final String NULL=" ";

    /**
     * Texte � afficher dans la barre d'�tat lorsqu'aucune op�ration n'est en cours.
     * S'il n'y a pas de texte � afficher, alors cette cha�ne devrait �tre la constante
     * <code>StatusBar.NULL</code> plut�t que <code>null</code>.
     */
    private String text=NULL;

    /**
     * Composante dans lequel �crire des messages.
     */
    private final JLabel message=new JLabel(NULL);

    /**
     * Composante dans lequel �crire les coordonn�es
     * point�es par le curseur de la souris.
     */
    private final JLabel coordinate=new JLabel(NULL);

    /**
     * Progression d'une op�ration quelconque. Ce sera
     * souvent la progression de la lecture d'une image.
     */
    private final BoundedRangeModel progress;

    /**
     * Liste de num�ros (<strong>en ordre croissant</code>) identifiant les objets
     * qui veulent �crire leur progression dans la barre des progr�s. Chaque objet
     * {@link ProgressListener} a un num�ro unique.  Le premier num�ro de la liste
     * est celui de l'objet {@link ProgressListener} qui poss�de la barre des progr�s.
     * On ne retient pas des r�f�rences directes afin de ne pas nuire au travail du
     * ramasse-miettes.
     */
    private transient int[] progressQueue=new int[0]; // must be transient

    /**
     * Construit une nouvelle barre d'�tat.
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
     * Configure la zone de texte sp�cifi�e.
     */
    private static void config(final JLabel label)
    {
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLoweredBevelBorder(),
                        BorderFactory.createEmptyBorder(0/*top*/, 6/*left*/, 0/*bottom*/,0/*right*/)));
    }

    /**
     * Retourne le texte � afficher dans la barre d'�tat. La valeur
     * <code>null</code> signifie qu'aucun texte ne sera affich�.
     */
    public String getText()
    {return (NULL.equals(text)) ? null : text;}

    /**
     * D�finit le texte � afficher dans la barre d'�tat. La valeur
     * <code>null</code> signifie qu'aucun texte ne doit �tre affich�.
     */
    public void setText(String text)
    {
        if (text==null || text.length()==0) text=NULL;
        message.setText(this.text=text);
    }

    /**
     * D�finit le texte � afficher dans la case r�serv�e aux coordonn�es. La
     * valeur <code>null</code> signifie qu'aucun texte ne doit �tre affich�.
     */
    public void setCoordinate(String text)
    {
        if (text==null || text.length()==0) text=NULL;
        coordinate.setText(text);
    }

    /**
     * Retourne un objet � informer chaque fois que la lecture d'une image a progress�e.
     * Cette m�thode ainsi que l'objet retourn� pevent �tre utilis�s dans n'importe quel
     * thread (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param name Nom de l'image en cours de lecture. Ce sera le plus souvent le nom du fichier.
     *        Ce nom servira � �crire automatiquement un texte lorsque la lecture commencera.
     */
    public synchronized IIOReadProgressListener getIIOReadProgressListener(final String name)
    {return new ProgressListener(Resources.getResources(getLocale()).getString(ResourceKeys.LOADING_$1, name));}

    /**
     * Classe charg�e de r�agir
     * au progr�s de la lecture.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class ProgressListener implements IIOReadProgressListener, Runnable
    {
        /** Pas d'op�ration.     */ private static final byte   NOP      = 0;
        /** D�but de la lecture. */ private static final byte   START    = 1;
        /** Lecture en cours.    */ private static final byte   PROGRESS = 2;
        /** Fin de la lecture.   */ private static final byte   END      = 3;
        /** Num�ro identificateur*/ private               int   ID;
        /** Nom de l'image.      */ private        final String name;
        /** Nom de l'image.      */ private              String toWrite;
        /** Pourcentage accompli.*/ private               int   percent;
        /** Code de l'op�ration. */ private              byte   operation=NOP;
        /** Ignor�.              */ public void sequenceStarted  (ImageReader source, int minIndex  ) {}
        /** Initialise la barre. */ public void imageStarted     (ImageReader source, int imageIndex) {invokeLater(START,                0);}
        /** Informe des progr�s. */ public void imageProgress    (ImageReader source, float percent ) {invokeLater(PROGRESS, (int) percent);}
        /** Efface la barre.     */ public void imageComplete    (ImageReader source                ) {invokeLater(END,                100);}
        /** Efface la barre.     */ public void readAborted      (ImageReader source                ) {invokeLater(END,                  0);}
        /** Ignor�.              */ public void thumbnailStarted (ImageReader source, int im, int th) {}
        /** Ignor�.              */ public void thumbnailProgress(ImageReader source, float percent ) {}
        /** Ignor�.              */ public void thumbnailComplete(ImageReader source                ) {}
        /** Ignor�.              */ public void sequenceComplete (ImageReader source                ) {}

        /**
         * Construit un objet charg� d'informer
         * des progr�s de la lecture d'une image.
         */
        protected ProgressListener(final String name)
        {
            this.name = name;
            toWrite   = name;
        }
        
        /**
         * Pr�pare une op�ration � ex�cuter dans le thread de <i>Swing</i>.
         * Cette op�ration sera d�crite par le champ {@link #operation} et
         * consistera typiquement � initialiser la barre des progr�s ou
         * afficher son pourcentage ({@link #percent}).
         *
         * @param nextOp  Code de l'op�ration ({@link #START}, {@link #PROGRESS} ou {@link #END}).
         * @param percent Pourcentage des progr�s accomplis.
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
         * Ex�cute une op�ration pr�par�e par {@link #invokeLater}. Cette op�ration peut
         * constiter � initialiser la barre des progr�s ({@link #START}), informer des
         * progr�s accomplis ({@link #PROGRESS}) ou informer que la t�che est termin�e
         * ({@link #END}). Cette m�thode doit obligatoirement �tre appel�e dans le thread
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
                         * Si on d�marre la lecture d'une nouvelle image, tente de
                         * prendre possession de la barre d'�tat.  Si on n'est pas
                         * le premier � demander la possession de la barre d'�tat,
                         * cet objet 'ProgressListener' sera plac� dans une liste
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
                         * Si la lecture de l'image a avanc�, on �crira les progr�s dans la barre d'�tat
                         * � la condition que cette barre d'�tat nous appartient. On �crira le nom de
                         * l'op�ration si ce n'�tait pas d�j� fait (c'est le cas si on n'avait pas pu
                         * prendre possession de la barre d'�tat au moment ou START avait �t� ex�cut�).
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
                         * A la fin de la lecture, rel�che la barre d'�tat. Elle
                         * pourra �tre r�cup�r�e par d'autres 'ProgressListener'
                         * qui �taient dans la liste d'attente.
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
         * Ecrit dans la barre d'�tat la description de cet objet <code>ProgressListener</code>, si
         * ce n'�tait pas d�j� fait.  Cette m�thode ne doit �tre appel�e que lorsque les conditions
         * suivantes ont �t� remplises:
         *
         * <ul>
         *   <li>Cette m�thode est appel�e dans le thread de Swing.</li>
         *   <li>Cette m�thode est appel�e dans un bloc synchronis� sur <code>StatusBar.this</code>.</li>
         *   <li>La m�thode {@link #lock} ou {@link #hasLock} a retourn� <code>true</code>.</li>
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
         * V�rifie si cet objet <code>ProgressBar</code> poss�de la barre d'�tat. Cette
         * m�thode ne doit �tre appel�e que lorsque les conditions suivantes ont �t� remplises:
         *
         * <ul>
         *   <li>Cette m�thode est appel�e dans un bloc synchronis� sur <code>StatusBar.this</code>.</li>
         * </ul>
         */
        private boolean hasLock()
        {
            final int[] progressQueue = StatusBar.this.progressQueue;
            return (progressQueue.length>=1 && progressQueue[0]==ID);
        }

        /**
         * tente de prendre possession de la barre d'�tat. Cette m�thode retourne <code>true</code>
         * si elle a effectivement r�ussie � en prendre possession, ou <code>false</code> si elle
         * s'est plac�e dans une liste d'attente. Cette m�thode ne doit �tre appel�e que lorsque
         * les conditions suivantes ont �t� remplises:
         *
         * <ul>
         *   <li>Cette m�thode est appel�e dans un bloc synchronis� sur <code>StatusBar.this</code>.</li>
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
         * D�clare que cet objet <code>ProgressBar</code> n'est plus int�ress�
         * a poss�der la barre d'�tat. Cette m�thode ne doit �tre appel�e que
         * lorsque les conditions suivantes ont �t� remplises:
         *
         * <ul>
         *   <li>Cette m�thode est appel�e dans un bloc synchronis� sur <code>StatusBar.this</code>.</li>
         * </ul>
         */
        private void unlock()
        {
            final int index=Arrays.binarySearch(progressQueue, ID);
            if (index>=0) progressQueue=XArray.remove(progressQueue, index, 1);
            ID=0;
        }

        /**
         * D�clare que cet objet <code>ProgressBar</code>
         * n'est plus int�ress� a poss�der la barre d'�tat.
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
