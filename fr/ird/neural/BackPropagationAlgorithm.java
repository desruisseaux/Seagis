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
 * The standard back propagation learning (without momentum).
 * This training method is not robust and is famous for it's
 * sluggishness, but it is also simple and easy to understand.
 * That is why it is included here.
 *
 * @version 1.0
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
final class BackPropagationAlgorithm extends TrainingAlgorithm
{
    /**
     * Serial number for compatibility with previous versions.
     */
    //private static final long serialVersionUID = ?; // TODO

    /**
     * The learning factor (between 0 and 1) used for network training.
     * For best results, the learning factor should change with increasing
     * iterations. The {@link #train} routines below make not attempt to do
     * this as there is no general rule for how it should be done.
     */
    private final double learningFactor = 0.3;

    /**
     * Construct a back propagation algorithm
     * with a default learning factor.
     */
    public BackPropagationAlgorithm()
    {}

    /**
     * Adjust the strengths of the connections (weights) between
     * the neurons in this network to reduce errors at the output
     * nodes using the method of back propagation without momentum.
     * This is called by the {@link #train} method.
     *
     * @param neurons Neural network to have the input weights adjusted for.
     */
    protected void adjustWeights(final Neuron[][] neurons)
    {
        for (int j=neurons.length; --j>=0;)
        {
            final Neuron[] layer=neurons[j];
            for (int i=layer.length; --i>=0;)
            {
                adjustWeights(layer[i]);
            }
        }
    }

    /**
     * Adjust the strengths of the connections (weights) between
     * the neurons in the network to reduce errors at the output
     * nodes using the method of back propagation without momentum.
     * This is called by the {@link #train} method.
     *
     * @param neuron Neuron to have the input weights adjusted for.
     */
    private void adjustWeights(final Neuron neuron)
    {
        final Neuron[] inputNeurons = neuron.inputs;
        if (inputNeurons != null)
        {
            // Determine how much we should adjust the input weights:
            // Delta = error * dr/dq
            final double   error = neuron.error;
            double delta = error * neuron.gradient;

            // If the node is WAY off, then try something drastic (this is an "engineering" solution).
            if (Math.abs(error)>0.9 && Math.abs(delta)<0.1)
            {
                delta = error * 0.1;
            }
            // Loop over all the input nodes adjusting the weights
            // to them proportional to their contribution.
            for (int i=inputNeurons.length; --i>=0;)
            {
                final double input = inputNeurons[i].output;
                neuron.weights[i]     += learningFactor * delta * input;
                inputNeurons[i].error += delta * neuron.weights[i];
            }
        }
    }
}
