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
package fr.ird.animat.seas;

// Divers J2SE
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
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
import org.geotools.resources.XDimension2D;
import fr.ird.util.XArray;

// Base de données
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment extends fr.ird.animat.impl.Environment {
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
     * Informations sur les données pour un paramètre. L'objet {@link Coverage3D} permet d'obtenir une
     * couverture à une date quelconque. L'objet {@link GridCoverage} contient les données obtenues à
     * une date précise et qui seront spécifiées à la méthode {@link Parameter#applyEvaluator}. L'objet
     * {@link Coverage} est la sortie de <code>applyEvaluator(...)</code>, mémorisée afin d'éviter que
     * cette dernière produit de nouveau calcul lorsque l'objet <code>GridCoverage</code> n'a pas changé
     * entre deux pas de temps.
     */
    private static final class Entry {
        /** La source de toutes les données pour ce paramètre. */
        public final Coverage3D coverage3D;

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
        public Entry(final Coverage3D coverage3D) {
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
     * Construit un environnement qui utilisera la configuration spécifiée.
     *
     * @param  config La configuration de la simulation.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     */
    public Environment(final Configuration config) throws SQLException {
        this(config, null);
    }

    /**
     * Construit un environnement qui utilisera la configuration spécifiée.
     *
     * @param  config La configuration de la simulation.
     * @param  database La base de données à utiliser, ou <code>null</code> pour utiliser
     *         une base de données par défaut.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     */
    protected Environment(final Configuration config, ImageDataBase database) throws SQLException {
        super(config.firstTimeStep);
        this.configuration = config;
        ImageTable  images = null;
        SeriesTable series = null;
        Range    timeRange = null;
        boolean    closedb = false;
        final Map<String,Coverage3D> coverageBySeries = new HashMap<String,Coverage3D>();
        for (final Iterator<fr.ird.animat.Parameter> it=config.parameters.iterator(); it.hasNext();) {
            final Parameter parameter = (Parameter) it.next();
            Coverage3D coverage3D = coverageBySeries.get(parameter.series);
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
                    if (database == null) {
                        database = new ImageDataBase();
                        closedb  = true;
                    }
                    series = database.getSeriesTable();
                    images = database.getImageTable(series.getSeries(parameter.series));
                    images.setPreferredResolution(new XDimension2D.Double(config.resolution,
                                                                          config.resolution));
                    images.setTimeRange(range);
                } else {
                    images.setSeries(series.getSeries(parameter.series));
                }
                /*
                 * Construit la nouvelle couverture 3D pour la série spécifiée  et met à jour
                 * la plage de temps de l'ensemble des données (pour détecter quand il faudra
                 * arrêter la simulation).
                 */
                coverage3D = new Coverage3D(images);
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
                Logger.getLogger("fr.ird.animat.seas").warning("Un paramètre est répété plusieurs fois");
            }
        }
        if (images != null) {
            images.close();
            series.close();
        }
        if (closedb) {
            database.close();
        }
        this.timeRange = timeRange;
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
    public Set<fr.ird.animat.Parameter> getParameters() {
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
    public Coverage getCoverage(final fr.ird.animat.impl.Parameter parameter) throws NoSuchElementException {
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
                     * Procède à la lecture des données et vérifie si les calculs ont déjà été
                     * effectuées sur les données obtenues.  Ca peut se produire si Coverage3D
                     * retourne le même GridCoverage pour deux pas de temps différents.
                     */
                    final Date time = getClock().getTime();
                    time.setTime(time.getTime() + param.timelag);
                    GridCoverage gridCoverage;
                    try {
                        gridCoverage = entry.coverage3D.getGridCoverage2D(time);
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
            for (final Iterator<Entry> it=coverages.values().iterator(); it.hasNext();) {
                final Entry entry = it.next();
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
     * Avance l'horloge d'un pas de temps.
     *
     * @return <code>true</code> si cette méthode a pu avancer au pas de temps suivant,
     *         ou <code>false</code> s'il n'y a plus de données disponibles pour les pas
     *         de temps suivants.
     */
    public boolean nextTimeStep() {
        synchronized (getTreeLock()) {
            for (final Iterator<Entry> it=coverages.values().iterator(); it.hasNext();) {
                it.next().isValid = false;
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
            super.dispose();
        }
    }
}
