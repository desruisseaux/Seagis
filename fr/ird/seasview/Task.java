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

// Miscellaneous
import java.awt.Cursor;
import java.awt.EventQueue;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

// Geotools dependencies
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Classe de base des objets capable d'effectuer une tache en arri�re-plan. Cette
 * tache consistera habituellement � contruire une fen�tre qui appara�tra sur le
 * bureau. Lorsque ces objets sont enregistr�s (<i>serialized</i>), ils doivent
 * enregistrer assez d'informations pour �tre capable de reconstruire la fen�tre
 * lors d'une lecture dans une autre session Java. Cette classe peut ainsi servir
 * � exp�dier le contenu d'une fen�tre via le r�seau avec les RMI.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Task implements Serializable {
    /**
     * Nom de cet objet. Ce nom servira � nommer le thread qui ex�cutera
     * l'action en arri�re-plan. Cette information est surtout utile �
     * des fins de d�bogages.
     */
    private final String name;

    /**
     * Action qui a lanc� ce travail. Cette action permet de
     * conna�tre le bureau sur lequel placer les fen�tres.
     * Ce champ est d�fini par {@link #run(Action)}.
     */
    private transient Action action;

    /**
     * Constructeur de base.
     *
     * @param name Nom de la tache. Le choix du nom est laiss� libre. Il n'a
     *        pas d'impact sur l'ex�cution et ne sert qu'� faciliter le d�bogage.
     */
    protected Task(final String name) {
        this.name = name;
    }

    /**
     * Retourne le bureau sur lequel placer les fen�tres.
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
     * Si cette m�thode est d�j� dans un autre thread, alors {@link #run()}
     * sera ex�cut� imm�diatement sans cr�er de nouveau thread.
     *
     * @param  source T�che qui a lanc� ce travail. Cette action permet entre
     *         autres de conna�tre le bureau sur lequel placer les fen�tres.
     * @throws Exception Si le travail a �t� ex�cut� dans le thread courant et
     *         qu'une erreur est survenu.
     */
    final void run(final Task task) throws Exception {
        run(task.action);
    }

    /**
     * Appele {@link #run()} dans un thread autre que celui de <i>Swing</i>.
     * Si cette m�thode est d�j� dans un autre thread, alors {@link #run()}
     * sera ex�cut� imm�diatement sans cr�er de nouveau thread.
     *
     * @param  source Action qui a lanc� ce travail. Cette action permet entre
     *         autres de conna�tre le bureau sur lequel placer les fen�tres.
     * @throws Exception Si le travail a �t� ex�cut� dans le thread courant et
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
            source.setEnabled(false); // En principe d�j� fait par 'Action'.
            thread.start();
        } else try {
            action = source;
            run();
        }  finally {
            action = null;
        }
    }

    /**
     * Appele {@link #run()} dans le thread courant,  puis ex�cute <code>clear</code>
     * dans le thread de <i>Swing</i>. Si une exception est lanc�e durant l'ex�cution
     * de {@link #run()}, elle sera attrap�e et affich�e dans une bo�te de dialogue.
     *
     * @param source Action qui a lanc� ce travail. Cette action permet entre
     *               autres de conna�tre le bureau sur lequel placer les fen�tres.
     * @param clear  M�thode � ex�cuter dans le thread de <i>Swing</i> apr�s
     *               {@link #run()}. Cet argument ne doit pas �tre <code>null</code>.
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
     * Effectue le travail. Cette m�thode est appel�e automatiquement par
     * un bureau � partir d'un thread autre que celui de <i>Swing</i>).
     *
     * @throws Exception si une erreur est survenue lors du travail. Cette erreur
     *         sera attrap�e par le bureau et affich�e dans une fen�tre.
     */
    protected abstract void run() throws Exception;

    /**
     * Change le curseur de la souris pour le sablier ou pour la fl�che standard.
     * Cette m�thode est con�ue pour �tre appel�e � partir de {@link #run()},
     * directement ou indirectement. Elle peut �tre appel�e � partir de n'importe
     * quel thread (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param wait <code>true</code> pour changer le curseur de la souris en sablier,
     *             ou <code>false</code> pour faire r�appara�tre le curseur standard.
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
     * Retourne le base de donn�es de l'application. Cette m�thode
     * est con�ue pour �tre appel�e � partir de {@link #run()}
     */
    protected final DataBase getDataBase() {
        return getDesktop(action).getDataBase();
    }

    /**
     * Fait appara�tre sur le bureau la fen�tre sp�cifi�e. Cette m�thode est
     * con�ue pour �tre appel�e � partir de {@link #run()}, directement ou
     * indirectement. Elle peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @throws IllegalStateException si cette m�thode n'est pas appel�e �
     *         partir de {@link #run()}, ou que {@link #run()} n'a pas �t�
     *         appel�e � partir d'un bureau.
     */
    protected final void show(final InternalFrame frame) throws IllegalStateException {
        getDesktop(action).addFrame(frame);
    }

    /**
     * Fait appara�tre dans une fen�tre la trace de l'exception sp�cifi�e.
     * Cette m�thode peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     */
    protected final void show(final Throwable exception) {
        ExceptionMonitor.show(action!=null ? action.getDesktop() : null, exception, getTitle(exception));
    }

    /**
     * Retourne un titre pour l'exception sp�cifi�, ou <code>null</code>
     * pour d�terminer ce titre automatiquement. L'impl�mentation par
     * d�faut retourne toujours <code>null</code>.
     */
    String getTitle(final Throwable exception) {
        return null;
    }

    /**
     * Retourne une cha�ne de caract�re repr�sentant cette tache.
     */
    public String toString() {
        return "Task["+name+']';
    }

    /**
     * Ex�cute <code>runnable</code> dans le thread de <i>Swing</i>, et attend
     * que son ex�cution se soit termin�e avant de retourner. Cette m�thode ne
     * doit pas �tre appel�e � partir du thread de <i>Swing</i>.
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
