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

// Miscellaneous
import java.util.Set;
import fr.ird.animat.Species;


/**
 * Classe de base des captures.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class AbstractCatchEntry /*extends SpeciesSet*/ implements CatchEntry
{
    /**
     * TODO: Temporary workaround for generic javac's bug.
     */
    private final Species[] species;

    /**
     * Numéro d'identification de la pêche.
     */
    protected final int ID;

    /**
     * Quantité de poissons capturés pour chaque espèce. Ce tableau devra
     * obligatoirement avoir la même longueur que {@link #species}.
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
    protected AbstractCatchEntry(final int ID, final Species[] species)
    {
//      super(species);
        this.species = species; // TODO: Temporary workaround.
        this.ID     = ID;
        this.amount = new float[species.length];
    }

    /**
     * Retourne un nom par défaut pour cette entrée. L'implémentation par défaut
     * retourne le numéro {@link #ID}. Les classes dérivées pourraient redéfinir
     * cette méthode pour retourner par exemple le numéro de marée suivit du
     * numéro de la capture dans cette marée.
     */
    public String getName()
    {return String.valueOf(ID);}

    /**
     * Retourne un numéro identifiant cette capture. Ce numéro doit être
     * unique pour une même base de données  (il peut être nécessaire de
     * redéfinir cette méthode pour ça,  en particulier pour les données
     * des senneurs  qui  peuvent combiner le numéro de la marée avec un
     * numéro de la capture dans cette marée).
     */
    public int getID()
    {return ID;}

    /**
     * Retourne l'ensemble des espèces pêchées. Il n'est pas obligatoire
     * que {@link #getCatchAmount(Species)} retourne une valeur différente
     * de zéro pour chacune de ces espèces.
     */
    public final Set<Species> getSpecies()
    {
    //  return this;
        return new SpeciesSet(species); // TODO: Temporary workaround.
    }

    /**
     * Retourne la quantité de poissons pêchés pour une expèce donnée.
     */
    public final float getCatchAmount(final Species species)
    {
        final Species[] array=this.species;
        for (int i=0; i<array.length; i++)
            if (array[i].equals(species))
                return amount[i];
        return 0;
    }

    /**
     * Retourne la quantité totale de poissons pêchés, toutes espèces confondues.
     * La quantité retournée par cette méthode doit être la somme des quantitées
     * <code>{@link #getCatchAmount getCatchAmount}(i)</code>   où  <var>i</var>
     * varie de 0 inclusivement jusqu'à <code>{@link #getSpecies()}.size()</code>
     * exclusivement.
     */
    public final float getCatchAmount()
    {
        double total=0;
        for (int i=amount.length; --i>=0;)
            total += amount[i];
        return (float) total;
    }

    /**
     * Si cette capture ne contient pas les coordonnées de début
     * et de fin de la ligne, ramène la position spécifiée à une
     * des valeurs {@link EnvironmentTable#START_POINT} ou
     * {@link EnvironmentTable#END_POINT} en fonction de la
     * coordonnée disponible.
     */
    int clampPosition(final int pos)
    {return pos;}

    /**
     * Retourne un code représentant cette capture.
     */
    public final int hashCode()
    {return ID;}
}
