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


/**
 * The transfert function and its derivative. Transfert functions are often "S" shaped
 * function used to transition neuron output from "on" to "off" depending on the value
 * input to it.
 *
 * @version 1.0
 * @author Joseph A. Huwaldt
 * @author Martin Desruisseaux
 */
public interface TransfertFunction
{
    /**
     * Log sigmoid transfert function. This transfert function output approximatively 1
     * for any <var>q</var> greater than 5 and 0 for <var>q</var> smaller than than -5.
     * The output is equals to 0.5 for <code><var>q</var>==0</code>.
     *
     * <table cellpadding="6">
     *   <tr>
     *     <td align="center">Function</td>
     *     <td align="center">Derivative</td>
     *     <td align="center">Plot</td>
     *   </tr>
     *   <tr>
     *     <td align="center"><img src="doc-files/transfert/sigmoid/function.png"></td>
     *     <td align="center"><img src="doc-files/transfert/sigmoid/derivative.png"></td>
     *     <td align="center"><img src="doc-files/transfert/sigmoid/plot.png"></td>
     *   </tr>
     * </table>
     */
    public static final TransfertFunction SIGMOID = new AbstractTransfertFunction("SIGMOID")
    {
        public double transfert(final double q)
        {return 1 / (1 + Math.exp(-q));}

        public double derivative(final double q, final double a)
        {return a*(1-a);}
    };

    /**
     * Returns the function value for the specified
     * sum of weighted neuron inputs.
     *
     * @param  q The sum of weighted inputs.
     * @return The transfert function value.
     */
    public abstract double transfert(final double q);

    /**
     * Returns the gradient of this transfert function with respect
     * to the weighted neuron inputs. This is the first derivative
     * of {@link #transfert}.
     *
     * @param  q The sum of weighted inputs.
     * @param  a The transfert function value as computed by
     *           <code>{@link #transfert transfert}(q)</code>.
     * @return The gradient of this transfert function.
     */
    public abstract double derivative(final double q, final double a);
}
