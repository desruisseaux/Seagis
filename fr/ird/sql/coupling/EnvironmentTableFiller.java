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
package fr.ird.sql.coupling;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cv.PointOutsideCoverageException;

// Requêtes SQL
import java.sql.SQLException;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.fishery.EnvironmentTable;

// Géométrie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// Ensembles
import java.util.List;
import java.util.Iterator;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Divers
import java.util.Date;
import javax.media.jai.util.Range;
import org.geotools.resources.Utilities;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.AverageEvaluator;
import fr.ird.operator.coverage.GradientEvaluator;


/**
 * Classe ayant la charge de remplir la table  "Environnement"  de la base de données "Pêches" à
 * partir des données satellitaires. La table "Environnement" contient les valeurs de paramètres
 * environnementaux (température, chlorophylle-a, hauteur de l'eau, etc.) aux positions de pêches.
 * Lorsque <code>EnvironmentTableFiller</code> trouve une données environnementale à une position
 * de pêche, il met à jour la cellule correspondante de la table "Environnement". S'il ne trouve
 * pas de données ou que la donnée est manquante (par exemple sous un nuage), alors
 * <code>EnvironmentTableFiller</code> <strong>laisse inchangée</strong> la cellule correspondante.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class EnvironmentTableFiller
{
    /**
     * <code>true</code> si on veut seulement tester cette classe sans écrire
     * dans la base de données. Note: une connexion en lecture aux bases de
     * données est tout de même nécessaire.
     */
    private static final boolean TEST_ONLY = false;

    /**
     * La durée d'une journée, en nombre de millisecondes.
     */
    private static final long DAY = 24*60*60*1000L;

    /**
     * Jours où extraire des données, avant,
     * pendant et après le jour de la pêche.
     */
    private static final int[] DAYS_TO_EVALUATE = {-15, -10, -5, 0, 5};

    /**
     * Séries de données à utiliser pour le remplissage des colonnes.
     * Pour chaque tableau <code>String[]</code>, le premier élément
     * représente la série et les éléments suivants les colonnes de
     * la table "Environnement" à remplir pour chaque canal des images
     * de la série.
     */
    private static final String[][] SERIES =
    {
        {"Bathymétrie de Sandwell (Réunion)", "FLR"},
        {"Bathymétrie de Baudry (Réunion)",   "FLR"},
        {"Pompage d'Ekman",                   "EKP"},
        {"SLA (Réunion - NRT)",               "SLA", "U", "V"},
        {"SLA (Réunion)",                     "SLA", "U", "V"},
        {"SLA (Monde - TP)",                  "SLA"},
        {"SLA (Monde - TP/ERS)",              "SLA"},
        {"SST (synthèse)",                    "SST"},
        {"Chlorophylle-a (Réunion)",          "CHL"}
    };

    /**
     * Opération à appliquer sur les données, ou <code>null</code>
     * pour n'en appliquer aucune.
     */
    private static final String OPERATION = null; // "GradientMagnitude";

    /**
     * Colonne de la table "environnement" dans lequel placer le résultat.
     * Ce nom de colonne est reliée à l'opération. Par exemple "sobel" pour
     * l'opération "GradientMagnitude".
     */
    private static final String COLUMN = "valeur";

    /**
     * Fonction à utiliser pour calculer les valeurs
     * à l'intérieur d'une région géographique.
     */
    private final Evaluator evaluator = null; // new GradientEvaluator();

    /**
     * Connection vers la base de données d'images.
     */
    private final ImageDataBase images;

    /**
     * Connection vers la base de données des pêches.
     */
    private final FisheryDataBase pêches;

    /**
     * Construit une connexion par défaut.  Cette connexion utilisera des
     * paramètres par défaut qui peuvent être préalablement configurés en
     * exécutant {@link SQLControler} à partir de la ligne de commande.
     *
     * @throws SQLException si la connexion a échouée.
     */
    public EnvironmentTableFiller() throws SQLException
    {
        images = new ImageDataBase();
        pêches = new FisheryDataBase();
    }

    /**
     * Lance le remplissage de la table "Environnement".
     *
     * @param  series Nom de la séries d'image à lire et nom des variables
     *         environnementales à mettre à jour. Le premier élément de ce
     *         tableau doit être le nom de la série. Les éléments suivants
     *         sont les noms des colonnes dans la table "Environnement".
     * @throws SQLException si un problème est survenu
     *         lors des accès à la base de données.
     */
    private void run(final String[] series) throws SQLException
    {
        final ImageTable         images = this.images.getImageTable(series[0]);
        final CatchTable         pêches = this.pêches.getCatchTable();
        final EnvironmentTable[] update = new EnvironmentTable[TEST_ONLY ? 0 : series.length-1];
        final List<CatchEntry>   catchs = pêches.getEntries();
        for (int i=0; i<update.length; i++)
        {
            update[i] = this.pêches.getEnvironmentTable(series[i+1], COLUMN);
        }
        images.setOperation(OPERATION);
        final CatchCoverage coverage = new CatchCoverage(images);
        images.close();

        computePointData(catchs, coverage, update);
        if (evaluator != null)
        {
            computeAreaData(catchs, coverage, update);
        }

        for (int i=0; i<update.length; i++)
        {
            update[i].close();
        }
        pêches.close();
    }

    /**
     * Calcule les données de la table "Environnement" sur une surface.
     * Cette méthode est appelée automatiquement par {@link #run}.
     *
     * @param  catchs Liste des captures à prendre en compte.
     * @param  coverage Couverture des données environnementales.
     * @param  update Tables des données environnementales à mettre à jour.
     * @throws SQLException si un problème est survenu
     *         lors des accès à la base de données.
     */
    private void computeAreaData(final List<CatchEntry> catchs, final CatchCoverage coverage, final EnvironmentTable[] update) throws SQLException
    {
        for (final Iterator<CatchEntry> it=catchs.iterator(); it.hasNext();)
        {
            final CatchEntry capture = it.next();
            final Shape area = coverage.getShape(capture);
            final Date  time = coverage.getTime(capture);
            final long timeAtDay0 = time.getTime();
            for (int t=0; t<DAYS_TO_EVALUATE.length; t++)
            {
                time.setTime(timeAtDay0 + DAY*DAYS_TO_EVALUATE[t]);
                final double[] values;
                try
                {
                    values = coverage.evaluate(capture, evaluator);
                }
                catch (PointOutsideCoverageException exception)
                {
                    warning(exception);
                    continue;
                }
                for (int c=0; c<update.length; c++)
                {
                    update[c].setPosition(EnvironmentTable.AREA);
                    update[c].set(capture, (float)values[c], time);
                }
            }
        }
    }

    /**
     * Calcule les données ponctuelles de la table "Environnement".
     * Cette méthode est appelée automatiquement par {@link #run}.
     *
     * @param  catchs Liste des captures à prendre en compte.
     * @param  coverage Couverture des données environnementales.
     * @param  update Tables des données environnementales à mettre à jour.
     * @throws SQLException si un problème est survenu
     *         lors des accès à la base de données.
     */
    private void computePointData(final List<CatchEntry> catchs, final CatchCoverage coverage, final EnvironmentTable[] update) throws SQLException
    {
        double[] values = null;
        final Point2D point = new Point2D.Double();
        for (final Iterator<CatchEntry> it=catchs.iterator(); it.hasNext();)
        {
            final CatchEntry capture = it.next();
            final Date time = coverage.getTime(capture);
            final long timeAtDay0 = time.getTime();
            final Line2D line = (Line2D) capture.getShape();
            for (int t=0; t<DAYS_TO_EVALUATE.length; t++)
            {
                time.setTime(timeAtDay0 + DAY*DAYS_TO_EVALUATE[t]);
                if (line!=null)
                {
                    final double x1 = line.getX1();
                    final double y1 = line.getY1();
                    final double x2 = line.getX2();
                    final double y2 = line.getY2();
                    for (int p=0; p<=100; p+=25)
                    {
                        final double x = (x2-x1)*(p/100.0)+x1;
                        final double y = (y2-y1)*(p/100.0)+y1;
                        point.setLocation(x,y);
                        try
                        {
                            values = coverage.evaluate(point, time, values);
                        }
                        catch (PointOutsideCoverageException exception)
                        {
                            warning(exception);
                            continue;
                        }
                        for (int c=0; c<update.length; c++)
                        {
                            update[c].setPosition(p);
                            update[c].set(capture, (float)values[c], time);
                        }
                    }
                }
                else // (line==null)
                {
                    try
                    {
                        values = coverage.evaluate(capture.getCoordinate(), time, values);
                    }
                    catch (PointOutsideCoverageException exception)
                    {
                        warning(exception);
                        continue;
                    }
                    // Si la capture était en un point seulement,
                    // 'EnvironmentTable' se chargera de vérifier
                    // si c'était le début où la fin de la ligne.
                    // On définit tout de même une position pour
                    // éviter qu'elle ne reste 'AREA'.
                    for (int c=0; c<update.length; c++)
                    {
                        update[c].setPosition(EnvironmentTable.CENTER);
                        update[c].set(capture, (float)values[c], time);
                    }
                }
            }
        }
    }

    /**
     * Indique qu'un point est en dehors de la région des données couvertes.
     * Cette méthode écrit un avertissement dans le journal, mais sans la
     * trace de l'exception (puisque cette erreur peut être normale).
     */
    private void warning(final PointOutsideCoverageException exception)
    {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setSourceClassName (Utilities.getShortClassName(this));
        record.setSourceMethodName("run");
        if (false) record.setThrown(exception);
        Logger.getLogger("fr.ird.sql.fishery").log(record);
    }

    /**
     * Ferme les connections avec les bases de données.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la fermeture des connections.
     */
    public void close() throws SQLException
    {
        pêches.close();
        images.close();
    }

    /**
     * Lance le remplissage de la table "Environnement"
     * à partir de la ligne de commande.
     *
     * @throws SQLException si un problème est survenu
     *         lors des accès aux bases de données.
     */
    public static void main(final String[] args) throws SQLException
    {
        final EnvironmentTableFiller worker = new EnvironmentTableFiller();
        for (int i=0; i<SERIES.length; i++)
        {
            worker.run(SERIES[i]);
        }
        worker.close();
    }
}
