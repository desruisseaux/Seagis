/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.sql.fishery.fill;

// Geotools dependencies
import org.geotools.cs.Ellipsoid;

// Database
import java.sql.SQLException;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;

// Input/output
import java.io.File;
import java.io.IOException;
import fr.ird.io.map.GEBCOReader;
import fr.ird.io.map.IsolineReader;

// Miscellaneous
import java.util.List;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.IllegalPathStateException;
import org.geotools.resources.Geometry;
import org.geotools.renderer.geom.GeometryCollection;


/**
 * Calcule la distance de la capture la plus proche d'une capture
 * donn�e le m�me jour. Ces distances seront copi�es dans la base
 * de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class CatchTableFiller {
    /**
     * Connexion avec la base de donn�es des p�ches.
     */
    private final FisheryDataBase database;

    /**
     * Ellipso�de � utiliser pour les
     * calculs de distances orthodromiques.
     */
    private final Ellipsoid ellipsoid = Ellipsoid.WGS84;

    /**
     * Construit un objet <code>CatchTableFiller</code> par d�faut.
     *
     * @throws SQLException si la connection avec la base de donn�es a �chou�e.
     */
    public CatchTableFiller() throws SQLException {
        database = new FisheryDataBase();
    }

    /**
     * Calcule la distance orthodromique entre le d�but et la fin de la ligne de p�che.
     * Cette distance n'est pas n�cessairement �gale � la longueur de la ligne, �tant
     * donn� que la ligne peut ne pas avoir �t� mouill�e en ligne droite entre ces
     * deux positions.
     *
     * @param  columnName Nom de la colonne dans laquelle �crire les distances.
     * @throws SQLException si une erreur est survenue
     *         lors d'un acc�s � la base de donn�es.
     */
    public void computeAnchorDistances(final String columnName) throws SQLException {
        final CatchTable        table = database.getCatchTable();
        final List<CatchEntry> catchs = table.getEntries();
        final int               count = catchs.size();
        for (int i=0; i<count; i++) {
            final CatchEntry capture = catchs.get(i);
            final Line2D        line = (Line2D) capture.getShape();
            if (line != null) {
                final double distance = ellipsoid.orthodromicDistance(line.getX1(), line.getY1(), line.getX2(), line.getY2());
                if (!Double.isInfinite(distance) && !Double.isNaN(distance)) {
                    table.setValue(capture, columnName, (float)(distance/1000)); // TODO: units
                }
            }
        }
        table.close();
    }

    /**
     * Calcule la distance la plus courte entre chaque captures faites la m�me journ�e.
     * Le r�sultat du calcul sera �crit dans la base de donn�es dans la colonne sp�cifi�e.
     *
     * @param  maxTimeLag Ecart de temps maximal entre deux donn�es de
     *         p�ches pour consid�rer qu'elles sont prises le m�me jour.
     *         Cet �cart doit �tre exprim� en nombre de millisecondes.
     * @param  columnName Nom de la colonne dans laquelle �crire les
     *         distances les plus courtes qui auront �t� trouv�es.
     * @throws SQLException si une erreur est survenue
     *         lors d'un acc�s � la base de donn�es.
     */
    public void computeInterCatchDistances(final long maxTimeLag, final String columnName) throws SQLException {
        final CatchTable        table = database.getCatchTable();
        final List<CatchEntry> catchs = table.getEntries();

        // TODO: Classer par date de d�but.
        final int count = catchs.size();
        for (int i=0; i<count; i++) {
            final CatchEntry capture = catchs.get(i);
            final long          time = capture.getTime().getTime();
            final Point2D      coord = capture.getCoordinate();
            double  smallestDistance = Double.POSITIVE_INFINITY;
            int scanDirection = -1;
            do { // Run this loop exactly 2 times.
                for (int j=i; (j+=scanDirection)>=0 && j<count;) {
                    final CatchEntry candidate = catchs.get(j);
                    if (Math.abs(time - candidate.getTime().getTime()) > maxTimeLag) {
                        break;
                    }
                    final double distance = ellipsoid.orthodromicDistance(coord, candidate.getCoordinate());
                    if (distance < smallestDistance) {
                        smallestDistance = distance;
                    }
                }
            }
            while ((scanDirection = -scanDirection) >= 0);
            /*
             * Ecrit le r�sultat dans la base de donn�es.
             */
            if (!Double.isInfinite(smallestDistance) && !Double.isNaN(smallestDistance)) {
                table.setValue(capture, columnName, (float)(smallestDistance/1000)); // TODO: units
            }
        }
        table.close();
    }

    /**
     * Calcule la distance la plus courte entre chaque captures et la c�te. Les coordonn�es du
     * point et de la c�te doivent �tre exprim�es selon l'ellipso�de WGS 1984. <strong>Note:
     * ce calcul n'est qu'approximatif</strong>.
     *
     * @param  coast Forme g�om�trique repr�sentant la c�te.
     * @param  columnName Nom de la colonne dans laquelle �crire les
     *         distances les plus courtes qui auront �t� trouv�es.
     * @throws SQLException si une erreur est survenue
     *         lors d'un acc�s � la base de donn�es.
     */
    public void computeCoastDistances(final Shape coast, final String columnName) throws SQLException {
        final CatchTable        table = database.getCatchTable();
        final List<CatchEntry> catchs = table.getEntries();
        final int               count = catchs.size();
        for (int i=0; i<count; i++) {
            final CatchEntry capture = catchs.get(i);
            final Point2D coordinate = capture.getCoordinate();
            final double    distance = computeCoastDistances(coast, coordinate.getX(), coordinate.getY());
            if (!Double.isInfinite(distance) && !Double.isNaN(distance)) {
                table.setValue(capture, columnName, (float)(distance/1000)); // TODO: units
            }
        }
        table.close();
    }

    /**
     * Calcule la distance la plus courte entre le point sp�cifi� et la c�te. Les coordonn�es
     * du point et de la c�te doivent �tre exprim�es selon l'ellipso�de WGS 1984. <strong>Note:
     * ce calcul n'est qu'approximatif</strong>.
     *
     * @param  coast Forme g�om�trique repr�sentant la c�te.
     * @param  x Coordonn�e <var>x</var> du point dont on veut la distance � la c�te.
     * @param  y Coordonn�e <var>y</var> du point dont on veut la distance � la c�te.
     * @return Distance la plus courte.
     */
    private double computeCoastDistances(final Shape coast, final double px, final double py) {
        final double[]    coords = new double[6];
        final Rectangle2D bounds = coast.getBounds2D();
        final PathIterator  iter = coast.getPathIterator(null, 0.001*(bounds.getWidth()+bounds.getHeight()));
        double smallestDistance  = Double.POSITIVE_INFINITY;
        double x0=Double.NaN;
        double y0=Double.NaN;
        double x1=Double.NaN;
        double y1=Double.NaN;
        double x2=Double.NaN;
        double y2=Double.NaN;
        for (; !iter.isDone(); iter.next()) {
            switch (iter.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO: {
                    x2 = x0 = coords[0];
                    y2 = y0 = coords[1];
                    continue;
                }
                case PathIterator.SEG_LINETO: {
                    x1=x2; x2=coords[0];
                    y1=y2; y2=coords[0];
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    x1=x2; x2=x0;
                    y1=y2; y2=y0;
                    break;
                }
                default: throw new IllegalPathStateException();
            }
            // APPROXIMATION IS THERE: 'nearestColinearPoint' is for a cartesian
            // coordinate system, not an ellipsoidal surface. This approximation
            // still okay if <code>coast</code> is build of many small segments.
            final Point2D point = Geometry.nearestColinearPoint(x1, y1, x2, y2, px, py);
            final double distance = ellipsoid.orthodromicDistance(point.getX(), point.getY(), px, py);
            if (distance<smallestDistance) {
                smallestDistance=distance;
            }
        }
        return Double.isInfinite(smallestDistance) ? Double.NaN : smallestDistance;
    }

    /**
     * Ferme la connexion avec la base de donn�es.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la fermeture des connections.
     */
    public void close() throws SQLException {
        database.close();
    }

    /**
     * Lance le calcul des distances les plus courtes
     * entre les donn�es de p�ches.
     *
     * @throws SQLException si une erreur est survenue
     *         lors d'un acc�s � la base de donn�es.
     * @throws IOException si une erreur est survenue lors
     *         de la lecture de la bathym�trie.
     */
    public static void main(final String[] args) throws SQLException, IOException {
        final GEBCOReader reader = new GEBCOReader();
        reader.setInput(new File("compilerData/Oc�an Indien.asc"));
        final GeometryCollection coast = reader.read(0);
        final CatchTableFiller  worker = new CatchTableFiller();
        worker.computeAnchorDistances             ("distance");
        worker.computeInterCatchDistances(0, "distance_p�che");
        worker.computeCoastDistances (coast, "distance_c�te" );
        worker.close();
    }
}
