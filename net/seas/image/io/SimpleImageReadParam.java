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
package net.seas.image.io;

// Divers
import javax.imageio.ImageReadParam;


/**
 * Param�tres des images simples.  Cette classe ne contient aucune nouvelle m�thode par rapport
 * � {@link ImageReadParam}. Elle sert simplement � indiquer que le d�codeur qui a construit ce
 * bloc de param�tre est de la classe {@link SimpleImageReader}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class SimpleImageReadParam extends ImageReadParam
{
    /**
     * Construit un bloc de param�tres initialement vide.
     */
    public SimpleImageReadParam()
    {}
}
