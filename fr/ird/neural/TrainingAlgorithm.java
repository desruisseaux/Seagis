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

// Miscellaneous
import java.io.Serializable;
import fr.ird.util.XArray;


/**
 * Base class for algorithms for training a neural network.
 *
 * @version 1.0
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
public abstract class TrainingAlgorithm implements Serializable
{
    /**
     * Serial number for compatibility with previous versions.
     */
    //private static final long serialVersionUID = ?; // TODO

    /**
     * Tolerance to train network to (mean error across
     * all the outputs accross a training set).
     */
    private double tolerance = 0;

    /**
     * The maximum amount of time (in millisecond) allowed for training.
     */
    private long maxTime = 4000;

    /**
     * Construct a train algorithm with default parameters.
     */
    public TrainingAlgorithm()
    {}

    /**
     * Returns the tolerance to train network to (RMS error
     * across all the outputs accross a training set).
     */
    public synchronized double getTolerance()
    {return tolerance;}

    /**
     * Set the tolerance to train network to (RMS error
     * across all the outputs accross a training set).
     */
    public synchronized void setTolerance(final double tolerance)
    {this.tolerance = Math.abs(tolerance);}

    /**
     * The maximum amount of time allowed for training.
     * @return The maximum amount of time in millisecond.
     */
    public synchronized long getMaxTime()
    {return maxTime;}

    /**
     * Set the maximum amount of time allowed for training.
     * @param maxTime The maximum amount of time in millisecond.
     */
    public synchronized void setMaxTime(final long maxTime)
    {this.maxTime = Math.abs(maxTime);}

    /**
     * Train a network to match a set of input patterns to a set of
     * output patterns. Trains until a certain tolerance or maximum
     * time elapsed is reached. The default implementation iterate
     * through the training set as many time as necessary and invoke
     * {@link #adjustWeights} for each training instance.
     *
     * @param  network The network to train.
     * @param  testGenerator Training instance generator that
     *         generates combinations of inputs and corresponding
     *         outputs to train the network to reproduce.
     * @return An history of RMS errors for each iteration.
     */
    public float[] train(final FeedForwardNet network, final TrainingSet testGenerator)
    {
        // Gets parameters.
        final long  startTime = System.currentTimeMillis();
        final long     maxTime;
        final double tolerance;
        synchronized (this)
        {
            maxTime   = getMaxTime();
            tolerance = getTolerance();
        }
        // Loop over all the training iterations, using a new training instance for each.
        float[]  history = new float[128];
        int iteration    = 0; // Number of iteration done so far.
        int historyCount = 0; // Number of valid values in 'history'.
        int historyDecim = 0; // Decimation to apply on history (will be computed later).
        long checkTime   = maxTime/history.length;
        long ellapsedTime;
        history[0] = (float)network.getError(testGenerator);
        do
        {
            testGenerator.rewind();
            while (testGenerator.next())
            {
                testGenerator.shuffle();
                network.initialize(testGenerator);
                adjustWeights(network.neurons);
            }
            // Determine the RMS error of the network after training.
            final double error = network.getError(testGenerator);
            if (error < tolerance)
            {
                // TODO: Make a last and unconditional update to the error history.
                break;
            }
            ellapsedTime = System.currentTimeMillis()-startTime;
            /*
             * Update error history. We will not record error for all iterations,
             * because there is way to much of them. Instead, we will wait for a
             * while and check how many iterations  we have been able to do in a
             * time slice. Then, we will record only one error after this number
             * of iterations (more specifically, we will record the maximum error).
             */
            if (historyCount >= history.length)
            {
                history = XArray.resize(history, historyCount*2);
                // Extra elements are initialized to 0.
            }
            if ((float)error > history[historyCount])
            {
                history[historyCount] = (float) error;
            }
            if (historyDecim!=0)
            {
                if (iteration % historyDecim == 0)
                    historyCount++;
            }
            else if (ellapsedTime > checkTime)
            {
                historyDecim = Math.max(1, iteration);
            }
            iteration++;
        }
        while (ellapsedTime <= maxTime);
        return XArray.resize(history, historyCount);
    }

    /**
     * Adjust the strengths of the connections (weights) between
     * the neurons in the network to reduce errors at the output
     * nodes. This is called by the {@link #train} method.
     *
     * @param neurons Neural network to have the input weights adjusted for.
     */
    protected abstract void adjustWeights(final Neuron[][] neurons);
}
