/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.sql.fishery.fill;

// Geotools dependencies
import org.geotools.gc.GridCoverage;

// Divers
import java.util.Arrays;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

// Base de données environnementales et de pêches
import java.sql.SQLException;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.fishery.CatchEntry;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.ParameterValue;


/**
 * Données environnementales à des positions de pêches. Cette couverture offre
 * une méthode {@link #evaluate(CatchEntry,Evaluator)} qui est capable d'adapter
 * son calcul en fonction de la données de pêche. Par exemple, le calcul pourrait
 * se faire dans une région géographique dont la taille dépend de la longueur
 * de la palangre.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CatchCoverage extends Coverage3D {
    /**
     * La durée d'une journée, en nombre de millisecondes.
     */
    private static final long DAY = 24*60*60*1000L;

    /**
     * The semi axis length along <var>x</var> axis.
     */
    private final double semiX = 10.0/60;

    /**
     * The semi axis length along <var>y</var> axis.
     */
    private final double semiY = 10.0/60;

    /**
     * Construit une couverture à partir des données de la table spécifiée.
     *
     * @param  table Table d'où proviennent les données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public CatchCoverage(final ImageTable table) throws SQLException {
        super(table);
    }

    /**
     * Evalue les valeurs du paramètre géophysique pour une capture.
     * La région géographique ainsi que la date des données environnementales
     * à utiliser sont déterminées à partir des coordonnées et de la date de
     * la capture.
     */
    public synchronized double[] evaluate(final CatchEntry capture, final Evaluator evaluator) {
        final GridCoverage coverage = getGridCoverage2D(getTime(capture));
        if (coverage == null) {
            final double[] result = new double[getNumSampleDimensions()];
            Arrays.fill(result, Double.NaN);
            return result;
        }
        final ParameterValue[] values = evaluator.evaluate(coverage, getShape(capture));
        final double[] result = new double[values.length];
        for (int i=0; i<values.length; i++) {
            final ParameterValue value = values[i];
            result[i] = (value!=null) ? value.getValue() : Double.NaN;
        }
        return result;
    }

    /**
     * Retourne la région géographique à prendre en compte pour les calculs relatifs à la
     * capture spécifiée. Cette région géographique inclue généralement les coordonnées de
     * la pêche, mais pas obligatoirement. Des classes dérivées pourraient redéfinir cette
     * méthode pour s'intéresser par exemple à ce qui se passe seulement à l'ouest de la
     * zone de pêche.
     *
     * @param  capture Capture pour laquelle évaluer les paramètres environnementaux.
     * @return Région géographique à prendre en compte pour cette capture.
     */
    protected Shape getShape(final CatchEntry capture) {
        final Point2D coord = capture.getCoordinate();
        return new Ellipse2D.Double(coord.getX()-semiX, coord.getY()-semiY, 2*semiX, 2*semiY);
    }

    /**
     * Retourne la date à laquelle interpoller les calculs relatifs à la capture spécifiée.
     * Cette date ne sera pas nécessairement égale à celle de la pêche. L'implémentation par
     * défaut retourne une date le même jour que la pêche, mais dont l'heure a été ajustée de
     * façon à coller à celle des données satellitaires disponibles (et éviter ainsi des
     * interpolations si une image est disponible le jour de la pêche).
     */
    protected Date getTime(final CatchEntry capture) {
        final Date time = capture.getTime();
        if (true) {
            /*
             * Change l'heure à laquelle on interpollera de façon à être celle de l'image
             * la plus proche, tout en restant dans les 24 heures qui suivent la pêche.
             * Cette opération a pour but d'éviter les interpolations lorsqu'on a des
             * données disponibles la journée même ("journée même" étant définie comme
             * étant les 24 heures qui suivent la pêche).
             */
            long dt = time.getTime();
            snap(null, time);
            final long imageTime = time.getTime();
            dt -= imageTime;
            if (dt >= 0) {
                dt += (DAY-1); // Force round to +infinity
            }
            time.setTime(imageTime + (dt/DAY)*DAY);
            assert((dt=time.getTime()-capture.getTime().getTime())<DAY && dt>=0) : dt;
        }
        return time;
    }
}
