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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.sql.SQLException;

// OpenGIS
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.resources.XDimension2D;

// Base de données
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment extends fr.ird.animat.Environment {
    /**
     * La configuration de la simulation.
     */
    private final Configuration configuration;

    /**
     * Couvertures à utiliser pour chaque paramètres.
     */
    private final Map<Parameter,Coverage3D> coverages = new HashMap<Parameter,Coverage3D>();

    /**
     * Construit un environnement qui utilisera
     * la base de données d'images spécifiée.
     *
     * @param  database Base de données à utiliser.
     * @param  config La configuration de la simulation.
     * @throws SQLException si une erreur est survenue
     *         lors de l'accès à la base de données.
     */
    public Environment(final ImageDataBase database,
                       final Configuration   config)
            throws SQLException
    {
        super(config.firstTimeStep);
        this.configuration = config;
        ImageTable  images = null;
        SeriesTable series = null;
        for (final Iterator<fr.ird.animat.Parameter> it=config.parameters.iterator(); it.hasNext();) {
            final Parameter parameter = (Parameter) it.next();
            if (images == null) {
                series = database.getSeriesTable();
                images = database.getImageTable(series.getSeries(parameter.series));
                images.setPreferredResolution(new XDimension2D.Double(config.resolution, config.resolution));
                images.setTimeRange(config.firstTimeStep.getStartTime(), new Date());
            } else {
                images.setSeries(series.getSeries(parameter.series));
            }
            images.setOperation(parameter.operation);
            coverages.put(parameter, new Coverage3D(images));
        }
        if (images != null) {
            images.close();
            series.close();
        }
    }

    /**
     * Retourne l'ensemble des paramètres compris dans cet environnement.
     */
    public Set<fr.ird.animat.Parameter> getParameters() {
        return configuration.parameters;
    }

    /**
     * Retourne les données d'un paramètre sous forme d'un objet
     * {@link Coverage}.
     */
    public Coverage getCoverage(final fr.ird.animat.Parameter parameter) throws NoSuchElementException {
        if (parameter instanceof Parameter) {
            final Parameter param = (Parameter) parameter;
            final Coverage3D coverage = coverages.get(param);
            if (coverage != null) {
                final Date time = getTimeStep().getStartTime();
                return param.applyEvaluator(coverage.getGridCoverage2D(time));
            }
        }
        return super.getCoverage(parameter);
    }

    /**
     * Libère les ressources utilisées par cet environnement. Toutes
     * les populations contenues dans cet environnement seront détruites,
     * et les éventuelles connections avec des bases de données seront
     * fermées.
     */
    public void dispose() {
        super.dispose();
        coverages.clear();
    }
}
