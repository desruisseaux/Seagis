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
import net.seas.util.XClass;
import net.seas.resources.Resources;


/**
 * Basic Feed Forward type network.
 *
 * @version 1.0
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
public class FeedForwardNet implements Serializable
{
    /**
     * Serial number for compatibility with previous versions.
     */
    private static final long serialVersionUID = 3890607020215623718L;

    /**
     * Array of neurons in all layers. <code>neurons[0]</code> is the array of
     * neurons in the input layer. <code>neurons[neurons.length-1]</code> is the
     * array of neurons in the output layer. Other arrays are hidden layers.
     */
    protected final Neuron[][] neurons;

    /**
     * The transfert function for all neurons in this network.
     */
    private TransfertFunction transfertFunction = TransfertFunction.SIGMOID;

    /**
     * The train algorithm to use for training this network.
     */
    private TrainingAlgorithm trainingAlgorithm = new BackPropagationAlgorithm();

    /**
     * The training history. This array contains
     * the mean error for each iteration.
     */
    float[] trainHistory;

    /**
     * Tells whatever this network output is valid.
     * If <code>false</code>, then {@link #validate}
     * must be invoked prior to returns an output.
     */
    private transient boolean isValid;

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
                currentLayer[numNeuronsInLayer] = new Neuron(1);
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
        /*
         * Set output labels first, then input labels.
         * We set input labels last in order to overwrite
         * output labels if this network has only 1 layer.
         */
        switch (neurons.length)
        {
            default: setLabels(neurons[neurons.length-1], "out ", getNumOutputs()); // fall through
            case 1:  setLabels(neurons[0],                 "in ", getNumInputs());  // fall through
            case 0:  break;
        }
    }

    /**
     * Set default labels for a neuron layer.
     *
     * @param layer  The neurons layer.
     * @param prefix The labels prefix.
     * @param count  The number of label. Sometime equals to <code>layer.length</code>,
     *               but may be less in order to ignore the bias neuron (which is last).
     */
    private static void setLabels(final Neuron[] layer, final String prefix, int count)
    {
        final StringBuffer buffer = new StringBuffer(prefix);
        final int length = buffer.length();
        while (--count>=0)
        {
            buffer.append(count);
            layer[count].label = buffer.toString();
            buffer.setLength(length);
        }
    }

    /**
     * Returns the transfert function for this neural network.
     */
    public TransfertFunction getTransfertFunction()
    {return transfertFunction;}

    /**
     * Sets the transfert function for this neural network.
     */
    public void setTransfertFunction(final TransfertFunction function)
    {
        if (function==null)
            throw new IllegalArgumentException();
        transfertFunction = function;
    }

    /**
     * Returns the training algorithm for this neural network.
     * This method do not clone the returned algorithm. Change
     * to the returned object will change the underlying algorithm
     * for this network.
     */
    public TrainingAlgorithm getTrainingAlgorithm()
    {return trainingAlgorithm;}

    /**
     * Sets the training algorithm for this neural network.
     */
    public void setTrainingAlgorithm(final TrainingAlgorithm algorithm)
    {
        if (algorithm==null)
            throw new IllegalArgumentException();
        this.trainingAlgorithm = algorithm;
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
     * @return The number of neurons in this network.
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
     * @return The number of connections between nodes
     *         in this network.
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

    /**
     * Returns the number of input nodes in this network (not
     * counting the "input" bias node).
     *
     * @return The number of input nodes in this network.
     * @see #setInput
     * @see #setInputs
     * @see #getNumOutputs
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
     * Returns the number of output nodes in this network.
     * @see #getOutput
     * @see #getOutputs
     * @see #getNumInputs
     */
    public final int getNumOutputs()
    {
        final int numLayers = neurons.length;
        return (numLayers!=0) ? neurons[numLayers-1].length : 0;
    }

    /**
     * Returns the label of a specified input node.
     * This is usually a parameter name.
     */
    public String getInputLabel(final int inputNum)
    {
        if (inputNum >= getNumInputs())
        {
            // The bias node is not a valid node.
            throw new ArrayIndexOutOfBoundsException(inputNum);
        }
        return neurons[0][inputNum].label;
    }

    /**
     * Sets the label of a specified input node. The label is usually a
     * parameter name set once for ever. Neurons's labels are displayed
     * when this network is inserted into a widget.
     */
    public void setInputLabel(final int inputNum, final String label)
    {
        if (inputNum >= getNumInputs())
        {
            // Do not allows overwriting the bias value.
            throw new ArrayIndexOutOfBoundsException(inputNum);
        }
        neurons[0][inputNum].label = label;
    }

    /**
     * Returns the label of a specified output node.
     * This is usually a parameter name.
     */
    public String getOutputLabel(final int outputNum)
    {
        final int numLayers = neurons.length;
        if (numLayers!=0)
        {
            return neurons[numLayers-1][outputNum].label;
        }
        else
        {
            throw new ArrayIndexOutOfBoundsException(outputNum);
        }
    }

    /**
     * Sets the label of a specified output node. The label is usually a
     * parameter name set once for ever. Neurons's labels are displayed
     * when this network is inserted into a widget.
     */
    public void setOutputLabel(final int outputNum, final String label)
    {
        final int numLayers = neurons.length;
        if (numLayers!=0)
        {
            neurons[numLayers-1][outputNum].label = label;
        }
        else
        {
            throw new ArrayIndexOutOfBoundsException(outputNum);
        }
    }

    /**
     * Get the current value of a specified input node.
     *
     * @param  inputNum  Index of the input node to get the value of.
     * @return The value of the specified input node.
     * @see #setInput
     * @see #getNumInputs
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
     * @return Array of all input node values.
     * @see #setInputs
     * @see #getNumInputs
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
     * Set a specified input node to a specified value.
     *
     * @param  inputNum Index of the input node to be set.
     * @param  value    Value to set input node to.
     * @see #getInput
     * @see #getNumInputs
     */
    public void setInput(final int inputNum, final double value)
    {
        if (inputNum >= getNumInputs())
        {
            // Do not allows overwriting the bias value.
            throw new ArrayIndexOutOfBoundsException(inputNum);
        }
        neurons[0][inputNum].output = value;
        isValid = false;
    }

    /**
     * Set all input nodes at once using a vector of input values.
     *
     * @param values Array of values to assign to input nodes.
     * @see #getInputs
     * @see #getNumInputs
     */
    public void setInputs(final double[] values)
    {
        // Remember, inputs[] includes a bias node but the values[] vector does not!
        if (values.length == getNumInputs())
        {
            if (neurons.length != 0)
            {
                isValid = false;
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
     * Get the output value of a specified output node.
     * Outputs are computed from last values set as inputs.
     *
     * @param  outputNum  Index of the output node to get the value of.
     * @return The value of the specified output node.
     * @see #setInputs
     * @see #getNumOutputs
     */
    public double getOutput(final int outputNum)
    {
        final int numLayers = neurons.length;
        if (numLayers!=0)
        {
            if (!isValid) validate(false);
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
     * @see #setInputs
     * @see #getNumOutputs
     */
    public double[] getOutputs()
    {
        final double[] values = new double[getNumOutputs()];
        final int numLayers = neurons.length;
        if (numLayers!=0)
        {
            if (!isValid) validate(false);
            final Neuron[] outputs = new Neuron[numLayers-1];
            for (int i=0; i<outputs.length; i++)
            {
                values[i] = outputs[i].output;
            }
        }
        return values;
    }

    /**
     * Compute immediately outputs for all neurons.
     * This method is invoked by {@link #getOutputs}
     * if it detected that inputs have changed.
     *
     * @param isTraining <code>true</code> if this method is invoking for training
     *        the network. If <code>false</code>, then {@link Neuron#gradient} will
     *        <strong>not</strong> be computed.
     */
    private void validate(final boolean isTraining)
    {
        // Starting the loop at j=0 would not hurt, but is not necessary
        // since neurons in layer 0 have no inputs neurons.
        for (int j=1; j<neurons.length; j++)
        {
            final Neuron[] layer = neurons[j];
            for (int i=0; i<layer.length; i++)
            {
                layer[i].validate(transfertFunction, isTraining);
            }
        }
        isValid = true;
    }

    /**
     * Initialize the network for training.  Input values are set according
     * current instance in the training set. Output values are computed and
     * compared to the target outputs from the training set. All neurons in
     * the output layer take an error value equals to the difference between
     * the target output and the neuron's output. All other layers (hidden
     * and input) take an error value of initially 0.
     *
     * @param training The training set. Only the current instance of
     *        this training set will be used.
     */
    final void initialize(final TrainingSet training)
    {
        setInputs(training.getTestInputs(null));
        validate(true); // Validate for training: compute gradients.
        boolean isOutputLayer = true;
        for (int j=neurons.length; --j>=0;)
        {
            final Neuron[] layer = neurons[j];
            if (isOutputLayer)
            {
                final double[] targetOutputs = training.getTestOutputs(null);
                if (targetOutputs.length != layer.length)
                {
                    throw new IllegalArgumentException(Resources.format(Clé.OUTPUT_LENGTH_MISMATCH));
                }
                for (int i=0; i<layer.length; i++)
                {
                    layer[i].error = targetOutputs[i] - layer[i].output;
                }
                isOutputLayer = false;
            }
            else
            {
                for (int i=0; i<layer.length; i++)
                {
                    layer[i].error = 0;
                }
            }
        }
    }

    /**
     * Train this network to match a set of input patterns to a set of
     * output patterns. Trains until a certain tolerance or maximum time
     * elapsed is reached.
     *
     * @param  testGenerator Training instance generator that
     *         generates combinations of inputs and corresponding
     *         outputs to train the network to reproduce.
     * @return <code>true</code> if the tolerance factor has been
     *         reached before the allowed time has been elapsed.
     */
    public boolean train(final TrainingSet testGenerator)
    {
        trainHistory = trainingAlgorithm.train(this, testGenerator);
        return (trainHistory!=null && trainHistory.length!=0 && trainHistory[trainHistory.length-1] <= trainingAlgorithm.getTolerance());
    }

    /**
     * Calculates the root mean square (RMS) error of this
     * network across all the supplied training instances.
     *
     * @param  testGenerator Reference to a class
     *         that can provide training instances.
     * @return The RMS error of this network.
     */
    public double getError(final TrainingSet testGenerator)
    {
        double sum = 0;
        int  count = 0;
        testGenerator.rewind();
        double[]  inputs = null;
        double[] outputs = null;
        while (testGenerator.next())
        {
             inputs = testGenerator.getTestInputs ( inputs);
            outputs = testGenerator.getTestOutputs(outputs);
            setInputs(inputs);
            double e2i = 0;
            for (int k=outputs.length; --k>=0;)
            {
                final double error = outputs[k] - getOutput(k);
                e2i += error * error;
            }
            // Add in the average error across the outputs for this case.
            sum += e2i / outputs.length;
            count++;
        }
        // Output the RMS error for the network.
        return Math.sqrt(sum/count);
    }

    /**
     * Returns a string representation of this network.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(Resources.format(Clé.NEURAL_NETWORK_SUMMARY¤5, new Object[] {
                      new Integer(getNumInputs()),
                      new Integer(getNumOutputs()),
                      new Integer(getNumConnections()),
                      new Integer(getNumNeurons()),
                      new Integer(getNumLayers())}));
        buffer.append(']');
        return buffer.toString();
    }
}
