/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.util;


/**
 * Statistiques concernant les intervalles entres des nombres réels.
 * Cette classe est utile lorsque l'on veut obtenir l'intervalle
 * d'échantillonnage entre des données ou déterminer si ces données
 * sont en ordre croissant ou décroissant.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class DeltaStatistics extends Statistics
{
    /**
     * Valeur de la dernière donnée qui avait été
     * transmises à la méthode {@link #add(double)}.
     */
    private transient double last=Double.NaN;

    /**
     * Valeur de la dernière donnée qui avait été
     * transmises à la méthode {@link #add(long)}.
     */
    private transient double last_long;

    /**
     * Indique si le dernier appel de <code>add</code>
     * était avec un entier long en argument.
     */
    private transient boolean longValid;

    /**
     * Construit des statistiques initialisées à NaN.
     */
    public DeltaStatistics()
    {}

    /**
     * Construit des statistics initialisées
     * avec les valeurs spécifiées.
     */
    public DeltaStatistics(final double min, final double max, final double sum, final double sum2, final int n)
    {super(min, max, sum, sum2, n);}

    /**
     * Réinitialise ces statistiques à NaN.
     */
    public void reset()
    {
        super.reset();
        last=Double.NaN;
    }

    /**
     * Ajoute une donnée aux statistiques. Cet objet retiendra des statistiques
     * (minimum, maximum, etc...) au sujet de la différence entre cette donnée
     * et la donnée précédente qui avait été spécifiée lors du dernier appel de
     * cette méthode ou de l'autre méthode {@link #add(long)}.
     */
    public void add(final double datum)
    {
        super.add(datum-last);
        last=datum;
        longValid=false;
    }

    /**
     * Ajoute une donnée aux statistiques. Cet objet retiendra des statistiques
     * (minimum, maximum, etc...) au sujet de la différence entre cette donnée
     * et la donnée précédente qui avait été spécifiée lors du dernier appel de
     * cette méthode ou de l'autre méthode {@link #add(double)}.
     */
    public void add(final long datum)
    {
        if (longValid)
            super.add(datum-last_long); // add(long)
        else
            super.add(datum-last); // add(double)
        last=datum;
        last_long=datum;
        longValid=true;
    }

    /**
     * Ajoute à ces statistiques celles de l'objet spécifiées. Cette méthode
     * n'accepte que des objets qui de classe <code>DeltaStatistics</code> ou
     * dérivée.
     *
     * @throws ClassCastException Si <code>stats</code> n'est
     *         pas de la classe <code>DeltaStatistics</code>.
     */
    public void add(final Statistics stats) throws ClassCastException
    {super.add((DeltaStatistics) stats);}
}
