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


/**
 * Un échantillon pris sur une ligne (par exemple une capture à la palangre).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class LinearSampleEntry extends SampleEntry {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -856538436667543534L;

    /**
     * Date et heure de l'échantillon, en nombre de
     * millisecondes écoulées depuis le 1 janvier 1970.
     */
    private final long date;

    /**
     * Longitude et latitude du début de l'échantillon, en degrés.
     */
    private final float x1,y1;

    /**
     * Longitude et latitude de la fin de l'échantillon, en degrés.
     */
    private final float x2,y2;

    /**
     * Construit un enregistrement représentant un échantillon mesurée sur une ligne.
     *
     * @param  table  Table d'où proviennent les données.
     * @param  result Résultat de la requête SQL.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public LinearSampleEntry(final LinearSampleTable table, final ResultSet result) throws SQLException {
        super(result.getInt(LinearSampleTable.ID), table.species.species);
        final float efu;

        date = table.getTimestamp(LinearSampleTable.DATE, result).getTime();
        x1   = getFloat(result,   LinearSampleTable.START_LONGITUDE);
        y1   = getFloat(result,   LinearSampleTable.START_LATITUDE );
        x2   = getFloat(result,   LinearSampleTable.END_LONGITUDE  );
        y2   = getFloat(result,   LinearSampleTable.END_LATITUDE   );
        efu  = getFloat(result,   LinearSampleTable.EFFORT_UNIT    )/1000;

        for (int i=0; i<amount.length; i++) {
            amount[i] = getFloat(result, LinearSampleTable.SAMPLE_VALUE + i)/efu;
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
     * Retourne la moyenne des deux nombres spécifiés. Si un des deux nombres
     * est NaN, l'autre sera retourné. Si les deux nombres sont NaN, alors NaN
     * sera retourné.
     */
    private static float mean(final float x1, final float x2) {
        if (Float.isNaN(x1)) return x2;
        if (Float.isNaN(x2)) return x1;
        return (x1+x2)*0.5f;
    }

    /**
     * {@inheritDoc}
     */
    public Point2D getCoordinate() {
        return new Point2D.Float(mean(x1,x2), mean(y1,y2));
    }

    /**
     * Retourne une forme représentant la ligne. Si les informations
     * disponibles ne permettent pas de connaître la ligne, retourne
     * <code>null</code>.
     */
    public Shape getShape() {
        if (Float.isNaN(x1) || Float.isNaN(x2) ||
            Float.isNaN(y1) || Float.isNaN(y2) ||
            (x1==x2 && y1==y2))
        {
            return null;
        }
        return new Line2D.Float(x1,y1, x2,y2);
    }

    /**
     * Verifie si la ligne intercepte le rectangle spécifié. Cette méthode suppose
     * que la trajectoire de l'échantillonage ne fait pas de crochets.
     */
    public boolean intersects(final Rectangle2D rect) {
        if (Float.isNaN(x1) || Float.isNaN(y1)) return rect.contains(x2, y2);
        if (Float.isNaN(x2) || Float.isNaN(y2)) return rect.contains(x1, y1);
        return rect.intersectsLine(x1, y1, x2, y2);
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
        return null; // TODO: dimensionless
    }

    /**
     * Vérifie si cet échantillon est identique à l'objet spécifié.
     */
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            final LinearSampleEntry that = (LinearSampleEntry) other;
            return this.date == that.date &&
                   Float.floatToIntBits(this.x1) == Float.floatToIntBits(that.x1) &&
                   Float.floatToIntBits(this.y1) == Float.floatToIntBits(that.y1) &&
                   Float.floatToIntBits(this.x2) == Float.floatToIntBits(that.x2) &&
                   Float.floatToIntBits(this.y2) == Float.floatToIntBits(that.y2);
        }
        return false;
    }
}
