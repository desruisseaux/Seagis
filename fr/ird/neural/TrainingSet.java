/*
 * Remote sensing images: database, visualisation and simulations
 * Copyright (C) 1999 by Joseph A. Huwaldt <jhuwaldt@gte.net>.
 *               2001 Institut de Recherche pour le Développement
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
 */
package fr.ird.neural;


/**
 * A set of inputs and outputs values used for training a network.
 * <code>TrainingSet</code> objects are used to train a neural network
 * that requires supervised training (such as a feed forward network).
 * Initially the cursor is positioned before the first training instance
 * (combination of inputs and the associated "correct" or target outputs).
 * The {@link #next} method moves the cursor to the next training instance,
 * and because it returns <code>false</code> when there are no more instances
 * in the <code>TrainingSet</code> object, it can be used in a <code>while</code>
 * loop to iterate through the training set:
 *
 * <blockquote><pre>
 * double[] inputs  = null;
 * double[] outputs = null;
 * while (tr.next())
 * {
 *     inputs  = tr.getTestInputs (inputs);
 *     outputs = tr.getTestOutputs(outputs);
 * }
 * </pre></blockquote>
 *
 * @version $Id$
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
public interface TrainingSet
{
    /**
     * Instructs the training set to create or move on to
     * a new training instance (combination of inputs and
     * the associated "correct" or target outputs).
     *
     * @return <code>true</code> if the new current training instance is
     *         valid; <code>false</code> if there are no more instances.
     */
    public abstract boolean next();

    /**
     * Returns a vector of the values associated with each input node
     * of a neural network for the current training instance.
     *
     * @param  dest A destination array, or <code>null</code> to create a
     *         new one. If non-null, values in this array will be overrided.
     * @return The current set of input values.
     */
    public abstract double[] getTestInputs(double[] dest);

    /**
     * Returns a vector of the values associated with each output node
     * of a neural network for the current training instance. These are
     * the correct values that the network will be trained to produce
     * using the inputs returned from {@link #getTestInputs}.
     *
     * @param  dest A destination array, or <code>null</code> to create a
     *         new one. If non-null, values in this array will be overrided.
     * @return The current set of output values.
     */
    public abstract double[] getTestOutputs(double[] dest);

    /**
     * Move the cursor before the first training instance.
     */
    public abstract void rewind();

    /**
     * Randomly permute instances in this training set.
     * This is an optional operation (some backing store
     * may be difficult to permute).
     */
    public abstract void shuffle();
}
