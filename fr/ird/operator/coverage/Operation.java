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
 * Classe de base des objets qui sont capable de transformer une image g�or�f�renc�e.
 * Une transformation peut consister par exemple � supprimer le grillage d'une image,
 * ou � appliquer une convolution. Certaines op�rations peuvent changer les unit�s du
 * param�tre g�ophysique.
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
     * Queue dans laquelle le ramasse-miettes placera les r�f�rences vers les images
     * qui ne sont plus utilis�e. Le contenu de cette queue sera examin�e de fa�on �
     * �liminer toute les objets {@link Reference} aussit�t que leur r�f�rent ont �t�
     * r�clam� par le ramasse-miettes.
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
     * Construit un op�rateur qui portera le nom sp�cifi�.
     *
     * @param name Nom de cet op�rateur.
     */
    public Operation(final String name)
    {this.name=name;}
    
    /**
     * Applique l'op�ration sur image. Si cette op�ration a d�j� �t� appliqu�e
     * sur l'image sp�cifi�e, alors cette m�thode tentera de retouner le m�me
     * objet qui celui qui avait �t� produit la derni�re fois.
     *
     * @param  covarage Image � transformer.
     * @return Image transform�e.
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
     * Applique l'op�ration sur une image. Cette m�thode sera appel�e automatiquement
     * par {@link #filter} s'il a �t� d�termin� que le r�sultat ne se trouvait pas d�j�
     * dans la cache. Cette m�thode peut retourner directement <code>coverage</code> si
     * elle choisit de ne faire aucune op�ration.
     *
     * @param  covarage Image � transformer.
     * @return Image transform�e.
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
     * Retourne le nom de cet op�rateur. Cette m�thode ne retourne pas
     * une cha�ne de d�boguage  car les objets {@link Operation} sont
     * souvent destin�s � �tre ins�r�s dans des composantes visuelles
     * comme {@link javax.swing.JList}, {@link javax.swing.JComboBox}
     * ou {@link javax.swing.JTree}.
     *
     * @return Le nom de cette op�rateur, tel qu'il appara�tra dans
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
     * R�f�rence vers une image source et vers le r�sultat de son filtrage.
     * La r�f�rence faible est pour le r�sultat du filtrage. Toutefois, si
     * ce r�sultat est r�clam� par le ramasse-miette, alors l'image source
     * sera aussit�t retir�e de l'ensemble {@link Operation#filtered} de
     * fa�on � permettre au ramasse miettes de r�clamer ce dernier aussi.
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
         * Construit une r�f�rence vers l'images sp�cifi�e.
         *
         * @param source Image source. Cette image sera retenue par une r�f�rence forte.
         * @param result Image filtr�e. Cette image sera retenue par une r�f�rence faible.
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
