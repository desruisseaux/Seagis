/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.awt;

// Angles
import org.geotools.pt.Angle;
import org.geotools.pt.Latitude;
import org.geotools.pt.Longitude;
import org.geotools.pt.AngleFormat;

// Swing (for JSpinner)
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JFormattedTextField;
import javax.swing.AbstractSpinnerModel;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.io.Serializable;

// Miscellaneous
import java.text.ParseException;
import net.seas.resources.Resources;
import net.seas.resources.ResourceKeys;
import org.geotools.resources.Utilities;


/**
 * A {@link SpinnerModel} for sequences of angles.
 * This model work like {@link SpinnerNumberModel}.
 *
 * @see JSpinner
 * @see SpinnerNumberModel
 *
 * @version 1.0
 * @author Adapted from Hans Muller
 * @author Martin Desruisseaux
 */
public class AngleSpinnerModel extends AbstractSpinnerModel implements Serializable
{
    /**
     * The current value.
     */
    private Angle value;

    /**
     * The minimum and maximum values.
     */
    private double minimum, maximum;

    /**
     * The step size.
     */
    private double stepSize = 1;

    /**
     * Constructs a <code>AngleSpinnerModel</code> that represents a closed sequence
     * of angles. Initial minimum and maximum values are choosen according the
     * <code>value</code> type:
     *
     * <table>
     *   <tr><td>{@link Longitude}&nbsp;</td> <td>-180� to 180�</td></tr>
     *   <tr><td>{@link Latitude}&nbsp;</td>  <td>-90� to 90�</td>  </tr>
     *   <tr><td>{@link Angle}&nbsp;</td>     <td>0� to 360�</td>   </tr>
     * </table>
     *
     * @param value the current (non <code>null</code>) value of the model
     */
    public AngleSpinnerModel(final Angle value)
    {
        this.value = value;
        if (value instanceof Longitude)
        {
            minimum  = Longitude.MIN_VALUE;
            maximum  = Longitude.MAX_VALUE;
        }
        else if (value instanceof Latitude)
        {
            minimum  = Latitude.MIN_VALUE;
            maximum  = Latitude.MAX_VALUE;
        }
        else if (value!=null)
        {
            minimum = 0;
            maximum = 360;
        }
        else throw new IllegalArgumentException();
    }

    /**
     * Changes the lower bound for angles in this sequence.
     */
    public void setMinimum(final double minimum)
    {
        if (this.minimum != minimum)
        {
            this.minimum = minimum;
            fireStateChanged();
        }
    }

    /**
     * Returns the first angle in this sequence.
     */
    public double getMinimum()
    {return minimum;}

    /**
     * Changes the upper bound for angles in this sequence.
     */
    public void setMaximum(final double maximum)
    {
        if (this.maximum != maximum)
        {
            this.maximum = maximum;
            fireStateChanged();
        }
    }

    /**
     * Returns the last angle in the sequence.
     */
    public double getMaximum()
    {return maximum;}

    /**
     * Changes the size of the value change computed by the
     * <code>getNextValue</code> and <code>getPreviousValue</code>
     * methods.
     */
    public void setStepSize(final double stepSize)
    {
        if (this.stepSize != stepSize)
        {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }

    /**
     * Returns the size of the value change computed by the
     * <code>getNextValue</code> and <code>getPreviousValue</code> methods.
     */
    public double getStepSize()
    {return stepSize;}

    /**
     * Wrap the specified value into an {@link Angle} object.
     */
    final Angle toAngle(final double newValue)
    {
        if (value instanceof Longitude) return new Longitude(newValue);
        if (value instanceof  Latitude) return new  Latitude(newValue);
        return new Angle(newValue);
    }

    /**
     * Returns <code>value+factor*stepSize</code>.
     */
    private Angle getNextValue(final int factor)
    {
        final double newValue = value.degrees() + stepSize*factor;
        if (!(newValue>=minimum && newValue<=maximum)) return null;
        return toAngle(newValue);
    }

    /**
     * Returns the next angle in the sequence.
     */
    public Object getNextValue()
    {return getNextValue(+1);}

    /**
     * Returns the previous angle in the sequence.
     */
    public Object getPreviousValue()
    {return getNextValue(-1);}

    /**
     * Returns the value of the current angle of the sequence.
     */
    public Object getValue()
    {return value;}

    /**
     * Sets the current value for this sequence.
     */
    public void setValue(final Object value)
    {
        if (!(value instanceof Angle))
        {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2, "value", value));
        }
        if (!Utilities.equals(value, this.value))
        {
            this.value = (Angle)value;
            fireStateChanged();
        }
    }

    /**
     * This subclass of {@link javax.swing.InternationalFormatter} maps the
     * minimum/maximum properties to a {@link AngleSpinnerModel}.
     *
     * @version 1.0
     * @author Adapted from Hans Muller
     * @author Martin Desruisseaux
     */
    private static class EditorFormatter extends InternationalFormatter
    {
        /**
         * The spinner model.
         */
        private final AngleSpinnerModel model;

        /**
         * Construct a formatter.
         */
        EditorFormatter(final AngleSpinnerModel model, final AngleFormat format)
        {
            super(format);
            this.model = model;
            setAllowsInvalid(true);
            setCommitsOnValidEdit(false);
            setOverwriteMode(false);

            final Class classe;
            final Object value=model.getValue();
            if      (value instanceof Longitude) classe=Longitude.class;
            else if (value instanceof  Latitude) classe=Latitude.class;
            else                                 classe=Angle.class;
            setValueClass(classe);
        }

        /**
         * Returns the {@link Object} representation
         * of the {@link String} <code>text</code>.
         */
        public Object stringToValue(final String text) throws ParseException
        {
            final Object value = super.stringToValue(text);
            if (value instanceof Longitude) return value;
            if (value instanceof  Latitude) return value;
            if (value instanceof     Angle)
            {
                final Class valueClass = getValueClass();
                if (Longitude.class.isAssignableFrom(valueClass))
                    return new Longitude(((Angle)value).degrees());
                if (Latitude.class.isAssignableFrom(valueClass))
                    return new Latitude(((Angle)value).degrees());
            }
            return value;
        }

        /**
         * Sets the minimum value.
         */
        public void setMinimum(final Comparable min)
        {model.setMinimum(((Angle)min).degrees());}

        /**
         * Gets the minimum value.
         */
        public Comparable getMinimum()
        {return model.toAngle(model.getMinimum());}

        /**
         * Sets the maximum value.
         */
        public void setMaximum(final Comparable max)
        {model.setMaximum(((Angle)max).degrees());}

        /**
         * Gets the maximum value.
         */
        public Comparable getMaximum()
        {return model.toAngle(model.getMaximum());}
    }

    /**
     * An editor for a {@link javax.swing.JSpinner}. The value of the editor is
     * displayed with a {@link javax.swing.JFormattedTextField} whose format is
     * defined by a {@link javax.swing.text.InternationalFormatter} instance
     * whose minimum and maximum properties are mapped to the
     * {@link SpinnerNumberModel}.
     *
     * @version 1.0
     * @author Adapted from Hans Muller
     * @author Martin Desruisseaux
     */
    public static class Editor extends JSpinner.DefaultEditor
    {
        /**
         * Construct an editor for the specified format.
         */
        public Editor(final JSpinner spinner, final AngleFormat format)
        {
            super(spinner);
            final SpinnerModel genericModel = spinner.getModel();
            if (!(genericModel instanceof AngleSpinnerModel))
            {
                throw new IllegalArgumentException();
            }
            final AngleSpinnerModel         model = (AngleSpinnerModel) genericModel;
            final EditorFormatter       formatter = new EditorFormatter(model, format);
            final DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            final JFormattedTextField       field = getTextField();
            field.setEditable(true);
            field.setFormatterFactory(factory);
            field.setHorizontalAlignment(JFormattedTextField.RIGHT);

            /* TBD - initializing the column width of the text field
             * is imprecise and doing it here is tricky because
             * the developer may configure the formatter later.
             */
            try
            {
                final String maxString = formatter.valueToString(formatter.getMinimum());
                final String minString = formatter.valueToString(formatter.getMaximum());
                field.setColumns(Math.max(maxString.length(), minString.length()));
            }
            catch (ParseException exception)
            {
                // TBD should throw a chained error here
            }
        }
    }
}
