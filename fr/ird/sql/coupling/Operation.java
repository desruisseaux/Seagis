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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Divers
import java.util.Date;
import java.sql.SQLException;

// JAI
import javax.media.jai.ParameterList;

// Geotools dependencies
import org.geotools.gp.GridCoverageProcessor; // For javadoc
import org.geotools.cv.PointOutsideCoverageException;

// Base de données des pêches
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.EnvironmentTable;


/**
 * Une operation appliquée sur une série d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class Operation {
    /**
     * Le nom de l'opération. Ce nom doit être un des noms reconnus par
     * {@link GridCoverageProcessor}.
     */
    final String name;

    /**
     * La colonne dans la base de données où écrire les valeurs du paramètre environnemental.
     */
    final String column;

    /**
     * La description de l'opération. Cette description n'est utilisée que pour l'affichage
     * dans une interface utilisateur et n'affecte pas l'opération réellement appliquée.
     */
    private final String description;

    /**
     * Indique si un avertissement a déjà été écrit.
     * Seul le premier d'une série d'avertissement sera écrit.
     */
    transient boolean warningReported;

    /**
     * Construit une nouvelle operation. Les arguments <code>name</code> et <code>column</code>
     * sont généralement liés. L'argument <code>name</code> doit être un des noms reconnu par
     * {@link GridCoverageProcessor}, tandis que l'argument <code>column</code> doit être une
     * des colonnes de la table "Environnements" de la base de données. En général, il y a une
     * correspondance de 1 à 1 entre ces deux arguments.
     *
     * @param operation Nom de l'opération (par exemple "GradientMagnitude"),
     *        ou <code>null</code> pour ne plus appliquer d'opération.
     * @param column Nom de colonne dans la base de données dans laquelle mémoriser les
     *        valeurs du paramètre environemental.
     * @param description La description de l'opération. Cette description n'est utilisée
     *        que pour l'affichage dans une interface utilisateur et n'affecte pas l'opération
     *        réellement appliquée.
     */
    public Operation(final String name, final String column, final String description) {
        this.name        = name;
        this.column      = column;
        this.description = description;
    }

    /**
     * Retourne la description de cette opération. Cette description
     * peut apparaître dans une interface utilisateur graphique.
     */
    public String toString() {
        return description;
    }

    /**
     * Setup the parameters for this operation. This method is automatically
     * invoked when {@link EnvironmentTableFiller} is about to apply this
     * operation.
     */
    protected void setup(final ParameterList parameters) {
    }

    /**
     * Indique qu'un point est en dehors de la région des données couvertes.
     * Cette méthode écrit un avertissement dans le journal, mais sans la
     * trace de l'exception (puisque cette erreur peut être normale).
     */
    final void warning(final PointOutsideCoverageException exception) {
        if (!warningReported) {
            final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
            record.setSourceClassName ("EnvironmentTableFiller");
            record.setSourceMethodName("run");
            if (false) record.setThrown(exception);
            Logger.getLogger("fr.ird.sql.fishery").log(record);
            warningReported = true;
        }
    }

    /**
     * Calcule les données ponctuelles de la table "Environnement".
     * Cette méthode est appelée automatiquement par {@link EnvironmentTableFiller#run}.
     *
     * @param  tasks Liste des captures à prendre en compte.
     * @param  coverage Couverture des données environnementales.
     * @param  update Tables des données environnementales à mettre à jour.
     * @throws SQLException si un problème est survenu
     *         lors des accès à la base de données.
     */
    protected void compute(final Task[] tasks, final CatchCoverage coverage, final EnvironmentTable[] update) throws SQLException {
        double[]     values = null;
        final Date     time = new Date();
        final Point2D point = new Point2D.Double();
        final float[] value = new float[1];
        for (int i=0; i<tasks.length; i++) {
            final CatchEntry capture = tasks[i].capture;
            final Line2D line = (Line2D) capture.getShape();
            time.setTime(tasks[i].time);
            if (line != null) {
                final double x1 = line.getX1();
                final double y1 = line.getY1();
                final double x2 = line.getX2();
                final double y2 = line.getY2();
                for (int p=0; p<=100; p+=25) {
                    final double x = (x2-x1)*(p/100.0)+x1;
                    final double y = (y2-y1)*(p/100.0)+y1;
                    point.setLocation(x,y);
                    try {
                        values = coverage.evaluate(point, time, values);
                        warningReported = false;
                    } catch (PointOutsideCoverageException exception) {
                        warning(exception);
                        continue;
                    }
                    for (int c=0; c<update.length; c++) {
                        value[0] = (float)values[c];
                        update[c].set(capture, p, time, value);
                    }
                }
            } else {
                try {
                    values = coverage.evaluate(capture.getCoordinate(), time, values);
                    warningReported = false;
                } catch (PointOutsideCoverageException exception) {
                    warning(exception);
                    continue;
                }
                // Si la capture était en un point seulement,
                // 'EnvironmentTable' se chargera de vérifier
                // si c'était le début où la fin de la ligne.
                for (int c=0; c<update.length; c++) {
                    value[0] = (float)values[c];
                    update[c].set(capture, EnvironmentTable.CENTER, time, value);
                }
            }
        }
    }
}
