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
 * Temporary wrapper around {@link org.geotools.resources.XArray}
 * leveraging generic type safety. This temporary wrapper will
 * be removed when generic type will be available in JDK 1.5.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class XArray
{
    /**
     * Toute construction d'objet
     * de cette classe est interdites.
     */
    private XArray()
    {}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à <code>null</code>
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static <Element> Element[] resize(final Element[] array, final int length)
    {return (Element[]) org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static double[] resize(final double[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static float[] resize(final float[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static long[] resize(final long[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static int[] resize(final int[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static short[] resize(final short[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static byte[] resize(final byte[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à 0
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static char[] resize(final char[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Renvoie un nouveau tableau qui contiendra les mêmes éléments que <code>array</code> mais avec la longueur <code>length</code>
     * spécifiée. Si la longueur désirée <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourné contiendra tous les éléments de <code>array</code> avec en plus des éléments initialisés à <code>false</code>
     * à la fin du tableau. Si au contraire la longueur désirée <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqué (c'est à dire que les éléments en trop de <code>array</code> seront oubliés).
     * Si la longueur de <code>array</code> est égale à <code>length</code>, alors <code>array</code> sera retourné tel quel.
     *
     * @param  array Tableau à copier.
     * @param  length Longueur du tableau désiré.
     * @return Tableau du même type que <code>array</code>, de longueur <code>length</code> et contenant les données de <code>array</code>.
     */
    public static boolean[] resize(final boolean[] array, final int length)
    {return org.geotools.resources.XArray.resize(array, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static <Element> Element[] remove(final Element[] array, final int index, final int length)
    {return (Element[]) org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static double[] remove(final double[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static float[] remove(final float[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static long[] remove(final long[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static int[] remove(final int[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static short[] remove(final short[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static byte[] remove(final byte[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static char[] remove(final char[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Retire des éléments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des éléments.
     * @param index   Index dans <code>array</code> du premier élément à retirer.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'éléments à retirer.
     * @return        Tableau qui contient la données de <code>array</code> avec des éléments retirés.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static boolean[] remove(final boolean[] array, final int index, final int length)
    {return org.geotools.resources.XArray.remove(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués d'élements nuls.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static <Element> Element[] insert(final Element[] array, final int index, final int length)
    {return (Element[]) org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static double[] insert(final double[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static float[] insert(final float[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> qui suivent cet index peuvent être décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static long[] insert(final long[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static int[] insert(final int[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static short[] insert(final short[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static byte[] insert(final byte[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de zéros.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static char[] insert(final char[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitués de <code>false</code>.
     *
     * @param array   Tableau dans lequel insérer des espaces.
     * @param index   Index de <code>array</code> où insérer les espaces.
     *                Tous les éléments de <code>array</code> dont l'index est
     *                égal ou supérieur à <code>index</code> seront décalés.
     * @param length  Nombre d'espaces à insérer.
     * @return        Tableau qui contient la données de <code>array</code> avec l'espace suplémentaire.
     *                Cette méthode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement créé.
     */
    public static boolean[] insert(final boolean[] array, final int index, final int length)
    {return org.geotools.resources.XArray.insert(array, index, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static <Element> Element[] insert(final Element[] src, final int src_pos, final Element[] dst, final int dst_pos, final int length)
    {return (Element[]) org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static double[] insert(final double[] src, final int src_pos, final double[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static float[] insert(final float[] src, final int src_pos, final float[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static long[] insert(final long[] src, final int src_pos, final long[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static int[] insert(final int[] src, final int src_pos, final int[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static short[] insert(final short[] src, final int src_pos, final short[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static byte[] insert(final byte[] src, final int src_pos, final byte[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static char[] insert(final char[] src, final int src_pos, final char[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}

    /**
     * Insère une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera inséré en totalité ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau à insérer dans <code>dst</code>.
     * @param src_pos Index de la première donnée de <code>src</code> à insérer dans <code>dst</code>.
     * @param dst     Tableau dans lequel insérer des données de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> où insérer les données de <code>src</code>.
     *                Tous les éléments de <code>dst</code> dont l'index est
     *                égal ou supérieur à <code>dst_pos</code> seront décalés.
     * @param length  Nombre de données de <code>src</code> à insérer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                méthode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement créé.
     */
    public static boolean[] insert(final boolean[] src, final int src_pos, final boolean[] dst, final int dst_pos, final int length)
    {return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);}
}
