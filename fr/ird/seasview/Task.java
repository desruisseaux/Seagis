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
package fr.ird.seasview;

// Miscellaneous
import java.awt.Cursor;
import java.awt.EventQueue;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

// Geotools dependencies
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Classe de base des objets capable d'effectuer une tache en arrière-plan. Cette
 * tache consistera habituellement à contruire une fenêtre qui apparaîtra sur le
 * bureau. Lorsque ces objets sont enregistrés (<i>serialized</i>), ils doivent
 * enregistrer assez d'informations pour être capable de reconstruire la fenêtre
 * lors d'une lecture dans une autre session Java. Cette classe peut ainsi servir
 * à expédier le contenu d'une fenêtre via le réseau avec les RMI.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Task implements Serializable {
    /**
     * Nom de cet objet. Ce nom servira à nommer le thread qui exécutera
     * l'action en arrière-plan. Cette information est surtout utile à
     * des fins de débogages.
     */
    private final String name;

    /**
     * Action qui a lancé ce travail. Cette action permet de
     * connaître le bureau sur lequel placer les fenêtres.
     * Ce champ est défini par {@link #run(Action)}.
     */
    private transient Action action;

    /**
     * Constructeur de base.
     *
     * @param name Nom de la tache. Le choix du nom est laissé libre. Il n'a
     *        pas d'impact sur l'exécution et ne sert qu'à faciliter le débogage.
     */
    protected Task(final String name) {
        this.name = name;
    }

    /**
     * Retourne le bureau sur lequel placer les fenêtres.
     */
    private static Desktop getDesktop(final Action action) {
        if (action != null) {
            final Desktop desktop = action.getDesktop();
            if (desktop != null) {
                return desktop;
            }
        }
        throw new IllegalStateException("No desktop");
    }

    /**
     * Appele {@link #run()} dans un thread autre que celui de <i>Swing</i>.
     * Si cette méthode est déjà dans un autre thread, alors {@link #run()}
     * sera exécuté immédiatement sans créer de nouveau thread.
     *
     * @param  source Tâche qui a lancé ce travail. Cette action permet entre
     *         autres de connaître le bureau sur lequel placer les fenêtres.
     * @throws Exception Si le travail a été exécuté dans le thread courant et
     *         qu'une erreur est survenu.
     */
    final void run(final Task task) throws Exception {
        run(task.action);
    }

    /**
     * Appele {@link #run()} dans un thread autre que celui de <i>Swing</i>.
     * Si cette méthode est déjà dans un autre thread, alors {@link #run()}
     * sera exécuté immédiatement sans créer de nouveau thread.
     *
     * @param  source Action qui a lancé ce travail. Cette action permet entre
     *         autres de connaître le bureau sur lequel placer les fenêtres.
     * @throws Exception Si le travail a été exécuté dans le thread courant et
     *         qu'une erreur est survenu.
     */
    final synchronized void run(final Action source) throws Exception {
        if (action != null) {
            throw new AssertionError(action.getValue(Action.NAME));
        }
        if (EventQueue.isDispatchThread()) {
            final Desktop desktop = getDesktop(source);
            final Runnable  clear = new Runnable() {
                public void run() {
                    desktop.setCursor(null);
                    source.setEnabled(true);
                    desktop.stateChanged();
                }
            };
            final Thread thread=desktop.createThread(name, new Runnable() {
                public void run() {
                    Task.this.run(source, clear);
                }
            });
            desktop.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            source.setEnabled(false); // En principe déjà fait par 'Action'.
            thread.start();
        } else try {
            action = source;
            run();
        }  finally {
            action = null;
        }
    }

    /**
     * Appele {@link #run()} dans le thread courant,  puis exécute <code>clear</code>
     * dans le thread de <i>Swing</i>. Si une exception est lancée durant l'exécution
     * de {@link #run()}, elle sera attrapée et affichée dans une boîte de dialogue.
     *
     * @param source Action qui a lancé ce travail. Cette action permet entre
     *               autres de connaître le bureau sur lequel placer les fenêtres.
     * @param clear  Méthode à exécuter dans le thread de <i>Swing</i> après
     *               {@link #run()}. Cet argument ne doit pas être <code>null</code>.
     */
    private synchronized void run(final Action source, final Runnable clear) {
        try {
            if (action != null) {
                throw new AssertionError(action.getValue(Action.NAME));
            }
            action = source;
            run();
            action = null;
            EventQueue.invokeLater(clear);
        } catch (Throwable exception) {
            action = null;
            invokeAndWait(clear);
            ExceptionMonitor.show(getDesktop(source), exception);
        }
    }

    /**
     * Effectue le travail. Cette méthode est appelée automatiquement par
     * un bureau à partir d'un thread autre que celui de <i>Swing</i>).
     *
     * @throws Exception si une erreur est survenue lors du travail. Cette erreur
     *         sera attrapée par le bureau et affichée dans une fenêtre.
     */
    protected abstract void run() throws Exception;

    /**
     * Change le curseur de la souris pour le sablier ou pour la flèche standard.
     * Cette méthode est conçue pour être appelée à partir de {@link #run()},
     * directement ou indirectement. Elle peut être appelée à partir de n'importe
     * quel thread (pas nécessairement celui de <i>Swing</i>).
     *
     * @param wait <code>true</code> pour changer le curseur de la souris en sablier,
     *             ou <code>false</code> pour faire réapparaître le curseur standard.
     */
    protected final void setWaitCursor(final boolean wait) {
        final Desktop desktop = getDesktop(action);
        final Cursor cursor = wait ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null;
        if (EventQueue.isDispatchThread()) {
            desktop.setCursor(cursor);
        } else {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    desktop.setCursor(cursor);
                }
            });
        }
    }

    /**
     * Retourne le base de données de l'application. Cette méthode
     * est conçue pour être appelée à partir de {@link #run()}
     */
    protected final DataBase getDataBase() {
        return getDesktop(action).getDataBase();
    }

    /**
     * Fait apparaître sur le bureau la fenêtre spécifiée. Cette méthode est
     * conçue pour être appelée à partir de {@link #run()}, directement ou
     * indirectement. Elle peut être appelée à partir de n'importe quel thread
     * (pas nécessairement celui de <i>Swing</i>).
     *
     * @throws IllegalStateException si cette méthode n'est pas appelée à
     *         partir de {@link #run()}, ou que {@link #run()} n'a pas été
     *         appelée à partir d'un bureau.
     */
    protected final void show(final InternalFrame frame) throws IllegalStateException {
        getDesktop(action).addFrame(frame);
    }

    /**
     * Fait apparaître dans une fenêtre la trace de l'exception spécifiée.
     * Cette méthode peut être appelée à partir de n'importe quel thread
     * (pas nécessairement celui de <i>Swing</i>).
     */
    protected final void show(final Throwable exception) {
        ExceptionMonitor.show(action!=null ? action.getDesktop() : null, exception, getTitle(exception));
    }

    /**
     * Retourne un titre pour l'exception spécifié, ou <code>null</code>
     * pour déterminer ce titre automatiquement. L'implémentation par
     * défaut retourne toujours <code>null</code>.
     */
    String getTitle(final Throwable exception) {
        return null;
    }

    /**
     * Retourne une chaîne de caractère représentant cette tache.
     */
    public String toString() {
        return "Task["+name+']';
    }

    /**
     * Exécute <code>runnable</code> dans le thread de <i>Swing</i>, et attend
     * que son exécution se soit terminée avant de retourner. Cette méthode ne
     * doit pas être appelée à partir du thread de <i>Swing</i>.
     *
     * @see EventQueue#invokeAndWait
     */
    public static void invokeAndWait(final Runnable runnable) {
        try {
            EventQueue.invokeAndWait(runnable);
        } catch (InterruptedException exception) {
            IllegalThreadStateException e = new IllegalThreadStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        } catch (InvocationTargetException target) {
            final Throwable exception = target.getTargetException();
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }
            if (exception instanceof Error) {
                throw (Error) exception;
            }
            throw new UndeclaredThrowableException(exception, exception.getLocalizedMessage());
        }
    }
}
