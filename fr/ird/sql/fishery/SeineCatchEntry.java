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
package fr.ird.sql.fishery;

// Base de donn�es
import java.sql.ResultSet;
import java.sql.SQLException;

// Coordonn�es spatio-temporelles
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
 * Donn�es d'une capture � la palangre. Un objet <code>SeineCatchEntry</code>
 * correspond � une entr�e de la table "Captures" de la base de donn�es "Sennes".
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SeineCatchEntry extends AbstractCatchEntry {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 6792710597884632992L;

    /**
     * Tableau vide d'esp�ces (utilis�e lorsqu'il n'y a eu aucune cal�e).
     */
    private static final Species[] EMPTY = new Species[0];

    /**
     * Unit�s des captures.
     */
    private static final Unit TON = Unit.KILOGRAM.scale(1000);

    /**
     * Date et heure de la capture, en nombre de
     * millisecondes �coul�es depuis le 1 janvier 1970.
     */
    private final long date;

    /**
     * Longitude et latitude de la capture, en degr�s.
     */
    private final float x,y;

    /**
     * Construit un enregistrement repr�sentant une capture � la senne.
     *
     * @param  table  Table d'o� proviennent les donn�es.
     * @param  result R�sultat de la requ�te SQL.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
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
     * Retourne le nombre r�el de la colonne sp�cifi�, ou
     * <code>NaN</code> si ce nombre r�el n'est pas sp�cifi�.
     */
    private static float getFloat(final ResultSet result, final int column) throws SQLException {
        final float value = result.getFloat(column);
        return result.wasNull() ? Float.NaN : value;
    }

    /**
     * Ram�ne toujours la position � {@link EnvironmentTable#CENTER},
     * �tant donn� que la base de donn�es des senneurs ne contient pas
     * de coordonn�es de d�but et de fin.
     */
    final int clampPosition(final int pos) {
        if (pos<EnvironmentTable.START_POINT || pos>EnvironmentTable.END_POINT) {
            return pos;
        }
        return EnvironmentTable.CENTER;
    }

    /**
     * Retourne une coordonn�e repr�sentative de la
     * capture, en degr�s de longitude et latitude.
     */
    public Point2D getCoordinate() {
        return new Point2D.Float(x,y);
    }

    /**
     * Retourne une forme repr�sentant le filage. Si les informations
     * disponibles ne permettent pas de conna�tre le filage, retourne
     * <code>null</code>.
     */
    public Shape getShape() {
        return null;
    }

    /**
     * Verifie si cette capture intercepte le rectangle sp�cifi�.
     * Cette m�thode v�rifie simplement si la coordonn�es de la
     * capture se trouve � l'int�rieur du rectangle.
     */
    public boolean intersects(final Rectangle2D rect) {
        return rect.contains(x,y);
    }

    /**
     * Retourne une date repr�sentative de la p�che. Dans le cas des p�ches
     * qui s'�tendent sur une certaine p�riode de temps, �a pourrait �tre par
     * exemple la date du milieu.
     */
    public Date getTime() {
        return new Date(date);
    }

    /**
     * Retourne la plage de temps pendant laquelle a �t� faite la capture.
     * Les �l�ments de la plage retourn�e seront du type {@link Date}.
     */
    public Range getTimeRange() {
        final Date date = new Date(this.date);
        return new Range(Date.class, date, date);
    }

    /**
     * Retourne les unit�s des captures.
     */
    public Unit getUnit() {
        return TON;
    }

    /**
     * V�rifie si cette capture est
     * identique � l'objet sp�cifi�.
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
