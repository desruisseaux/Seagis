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
package fr.ird.database.sample;

// Géométrie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Ensembles
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Arrays;

// Divers
import java.util.Date;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.ct.TransformException;
import org.geotools.gp.GridCoverageProcessor; // Pour javadoc
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.Table;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.XArray;


/**
 * Remplit la table "Environnement" de la base de données des échantillons à partir des
 * données satellitaires. La table "Environnement" contient les valeurs de paramètres
 * environnementaux (température, chlorophylle-a, hauteur de l'eau, etc.) aux positions des
 * échantillons. Lorsque <code>EnvironmentTableFiller</code> trouve une données environnementale
 * à une position d'un échantillon, il met à jour la cellule correspondante de la table
 * "Environnement". S'il ne trouve pas de données ou que la donnée est manquante
 * (par exemple sous un nuage), alors <code>EnvironmentTableFiller</code> <strong>laisse
 * inchangée</strong> la cellule correspondante.
 * <br><br>
 * <strong>Choix des données à calculer</strong><br>
 * Par défaut, <code>EnvironmentTableFiller</code> calcule les données environnementales de toutes
 * les {@linkplain SeriesTable séries d'images} pour tous les {@linkplain SampleEntry échantillons}.
 *
 * Les séries à traiter peuvent être spécifiée en appellant
 * <code>{@link #getSeries}.retainAll(series)</code>.
 *
 * La plage de temps des échantillons à traiter peut être spécifiée avec
 * <code>{@link #getSampleTable}.setTimeRange(startTime, endTime)</code>,
 * et de même pour les coordonnées géographiques.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentTableFiller implements Table {
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
     * @task TODO: Cette liste devrait être construite à partir de la table "Parameters" de la
     *       base de données des échantillons. Cette dernière contient toutes les informations
     *       nécessaires à cet effet. Un code à cet effet (non testé) existe déjà dans le
     *       constructeur.
     */
    private static final String[][] DEFAULT_SERIES = {
        {"SST (Réunion - synthèse 5 jours)",  "SST"},
        {"SST (Monde - hebdomadaires)",       "SST-GAC"},
        {"CHL (Monde - hebdomadaires)",       "CHL"},
        {"EKP (Monde)",                       "EKP"},
        {"SLA (Réunion - NRT)",               "SLA", "U", "V"},
        {"SLA (Réunion)",                     "SLA", "U", "V"},
        {"SLA (Monde - TP)",                  "SLA"},
        {"SLA (Monde - TP/ERS)",              "SLA"},
        {"Bathymétrie de Sandwell (Réunion)", "FLR"},
        {"Bathymétrie de Baudry (Réunion)",   "FLR"}
    };

    /**
     * Liste des séries à traiter. Chaque séries est associée à une liste de noms de paramètres
     * dans la base de données. Le premier paramètre de la liste contiendra la valeur de la bande
     * 0; le second paramètre de la liste contiendra la valeur de la bande 1, etc.
     */
    private final Map<SeriesEntry,ParameterEntry[]> series = new LinkedHashMap<SeriesEntry,ParameterEntry[]>();

    /**
     * Liste des opérations applicables.
     */
    private final Set<OperationEntry> operations;

    /**
     * Jours où extraire des données, avant, pendant et après le jour de l'échantillon.
     */
    private final Set<RelativePositionEntry> positions;

    /**
     * Connection vers la base de données d'images.
     */
    private final CoverageDataBase coverages;

    /**
     * Connection vers la base de données des échantillons.
     */
    private final SampleDataBase samples;

    /**
     * La table des images. Ne sera construite que lorsqu'elle sera nécessaire.
     */
    private CoverageTable coverageTable;

    /**
     * La table des échantillons. Ne sera construite que lorsqu'elle sera nécessaire.
     */
    private SampleTable sampleTable;

    /**
     * La table des séries. Ne sera construite que lorsqu'elle sera nécessaire.
     */
    private SeriesTable seriesTable;

    /**
     * <code>true</code> si cet objet possède les connections vers les bases de données
     * d'images et d'échantillons. Dans ce cas, {@link #close} fermera ces connections.
     */
    private boolean canClose;

    /**
     * Indique si les interpolations spatio-temporelles sont permises.
     */
    private boolean interpolationAllowed = true;

    /**
     * Construit un objet utlisant une connexion par défaut.  Cette connexion utilisera
     * des paramètres par défaut qui peuvent être préalablement configurés en exécutant
     * {@link SQLControler} à partir de la ligne de commande.
     *
     * @throws SQLException si la connexion a échouée.
     */
    public EnvironmentTableFiller() throws SQLException {
        this(new fr.ird.database.coverage.sql.CoverageDataBase(),
             new fr.ird.database.sample.sql.SampleDataBase());
        canClose = true;
    }

    /**
     * Construit un objet utilisant les connections specifiées.
     *
     * @param  coverages Connexion vers la base de données d'images.
     * @param  samples Connexion vers la base de données des d'échantillons.
     * @throws SQLException si la connexion a échouée.
     */
    public EnvironmentTableFiller(final CoverageDataBase coverages,
                                  final SampleDataBase   samples)
        throws SQLException
    {
        this.coverages   = coverages;
        this.samples     = samples;
        this.seriesTable = coverages.getSeriesTable();
        if (DEFAULT_SERIES == null) {
            /*
             * Obtient les séries à partir de la base de données.
             * NOTE: Cette partie n'a pas encore été testée.
             */
            for (final ParameterEntry parameter : samples.getParameters(seriesTable)) {
                int index = 0;
                SeriesEntry entry;
                while ((entry=parameter.getSeries(index++)) != null) {
                    final int band = parameter.getBand();
                    ParameterEntry[] list = series.get(entry);
                    if (list == null) {
                        list = new ParameterEntry[band+1];
                    } else if (list.length <= band) {
                        list = XArray.resize(list, band+1);
                    }
                    list[band] = parameter;
                    series.put(entry, list);
                }
            }
        } else {
            /*
             * Obtient les séries à partir de tableau des séries par défaut (codé en dur).
             */
            final Map<String,ParameterEntry> parameters = new HashMap<String,ParameterEntry>();
            for (final ParameterEntry parameter : samples.getParameters(seriesTable)) {
                if (parameters.put(parameter.getName(), parameter) != null) {
                    throw new IllegalRecordException("Parameters", parameter.getName());
                }
            }
            for (int i=0; i<DEFAULT_SERIES.length; i++) {
                final String[] param = DEFAULT_SERIES[i];
                final ParameterEntry[] list = new ParameterEntry[param.length-1];
                for (int j=0; j<list.length; j++) {
                    list[j] = parameters.get(param[j+1]);
                }
                series.put(seriesTable.getEntry(param[0]), list);
            }
        }
        positions  = samples.getRelativePositions();
        operations = samples.getOperations();
    }

    /**
     * Retourne l'ensemble des séries à traiter. Une série peut être retirée de l'ensemble
     * en appellant {@link Set#remove}. On peut aussi ne retenir qu'un sous ensemble de
     * séries en appellant {@link Set#retainAll}.
     */
    public final Set<SeriesEntry> getSeries() {
        return series.keySet();
    }

    /**
     * Retourne les opérations qui seront à appliquer sur les paramètres environnementaux.
     * Cet ensemble contient initialement toutes les opérations disponibles. L'utilisateur
     * devrait appeller {@link Set#retainAll} avec en argument les seuls opérations qu'il
     * souhaite conserver.
     */
    public Set<OperationEntry> getOperations() {
        return operations;
    }

    /**
     * Retourne les positions relatives auxquelles extraires les paramètres environnementaux.
     * Cet ensemble contient initialement toutes les positions disponibles. L'utilisateur
     * devrait appeller {@link Set#retainAll} avec en argument les seuls positions qu'il
     * souhaite conserver.
     */
    public Set<RelativePositionEntry> getRelativePositions() {
        return positions;
    }

    /**
     * Retourne la table des images qui sera utilisée pour obtenir les données environnementales.
     * Cette table peut être configurée en appelant ses méthodes telles que
     * {@link CoverageTable#setOperation}, ce qui affectera toutes les données qui seront
     * calculées lors du prochain appel de {@link #run}.
     *
     * <strong>Ne fermez pas cette table</strong>; sa fermeture sera plutôt prise en charge par
     * la méthode {@link #close} de cet objet <code>EnvironmentTableFiller</code>.
     *
     * @return La table des images environnementales.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     *
     * @task TODO: Envelopper dans un proxy qui ignore les appels de setSeries(...) et close().
     */
    public synchronized CoverageTable getCoverageTable() throws SQLException {
        if (coverageTable == null) {
            coverageTable = coverages.getCoverageTable();
        }
        return coverageTable;
    }

    /**
     * Retourne la table qui sera utilisée pour obtenir les échantillons. Cette table peut être
     * configurée en appelant ses méthodes telles que {@link SampleTable#setGeographicArea} et
     * {@link SampleTable#setTimeRange}, ce qui réduira le nombre d'échantillons qui seront traités
     * lors du prochain appel de {@link #run}.
     *
     * <strong>Ne fermez pas cette table</strong>; sa fermeture sera plutôt prise en charge par
     * la méthode {@link #close} de cet objet <code>EnvironmentTableFiller</code>.
     *
     * @return La table des échantillons.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     *
     * @task TODO: Envelopper dans un proxy qui ignore les appels de close().
     */
    public synchronized SampleTable getSampleTable() throws SQLException {
        if (sampleTable == null) {
            sampleTable = samples.getSampleTable();
        }
        return sampleTable;
    }

    /**
     * Indique si cet objet est autorisé à interpoller dans l'espace et dans le temps.
     * La valeur par défaut est <code>true</code>.
     */
    public boolean isInterpolationAllowed() {
        return interpolationAllowed;
    }

    /**
     * Spécifie si cet objet est autorisé à interpoller dans l'espace et dans le temps.
     * La valeur par défaut est <code>true</code>.
     */
    public synchronized void setInterpolationAllowed(final boolean flag) {
        this.interpolationAllowed = flag;
    }

    /**
     * Lance le remplissage de la table "Environnement".
     *
     * @throws SQLException si un problème est survenu lors des accès à la base de données.
     * @throws TransformException si une transformation de coordonnées était nécessaire et
     *         a échouée.
     */
    public void run() throws SQLException, TransformException {
        SampleDataBase.LOGGER.info("Prépare le remplissage de la table d'environnement.");
        final Collection<SampleEntry>          sampleEntries = getSampleTable().getEntries();
        final CoverageTable                    coverageTable = getCoverageTable();
        final Set<? extends OperationEntry>       operations = getOperations();
        final Set<? extends RelativePositionEntry> positions = getRelativePositions();
        for (final OperationEntry operation : operations) {
            /*
             * Pour chaque opérations, configure la table d'images en lui appliquant l'opération
             * souhaitée, puis copie les arguments de l'opération dans son objet 'ParameterList'.
             */
            final ParameterList operationParameters;
            operationParameters = coverageTable.setOperation(operation.getProcessorOperation());
            if (operationParameters != null) {
                final String[] names = operationParameters.getParameterListDescriptor().getParamNames();
                for (int i=0; i<names.length; i++) {
                    final Object value = operation.getParameter(names[i]);
                    if (value != null) {
                        operationParameters.setParameter(names[i], value);
                    }
                }
            }
            /*
             * Pour chaque séries, construit un objet EnvironmentTable qui contiendra toutes les
             * bandes (par exemple "SLA", "U" et "V").   On déclarera aussi toutes les positions
             * relatives, mais ces positions seront transmises de manière explicites plus loin.
             */
            for (final Map.Entry<SeriesEntry,ParameterEntry[]> series : this.series.entrySet()) {
                final SeriesCoverage3D coverage;
                final SamplePosition[]    tasks;

                coverageTable.setSeries(series.getKey());
                coverage = new SeriesCoverage3D(coverageTable);
                coverage.setInterpolationAllowed(interpolationAllowed);
                tasks = SamplePosition.getInstances(sampleEntries, positions, coverage);
                info(ResourceKeys.POSITIONS_TO_EVALUATE_$1, new Integer(tasks.length));
                final EnvironmentTable table = samples.getEnvironmentTable(seriesTable);
                final ParameterEntry[] parameters = series.getValue();
                for (final RelativePositionEntry position : positions) {
                    for (int i=0; i<parameters.length; i++) {
                        table.addParameter(parameters[i], operation, position, false);
                    }
                }
                /*
                 * Procède maintenant à l'évaluation de toutes les valeurs et leur
                 * écriture dans la base de données.
                 */
                double[] values = null;
                for (int i=0; i<tasks.length; i++) {
                    final SampleEntry           sample   = tasks[i].sample;
                    final RelativePositionEntry position = tasks[i].position;
                    try {
                        values = coverage.evaluate(sample, position, values);
                        coverage.lastWarning = null;
                    } catch (PointOutsideCoverageException exception) {
                        warning(coverage, exception);
                        continue;
                    }
                    table.set(sample, position, values);
                }
                table.close();
            }
        }
        SampleDataBase.LOGGER.info("Remplissage de la table d'environnement terminé.");
    }

    /**
     * Ecrit un message dans le journal avec le niveau "info".
     */
    private static void info(final int key, final Object arg) {
        LogRecord record = Resources.getResources(null).getLogRecord(Level.INFO, key, arg);
        record.setSourceClassName("EnvironmentTableFiller");
        record.setSourceMethodName("run");
        SampleDataBase.LOGGER.log(record);
    }

    /**
     * Indique qu'un point est en dehors de la région des données couvertes.
     * Cette méthode écrit un avertissement dans le journal, à la condition
     * qu'il n'y en avait pas déjà un.
     */
    private static void warning(final SeriesCoverage3D source, final PointOutsideCoverageException exception) {
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
     * Ferme les connections avec les bases de données.
     *
     * @throws SQLException si un problème est survenu lors de la fermeture des connections.
     */
    public void close() throws SQLException {
        if (sampleTable != null) {
            sampleTable.close();
            sampleTable = null;
        }
        if (seriesTable != null) {
            seriesTable.close();
            seriesTable = null;
        }
        if (coverageTable != null) {
            coverageTable.close();
            coverageTable = null;
        }
        if (canClose) {
            samples.close();
            coverages.close();
            canClose = false;
        }
    }
}
