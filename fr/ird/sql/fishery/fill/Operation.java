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
package fr.ird.sql.fishery.fill;

// J2SE et JAI
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gp.GridCoverageProcessor; // Pour javadoc
import org.geotools.cv.PointOutsideCoverageException;

// Base de donn�es des p�ches
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.EnvironmentTable;


/**
 * Une operation appliqu�e sur une s�rie d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Operation {
    /**
     * Le nom de l'op�ration. Ce nom doit �tre un des noms reconnus par
     * {@link GridCoverageProcessor}.
     */
    final String name;

    /**
     * La colonne dans la base de donn�es o� �crire les valeurs du param�tre environnemental.
     */
    final String column;

    /**
     * La description de l'op�ration. Cette description n'est utilis�e que pour l'affichage
     * dans une interface utilisateur et n'affecte pas l'op�ration r�ellement appliqu�e.
     */
    private final String description;

    /**
     * Construit une nouvelle operation. Les arguments <code>name</code> et <code>column</code>
     * sont g�n�ralement li�s. L'argument <code>name</code> doit �tre un des noms reconnu par
     * {@link GridCoverageProcessor}, tandis que l'argument <code>column</code> doit �tre une
     * des colonnes de la table "Environnements" de la base de donn�es. En g�n�ral, il y a une
     * correspondance de 1 � 1 entre ces deux arguments.
     *
     * @param operation Nom de l'op�ration (par exemple "GradientMagnitude"),
     *        ou <code>null</code> pour ne plus appliquer d'op�ration.
     * @param column Nom de colonne dans la base de donn�es dans laquelle m�moriser les
     *        valeurs du param�tre environemental.
     * @param description La description de l'op�ration. Cette description n'est utilis�e
     *        que pour l'affichage dans une interface utilisateur et n'affecte pas l'op�ration
     *        r�ellement appliqu�e.
     */
    public Operation(final String name, final String column, final String description) {
        this.name        = name;
        this.column      = column;
        this.description = description;
    }

    /**
     * Retourne la description de cette op�ration. Cette description
     * peut appara�tre dans une interface utilisateur graphique.
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
     * Indique qu'un point est en dehors de la r�gion des donn�es couvertes.
     * Cette m�thode �crit un avertissement dans le journal, � la condition
     * qu'il n'y en avait pas d�j� un.
     */
    private void warning(final CatchCoverage source, final PointOutsideCoverageException exception) {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setSourceClassName ("EnvironmentTableFiller");
        record.setSourceMethodName("run");
        record.setThrown(exception);
        if (source.lastWarning == null) {
            source.log(record);
        }
        source.lastWarning = record;
    }

    /**
     * Calcule les donn�es ponctuelles de la table "Environnement".
     * Cette m�thode est appel�e automatiquement par {@link EnvironmentTableFiller#run}.
     *
     * @param  tasks Liste des captures � prendre en compte.
     * @param  coverage Couverture des donn�es environnementales.
     * @param  update Tables des donn�es environnementales � mettre � jour.
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s � la base de donn�es.
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
                        coverage.lastWarning = null;
                    } catch (PointOutsideCoverageException exception) {
                        warning(coverage, exception);
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
                    coverage.lastWarning = null;
                } catch (PointOutsideCoverageException exception) {
                    warning(coverage, exception);
                    continue;
                }
                // Si la capture �tait en un point seulement,
                // 'EnvironmentTable' se chargera de v�rifier
                // si c'�tait le d�but o� la fin de la ligne.
                for (int c=0; c<update.length; c++) {
                    value[0] = (float)values[c];
                    update[c].set(capture, EnvironmentTable.CENTER, time, value);
                }
            }
        }
    }
}
