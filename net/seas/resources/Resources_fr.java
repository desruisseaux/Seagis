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
package net.seas.resources;

// Divers
import java.util.Locale;
import java.io.IOException;


/**
 * Ressources en langue fran�aise.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Resources_fr extends Resources
{
    /**
     * Nom du fichier dans lequel sont enregistr�es les donn�es.
     */
    private static final String FILEPATH = "net/seas/resources/resources_fr.serialized";

    /**
     * Initialise les ressources fran�aises.
     * @throws IOException si les ressources
     *         n'ont pas pu �tre ouvertes.
     */
    public Resources_fr() throws IOException
    {super(Locale.FRANCE, FILEPATH);}
}
