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
package fr.ird.animat.seas;

// Divers J2SE
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.sql.SQLException;

// JAI
import javax.media.jai.util.Range;

// Geotools
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.resources.XDimension2D;

// Base de donn�es
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;


/**
 * Repr�sentation de l'environnement dans lequel �volueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment extends fr.ird.animat.impl.Environment {
    /**
     * La configuration de la simulation.
     */
    private final Configuration configuration;

    /**
     * Couvertures � utiliser pour chaque param�tres. Les objets {@link Coverage3D} permettent
     * d'obtenir une couverture � une date quelconque.  Les couvertures � une date sp�cifiques
     * seront d�clar�es dans <code>coverage2D</code>  la premi�re fois o� elles sont demand�es
     * pour le pas de temps courant.
     */
    private final Map<Parameter,Coverage3D> coverage3D = new HashMap<Parameter,Coverage3D>();

    /**
     * Les couvertures pour le pas de temps courant. Les �l�ments de cet ensemble ne sont
     * cr��s que lorsqu'ils sont demand�s pour la premi�re fois.
     */
    private final Map<Parameter,Coverage> coverage2D = new HashMap<Parameter,Coverage>();

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
     * Table des captures. Cette table n'est pas utilis�e par cette classe. Elle
     * est plut�t acc�d�e directement par le constructeur de {@link Population}.
     */
    final CatchTable catchs;

    /**
     * Construit un environnement qui utilisera la base de donn�es d'images sp�cifi�e.
     *
     * @param  database Base de donn�es � utiliser.
     * @param  catchs La table des captures. Cette table n'est pas utilis�e par cette classe.
     *         Elle est plut�t acc�d�e directement par le constructeur de {@link Population}.
     * @param  config La configuration de la simulation.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la base de donn�es.
     */
    public Environment(final ImageDataBase database,
                       final FisheryDataBase catchs,
                       final Configuration   config)
            throws SQLException
    {
        super(config.firstTimeStep);
        this.configuration = config;
        this.catchs        = catchs.getCatchTable(config.species.toArray(new String[config.species.size()]));
        ImageTable  images = null;
        SeriesTable series = null;
        Range    timeRange = null;
        for (final Iterator<fr.ird.animat.Parameter> it=config.parameters.iterator(); it.hasNext();) {
            final Parameter parameter = (Parameter) it.next();
            if (images == null) {
                Range range = config.firstTimeStep.getTimeRange();
                range  = new Range(range.getElementClass(),
                                   range.getMinValue(), range.isMinIncluded(), new Date(), true);
                series = database.getSeriesTable();
                images = database.getImageTable(series.getSeries(parameter.series));
                images.setPreferredResolution(new XDimension2D.Double(config.resolution, config.resolution));
                images.setTimeRange(range);
            } else {
                images.setSeries(series.getSeries(parameter.series));
            }
            images.setOperation(parameter.operation);
            final Coverage3D coverage = new Coverage3D(images);
            if (coverage3D.put(parameter, coverage) != null) {
                // Should not happen since 'config.parameters' is a Set.
                Logger.getLogger("fr.ird.animat.seas").warning("Un param�tre est r�p�t� plusieurs fois");
            }
            final Range expand = coverage.getTimeRange();
            if (timeRange == null) {
                timeRange = expand;
            } else {
                timeRange = timeRange.union(expand);
            }
        }
        if (images != null) {
            images.close();
            series.close();
        }
        this.timeRange = timeRange;
    }

    /**
     * Retourne un objet {@link Species} pour l'esp�ce sp�cifi�e.
     */
    final Species wrap(final fr.ird.animat.Species sp) {
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
    public Set<fr.ird.animat.Parameter> getParameters() {
        return configuration.parameters;
    }

    /**
     * Retourne toute la {@linkplain Coverage couverture spatiale des donn�es} � la
     * {@linkplain Clock#getTime date courante} pour un param�tre sp�cifi�.
     *
     * @param  parameter Le param�tre d�sir�.
     * @return La couverture spatiale des donn�es pour le param�tre sp�cifi�.
     *
     * @throws NoSuchElementException si le param�tre sp�cifi� n'existe pas dans cet environnement.
     */
    public Coverage getCoverage(final fr.ird.animat.impl.Parameter parameter) throws NoSuchElementException {
        synchronized (getTreeLock()) {
            if (parameter instanceof Parameter) {
                final Parameter param = (Parameter) parameter;
                Coverage coverage = coverage2D.get(param);
                if (coverage != null) {
                    return coverage;
                }
                final Coverage3D coverage3D = this.coverage3D.get(param);
                if (coverage3D != null) {
                    final Date time = getClock().getTime();
                    coverage = param.applyEvaluator(coverage3D.getGridCoverage2D(time));
                    if (coverage2D.put(param, coverage) != null) {
                        throw new AssertionError();
                    }
                    return coverage;
                }
            }
            return super.getCoverage(parameter);
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
            coverage2D.clear();
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
            coverage2D.clear();
            coverage3D.clear();
            super.dispose();
        }
    }
}
