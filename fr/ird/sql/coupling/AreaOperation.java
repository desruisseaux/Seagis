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
package fr.ird.sql.coupling;

// Géométrie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// Divers
import java.util.Date;
import java.sql.SQLException;

// Geotools dependencies
import org.geotools.cv.PointOutsideCoverageException;

// Base de données des pêches
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.EnvironmentTable;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.AverageEvaluator;
import fr.ird.operator.coverage.GradientEvaluator;


/**
 * Une operation appliquée sur une série d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class AreaOperation extends Operation {
    /**
     * Fonction à utiliser pour calculer les valeurs
     * à l'intérieur d'une région géographique.
     */
    private Evaluator evaluator = new GradientEvaluator(0.95);

    /**
     * Construit une nouvelle operation.
     */
    public AreaOperation(final String name, final String column, final String description) {
        super(name, column, description);
    }

    /**
     * Calcule les données de la table "Environnement" sur une surface.
     * Cette méthode est appelée automatiquement par {@link EnvironmentTableFiller#run}.
     *
     * @param  tasks Liste des captures à prendre en compte.
     * @param  coverage Couverture des données environnementales.
     * @param  update Tables des données environnementales à mettre à jour.
     * @throws SQLException si un problème est survenu
     *         lors des accès à la base de données.
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
                // TODO: Il faut définir le temps ici!!!
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
