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
package fr.ird.operator.coverage;

// Collections
import java.util.Map;
import java.util.HashMap;

// References
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;


/**
 * Classe de base des objets qui sont capable de transformer une image géoréférencée.
 * Une transformation peut consister par exemple à supprimer le grillage d'une image,
 * ou à appliquer une convolution. Certaines opérations peuvent changer les unités du
 * paramètre géophysique.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Operation
{
    /**
     * The operation name.
     */
    private final String name;

    /**
     * Map of transformed image. Keys are original images,
     * and values are weak references to transformed images.
     * This map will be constructed only when first needed.
     */
    private Map<GridCoverage,Reference> filtered;

    /**
     * Queue dans laquelle le ramasse-miettes placera les références vers les images
     * qui ne sont plus utilisée. Le contenu de cette queue sera examinée de façon à
     * éliminer toute les objets {@link Reference} aussitôt que leur référent ont été
     * réclamé par le ramasse-miettes.
     */
    private static ReferenceQueue<GridCoverage> queue;

    /**
     * Thread used for removing enqueded objects from the {@link #filtered} map.
     * Objects are removed as soon as reclamed by the garbage-collector in
     * order to allows the source {@link GridCoverage} to be garbage-collected
     * as well as the transformed {@link GridCoverage}.
     */
    private static Thread cleaner;

    /**
     * Construit un opérateur qui portera le nom spécifié.
     *
     * @param name Nom de cet opérateur.
     */
    public Operation(final String name)
    {this.name=name;}
    
    /**
     * Applique l'opération sur image. Si cette opération a déjà été appliquée
     * sur l'image spécifiée, alors cette méthode tentera de retouner le même
     * objet qui celui qui avait été produit la dernière fois.
     *
     * @param  covarage Image à transformer.
     * @return Image transformée.
     */
    public final synchronized GridCoverage filter(final GridCoverage coverage)
    {
        // Check if a cached instance is available.
        if (filtered != null)
        {
            final Reference ref = filtered.get(coverage);
            if (ref!=null)
            {
                final GridCoverage result = (GridCoverage) ref.get();
                if (result!=null)
                {
                    return result;
                }
            }
        }
        // Performs the operation. This call is usually fast. The real work
        // is defered by {@link javax.media.jai.OpImage} to rendering time.
        final GridCoverage result = doFilter(coverage);
        if (result != coverage)
        {
            // Put the result in a cache for later use. If the
            // cleaner thread was not yet started, start it now.
            synchronized (Operation.class)
            {
                if (queue == null)
                {
                    queue = new ReferenceQueue<GridCoverage>();
                }
                if (cleaner==null || !cleaner.isAlive())
                {
                    cleaner = new Cleaner();
                    cleaner.start();
                }
            }
            if (filtered == null)
            {
                filtered = new HashMap<GridCoverage,Reference>();
            }
            filtered.put(coverage, new Entry(coverage, result));
        }
        return result;
    }

    /**
     * Applique l'opération sur une image. Cette méthode sera appelée automatiquement
     * par {@link #filter} s'il a été déterminé que le résultat ne se trouvait pas déjà
     * dans la cache. Cette méthode peut retourner directement <code>coverage</code> si
     * elle choisit de ne faire aucune opération.
     *
     * @param  covarage Image à transformer.
     * @return Image transformée.
     */
    protected abstract GridCoverage doFilter(final GridCoverage coverage);

    /**
     * Remove a reference from the cache.
     */
    private synchronized void remove(final GridCoverage source)
    {
        if (filtered!=null)
        {
            if (filtered.remove(source)==null)
            {
                // Should not happen
                final LogRecord record = new LogRecord(Level.WARNING, "Missing source");
                record.setSourceClassName("Operation.Cleaner");
                record.setSourceMethodName("run");
                Logger.getLogger("fr.ird.operator").log(record);
            }
        }
    }

    /**
     * Retourne le nom de cet opérateur. Cette méthode ne retourne pas
     * une chaîne de déboguage  car les objets {@link Operation} sont
     * souvent destinés à être insérés dans des composantes visuelles
     * comme {@link javax.swing.JList}, {@link javax.swing.JComboBox}
     * ou {@link javax.swing.JTree}.
     *
     * @return Le nom de cette opérateur, tel qu'il apparaîtra dans
     *         les composantes <cite>Swing</cite>.
     */
    public String toString()
    {return name;}

    /**
     * Free all resources used by this operation.
     */
    public synchronized void dispose()
    {filtered = null;}

    /**
     * Thread used for removing enqueded objects from the {@link #filtered} map.
     * Objects are removed as soon as reclamed by the garbage-collector in
     * order to allows the source {@link GridCoverage} to be garbage-collected
     * as well as the transformed {@link GridCoverage}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Cleaner extends Thread
    {
        /**
         * Construct a new thread.
         */
        public Cleaner()
        {
            super("Transformed GridCoverage cleaner");
            setDaemon(true);
        }

        /**
         * Wait for a reference to be enqueded, and remove the
         * "source-transformed {@link GridCoverage}s" entry from
         * the map.
         */
        public void run()
        {
            while (true)
            {
                try
                {
                    queue.remove().clear();
                }
                catch (InterruptedException exception)
                {
                    // Should not happen.
                    Utilities.unexpectedException("fr.ird.operator", "Operation.Cleaner", "run", exception);
                }
            }
        }
    }

    /**
     * Référence vers une image source et vers le résultat de son filtrage.
     * La référence faible est pour le résultat du filtrage. Toutefois, si
     * ce résultat est réclamé par le ramasse-miette, alors l'image source
     * sera aussitôt retirée de l'ensemble {@link Operation#filtered} de
     * façon à permettre au ramasse miettes de réclamer ce dernier aussi.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class Entry extends WeakReference<GridCoverage>
    {
        /**
         * The source image.
         */
        private final GridCoverage source;

        /**
         * Construit une référence vers l'images spécifiée.
         *
         * @param source Image source. Cette image sera retenue par une référence forte.
         * @param result Image filtrée. Cette image sera retenue par une référence faible.
         */
        public Entry(final GridCoverage source, final GridCoverage result)
        {
            super(result, queue);
            this.source = source;
        }

        /**
         * Remove this reference.
         */
        public void clear()
        {
            super.clear();
            remove(source);
        }
    }
}
