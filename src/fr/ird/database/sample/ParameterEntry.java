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
 */
package fr.ird.database.sample;

// J2SE
import java.util.List;

// Seagis
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;


/**
 * Un param�tre environnemental. Les param�tres sont souvent associ�s � une
 * {@linklplain SeriesEntry s�rie} de la base de donn�es d'images. Un param�tre
 * environnemental peut �tre par exemple la temp�rature de surface de la mer, ou
 * la concentration en chlorophylle-a.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getParameters
 */
public interface ParameterEntry extends Entry {
    /**
     * Retourne un num�ro unique identifiant ce param�tre.
     */
    public abstract int getID();

    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public abstract String getRemarks();

    /**
     * Retourne <code>true</code> si ce param�tre est le <cite>param�tre identit�</cite>.
     * Le "param�tre identit�" est un param�tre artificiel repr�sentant une image
     * dont tous les pixels auraient la valeur 1. Il est utilis� dans des expressions de
     * la forme <code>y = C0 + C1*x + C2*x� + ...</code>, ou <code>C0</code> peut s'�crire
     * <code>C0&times;identit�</code>.
     */
    public abstract boolean isIdentity();

    /**
     * Retourne la s�rie d'image � utiliser pour ce param�tre.
     *
     * @param n 0 pour la s�rie principale, ou 1 pour la s�rie de rechange � utiliser si
     *          jamais la s�rie principale n'est pas disponible.
     * @return  La s�rie d'images, ou <code>null</code> si aucune.
     *          Cette r�f�rence ne sera jamais nulle si <code>n=0</code>.
     */
    public abstract SeriesEntry getSeries(int n);

    /**
     * Retourne le num�ro de la bande � utiliser dans les images.
     */
    public abstract int getBand();

    /**
     * Retourne les termes d'un mod�le lin�aire calculant ce param�tre, ou <code>null</code>
     * s'il n'y a pas de mod�le lin�aire. Un param�tre peut �tre le r�sultat d'une combinaison
     * de d'autres param�tres, par exemple sous la forme de l'�quation suivante:
     *
     * <p align="center"><code>PP</code> = <var>C</var><sub>0</sub> +
     * <var>C</var><sub>1</sub>&times;<code>SST</code> +
     * <var>C</var><sub>2</sub>&times;<code>SLA</code> +
     * <var>C</var><sub>3</sub>&times;<code>SST</code>&times;<code>SLA</code> + ...</p>
     *
     * Chacun des termes � droite du signe = est d�crit par un objet {@link LinearModelTerm}.
     * Ces descriptions incluent le coefficient <var>C</var><sub>n</sub>, qui r�sulte
     * g�n�ralement d'une r�gression lin�aire multiple.
     *
     * @return La liste de tous les termes composant le mod�le lin�aire,
     *         ou <code>null</code> s'il n'y en a pas. Cette liste est immutable.
     */
    public abstract List<? extends LinearModelTerm> getLinearModel();
}
