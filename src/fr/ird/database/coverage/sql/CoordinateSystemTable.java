/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.database.coverage.sql;

// J2SE
import java.util.Date;
import java.sql.Connection;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Geotools
import org.geotools.units.Unit;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.Projection;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.ProjectedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;

/**
 * Table des systèmes de coordonnées utilisés par les images.
 *
 * NOTE: L'implémentation actuelle code "en dur" les systèmes de coordonnées à utiliser pour
 *       différentes séries. Une version future devra aller chercher cette information dans
 *       la base de données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class CoordinateSystemTable extends Table {
    /**
     * Nombre de millisecondes entre le 01/01/1970 00:00 UTC et le 01/01/1950 00:00 UTC.
     * Le 1er janvier 1970 est l'epoch du Java, tandis que le 1er janvier 1950 est celui
     * de la Nasa (son jour julier "0"). La constante <code>EPOCH</code> sert à faire les
     * conversions d'un système à l'autre.
     */
    private static final long EPOCH = -631152000000L; // Pour 1958, utiliser -378691200000L;

    /**
     * Nombre de millisecondes dans une journée.
     */
    static final double DAY = 24*60*60*1000;

    /**
     * Système de coordonnées temporelles d'AVISO.
     */
    private static final TemporalCoordinateSystem TIME = new TemporalCoordinateSystem("Aviso", new Date(EPOCH));

    /**
     * L'ellipsoïde utilisé par AVISO. Très proche de WGS84.
     */
    private static final Ellipsoid ELLIPSOID =
                Ellipsoid.createFlattenedSphere("AVISO", 6378136.3, 298.257, Unit.METRE);
    
    /**
     * Système de coordonnées utilisées par les images AVISO.
     * Note: En principe, l'ellipsoïde devrait être celui d'AVISO. Mais la différence avec
     *       WGS84 étant faible, nous l'ignorons pour l'instant.
     */
    private static final CompoundCoordinateSystem MERCATOR =
                new CompoundCoordinateSystem("SEAS Mercator",
                new ProjectedCoordinateSystem("Mercator", GeographicCoordinateSystem.WGS84,
                new Projection("Mercator", "Mercator_1SP", Ellipsoid.WGS84, null, null)), TIME);

    /**
     * Système de coordonnées géographiques avec un axe temporel.
     * C'est aussi le système utilisé par défaut pour les {@link GridCoverageTable}s.
     */
    static final CompoundCoordinateSystem WGS84 =
                new CompoundCoordinateSystem("SEAS", GeographicCoordinateSystem.WGS84, TIME);

    /**
     * Le système de coordonnées géographiques dans la base de données.
     *
     * @task TODO: NE PAS CODER CETTE INFORMATION EN DUR!!
     *             Puiser cette information dans la base de données plutôt.
     */
    private static final int WGS84_ID = 1001462621;

    /**
     * La projection de Mercator dans la base de données.
     *
     * @task TODO: NE PAS CODER CETTE INFORMATION EN DUR!!
     *             Puiser cette information dans la base de données plutôt.
     */
    private static final int MERCATOR_ID = -187629553;

    /**
     * Convertit un jour julien en date.
     */
    static Date toDate(final double t) {
        return new Date(Math.round(t*DAY)+EPOCH);
    }

    /**
     * Convertit une date en nombre de jours écoulés depuis le 1er janvier 1950.
     * Les valeurs <code>[MIN/MAX]_VALUE</code> sont converties en infinies.
     */
    static double toJulian(final long time) {
        if (time==Long.MIN_VALUE) return Double.NEGATIVE_INFINITY;
        if (time==Long.MAX_VALUE) return Double.POSITIVE_INFINITY;
        return (time-EPOCH)/DAY;
    }

    /**
     * Construit une table.
     */
    public CoordinateSystemTable(final Connection connection) throws RemoteException {
        // TODO
    }

    /**
     * Retourne le système de coordonnées spatio-temporel pour la série spécifiée.
     *
     * @throws SQLException si l'interrogation de la base de données a échouée.
     *
     * @task TODO: Puiser les informations dans la bases de données plutôt que de les coder en dur.
     */
    public CoordinateSystem getCoordinateSystem(final int ID) throws SQLException {
        switch (ID) {
            case WGS84_ID:    return WGS84;
            case MERCATOR_ID: return MERCATOR;
            default: throw new SQLException("Projection non supportée (pour l'instant).");
        }
    }

    /**
     * Retourne le numéro identifiant le système de coordonnées spécifié.
     *
     * @throws SQLException si l'interrogation de la base de données a échouée.
     *
     * @task TODO: Puiser les informations dans la bases de données plutôt que de les coder en dur.
     */
    public int getID(final CoordinateSystem cs) throws SQLException {
        if (WGS84.equals(cs, false)) {
            return WGS84_ID;
        }
        if (MERCATOR.equals(cs, false)) {
            return MERCATOR_ID;
        }
        throw new SQLException("Projection non supportée (pour l'instant).");
    }

    /**
     * Libère les ressources utilisées par cette table.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws RemoteException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public void close() throws RemoteException {        
    }
}
