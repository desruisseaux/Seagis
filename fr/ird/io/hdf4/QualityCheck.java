/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.hdf4;


/**
 * Classe ayant la charge d'effectuer un test de qualité d'une donnée.
 * Ce test est fait en vérifiant les bits d'un drapeau de qualité.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class QualityCheck
{
    /**
     * Ensembles de données
     */
    private final DataSet flags;

    /**
     * Construit un nouvel objet qui utilisera les
     * drapeaux de <code>flags</code> pour vérifier
     * la qualité des données.
     */
    protected QualityCheck(final DataSet flags)
    {
        this.flags=flags;
        if (flags==null)
            throw new NullPointerException();
    }

    /**
     * Indique si les données avec le drapeau <code>flag</code> sont acceptés.
     */
    protected abstract boolean accept(final int flag);

    /**
     * Indique si les données à l'index spécifié sont acceptés.
     */
    final boolean acceptIndex(final int index)
    {return accept(flags.getInteger(index));}

    /**
     * Indique si les dimensions de <code>this</code> sont
     * compatibles avec celles du {@link DataSet} spécifié.
     */
    final boolean sizeEquals(final DataSet dataSet)
    {return flags.sizeEquals(dataSet);}
}
