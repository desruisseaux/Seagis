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


/**
 * A class managing repaint events synchronized on a lock. If a map should not
 * be repainted while its features are changing, the feature changes should be
 * protected inside a <code>synchronized(lock)</code> block.   However, such a
 * synchronized statement inside the Swing thread may prevent Swing to repaint
 * other components until the lock is released. In order to reduce this negative
 * impact, {@link #repaint} is implemented in a such a way that all repaint events
 * are delayed until the lock is released. Once the lock is released, multiple
 * repaint events are collapsed in the Swing thread.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class RepaintManager
{
    /**
     * The thread managing repaint events. Must be a separated
     * object in order to give {@link #finalize} a chance to
     * terminate this thread.
     */
    private final RepaintThread thread;

    /**
     * Construct a new repaint manager.
     *
     * @param lock The object to synchronize on, or <code>null</code> if none.
     *        All {@link Layer#repaint} invocations will occur inside a
     *        <code>synchronized(lock)</code> block.
     */
    public RepaintManager(final Object lock)
    {
        thread = new RepaintThread(lock);
        thread.start();
    }

    /**
     * Schedule a layer for repaint. This method can be invoked from any thread,
     * which may or may not be the Swing thread. In all cases, this method returns
     * immediately. All repaint events are enqueued for later processing in the
     * Swing thread.
     *
     * <ul>
     *   <li>If a lock on <code>lock</code> is hold, then the repaint event will
     *       be delayed until the lock is released. Other repaint events may be
     *       enqueued in the main time.</li>
     *   <li>Once the lock has been released, {@link Layer#repaint} is invoked
     *       in the Swing thread for all pending repaint events. Processing all
     *       events at once in the Swing thread give a chance for Swing to merge
     *       multiple events.</li>
     * </ul>
     */
    public void repaint(final Layer layer)
    {
        thread.repaint(layer);
    }

    /**
     * Release any resources hold by this object.
     */
    protected void finalize() throws Throwable
    {
        thread.cancel();
        super.finalize();
    }
}
