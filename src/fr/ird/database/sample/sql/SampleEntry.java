/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 * Classe de base des �chantillons.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class SampleEntry extends SpeciesSet implements fr.ird.database.sample.SampleEntry {
    /**
     * Num�ro de s�rie pour compatibilit� entre diff�rentes versions.
     */
    private static final long serialVersionUID = -7400603494537440922L;

    /**
     * Objet � utiliser par d�faut pour les �critures des dates.
     */
    private static DateFormat dateFormat;

    /**
     * Objet � utiliser par d�faut pour les �critures des coordonn�es.
     */
    private static AngleFormat angleFormat;

    /**
     * Index de la derni�re esp�ce interrog�e.
     * Utilis� pour optimiser {@link #getValue(Species)}.
     */
    private transient int last;

    /**
     * Num�ro d'identification de l'�chantillons.
     */
    protected final int ID;

    /**
     * La campagne d'�chantillonage pendant laquelle a �t� pris cet �chantillon.
     */
    private final CruiseEntry cruise;

    /**
     * Quantit� mesur� (par exemple nombre de poissons captur�s) pour chaque esp�ce.
     * Ce tableau devra obligatoirement avoir une longueur �gale au nombre d'esp�ces.
     */
    protected final float[] amount;

    /**
     * Construit une entr�. Ce constructeur initialise le tableau {@link #amount},
     * mais ne lui affecte pas de valeurs. Il est du ressort des constructeurs des
     * classes d�riv�es d'affecter des valeurs aux �l�ments de {@link #amount}.
     *
     * @param ID Num�ro identifiant cet �chantillons.
     * @param cruise La campagne d'�chantillonage pendant laquelle a �t� pris cet �chantillon.
     * @param species Esp�ce composant cet �chantillons. <strong>Ce tableau ne sera pas clon�</strong>.
     *        Evitez donc de le modifier apr�s la construction.
     */
    protected SampleEntry(final int ID, final CruiseEntry cruise, final Species[] species) {
        super(species);
        this.ID     = ID;
        this.cruise = cruise;
        this.amount = new float[species.length];
    }

    /**
     * Retourne un num�ro identifiant cet �chantillons. Ce num�ro doit �tre unique pour une
     * m�me base de donn�es (il peut �tre n�cessaire de red�finir cette m�thode pour �a, en
     * particulier pour les donn�es des senneurs qui peuvent combiner le num�ro de la mar�e
     * avec un num�ro de la capture dans cette mar�e).
     */
    public int getID() {
        return ID;
    }

    /**
     * Retourne un nom par d�faut pour cette entr�e. L'impl�mentation par d�faut
     * retourne le num�ro {@link #ID}. Les classes d�riv�es pourraient red�finir
     * cette m�thode pour retourner par exemple le num�ro de mar�e suivit du
     * num�ro de la capture dans cette mar�e.
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
     * Retourne un code repr�sentant cet �chantillon.
     */
    public final int hashCode() {
        return ID;
    }

    /**
     * V�rifie si cette entr� est identique � l'objet sp�cifi�.
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
     * Retourne une cha�ne de caract�res repr�sentant cet �chantillon.
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
