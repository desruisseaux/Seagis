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
package fr.ird.map;

// J2SE dependencies
import java.util.LinkedList;
import java.awt.EventQueue;


/**
 * The thread managing {@link Layer#repaint()} events synchronized on a lock.
 * Instance of this thread are created by {@link RepaintManager}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class RepaintThread extends Thread
{
    /**
     * The group for all <code>RepaintThread</code>.
     */
    private static final ThreadGroup GROUP = new ThreadGroup("RepaintManager");

    /**
     * The object to synchronize on.
     */
    private final Object lock;

    /**
     * List of layers to repaint.
     */
    private final LinkedList<Layer> layers = new LinkedList<Layer>();

    /**
     * The {@link Runnable} to run for repainting layers.
     */
    private final Runnable repaint = new Runnable()
    {
        public void run()
        {repaint();}
    };

    /**
     * Set to <code>true</code> when this thread must exit. This
     * is set during finalization of {@link RepaintManager}.
     */
    private transient boolean kill;

    /**
     * Construct a new repaint thread. {@link Thread#start} must be invoked
     * after the construction.
     *
     * @param lock The object to synchronize on, or <code>null</code> if none.
     *        All {@link Layer#repaint} invocations will occur inside a
     *        <code>synchronized(lock)</code> block.
     */
    public RepaintThread(final Object lock)
    {
        super(GROUP, "RepaintManager");
        this.lock = (lock!=null) ? (Object)lock : (Object)layers;
        setPriority(NORM_PRIORITY);
        setDaemon(true);
    }

    /**
     * Schedule a layer for repaint. This method can be invoked from any thread,
     * which may or may not be the Swing thread. In all cases, this method returns
     * immediately. All repaint events are enqueued for later processing in the
     * Swing thread.
     *
     * <ul>
     *   <li>If a lock on {@link #lock} is hold, then the repaint event will
     *       be delayed until the lock is released. Other repaint events may
     *       be enqueued in the main time.</li>
     *   <li>Once the lock has been released, {@link Layer#repaint} is invoked
     *       in the Swing thread for all pending repaint events. Processing all
     *       events at once in the Swing thread give a chance for Swing to merge
     *       multiple events.</li>
     * </ul>
     */
    public void repaint(final Layer layer)
    {
        synchronized (lock)
        {
            final boolean start = layers.isEmpty();
            layers.addLast(layer);
            if (start)
            {
                // Needs the lock
                lock.notifyAll();
            }
        }
    }

    /**
     * Repaint all layers in "first in, first out" order.
     */
    private void repaint()
    {
        synchronized (lock)
        {
            while (!layers.isEmpty())
            {
                layers.removeFirst().repaint();
            }
        }
    }

    /**
     * The running thread. Process all pending paint events,
     * and then wait for the next {@link #notify} invocation.
     */
    public void run()
    {
        synchronized (lock)
        {
            while (!kill)
            {
                /*
                 * If we are going to wait for the lock, block in this thread
                 * instead of Swing thread. We will invoke 'repaint' in Swing
                 * thread later in order to give it a chance to collapse
                 * multiple repaint events.
                 */
                if (!layers.isEmpty())
                {
                    EventQueue.invokeLater(repaint);
                }
                try
                {
                    // Release the lock and wait for notification.
                    lock.wait();
                }
                catch (InterruptedException exception)
                {
                    // Somebody doesn't want to lets us sleep.
                    // Go back to work...
                }
            }
        }
    }

    /**
     * Terminate this thread.
     */
    public void cancel()
    {
        synchronized (lock)
        {
            kill = true;
            lock.notifyAll();
        }
    }
}
