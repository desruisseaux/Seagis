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
package fr.ird.sql.fishery;

// Geometry
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Collections
import java.util.Set;

// Formatting
import java.text.DateFormat;
import java.text.FieldPosition;

// Formatting (Geotools)
import org.geotools.pt.Latitude;
import org.geotools.pt.Longitude;
import org.geotools.pt.AngleFormat;

// Miscellaneous
import fr.ird.animat.Species;


/**
 * Classe de base des captures.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class AbstractCatchEntry extends SpeciesSet implements CatchEntry {
    /**
     * Objet à utiliser par défaut pour les écritures des dates.
     */
    private static DateFormat dateFormat;

    /**
     * Objet à utiliser par défaut pour les écritures des coordonnées.
     */
    private static AngleFormat angleFormat;

    /**
     * Index de la dernière espèce interrogée.
     * Utilisé pour optimiser {@link #getCatch(Species)}.
     */
    private transient int last;

    /**
     * Numéro d'identification de la pêche.
     */
    protected final int ID;

    /**
     * Quantité de poissons capturés pour chaque espèce. Ce tableau devra
     * obligatoirement avoir une longueur égale au nombre d'espèces.
     */
    protected final float[] amount;

    /**
     * Construit une capture. Ce constructeur initialise le tableau {@link #amount},
     * mais ne lui affecte pas de valeurs. Il est du ressort des constructeurs des
     * classes dérivées d'affecter des valeurs aux éléments de {@link #amount}.
     *
     * @param ID Numéro identifiant cette capture.
     * @param species Espèce composant cette capture.
     *        <strong>Ce tableau ne sera pas cloné</strong>.
     *        Evitez donc de le modifier après la construction.
     */
    protected AbstractCatchEntry(final int ID, final Species[] species) {
        super(species);
        this.ID     = ID;
        this.amount = new float[species.length];
    }

    /**
     * Retourne un numéro identifiant cette capture. Ce numéro doit être
     * unique pour une même base de données  (il peut être nécessaire de
     * redéfinir cette méthode pour ça,  en particulier pour les données
     * des senneurs  qui  peuvent combiner le numéro de la marée avec un
     * numéro de la capture dans cette marée).
     */
    public int getID() {
        return ID;
    }

    /**
     * Retourne un nom par défaut pour cette entrée. L'implémentation par défaut
     * retourne le numéro {@link #ID}. Les classes dérivées pourraient redéfinir
     * cette méthode pour retourner par exemple le numéro de marée suivit du
     * numéro de la capture dans cette marée.
     */
    public String getName() {
        return String.valueOf(ID);
    }

    /**
     * Retourne toujours <code>null</code>.
     */
    public String getRemarks() {
        return null;
    }

    /**
     * Retourne l'espèce la plus pêchée dans cette capture. Si aucune espèce
     * n'a été capturée, alors cette méthode retourne <code>null</code>.
     */
    public Species getDominantSpecies() {
        Species dominant = null;
        float max=Float.NEGATIVE_INFINITY;
        for (int i=amount.length; --i>=0;) {
            if (amount[i] >= max) {
                max = amount[i];
                dominant = species[i];
            }
        }
        return dominant;
    }

    /**
     * Retourne l'ensemble des espèces pêchées. Il n'est pas obligatoire
     * que {@link #getCatch(Species)} retourne une valeur différente de
     * zéro pour chacune de ces espèces.
     */
    public final Set<Species> getSpecies() {
        return this;
    }

    /**
     * Retourne la quantité de poissons pêchés pour une espèce donnée.
     */
    public final float getCatch(final Species species) {
        // Fast check (slight optimization)
        final Species[] array=this.species;
        if (++last == array.length) last=0;
        if (array[last]==species) {
            return amount[last];
        }
        // Normal (slower) check.
        for (int i=0; i<array.length; i++) {
            if (array[i].equals(species)) {
                return amount[last=i];
            }
        }
        return 0;
    }

    /**
     * Retourne la quantité totale de poissons pêchés, toutes espèces confondues.
     * La quantité retournée par cette méthode est la somme des quantitées
     * <code>{@link #getCatch getCatch}(i)</code> où <var>i</var> varie de 0
     * inclusivement jusqu'à <code>{@link #getSpecies()}.size()</code> exclusivement.
     */
    public final float getCatch() {
        double total=0;
        for (int i=amount.length; --i>=0;) {
            total += amount[i];
        }
        return (float) total;
    }

    /**
     * Si cette capture ne contient pas les coordonnées de début
     * et de fin de la ligne, ramène la position spécifiée à une
     * des valeurs {@link EnvironmentTable#START_POINT} ou
     * {@link EnvironmentTable#END_POINT} en fonction de la
     * coordonnée disponible.
     */
    int clampPosition(final int pos) {
        return pos;
    }

    /**
     * Retourne un code représentant cette capture.
     */
    public final int hashCode() {
        return ID;
    }

    /**
     * Retourne une chaîne de caractères
     * représentant cette capture.
     */
    public String toString() {
        if (dateFormat ==null)  dateFormat=DateFormat.getDateInstance();
        if (angleFormat==null) angleFormat=new AngleFormat();
        final FieldPosition dummy = new FieldPosition(0);
        final StringBuffer buffer = new StringBuffer("CatchEntry[");
        final Point2D  coordinate = getCoordinate();
        dateFormat .format(getTime(),                        buffer, dummy); buffer.append(", ");
        angleFormat.format(new Latitude (coordinate.getY()), buffer, dummy); buffer.append(' ');
        angleFormat.format(new Longitude(coordinate.getX()), buffer, dummy); buffer.append(']');
        return buffer.toString();
    }
}
