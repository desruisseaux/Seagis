/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contacts: Observatoire du Saint-Laurent         Michel Petit
 *           Institut Maurice Lamontagne           Institut de Recherche pour le Développement
 *           850 de la Mer, C.P. 1000              500 rue Jean-François Breton
 *           Mont-Joli (Québec)                    34093 Montpellier
 *           G5H 3Z4                               France
 *           Canada
 *
 *           mailto:osl@osl.gc.ca                  mailto:Michel.Petit@teledetection.fr
 */
package net.seas.plot.axis;

// Miscellaneous
import java.util.Locale;
import javax.units.Unit;
import java.io.Serializable;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.EventListenerList;
import net.seagis.resources.Utilities;
import net.seas.resources.ResourceKeys;
import net.seas.resources.Resources;


/**
 * Base class for graduation.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class AbstractGraduation implements Graduation, Serializable
{
    /**
     * The axis label for this graduation.
     */
    private String label;

    /**
     * The locale for formatting labels.
     */
    private Locale locale=Locale.getDefault();

    /**
     * A list of event listeners for this component.
     */
    protected final EventListenerList listenerList=new EventListenerList();

    /**
     * Construct a default graduation.
     */
    public AbstractGraduation()
    {}

    /**
     * Set the minimum value for this graduation. If the new minimum is greater
     * than the current maximum, then the maximum will also be set to a value
     * greater than or equals to the minimum.
     *
     * @param  value The new minimum in {@link #getUnit} units.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     * @throws IllegalArgumentException If <code>value</code> is NaN ou infinite.
     *
     * @see #getMinimum
     * @see #setMaximum(double)
     */
    public abstract boolean setMinimum(final double value) throws IllegalArgumentException;

    /**
     * Set the maximum value for this graduation. If the new maximum is less
     * than the current minimum, then the minimum will also be set to a value
     * less than or equals to the maximum.
     *
     * @param  value The new maximum in {@link #getUnit} units.
     * @return <code>true</code> if the state of this graduation changed
     *         as a result of this call, or <code>false</code> if the new
     *         value is identical to the previous one.
     * @throws IllegalArgumentException If <code>value</code> is NaN ou infinite.
     *
     * @see #getMaximum
     * @see #setMinimum(double)
     */
    public abstract boolean setMaximum(final double value) throws IllegalArgumentException;
    
    /**
     * Returns the axis label. This label should not include units or
     * timezone, since this part is provided by {@link #getUnitLabel}.
     */
    public String getAxisLabel()
    {return label;}

    /**
     * Set the axis label. This label should not include units or
     * timezone, since this part is provided by {@link #getUnitLabel}.
     * This method will fire a property change event with the
     * <code>"label"</code> property name.
     *
     * @param label New axis label, or <code>null</code>
     *        to remove any previous setting.
     */
    public synchronized void setAxisLabel(final String label)
    {
        final String old=this.label;
        this.label=label;
        firePropertyChange("label", old, label);
    }

    /**
     * Returns a string representation of axis's units, or <code>null</code>
     * if there is none. The default implementation returns the string
     * representation of {@link #getUnit}.
     */
    public String getUnitLabel()
    {
        final Unit unit=getUnit();
        return (unit!=null) ? unit.toString() : null;
    }

    /**
     * Returns the locale to use for formatting labels.
     */
    public Locale getLocale()
    {return locale;}

    /**
     * Set the locale to use for formatting labels.
     * This will fire a property change event with
     * the <code>"locale"</code> property name.
     */
    public synchronized void setLocale(final Locale locale)
    {
        final Locale old=this.locale;
        this.locale=locale;
        firePropertyChange("locale", old, locale);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the listener list. The listener is
     * registered for all properties. A {@link PropertyChangeEvent} will get fired
     * in response to setting a property, such as {@link #setAxisLabel} or {@link #setLocale}.
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener)
    {listenerList.add(PropertyChangeListener.class, listener);}

    /**
     * Removes a {@link PropertyChangeListener} from the listener list.
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener)
    {listenerList.remove(PropertyChangeListener.class, listener);}

    /**
     * Support for reporting property changes. This method can be called when a
     * property has changed. It will send the appropriate {@link PropertyChangeEvent}
     * to any registered {@link PropertyChangeListeners}.
     *
     * @param propertyName The property whose value has changed.
     * @param oldValue     The property's previous value.
     * @param newValue     The property's new value.
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue)
    {
        if (propertyName==null || !Utilities.equals(oldValue, newValue))
        {
            PropertyChangeEvent changeEvent=null;
            final Object[] listeners=listenerList.getListenerList();
            for (int i=listeners.length-2; i>=0; i-=2)
            {
                if (listeners[i]==PropertyChangeListener.class)
                {
                    if (changeEvent==null)
                        changeEvent=new PropertyChangeEvent(this, propertyName, oldValue, newValue);
                    ((PropertyChangeListener)listeners[i+1]).propertyChange(changeEvent);
                }
            }
        }
    }

    /**
     * Retourne la longueur de l'axe, en
     * pixels ou en points (1/72 de pouce).
     */
    static float getVisualAxisLength(final RenderingHints hints)
    {return getValue(hints, VISUAL_AXIS_LENGTH, 600);}

    /**
     * Retourne l'espace approximatif (en pixels ou en points) à laisser entre les
     * graduations principales. L'espace réel entre les graduations peut être légèrement
     * différent, par exemple pour avoir des étiquettes qui correspondent à des valeurs
     * arrondies.
     */
    static float getVisualTickSpacing(final RenderingHints hints)
    {return getValue(hints, VISUAL_TICK_SPACING, 48);}

    /**
     * Retourne une valeur sous forme de nombre réelle.
     */
    private static float getValue(final RenderingHints hints, final RenderingHints.Key key, final float defaultValue)
    {
        if (hints!=null)
        {
            final Object object = hints.get(key);
            if (object instanceof Number)
            {
                final float value = ((Number) object).floatValue();
                if (value!=0 && !Float.isInfinite(value)) return value;
            }
        }
        return defaultValue;
    }
    
    /**
     * Vérifie que le nombre spécifié est non-nul. S'il
     * est 0, NaN ou infini, une exception sera lancée.
     *
     * @param  name Nom de l'argument.
     * @param  n Nombre à vérifier.
     * @throws IllegalArgumentException Si <var>n</var> est NaN ou infini.
     */
    static void ensureNonNull(final String name, final double n) throws IllegalArgumentException
    {
        if (Double.isNaN(n) || Double.isInfinite(n) || n==0)
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2, name, new Double(n)));
    }
    
    /**
     * Vérifie que le nombre spécifié est réel. S'il
     * est NaN ou infini, une exception sera lancée.
     *
     * @param  name Nom de l'argument.
     * @param  n Nombre à vérifier.
     * @throws IllegalArgumentException Si <var>n</var> est NaN ou infini.
     */
    static void ensureFinite(final String name, final double n) throws IllegalArgumentException
    {
        if (Double.isNaN(n) || Double.isInfinite(n))
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2, name, new Double(n)));
    }

    /**
     * Vérifie que le nombre spécifié est réel. S'il
     * est NaN ou infini, une exception sera lancée.
     *
     * @param  name Nom de l'argument.
     * @param  n Nombre à vérifier.
     * @throws IllegalArgumentException Si <var>n</var> est NaN ou infini.
     */
    static void ensureFinite(final String name, final float n) throws IllegalArgumentException
    {
        if (Float.isNaN(n) || Float.isInfinite(n))
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2, name, new Float(n)));
    }
}
