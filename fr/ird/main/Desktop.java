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
package fr.ird.main;

// Base de données
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import java.sql.SQLException;

// Interface utilisateur
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import fr.ird.awt.CoordinateChooser;
import net.seas.awt.ExceptionMonitor;
import net.seas.awt.TimeZoneChooser;
import net.seas.awt.About;

// Viewers
import fr.ird.main.catalog.CatalogFrame;
import fr.ird.main.viewer.NavigatorFrame;

// Evénements
import java.awt.EventQueue;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.Deflater;

// Ensembles
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Temps et divers
import java.util.Date;
import java.util.TimeZone;
import net.seagis.io.DefaultFileFilter;
import fr.ird.resources.ResourceKeys;
import fr.ird.resources.Resources;


/**
 * Bureau de l'application. Ce bureau contiendra plusieurs fenêtres. Une fenêtre
 * peut contenir une mosaïque d'images de différents types (température, vorticité,
 * etc...), tandis qu'une autre fenêtre peut contenir les résultats des couplages
 * entres les images et les données de pêches par exemple.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Desktop extends JDesktopPane implements PropertyChangeListener
{
    /**
     * Extension des fichiers binaires
     * enregistrés par cette application.
     */
    private static final String EXTENSION = "ops";

    /**
     * Décalage en pixels entre les fenêtres
     * lorsqu'elles sont disposées en cascades.
     */
    private static final int CASCADE_STEP=30;

    /**
     * Fuseau horaire pour l'affichage et la saisie des dates dans l'application.
     * Par défaut, ce sera le fuseau horaire de la base de données. L'utilisateur
     * pourra toutefois changer ce fuseau horaire sans que cela n'affecte celui de
     * la base de données.
     */
    private TimeZone timezone;

    /**
     * Connection avec l'ensemble des bases de données du projet PELOPS.
     * Cette connection est retournée par {@link InternalFrame#getDataBase}.
     */
    private final DataBase database;

    /**
     * Nom du fichier sous lequel le bureau
     * a été enregistré la dernière fois.
     */
    private transient File lastSavedFile;

    /**
     * Dernier répertoire à avoir été ouvert pour
     * les lectures ou écriture de fichier.
     */
    private transient String lastDirectory;

    /**
     * Coordonnées auxquelles apparaîtront
     * la prochaine fenêtre a créer.
     */
    private int nextFramePosition = CASCADE_STEP/2;

    /**
     * Liste des actions qui peuvent être activées ou
     * désactivées dépendament de la fenêtre active.
     */
    private Action[] actions;

    /**
     * Objet ayant la charge de réagir aux
     * changements de fenêtre actives.
     */
    private final InternalFrameListener listener=new InternalFrameAdapter()
    {
        public void internalFrameActivated(final InternalFrameEvent e)
        {stateChanged();}

        public void internalFrameDeactivated(final InternalFrameEvent e)
        {stateChanged();}
    };

    /**
     * Construit le bureau de l'application PELOPS.
     * @param database Connection avec l'ensemble des
     *              bases de données du projet PELOPS.
     */
    public Desktop(final DataBase database)
    {
        this.database = database;
        this.timezone = TimeZone.getTimeZone("UTC");
        setDragMode(OUTLINE_DRAG_MODE);
    }

    /**
     * Spécifie la liste des actions dont
     * l'état dépendra de la fenêtre active.
     */
    final void setActions(final Collection<Action> actions)
    {
        this.actions = actions.toArray(new Action[actions.size()]);
        stateChanged();
    }

    /**
     * Place une nouvelle fenêtre sur le bureau avec une position, une largeur
     * et une hauteur par défaut.  Cette méthode peut être appelée à partir de
     * n'importe quel thread (pas nécessairement celui de <i>Swing</i>).
     */
    protected final void addFrame(final InternalFrame frame)
    {
        if (frame!=null)
        {
            if (!EventQueue.isDispatchThread())
            {
                EventQueue.invokeLater(new Runnable()
                {
                    public void run()
                    {addFrame(frame);}
                });
                return;
            }
            frame.setTimeZone(timezone);
            frame.setPaintingWhileAdjusting(getDragMode()==LIVE_DRAG_MODE);
            final int  width = Math.max(100, Math.min(620, (getWidth ()/50)*50));
            final int height = Math.max(100, Math.min(540, (getHeight()/50)*50));
            if (nextFramePosition+width>=getWidth() || nextFramePosition+height>=getHeight())
            {
                nextFramePosition = CASCADE_STEP/2;
            }
            add(frame);
            frame.setBounds(nextFramePosition, nextFramePosition, width, height);
            nextFramePosition += CASCADE_STEP;
            frame.addInternalFrameListener(listener);
            frame.show();
            stateChanged();
        }
    }

    /**
     * Fait apparaître une boîte de dialogue demandant à l'utilisateur de confirmer une action.
     * Cette méthode peut être appelée de n'importe quel thread (pas nécessairement celui de
     * <i>Swing</i>).
     *
     * @param message     Message à afficher.
     * @param title       Titre de la boîte de dialogue.
     * @param messageType Type du message ({@link JOptionPane.PLAIN_MESSAGE}, {@link JOptionPane.QUESTION_MESSAGE},
     *                    {@link JOptionPane.WARNING_MESSAGE} ou {@link JOptionPane.ERROR_MESSAGE}).
     * @return <code>true</code> si l'utilisateur a cliqué sur "Oui".
     */
    private final boolean showConfirmDialog(final String message, final String title, final int messageType, final boolean yesNoOptions)
    {
        if (!EventQueue.isDispatchThread())
        {
            final boolean[] result=new boolean[1];
            Task.invokeAndWait(new Runnable()
            {
                public void run()
                {result[0]=showConfirmDialog(message, title, messageType, yesNoOptions);}
            });
            return result[0];
        }
        final int WIDTH=80;
        final int length=message.length();
        Object messageObject=message;
        if (length>WIDTH)
        {
            final List<String> list=new ArrayList<String>();
            int lower=0;
            int upper=0;
            while ((upper=message.indexOf(' ', upper+1))>=0)
            {
                if (upper-lower>WIDTH)
                {
                    list.add(message.substring(lower, upper));
                    while (upper<length && Character.isSpaceChar(message.charAt(upper))) upper++;
                    lower=upper;
                }
            }
            list.add(message.substring(lower));
            messageObject=list.toArray();
        }
        if (yesNoOptions)
        {
            return JOptionPane.showInternalConfirmDialog(this, messageObject, title, JOptionPane.YES_NO_OPTION, messageType)==JOptionPane.YES_OPTION;
        }
        else
        {
            JOptionPane.showInternalMessageDialog(this, messageObject, title, messageType);
            return true;
        }
    }

    /**
     * Vérifie que le nom de fichier spécifié est valide. S'il n'a pas
     * d'extension, cette méthode lui rajoutera {@link #EXTENSION}.
     */
    private static File valid(File file)
    {
        if (file.getName().indexOf('.')<0)
            file=new File(file.getPath()+'.'+EXTENSION);
        return file;
    }

    /**
     * Retourne une tache capable de lire ou d'enregistrer le fichier spécifié.
     * La tache pourra être effectué en arrière plan.
     */
    private Task getSerializable(final File file, final boolean save)
    {
        final Resources resources = Resources.getResources(getLocale());
        lastSavedFile=file;
        if (save)
        {
            final JInternalFrame[] frames=getAllFrames();
            final Task[] serial=new Task[frames.length];
            for (int i=0; i<frames.length; i++)
                if (frames[i] instanceof InternalFrame)
                    serial[i]=((InternalFrame) frames[i]).getSerializable();
            return new Task(resources.getString(ResourceKeys.SAVE))
            {
                protected void run() throws IOException
                {
                    final ObjectOutputStream out=new ObjectOutputStream(
                                                 new DeflaterOutputStream(
                                                 new FileOutputStream(file), new Deflater(Deflater.BEST_COMPRESSION)));
                    for (int i=0; i<serial.length; i++)
                        if (serial[i]!=null) out.writeObject(serial[i]);
                    out.writeObject(null); // Valeur sentinelle
                    out.close();
                }
            };
        }
        else return new Task(resources.getString(ResourceKeys.OPEN))
        {
            protected void run() throws Exception
            {
                Object serial;
                final ObjectInputStream in=new ObjectInputStream(new InflaterInputStream(new FileInputStream(file)));
                while ((serial=in.readObject()) != null)
                {
                    ((Task) serial).run(this);
                }
                in.close();
            }

            protected String getTitle(final Throwable exception)
            {return resources.getString(ResourceKeys.ERROR_BAD_FILE_$1, file.getName());}
        };
    }

    /**
     * Returns a thread for GUI construction.
     *
     * @param name The thread name.
     * @param runnable The task to run.
     */
    public final Thread createThread(final String name, final Runnable runnable)
    {return new Thread(database.builder, runnable, name);}

    /**
     * Retourne la connection avec l'ensemble des bases de
     * données du projet PELOPS.
     */
    public final DataBase getDataBase()
    {return database;}

    /**
     * Retourne le fuseau horaire pour l'affichage et la saisie des dates dans l'application.
     * L'utilisateur pourra changer ce fuseau horaire sans que cela n'affecte celui de la base
     * de données.
     */
    public final TimeZone getTimeZone()
    {return timezone;}

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre. L'implémentation par
     * défaut appelle les méthodes {@link InternalFrame#setTimeZone} de
     * toutes les fenêtres internes.
     */
    public final void setTimeZone(final TimeZone timezone)
    {
        this.timezone=timezone;
        final JInternalFrame[] frames=getAllFrames();
        for (int i=0; i<frames.length; i++)
            if (frames[i] instanceof InternalFrame)
                ((InternalFrame) frames[i]).setTimeZone(timezone);
    }

    /**
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    public final void setPaintingWhileAdjusting(final boolean s)
    {
        setDragMode(s ? LIVE_DRAG_MODE : OUTLINE_DRAG_MODE);
        final JInternalFrame[] frames=getAllFrames();
        for (int i=0; i<frames.length; i++)
            if (frames[i] instanceof InternalFrame)
                ((InternalFrame) frames[i]).setPaintingWhileAdjusting(s);
    }

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqué sur une image d'une
     * mosaïque doit être répliqué sur les autres.
     */
    public final void setImagesSynchronized(final boolean s)
    {
        final JInternalFrame[] frames=getAllFrames();
        for (int i=0; i<frames.length; i++)
            if (frames[i] instanceof InternalFrame)
                ((InternalFrame) frames[i]).setImagesSynchronized(s);
    }

    /**
     * Met à jour l'état activé/désactivé des menus et des boutons. Les actions
     * qui ne sont pas déjà occupées seront activées ou désactivées en fonction
     * de la fenêtre qui a le focus. Les actions en cours d'exécution seront
     * laissées inchangées.
     */
    protected final void stateChanged()
    {
        if (actions!=null)
        {
            final JInternalFrame window=getSelectedFrame();
            if (window instanceof InternalFrame)
            {
                final InternalFrame frame=(InternalFrame) window;
                for (int i=0; i<actions.length; i++)
                {
                    final Action action=actions[i];
                    if (!action.isBusy())
                        action.setEnabled(frame.canProcess(action.getCommandKey()));
                }
            }
            else for (int i=0; i<actions.length; i++)
            {
                final Action action=actions[i];
                if (!action.isBusy())
                    action.setEnabled(false);
            }
        }
    }

    /**
     * Listen for property changes in other components.
     */
    public void propertyChange(final PropertyChangeEvent event)
    {
        final String property = event.getPropertyName();
        final Object newValue = event.getNewValue();
        if (property!=null)
        {
            if (property.equalsIgnoreCase("TimeZone"))
            {
                setTimeZone((TimeZone) newValue);
                return;
            }
        }
    }

    /**
     * Affiche en avertissement. Cette méthode peut être appelée
     * à partir de n'importe quel thread (pas nécessairement celui
     * de <i>Swing</i>).
     *
     * @param source  Source de l'avertissement. Il peu s'agir
     *                par exemple du nom du fichier d'une image.
     * @param message Message à afficher.
     */
    protected final void warning(final String source, final String message)
    {
        // TODO: We should handle that in a better way.
        final LogRecord record = new LogRecord(Level.WARNING, message);
        DataBase.logger.log(record);
    }

    /**
     * Méthode appelée automatiquement lorsque l'application
     * est en train d'être fermée. L'implémentation par défaut
     * ferme les connections avec les bases de données, puis
     * termine le programme par un appel à {@link System#exit}.
     */
    protected void exit()
    {
        try
        {
            // TODO: VERIFIER SI ON EST AUTORISE A FERMER L'APPLICATION!!!!
            //       Il faudrait en particulier vérifier s'il n'y a pas un
            //       thread en cours d'exécution en arrière-plan.
            database.close();
        }
        catch (SQLException exception)
        {
            ExceptionMonitor.show(this, exception);
            // Continue to close the application.
        }
        Container parent=this;
        while ((parent=parent.getParent())!=null)
            if (parent instanceof Window)
                ((Window) parent).dispose();
        DataBase.logger.log(Resources.getResources(getLocale()).getLogRecord(Level.FINE, ResourceKeys.END_APPLICATION));
        System.exit(0);
    }

    /**
     * Exécute une action désignée par le code spécifié. Le code <code>clé</code>
     * désigne une des constantes de l'interface {@link ResourceKeys}, utilisé pour la
     * construction des menus. Par exemple la clé {@link ResourceKeys#EXPORT} désigne
     * le menu "exporter". L'implémentation par défaut reconnaît les clés suivantes:
     *
     * <ul>
     *   <li>{@link ResourceKeys#OPEN}</li>
     *   <li>{@link ResourceKeys#SAVE}</li>
     *   <li>{@link ResourceKeys#SAVE_AS}</li>
     *   <li>{@link ResourceKeys#EXIT}</li>
     *   <li>{@link ResourceKeys#TIMEZONE}</li>
     *   <li>{@link ResourceKeys#DATABASE}</li>
     *   <li>{@link ResourceKeys#ABOUT}</li>
     * </ul>
     *
     * Les autres codes sont redirigés vers la fenêtre {@link InternalFrame} active.
     *
     * @return Tache à exécuter en arrière plan pour continuer l'action,
     *         ou <code>null</code> si l'action est terminée.
     * @throws SQLException si une interrogation de la base
     *         de données était nécessaire et a échouée.
     */
    protected Task process(final int clé) throws SQLException
    {
        final Resources resources = Resources.getResources(getLocale());
        Task task=null;
        switch (clé)
        {
            default:
            {
                final JInternalFrame frame=getSelectedFrame();
                if (frame instanceof InternalFrame)
                    task = ((InternalFrame) frame).process(clé);
                break;
            }

            ////////////////////////////////////////////
            ///  Fichier - Nouveau - Séries d'images ///
            ////////////////////////////////////////////
            case ResourceKeys.IMAGES_SERIES:
            {
                task=new Task(resources.getString(ResourceKeys.NEW_IMAGES_SERIES))
                {
                    protected void run() throws SQLException
                    {
                        final DataBase database = getDataBase();
                        final CoordinateChooser chooser=new CoordinateChooser(database.getImageDataBase());
                        chooser.setMultiSeriesAllowed(true);
                        chooser.setTimeZone(getTimeZone());
                        setWaitCursor(false);
                        if (chooser.showDialog(Desktop.this, resources.format(ResourceKeys.NEW_IMAGES_SERIES)))
                        {
                            setWaitCursor(true);
                            final NavigatorFrame frame = new NavigatorFrame(database, chooser, Desktop.this);
                            addFrame(frame);
                            frame.resetDividerLocation(); // Work around a regression bug in JDK 1.4...
                        }
                    }
                };
                break;
            }

            //////////////////////////
            ///  Fichier - Ouvrir  ///
            //////////////////////////
            case ResourceKeys.OPEN:
            {
                final JFileChooser chooser=new JFileChooser(lastDirectory);
                chooser.setFileFilter  (new DefaultFileFilter("*."+EXTENSION, resources.getString(ResourceKeys.DESKTOP_FILES_TYPE_$1, EXTENSION)));
                chooser.setDialogTitle (resources.getString(ResourceKeys.OPEN_DESKTOP));
                chooser.setSelectedFile(lastSavedFile);
                if (chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)
                {
                    final File file = valid(chooser.getSelectedFile());
                    task = getSerializable(file, false);
                    lastDirectory = file.getParent();
                }
                break;
            }

            ///////////////////////////////
            ///  Fichier - Enregistrer  ///
            ///////////////////////////////
            case ResourceKeys.SAVE:
            {
                if (lastSavedFile!=null)
                {
                    task = getSerializable(lastSavedFile, true);
                    break;
                }
                // fall through
            }

            ///////////////////////////////////////
            ///  Fichier - Enregistrer sous...  ///
            ///////////////////////////////////////
            case ResourceKeys.SAVE_AS:
            {
                if (showConfirmDialog(resources.getString(ResourceKeys.WARNING_SERIALIZATION), resources.getString(ResourceKeys.WARNING), JOptionPane.WARNING_MESSAGE, false))
                {
                    final JFileChooser chooser=new JFileChooser(lastDirectory);
                    chooser.setFileFilter  (new DefaultFileFilter("*."+EXTENSION, resources.getString(ResourceKeys.DESKTOP_FILES_TYPE_$1, EXTENSION)));
                    chooser.setDialogTitle (resources.getString(ResourceKeys.SAVE_DESKTOP));
                    chooser.setSelectedFile(lastSavedFile);
                    while (chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)
                    {
                        final File file=valid(chooser.getSelectedFile());
                        if (!file.exists() || showConfirmDialog(resources.getString(ResourceKeys.FILE_ALREADY_EXIST_$1, file.getName()),
                                              resources.getString(ResourceKeys.CONFIRM_OVERWRITE), JOptionPane.QUESTION_MESSAGE, true))
                        {
                            task = getSerializable(file, true);
                            lastDirectory = file.getParent();
                            break;
                        }
                    }
                }
                break;
            }

            //////////////////////////
            ///  Fichier - Quitter ///
            //////////////////////////
            case ResourceKeys.EXIT:
            {
                exit();
            }

            //////////////////////////////////////////////
            ///  Séries - Sommaire des plages de temps ///
            //////////////////////////////////////////////
            case ResourceKeys.IMAGES_CATALOG:
            {
                task=new Task(resources.getString(ResourceKeys.IMAGES_CATALOG))
                {
                    protected void run() throws SQLException
                    {
                        addFrame(new CatalogFrame(getDataBase(), Desktop.this));
                    }
                };
                break;
            }

            //////////////////////////////////////
            ///  Préférences - Fuseau horaire  ///
            //////////////////////////////////////
            case ResourceKeys.TIMEZONE:
            {
                TimeZone selectedTimezone=this.timezone;
                final TimeZoneChooser chooser=new TimeZoneChooser(resources.getString(ResourceKeys.TIMEZONE_SELECTION_MESSAGE));
                chooser.setTimeZone(selectedTimezone);
                chooser.setPreferredSize(new Dimension(320,250));
                final TimeZone newtz;
                try
                {
                    chooser.addPropertyChangeListener(this);
                    newtz = chooser.showDialog(this);
                }
                finally
                {
                    chooser.removePropertyChangeListener(this);
                }
                if (newtz!=null) selectedTimezone=newtz;
                setTimeZone(selectedTimezone);
                break;
            }

            ///////////////////////////////////////
            ///  Préférences - Base de données  ///
            ///////////////////////////////////////
            case ResourceKeys.DATABASES:
            {
                if (showConfirmDialog(resources.getString(ResourceKeys.WARNING_ADVANCED_USER), resources.getString(ResourceKeys.WARNING), JOptionPane.WARNING_MESSAGE, true))
                {
                    new fr.ird.sql.ControlPanel().showDialog(this);
                }
                break;
            }

            ////////////////////////////
            ///  ? - A propos de...  ///
            ////////////////////////////
            case ResourceKeys.ABOUT:
            {
                task=new Task(resources.getString(ResourceKeys.ABOUT))
                {
                    protected void run()
                    {
                        final About about=new About("applicationData/images/About.gif",
                                                    Desktop.class, getDataBase().threads);
                        setWaitCursor(false);
                        about.showDialog(Desktop.this);
                    }
                };
                break;
            }
        }
        return task;
    }
}
