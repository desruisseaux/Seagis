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
package fr.ird.animat.server.tuna;

// Divers J2SE
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.sql.SQLException;

// JAI
import javax.media.jai.util.Range;

// OpenGIS et Geotools
import org.opengis.cv.CV_Coverage;
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.ct.TransformException;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.Utilities;

// Base de données et animats
import fr.ird.database.DataBase;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.database.coverage.SeriesCoverage3D;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.sample.SampleEntry;
import fr.ird.database.sample.SampleTable;
import fr.ird.database.sample.SampleDataBase;
import fr.ird.animat.server.SampleSource;
import fr.ird.resources.XArray;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment extends fr.ird.animat.server.Environment implements SampleSource, Runnable {
    /**
     * La résolution temporelle des données de pêches. En général, on n'a que la date de la
     * pêche et peu ou aucune indication sur l'heure.  On considèrera donc que la précision
     * est de 24 heures.
     */
    private static final int TIME_RESOLUTION = 24*60*60*1000;

    /**
     * La configuration de la simulation.
     */
    final Configuration configuration;

    /**
     * Données à utiliser pour chaque paramètres. Ces données seront classées dans le
     * même ordre que les ont été déclarés dans {@link Configuration}.
     */
    private final Map<Parameter,Entry> coverages = new TreeMap<Parameter,Entry>();

    /**
     * Informations sur les données pour un paramètre. L'objet {@link SeriesCoverage3D} permet
     * d'obtenir une couverture à une date quelconque. L'objet {@link GridCoverage} contient
     * les données obtenues à une date précise et qui seront spécifiées à la méthode
     * {@link Parameter#applyEvaluator}. L'objet {@link Coverage} est la sortie de
     * <code>applyEvaluator(...)</code>, mémorisée afin d'éviter que cette dernière produit de
     * nouveau calcul lorsque l'objet <code>GridCoverage</code> n'a pas changé entre deux pas
     * de temps.
     */
    private static final class Entry {
        /** La source de toutes les données pour ce paramètre. */
        public final SeriesCoverage3D coverage3D;

        /** L'objet {@link GridCoverage} donné par {@link #coverage3D} à la date courante. */
        public GridCoverage gridCoverage;

        /** L'objet {@link Coverage} donné par <code>applyEvaluator({@link #gridCoverage})</code>. */
        public Coverage coverage;

        /** <code>false</code> si la validité de {@link #coverage} a besoin d'être vérifiée. */
        public boolean isValid;

        /**
         * Construit une nouvelle entrée pour les données spécifiées.
         * @param La source de toute les données pour un paramètre.
         */
        public Entry(final SeriesCoverage3D coverage3D) {
            this.coverage3D = coverage3D;
        }
    }

    /**
     * Les objets {@link Species} pour chaque espèce.
     */
    private final Map<fr.ird.animat.Species,Species> species = new HashMap<fr.ird.animat.Species,Species>();

    /**
     * Place de temps couvrant l'ensemble des données. La date de fin notamment est utilisée
     * pour déterminer à quel moment la simulation est terminée.
     */
    private final Range timeRange;

    /**
     * Table des échantillons. Utilisé lors de la construction des populations.
     */
    private final SampleTable samples;

    /**
     * Bases de données à fermer lors de la destruction de l'environnement.
     */
    private final DataBase[] toClose;

    /**
     * Construit un environnement qui utilisera la configuration spécifiée.
     *
     * @param  config La configuration de la simulation.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     * @throws TransformException si une transformation de coordonnées était nécessaire et a échouée.
     */
    public Environment(final Configuration config) throws SQLException, TransformException {
        this(config, null, null);
    }

    /**
     * Construit un environnement qui utilisera la configuration spécifiée.
     *
     * @param  config La configuration de la simulation.
     * @param  coverageDB La base de données d'images à utiliser,
     *         ou <code>null</code> pour utiliser une base de données par défaut.
     * @param  samplesDB La base de données de pêches à utiliser,
     *         ou <code>null</code> pour utiliser une base de données par défaut.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     * @throws TransformException si une transformation de coordonnées était nécessaire et a échouée.
     */
    protected Environment(final Configuration config,
                          CoverageDataBase coverageDB,
                          SampleDataBase   samplesDB)
        throws SQLException, TransformException
    {
        super(config.firstTimeStep);
        this.configuration = config;
        CoverageTable images = null;
        SeriesTable   series = null;
        Range      timeRange = null;
        boolean      closedb = false;
        final Map<String,SeriesCoverage3D> coverageBySeries = new HashMap<String,SeriesCoverage3D>();
        for (final Parameter parameter : config.parameters) {
            SeriesCoverage3D coverage3D = coverageBySeries.get(parameter.series);
            if (coverage3D == null) {
                /*
                 * Une nouvelle série a été demandée. Configure la table 'images' de façon
                 * à ce qu'elle pointe vers la nouvelle séries à prendre en compte.
                 */
                if (images == null) {
                    Range range = config.firstTimeStep.getTimeRange();
                    Date startTime = (Date) range.getMinValue();
                    startTime = new Date(startTime.getTime() + configuration.getTimeLag());
                    range = new Range(Date.class, startTime, range.isMinIncluded(), new Date(), true);
                    if (coverageDB == null) {
                        coverageDB = new fr.ird.database.coverage.sql.CoverageDataBase();
                        closedb    = true;
                    }
                    series = coverageDB.getSeriesTable();
                    images = coverageDB.getCoverageTable(series.getEntry(parameter.series));
                    images.setPreferredResolution(new XDimension2D.Double(config.resolution,
                                                                          config.resolution));
                    images.setTimeRange(range);
                } else {
                    images.setSeries(series.getEntry(parameter.series));
                }
                /*
                 * Construit la nouvelle couverture 3D pour la série spécifiée  et met à jour
                 * la plage de temps de l'ensemble des données (pour détecter quand il faudra
                 * arrêter la simulation).
                 */
                coverage3D = new SeriesCoverage3D(images);
                if (coverageBySeries.put(parameter.series, coverage3D) != null) {
                    throw new AssertionError();
                }
                final Range expand = coverage3D.getTimeRange();
                if (timeRange == null) {
                    timeRange = expand;
                } else {
                    timeRange = timeRange.union(expand);
                }
            }
            if (coverages.put(parameter, new Entry(coverage3D)) != null) {
                // Should not happen since 'config.parameters' is a Set.
                Logger.getLogger("fr.ird.animat.server").warning("Un paramètre est répété plusieurs fois");
            }
        }
        if (images != null) {
            images.close();
            series.close();
        }
        if (closedb) {
            coverageDB.close();
        }
        this.timeRange = timeRange;
        /*
         * Construction de la table des échantillons.
         */
        if (samplesDB == null) {
            samplesDB = new fr.ird.database.sample.sql.SampleDataBase();
            toClose   = new DataBase[] {samplesDB};
        } else {
            toClose = new DataBase[0];
        }
        final Collection<String> species = configuration.species;
        samples = samplesDB.getSampleTable((String[])species.toArray(new String[species.size()]));
        samples.setTimeRange(timeRange);
    }

    /**
     * Retourne un objet {@link Species} pour l'espèce spécifiée.
     *
     * @param  parent L'espèce dont on veut copier les propriétés (noms, couleur).
     * @throws RemoteException si des méthodes devaient être appelée sur une machine distance
     *         et que ces appels ont échoués.
     */
    final Species wrap(final fr.ird.animat.Species sp) throws RemoteException {
        assert Thread.holdsLock(getTreeLock());
        Species candidate = species.get(sp);
        if (candidate == null) {
            if (sp instanceof Species) {
                candidate = (Species) sp;
            } else {
                candidate = new Species(sp, configuration);
            }
            species.put(sp, candidate);
        }
        return candidate;
    }

    /**
     * Ajoute une nouvelle population dans cet environnement. La population contiendra un
     * thon pour chacune des positions de pêche du pas de temps courant.  Le constructeur
     * de {@link Population} se charge d'ajouter automatiquement d'obtenir ces positions
     * et d'ajouter la nouvelle population à cet environnement.
     *
     * @return La population créée.
     * @throws RemoteException si l'exportation de la nouvelle population a échoué.
     */
    public Population newPopulation() throws RemoteException {
        synchronized (getTreeLock()) {
            return new Population(this);
        }
    }

    /**
     * Retourne l'ensemble des paramètres compris dans cet environnement. Cet ensemble
     * dépendra de la {@linkplain Configuration configuration} donnée au constructeur.
     */
    public Set<+Parameter> getParameters() {
        return configuration.parameters;
    }

    /**
     * Retourne toute la {@linkplain Coverage couverture spatiale des données} à la
     * {@linkplain Clock#getTime date courante} pour un paramètre spécifié.
     *
     * @param  parameter Le paramètre désiré.
     * @return La couverture spatiale des données pour le paramètre spécifié, ou <code>null</code>
     *         si aucune donnée n'est disponible à la date courante. Ce dernier cas peut se produire
     *         s'il y a des trous dans la couverture temporelle des données.
     *
     * @throws NoSuchElementException si le paramètre spécifié n'existe pas dans cet environnement.
     */
    public Coverage getCoverage(final fr.ird.animat.server.Parameter parameter) throws NoSuchElementException {
        synchronized (getTreeLock()) {
            final Parameter param = Parameter.getImplementation(parameter);
            if (param != null) {
                final Entry entry = coverages.get(param);
                if (entry != null) {
                    /*
                     * Vérifie si la couverture a déjà été calculée pour le paramètre spécifié.
                     * Ca nous évite de faire une nouvelle lecture et de nouveaux calculs.
                     */
                    if (entry.isValid) {
                        return entry.coverage;
                    }
                    /*
                     * Détermine la date de l'image a demandée. Etant donnée que la précision des
                     * données de pêches est généralement inférieure à 12 heures, on ne va pas
                     * demander des images à cette heure précise; on laissera plutôt le système
                     * choisir une date qui évitera des interpollations autant que possible. Dans
                     * tous les cas, on s'assure que la date "snappée" ne s'écarte pas de la date
                     * de la pêche de plus de TIME_RESOLUTION millisecondes.
                     */
                    final Date date = getClock().getTime();
                    final long time = date.getTime() + param.timelag;
                    date.setTime(time);
                    entry.coverage3D.snap(null, date); // Avoid temporal interpolation.
                    final long delta = ((time-date.getTime()) / TIME_RESOLUTION) * TIME_RESOLUTION;
                    date.setTime(date.getTime() + delta);
                    assert Math.abs(date.getTime() - time) <= TIME_RESOLUTION : delta;
                    /*
                     * Procède à la lecture des données et vérifie si les calculs ont déjà été
                     * effectuées sur les données obtenues. Ca peut se produire si SeriesCoverage3D
                     * retourne le même GridCoverage pour deux pas de temps différents.
                     */
                    GridCoverage gridCoverage;
                    try {
                        gridCoverage = entry.coverage3D.getGridCoverage2D(date);
                    } catch (PointOutsideCoverageException exception) {
                        gridCoverage = null;
                    }
                    if (gridCoverage != entry.gridCoverage) {
                        entry.gridCoverage = gridCoverage;
                        entry.coverage = param.applyEvaluator(gridCoverage);
                    }
                    entry.isValid = true;
                    return entry.coverage;
                }
            }
            return super.getCoverage(parameter);
        }
    }

    /**
     * Retourne les noms de toutes les {@linkplain CV_Coverage couvertures spatiales des données}
     * qui ont été utilisées pour le pas de temps de la {@linkplain Clock#getTime date courante}.
     *
     * @return Les noms couvertures spatiales utilisées pour le pas de temps courant.
     */
    public String[] getCoverageNames() {
        synchronized (getTreeLock()) {
            int count = 0;
            final String[] names = new String[coverages.size()];
            for (final Entry entry : coverages.values()) {
                if (entry.isValid) {
                    final String name;
                    if (entry.coverage != null) {
                        name = entry.coverage.getName(null);
                    } else {
                        name = "(aucune données)";
                    }
                    names[count++] = name;
                }
            }
            return XArray.resize(names, count);
        }
    }

    /**
     * Retourne toutes les espèces se trouvant dans la table.
     *
     * @return Les espèces se trouvant dans la table.
     * @throws SQLException si une erreur est survenue lors de l'interrogation de la base de données.
     */
    public Set<fr.ird.animat.Species> getSpecies() throws SQLException {
        return samples.getSpecies();
    }

    /**
     * Retourne l'ensemble des échantillons pour le pas de temps courant.
     *
     * @return Les échantillons pour le pas de temps courant.
     * @throws SQLException si une erreur est survenue lors de l'interrogation de la base de données.
     */
    public Collection<SampleEntry> getSamples() throws SQLException {
        Range timeRange = getClock().getTimeRange();
        Date  startTime = (Date) timeRange.getMinValue();
        Date    endTime = (Date) timeRange.getMaxValue();
        if (endTime.getTime() - startTime.getTime() < TIME_RESOLUTION) {
            endTime   = new Date(startTime.getTime() + TIME_RESOLUTION);
            timeRange = new Range(Date.class, startTime,  timeRange.isMinIncluded(),
                                                endTime, !timeRange.isMinIncluded());
        }
        synchronized (samples) {
            samples.setTimeRange(timeRange);
            return samples.getEntries();
        }
    }

    /**
     * Avance l'horloge d'un pas de temps.
     *
     * @return <code>true</code> si cette méthode a pu avancer au pas de temps suivant,
     *         ou <code>false</code> s'il n'y a plus de données disponibles pour les pas
     *         de temps suivants.
     */
    public boolean nextTimeStep() {
        synchronized (getTreeLock()) {
            for (final Entry entry : coverages.values()) {
                entry.isValid = false;
            }
            final Comparable min = getClock().getTimeRange().getMinValue();
            final Comparable max = timeRange.getMaxValue();
            if (min.compareTo(max) >= 0) {
                return false;
            }
            return super.nextTimeStep();
        }
    }

    /**
     * Libère les ressources utilisées par cet environnement. Toutes
     * les populations contenues dans cet environnement seront détruites,
     * et les éventuelles connections avec des bases de données seront
     * fermées.
     */
    public void dispose() {
        synchronized (getTreeLock()) {
            coverages.clear();
            try {
                samples.close();
                for (int i=toClose.length; --i>=0;) {
                    toClose[i].close();
                }
            } catch (SQLException exception) {
                Utilities.unexpectedException("fr.ird.animat.server", "Environment", "dispose", exception);
            }
            super.dispose();
        }
    }

    /**
     * Libère les ressources utilisées par cet environnement. Cette méthode ne fait qu'appeler
     * {@link #dispose}. Elle est définie afin de permettre à {@link Simulation} de construire
     * un "shutdown hook".
     */
    public void run() {
        dispose();
    }
}
