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

// Geotools dependencies
import org.geotools.gc.GridCoverage;

// Divers
import java.util.Arrays;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

// Base de donn�es environnementales et de p�ches
import java.sql.SQLException;
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.fishery.CatchEntry;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.ParameterValue;


/**
 * Donn�es environnementales � des positions de p�ches. Cette couverture offre
 * une m�thode {@link #evaluate(CatchEntry,Evaluator)} qui est capable d'adapter
 * son calcul en fonction de la donn�es de p�che. Par exemple, le calcul pourrait
 * se faire dans une r�gion g�ographique dont la taille d�pend de la longueur
 * de la palangre.
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
     * Evalue les valeurs du param�tre g�ophysique pour une capture.
     * La r�gion g�ographique ainsi que la date des donn�es environnementales
     * � utiliser sont d�termin�es � partir des coordonn�es et de la date de
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

    /**
     * Retourne la date � laquelle interpoller les calculs relatifs � la capture sp�cifi�e.
     * Cette date ne sera pas n�cessairement �gale � celle de la p�che. L'impl�mentation par
     * d�faut retourne une date le m�me jour que la p�che, mais dont l'heure a �t� ajust�e de
     * fa�on � coller � celle des donn�es satellitaires disponibles (et �viter ainsi des
     * interpolations si une image est disponible le jour de la p�che).
     */
    protected Date getTime(final CatchEntry capture) {
        final Date time = capture.getTime();
        if (true) {
            /*
             * Change l'heure � laquelle on interpollera de fa�on � �tre celle de l'image
             * la plus proche, tout en restant dans les 24 heures qui suivent la p�che.
             * Cette op�ration a pour but d'�viter les interpolations lorsqu'on a des
             * donn�es disponibles la journ�e m�me ("journ�e m�me" �tant d�finie comme
             * �tant les 24 heures qui suivent la p�che).
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
