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
package fr.ird.sql.image;

// J2SE dependencies
import java.util.Date;
import java.sql.SQLException;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.Projection;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.ProjectedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;


/**
 * Table des syst�mes de coordonn�es utilis�s par les images.
 *
 * NOTE: L'impl�mentation actuelle code "en dur" les syst�mes de coordonn�es � utiliser pour
 *       diff�rentes s�ries. Une version future devra aller chercher cette information dans
 *       la base de donn�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CoordinateSystemTable extends Table {
    /**
     * Nombre de millisecondes entre le 01/01/1970 00:00 UTC et le 01/01/1950 00:00 UTC.
     * Le 1er janvier 1970 est l'epoch du Java, tandis que le 1er janvier 1950 est celui
     * de la Nasa (son jour julier "0"). La constante <code>EPOCH</code> sert � faire les
     * conversions d'un syst�me � l'autre.
     */
    private static final long EPOCH = -631152000000L; // Pour 1958, utiliser -378691200000L;

    /**
     * Nombre de millisecondes dans une journ�e.
     */
    static final double DAY = 24*60*60*1000;

    /**
     * Convertit un jour julien en date.
     */
    static Date toDate(final double t) {
        return new Date(Math.round(t*DAY)+EPOCH);
    }

    /**
     * Convertit une date en nombre de jours �coul�s depuis le 1er janvier 1950.
     * Les valeurs <code>[MIN/MAX]_VALUE</code> sont converties en infinies.
     */
    static double toJulian(final long time) {
        if (time==Long.MIN_VALUE) return Double.NEGATIVE_INFINITY;
        if (time==Long.MAX_VALUE) return Double.POSITIVE_INFINITY;
        return (time-EPOCH)/DAY;
    }

    /**
     * Syst�me de coordonn�es temporelles d'AVISO.
     */
    private static final CoordinateSystem TIME = new TemporalCoordinateSystem("Aviso", new Date(EPOCH));

    /**
     * L'ellipso�de utilis� par AVISO. Tr�s proche de WGS84.
     */
    private static final Ellipsoid ELLIPSOID =
                Ellipsoid.createFlattenedSphere("AVISO", 6378136.3, 298.257, Unit.METRE);
    
    /**
     * Syst�me de coordonn�es utilis�es par les images AVISO.
     * Note: En principe, l'ellipso�de devrait �tre celui d'AVISO. Mais la diff�rence avec
     *       WGS84 �tant faible, nous l'ignorons pour l'instant.
     */
    private static final CompoundCoordinateSystem MERCATOR =
                new CompoundCoordinateSystem("SEAS mercator",
                new ProjectedCoordinateSystem("AVISO", GeographicCoordinateSystem.WGS84,
                new Projection("Mercator", "Mercator_1SP", Ellipsoid.WGS84, null, null)), TIME);

    /**
     * Syst�me de coordonn�es g�ographiques avec un axe temporel.
     * C'est aussi le syst�me utilis� par d�faut pour les {@link ImageTable}s.
     */
    static final CompoundCoordinateSystem WGS84 =
                new CompoundCoordinateSystem("SEAS", GeographicCoordinateSystem.WGS84, TIME);

    /**
     * Construit une table.
     */
    public CoordinateSystemTable() {
    }

    /**
     * Retourne le syst�me de coordonn�es spatio-temporel pour la s�rie sp�cifi�e.
     */
    public CompoundCoordinateSystem getCoordinateSystem(final SeriesEntry series) {
        switch (series.getID()) {
            case 1045068882: return MERCATOR;
            default:         return WGS84;
        }
    }
    
    /**
     * Lib�re les ressources utilis�es par cette table.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public void close() throws SQLException {
    }
}
