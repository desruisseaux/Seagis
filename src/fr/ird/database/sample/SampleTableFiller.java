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
package fr.ird.database.sample;

// J2SE
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.IllegalPathStateException;
import java.sql.SQLException;

// Geotools
import org.geotools.cs.Ellipsoid;
import org.geotools.resources.XMath;
import org.geotools.resources.Geometry;
import org.geotools.resources.Arguments;

// Seagis
import fr.ird.database.Table;
import fr.ird.database.sample.sql.SampleDataBase;


/**
 * Calcule les distances entre des �chantillons du m�me jour.
 * Ces distances seront copi�es dans la base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleTableFiller implements Table {
    /**
     * Connexion avec la base de donn�es des �chantillons.
     */
    private SampleTable table;

    /**
     * Connexion vers la base de donn�es � fermer lorsque {@link #toClose} sera appel�e,
     * ou <code>null</code> si aucune.
     */
    private SampleDataBase database;

    /**
     * Ellipso�de � utiliser pour les calculs de distances orthodromiques.
     */
    private final Ellipsoid ellipsoid = Ellipsoid.WGS84;

    /**
     * <code>true</code> si cet objet poss�de les connexion vers la base de donn�es.
     * Dans ce cas, {@link #close} fermera cette connexion.
     */
    private boolean canClose;

    /**
     * Construit un objet <code>SampleTableFiller</code> puisant les donn�es
     * dans la base de donn�es par d�faut.
     *
     * @throws SQLException si la connection avec la base de donn�es a �chou�e.
     */
    public SampleTableFiller() throws SQLException {
        database = new SampleDataBase();
        table    = database.getSampleTable();
        canClose = true;
    }

    /**
     * Construit un objet <code>SampleTableFiller</code> puisant les donn�es dans la table
     * sp�cifi�e. Cette table ne sera <strong>pas</strong> ferm�e par la m�thode {@link #close},
     * puisqu'elle n'appartient pas � cet objet.
     *
     * @throws SQLException si la connection avec la base de donn�es a �chou�e.
     */
    public SampleTableFiller(final SampleTable table) throws SQLException {
        this.table = table;
    }

    /**
     * Calcule la longueur du trajet de l'�chantillon. Plus sp�cifiquement, cette m�thode calcule la
     * distance orthodromique entre les points de la forme retourn�e par {@link SampleEntry#getShape}.
     * Dans le cas d'une p�che � la palangre, �a correspondrait � la longueur de la ligne de p�che si
     * on suppose que <code>getShape()</code> contient les positions pr�cises de cette ligne.
     *
     * @param  columnName Nom de la colonne dans laquelle �crire les longueurs.
     * @throws SQLException si une erreur est survenue lors d'un acc�s � la base de donn�es.
     */
    public void computePathLength(final String columnName) throws SQLException {
        for (final SampleEntry sample : table.getEntries()) {
            final Shape shape = sample.getShape();
            if (shape != null) {
                double length = 0;
                double x1 = Double.NaN;
                double y1 = Double.NaN;
                final double[] coords = new double[6];
                final PathIterator it = shape.getPathIterator(null, Geometry.getFlatness(shape));
                while (!it.isDone()) {
                    switch (it.currentSegment(coords)) {
                        default: {
                            throw new IllegalPathStateException();
                        }
                        case PathIterator.SEG_LINETO: {
                            if (Double.isNaN(x1) || Double.isNaN(y1)) {
                                throw new IllegalPathStateException();
                            }
                            length += ellipsoid.orthodromicDistance(x1, y1, coords[0], coords[1]);
                            // fall through
                        }
                        case PathIterator.SEG_MOVETO: {
                            x1 = coords[0];
                            y1 = coords[1];
                            break;
                        }
                    }
                }
                if (!Double.isInfinite(length) && !Double.isNaN(length)) {
                    table.setValue(sample, columnName, (float)(length/1000)); // TODO: units
                }
            }
        }
    }

    /**
     * Calcule la distance la plus courte entre chaque �chantillon pris la m�me journ�e.
     * Le r�sultat du calcul sera �crit dans la base de donn�es dans la colonne sp�cifi�e.
     *
     * @param  maxTimeLag Ecart de temps maximal entre deux �chantillons pour consid�rer qu'is
     *         sont pris le m�me jour. Cet �cart doit �tre exprim� en nombre de millisecondes.
     * @param  columnName Nom de la colonne dans laquelle �crire les distances les plus courtes
     *         qui auront �t� trouv�es.
     * @throws SQLException si une erreur est survenue lors d'un acc�s � la base de donn�es.
     */
    public void computeInterSampleDistances(final long maxTimeLag,
                                            final String columnName)
            throws SQLException
    {
        final Collection<SampleEntry> list = table.getEntries();
        final SampleEntry[] samples = (SampleEntry[])list.toArray(new SampleEntry[list.size()]);
        Arrays.sort(samples, new Comparator<SampleEntry>() {
            public int compare(final SampleEntry e1, final SampleEntry e2) {
                return e1.getTime().compareTo(e2.getTime());
            }
        });
        for (int i=0; i<samples.length; i++) {
            final SampleEntry  sample = samples[i];
            final CruiseEntry  cruise = sample.getCruise();
            final long           time = sample.getTime().getTime();
            final Point2D       coord = sample.getCoordinate();
            double   smallestDistance = Double.POSITIVE_INFINITY;
            int scanDirection = -1;
            do { // Run this loop exactly 2 times.
                for (int j=i; (j+=scanDirection)>=0 && j<samples.length;) {
                    final SampleEntry candidate = samples[j];
                    if (Math.abs(time - candidate.getTime().getTime()) > maxTimeLag) {
                        break;
                    }
                    if (cruise != null) {
                        if (cruise.equals(candidate.getCruise())) {
                            // Ignore les positions qui proviennent du m�me bateau.
                            continue;
                        }
                    }
                    final double distance = ellipsoid.orthodromicDistance(coord, candidate.getCoordinate());
                    if (distance < smallestDistance) {
                        smallestDistance = distance;
                    }
                }
            }
            while ((scanDirection = -scanDirection) >= 0);
            if (!Double.isInfinite(smallestDistance) && !Double.isNaN(smallestDistance)) {
                table.setValue(sample, columnName, (float)(smallestDistance/1000)); // TODO: units
            }
        }
    }

    /**
     * Calcule la vitesse des bateaux en mesurant la distance orthodromique entre un point et la
     * position de la journ�e pr�c�dente.
     *
     * @param  columnName Nom de la colonne dans laquelle �crire les vitesses.
     * @throws SQLException si une erreur est survenue lors d'un acc�s � la base de donn�es.
     */
    public void computeSpeed(final String columnName) throws SQLException {
        final Map<CruiseEntry,SampleEntry> positions = new HashMap<CruiseEntry,SampleEntry>();
        for (final SampleEntry sample : table.getEntries()) {
            final CruiseEntry cruise = sample.getCruise();
            if (cruise != null) {
                final Point2D coord = sample.getCoordinate();
                final Date    time  = sample.getTime();
                if (coord!=null && time!=null) {
                    final SampleEntry last = positions.put(cruise, sample);
                    if (last != null) {
                        assert cruise.equals(last.getCruise()) : cruise;
                        double distance = ellipsoid.orthodromicDistance(coord, last.getCoordinate());
                        final double delay;
                        delay = (time.getTime() - last.getTime().getTime()) / (24*60*60*1000.0);
                        distance /= delay;
                        if (!Double.isNaN(distance) && !Double.isInfinite(distance)) {
                            table.setValue(sample, columnName, (float)(distance/1000)); // TODO: units
                        }
                    }
                }
            }
        }
    }

    /**
     * Calcule la distance la plus courte entre chaque �chantillon et la c�te. Les coordonn�es
     * du point et de la c�te doivent �tre exprim�es selon l'ellipso�de WGS 1984. <strong>Note:
     * ce calcul n'est qu'approximatif</strong>.
     *
     * @param  coast Forme g�om�trique repr�sentant la c�te.
     * @param  columnName Nom de la colonne dans laquelle �crire les
     *         distances les plus courtes qui auront �t� trouv�es.
     * @throws SQLException si une erreur est survenue
     *         lors d'un acc�s � la base de donn�es.
     */
    public void computeCoastDistances(final Shape coast, final String columnName) throws SQLException {
        for (final SampleEntry sample : table.getEntries()) {
            final Point2D coordinate = sample.getCoordinate();
            final double distance = computeCoastDistances(coast, coordinate.getX(), coordinate.getY());
            if (!Double.isInfinite(distance) && !Double.isNaN(distance)) {
                table.setValue(sample, columnName, (float)(distance/1000)); // TODO: units
            }
        }
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
        final PathIterator  iter = coast.getPathIterator(null, Geometry.getFlatness(coast));
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
     * Lib�re les ressources utilis�es par cet objet. Cette m�thode ne ferme <strong>pas</strong>
     * la table sp�cifi�e au constructeur, puisqu'elle n'appartient pas � cet objet.
     */
    public void close() throws SQLException {
        if (canClose) {
            table.close();
            canClose = false;
        }
        table = null;
        if (database != null) {
            database.close();
            database = null;
        }
    }

    /**
     * Lance le calcul des distances les plus courtes entre les donn�es de p�ches.
     */
    public static void main(final String[] args) throws SQLException {
        final Arguments arguments = new Arguments(args);
        SampleTableFiller worker = null;
        try {
            worker = new SampleTableFiller();
            worker.computeSpeed("vitesse");
            worker.computeInterSampleDistances(12*60*60*1000, "voisin");
        } catch (Exception exception) {
            exception.printStackTrace(arguments.out);
        }
        if (worker != null) {
            worker.close();
        }
    }
}
