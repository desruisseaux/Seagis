/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.util;


/**
 * Statistiques concernant les intervalles entres des nombres r�els.
 * Cette classe est utile lorsque l'on veut obtenir l'intervalle
 * d'�chantillonnage entre des donn�es ou d�terminer si ces donn�es
 * sont en ordre croissant ou d�croissant.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class DeltaStatistics extends Statistics
{
    /**
     * Valeur de la derni�re donn�e qui avait �t�
     * transmises � la m�thode {@link #add(double)}.
     */
    private transient double last=Double.NaN;

    /**
     * Valeur de la derni�re donn�e qui avait �t�
     * transmises � la m�thode {@link #add(long)}.
     */
    private transient double last_long;

    /**
     * Indique si le dernier appel de <code>add</code>
     * �tait avec un entier long en argument.
     */
    private transient boolean longValid;

    /**
     * Construit des statistiques initialis�es � NaN.
     */
    public DeltaStatistics()
    {}

    /**
     * Construit des statistics initialis�es
     * avec les valeurs sp�cifi�es.
     */
    public DeltaStatistics(final double min, final double max, final double sum, final double sum2, final int n)
    {super(min, max, sum, sum2, n);}

    /**
     * R�initialise ces statistiques � NaN.
     */
    public void reset()
    {
        super.reset();
        last=Double.NaN;
    }

    /**
     * Ajoute une donn�e aux statistiques. Cet objet retiendra des statistiques
     * (minimum, maximum, etc...) au sujet de la diff�rence entre cette donn�e
     * et la donn�e pr�c�dente qui avait �t� sp�cifi�e lors du dernier appel de
     * cette m�thode ou de l'autre m�thode {@link #add(long)}.
     */
    public void add(final double datum)
    {
        super.add(datum-last);
        last=datum;
        longValid=false;
    }

    /**
     * Ajoute une donn�e aux statistiques. Cet objet retiendra des statistiques
     * (minimum, maximum, etc...) au sujet de la diff�rence entre cette donn�e
     * et la donn�e pr�c�dente qui avait �t� sp�cifi�e lors du dernier appel de
     * cette m�thode ou de l'autre m�thode {@link #add(double)}.
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
     * Ajoute � ces statistiques celles de l'objet sp�cifi�es. Cette m�thode
     * n'accepte que des objets qui de classe <code>DeltaStatistics</code> ou
     * d�riv�e.
     *
     * @throws ClassCastException Si <code>stats</code> n'est
     *         pas de la classe <code>DeltaStatistics</code>.
     */
    public void add(final Statistics stats) throws ClassCastException
    {super.add((DeltaStatistics) stats);}
}
