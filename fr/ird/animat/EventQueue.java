/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat;

// J2SE dependencies
import java.util.LinkedList;


/**
 * The thread managing events synchronized on a lock. An instance of this thread
 * is created by {@link Environment}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EventQueue extends Thread {
    /**
     * The group for all <code>EventQueue</code>.
     */
    private static final ThreadGroup GROUP = new ThreadGroup("Animats EventQueue");

    /**
     * The object to synchronize on. No event will be fired as long as an other
     * thread holds the lock.
     */
    final Object lock;

    /**
     * List of events to fire once the {@link #lock} is released.
     */
    private final LinkedList<Runnable> events = new LinkedList<Runnable>();

    /**
     * Set to <code>true</code> when this thread must exit. This
     * is set during finalization of {@link Environment}.
     */
    private transient boolean kill;

    /**
     * Construct a new event thread. The thread will be started later, once needed.
     *
     * @param lock The object to synchronize on, or <code>null</code> if none.
     *        All events will be fired in this thread inside a
     *        <code>synchronized(lock)</code> block.
     */
    public EventQueue(final Object lock) {
        super(GROUP, "RepaintManager");
        this.lock = lock;
        setPriority(NORM_PRIORITY);
        setDaemon(true);
    }

    /**
     * Schedule an event to be fired. This method can be invoked from any thread,
     * which may or may not be the Swing thread. In all cases, this method returns
     * immediately. All events are enqueued for later processing in the Swing thread.
     *
     * <ul>
     *   <li>If a lock on {@link #lock} is hold, then the event will be delayed until
     *       the lock is released. Other repaint events may be enqueued in the main time.</li>
     *   <li>Once the lock has been released, events are fired in the Swing thread for all
     *       pending events. Processing all events at once in the Swing thread give a chance
     *       for Swing to merge multiple repaint events.</li>
     * </ul>
     *
     * This method is usually invoked in a thread holding the <code>lock</code>.
     */
    public void invokeLater(final Runnable event) {
        synchronized (lock) {
            final boolean start = events.isEmpty();
            events.addLast(event);
            if (start) {
                if (isAlive()) {
                    // Needs the lock
                    lock.notifyAll();
                } else {
                    start();
                }
            }
        }
    }

    /**
     * The running thread. Process all pending events,
     * and then wait for the next {@link #notify} invocation.
     */
    public void run() {
        synchronized (lock) {
            while (!kill) {
                /*
                 * If we are going to wait for the lock,  block in this thread instead
                 * of Swing thread. We will fire events in Swing thread later in order
                 * to gives it a chance to collapse multiple repaint events.
                 */
                while (!events.isEmpty()) {
                    java.awt.EventQueue.invokeLater(events.removeFirst());
                }
                try {
                    // Release the lock and wait for notification.
                    lock.wait();
                } catch (InterruptedException exception) {
                    // Somebody doesn't want to lets us sleep.
                    // Go back to work...
                }
            }
        }
    }

    /**
     * Terminate this thread. This method must be invoked in order to lets
     * the garbage collector do its work.
     */
    public void dispose() {
        synchronized (lock) {
            kill = true;
            lock.notifyAll();
        }
    }
}
