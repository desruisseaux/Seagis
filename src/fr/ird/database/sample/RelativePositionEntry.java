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
import java.util.Date;
import java.awt.geom.Point2D;

// Seagis
import fr.ird.database.Entry;


/**
 * Position spatio-temporelle relative � un �chantillon ({@linkp SampleEntry}}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @see SampleDataBase#getRelativePositions
 */
public interface RelativePositionEntry extends Entry {
    /**
     * Retourne un num�ro unique identifiant cette proc�dure.
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
     * Retourne la date � laquelle �valuer l'environnement relativement � l'�chantillon
     * sp�cifi�. On pourrait par exemple �tre int�ress�s � la hauteur de l'eau 15 jours
     * avant une p�che.
     */
    public abstract Date getTime(final SampleEntry sample);

    /**
     * Retourne la coordonn�es g�ographiques � laquelle �valuer l'environnement pour
     * l'�chantillon sp�cifi�. Il s'agit souvent (mais pas obligatoirement) de la
     * coordonn�es de l'�chantillon lui-m�me.
     */
    public abstract Point2D getCoordinate(final SampleEntry sample);

    /**
     * Applique le d�placement relatif sur les coordonn�es spatio-temporelles sp�cifi�es.
     * La coordonn�e spatiale et la date sont habituellement obtenues par
     * {@link SampleEntry#getCoordinate} et {@link SampleEntry#getTime} respectivement.
     * Toutefois, si l'�chantillon {@link SampleEntry} est disponible, alors il vaut mieux
     * appeler les m�thodes {@link #getCoordinate} et {@link #getTime} de cet objet, car elles
     * peuvent faire un travail plus �labor� en fonction de la classe de l'�chantillon.
     *
     * @param coordinate La position spatiale de l'�chantillon, ou <code>null</code>.
     *                   La nouvelle position �crasera la position courante dans cet objet.
     * @param time       La date de l'�chantillon, ou <code>null</code>.
     *                   La nouvelle date �crasera la date courante dans cet objet.
     *
     * @see #getCoordinate
     * @see #getTime
     */
    public abstract void applyOffset(final Point2D coordinate, final Date time);

    /**
     * Applique le m�me d�placement que <code>applyOffset(...)</code>, mais dans la
     * direction oppos�e.
     *
     * @param coordinate La position spatiale ou <code>null</code>.
     *                   La nouvelle position �crasera la position courante dans cet objet.
     * @param time       La date ou <code>null</code>.
     *                   La nouvelle date �crasera la date courante dans cet objet.
     */
    public abstract void applyOppositeOffset(final Point2D coordinate, final Date time);

    /**
     * Retourne l'�cart de temps typique entre les �chantillons et la date � laquelle �valuer le
     * param�tre environnemental. Cet �cart de temps n'est qu'� titre indicatif et n'a pas � �tre
     * pr�cis; la mani�re la plus pr�cise d'obtenir la date pour un �chantillon reste la m�thode
     * {@link #getTime}. La m�thode <code>getTypicalTimeOffset()</code> ne sert qu'� r�duire les
     * temps de calculs en planifiant d'une mani�re plus optimale l'ordre et la fr�quence dans
     * lesquelles les images seront lues.
     *
     * @return Un d�calage de temps typique, en nombre de jours.
     */
    public abstract float getTypicalTimeOffset();

    /**
     * Indique si cette position relative devrait �tre s�lectionn�e par d�faut.
     * Cette information peut �tre utilis�e dans une interface utilisateur afin
     * de pr�-selectionner un jeu de positions courrament utilis�.
     */
    public abstract boolean isDefault();
}
