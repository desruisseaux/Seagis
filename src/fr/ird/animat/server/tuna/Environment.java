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

// Base de donn�es et animats
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
 * Repr�sentation de l'environnement dans lequel �volueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment extends fr.ird.animat.server.Environment implements SampleSource, Runnable {
    /**
     * La r�solution temporelle des donn�es de p�ches. En g�n�ral, on n'a que la date de la
     * p�che et peu ou aucune indication sur l'heure.  On consid�rera donc que la pr�cision
     * est de 24 heures.
     */
    private static final int TIME_RESOLUTION = 24*60*60*1000;

    /**
     * La configuration de la simulation.
     */
    final Configuration configuration;

    /**
     * Donn�es � utiliser pour chaque param�tres. Ces donn�es seront class�es dans le
     * m�me ordre que les ont �t� d�clar�s dans {@link Configuration}.
     */
    private final Map<Parameter,Entry> coverages = new TreeMap<Parameter,Entry>();

    /**
     * Informations sur les donn�es pour un param�tre. L'objet {@link SeriesCoverage3D} permet
     * d'obtenir une couverture � une date quelconque. L'objet {@link GridCoverage} contient
     * les donn�es obtenues � une date pr�cise et qui seront sp�cifi�es � la m�thode
     * {@link Parameter#applyEvaluator}. L'objet {@link Coverage} est la sortie de
     * <code>applyEvaluator(...)</code>, m�moris�e afin d'�viter que cette derni�re produit de
     * nouveau calcul lorsque l'objet <code>GridCoverage</code> n'a pas chang� entre deux pas
     * de temps.
     */
    private static final class Entry {
        /** La source de toutes les donn�es pour ce param�tre. */
        public final SeriesCoverage3D coverage3D;

        /** L'objet {@link GridCoverage} donn� par {@link #coverage3D} � la date courante. */
        public GridCoverage gridCoverage;

        /** L'objet {@link Coverage} donn� par <code>applyEvaluator({@link #gridCoverage})</code>. */
        public Coverage coverage;

        /** <code>false</code> si la validit� de {@link #coverage} a besoin d'�tre v�rifi�e. */
        public boolean isValid;

        /**
         * Construit une nouvelle entr�e pour les donn�es sp�cifi�es.
         * @param La source de toute les donn�es pour un param�tre.
         */
        public Entry(final SeriesCoverage3D coverage3D) {
            this.coverage3D = coverage3D;
        }
    }

    /**
     * Les objets {@link Species} pour chaque esp�ce.
     */
    private final Map<fr.ird.animat.Species,Species> species = new HashMap<fr.ird.animat.Species,Species>();

    /**
     * Place de temps couvrant l'ensemble des donn�es. La date de fin notamment est utilis�e
     * pour d�terminer � quel moment la simulation est termin�e.
     */
    private final Range timeRange;

    /**
     * Table des �chantillons. Utilis� lors de la construction des populations.
     */
    private final SampleTable samples;

    /**
     * Bases de donn�es � fermer lors de la destruction de l'environnement.
     */
    private final DataBase[] toClose;

    /**
     * Construit un environnement qui utilisera la configuration sp�cifi�e.
     *
     * @param  config La configuration de la simulation.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la base de donn�es.
     * @throws TransformException si une transformation de coordonn�es �tait n�cessaire et a �chou�e.
     */
    public Environment(final Configuration config) throws SQLException, TransformException {
        this(config, null, null);
    }

    /**
     * Construit un environnement qui utilisera la configuration sp�cifi�e.
     *
     * @param  config La configuration de la simulation.
     * @param  coverageDB La base de donn�es d'images � utiliser,
     *         ou <code>null</code> pour utiliser une base de donn�es par d�faut.
     * @param  samplesDB La base de donn�es de p�ches � utiliser,
     *         ou <code>null</code> pour utiliser une base de donn�es par d�faut.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la base de donn�es.
     * @throws TransformException si une transformation de coordonn�es �tait n�cessaire et a �chou�e.
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
                 * Une nouvelle s�rie a �t� demand�e. Configure la table 'images' de fa�on
                 * � ce qu'elle pointe vers la nouvelle s�ries � prendre en compte.
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
                 * Construit la nouvelle couverture 3D pour la s�rie sp�cifi�e  et met � jour
                 * la plage de temps de l'ensemble des donn�es (pour d�tecter quand il faudra
                 * arr�ter la simulation).
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
                Logger.getLogger("fr.ird.animat.server").warning("Un param�tre est r�p�t� plusieurs fois");
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
         * Construction de la table des �chantillons.
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
     * Retourne un objet {@link Species} pour l'esp�ce sp�cifi�e.
     *
     * @param  parent L'esp�ce dont on veut copier les propri�t�s (noms, couleur).
     * @throws RemoteException si des m�thodes devaient �tre appel�e sur une machine distance
     *         et que ces appels ont �chou�s.
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
     * thon pour chacune des positions de p�che du pas de temps courant.  Le constructeur
     * de {@link Population} se charge d'ajouter automatiquement d'obtenir ces positions
     * et d'ajouter la nouvelle population � cet environnement.
     *
     * @return La population cr��e.
     * @throws RemoteException si l'exportation de la nouvelle population a �chou�.
     */
    public Population newPopulation() throws RemoteException {
        synchronized (getTreeLock()) {
            return new Population(this);
        }
    }

    /**
     * Retourne l'ensemble des param�tres compris dans cet environnement. Cet ensemble
     * d�pendra de la {@linkplain Configuration configuration} donn�e au constructeur.
     */
    public Set<+Parameter> getParameters() {
        return configuration.parameters;
    }

    /**
     * Retourne toute la {@linkplain Coverage couverture spatiale des donn�es} � la
     * {@linkplain Clock#getTime date courante} pour un param�tre sp�cifi�.
     *
     * @param  parameter Le param�tre d�sir�.
     * @return La couverture spatiale des donn�es pour le param�tre sp�cifi�, ou <code>null</code>
     *         si aucune donn�e n'est disponible � la date courante. Ce dernier cas peut se produire
     *         s'il y a des trous dans la couverture temporelle des donn�es.
     *
     * @throws NoSuchElementException si le param�tre sp�cifi� n'existe pas dans cet environnement.
     */
    public Coverage getCoverage(final fr.ird.animat.server.Parameter parameter) throws NoSuchElementException {
        synchronized (getTreeLock()) {
            final Parameter param = Parameter.getImplementation(parameter);
            if (param != null) {
                final Entry entry = coverages.get(param);
                if (entry != null) {
                    /*
                     * V�rifie si la couverture a d�j� �t� calcul�e pour le param�tre sp�cifi�.
                     * Ca nous �vite de faire une nouvelle lecture et de nouveaux calculs.
                     */
                    if (entry.isValid) {
                        return entry.coverage;
                    }
                    /*
                     * D�termine la date de l'image a demand�e. Etant donn�e que la pr�cision des
                     * donn�es de p�ches est g�n�ralement inf�rieure � 12 heures, on ne va pas
                     * demander des images � cette heure pr�cise; on laissera plut�t le syst�me
                     * choisir une date qui �vitera des interpollations autant que possible. Dans
                     * tous les cas, on s'assure que la date "snapp�e" ne s'�carte pas de la date
                     * de la p�che de plus de TIME_RESOLUTION millisecondes.
                     */
                    final Date date = getClock().getTime();
                    final long time = date.getTime() + param.timelag;
                    date.setTime(time);
                    entry.coverage3D.snap(null, date); // Avoid temporal interpolation.
                    final long delta = ((time-date.getTime()) / TIME_RESOLUTION) * TIME_RESOLUTION;
                    date.setTime(date.getTime() + delta);
                    assert Math.abs(date.getTime() - time) <= TIME_RESOLUTION : delta;
                    /*
                     * Proc�de � la lecture des donn�es et v�rifie si les calculs ont d�j� �t�
                     * effectu�es sur les donn�es obtenues. Ca peut se produire si SeriesCoverage3D
                     * retourne le m�me GridCoverage pour deux pas de temps diff�rents.
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
     * Retourne les noms de toutes les {@linkplain CV_Coverage couvertures spatiales des donn�es}
     * qui ont �t� utilis�es pour le pas de temps de la {@linkplain Clock#getTime date courante}.
     *
     * @return Les noms couvertures spatiales utilis�es pour le pas de temps courant.
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
                        name = "(aucune donn�es)";
                    }
                    names[count++] = name;
                }
            }
            return XArray.resize(names, count);
        }
    }

    /**
     * Retourne toutes les esp�ces se trouvant dans la table.
     *
     * @return Les esp�ces se trouvant dans la table.
     * @throws SQLException si une erreur est survenue lors de l'interrogation de la base de donn�es.
     */
    public Set<fr.ird.animat.Species> getSpecies() throws SQLException {
        return samples.getSpecies();
    }

    /**
     * Retourne l'ensemble des �chantillons pour le pas de temps courant.
     *
     * @return Les �chantillons pour le pas de temps courant.
     * @throws SQLException si une erreur est survenue lors de l'interrogation de la base de donn�es.
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
     * @return <code>true</code> si cette m�thode a pu avancer au pas de temps suivant,
     *         ou <code>false</code> s'il n'y a plus de donn�es disponibles pour les pas
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
     * Lib�re les ressources utilis�es par cet environnement. Toutes
     * les populations contenues dans cet environnement seront d�truites,
     * et les �ventuelles connections avec des bases de donn�es seront
     * ferm�es.
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
     * Lib�re les ressources utilis�es par cet environnement. Cette m�thode ne fait qu'appeler
     * {@link #dispose}. Elle est d�finie afin de permettre � {@link Simulation} de construire
     * un "shutdown hook".
     */
    public void run() {
        dispose();
    }
}
