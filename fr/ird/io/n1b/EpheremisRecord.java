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
package fr.ird.io.n1b;

// J2SE dependencies
import java.text.NumberFormat;
import java.text.ParseException;

// Geotools dependencies
import org.geotools.pt.CoordinatePoint;
import org.geotools.resources.Utilities;
import org.geotools.units.Unit;


/**
 * Un enregistrement des éphémérides terrestres. Cet enregistrement contient la position
 * et la vitesse du satellite à un instant donné. Cette position et vitesse est exprimée
 * dans un système de coordonnées géocentriques centré sur la Terre.
 * 
 * @version $Id$
 * @author Remi EVE
 * @author Martin Desruisseaux
 */
final class EpheremisRecord
{
    /**
     * Temps en millisecondes écoulé entre deux relevés de la position du satellite
     * (c'est-à-dire deux objets {@link EpheremisRecord} consécutifs).
     */
    public static final int TIME_INTERVAL = 120 * 1000;

    /**
     * Constante indiquant que le satellite se trouve en plein jour.
     */
    public static final int DAY_LIGHT  = 0;

    /**
     * Constante indiquant que le satellite se trouve en pleine nuit.
     */
    public static final int NIGHT_TIME = 1;

    /**
     * Constante indiquant que le satellite se trouve dans la pénombre.
     * Ce n'est pas tout-à-fait le jour ni la nuit.
     */
    public static final int PENOMBRA = 2;

    /**
     * Le numéro de cet enregistrement. Ce numéro augmente de 1 pour chaque
     * nouvelle ligne (enregistrement) dans le fichier de bulletin de CLS.
     */
    private final int step;

    /**
     * Le numéro de l'orbite pour cet enregistrement.
     * Ce numéro change peu dans un même bulletin.
     */
    private final int orbitNumber;

    /**
     * La lumière ambiante du satellite. Ce champ doit être une des constantes
     * suivantes: {@link #DAY_LIGHT}, {@link #NIGHT_TIME} ou {@link #PENOMBRA}.
     */
    private final int light;

    /**
     * Indique si le satellite est visible à partir de la station.
     * <strong>NOTE:</strong> Dans les bulletin de CLS, la valeur
     * <code>true</code> est représentéé par le nombre "0", tandis
     * que la valeur <code>false</code> est représentée par le
     * nombre "1".
     */
    private final boolean visible;

    /**
     * Composantes (<var>x</var>, <var>y</var>, <var>z</var>) de la position du
     * satellite, en kilomètres. Cette position est exprimée selon le système de
     * coordonnées géocentrique {@link #cs}.
     */
    private final double x, y, z;

    /**
     * Composantes (<var>u</var>, <var>v</var> et <var>w</var>) de la vitesse du
     * satellite, en kilomètres par seconde. Cette vitesse est exprimée selon le
     * système de coordonnées géocentrique {@link #cs}.
     */
    private final double u, v, w;

    /**
     * Construit un enregistrement de l'éphéremis. Le tableau spécifié
     * en argument doit contenir dans l'ordre les valeurs suivantes:
     *
     * <ul>
     *   <li>Le numéro de l'enregistrement</li>
     *   <li>La coordonnée <var>x</var> de la position géocentrique (km)</li>
     *   <li>La coordonnée <var>y</var> de la position géocentrique (km)</li>
     *   <li>La coordonnée <var>z</var> de la position géocentrique (km)</li>
     *   <li>La composante <var>u</var> de la vitesse  géocentrique (km/s)</li>
     *   <li>La composante <var>v</var> de la vitesse  géocentrique (km/s)</li>
     *   <li>La composante <var>w</var> de la vitesse  géocentrique (km/s)</li>
     *   <li>Le numéro de l'orbite.</li>
     *   <li>Une des constantes {@link #DAY_LIGHT}, {@link #NIGHT_TIME} ou {@link #PENOMBRA}</li>
     *   <li>0 si le satellite est visible à partir de la station, ou 1 sinon.</li>
     * </ul>
     *
     * @throws ParseException si un nombre fractionnaire a été trouvé alors qu'un nombre
     *         entier était attendu. Ce type d'exception est lancé parce que ce constructeur
     *         est généralement appelé à partir de <code>Bulletin.load(...)</code>
     */ 
    EpheremisRecord(final double[] record) throws ParseException
    {
        int i=0;
        step        = cast(record[i++]);
        x           =     (record[i++]);
        y           =     (record[i++]);
        z           =     (record[i++]);
        u           =     (record[i++]);
        v           =     (record[i++]);
        w           =     (record[i++]);
        orbitNumber = cast(record[i++]);
        light       = cast(record[i++]);
        visible     = cast(record[i++]) == 0;
        if (i != record.length)
        {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Convertit un nombre réel un entier. Si la conversion échoue,
     * alors cette méthode lance une {@link ParseException}.
     */
    private static int cast(final double value) throws ParseException
    {
        final int integer = (int) value;
        if (integer == value)
        {
            return integer;
        }
        throw new ParseException(value+" n'est pas un nombre entier.", 0);
    }

    /**
     * Retourne le numero (step) de ces donnees ephemerides.
     */ 
    public int getStep()
    {
        return step;
    }

    /**
     * Le numéro de l'orbite pour cet enregistrement.
     * Ce numéro change peu dans un même bulletin.
     */
    public int getOrbitNumber()
    {
        return orbitNumber;
    }

    /**
     * Retourne la lumière ambiante du satellite. La valeur retournée sera une des constantes
     * suivantes: {@link #DAY_LIGHT}, {@link #NIGHT_TIME} ou {@link #PENOMBRA}.
     */
    public int getLight()
    {
        return light;
    }

    /**
     * Indique si le satellite est visible à partir de la station.
     */
    public boolean isVisible()
    {
        return visible;
    }

    /**
     * Retourne la position du satellite. Cette position est exprimée
     * selon le système de coordonnées {@link #getCoordinateSystem}.
     */
    public CoordinatePoint getPosition()
    {
        return new CoordinatePoint(x,y,z);
    }

    /**
     * Retourne la vitesse du satellite. Cette vitesse est exprimée
     * selon le système de coordonnées {@link #getCoordinateSystem}.
     */ 
    public double[] getSpeed()
    {
        return new double[] {u,v,w};
    }

    /**
     * Ecrit une chaîne de caractères dans le buffer specifié.
     *
     * @param value  Chaîne de caractères à écrire.
     * @param buffer Le buffer dans lequel écrire la chaîne.
     * @param width  Le nombre minimal d'espaces que doit contenir le champs.
     *               Si ce nombre est négatif, alors la valeur sera alignée à
     *               droite plutôt qu'à gauche.
     */
    private static void format(final String value, final StringBuffer buffer, int width)
    {
        final String spaces = Utilities.spaces(Math.abs(width) - value.length());
        if (width<0) buffer.append(spaces);
        buffer.append(value);
        if (width>0) buffer.append(spaces);
    }

    /**
     * Retourne une chaîne de caracètres représentant les donnees de cet objet.
     */
    public String toString()
    {
        final String light;
        switch (this.light) {
            case DAY_LIGHT:  light = "day";      break;
            case NIGHT_TIME: light = "night";    break;
            case PENOMBRA:   light = "penombra"; break;
            default:         light = "?";        break;
        }
        final StringBuffer buffer = new StringBuffer();
        final NumberFormat format = NumberFormat.getNumberInstance();
        format(format.format (orbitNumber), buffer, -6);
        format(format.format (step),        buffer, -8);
        format(String.valueOf(light),       buffer,  9);
        format(String.valueOf(visible),     buffer,  6);
        format.setMinimumFractionDigits(3);
        format.setMaximumFractionDigits(3);
        format(format.format(x), buffer, -12);
        format(format.format(y), buffer, -10);
        format(format.format(z), buffer, -10);
        format.setMinimumFractionDigits(6);
        format.setMaximumFractionDigits(6);
        format(format.format(u), buffer, -12);
        format(format.format(v), buffer, -10);
        format(format.format(w), buffer, -10);
        return buffer.toString();
    }
}
