/*
 * Remote sensing images: database, visualisation and simulations
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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

// Input/output
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;

// Miscellaneous
import java.util.Random;
import net.seas.util.XClass;
import net.seas.util.XArray;
import net.seas.resources.Resources;
import java.util.NoSuchElementException;


/**
 * A default training set backed by an array.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class DefaultTrainingSet implements TrainingSet, Serializable
{
    /**
     * Number of inputs values per instance.
     */
    private int numInputs;

    /**
     * Number of outputs values per instance.
     */
    private int numOutputs;

    /**
     * Merged set of data (include input and output values).
     */
    private double[] data = new double[64];

    /**
     * Number of test sets. The length of valid values in {@link #data}
     * is equals to <code>count * (numInputs + numOutputs)</code>.
     */
    private int count;

    /**
     * Current position. Methods {@link #getTestInputs} and {@link #getTestOutputs}
     * will return data at this position. Index of first input value into {@link #data}
     * is equals to <code>position * (numInputs + numOutputs)</code>.
     */
    private int position = -1;

    /**
     * Construct a initially empty training set.
     */
    public DefaultTrainingSet()
    {}

    /**
     * Returns the index into the {@link #data}
     * array for the specified instance index.
     */
    private int toIndex(final int record)
    {return record * (numInputs + numOutputs);}

    /**
     * Add all data from the specified training set.
     */
    public synchronized void addAll(final TrainingSet set)
    {
        set.rewind();
        double[]  inputs = null;
        double[] outputs = null;
        while (set.next())
        {
            inputs  = set.getTestInputs ( inputs);
            outputs = set.getTestOutputs(outputs);
            add(inputs, outputs);
        }
    }

    /**
     * Add a set of test values to this training set.
     *
     * @param  inputs The set of input values. Values will be copied.
     * @param outputs The set of output values. Values will be copied.
     */
    public synchronized void add(final double[] inputs, final double[] outputs)
    {
        if (count==0)
        {
            numInputs  =  inputs.length;
            numOutputs = outputs.length;
        }
        if (numInputs  !=  inputs.length) throw new IllegalArgumentException(); // TODO
        if (numOutputs != outputs.length) throw new IllegalArgumentException(); // TODO
        int length = numInputs + numOutputs;
        if (length*(count+1) > data.length)
        {
            data = XArray.resize(data, length*(count + Math.min(count, 512)));
        }
        length *= count++;
        System.arraycopy(inputs,  0, data, length,            inputs.length);
        System.arraycopy(outputs, 0, data, length+numInputs, outputs.length);
    }

    /**
     * Trims the capacity of this training set to be the set's current
     * size. An application can use this operation to minimize the storage
     * of a training set.
     */
    public synchronized void trimToSize()
    {data = XArray.resize(data, toIndex(count));}
    
    /**
     * Instructs the training set to move on a new training instance
     * (combination of inputs and the associated "correct" or target
     * outputs).
     *
     * @return <code>true</code> if the new current training instance is
     *        valid; <code>false</code> if there are no more instances.
     */
    public boolean next()
    {return ++position < count;}

    /**
     * Returns a vector of the values associated with each input node
     * of a neural network for the current training instance.
     *
     * @param  dest A destination array, or <code>null</code> to create a
     *         new one. If non-null, values in this array will be overrided.
     * @return The current set of input values.
     */
    public double[] getTestInputs(double[] dest)
    {
        if (position >= count)
        {
            throw new NoSuchElementException();
        }
        if (dest==null) dest=new double[numInputs];
        if (dest.length != numInputs)
        {
            throw new IllegalArgumentException(); // TODO
        }
        System.arraycopy(data, toIndex(position), dest, 0, numInputs);
        return dest;
    }
    
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
    public double[] getTestOutputs(double[] dest)
    {
        if (position >= count)
        {
            throw new NoSuchElementException();
        }
        if (dest==null) dest=new double[numOutputs];
        if (dest.length != numOutputs)
        {
            throw new IllegalArgumentException(); // TODO
        }
        System.arraycopy(data, toIndex(position)+numInputs, dest, 0, numOutputs);
        return dest;
    }
    
    /**
     * Move the cursor before the first training instance.
     */
    public void rewind()
    {position = -1;}

    /**
     * Randomly permute instances in this training set.
     */
    public void shuffle()
    {
        final Random random = new Random();
        final double[] buffer = new double[numInputs + numOutputs];
        for (int i=count; i>=1; i--)
        {
            final int index0 = toIndex(i-1);
            final int index1 = toIndex(random.nextInt(i));
            System.arraycopy(data, index0, buffer,    0, buffer.length);
            System.arraycopy(data, index1, data, index0, buffer.length);
            System.arraycopy(buffer,    0, data, index1, buffer.length);
        }
    }

    /**
     * Trim this training set before to save it.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException
    {
        trimToSize();
        out.defaultWriteObject();
    }

    /**
     * Returns a string representation of this training set.
     */
    public String toString()
    {
        final double sum[] = new double[numInputs + numOutputs];
        for (int i=0; i<count; i++)
        {
            final int index = toIndex(i);
            for (int j=0; j<sum.length; j++)
            {
                sum[j] += data[index+j];
            }
        }
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(count);
        buffer.append(" sets.");
        for (int i=0; i<sum.length; i++)
        {
            buffer.append(i==0 ? " Main=" : ", ");
            buffer.append((float) (sum[i]/count));
        }
        buffer.append(']');
        return buffer.toString();
    }
}
