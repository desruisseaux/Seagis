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
package fr.ird.database.sample;

// J2SE
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.Arrays;
import java.util.Date;

// Geotools
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.ct.TransformException;

// Base de données environnementales et de pêches
import java.sql.SQLException;
import fr.ird.operator.coverage.Evaluator;
import fr.ird.database.coverage.CoverageTable;


/**
 * Données environnementales à des positions d'échantillons. Cette couverture offre une
 * méthode {@link #evaluate(SampleEntry,RelativePositionEntry,double[]) evaluate(...)}
 * qui est capable d'adapter son calcul en fonction des données de l'échantillon. Par
 * exemple, le calcul pourrait se faire dans une région géographique dont la taille
 * dépend de la longueur de la palangre.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Coverage3D extends fr.ird.database.coverage.Coverage3D {
    /**
     * Incertitude dans la détermination de la date des échantillons, en nombre de millisecondes.
     * Afin de réduire les interpolations temporelles, les dates des échantillons sont "calées"
     * sur les dates des images sans que la différence soit supérieure à ce nombre.
     * La valeur par défaut est de 24 heures.
     */
    private static final long TIME_UNCERTAINTY = 24*60*60*1000L;

    /**
     * Petite valeur à utiliser lors des vérifications. Cette valeur devrait être près
     * de celle du type <code>float</code> (et non celle du type <code>double</code>).
     */
    private static final double EPS = 1E-5;

    /**
     * La longueur du demi-axe le long de l'axe des <var>x</var>.
     *
     * @task TODO: Rendre ce paramètre configurable dans une version future.
     */
    private static final double semiX = 10.0/60;

    /**
     * La longueur du demi-axe le long de l'axe des <var>y</var>.
     *
     * @task TODO: Rendre ce paramètre configurable dans une version future.
     */
    private static final double semiY = 10.0/60;

    /**
     * Le dernier avertissement reporté, ou <code>null</code> s'il n'y en a pas.
     */
    transient LogRecord lastWarning;

    /**
     * <code>true</code> si cette implémentation est l'implémentation par défaut. Dans ce
     * dernier cas, {@link #evaluate(SampleEntry,double[])} pourra profiter de la méthode
     * {@link #evaluate(Point2D,Date,double[])}, qui applique une interpolation temporelle.
     * Ce bricolage est temporaire et pourra être supprimé lorsque {@link #getGridCoverage2D}
     * retournera une image interpolée.
     */
    private final boolean isDefaultImplementation;

    /**
     * Construit une couverture à partir des données de la table spécifiée.
     *
     * @param  table Table d'où proviennent les données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     * @throws TransformException si une transformation de coordonnées était nécessaire et a échoué.
     */
    public Coverage3D(final CoverageTable table) throws SQLException, TransformException {
        super(table);
        isDefaultImplementation = Coverage3D.class.equals(getClass());
    }

    /**
     * Évalue les valeurs du paramètre géophysique pour un échantillon. La région géographique
     * ainsi que la date des données environnementales à utiliser sont déterminées à partir de
     * <code>position.{@link RelativePositionEntry#getCoordinate getCoordinate}(sample)</code> et
     * <code>position.{@link RelativePositionEntry#getTime getTime}(sample)</code>. La couverture
     * spatiales des données est obtenues par un appel à
     * <code>{@link #getCoverage getCoverage}(sample)<code>.
     * Toutes ces méthodes peuvent être redéfinies.
     *
     * @param  sample Échantillon pour lequel on veut les paramètres environnementaux.
     * @param  position Position relative à laquelle évaluer les paramètres environnementaux, ou
     *                  <code>null</code> pour les évaluer à la position exacte de l'échantillon.
     * @param  dest Tableau de destination, ou <code>null</code>.
     * @return Les paramètres environnementaux pour l'échantillon spécifié.
     */
    public synchronized double[] evaluate(final SampleEntry           sample,
                                          final RelativePositionEntry position,
                                          double[]                    dest)
    {
        final Date    time;
        final Point2D coord;
        if (position != null) {
            time  = position.getTime(sample);
            coord = position.getCoordinate(sample);
        } else {
            time  = sample.getTime();
            coord = sample.getCoordinate();
        }
        adjust(time);
        if (isDefaultImplementation) {
            // Faster and more accurate than 'getGridCoverage2D()'
            dest = evaluate(coord, time, dest);
            assert testGridCoverage2D(coord, time, dest);
            return dest;
        }
        final Coverage coverage = getCoverage(sample, time);
        if (coverage == null) {
            final int numBands = getNumSampleDimensions();
            if (dest == null) {
                dest = new double[numBands];
            }
            Arrays.fill(dest, 0, numBands, Double.NaN);
            return dest;
        }
        if (coverage instanceof Evaluator) {
            return ((Evaluator) coverage).evaluate(getShape(sample, coord), dest);
        }
        if (coverage instanceof GridCoverage) {
            return ((GridCoverage) coverage).evaluate(coord, dest);
        }
        return coverage.evaluate(new CoordinatePoint(coord), dest);
    }

    /**
     * Retourne toute la couverture spatiale disponible des données environnementales
     * pour l'échantillon spécifié. Cette couverture sera utilisée par la méthode
     * <code>{@link #evaluate(SampleEntry,RelativePositionEntry,double[]) evaluate}(sample, ...)</code>
     * L'implémentation par défaut retourne simplement
     * <code>{@link #getGridCoverage2D getGridCoverage2D}(time)</code>.
     * Les classes dérivées peuvent redéfinir cette méthode pour appliquer une opération sur
     * l'image avant de la retourner. Les opérations de type {@link Evaluator} sont traitées
     * d'une façon spéciale afin profiter de leur méthode {@link Evaluator#evaluate(Shape,double[])}.
     *
     * @param  sample L'échantillon pour laquelle on veut la couverture des données environnementales.
     * @param  time La date pour laquelle on veut l'image. Cette date n'est pas nécessairement égale
     *         à celle de l'échantillon.
     * @return La couverture de données environnementales pour l'échantillon spécifié.
     */
    protected Coverage getCoverage(final SampleEntry sample, final Date time) {
        return getGridCoverage2D(time);
    }

    /**
     * Compare les valeurs calculées par {@link #getGridCoverage2D} avec le tableau de
     * valeurs spécifié. Cette méthode sert à vérifier la cohérence des interpolations.
     */
    private boolean testGridCoverage2D(final Point2D coord, final Date time, final double[] values) {
        final GridCoverage coverage;
        try {
            coverage = getGridCoverage2D(time);
        } catch (IllegalArgumentException exception) {
            // This is just used in an assertion;
            // do not prevent the code to work like usual.
            SampleDataBase.LOGGER.warning(exception.getLocalizedMessage());
            return true;
        }
        if (coverage != null) {
            final double[] tests = coverage.evaluate(coord, (double[])null);
            if (tests.length != values.length) {
                return false;
            }
            for (int i=0; i<values.length; i++) {
                final double v = values[i];
                final double t = tests [i];
                if (Math.abs(t-v) > EPS*Math.max(Math.abs(t), Math.abs(v))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Ajuste la date de l'échantillon si elle est proche de la date d'une image.
     * Cette méthode retourne une date dans les 24 heures avant ou après l'échantillon,
     * mais dont l'heure a été ajustée de façon à coller à celle des données satellitaires
     * disponibles. On évite ainsi des interpolations si une image est disponible le jour de
     * l'échantillon. Cette démarche est justifiée par le fait que l'heure de la pêche n'est
     * pas bien connue. Dans ces conditions, interpoler entre deux images séparées de 24 heures
     * n'a pas beaucoup de sens.
     *
     * @param  time En entré, date d'un échantillon. En sortie, une date et heure proche de
     *         celle de l'échantillon (à moins de 24 heures), mais éventuellement ajustée pour
     *         correspondre à celles des images.
     */
    final void adjust(final Date time) {
        if (TIME_UNCERTAINTY != 0) {
            /*
             * Change l'heure à laquelle on interpollera de façon à utiliser celle de l'image
             * la plus proche, tout en restant dans les 24 heures qui suivent ou précedent la
             * pêche.  Cette opération a pour but d'éviter les interpolations lorsqu'on a des
             * données de disponibles la journée même.
             */
            final long sampleTime = time.getTime();
            snap(null, time);
            final long  imageTime = time.getTime();
            long dt = sampleTime-imageTime; // Temps entre l'échantillon et l'image, positif si l'échantillon vient après.
            dt = (dt/TIME_UNCERTAINTY)*TIME_UNCERTAINTY;  // Arrondi (vers 0) l'intervalle à un nombre entier de jours.
            time.setTime(imageTime + dt);
            assert Math.abs(time.getTime() - sampleTime)<TIME_UNCERTAINTY : dt;
        }
    }

    /**
     * Retourne la région géographique à prendre en compte pour les calculs relatifs à
     * l'échantillon spécifiée. Cette région géographique inclue généralement les coordonnées
     * de la pêche, mais pas obligatoirement. Des classes dérivées pourraient redéfinir cette
     * méthode pour s'intéresser par exemple à ce qui se passe seulement à l'ouest de la
     * zone de pêche. Cette méthode est appelée par
     * <code>{@link #evaluate(SampleEntry,RelativePositionEntry,double[]) evaluate}(sample, ...)</code>
     * lorsque la couverture des données environnementales est un objet {@link Evaluator}.
     *
     * @param  sample Capture pour laquelle évaluer les paramètres environnementaux.
     * @param  coord  Coordonnée à laquelle évaluer les paramètres environnementaux.
     *                Cette coordonnée aura été obtenue avec {@link RelativePosition#getCoordinate}.
     * @return Région géographique à prendre en compte pour cet échantillon.
     */
    protected Shape getShape(final SampleEntry sample, final Point2D coord) {
        return new Ellipse2D.Double(coord.getX()-semiX, coord.getY()-semiY, 2*semiX, 2*semiY);
    }

    /**
     * Enregistre un message vers le journal des événements. Cette méthode redéfinie celle
     * de {@link fr.ird.database.coverage.Coverage3D} de façon à n'enregistrer que le premier
     * et dernier message d'une succession de points en dehors de la région spatio-temporelle.
     */
    protected void log(final LogRecord record) {
        if (lastWarning != null) {
            super.log(lastWarning);
            lastWarning = null;
        }
        super.log(record);
    }
}
