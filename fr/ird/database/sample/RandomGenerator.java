/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.*;
import javax.media.jai.util.Range;

import fr.ird.database.sample.*;


/**
 * G�nr�re des positions al�atoires dans le voisinage de positions existantes.
 *
 * @author Martin Desruisseaux
 * @version $Id$
 */
public class RandomGenerator {
    /**
     * Form� g�om�trique autour de laquelle accepter des positions al�atoires.
     * L'origine de la forme (0,0) correspondra � la position de l'�chantillon.
     */
    private final Shape shape = new Ellipse2D.Double(-5, -5, 10, 10);

    /**
     * Les �chantillons autour desquels g�n�rer des positions al�atoires.
     */
    private Collection<SampleEntry> samples;

    /**
     * Un rectangle englobant les positions de tous les �chantillons,
     * incluant l'extension g�ographique {@link #shape}.
     */
    private Rectangle2D bounds;

    /**
     * Les dates de d�but et de fin de tous les �chantillons {@link #samples}.
     */
    private long startTime, endTime;

    /**
     * G�n�rateur de nombres al�atoires.
     */
    private final Random random = new Random();

    /**
     */
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    /**
     */
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    /**
     * Le flot de sortie.
     */
    private final Writer out;

    /**
     * Construit un nouveau g�n�rateur de position al�atoire.
     */
    public RandomGenerator(final Writer out) {
        this.out = out;
        numberFormat.setGroupingUsed(false);
        numberFormat.setMaximumFractionDigits(15);
    }

    /**
     */
    public void setSamples(final Collection<SampleEntry> samples) {
        this.bounds    = null;
        this.samples   = samples;
        this.startTime = Long.MAX_VALUE;
        this.  endTime = Long.MIN_VALUE;
        final Rectangle2D shapeBounds = shape.getBounds2D();
        final double             xmin = shapeBounds.getMinX();
        final double             ymin = shapeBounds.getMinY();
        final Rectangle2D.Double rect = new Rectangle2D.Double(0,0,
                                            shapeBounds.getWidth(), shapeBounds.getHeight());
        boolean init = false;
        for (final SampleEntry sample : samples) {
            final Point2D coord = sample.getCoordinate();
            rect.x = coord.getX();
            rect.y = coord.getY();
            if (bounds == null) {
                bounds = shapeBounds;
                bounds.setRect(rect);
            } else {
                bounds.add(rect);
            }
            long time;
            final Range range = sample.getTimeRange();
            if ((time=((Date)range.getMinValue()).getTime()) < startTime) startTime=time;
            if ((time=((Date)range.getMaxValue()).getTime()) >   endTime)   endTime=time;
        }
    }

    /**
     */
    public void createRandomSamples(int numToCreate) throws IOException {
        if (samples.isEmpty()) {
            return;
        }
        int count = 0;
        while (numToCreate > 0) {
            final long time = Math.round(random.nextDouble()*(endTime-startTime)) + startTime;
            final double x = random.nextDouble()*bounds.getWidth()  + bounds.getX();
            final double y = random.nextDouble()*bounds.getHeight() + bounds.getY();
            for (final SampleEntry sample : samples) {
                final Point2D coord = sample.getCoordinate();
                if (shape.contains(x-coord.getX(), y-coord.getY())) {
                    coord.setLocation(x,y);
                    addEntry(new Date(time), coord);
                    numToCreate--;
                    break;
                }
            }        
        }
    }

    /**
     */
    public void createRandomSamples(final SampleTable samples,
                                    final long window,
                                    final long step,
                                    final int  numToCreate)
            throws SQLException, IOException
    {
        setSamples(samples.getEntries());
        long startTime = this.startTime;
        final long finishTime = endTime;

//        final Range timeRange = samples.getTimeRange();
//        final long finishTime = ((Date)timeRange.getMaxValue()).getTime();
//        long startTime = ((Date)timeRange.getMinValue()).getTime();

        long endTime;
        while ((endTime=startTime+window) <= finishTime) {
            samples.setTimeRange(new Range(Date.class, new Date(startTime), true,
                                                       new Date( endTime), false));
            setSamples(samples.getEntries());
            createRandomSamples(numToCreate);
            startTime += step;
        }
//        samples.setTimeRange(timeRange);
    }

    /**
     */
    protected void addEntry(final Date date, final Point2D coord) throws IOException {
        out.write(dateFormat.format(date));
        out.write('\t');
        out.write(numberFormat.format(coord.getX()));
        out.write('\t');
        out.write(numberFormat.format(coord.getY()));
        out.write(System.getProperty("line.separator", "\r\n"));
    }

    /**
     * Lance la g�n�ration � partir de la ligne de commande.
     */
    public static void main(String[] args) throws SQLException, IOException {
        final long window = 7*24*60*60*1000L;
        final long step   = 1*24*60*60*1000L;
        final Writer out  = new FileWriter("random.txt");
        final RandomGenerator generator = new RandomGenerator(out);
        final SampleDataBase  database  = new fr.ird.database.sample.sql.SampleDataBase();
        try {
            final SampleTable samples = database.getSampleTable();
            generator.createRandomSamples(samples, window, step, 8);
            samples.close();
            out.flush();
        } finally {
            database.close();
            out.close();
        }
    }
}
