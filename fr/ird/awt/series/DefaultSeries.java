/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2002 Institut de Recherche pour le Développement
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
package fr.ird.awt.series;

// Geometry
import java.awt.Shape;
import java.awt.geom.GeneralPath;

// Vectors
import javax.vecmath.MismatchedSizeException;


/**
 * Default implementation for a series.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class DefaultSeries implements Series
{
    /**
     * Name of this series.
     */
    private String name;

    /**
     * The series as a path. This path
     * use logical coordinates.
     */
    private GeneralPath path;

    /**
     * Construct a new series.
     *
     * @param name Name of this series.
     * @param x    <var>x</var> values for this series.
     * @param y    <var>y</var> values for this series.
     * @throws MismatchedSizeException if arrays doesn't have the same length.
     */
    public DefaultSeries(final String name, final float[] x, final float[] y)
    {
        if (x.length != y.length)
        {
            throw new MismatchedSizeException();
        }
        this.name = name.trim();
        path = new GeneralPath();
        for (int i=0; i<x.length; i++)
        {
            float xi = x[i];
            float yi = y[i];
            if (i==0)
            {
                path.moveTo(xi,yi);
            }
            else
            {
                path.lineTo(xi,yi);
            }
        }
    }

    /**
     * Construct a new series.
     *
     * @param name Name of this series.
     * @param x    <var>x</var> values for this series.
     * @param y    <var>y</var> values for this series.
     * @throws MismatchedSizeException if arrays doesn't have the same length.
     */
    public DefaultSeries(final String name, final double[] x, final double[] y)
    {
        if (x.length != y.length)
        {
            throw new MismatchedSizeException();
        }
        this.name = name.trim();
        path = new GeneralPath();
        for (int i=0; i<x.length; i++)
        {
            float xi = (float) x[i];
            float yi = (float) y[i];
            if (i==0)
            {
                path.moveTo(xi,yi);
            }
            else
            {
                path.lineTo(xi,yi);
            }
        }
    }

    /**
     * Returns the name of this series.
     */
    public String getName()
    {return name;}

    /**
     * Returns the series data as a path.
     * This path use logical coordinates.
     */
    public Shape getPath()
    {
        return path;
    }
    
}
