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
package net.seas.resources;

// Divers
import java.util.Locale;
import java.io.IOException;


/**
 * Ressources en langue française.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Resources_fr extends Resources
{
    /**
     * Nom du fichier dans lequel sont enregistrées les données.
     */
    private static final String FILEPATH = "net/seas/resources/resources_fr.serialized";

    /**
     * Initialise les ressources françaises.
     * @throws IOException si les ressources
     *         n'ont pas pu être ouvertes.
     */
    public Resources_fr() throws IOException
    {super(Locale.FRANCE, FILEPATH);}
}
