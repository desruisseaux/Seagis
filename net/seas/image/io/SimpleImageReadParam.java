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
package net.seas.image.io;

// Divers
import javax.imageio.ImageReadParam;


/**
 * Paramètres des images simples.  Cette classe ne contient aucune nouvelle méthode par rapport
 * à {@link ImageReadParam}. Elle sert simplement à indiquer que le décodeur qui a construit ce
 * bloc de paramètre est de la classe {@link SimpleImageReader}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class SimpleImageReadParam extends ImageReadParam
{
    /**
     * Construit un bloc de paramètres initialement vide.
     */
    public SimpleImageReadParam()
    {}
}
