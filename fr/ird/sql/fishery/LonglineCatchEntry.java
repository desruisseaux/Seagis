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
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.text.AngleFormat;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.GeographicCoordinateSystem;

// Divers
import javax.units.Unit;
import java.text.DateFormat;
import java.text.FieldPosition;
import javax.media.jai.util.Range;

// Divers
import fr.ird.animat.Species;
import fr.ird.resources.Resources;
import fr.ird.resources.Clé;


/**
 * Données d'une capture à la palangre. Un objet <code>PalangreCatch</code>
 * correspond à une entrée de la table "Fisheries" de la base de données des
 * pêches.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class LonglineCatchEntry extends AbstractCatchEntry
{
    /**
     * Objet à utiliser par défaut pour les écritures des dates.
     */
    private static DateFormat dateFormat;

    /**
     * Objet à utiliser par défaut pour les écritures des coordonnées.
     */
    private static AngleFormat angleFormat;

    /**
     * Date et heure de la capture, en nombre de
     * millisecondes écoulées depuis le 1 janvier 1970.
     */
    private final long date;

    /**
     * Longitude et latitude du début
     * de la capture, en degrés.
     */
    private final float x1,y1;

    /**
     * Longitude et latitude de la fin
     * de la capture, en degrés.
     */
    private final float x2,y2;

    /**
     * Construit un enregistrement représentant une capture à la palangre.
     *
     * @param  table  Table d'où proviennent les données.
     * @param  result Résultat de la requête SQL.
     * @throws SQLException si l'interrogation de la base de données a échoué.
     */
    public LonglineCatchEntry(final LonglineCatchTable table, final ResultSet result) throws SQLException
    {
        super(result.getInt(LonglineCatchTable.ID), table.species);
        final float efu;

        date = table.getTimestamp(LonglineCatchTable.DATE, result).getTime();
        x1   = getFloat(result,   LonglineCatchTable.START_LONGITUDE);
        y1   = getFloat(result,   LonglineCatchTable.START_LATITUDE );
        x2   = getFloat(result,   LonglineCatchTable.END_LONGITUDE  );
        y2   = getFloat(result,   LonglineCatchTable.END_LATITUDE   );
        efu  = getFloat(result,   LonglineCatchTable.EFFORT_UNIT    )/1000;

        for (int i=0; i<amount.length; i++)
        {
            amount[i] = getFloat(result, LonglineCatchTable.CATCHS + i)/efu;
        }
    }

    /**
     * Retourne le nombre réel de la colonne spécifié, ou
     * <code>NaN</code> si ce nombre réel n'est pas spécifié.
     */
    private static float getFloat(final ResultSet result, final int column) throws SQLException
    {
        final float value = result.getFloat(column);
        return result.wasNull() ? Float.NaN : value;
    }

    /**
     * Si cette capture ne contient pas les coordonnées de début
     * et de fin de la ligne, ramène la position spécifiée à une
     * des valeurs {@link EnvironmentTable#START_POINT} ou
     * {@link EnvironmentTable#END_POINT} en fonction de la
     * coordonnée disponible.
     */
    final int clampPosition(final int pos)
    {
        if (pos<EnvironmentTable.START_POINT || pos>EnvironmentTable.END_POINT)
        {
            return pos;
        }
        int missing = 0;
        if (Float.isNaN(x1) || Float.isNaN(y1)) missing |= 1;
        if (Float.isNaN(x2) || Float.isNaN(y2)) missing |= 2;
        switch (missing)
        {
            case 0:  return pos;
            case 1:  return EnvironmentTable.END_POINT;
            case 2:  return EnvironmentTable.START_POINT;
            case 3:  return EnvironmentTable.CENTER;
            default: throw new AssertionError(missing);
        }
    }

    /**
     * Retourne la moyenne des deux nombres spécifiés. Si un des deux nombres
     * est NaN, l'autre sera retourné. Si les deux nombres sont NaN, alors NaN
     * sera retourné.
     */
    private static float mean(final float x1, final float x2)
    {
        if (Float.isNaN(x1)) return x2;
        if (Float.isNaN(x2)) return x1;
        return (x1+x2)*0.5f;
    }

    /**
     * Retourne une coordonnée représentative de la
     * capture, en degrés de longitude et latitude.
     */
    public Point2D getCoordinate()
    {return new Point2D.Float(mean(x1,x2), mean(y1,y2));}

    /**
     * Retourne une forme représentant le filage. Si les informations
     * disponibles ne permettent pas de connaître le filage, retourne
     * <code>null</code>.
     */
    public Shape getShape()
    {
        if (Float.isNaN(x1) || Float.isNaN(x2) ||
            Float.isNaN(y1) || Float.isNaN(y2) ||
            (x1==x2 && y1==y2)) return null;
        return new Line2D.Float(x1,y1, x2,y2);
    }

    /**
     * Retourne une date représentative de la pêche. Dans le cas des pêches
     * qui s'étendent sur une certaine période de temps, ça pourrait être par
     * exemple la date du milieu.
     */
    public Date getTime()
    {return new Date(date);}

    /**
     * Retourne la plage de temps pendant laquelle a été faite la capture.
     * Les éléments de la plage retournée seront du type {@link Date}.
     */
    public Range getTimeRange()
    {
        final Date date = new Date(this.date);
        return new Range(Date.class, date, date);
    }

    /**
     * Retourne les unités des captures.
     */
    public Unit getUnit()
    {return null;} // TODO: dimensionless

    /**
     * Retourne une chaîne de caractères
     * représentant cette capture.
     */
    public String toString()
    {
        if (dateFormat ==null)  dateFormat=DateFormat.getDateInstance();
        if (angleFormat==null) angleFormat=new AngleFormat();
        final FieldPosition dummy=new FieldPosition(0);
        final StringBuffer buffer=new StringBuffer("CatchEntry[");
        dateFormat .format(new Date     (date),        buffer, dummy); buffer.append(", ");
        angleFormat.format(new Latitude (mean(y1,y2)), buffer, dummy); buffer.append(' ');
        angleFormat.format(new Longitude(mean(x1,x2)), buffer, dummy); buffer.append(']');
        return buffer.toString();
    }

    /**
     * Vérifie si cette capture est
     * identique à l'objet spécifié.
     */
    public boolean equals(final Object other)
    {
        if (other instanceof LonglineCatchEntry)
        {
            final LonglineCatchEntry that = (LonglineCatchEntry) other;
            return this.ID   == that.ID   &&
                   this.date == that.date &&
                   Float.floatToIntBits(this.x1) == Float.floatToIntBits(that.x1) &&
                   Float.floatToIntBits(this.y1) == Float.floatToIntBits(that.y1) &&
                   Float.floatToIntBits(this.x2) == Float.floatToIntBits(that.x2) &&
                   Float.floatToIntBits(this.y2) == Float.floatToIntBits(that.y2);
        }
        else return false;
    }
}
