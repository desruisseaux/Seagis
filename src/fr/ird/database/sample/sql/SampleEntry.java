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
package fr.ird.database.sample.sql;

// J2SE dependencies
import java.util.Set;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Formatting (Geotools)
import org.geotools.pt.Latitude;
import org.geotools.pt.Longitude;
import org.geotools.pt.AngleFormat;

// Miscellaneous
import fr.ird.animat.Species;
import fr.ird.database.sample.CruiseEntry;


/**
 * Classe de base des échantillons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class SampleEntry extends SpeciesSet implements fr.ird.database.sample.SampleEntry {
    /**
     * Numéro de série pour compatibilité entre différentes versions.
     */
    private static final long serialVersionUID = -7400603494537440922L;

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
     * Utilisé pour optimiser {@link #getValue(Species)}.
     */
    private transient int last;

    /**
     * Numéro d'identification de l'échantillons.
     */
    protected final int ID;

    /**
     * La campagne d'échantillonage pendant laquelle a été pris cet échantillon.
     */
    private final CruiseEntry cruise;

    /**
     * Quantité mesuré (par exemple nombre de poissons capturés) pour chaque espèce.
     * Ce tableau devra obligatoirement avoir une longueur égale au nombre d'espèces.
     */
    protected final float[] amount;

    /**
     * Construit une entré. Ce constructeur initialise le tableau {@link #amount},
     * mais ne lui affecte pas de valeurs. Il est du ressort des constructeurs des
     * classes dérivées d'affecter des valeurs aux éléments de {@link #amount}.
     *
     * @param ID Numéro identifiant cet échantillons.
     * @param cruise La campagne d'échantillonage pendant laquelle a été pris cet échantillon.
     * @param species Espèce composant cet échantillons. <strong>Ce tableau ne sera pas cloné</strong>.
     *        Evitez donc de le modifier après la construction.
     */
    protected SampleEntry(final int ID, final CruiseEntry cruise, final Species[] species) {
        super(species);
        this.ID     = ID;
        this.cruise = cruise;
        this.amount = new float[species.length];
    }

    /**
     * Retourne un numéro identifiant cet échantillons. Ce numéro doit être unique pour une
     * même base de données (il peut être nécessaire de redéfinir cette méthode pour ça, en
     * particulier pour les données des senneurs qui peuvent combiner le numéro de la marée
     * avec un numéro de la capture dans cette marée).
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
     * {@inheritDoc}
     */
    public CruiseEntry getCruise() {
        return cruise;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public final Set<Species> getSpecies() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public final float getValue(final Species species) {
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
     * {@inheritDoc}
     */
    public final float getValue() {
        double total=0;
        for (int i=amount.length; --i>=0;) {
            total += amount[i];
        }
        return (float) total;
    }

    /**
     * Retourne un code représentant cet échantillon.
     */
    public final int hashCode() {
        return ID;
    }

    /**
     * Vérifie si cette entré est identique à l'objet spécifié.
     */
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            final SampleEntry that = (SampleEntry) object;
            return this.ID == that.ID &&
                   Arrays.equals(this.amount,  that.amount) &&
                   Arrays.equals(this.species, that.species);
        }
        return false;
    }

    /**
     * Retourne une chaîne de caractères représentant cet échantillon.
     */
    public String toString() {
        if (dateFormat ==null)  dateFormat=DateFormat.getDateInstance();
        if (angleFormat==null) angleFormat=new AngleFormat();
        final FieldPosition dummy = new FieldPosition(0);
        final StringBuffer buffer = new StringBuffer("SampleEntry[");
        final Point2D  coordinate = getCoordinate();
        dateFormat .format(getTime(),                        buffer, dummy); buffer.append(", ");
        angleFormat.format(new Latitude (coordinate.getY()), buffer, dummy); buffer.append(' ');
        angleFormat.format(new Longitude(coordinate.getX()), buffer, dummy); buffer.append(']');
        return buffer.toString();
    }
}
