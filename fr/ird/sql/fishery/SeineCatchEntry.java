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
package fr.ird.sql.fishery;

// Base de données
import java.sql.ResultSet;
import java.sql.SQLException;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.units.Unit;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;

// Seagis
import fr.ird.animat.Species;


/**
 * Données d'une capture à la palangre. Un objet <code>SeineCatchEntry</code>
 * correspond à une entrée de la table "Captures" de la base de données "Sennes".
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SeineCatchEntry extends AbstractCatchEntry {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 6792710597884632992L;

    /**
     * Tableau vide d'espèces (utilisée lorsqu'il n'y a eu aucune calée).
     */
    private static final Species[] EMPTY = new Species[0];

    /**
     * Unités des captures.
     */
    private static final Unit TON = Unit.KILOGRAM.scale(1000);

    /**
     * Date et heure de la capture, en nombre de
     * millisecondes écoulées depuis le 1 janvier 1970.
     */
    private final long date;

    /**
     * Longitude et latitude de la capture, en degrés.
     */
    private final float x,y;

    /**
     * Construit un enregistrement représentant une capture à la senne.
     *
     * @param  table  Table d'où proviennent les données.
     * @param  result Résultat de la requête SQL.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public SeineCatchEntry(final SeineCatchTable table, final ResultSet result) throws SQLException {
        super(result.getInt(SeineCatchTable.ID),
              result.getInt(SeineCatchTable.CALEES)!=0 ? table.species.species : EMPTY);

        date = table.getTimestamp(SeineCatchTable.DATE, result).getTime();
        x    = getFloat(result,   SeineCatchTable.LONGITUDE);
        y    = getFloat(result,   SeineCatchTable.LATITUDE );

        for (int i=0; i<amount.length; i++) {
            amount[i] = getFloat(result, SeineCatchTable.CATCH_AMOUNT + i);
        }
    }

    /**
     * Retourne le nombre réel de la colonne spécifié, ou
     * <code>NaN</code> si ce nombre réel n'est pas spécifié.
     */
    private static float getFloat(final ResultSet result, final int column) throws SQLException {
        final float value = result.getFloat(column);
        return result.wasNull() ? Float.NaN : value;
    }

    /**
     * Ramène toujours la position à {@link EnvironmentTable#CENTER},
     * étant donné que la base de données des senneurs ne contient pas
     * de coordonnées de début et de fin.
     */
    final int clampPosition(final int pos) {
        if (pos<EnvironmentTable.START_POINT || pos>EnvironmentTable.END_POINT) {
            return pos;
        }
        return EnvironmentTable.CENTER;
    }

    /**
     * Retourne une coordonnée représentative de la
     * capture, en degrés de longitude et latitude.
     */
    public Point2D getCoordinate() {
        return new Point2D.Float(x,y);
    }

    /**
     * Retourne une forme représentant le filage. Si les informations
     * disponibles ne permettent pas de connaître le filage, retourne
     * <code>null</code>.
     */
    public Shape getShape() {
        return null;
    }

    /**
     * Verifie si cette capture intercepte le rectangle spécifié.
     * Cette méthode vérifie simplement si la coordonnées de la
     * capture se trouve à l'intérieur du rectangle.
     */
    public boolean intersects(final Rectangle2D rect) {
        return rect.contains(x,y);
    }

    /**
     * Retourne une date représentative de la pêche. Dans le cas des pêches
     * qui s'étendent sur une certaine période de temps, ça pourrait être par
     * exemple la date du milieu.
     */
    public Date getTime() {
        return new Date(date);
    }

    /**
     * Retourne la plage de temps pendant laquelle a été faite la capture.
     * Les éléments de la plage retournée seront du type {@link Date}.
     */
    public Range getTimeRange() {
        final Date date = new Date(this.date);
        return new Range(Date.class, date, date);
    }

    /**
     * Retourne les unités des captures.
     */
    public Unit getUnit() {
        return TON;
    }

    /**
     * Vérifie si cette capture est
     * identique à l'objet spécifié.
     */
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            final SeineCatchEntry that = (SeineCatchEntry) other;
            return this.date == that.date &&
                   Float.floatToIntBits(this.x) == Float.floatToIntBits(that.x) &&
                   Float.floatToIntBits(this.y) == Float.floatToIntBits(that.y);
        }
        return false;
    }
}
