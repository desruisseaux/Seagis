/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.seas;

// J2SE standard
import java.util.Arrays;
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

// Geotools
import org.geotools.resources.XArray;

// Implémentation de base
import fr.ird.animat.Clock;
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;


/**
 * Représentation d'un animal "thon". En plus d'être mobile,
 * cet animal est attentif aux signaux de son environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Tuna extends Animal {
    /**
     * Rayon de perception de l'animal en mètres.
     *
     * @see #getPerceptionArea
     */
    private static final double PERCEPTION_RADIUS = 20000;

    /**
     * Les valeurs des paramètres mesurés. Chaque pas de temps peut mesurer plusieurs
     * paramètres, et chaque paramètre contient plusieurs éléments tels que la position
     * (<var>x</var>,<var>y</var>) en coordonnées géographiques ainsi que la valeur
     * <var>value</var> du paramètre.
     */
    private float[] parameters = new float[8];

    /**
     * Longueur valide du tableau {@link #parameters}. Le nombre d'éléments est cette
     * longueur divisée par <code>{@link #paramCount}*{@link #RECORD_LENGTH}</code>.
     */
    private int validLength;

    /**
     * Nombre de paramètres. Sa valeur sera déterminée lorsque la lecture des paramètres
     * aura commencée. En attendant, il est initialisé à 1 pour éviter des divisions par
     * zéro dans le reste du code (par exemple {@link #getObservationCount}.
     */
    private int paramCount = 1;

    /**
     * Espèce à laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Construit un animal appartenant à l'espèce spécifié. L'animal sera
     * initiallement positionné à la position spécifiée.
     *
     * @param species Espèce de l'animal.
     */
    public Tuna(final Species species, final Point2D position) {
        super(position);
        this.species = species;
    }

    /**
     * Retourne l'espèce à laquelle appartient cet animal.
     *
     * @return L'espèce à laquelle appartient cet animal.
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Retourne la région jusqu'où s'étend la perception de cette
     * animal. Il peut s'agir par exemple d'un cercle centré sur
     * la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la région perçue,
     *         ou <code>null</code> pour la région actuelle.
     * @param  La région perçue, ou <code>null</code> si la date
     *         spécifiée n'est pas pendant la durée de vie de cet animal.
     */
    public Shape getPerceptionArea(final Date time) {
        final double radius = PERCEPTION_RADIUS;
        final Ellipse2D area = new Ellipse2D.Double(-radius, -radius, 2*radius, 2*radius);
        path.relativeToGeographic(area);
        return area;
    }

    /**
     * Retourne les observations de l'animal.
     *
     * @return Les observations de l'animal, ou <code>null</code>
     *         si aucune observation n'a encore été faite à la
     *         position actuelle de l'animal.
     */
    public ParameterValue[] getObservations()
    {
        int pos = getObservationCount();
        if (pos!=0 && pos==getPointCount())
        {
            pos = (pos-1) * (paramCount*RECORD_LENGTH);
            final ParameterValue[] values = new ParameterValue[paramCount];
            for (int i=0; i<paramCount; i++)
            {
                final float x = parameters[pos + LONGITUDE];
                final float y = parameters[pos +  LATITUDE];
                final float z = parameters[pos +     VALUE];
                final ParameterValue.Float param = new ParameterValue.Float("param #"+(i+1)); // TODO
                param.setValue(z,x,y);
                values[i] = param;
                pos += RECORD_LENGTH;
            }
            return values;
        }
        return null;
    }

    /**
     * Observe l'environnement de l'animal. Cette méthode doit être appelée
     * avant {@link #move}, sans quoi l'animal ne saura pas comment se déplacer.
     *
     * @param environment L'environment à observer.
     */
    public void observe(final Environment environment)
    {
        final ParameterValue[] perceptions = environment.getParameters(this);
        if (validLength == 0)
        {
            paramCount = perceptions.length;
        }
        else if (paramCount != perceptions.length)
        {
            throw new IllegalStateException("Attendait "+paramCount+" paramètres, "+
                                            "mais en a trouvés "+perceptions.length);
        }
        final int requiredLength = getPointCount()*(paramCount*RECORD_LENGTH);
        if (requiredLength > parameters.length)
        {
            parameters = XArray.resize(parameters, requiredLength + Math.min(requiredLength, 4096));
            Arrays.fill(parameters, validLength, parameters.length, Float.NaN);
        }
        if (requiredLength != 0) // 'false' if initial position is not set.
        {
            validLength = requiredLength - (paramCount*RECORD_LENGTH);
            for (int i=0; i<paramCount; i++)
            {
                final ParameterValue value = perceptions[i];
                if (value != null)
                {
                    Point2D pos = value.getLocation();
                    if (pos!=null)
                    {
                        parameters[validLength + LONGITUDE] = (float) pos.getX();
                        parameters[validLength +  LATITUDE] = (float) pos.getY();
                    }
                    parameters[validLength + VALUE] = (float) value.getValue();
                }
                validLength += RECORD_LENGTH;
            }
        }
        assert validLength == requiredLength;
    }

    /**
     * Déplace l'animal en fonction de son environnement. La méthode
     * {@link #observe} doit avoir d'abord été appelée, sans quoi
     * aucun déplacement ne sera fait (l'animal ne sachant pas où aller).
     *
     * @param maximumDistance Distance maximale (en mètres) que peut
     *        parcourir l'animal au cours de ce déplacement.
     */
    public void move(final double maximumDistance)
    {
        final ParameterValue[] values = getObservations();
        if (values != null)
        {
            final ParameterValue param = values[values.length-1];
            if (param!=null)
            {
                final Point2D location = param.getLocation();
                if (location != null)
                {
                    moveToward(maximumDistance, location);
                }
            }
        }
    }

    /**
     * Retourne les valeurs des paramètres que perçoit l'animal spécifié.
     * Ces valeurs dépendront du rayon de perception de l'animal, tel que
     * retourné par {@link Animal#getPerceptionArea}.
     *
     * @param  animal Animal pour lequel retourner les paramètres de
     *         l'environnement qui se trouvent dans son rayon de perception.
     * @return Les paramètres perçus, ou <code>null</code> s'il n'y en a pas.
     */
    public ParameterValue[] getParameters(final Animal animal)
    {
        int index = 0;
        final ParameterValue[] values = new ParameterValue[parameterCount];
        for (int i=0; i<coverages.length; i++)
        {
            final GridCoverage gc = coverages[i].getGridCoverage2D(time);
            if (gc != null)
            {
                final Shape area = animal.getPerceptionArea(condition);
                final ParameterValue[] toCopy = evaluator.evaluate(gc, area);
                System.arraycopy(toCopy, 0, values, index, toCopy.length);
                index += toCopy.length;
            }
            else
            {
                index += coverages[i].getNumSampleDimensions();
            }
        }
        assert index == values.length;
        return values;
    }
    
    /** Retourne les observations de l'animal à la date spécifiée. Le nombre de {@linkplain Parameter
     * paramètres} observés n'est pas nécessairement égal au nombre de paramètres de l'{@linkplain
     * Environment environnement}, car un animal peut ignorer les paramètres qui ne l'intéresse pas.
     * A l'inverse, un animal peut aussi faire quelques observations "internes" (par exemple la
     * température de ses muscles) qui ne font pas partie des paramètres de son environnement
     * externe. En général, {@linkplain Parameter#HEADING le cap et la position} de l'animal font
     * partis des paramètres observés.
     *
     * @param  time Date pour laquelle on veut les observations,
     *         ou <code>null</code> pour les dernières observations
     *         (c'est-à-dire celle qui ont été faites après le dernier
     *         déplacement).
     * @return Les observations de l'animal, ou <code>null</code> si la
     *         date spécifiée n'est pas pendant la durée de vie de cet animal.
     *         L'ensemble des clés ne comprend que les {@linkplain Parameter
     *         paramètres} qui intéressent l'animal. Si un paramètre intéresse
     *         l'animal mais qu'aucune donnée correspondante n'est disponible
     *         dans son environnement, alors les observations correspondantes
     *         seront <code>null</code>.
     *
     */
    public Map getObservations(Date time) {
    }
    
    /** Fait avancer l'animal pendant le laps de temps spécifié. La vitesse à laquelle se
     * déplacera l'animal (et donc la distance qu'il parcourera) peuvent dépendre de son
     * état ou des conditions environnementales. Le comportement de l'animal dépendra de
     * l'implémentation. Il peut par exemple {@linkplain Path#rotate changer de cap}  et
     * {@linkplain Path#moveForward se déplacer vers ce cap}.  Il peut aussi {@linkplain
     * Path#moveToward se déplacer vers un certain point}, qu'il peut ne pas atteindre si
     * le laps de temps n'est pas suffisant.
     *
     * @param duration Durée du déplacement, en nombre de jours. Cette valeur est généralement
     *        la même que celle qui a été spécifiée à {@link Population#evoluate}.
     *
     */
    public void move(float duration) {
    }
}
