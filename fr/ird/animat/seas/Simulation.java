/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.animat.seas;

// J2SE
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Seagis
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.fishery.FisheryDataBase;


/**
 * Simulation pour le projet Seas.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Simulation extends fr.ird.animat.impl.Simulation {
    /**
     * La base de données des images.
     */
    private final ImageDataBase images;

    /**
     * La base de données des pêches.
     */
    private final FisheryDataBase fisheries;

    /**
     * Le processus qui aura la charge de fermer les connexion aux bases de données.
     */
    private final Thread shutdown;

    /**
     * Construit une simulation à partir du fichier de configuration spécifié.
     *
     * @param  url L'adresse URL du fichier de configuration.
     * @throws IOException si le fichier de configuration n'a pas pu être lu.
     * @throws SQLException si la connexion à une base de données a échouée.
     */
    public Simulation(final URL url) throws IOException, SQLException {
        this(url.getPath(), new Configuration(url));
    }
    
    /**
     * Construit une simulation à partir du fichier de configuration spécifié.
     *
     * @param  file Le fichier de configuration.
     * @throws IOException si le fichier de configuration n'a pas pu être lu.
     * @throws SQLException si la connexion à une base de données a échouée.
     */
    public Simulation(final File file) throws IOException, SQLException {
        this(file.getPath(), new Configuration(file));
    }
    
    /**
     * Construit une simulation à partir de la configuration spécifiée.
     * Ce constructeur devrait faire partie intégrante d'un constructeur précédent
     * si Sun voulait bien donner suite au RFE #4093999
     */
    private Simulation(final String name, final Configuration configuration) throws SQLException {
        this(name, new ImageDataBase(), new FisheryDataBase(), configuration);
    }

    /**
     * Construit une nouvelle simulation. Ce constructeur devrait faire partie intégrante
     * d'un constructeur précédent si Sun voulait bien donner suite au RFE #4093999
     */
    private Simulation(final String          name,
                       final ImageDataBase   images,
                       final FisheryDataBase fisheries,
                       final Configuration   configuration)
            throws SQLException
    {
        super(name, new Environment(images, fisheries, configuration));
        this.images    = images;
        this.fisheries = fisheries;
        shutdown = new Thread() {
            public void run() {
                try {
                    images.close();
                    fisheries.close();
                } catch (SQLException exception) {
                    final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
                    record.setSourceClassName("Simulation");
                    record.setSourceMethodName("shutdown");
                    record.setThrown(exception);
                    Logger.getLogger("fr.ird.animat.seas").log(record);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    /**
     * Ferme les connexions à la base de données.
     */
    protected void finalize() throws Throwable {
        Runtime.getRuntime().removeShutdownHook(shutdown);
        fisheries.close();
        images.close();
        super.finalize();
    }
}
