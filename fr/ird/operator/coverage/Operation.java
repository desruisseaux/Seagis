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

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;
import org.geotools.util.WeakValueHashMap;


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
    private Map filtered;

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
            final GridCoverage result = (GridCoverage) filtered.get(coverage);
            if (result!=null)
            {
                return result;
            }
        }
        // Performs the operation. This call is usually fast. The real work
        // is defered by {@link javax.media.jai.OpImage} to rendering time.
        final GridCoverage result = doFilter(coverage);
        if (result != coverage)
        {
            if (filtered == null)
            {
                filtered = new WeakValueHashMap();
            }
            filtered.put(coverage, result);
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
}
