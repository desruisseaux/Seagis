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
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// Formating
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import net.seagis.io.LineFormat;

// Miscellaneous
import java.util.Random;
import net.seas.util.XArray;
import net.seas.resources.Resources;
import java.util.NoSuchElementException;
import net.seagis.resources.Utilities;


/**
 * A default training set backed by an array.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class DefaultTrainingSet implements TrainingSet, Serializable
{
    /**
     * Serial number for compatibility with previous versions.
     */
    //private static final long serialVersionUID = ?; // TODO

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
    private transient int position = -1;

    /**
     * Construct a initially empty training set.
     */
    public DefaultTrainingSet()
    {}

    /**
     * Construct a training set with data from the specified file.
     * The file should contains a matrix of numbers. The matrix may
     * have any number of rows, but the number of columns should be
     * equals to <code>numInputs+numOutputs</code>.
     *
     * @param file The file to parse.
     * @param numInputs The expected number of input parameters.
     * @param numInputs The expected number of output parameters.
     * @throws IOException if the file can't be opened or parsed.
     */
    public DefaultTrainingSet(final File file, final int numInputs, final int numOutputs) throws IOException
    {
        this.numInputs  = numInputs;
        this.numOutputs = numOutputs;
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        load(reader);
        reader.close();
    }

    /**
     * Add a matrix of data from the specified stream to this training set.
     *
     *
     * @param reader The input stream to parse. Line will be read
     *        until end-of-stream, but the stream will not be closed.
     * @throws IOException if the stream can't be parsed.
     */
    private void load(final BufferedReader reader) throws IOException
    {
        final double[]   inputs = new double[numInputs];
        final double[]  outputs = new double[numOutputs];
        final double[] dataline = new double[numInputs + numOutputs];
        final LineFormat  linef = new LineFormat();
        try
        {
            String line;
            while ((line=reader.readLine())!=null)
            {
                linef.setLine(line);
                linef.getValues(dataline);
                System.arraycopy(dataline, 0,          inputs, 0,  numInputs);
                System.arraycopy(dataline, numInputs, outputs, 0, numOutputs);
                add(inputs, outputs);
            }
        }
        catch (ParseException exception)
        {
            final IOException e = new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

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
        if (position<0 || position>=count)
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
        if (position<0 || position>=count)
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
     * Changes the current vector of values associated with each input node.
     *
     * @param  inputs The new inputs values for the current training instance.
     */
    public void updateTestInputs(final double[] inputs)
    {
        if (position<0 || position>=count)
        {
            throw new NoSuchElementException();
        }
        if (inputs.length != numInputs)
        {
            throw new IllegalArgumentException(); // TODO
        }
        System.arraycopy(inputs, 0, data, toIndex(position), numInputs);
    }

    /**
     * Changes the current vector of values associated with each output node.
     *
     * @param  inputs The new outputs values for the current training instance.
     */
    public void updateTestOutputs(final double[] outputs)
    {
        if (position<0 || position>=count)
        {
            throw new NoSuchElementException();
        }
        if (outputs.length != numOutputs)
        {
            throw new IllegalArgumentException(); // TODO
        }
        System.arraycopy(outputs, 0, data, toIndex(position)+numInputs, numOutputs);
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
     * Normalize inputs and outputs data. Mean and standard deviation are first computed
     * for all inputs and outputs node.  Then, values are normalized (assuming that data
     * have a normal distribution) as in the following pseudo-code:
     *
     * <blockquote><pre>
     * value = (value-mean)/standardDeviation
     * </pre></blockquote>
     */
    public void normalize()
    {
        final double mean[] = new double[numInputs + numOutputs];
        final double stdv[] = new double[numInputs + numOutputs];
        statistics(mean, stdv);
        for (int j=count; --j>=0;)
        {
            final int index = toIndex(j);
            for (int i=mean.length; --i>=0;)
            {
                final double dev = stdv[i];
                if (!Double.isNaN(dev))
                {
                    data[index+i] = (data[index+i] - mean[i]) / dev;
                }
            }
        }
    }

    /**
     * Compute statistics.
     *
     * @param mean Arrays in which to store means values. All elements must be initially 0.
     * @param stdv Arrays in which to store standard deviation. All elements must be initially 0.
     */
    private void statistics(final double[] mean, final double[] stdv)
    {
        final int n[] = new int[Math.min(mean.length, stdv.length)];
        for (int j=count; --j>=0;)
        {
            final int index = toIndex(j);
            for (int i=n.length; --i>=0;)
            {
                final double value = data[index+i];
                if (!Double.isNaN(value))
                {
                    mean[i] += value;
                    stdv[i] += value*value;
                    n   [i]++;
                }
            }
        }
        for (int i=n.length; --i>=0;)
        {
            final int    ni   = n[i];
            final double sum  = mean[i];
            final double sum2 = stdv[i];
            stdv[i]  = Math.sqrt((sum2 - sum*sum/ni) / (ni-1));
            mean[i] /= ni;
        }
    }

    /**
     * Returns a string representation of this training set.
     */
    public String toString()
    {
        final double mean[] = new double[numInputs + numOutputs];
        final double stdv[] = new double[numInputs + numOutputs];
        statistics(mean, stdv);

              StringBuffer  buffer = new StringBuffer(Utilities.getShortClassName(this));
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final NumberFormat numbers = NumberFormat.getNumberInstance();
        final FieldPosition  dummy = new FieldPosition(0);
        numbers.setMinimumFractionDigits(3);
        numbers.setMaximumFractionDigits(3);
        buffer.append('[');
        buffer.append(count);
        buffer.append(" sets]");
        buffer.append(lineSeparator);
        for (int i=0; i<mean.length; i++)
        {
            final int n = i<numInputs ? i : i-numInputs;
            buffer.append("    ");
            buffer.append(i<numInputs ? " in #" : "out #");
            if (n>=0 && n<10) buffer.append('0');
            buffer.append(n);
            buffer.append(':');
            buffer=format('\u03BC', numbers, mean[i], buffer, dummy);
            buffer=format('\u03C3', numbers, stdv[i], buffer, dummy);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }

    /**
     * Helper method for {@link #toString}: Format a number.
     */
    private static StringBuffer format(char var, NumberFormat numbers, double value, StringBuffer buffer, FieldPosition dummy)
    {
        buffer.append(' ');
        buffer.append(var);
        buffer.append('=');
        final int p = buffer.length();
        buffer=numbers.format(value, buffer, dummy);
        buffer.insert(p, Utilities.spaces(11-(buffer.length()-p)));
        return buffer;
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
     * Set transients fields after reading.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        position = -1;
    }
}
