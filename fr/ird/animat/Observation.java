/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat;

// J2SE dependencies
import java.io.Serializable;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.resources.Utilities;


/**
 * La valeur d'un {@link Parameter} observé par un {@link Animal}. En général,
 * chaque animal fera plusieurs observations de son environnement pendant ses
 * déplacements. En termes d'objets, l'animal ({@link Animal}) peut obtenir une
 * observation ({@link Observation}) pour chaque paramètre ({@link Parameter})
 * de son environnement ({@link Environment}), et ce à chaque pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Observation implements Cloneable, Serializable {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = 3482078148944452831L;

    /**
     * Le paramètre observé.
     */
    private final Parameter parameter;

    /**
     * La valeur évaluée.
     */
    private double value = Double.NaN;

    /**
     * Coordonnées géographiques (<var>x</var>,<var>y</var>) du point évalué,
     * ou {@link java.lang.Double#NaN} si l'évaluation ne s'est pas faite en
     * un point en particulier.
     */
    private double x=Double.NaN, y=Double.NaN;

    /**
     * Constructeur une nouvelle observation. La valeur ainsi que les
     * coordonnées sont initialisée à {@link Double#NaN}. Des valeurs
     * peuvent leur être affectées après la construction en appelant
     * {@link #setValue}.
     *
     * @param parameter Le paramètre observé, ou <code>null</code> si
     *        ne s'applique pas.
     */
    public Observation(final Parameter parameter) {
        this.parameter = parameter;
    }
        
    /**
     * Retourne le paramètre observé.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * Retourne la position de la valeur évaluée, ou <code>null</code>
     * si la valeur n'a pas été évaluée à une position en particulier.
     * Les coordonnées de ce point doivent être en degrés de longitudes
     * et de latitudes.
     */
    public Point2D getLocation() {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return new Point2D.Double(x,y);
    }

    /**
     * Retourne la valeur évaluée.
     */
    public double getValue() {
        return value;
    }

    /**
     * Définit la valeur et sa position.
     *
     * @param value Nouvelle valeur.
     * @param position Position, ou <code>null</code> si elle n'est pas définie.
     *        Dans ce dernier cas, l'ancienne position ne sera pas modifiée.
     */
    public void setValue(final double value, final Point2D position) {
        this.value = value;
        if (position != null) {
            this.x = position.getX();
            this.y = position.getY();
        }
    }

    /**
     * Retourne une représentation textuelle de ce paramètre.
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
     * basé sur la valeur et les coordonnées de l'observation.
     */
    public int hashCode() {
        long code = ((Double.doubleToLongBits(value) * 37) +
                      Double.doubleToLongBits(x)     * 37) +
                      Double.doubleToLongBits(y);
        return (int) code ^ (int) (code >>> 32);
    }

    /**
     * Indique si cette observation est identique à l'objet spécifié.
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
