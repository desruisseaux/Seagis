/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat.seas;

// J2SE standard
import java.util.Arrays;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.resources.XArray;

// Divers
import fr.ird.animat.Environment;
import fr.ird.operator.coverage.ParameterValue;


/**
 * Objet qui, en plus d'�tre mobile, est attentif aux signaux de son
 * environnement.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class AttentiveObject extends MobileObject
{
    /**
     * Nombre de param�tres.
     */
    private static final int paramCount = 1;

    /**
     * Les valeurs des param�tres mesur�s. Chaque pas de temps peux mesurer
     * plusieurs param�tres, et chaque param�tres contient trois �l�ments:
     * la position (<var>x</var>,<var>y</var>) en coordonn�es g�ographiques
     * ainsi que la valeur <var>value</var> du param�tre.
     */
    private float[] parameters = new float[8];

    private int validLength;

    /**
     * Nombre d'�l�ments dans chaque enregistrement <code>parameters</code>.
     */
    private static final int RECORD_LENGTH = 3;

    /**
     * Construit un objet mobile qui n'a pas de position initiale.
     * Appellez {@link #setLocation} apr�s la construction de cet
     * objet pour lui affecter une position.
     */
    public AttentiveObject()
    {
        Arrays.fill(parameters, Float.NaN);
    }

    /**
     */
    public void observe(final Environment environment)
    {
        final ParameterValue[] perceptions = environment.getParameters(null); // TODO
        final int requiredLength = paramCount*RECORD_LENGTH * (getPointCount()+1);
        if (requiredLength > parameters.length)
        {
            parameters = XArray.resize(parameters, requiredLength + Math.min(requiredLength, 4096));
            Arrays.fill(parameters, validLength, parameters.length, Float.NaN);
        }
        for (int i=0; i<paramCount; i++)
        {
            Point2D pos = getLocation();
            final ParameterValue value = perceptions[i];
            parameters[validLength++]  = (float) (pos.getX()-0.2*(0.5+Math.random()));
            parameters[validLength++]  = (float) (pos.getY()-0.2*(0.5+Math.random()));
            parameters[validLength++]  = (float) value.getValue();
        }
    }
}
