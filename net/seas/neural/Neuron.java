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
package net.seas.neural;

// Miscellaneous
import java.util.Random;
import java.io.Serializable;
import net.seas.util.XClass;


/**
 * Base class for all neuron types.
 *
 * @version 1.0
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
public class Neuron implements Serializable
{
    /**
     * A default random number generator.
     */
    private static final Random random = new Random();

    /**
     * List of neurons that input to this one.
     * May be <code>null</code> if this neuron
     * is in the input layer.
     */
    private final Neuron[] inputs;

    /*
     * Weights (strengths) of each link between this neuron and it's inputs.
     * This array length must be equals to <code>inputs</code>'s length. May
     * be <code>null</code> if this neuron is in the input layer.
     */
    private final double[] weights;

    /**
     * The transfert function.
     */
    private TransfertFunction transfert = TransfertFunction.SIGMOID;

    /*
     * Output value of this neuron or node.
     */
    double output;

    /**
     * Construct a default input neuron.
     */
    public Neuron()
    {this(null);}

    /**
     * Construct a neuron connected to the specified input neurons.
     *
     * @param inputs List of neurons that input to this one, or
     *               <code>null</code> if this neuron is an input neuron.
     *               This array is <strong>not</strong> cloned.
     */
    public Neuron(final Neuron[] inputs)
    {
        this.inputs  = inputs;
        this.weights = (inputs!=null) ? new double[inputs.length] : null;
        setRandomWeights();
    }

    /**
     * Set the weights associated with all of the inputs to this
     * neuron to random values with a gaussian distribution
     * between -5 and +5.
     */
    private void setRandomWeights()
    {
        if (weights != null)
        {
            for (int i=0; i<weights .length; i++)
            {
                weights[i] = 5.0 * random.nextGaussian();
            }
        }
    }

    /**
     * Return the number of inputs to this neuron (also the number
     * of weights for this neuron).
     *
     * @return The number of inputs to this neuron.
     */
    public int getNumInputs()
    {return (inputs!=null) ? inputs.length : 0;}

    /**
     * Calculates the value of this neuron based on a weighted
     * sum of the outputs of all the neurons that input into this
     * one processed by the "sigmoid" function. The input neurons
     * <strong>must</strong> have a valid output before invoking
     * this method. Invoking this method on an input neuron has
     * no effect.
     */
    final void compute()
    {
        if (inputs!=null)
        {
            double weightedSum = 0;
            for (int i=0; i<inputs.length; i++)
            {
                weightedSum += inputs[i].output * weights[i];
            }
            output = transfert.transfert(weightedSum);
        }
    }

    /**
     * Returns a string representation of this neuron.
     */
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        if (inputs!=null)
        {
            buffer.append(inputs.length);
            buffer.append(" input; ");
        }
        buffer.append("transfert=");
        buffer.append(transfert);
        buffer.append(']');
        return buffer.toString();
    }
}
