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

// Géométrie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Ensembles
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Arrays;

// Divers
import java.util.Date;
import java.sql.SQLException;

// JAI
import javax.media.jai.ParameterList;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.resources.Utilities;
import org.geotools.cv.PointOutsideCoverageException;

// Requêtes SQL
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.fishery.EnvironmentTable;

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
public final class EnvironmentTableFiller {
    /**
     * <code>true</code> si on veut seulement tester cette classe sans écrire
     * dans la base de données. Note: une connexion en lecture aux bases de
     * données est tout de même nécessaire.
     */
    private static final boolean TEST_ONLY = false;

    /**
     * Séries de données à utiliser pour le remplissage des colonnes.
     * Pour chaque tableau <code>String[]</code>, le premier élément
     * représente la série et les éléments suivants les colonnes de
     * la table "Environnement" à remplir pour chaque canal des images
     * de la série.
     *
     * TODO: Cette liste devrait être construite à partir de la table "Parameters" de la
     *       base de données des pêches. Cette dernière contient toutes les informations
     *       nécessaire à cet effet.
     */
    private static final String[][] DEFAULT_SERIES = {
        {"SST (synthèse)",                    "SST"},
        {"Chlorophylle-a (Monde)",            "CHL"},
        {"Pompage d'Ekman",                   "EKP"},
        {"SLA (Réunion - NRT)",               "SLA", "U", "V"},
        {"SLA (Réunion)",                     "SLA", "U", "V"},
        {"SLA (Monde - TP)",                  "SLA"},
        {"SLA (Monde - TP/ERS)",              "SLA"},
        {"Bathymétrie de Sandwell (Réunion)", "FLR"},
        {"Bathymétrie de Baudry (Réunion)",   "FLR"}
    };

    /**
     * Jours où extraire des données, avant, pendant et après le jour de la pêche.
     * Il ne s'agit que d'une liste de jours à utiliser par défaut (si l'utilisateur
     * n'a pas modifié la sélection). Cette liste doit être classée en ordre croissant.
     */
    private static final int[] DAYS_TO_EVALUATE = {-30, -25, -20, -15, -10, -5, 0, 5};

    /**
     * Liste des opérations par défaut et des noms de colonnes dans lesquelles
     * mémoriser le résultat.
     *
     * TODO: Ces operations devraient apparaître dans la base de données.
     *       Une nouvelle table devra être créée.
     */
    private static final Operation[] DEFAULT_OPERATIONS = {
        new Operation(null,                  "valeur",   "Valeur interpolée"),
        new Operation("Interpolate",         "pixel",    "Valeur sans interpolation"),
        new Operation("GradientMagnitude",   "sobel",    "Magnitude du gradient")
    };

    /**
     * Liste des séries à traiter. Chaque séries est associée à une liste de noms de paramètres
     * dans la base de données. Le premier paramètre de la liste contiendra la valeur de la bande
     * 0; le second paramètre de la liste contiendra la valeur de la bande 1, etc.
     */
    private final Map<SeriesEntry,String[]> series = new LinkedHashMap<SeriesEntry,String[]>();

    /**
     * Date de départ et de fin d'échantillonage.
     * La valeur <code>null</code> signifie qu'aucune plage de temps n'a été fixée.
     */
    private Date startTime, endTime;

    /**
     * Coordonnées géographiques de la zone d'étude. La valeur
     * <code>null</code> signifie qu'aucune zone n'a été fixée.
     */
    private Rectangle2D geographicArea;

    /**
     * Opération à appliquer sur les données.
     */
    private Operation operation = DEFAULT_OPERATIONS[0];

    /**
     * Une liste optionnelle d'arguments.
     */
    private final Map<String,Object> arguments = new HashMap<String,Object>();

    /**
     * La colonne de destination dans la base de données.
     * Il s'agit habituellement de {@link Operation#column}.
     */
    private String column = operation.column;

    /**
     * Jours où extraire des données, avant, pendant et après le jour de la pêche.
     */
    private int[] daysToEvaluate = DAYS_TO_EVALUATE;

    /**
     * Connection vers la base de données d'images.
     */
    final ImageDataBase images;

    /**
     * Connection vers la base de données des pêches.
     */
    final FisheryDataBase pêches;

    /**
     * Construit une connexion par défaut.  Cette connexion utilisera des
     * paramètres par défaut qui peuvent être préalablement configurés en
     * exécutant {@link SQLControler} à partir de la ligne de commande.
     *
     * @throws SQLException si la connexion a échouée.
     */
    public EnvironmentTableFiller() throws SQLException {
        images = new ImageDataBase();
        pêches = new FisheryDataBase();
        final SeriesTable series = images.getSeriesTable();
        for (int i=0; i<DEFAULT_SERIES.length; i++) {
            final String[] param = DEFAULT_SERIES[i];
            final String[] list = new String[param.length-1];
            System.arraycopy(param, 1, list, 0, list.length);
            this.series.put(series.getSeries(param[0]), list);
        }
        series.close();
    }

    /**
     * Retourne la liste des séries à traiter. Une série peut être retirée de la liste des
     * séries à traiter en appellant {@link Set#remove}. On peut aussi ne retenir qu'un sous
     * ensemble de séries en appellant {@link Set#retainAll}.
     */
    public Set<SeriesEntry> getSeries() {
        return series.keySet();
    }

    /**
     * Retourne la liste des captures qui seront à traiter.
     *
     * @return Les captures pour lesquelles on calculera des paramètres environnementaux.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     */
    public CatchEntry[] getCatchs() throws SQLException {
        final CatchTable table = pêches.getCatchTable();
        if (startTime!=null && endTime!=null) {
            table.setTimeRange(startTime, endTime);
        }
        if (geographicArea != null) {
            table.setGeographicArea(geographicArea);
        }
        if (false) {
            table.setCatchRange(250, Double.POSITIVE_INFINITY);
        }
        final List<CatchEntry> list = table.getEntries();
        final CatchEntry[] catchs = list.toArray(new CatchEntry[list.size()]);
        table.close();
        return catchs;
    }

    /**
     * Définit les coordonnées géographiques de la zone d'étude.
     * La valeur <code>null</code> signifie qu'aucune restriction n'est imposée.
     */
    public void setGeographicArea(final Rectangle2D area) {
        if (area != null) {
            if (geographicArea == null) {
                geographicArea = new Rectangle2D.Double();
            }
            geographicArea.setRect(area);
        }
        else geographicArea = null;
    }

    /**
     * Définit les dates de début et de fin de la période d'intérêt.
     */
    public void setTimeRange(final Date start, final Date end) {
        startTime = (start!=null) ? new Date(start.getTime()) : null;
          endTime = (  end!=null) ? new Date(  end.getTime()) : null;
    }

    /**
     * Spécifie une opération à appliquer sur les images, ainsi que
     * la colonne dans laquelle mémoriser le résultat.
     *
     * @param operation L'opération à appliquer
     * @param column Le nom de la colonne de destination dans la base
     *        de données, ou <code>null</code> pour le nom par défaut.
     * @param arguments Une liste optionnelle d'arguments (peut être nulle).
     *        Seul les noms d'arguments attendu par l'opération seront pris
     *        en compte. Les autres arguments seront ignorés.
     */
    final void setOperation(final Operation operation, final String column,
                            final Map<String,Object> arguments)
    {
        this.operation = operation;
        this.column = (column!=null) ? column : operation.column;
        this.arguments.clear();
        if (arguments != null) {
            this.arguments.putAll(arguments);
        }
    }

    /**
     * Retourne une liste d'opération reconnues par {@link #setOperation}.
     */
    final Operation[] getAvailableOperations() {
        return (Operation[]) DEFAULT_OPERATIONS.clone();
    }

    /**
     * Retourne les jours où extraire des données,
     * avant, pendant et après le jour de la pêche.
     */
    final int[] getDaysToEvaluate() {
        return (int[]) daysToEvaluate.clone();
    }

    /**
     * Définie les jours où extraire des données,
     * avant, pendant et après le jour de la pêche.
     */
    final void setDaysToEvaluate(final int[] days) {
        daysToEvaluate = (int[]) days.clone();
    }

    /**
     * Lance le remplissage de la table "Environnement".
     *
     * @throws SQLException si un problème est survenu
     *         lors des accès à la base de données.
     */
    public void run() throws SQLException {
        final CatchEntry[]   catchs = getCatchs();
        final ImageTable imageTable = images.getImageTable();
        final ParameterList  params = imageTable.setOperation(operation.name);
        if (true) {
            final String[] names = params.getParameterListDescriptor().getParamNames();
            for (int i=0; i<names.length; i++) {
                final Object value = arguments.get(names[i]);
                if (value != null) {
                    params.setParameter(names[i], value);
                }
            }
        }

        // Calcule les données environnementales pour une série à la fois.
        for (final Iterator<Map.Entry<SeriesEntry,String[]>> it=series.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<SeriesEntry,String[]> series = it.next();
            imageTable.setSeries(series.getKey());
            final CatchCoverage    coverage = new CatchCoverage(imageTable);
            coverage.setInterpolationAllowed(true);
            final Task[]              tasks = Task.getTasks(catchs, coverage, daysToEvaluate);
            final String[]       parameters = series.getValue();
            final EnvironmentTable[] update = new EnvironmentTable[TEST_ONLY ? 0 : parameters.length];
            for (int i=0; i<update.length; i++) {
                update[i] = pêches.getEnvironmentTable(parameters[i], column);
            }
            operation.compute(tasks, coverage, update);
            for (int i=0; i<update.length; i++) {
                update[i].close();
            }
        }
        imageTable.close();
    }

    /**
     * Ferme les connections avec les bases de données.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la fermeture des connections.
     */
    public void close() throws SQLException {
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
    public static void main(final String[] args) throws SQLException {
        final EnvironmentTableFiller worker = new EnvironmentTableFiller();
        worker.run();
        worker.close();
    }
}
