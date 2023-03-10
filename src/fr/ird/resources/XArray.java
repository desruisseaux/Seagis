/*
 * SEAS - Surveillance de l'Environnement Assist?e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist?e par Satellite
 *             Institut de Recherche pour le D?veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.resources;


/**
 * Temporary wrapper around {@link org.geotools.resources.XArray}
 * leveraging generic type safety. This temporary wrapper will
 * be removed when generic type will be available in JDK 1.5.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class XArray {
    /**
     * Toute construction d'objet
     * de cette classe est interdites.
     */
    private XArray() {
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? <code>null</code>
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static <Element> Element[] resize(final Element[] array, final int length) {
        return (Element[]) org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static double[] resize(final double[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static float[] resize(final float[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static long[] resize(final long[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static int[] resize(final int[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static short[] resize(final short[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static byte[] resize(final byte[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? 0
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static char[] resize(final char[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Renvoie un nouveau tableau qui contiendra les m?mes ?l?ments que <code>array</code> mais avec la longueur <code>length</code>
     * sp?cifi?e. Si la longueur d?sir?e <code>length</code> est plus grande que la longueur initiale du tableau <code>array</code>,
     * alors le tableau retourn? contiendra tous les ?l?ments de <code>array</code> avec en plus des ?l?ments initialis?s ? <code>false</code>
     * ? la fin du tableau. Si au contraire la longueur d?sir?e <code>length</code> est plus courte que la longueur initiale du tableau
     * <code>array</code>, alors le tableau sera tronqu? (c'est ? dire que les ?l?ments en trop de <code>array</code> seront oubli?s).
     * Si la longueur de <code>array</code> est ?gale ? <code>length</code>, alors <code>array</code> sera retourn? tel quel.
     *
     * @param  array Tableau ? copier.
     * @param  length Longueur du tableau d?sir?.
     * @return Tableau du m?me type que <code>array</code>, de longueur <code>length</code> et contenant les donn?es de <code>array</code>.
     */
    public static boolean[] resize(final boolean[] array, final int length) {
        return org.geotools.resources.XArray.resize(array, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static <Element> Element[] remove(final Element[] array, final int index, final int length) {
        return (Element[]) org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static double[] remove(final double[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static float[] remove(final float[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static long[] remove(final long[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static int[] remove(final int[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static short[] remove(final short[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static byte[] remove(final byte[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static char[] remove(final char[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Retire des ?l?ments au milieu d'un tableau.
     *
     * @param array   Tableau dans lequel retirer des ?l?ments.
     * @param index   Index dans <code>array</code> du premier ?l?ment ? retirer.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'?l?ments ? retirer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec des ?l?ments retir?s.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static boolean[] remove(final boolean[] array, final int index, final int length) {
        return org.geotools.resources.XArray.remove(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s d'?lements nuls.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static <Element> Element[] insert(final Element[] array, final int index, final int length) {
        return (Element[]) org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static double[] insert(final double[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static float[] insert(final float[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> qui suivent cet index peuvent ?tre d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static long[] insert(final long[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static int[] insert(final int[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static short[] insert(final short[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static byte[] insert(final byte[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de z?ros.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static char[] insert(final char[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re des espaces au milieu d'un tableau.
     * Ces "espaces" seront constitu?s de <code>false</code>.
     *
     * @param array   Tableau dans lequel ins?rer des espaces.
     * @param index   Index de <code>array</code> o? ins?rer les espaces.
     *                Tous les ?l?ments de <code>array</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>index</code> seront d?cal?s.
     * @param length  Nombre d'espaces ? ins?rer.
     * @return        Tableau qui contient la donn?es de <code>array</code> avec l'espace supl?mentaire.
     *                Cette m?thode peut retourner directement <code>dst</code>, mais la plupart du temps
     *                elle retournera un tableau nouvellement cr??.
     */
    public static boolean[] insert(final boolean[] array, final int index, final int length) {
        return org.geotools.resources.XArray.insert(array, index, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static <Element> Element[] insert(final Element[] src, final int src_pos, final Element[] dst, final int dst_pos, final int length) {
        return (Element[]) org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static double[] insert(final double[] src, final int src_pos, final double[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static float[] insert(final float[] src, final int src_pos, final float[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static long[] insert(final long[] src, final int src_pos, final long[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static int[] insert(final int[] src, final int src_pos, final int[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static short[] insert(final short[] src, final int src_pos, final short[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static byte[] insert(final byte[] src, final int src_pos, final byte[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static char[] insert(final char[] src, final int src_pos, final char[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }

    /**
     * Ins?re une portion de tableau dans un autre tableau. Le tableau <code>src</code>
     * sera ins?r? en totalit? ou en partie dans le tableau <code>dst</code>.
     *
     * @param src     Tableau ? ins?rer dans <code>dst</code>.
     * @param src_pos Index de la premi?re donn?e de <code>src</code> ? ins?rer dans <code>dst</code>.
     * @param dst     Tableau dans lequel ins?rer des donn?es de <code>src</code>.
     * @param dst_pos Index de <code>dst</code> o? ins?rer les donn?es de <code>src</code>.
     *                Tous les ?l?ments de <code>dst</code> dont l'index est
     *                ?gal ou sup?rieur ? <code>dst_pos</code> seront d?cal?s.
     * @param length  Nombre de donn?es de <code>src</code> ? ins?rer.
     * @return        Tableau qui contient la combinaison de <code>src</code> et <code>dst</code>. Cette
     *                m?thode peut retourner directement <code>dst</code>, mais jamais <code>src</code>.
     *                La plupart du temps, elle retournera un tableau nouvellement cr??.
     */
    public static boolean[] insert(final boolean[] src, final int src_pos, final boolean[] dst, final int dst_pos, final int length) {
        return org.geotools.resources.XArray.insert(src, src_pos, dst, dst_pos, length);
    }
}
