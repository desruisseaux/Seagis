/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.io.hdf4;


/**
 * Classe ayant la charge d'effectuer un test de qualit� d'une donn�e.
 * Ce test est fait en v�rifiant les bits d'un drapeau de qualit�.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class QualityCheck
{
    /**
     * Ensembles de donn�es
     */
    private final DataSet flags;

    /**
     * Construit un nouvel objet qui utilisera les
     * drapeaux de <code>flags</code> pour v�rifier
     * la qualit� des donn�es.
     */
    protected QualityCheck(final DataSet flags)
    {
        this.flags=flags;
        if (flags==null)
            throw new NullPointerException();
    }

    /**
     * Indique si les donn�es avec le drapeau <code>flag</code> sont accept�s.
     */
    protected abstract boolean accept(final int flag);

    /**
     * Indique si les donn�es � l'index sp�cifi� sont accept�s.
     */
    final boolean acceptIndex(final int index)
    {return accept(flags.getInteger(index));}

    /**
     * Indique si les dimensions de <code>this</code> sont
     * compatibles avec celles du {@link DataSet} sp�cifi�.
     */
    final boolean sizeEquals(final DataSet dataSet)
    {return flags.sizeEquals(dataSet);}
}
