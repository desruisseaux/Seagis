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

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;
import org.geotools.util.WeakValueHashMap;


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
    private Map filtered;

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
}
