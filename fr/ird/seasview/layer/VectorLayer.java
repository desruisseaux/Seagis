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
package fr.ird.seasview.layer;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.ct.TransformException;
import org.geotools.renderer.j2d.RenderedGridMarks;


/**
 * Repr�sentation graphique d'un champ de vecteur construit � partir des donn�es
 * d'images. Cette classe peut servir lorsque l'on dispose d'une image avec au
 * moins deux bandes. Une bande peut repr�senter la composante U d'un vecteur,
 * tandis que l'autre repr�sente la composante V.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class VectorLayer extends RenderedGridMarks {
    /**
     * Construit un champ de vecteur qui utilisera l'image sp�cifi�e.
     *
     * @param  coverage Les donn�es � utiliser.
     * @param  bandU Bande de la composante U des vecteurs.
     * @param  bandV Bande de la composante V des vecteurs.
     */
    public VectorLayer(final GridCoverage coverage, final int bandU, final int bandV)
            throws TransformException
    {
        super(null);
        setBands(new int[]{bandU, bandV});
        setGridCoverage(coverage);
    }

    /**
     * Retourne l'amplitude typique des donn�es de cette couche.
     */
    public double getTypicalAmplitude() {
        return 15; // TODO
    }
}
