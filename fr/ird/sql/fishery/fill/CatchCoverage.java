/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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

// Base de donn�es environnementales et de p�ches
import java.sql.SQLException;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.operator.coverage.Evaluator;


/**
 * Donn�es environnementales � des positions de p�ches. Cette couverture offre
 * une m�thode {@link #evaluate(CatchEntry)} qui est capable d'adapter son calcul
 * en fonction de la donn�es de p�che. Par exemple, le calcul pourrait se faire
 * dans une r�gion g�ographique dont la taille d�pend de la longueur de la palangre.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CatchCoverage extends Coverage3D {
    /**
     * La dur�e d'une journ�e, en nombre de millisecondes.
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
     * Construit une couverture � partir des donn�es de la table sp�cifi�e.
     *
     * @param  table Table d'o� proviennent les donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public CatchCoverage(final ImageTable table) throws SQLException {
        super(table);
    }

    /**
     * Evalue les valeurs du param�tre g�ophysique pour une capture. La r�gion g�ographique
     * ainsi que la date des donn�es environnementales � utiliser sont d�termin�es � partir
     * des coordonn�es et de la date de la capture, � l'aide des m�thodes {@link #getShape}
     * et {@link #getTime}. La couverture spatiales des donn�es est obtenues par un appel �
     * {@link #getCoverage(CathEntry)}. Toutes ces m�thodes peuvent �tre red�finies.
     *
     * @param  capture Donn�es de p�che pour laquelle on veut les param�tres environnementaux.
     * @param  dest    Tableau de destination, ou <code>null</code>.
     * @return Les param�tres environnementaux pour la donn�e de p�che sp�cifi�e.
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
     * Retourne toute la couverture spatiale disponible des donn�es environnementales pour
     * la capture sp�cifi�e. Cette m�thode obtient une date appropri�e pour la capture par
     * un appel � <code>{@linkplain #getTime getTime}(capture)</code>, puis obtient les donn�es
     * spatiales correspondantes par un appel � {@link #getGridCoverage2D}. La date de l'image
     * retourn�e n'est pas n�cessairement �gale � celle de la p�che. En effet, l'impl�mentation
     * par d�faut de {@link #getTime} retourne plut�t une date dans les 24 heures avant ou apr�s
     * la p�che, mais dont l'heure a �t� ajust�e de fa�on � correspondre � celle des donn�es
     * satellitaires disponibles.
     * <br><br>
     * Les classes d�riv�es peuvent red�finir cette m�thode pour appliquer une op�ration sur
     * l'image avant de la retourner. Les op�rations de type {@link Evaluator} sont trait�es
     * d'une fa�on sp�ciale par {@link #evaluate(CatchEntry, double[])} afin profiter de leur
     * m�thode {@link Evaluator#evaluate(Shape,double[])}.
     *
     * @param  capture La capture pour laquelle on veut la couverture des donn�es environnementales.
     * @return La couverture de donn�es environnementales pour la capture sp�cifi�e.
     */
    protected Coverage getCoverage(final CatchEntry capture) {
        return getGridCoverage2D(getTime(capture));
    }

    /**
     * Retourne la date d'une image proche de la date de la capture, si une telle image existe.
     * L'impl�mentation par d�faut retourne une date dans les 24 heures avant ou apr�s la p�che,
     * mais dont l'heure a �t� ajust�e de fa�on � coller � celle des donn�es satellitaires
     * disponibles. On �vite ainsi des interpolations si une image est disponible le jour de la
     * p�che. Cette d�marche est justifi�e par le fait que l'heure de la p�che n'est pas bien
     * connue. Dans ces conditions, interpoler entre deux images s�par�es de 24 heures n'a pas
     * beaucoup de sens.
     *
     * @param  capture La capture dont on veut la date et heure.
     * @return Une date et heure proche de celle de la capture (� moins de 24 heures),
     *         mais �ventuellement ajust�e pour correspondre � celle des images.
     */
    protected Date getTime(final CatchEntry capture) {
        final Date time = capture.getTime();
        if (true) {
            /*
             * Change l'heure � laquelle on interpollera de fa�on � utiliser celle de l'image
             * la plus proche, tout en restant dans les 24 heures qui suivent ou pr�cedent la
             * p�che.  Cette op�ration a pour but d'�viter les interpolations lorsqu'on a des
             * donn�es de disponibles la journ�e m�me.
             */
            long dt = time.getTime();
            snap(null, time);
            final long imageTime = time.getTime();
            dt -= imageTime;    // Temps entre la p�che et l'image, positif si la p�che vient apr�s.
            dt = (dt/DAY)*DAY;  // Arrondi (vers 0) l'intervalle � un nombre entier de jours.
            time.setTime(imageTime + dt);
            assert Math.abs(time.getTime() - capture.getTime().getTime())<DAY : dt;
        }
        return time;
    }

    /**
     * Retourne la r�gion g�ographique � prendre en compte pour les calculs relatifs � la
     * capture sp�cifi�e. Cette r�gion g�ographique inclue g�n�ralement les coordonn�es de
     * la p�che, mais pas obligatoirement. Des classes d�riv�es pourraient red�finir cette
     * m�thode pour s'int�resser par exemple � ce qui se passe seulement � l'ouest de la
     * zone de p�che.
     *
     * @param  capture Capture pour laquelle �valuer les param�tres environnementaux.
     * @return R�gion g�ographique � prendre en compte pour cette capture.
     */
    protected Shape getShape(final CatchEntry capture) {
        final Point2D coord = capture.getCoordinate();
        return new Ellipse2D.Double(coord.getX()-semiX, coord.getY()-semiY, 2*semiX, 2*semiY);
    }
}
