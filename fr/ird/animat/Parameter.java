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
package fr.ird.animat;

// Entr�s/sorties et logging
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.io.ObjectStreamException;

// JAI dependencies
import javax.media.jai.EnumeratedParameter;


/**
 * Un param�tre observ� par les {@linkplain Animal animaux}. Le classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plut�t des indications sur ces
 * observations, un peu comme des noms de colonnes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Parameter extends EnumeratedParameter {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -6758126960652251313L;

    /**
     * Le cap des animaux, ainsi que leur position actuelle. Le cap peut �tre obtenu
     * par un appel � {@link #getValue}, alors que la position peut �tre obtenue par
     * un appel � {@link #getLocation}.
     */
    public static final Parameter HEADING = new Parameter("Heading", 0);

    /**
     * Construit un nouveau param�tre.
     *
     * @param name  Le nom du param�tre.
     * @param value La valeur de l'�num�ration (code r�serv� � un usage interne).
     */
    protected Parameter(final String name, final int value) {
        super(name, value);
    }

    /**
     * Retourne la valeur de l'observation sp�cifi�e. L'impl�mentation par d�faut
     * retourne <code>data[0]</code> � la condition que <code>data</code> ne soit
     * pas nul et aie une longueur d'au moins 1.
     *
     * @param  data Les observations.
     * @return La valeur de l'observation, ou {@link Double#NaN}.
     */
    public double getValue(final double[] data) {
        return (data!=null && data.length!=0) ? data[0] : Double.NaN;
    }

    /**
     * Retourne la position de l'observation sp�cifi�e. L'impl�mentation par d�faut retourne
     * (<code>data[1]</code>,<code>data[2]</code>) � la condition que <code>data</code> ne
     * soit pas nul et aie une longueur d'au moins 3.
     *
     * @param  data Les observations.
     * @return La position de l'observation, ou <code>null</code>.
     */
    public Point2D getLocation(final double[] data) {
        if (data!=null && data.length>=3) {
            final double x = data[1];
            final double y = data[2];
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                return new Point2D.Double(x,y);
            }
        }
        return null;
    }

    /**
     * Retourne le nom de ce param�tre. Cette m�thode ne retourne que le nom afin de
     * permettre aux objets <code>Parameter</code> de s'ins�rer plus facilement dans
     * des composantes graphiques de <cite>Swing</cite> tel que des menus d�roulants.
     */
    public String toString() {
        return getName();
    }

    /**
     * Retourne la classe qui contient les d�clarations des constantes.
     * Apr�s chaque lecture binaire d'un objet {@link ConstantParameter},
     * la m�thode {@link #readResolve} remplacera <code>this</code> par la
     * constante qui porte le nom {@link #getName} dans la classe retourn�e.
     */
    protected Class getOwner() {
        return Parameter.class;
    }

    /**
     * Remplace <code>this</code> par la constante nomm�e {@link #getName}
     * dans la classe retourn�e par {@link #getOwner}. Cette m�thode
     * permet de tester l'�galit� de deux constantes avec l'op�rateur
     * <code>==</code> apr�s la lecture.
     */
    protected Object readResolve() throws ObjectStreamException {
        final Class owner = getOwner();
        final String name = getName();
        try {
            return owner.getField(name).get(null);
        } catch (Exception exception) {
            final LogRecord record = new LogRecord(Level.WARNING, "Unknow constant: "+super.toString());
            record.setSourceClassName(owner.getName());
            record.setSourceMethodName("readResolve");
            record.setThrown(exception);
            Logger.getLogger("fr.ird.animat").log(record);
            return this;
        }
    }
}
