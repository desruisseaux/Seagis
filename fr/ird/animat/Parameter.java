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
package fr.ird.animat;

// Entrés/sorties et logging
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.io.ObjectStreamException;

// JAI dependencies
import javax.media.jai.EnumeratedParameter;


/**
 * Un paramètre observé par les {@linkplain Animal animaux}. Le classe <code>Parameter</code>
 * ne contient pas les valeurs des observations,  mais donnent plutôt des indications sur ces
 * observations, un peu comme des noms de colonnes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Parameter extends EnumeratedParameter {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -6758126960652251313L;

    /**
     * Le cap des animaux, ainsi que leur position actuelle. Le cap peut être obtenu
     * par un appel à {@link #getValue}, alors que la position peut être obtenue par
     * un appel à {@link #getLocation}.
     */
    public static final Parameter HEADING = new Parameter("Heading", 0);

    /**
     * Construit un nouveau paramètre.
     *
     * @param name  Le nom du paramètre.
     * @param value La valeur de l'énumération (code réservé à un usage interne).
     */
    protected Parameter(final String name, final int value) {
        super(name, value);
    }

    /**
     * Retourne la valeur de l'observation spécifiée. L'implémentation par défaut
     * retourne <code>data[0]</code> à la condition que <code>data</code> ne soit
     * pas nul et aie une longueur d'au moins 1.
     *
     * @param  data Les observations.
     * @return La valeur de l'observation, ou {@link Double#NaN}.
     */
    public double getValue(final double[] data) {
        return (data!=null && data.length!=0) ? data[0] : Double.NaN;
    }

    /**
     * Retourne la position de l'observation spécifiée. L'implémentation par défaut retourne
     * (<code>data[1]</code>,<code>data[2]</code>) à la condition que <code>data</code> ne
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
     * Retourne le nom de ce paramètre. Cette méthode ne retourne que le nom afin de
     * permettre aux objets <code>Parameter</code> de s'insérer plus facilement dans
     * des composantes graphiques de <cite>Swing</cite> tel que des menus déroulants.
     */
    public String toString() {
        return getName();
    }

    /**
     * Retourne la classe qui contient les déclarations des constantes.
     * Après chaque lecture binaire d'un objet {@link ConstantParameter},
     * la méthode {@link #readResolve} remplacera <code>this</code> par la
     * constante qui porte le nom {@link #getName} dans la classe retournée.
     */
    protected Class getOwner() {
        return Parameter.class;
    }

    /**
     * Remplace <code>this</code> par la constante nommée {@link #getName}
     * dans la classe retournée par {@link #getOwner}. Cette méthode
     * permet de tester l'égalité de deux constantes avec l'opérateur
     * <code>==</code> après la lecture.
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
