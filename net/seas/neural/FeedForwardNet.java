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
import java.io.Serializable;
import net.seas.resources.Resources;


/**
 * Basic Feed Forward type network with no built in learning scheme.
 * This class can be used by itself if you provide a working set of
 * connection weights between the nodes. However, normally this class
 * is sub-classed in order to provide a training mechanism.
 *
 * @version 1.0
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
public class FeedForwardNet implements Serializable
{
    /**
     * Array of neurons found in all layers. <code>neurons[0]</code> is the array of
     * neurons in the input layer. <code>neurons[neurons.length-1]</code> is the array
     * of neurons in the output layer. Other arrays are hidden layers.
     */
    private final Neuron[][] neurons;

    /**
     * Bias node (always outputs 1) that inputs into each computational layer
     * <strong>except</strong> the output layer. Since no mutable methods are
     * going to be invoked on this instance, the same instance will is shared
     * by all layers. This node may appears in any <code>neurons</code> array.
     */
    private final Neuron bias;

    /**
     * Tells whatever this network output is valid.
     * If <code>false</code>, then {@link #validate}
     * must be invoked prior to returns an output.
     */
    private boolean valid;

    /**
     * Constructor for a feed forward network where the weights
     * are set to random values (with a gaussian distribution
     * between -5 and +5).
     *
     * @param numNeuronsPerLayer Number of neurons in each layer.
     *        <code>numNeuronsPerLayer[0]</code> is the number of neurons in the input layer.
     *        <code>numNeuronsPerLayer[numNeuronsPerLayer.length-1]</code> is the number of
     *        neurons in the output layer. Other integers (if any) are the number of neurons
     *        in hidden layers.
     */
    public FeedForwardNet(final int[] numNeuronsPerLayer)
    {
        bias = new Neuron();
        bias.output = 1;

        Neuron[] previousLayer = null;
        final int numLayers = numNeuronsPerLayer.length;
        neurons = new Neuron[numLayers][];
        for (int j=0; j<numLayers; j++)
        {
            final Neuron[] currentLayer;
            final int numNeuronsInLayer = numNeuronsPerLayer[j];
            if (j != numLayers-1)
            {
                currentLayer = new Neuron[numNeuronsInLayer + 1];
                currentLayer[numNeuronsInLayer] = bias;
            }
            else
            {
                // If filling the last layer (the output layer), ommit bias.
                currentLayer = new Neuron[numNeuronsInLayer];
            }
            for (int i=0; i<numNeuronsInLayer; i++)
            {
                currentLayer[i] = new Neuron(previousLayer);
            }
            neurons[j] = previousLayer = currentLayer;
        }
    }

    /**
     * Returns the number of input nodes in this network (not
     * counting the "input" bias node).
     *
     * @return The number of input nodes in this network.
     */
    public final int getNumInputs()
    {
        switch (neurons.length)
        {
            case 0:  return 0;
            case 1:  return neurons[0].length; // Input layer == output layer: no bias node.
            default: return neurons[0].length-1;
        }
    }

    /**
     * Set a specified input node to a specified value.
     *
     * @param  inputNum Index of the input node to be set.
     * @param  value    Value to set input node to.
     */
    public void setInput(final int inputNum, final double value)
    {
        if (inputNum >= getNumInputs())
        {
            // Do not allows overwriting the bias value.
            throw new ArrayIndexOutOfBoundsException(inputNum);
        }
        neurons[0][inputNum].output = value;
        valid = false;
    }

    /**
     * Set all input nodes at once using a vector of input values.
     *
     * @param values Array of values to assign to input nodes.
     */
    public void setInputs(final double[] values)
    {
        // Remember, inputs[] includes a bias node but the values[] vector does not!
        if (values.length == getNumInputs())
        {
            if (neurons.length != 0)
            {
                valid = false;
                final Neuron[] inputs=neurons[0];
                for (int i=0; i<values.length; i++)
                    inputs[i].output = values[i];
            }
        }
        else
        {
            throw new IllegalArgumentException(Resources.format(Clé.INPUT_LENGTH_MISMATCH));
        }
    }

    /**
     * Get the current value of a specified input node.
     *
     * @param  inputNum  Index of the input node to get the value of.
     * @return The value of the specified input node.
     */
    public double getInput(final int inputNum)
    {
        if (inputNum >= getNumInputs())
        {
            // The bias node is not a valid node.
            throw new ArrayIndexOutOfBoundsException(inputNum);
        }
        return neurons[0][inputNum].output;
    }

    /**
     * Get values of all the input nodes at once returned as an
     * array.
     *
     *  @return Array of all input node values.
     */
    public double[] getInputs()
    {
        // Remember, inputs[] includes a bias node but values[] does not!
        final double[] values = new double[getNumInputs()];
        if (neurons.length != 0)
        {
            final Neuron[] inputs=neurons[0];
            for (int i=0; i<values.length; i++)
            {
                values[i] = inputs[i].output;
            }
        }
        return values;
    }

    /**
     * Returns the number of output nodes in this network.
     */
    public final int getNumOutputs()
    {
        final int numLayers = neurons.length;
        return (numLayers!=0) ? neurons[numLayers-1].length : 0;
    }

    /**
     * Get the output value of a specified output node.
     * Outputs are computed from last values set as inputs.
     *
     * @param  outputNum  Index of the output node to get the value of.
     * @return The value of the specified output node.
     */
    public double getOutput(final int outputNum)
    {
        final int numLayers = neurons.length;
        if (numLayers!=0)
        {
            if (!valid) compute();
            return neurons[numLayers-1][outputNum].output;
        }
        else
        {
            throw new ArrayIndexOutOfBoundsException(outputNum);
        }
    }

    /**
     * Get output values of all the output nodes at once returned as an array.
     * Outputs are computed from last values set as inputs.
     *
     * @return Array of output node values.
     */
    public double[] getOutputs()
    {
        final double[] values = new double[getNumOutputs()];
        final int numLayers = neurons.length;
        if (numLayers!=0)
        {
            if (!valid) compute();
            final Neuron[] outputs = new Neuron[numLayers-1];
            for (int i=0; i<outputs.length; i++)
            {
                values[i] = outputs[i].output;
            }
        }
        return values;
    }

    /**
     * Compute immediately all outputs.
     */
    private void compute()
    {
        for (int j=1; j<neurons.length; j++)
        {
            final Neuron[] layer = neurons[j];
            for (int i=0; i<layer.length; i++)
            {
                layer[i].compute();
            }
        }
        valid = true;
    }

    /**
     * Returns the number of layers in this network.
     * This include input, output and hidden layers.
     */
    public int getNumLayers()
    {return neurons.length;}

    /**
     * Returns the total number of neurons or nodes in the
     * entire network, including input nodes.  Includes the bias
     * node that is a part of every hidden and input layer.
     * This is different than {@link #getNumInputs}.
     *
     *  @return The number of neurons in this network.
     */
    public int getNumNeurons()
    {
        int num = 0;
        for (int j=neurons.length; --j>=0;)
        {
            num += neurons[j].length;
        }
        return num;
    }

    /**
     * Return the total number of connections between all
     * neurons in this network.  Includes connections to the
     * bias node in the input and each of the hidden layers.
     *
     *  @return The number of connections between nodes
     *          in this network.
     */
    public int getNumConnections()
    {
        int num = 0;
        for (int j=neurons.length; --j>=0;)
        {
            final Neuron[] layer = neurons[j];
            for (int i=layer.length; --i>=0;)
            {
                num += layer[i].getNumInputs();
            }
        }
        return num;
    }
}
