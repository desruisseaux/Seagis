/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat;

// J2SE dependencies
import java.io.Serializable;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.resources.Utilities;


/**
 * La valeur d'un {@link Parameter} observ� par un {@link Animal}. En g�n�ral,
 * chaque animal fera plusieurs observations de son environnement pendant ses
 * d�placements. En termes d'objets, l'animal ({@link Animal}) peut obtenir une
 * observation ({@link Observation}) pour chaque param�tre ({@link Parameter})
 * de son environnement ({@link Environment}), et ce � chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Observation implements Cloneable, Serializable {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = 3482078148944452831L;

    /**
     * Le param�tre observ�.
     */
    private final Parameter parameter;

    /**
     * La valeur �valu�e.
     */
    private double value = Double.NaN;

    /**
     * Coordonn�es g�ographiques (<var>x</var>,<var>y</var>) du point �valu�,
     * ou {@link java.lang.Double#NaN} si l'�valuation ne s'est pas faite en
     * un point en particulier.
     */
    private double x=Double.NaN, y=Double.NaN;

    /**
     * Constructeur une nouvelle observation. La valeur ainsi que les
     * coordonn�es sont initialis�e � {@link Double#NaN}. Des valeurs
     * peuvent leur �tre affect�es apr�s la construction en appelant
     * {@link #setValue}.
     *
     * @param parameter Le param�tre observ�, ou <code>null</code> si
     *        ne s'applique pas.
     */
    public Observation(final Parameter parameter) {
        this.parameter = parameter;
    }
        
    /**
     * Retourne le param�tre observ�.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * Retourne la position de la valeur �valu�e, ou <code>null</code>
     * si la valeur n'a pas �t� �valu�e � une position en particulier.
     * Les coordonn�es de ce point doivent �tre en degr�s de longitudes
     * et de latitudes.
     */
    public Point2D getLocation() {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return new Point2D.Double(x,y);
    }

    /**
     * Retourne la valeur �valu�e.
     */
    public double getValue() {
        return value;
    }

    /**
     * D�finit la valeur et sa position.
     *
     * @param value Nouvelle valeur.
     * @param position Position, ou <code>null</code> si elle n'est pas d�finie.
     *        Dans ce dernier cas, l'ancienne position ne sera pas modifi�e.
     */
    public void setValue(final double value, final Point2D position) {
        this.value = value;
        if (position != null) {
            this.x = position.getX();
            this.y = position.getY();
        }
    }

    /**
     * Retourne une repr�sentation textuelle de ce param�tre.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(parameter);
        buffer.append("\" = ");
        buffer.append((float)getValue());
        final Point2D location = getLocation();
        if (location != null) {
            buffer.append(" at ");
            buffer.append((float)location.getX());
            buffer.append("; ");
            buffer.append((float)location.getY());
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Retourne un code pour cette observation. Ce code est
     * bas� sur la valeur et les coordonn�es de l'observation.
     */
    public int hashCode() {
        long code = ((Double.doubleToLongBits(value) * 37) +
                      Double.doubleToLongBits(x)     * 37) +
                      Double.doubleToLongBits(y);
        return (int) code ^ (int) (code >>> 32);
    }

    /**
     * Indique si cette observation est identique � l'objet sp�cifi�.
     */
    public boolean equals(final Object other) {
        if (other!=null && other.getClass().equals(getClass())) {
            final Observation that = (Observation) other;
            return Double.doubleToLongBits(this.value) ==
                   Double.doubleToLongBits(that.value) &&
                   Double.doubleToLongBits(this.x    ) ==
                   Double.doubleToLongBits(that.x    ) &&
                   Double.doubleToLongBits(this.y    ) ==
                   Double.doubleToLongBits(that.y    ) &&
                   Utilities.equals(this.parameter, that.parameter);
        }
        return false;
    }

    /**
     * Retourne une copie de cette observation.
     */
    public Observation clone() {
        try {
            return (Observation) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }
}
