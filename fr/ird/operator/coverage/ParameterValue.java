/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.operator.coverage;

// Divers
import java.awt.geom.Point2D;
import org.geotools.resources.Utilities;


/**
 * Une des valeur retourn�es par un objet {@link Evaluator]. Cet objet
 * peut aussi m�moriser la coordonn�es du pixel �valu�, si la l'�valuation
 * s'est port�e sur un seul pixel.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class ParameterValue
{
    /**
     * Le nom de ce param�tre.
     */
    private final String name;

    /**
     * La valeur �valu�e.
     */
    private double value;

    /**
     * Coordonn�es g�ographiques (<var>x</var>,<var>y</var>) du point �valu�,
     * ou {@link Double#NaN} si l'�valuation ne s'est pas faite en un point
     * en particulier.
     */
    private double x,y;

    /**
     * Construit un param�tre du nom sp�cifi�.
     */
    public ParameterValue(final String name)
    {
        this.name  = name;
    }

    /**
     * Retourne le nom de ce param�tre.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Retourne la valeur �valu�e.
     */
    public double getValue()
    {
        return value;
    }

    /**
     * Retourne la position de la valeur �valu�e, ou <code>null</code>
     * si la valeur n'a pas �t� �valu�e a une position en particulier.
     */
    public Point2D getLocation()
    {
        if (Double.isNaN(x) || Double.isNaN(y))
        {
            return null;
        }
        return new Point2D.Double(x,y);
    }

    /**
     * D�finie la valeur et sa position.
     */
    final void setValue(final double value, final Point2D position)
    {
        this.value = value;
        this.x = position.getX();
        this.y = position.getY();
    }

    /**
     * Retourne une repr�sentation textuelle
     * de ce param�tre.
     */
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(name);
        buffer.append("\" = ");
        buffer.append((float)value);
        if (!Double.isNaN(x) && !Double.isNaN(y))
        {
            buffer.append(" at ");
            buffer.append((float)x);
            buffer.append("; ");
            buffer.append((float)y);
        }
        buffer.append(']');
        return buffer.toString();
    }
}
