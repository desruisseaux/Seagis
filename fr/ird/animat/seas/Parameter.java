/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat.seas;

// Base de donn�es
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.ParameterValue;
import fr.ird.operator.coverage.MaximumEvaluator;


/**
 * Un param�tre environnemental � �valuer.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Parameter
{
    /**
     * Nom de la s�rie d'images.
     * Exemples:
     * <ul>
     *   <li>Pompage d'Ekman</li>
     *   <li>SLA (Monde - TP/ERS)</li>
     *   <li>SST (synth�se)</li>
     *   <li>Chlorophylle-a (R�union)</li>
     * </ul>
     */
    public final String series;

    /**
     * Nom de l'op�ration � appliquer, ou <code>null</code> si aucune.
     * Exemples:
     * <ul>
     *   <li>GradientMagnitude</li>
     * </ul>
     */
    public final String operation;

    /**
     * Nom de l'�valuateur � utiliser.
     * Exemples:
     * <ul>
     *   <li>Maximum</li>
     *   <li>Average</li>
     * </ul>
     */
    public final String evaluator;

    /**
     * Construit un param�tre.
     */
    public Parameter(final String series, final String operation, final String evaluator)
    {
        this.series    = series.trim();
        this.operation = operation; // May be null.
        this.evaluator = evaluator.trim();
    }
}
