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

// J2SE dependencies
import java.util.Arrays;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;

// Base de données environnementales et de pêches
import java.sql.SQLException;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.operator.coverage.Evaluator;


/**
 * Données environnementales à des positions de pêches. Cette couverture offre
 * une méthode {@link #evaluate(CatchEntry)} qui est capable d'adapter son calcul
 * en fonction de la données de pêche. Par exemple, le calcul pourrait se faire
 * dans une région géographique dont la taille dépend de la longueur de la palangre.
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
     * Evalue les valeurs du paramètre géophysique pour une capture. La région géographique
     * ainsi que la date des données environnementales à utiliser sont déterminées à partir
     * des coordonnées et de la date de la capture, à l'aide des méthodes {@link #getShape}
     * et {@link #getTime}. La couverture spatiales des données est obtenues par un appel à
     * {@link #getCoverage(CathEntry)}. Toutes ces méthodes peuvent être redéfinies.
     *
     * @param  capture Données de pêche pour laquelle on veut les paramètres environnementaux.
     * @param  dest    Tableau de destination, ou <code>null</code>.
     * @return Les paramètres environnementaux pour la donnée de pêche spécifiée.
     */
    public synchronized double[] evaluate(final CatchEntry capture, double[] dest) {
        final Coverage coverage = getCoverage(capture);
        if (coverage == null) {
            final int numBands = getNumSampleDimensions();
            if (dest == null) {
                dest = new double[numBands];
            }
            Arrays.fill(dest, 0, numBands, Double.NaN);
            return dest;
        }
        if (coverage instanceof Evaluator) {
            return ((Evaluator) coverage).evaluate(getShape(capture), dest);
        } else {
            final Point2D coord = capture.getCoordinate();
            if (coverage instanceof GridCoverage) {
                return ((GridCoverage) coverage).evaluate(coord, dest);
            } else {
                return coverage.evaluate(new CoordinatePoint(coord), dest);
            }
        }
    }

    /**
     * Retourne toute la couverture spatiale disponible des données environnementales pour
     * la capture spécifiée. Cette méthode obtient une date appropriée pour la capture par
     * un appel à <code>{@linkplain #getTime getTime}(capture)</code>, puis obtient les données
     * spatiales correspondantes par un appel à {@link #getGridCoverage2D}. La date de l'image
     * retournée n'est pas nécessairement égale à celle de la pêche. En effet, l'implémentation
     * par défaut de {@link #getTime} retourne plutôt une date dans les 24 heures avant ou après
     * la pêche, mais dont l'heure a été ajustée de façon à correspondre à celle des données
     * satellitaires disponibles.
     * <br><br>
     * Les classes dérivées peuvent redéfinir cette méthode pour appliquer une opération sur
     * l'image avant de la retourner. Les opérations de type {@link Evaluator} sont traitées
     * d'une façon spéciale par {@link #evaluate(CatchEntry, double[])} afin profiter de leur
     * méthode {@link Evaluator#evaluate(Shape,double[])}.
     *
     * @param  capture La capture pour laquelle on veut la couverture des données environnementales.
     * @return La couverture de données environnementales pour la capture spécifiée.
     */
    protected Coverage getCoverage(final CatchEntry capture) {
        return getGridCoverage2D(getTime(capture));
    }

    /**
     * Retourne la date d'une image proche de la date de la capture, si une telle image existe.
     * L'implémentation par défaut retourne une date dans les 24 heures avant ou après la pêche,
     * mais dont l'heure a été ajustée de façon à coller à celle des données satellitaires
     * disponibles. On évite ainsi des interpolations si une image est disponible le jour de la
     * pêche. Cette démarche est justifiée par le fait que l'heure de la pêche n'est pas bien
     * connue. Dans ces conditions, interpoler entre deux images séparées de 24 heures n'a pas
     * beaucoup de sens.
     *
     * @param  capture La capture dont on veut la date et heure.
     * @return Une date et heure proche de celle de la capture (à moins de 24 heures),
     *         mais éventuellement ajustée pour correspondre à celle des images.
     */
    protected Date getTime(final CatchEntry capture) {
        final Date time = capture.getTime();
        if (true) {
            /*
             * Change l'heure à laquelle on interpollera de façon à utiliser celle de l'image
             * la plus proche, tout en restant dans les 24 heures qui suivent ou précedent la
             * pêche.  Cette opération a pour but d'éviter les interpolations lorsqu'on a des
             * données de disponibles la journée même.
             */
            long dt = time.getTime();
            snap(null, time);
            final long imageTime = time.getTime();
            dt -= imageTime;    // Temps entre la pêche et l'image, positif si la pêche vient après.
            dt = (dt/DAY)*DAY;  // Arrondi (vers 0) l'intervalle à un nombre entier de jours.
            time.setTime(imageTime + dt);
            assert Math.abs(time.getTime() - capture.getTime().getTime())<DAY : dt;
        }
        return time;
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
}
