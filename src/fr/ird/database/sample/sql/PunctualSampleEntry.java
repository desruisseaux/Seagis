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
package fr.ird.database.sample.sql;

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
 * Echantillon pris en un seul point.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class PunctualSampleEntry extends SampleEntry {
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
     * Date et heure de l'échantillon, en nombre de
     * millisecondes écoulées depuis le 1 janvier 1970.
     */
    private final long date;

    /**
     * Longitude et latitude de l'échantillon, en degrés.
     */
    private final float x,y;

    /**
     * Construit un enregistrement représentant un échantillon en un point.
     *
     * @param  table  Table d'où proviennent les données.
     * @param  result Résultat de la requête SQL.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public PunctualSampleEntry(final PunctualSampleTable table, final ResultSet result) throws SQLException {
        super(result.getInt(PunctualSampleTable.ID),
              table.getCruise(result.getInt(PunctualSampleTable.CRUISE)),
              result.getInt(PunctualSampleTable.CALEES)!=0 ? table.species.species : EMPTY);

        date = table.getTimestamp(PunctualSampleTable.DATE, result).getTime();
        x    = getFloat(result,   PunctualSampleTable.LONGITUDE);
        y    = getFloat(result,   PunctualSampleTable.LATITUDE );

        for (int i=0; i<amount.length; i++) {
            amount[i] = getFloat(result, PunctualSampleTable.SAMPLE_VALUE + i);
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
     * {@inheritDoc}
     */
    public Point2D getCoordinate() {
        return new Point2D.Float(x,y);
    }

    /**
     * Retourne toujours <code>null</code>, puisque cet
     * échantillon n'est pris qu'en un seul point.
     */
    public Shape getShape() {
        return null;
    }

    /**
     * Verifie si cet échantillon intercepte le rectangle spécifié.
     * Cette méthode vérifie simplement si la coordonnées de la
     * capture se trouve à l'intérieur du rectangle.
     */
    public boolean intersects(final Rectangle2D rect) {
        return rect.contains(x,y);
    }

    /**
     * {@inheritDoc}
     */
    public Date getTime() {
        return new Date(date);
    }

    /**
     * {@inheritDoc}
     */
    public Range getTimeRange() {
        final Date date = new Date(this.date);
        return new Range(Date.class, date, date);
    }

    /**
     * {@inheritDoc}
     */
    public Unit getUnit() {
        return TON;
    }

    /**
     * Vérifie si cet échantillon est identique à l'objet spécifié.
     */
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            final PunctualSampleEntry that = (PunctualSampleEntry) other;
            return this.date == that.date &&
                   Float.floatToIntBits(this.x) == Float.floatToIntBits(that.x) &&
                   Float.floatToIntBits(this.y) == Float.floatToIntBits(that.y);
        }
        return false;
    }
}
