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
     * Index de la derni�re esp�ce interrog�e.
     * Utilis� pour optimiser {@link #getCatch(Species)}.
     */
    private transient int last;

    /**
     * Num�ro d'identification de la p�che.
     */
    protected final int ID;

    /**
     * Quantit� de poissons captur�s pour chaque esp�ce. Ce tableau devra
     * obligatoirement avoir la m�me longueur que {@link #species}.
     */
    protected final float[] amount;

    /**
     * Construit une capture. Ce constructeur initialise le tableau {@link #amount},
     * mais ne lui affecte pas de valeurs. Il est du ressort des constructeurs des
     * classes d�riv�es d'affecter des valeurs aux �l�ments de {@link #amount}.
     *
     * @param ID Num�ro identifiant cette capture.
     * @param species Esp�ce composant cette capture.
     *        <strong>Ce tableau ne sera pas clon�</strong>.
     *        Evitez donc de le modifier apr�s la construction.
     */
    protected AbstractCatchEntry(final int ID, final Species[] species)
    {
//      super(species);
        this.species = species; // TODO: Temporary workaround.
        this.ID     = ID;
        this.amount = new float[species.length];
    }

    /**
     * Retourne un nom par d�faut pour cette entr�e. L'impl�mentation par d�faut
     * retourne le num�ro {@link #ID}. Les classes d�riv�es pourraient red�finir
     * cette m�thode pour retourner par exemple le num�ro de mar�e suivit du
     * num�ro de la capture dans cette mar�e.
     */
    public String getName()
    {return String.valueOf(ID);}

    /**
     * Retourne un num�ro identifiant cette capture. Ce num�ro doit �tre
     * unique pour une m�me base de donn�es  (il peut �tre n�cessaire de
     * red�finir cette m�thode pour �a,  en particulier pour les donn�es
     * des senneurs  qui  peuvent combiner le num�ro de la mar�e avec un
     * num�ro de la capture dans cette mar�e).
     */
    public int getID()
    {return ID;}

    /**
     * Retourne l'esp�ce la plus p�ch�e dans cette capture. Si aucune esp�ce
     * n'a �t� captur�e, alors cette m�thode retourne <code>null</code>.
     */
    public Species getDominantSpecies()
    {
        Species dominant = null;
        float max=Float.NEGATIVE_INFINITY;
        for (int i=amount.length; --i>=0;)
        {
            if (amount[i] >= max)
            {
                max = amount[i];
                dominant = species[i];
            }
        }
        return dominant;
    }

    /**
     * Retourne l'ensemble des esp�ces p�ch�es. Il n'est pas obligatoire
     * que {@link #getCatch(Species)} retourne une valeur diff�rente de
     * z�ro pour chacune de ces esp�ces.
     */
    public final Set<Species> getSpecies()
    {
    //  return this;
        return new SpeciesSet(species); // TODO: Temporary workaround.
    }

    /**
     * Retourne la quantit� de poissons p�ch�s pour une esp�ce donn�e.
     */
    public final float getCatch(final Species species)
    {
        // Fast check (slight optimization)
        final Species[] array=this.species;
        if (++last == array.length) last=0;
        if (array[last]==species)
            return amount[last];

        // Normal (slower) check.
        for (int i=0; i<array.length; i++)
            if (array[i].equals(species))
                return amount[last=i];
        return 0;
    }

    /**
     * Retourne la quantit� totale de poissons p�ch�s, toutes esp�ces confondues.
     * La quantit� retourn�e par cette m�thode est la somme des quantit�es
     * <code>{@link #getCatch getCatch}(i)</code> o� <var>i</var> varie de 0
     * inclusivement jusqu'� <code>{@link #getSpecies()}.size()</code> exclusivement.
     */
    public final float getCatch()
    {
        double total=0;
        for (int i=amount.length; --i>=0;)
            total += amount[i];
        return (float) total;
    }

    /**
     * Si cette capture ne contient pas les coordonn�es de d�but
     * et de fin de la ligne, ram�ne la position sp�cifi�e � une
     * des valeurs {@link EnvironmentTable#START_POINT} ou
     * {@link EnvironmentTable#END_POINT} en fonction de la
     * coordonn�e disponible.
     */
    int clampPosition(final int pos)
    {return pos;}

    /**
     * Retourne un code repr�sentant cette capture.
     */
    public final int hashCode()
    {return ID;}
}
