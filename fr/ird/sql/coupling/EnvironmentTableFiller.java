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
package fr.ird.sql.coupling;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cv.PointOutsideCoverageException;

// Requ�tes SQL
import java.sql.SQLException;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.fishery.EnvironmentTable;

// G�om�trie
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
 * Classe ayant la charge de remplir la table  "Environnement"  de la base de donn�es "P�ches" �
 * partir des donn�es satellitaires. La table "Environnement" contient les valeurs de param�tres
 * environnementaux (temp�rature, chlorophylle-a, hauteur de l'eau, etc.) aux positions de p�ches.
 * Lorsque <code>EnvironmentTableFiller</code> trouve une donn�es environnementale � une position
 * de p�che, il met � jour la cellule correspondante de la table "Environnement". S'il ne trouve
 * pas de donn�es ou que la donn�e est manquante (par exemple sous un nuage), alors
 * <code>EnvironmentTableFiller</code> <strong>laisse inchang�e</strong> la cellule correspondante.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class EnvironmentTableFiller
{
    /**
     * <code>true</code> si on veut seulement tester cette classe sans �crire
     * dans la base de donn�es. Note: une connexion en lecture aux bases de
     * donn�es est tout de m�me n�cessaire.
     */
    private static final boolean TEST_ONLY = false;

    /**
     * La dur�e d'une journ�e, en nombre de millisecondes.
     */
    private static final long DAY = 24*60*60*1000L;

    /**
     * Jours o� extraire des donn�es, avant,
     * pendant et apr�s le jour de la p�che.
     */
    private static final int[] DAYS_TO_EVALUATE = {-15, -10, -5, 0, 5};

    /**
     * S�ries de donn�es � utiliser pour le remplissage des colonnes.
     * Pour chaque tableau <code>String[]</code>, le premier �l�ment
     * repr�sente la s�rie et les �l�ments suivants les colonnes de
     * la table "Environnement" � remplir pour chaque canal des images
     * de la s�rie.
     */
    private static final String[][] SERIES =
    {
        {"Bathym�trie de Sandwell (R�union)", "FLR"},
        {"Bathym�trie de Baudry (R�union)",   "FLR"},
        {"Pompage d'Ekman",                   "EKP"},
        {"SLA (R�union - NRT)",               "SLA", "U", "V"},
        {"SLA (R�union)",                     "SLA", "U", "V"},
        {"SLA (Monde - TP)",                  "SLA"},
        {"SLA (Monde - TP/ERS)",              "SLA"},
        {"SST (synth�se)",                    "SST"},
        {"Chlorophylle-a (R�union)",          "CHL"}
    };

    /**
     * Op�ration � appliquer sur les donn�es, ou <code>null</code>
     * pour n'en appliquer aucune.
     */
    private static final String OPERATION = null; // "GradientMagnitude";

    /**
     * Colonne de la table "environnement" dans lequel placer le r�sultat.
     * Ce nom de colonne est reli�e � l'op�ration. Par exemple "sobel" pour
     * l'op�ration "GradientMagnitude".
     */
    private static final String COLUMN = "valeur";

    /**
     * Fonction � utiliser pour calculer les valeurs
     * � l'int�rieur d'une r�gion g�ographique.
     */
    private final Evaluator evaluator = null; // new GradientEvaluator();

    /**
     * Connection vers la base de donn�es d'images.
     */
    private final ImageDataBase images;

    /**
     * Connection vers la base de donn�es des p�ches.
     */
    private final FisheryDataBase p�ches;

    /**
     * Construit une connexion par d�faut.  Cette connexion utilisera des
     * param�tres par d�faut qui peuvent �tre pr�alablement configur�s en
     * ex�cutant {@link SQLControler} � partir de la ligne de commande.
     *
     * @throws SQLException si la connexion a �chou�e.
     */
    public EnvironmentTableFiller() throws SQLException
    {
        images = new ImageDataBase();
        p�ches = new FisheryDataBase();
    }

    /**
     * Lance le remplissage de la table "Environnement".
     *
     * @param  series Nom de la s�ries d'image � lire et nom des variables
     *         environnementales � mettre � jour. Le premier �l�ment de ce
     *         tableau doit �tre le nom de la s�rie. Les �l�ments suivants
     *         sont les noms des colonnes dans la table "Environnement".
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s � la base de donn�es.
     */
    private void run(final String[] series) throws SQLException
    {
        final ImageTable         images = this.images.getImageTable(series[0]);
        final CatchTable         p�ches = this.p�ches.getCatchTable();
        final EnvironmentTable[] update = new EnvironmentTable[TEST_ONLY ? 0 : series.length-1];
        final List<CatchEntry>   catchs = p�ches.getEntries();
        for (int i=0; i<update.length; i++)
        {
            update[i] = this.p�ches.getEnvironmentTable(series[i+1], COLUMN);
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
        p�ches.close();
    }

    /**
     * Calcule les donn�es de la table "Environnement" sur une surface.
     * Cette m�thode est appel�e automatiquement par {@link #run}.
     *
     * @param  catchs Liste des captures � prendre en compte.
     * @param  coverage Couverture des donn�es environnementales.
     * @param  update Tables des donn�es environnementales � mettre � jour.
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s � la base de donn�es.
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
     * Calcule les donn�es ponctuelles de la table "Environnement".
     * Cette m�thode est appel�e automatiquement par {@link #run}.
     *
     * @param  catchs Liste des captures � prendre en compte.
     * @param  coverage Couverture des donn�es environnementales.
     * @param  update Tables des donn�es environnementales � mettre � jour.
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s � la base de donn�es.
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
                    // Si la capture �tait en un point seulement,
                    // 'EnvironmentTable' se chargera de v�rifier
                    // si c'�tait le d�but o� la fin de la ligne.
                    // On d�finit tout de m�me une position pour
                    // �viter qu'elle ne reste 'AREA'.
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
     * Indique qu'un point est en dehors de la r�gion des donn�es couvertes.
     * Cette m�thode �crit un avertissement dans le journal, mais sans la
     * trace de l'exception (puisque cette erreur peut �tre normale).
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
     * Ferme les connections avec les bases de donn�es.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la fermeture des connections.
     */
    public void close() throws SQLException
    {
        p�ches.close();
        images.close();
    }

    /**
     * Lance le remplissage de la table "Environnement"
     * � partir de la ligne de commande.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s aux bases de donn�es.
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
