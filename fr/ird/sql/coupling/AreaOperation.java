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
package fr.ird.sql.coupling;

// G�om�trie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// Divers
import java.util.Date;
import java.sql.SQLException;

// Geotools dependencies
import org.geotools.cv.PointOutsideCoverageException;

// Base de donn�es des p�ches
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.EnvironmentTable;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.AverageEvaluator;
import fr.ird.operator.coverage.GradientEvaluator;


/**
 * Une operation appliqu�e sur une s�rie d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class AreaOperation extends Operation {
    /**
     * Fonction � utiliser pour calculer les valeurs
     * � l'int�rieur d'une r�gion g�ographique.
     */
    private Evaluator evaluator = new GradientEvaluator(0.95);

    /**
     * Construit une nouvelle operation.
     */
    public AreaOperation(final String name, final String column, final String description) {
        super(name, column, description);
    }

    /**
     * Calcule les donn�es de la table "Environnement" sur une surface.
     * Cette m�thode est appel�e automatiquement par {@link EnvironmentTableFiller#run}.
     *
     * @param  tasks Liste des captures � prendre en compte.
     * @param  coverage Couverture des donn�es environnementales.
     * @param  update Tables des donn�es environnementales � mettre � jour.
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s � la base de donn�es.
     */
    protected void compute(final Task[] tasks, final CatchCoverage coverage, final EnvironmentTable[] update) throws SQLException {
        final Date     time = new Date();
        final float[] value = new float[1];
        for (int i=0; i<tasks.length; i++) {
            final CatchEntry capture = tasks[i].capture;
            final Shape area = coverage.getShape(capture);
            time.setTime(tasks[i].time);
            final double[] values;
            try {
                // TODO: Il faut d�finir le temps ici!!!
                values = coverage.evaluate(capture, evaluator);
                warningReported = false;
            } catch (PointOutsideCoverageException exception) {
                warning(exception);
                continue;
            }
            for (int c=0; c<update.length; c++) {
                value[0] = (float)values[c];
                update[c].set(capture, EnvironmentTable.CENTER, time, value);
            }
        }
    }
}
