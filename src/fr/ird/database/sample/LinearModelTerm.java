/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
 *
 *
 * Contact: Michel Petit
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.database.sample;

// J2SE
import java.util.List;

// Seagis
import fr.ird.database.Entry;


/**
 * Un terme dans un mod�le lin�aire. Un mod�le lin�aire peut s'�crire de la forme suivante:
 *
 * <p align="center"><var>y</var> = <var>C</var><sub>0</sub> +
 * <var>C</var><sub>1</sub>&times;<var>x</var><sub>1</sub> +
 * <var>C</var><sub>2</sub>&times;<var>x</var><sub>2</sub> +
 * <var>C</var><sub>3</sub>&times;<var>x</var><sub>3</sub> + ...</p>
 *
 * Dans ce mod�le, le terme <var>C</var><sub>0</sub> est repr�sent� par un objet
 * <code>LinearModelTerm</code>, le terme <var>C</var><sub>1</sub>&times;<var>x</var><sub>1</sub>
 * par un autre objet <code>LinearModelTerm</code>, et ainsi de suite.
 *
 * Les variables ind�pendantes <var>x</var><sub>1</sub>, <var>x</var><sub>2</sub>, etc. sont
 * les {@linkplain DescriptorEntry descripteurs du paysage oc�anique}, eux-m�mes d�riv�s de
 * {@linkplain ParameterEntry param�tres environnementaux}.
 *
 * La variable d�pendante <var>y</var> sera stock�e dans un nouveau param�tre environnemental
 * (par exemple un param�tre appel� "potentiel de p�che"). Elle pourra donc servir d'entr�e �
 * un autre mod�le lin�aire.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see ParameterEntry#getLinearModel
 */
public interface LinearModelTerm {
    /**
     * Retourne le param�tre dans lequel seront stock�es les valeurs de la variable
     * d�pendante <var>y</var>. Notez que le terme <code>this</code> n'est qu'un des
     * termes composant ce param�tre. La liste compl�te peut �tre obtenue avec
     * <code>getTarget().{@linkplain ParameterEntry#getLinearModel() getLinearModel()}</code>.
     */
    public abstract ParameterEntry getTarget();

    /**
     * Retourne les descripteurs du paysage oc�anique composant ce terme. Par exemple, le terme
     * <code>this</code> pourrait �tre <var>C</var>&times;<code>SST</code>&times;<code>SLA</code>,
     * ou <var>C</var> est le {@linkplain #getCoefficient coefficient} d�termin� par la r�gression
     * lin�aire, tandis que <code>SST</code> et <code>SLA</code> sont les valeurs {@linkplain
     * DescriptorEntry#normalize normalis�es} de temp�rature de surface et d'anomalie de la hauteur
     * de l'eau respectivement. Pour cet exemple, <code>getDescriptors()</code> retournerait dans
     * une liste les deux descripteurs <code>SST</code> et <code>SLA</code>.
     */
    public abstract List<DescriptorEntry> getDescriptors();

    /**
     * Retourne le coefficient <var>C</var> de ce terme. Ce coefficient a g�n�ralement
     * �t� obtenu par une r�gression lin�aire multiple.
     */
    public double getCoefficient();
}
