/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.operator.coverage;

// Divers
import java.awt.geom.Point2D;
import org.geotools.gc.GridCoverage;
import org.geotools.resources.Utilities;


/**
 * Une des valeur retournées par un objet {@link Evaluator]. Cet objet
 * peut aussi mémoriser la coordonnées du pixel évalué, si la l'évaluation
 * s'est portée sur un seul pixel.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class ParameterValue
{
    /**
     * Constructeur par défaut.
     */
    protected void ParameterValue()
    {
    }

    /**
     * Retourne le nom de ce paramètre.
     */
    public abstract String getName();

    /**
     * Retourne la valeur évaluée.
     */
    public abstract double getValue();

    /**
     * Retourne la position de la valeur évaluée, ou <code>null</code>
     * si la valeur n'a pas été évaluée a une position en particulier.
     */
    public abstract Point2D getLocation();

    /**
     * Définie la valeur et sa position.
     *
     * @param value Nouvelle valeur
     * @param position Position, ou <code>null</code> si elle n'est pas définie.
     *        Dans ce dernier cas, l'ancienne position ne sera pas modifiée.
     */
    public abstract void setValue(final double value, final Point2D position);

    /**
     * Implémentation de {@link ParameterValue} pour
     * des nombres de type {@link java.lang.Double}.
     */
    public static final class Double extends ParameterValue
    {
        /**
         * Le nom des données.
         */
        private final String coverage;

        /**
         * Le nom de l'objet ayant calculé les valeurs.
         */
        private final String evaluator;

        /**
         * La valeur évaluée.
         */
        private double value = java.lang.Double.NaN;

        /**
         * Coordonnées géographiques (<var>x</var>,<var>y</var>) du point évalué,
         * ou {@link java.lang.Double#NaN} si l'évaluation ne s'est pas faite en
         * un point en particulier.
         */
        private double x=java.lang.Double.NaN, y=java.lang.Double.NaN;

        /**
         * Construit un paramètre du nom spécifié.
         */
        public Double(final String name)
        {
            evaluator = name;
            coverage  = null;
        }

        /**
         * Construit un paramètre pour l'image spécifiée.
         */
        Double(final GridCoverage coverage, final Evaluator evaluator)
        {
            this.coverage  = coverage.getName(null);
            this.evaluator = evaluator.getName();
        }

        /**
         * Retourne le nom de ce paramètre.
         */
        public String getName()
        {
            if (coverage==null)
            {
                return evaluator;
            }
            return evaluator+" of "+coverage;
        }

        /**
         * Retourne la valeur évaluée.
         */
        public double getValue()
        {
            return value;
        }

        /**
         * Retourne la position de la valeur évaluée, ou <code>null</code>
         * si la valeur n'a pas été évaluée a une position en particulier.
         */
        public Point2D getLocation()
        {
            if (java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y))
            {
                return null;
            }
            return new Point2D.Double(x,y);
        }

        /**
         * Définie la valeur et sa position.
         *
         * @param value Nouvelle valeur
         * @param position Position, ou <code>null</code> si elle n'est pas définie.
         *        Dans ce dernier cas, l'ancienne position ne sera pas modifiée.
         */
        public void setValue(final double value, final Point2D position)
        {
            this.value = value;
            if (position != null)
            {
                this.x = position.getX();
                this.y = position.getY();
            }
        }
    }

    /**
     * Implémentation de {@link ParameterValue} pour
     * des nombres de type {@link java.lang.Float}.
     */
    public static final class Float extends ParameterValue
    {
        /**
         * Le nom des données.
         */
        private final String name;

        /**
         * La valeur évaluée.
         */
        private float value = java.lang.Float.NaN;

        /**
         * Coordonnées géographiques (<var>x</var>,<var>y</var>) du point évalué,
         * ou {@link java.lang.Float#NaN} si l'évaluation ne s'est pas faite en
         * un point en particulier.
         */
        private float x=java.lang.Float.NaN, y=java.lang.Float.NaN;

        /**
         * Construit un paramètre du nom spécifié.
         */
        public Float(final String name)
        {
            this.name = name;
        }

        /**
         * Retourne le nom de ce paramètre.
         */
        public String getName()
        {
            return name;
        }

        /**
         * Retourne la valeur évaluée.
         */
        public double getValue()
        {
            return value;
        }

        /**
         * Retourne la position de la valeur évaluée, ou <code>null</code>
         * si la valeur n'a pas été évaluée a une position en particulier.
         */
        public Point2D getLocation()
        {
            if (java.lang.Float.isNaN(x) || java.lang.Float.isNaN(y))
            {
                return null;
            }
            return new Point2D.Float(x,y);
        }

        /**
         * Définie la valeur et sa position.
         *
         * @param value Nouvelle valeur
         * @param position Position, ou <code>null</code> si elle n'est pas définie.
         *        Dans ce dernier cas, l'ancienne position ne sera pas modifiée.
         */
        public void setValue(final double value, final Point2D position)
        {
            this.value = (float) value;
            if (position != null)
            {
                this.x = (float) position.getX();
                this.y = (float) position.getY();
            }
        }

        /**
         * Définie la valeur et sa position.
         */
        public void setValue(final float value, final float x, final float y)
        {
            this.value = value;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Retourne une représentation textuelle
     * de ce paramètre.
     */
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(getName());
        buffer.append("\" = ");
        buffer.append((float)getValue());
        final Point2D location = getLocation();
        if (location != null)
        {
            buffer.append(" at ");
            buffer.append((float)location.getX());
            buffer.append("; ");
            buffer.append((float)location.getY());
        }
        buffer.append(']');
        return buffer.toString();
    }
}
