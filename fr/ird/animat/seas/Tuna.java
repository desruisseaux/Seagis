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
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

// Geotools
import org.geotools.resources.XArray;

// Impl�mentation de base
import fr.ird.animat.Clock;
import fr.ird.animat.Animal;
import fr.ird.animat.Species;
import fr.ird.animat.Environment;


/**
 * Repr�sentation d'un animal "thon". En plus d'�tre mobile,
 * cet animal est attentif aux signaux de son environnement.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Tuna extends Animal {
    /**
     * Rayon de perception de l'animal en m�tres.
     *
     * @see #getPerceptionArea
     */
    private static final double PERCEPTION_RADIUS = 20000;

    /**
     * Les valeurs des param�tres mesur�s. Chaque pas de temps peut mesurer plusieurs
     * param�tres, et chaque param�tre contient plusieurs �l�ments tels que la position
     * (<var>x</var>,<var>y</var>) en coordonn�es g�ographiques ainsi que la valeur
     * <var>value</var> du param�tre.
     */
    private float[] parameters = new float[8];

    /**
     * Longueur valide du tableau {@link #parameters}. Le nombre d'�l�ments est cette
     * longueur divis�e par <code>{@link #paramCount}*{@link #RECORD_LENGTH}</code>.
     */
    private int validLength;

    /**
     * Nombre de param�tres. Sa valeur sera d�termin�e lorsque la lecture des param�tres
     * aura commenc�e. En attendant, il est initialis� � 1 pour �viter des divisions par
     * z�ro dans le reste du code (par exemple {@link #getObservationCount}.
     */
    private int paramCount = 1;

    /**
     * Esp�ce � laquelle appartient cet animal.
     */
    private final Species species;

    /**
     * Construit un animal appartenant � l'esp�ce sp�cifi�. L'animal sera
     * initiallement positionn� � la position sp�cifi�e.
     *
     * @param species Esp�ce de l'animal.
     */
    public Tuna(final Species species, final Point2D position) {
        super(position);
        this.species = species;
    }

    /**
     * Retourne l'esp�ce � laquelle appartient cet animal.
     *
     * @return L'esp�ce � laquelle appartient cet animal.
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Retourne la r�gion jusqu'o� s'�tend la perception de cette
     * animal. Il peut s'agir par exemple d'un cercle centr� sur
     * la position de l'animal.
     *
     * @param  time La date pour laquelle on veut la r�gion per�ue,
     *         ou <code>null</code> pour la r�gion actuelle.
     * @param  La r�gion per�ue, ou <code>null</code> si la date
     *         sp�cifi�e n'est pas pendant la dur�e de vie de cet animal.
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
     *         si aucune observation n'a encore �t� faite � la
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
     * Observe l'environnement de l'animal. Cette m�thode doit �tre appel�e
     * avant {@link #move}, sans quoi l'animal ne saura pas comment se d�placer.
     *
     * @param environment L'environment � observer.
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
            throw new IllegalStateException("Attendait "+paramCount+" param�tres, "+
                                            "mais en a trouv�s "+perceptions.length);
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
     * D�place l'animal en fonction de son environnement. La m�thode
     * {@link #observe} doit avoir d'abord �t� appel�e, sans quoi
     * aucun d�placement ne sera fait (l'animal ne sachant pas o� aller).
     *
     * @param maximumDistance Distance maximale (en m�tres) que peut
     *        parcourir l'animal au cours de ce d�placement.
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
     * Retourne les valeurs des param�tres que per�oit l'animal sp�cifi�.
     * Ces valeurs d�pendront du rayon de perception de l'animal, tel que
     * retourn� par {@link Animal#getPerceptionArea}.
     *
     * @param  animal Animal pour lequel retourner les param�tres de
     *         l'environnement qui se trouvent dans son rayon de perception.
     * @return Les param�tres per�us, ou <code>null</code> s'il n'y en a pas.
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
    
    /** Retourne les observations de l'animal � la date sp�cifi�e. Le nombre de {@linkplain Parameter
     * param�tres} observ�s n'est pas n�cessairement �gal au nombre de param�tres de l'{@linkplain
     * Environment environnement}, car un animal peut ignorer les param�tres qui ne l'int�resse pas.
     * A l'inverse, un animal peut aussi faire quelques observations "internes" (par exemple la
     * temp�rature de ses muscles) qui ne font pas partie des param�tres de son environnement
     * externe. En g�n�ral, {@linkplain Parameter#HEADING le cap et la position} de l'animal font
     * partis des param�tres observ�s.
     *
     * @param  time Date pour laquelle on veut les observations,
     *         ou <code>null</code> pour les derni�res observations
     *         (c'est-�-dire celle qui ont �t� faites apr�s le dernier
     *         d�placement).
     * @return Les observations de l'animal, ou <code>null</code> si la
     *         date sp�cifi�e n'est pas pendant la dur�e de vie de cet animal.
     *         L'ensemble des cl�s ne comprend que les {@linkplain Parameter
     *         param�tres} qui int�ressent l'animal. Si un param�tre int�resse
     *         l'animal mais qu'aucune donn�e correspondante n'est disponible
     *         dans son environnement, alors les observations correspondantes
     *         seront <code>null</code>.
     *
     */
    public Map getObservations(Date time) {
    }
    
    /** Fait avancer l'animal pendant le laps de temps sp�cifi�. La vitesse � laquelle se
     * d�placera l'animal (et donc la distance qu'il parcourera) peuvent d�pendre de son
     * �tat ou des conditions environnementales. Le comportement de l'animal d�pendra de
     * l'impl�mentation. Il peut par exemple {@linkplain Path#rotate changer de cap}  et
     * {@linkplain Path#moveForward se d�placer vers ce cap}.  Il peut aussi {@linkplain
     * Path#moveToward se d�placer vers un certain point}, qu'il peut ne pas atteindre si
     * le laps de temps n'est pas suffisant.
     *
     * @param duration Dur�e du d�placement, en nombre de jours. Cette valeur est g�n�ralement
     *        la m�me que celle qui a �t� sp�cifi�e � {@link Population#evoluate}.
     *
     */
    public void move(float duration) {
    }
}
